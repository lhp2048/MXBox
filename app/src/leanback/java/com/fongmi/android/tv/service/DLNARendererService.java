package com.fongmi.android.tv.service;

import android.app.Notification;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.media3.common.C;
import androidx.media3.common.Player;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.BuildConfig;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.dlna.CastAction;
import com.fongmi.android.tv.dlna.DLNAAvTransportImpl;
import com.fongmi.android.tv.dlna.DLNARenderingControlImpl;
import com.fongmi.android.tv.dlna.DLNAServiceConfiguration;
import com.fongmi.android.tv.dlna.RenderState;
import com.fongmi.android.tv.player.PlayerManager;
import com.fongmi.android.tv.setting.CastSetting;
import com.fongmi.android.tv.utils.Notify;
import com.fongmi.android.tv.MxBoxConstants;

import com.github.catvod.utils.Util;

import org.jupnp.UpnpServiceConfiguration;
import org.jupnp.android.AndroidUpnpServiceImpl;
import org.jupnp.binding.annotations.AnnotationLocalServiceBinder;
import org.jupnp.model.DefaultServiceManager;
import org.jupnp.model.meta.DeviceDetails;
import org.jupnp.model.meta.DeviceIdentity;
import org.jupnp.model.meta.LocalDevice;
import org.jupnp.model.meta.LocalService;
import org.jupnp.model.meta.ManufacturerDetails;
import org.jupnp.model.meta.ModelDetails;
import org.jupnp.model.types.UDADeviceType;
import org.jupnp.model.types.UDN;
import org.jupnp.support.avtransport.lastchange.AVTransportLastChangeParser;
import org.jupnp.support.connectionmanager.ConnectionManagerService;
import org.jupnp.support.lastchange.LastChangeAwareServiceManager;
import org.jupnp.support.renderingcontrol.lastchange.RenderingControlLastChangeParser;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class DLNARendererService extends AndroidUpnpServiceImpl implements ServiceConnection {

    private static final String TAG = "DLNARendererService";

    private static volatile boolean registered;

    private final IBinder binder = new LocalBinder();

    private volatile PlayerManager player;
    private volatile boolean isDlnaActive;

    private DLNARenderingControlImpl renderingControlImpl;
    private DLNAAvTransportImpl avTransportImpl;
    private PlaybackService playbackService;
    private Player currentListenerPlayer;
    private LocalDevice localDevice;
    private WifiManager.MulticastLock multicastLock;
    private ConnectivityManager.NetworkCallback networkCallback;
    private boolean bound;

    public static boolean isRegistered() {
        return registered;
    }

    public static void start(Context context) {
        context.startService(new Intent(context, DLNARendererService.class));
    }

    public static void stop(Context context) {
        context.stopService(new Intent(context, DLNARendererService.class));
    }

    public String getDisplayDeviceName() {
        return CastSetting.getDeviceName();
    }

    public String getLocalIp() {
        return Util.getIp();
    }

    public void reregisterDevice() {
        unregisterLocalDevice();
        registerLocalDevice();
    }

    @Override
    protected UpnpServiceConfiguration createConfiguration() {
        return new DLNAServiceConfiguration();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Notification notification = new NotificationCompat.Builder(this, Notify.DEFAULT)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(getString(R.string.setting_cast_running))
                .setSilent(true)
                .build();
        startForeground(Notify.ID + 1, notification);
        acquireMulticastLock();
        registerNetworkCallback();
        upnpService.startup();
        registerLocalDevice();
    }

    private void acquireMulticastLock() {
        try {
            WifiManager wm = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            if (wm == null) return;
            multicastLock = wm.createMulticastLock("dlna");
            multicastLock.setReferenceCounted(true);
            multicastLock.acquire();
        } catch (Exception e) {
            Log.w(TAG, "Failed to acquire multicast lock", e);
        }
    }

    private void releaseMulticastLock() {
        if (multicastLock == null) return;
        try {
            if (multicastLock.isHeld()) multicastLock.release();
        } catch (Exception e) {
            Log.w(TAG, "Failed to release multicast lock", e);
        }
        multicastLock = null;
    }

    private void registerNetworkCallback() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return;
        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(@NonNull Network network) {
                App.post(DLNARendererService.this::reregisterDevice);
            }

            @Override
            public void onLost(@NonNull Network network) {
                App.post(DLNARendererService.this::unregisterLocalDevice);
            }
        };
        cm.registerDefaultNetworkCallback(networkCallback);
    }

    private void unregisterNetworkCallback() {
        if (networkCallback == null) return;
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null) cm.unregisterNetworkCallback(networkCallback);
        networkCallback = null;
    }

    private void registerLocalDevice() {
        if (localDevice != null) return;
        LocalService<DLNAAvTransportImpl> avTransport = createAvTransport();
        LocalService<ConnectionManagerService> connManager = createConnectionManager();
        LocalService<DLNARenderingControlImpl> renderControl = createRenderingControl();
        DeviceIdentity identity = new DeviceIdentity(new UDN(UUID.nameUUIDFromBytes((Build.MANUFACTURER + Build.MODEL + "-MediaRenderer").getBytes(StandardCharsets.UTF_8))));
        UDADeviceType type = new UDADeviceType("MediaRenderer", 1);
        DeviceDetails details = new DeviceDetails(CastSetting.getDeviceName(), new ManufacturerDetails(MxBoxConstants.APP_NAME), new ModelDetails(Build.MODEL, MxBoxConstants.APP_NAME, BuildConfig.VERSION_NAME));
        try {
            localDevice = new LocalDevice(identity, type, details, new LocalService[]{avTransport, connManager, renderControl});
            upnpService.getRegistry().addDevice(localDevice);
            registered = true;
            Log.i(TAG, "DLNA device registered: " + CastSetting.getDeviceName() + " @ " + Util.getIp());
        } catch (Exception e) {
            registered = false;
            localDevice = null;
            Log.w(TAG, "Failed to register DLNA device", e);
        }
    }

    private void unregisterLocalDevice() {
        if (localDevice == null) return;
        try {
            upnpService.getRegistry().removeDevice(localDevice);
            Log.i(TAG, "DLNA device unregistered");
        } catch (Exception e) {
            Log.w(TAG, "Failed to unregister DLNA device", e);
        }
        localDevice = null;
        registered = false;
    }

    @SuppressWarnings("unchecked")
    private LocalService<DLNAAvTransportImpl> createAvTransport() {
        avTransportImpl = new DLNAAvTransportImpl(this);
        LocalService<DLNAAvTransportImpl> service = new AnnotationLocalServiceBinder().read(DLNAAvTransportImpl.class);
        service.setManager(new LastChangeAwareServiceManager<>(service, new AVTransportLastChangeParser()) {
            @Override
            protected DLNAAvTransportImpl createServiceInstance() {
                return avTransportImpl;
            }
        });
        return service;
    }

    @SuppressWarnings("unchecked")
    private LocalService<ConnectionManagerService> createConnectionManager() {
        LocalService<ConnectionManagerService> service = new AnnotationLocalServiceBinder().read(ConnectionManagerService.class);
        service.setManager(new DefaultServiceManager<>(service, ConnectionManagerService.class));
        return service;
    }

    @SuppressWarnings("unchecked")
    private LocalService<DLNARenderingControlImpl> createRenderingControl() {
        renderingControlImpl = new DLNARenderingControlImpl(this);
        LocalService<DLNARenderingControlImpl> service = new AnnotationLocalServiceBinder().read(DLNARenderingControlImpl.class);
        service.setManager(new LastChangeAwareServiceManager<>(service, new RenderingControlLastChangeParser()) {
            @Override
            protected DLNARenderingControlImpl createServiceInstance() {
                return renderingControlImpl;
            }
        });
        return service;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onDestroy() {
        unregisterLocalDevice();
        unregisterNetworkCallback();
        releaseMulticastLock();
        unbindPlaybackService();
        registered = false;
        super.onDestroy();
    }

    private void bindPlaybackService() {
        if (bound) return;
        bound = bindService(new Intent(this, PlaybackService.class).setAction(PlaybackService.LOCAL_BIND_ACTION), this, BIND_AUTO_CREATE);
    }

    private void cleanupPlaybackRefs() {
        App.removeCallbacks(positionUpdater);
        if (currentListenerPlayer != null) {
            currentListenerPlayer.removeListener(listener);
            currentListenerPlayer = null;
        }
        if (playbackService != null) {
            playbackService.removePlayerCallback(playerCallback);
            playbackService = null;
        }
        player = null;
        if (avTransportImpl != null) avTransportImpl.setPlayerManager(null);
    }

    private void unbindPlaybackService() {
        if (!bound) return;
        bound = false;
        cleanupPlaybackRefs();
        unbindService(this);
    }

    public void setDlnaActive(boolean active) {
        isDlnaActive = active;
        if (avTransportImpl != null) avTransportImpl.setDlnaActive(active);
        if (active) bindPlaybackService();
        else {
            if (avTransportImpl != null) avTransportImpl.reset();
            unbindPlaybackService();
        }
    }

    public long consumePendingSeekMs() {
        return avTransportImpl != null ? avTransportImpl.consumePendingSeekMs() : -1;
    }

    public CastAction consumeNext() {
        return avTransportImpl != null ? avTransportImpl.popNext() : null;
    }

    public void notifyError() {
        if (avTransportImpl != null) avTransportImpl.fireStateChange(RenderState.STOPPED);
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder binder) {
        if (!bound || !isDlnaActive) {
            unbindPlaybackService();
            return;
        }
        playbackService = ((PlaybackService.LocalBinder) binder).getService();
        playbackService.addPlayerCallback(playerCallback);
        player = playbackService.player();
        avTransportImpl.setPlayerManager(player);
        currentListenerPlayer = player.getPlayer();
        currentListenerPlayer.addListener(listener);
        App.post(positionUpdater, 1000);
        notifyState();
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        cleanupPlaybackRefs();
    }

    private void notifyState() {
        if (avTransportImpl == null || player == null || !isDlnaActive) return;
        int state = player.getPlaybackState();
        if (state == Player.STATE_IDLE) return;
        avTransportImpl.updatePositionCache(player.getPosition(), getDuration());
        RenderState renderState = switch (state) {
            case Player.STATE_BUFFERING -> RenderState.PREPARING;
            case Player.STATE_READY -> player.isPlaying() ? RenderState.PLAYING : RenderState.PAUSED;
            case Player.STATE_ENDED -> avTransportImpl.hasNext() ? RenderState.PREPARING : RenderState.STOPPED;
            default -> null;
        };
        if (renderState != null) avTransportImpl.fireStateChange(renderState);
    }

    private final Runnable positionUpdater = new Runnable() {
        @Override
        public void run() {
            if (player != null && avTransportImpl != null && player.isPlaying()) {
                avTransportImpl.updatePositionCache(player.getPosition(), getDuration());
            }
            if (player != null) App.post(this, 1000);
        }
    };

    private long getDuration() {
        long duration = player.getDuration();
        return duration == C.TIME_UNSET || duration <= 0 ? -1 : duration;
    }

    private final Player.Listener listener = new Player.Listener() {
        @Override
        public void onPlaybackStateChanged(int playbackState) {
            notifyState();
        }

        @Override
        public void onIsPlayingChanged(boolean isPlaying) {
            notifyState();
        }
    };

    private final PlaybackService.PlayerCallback playerCallback = new PlaybackService.PlayerCallback() {
        @Override
        public void onPlayerRebuild(Player newPlayer) {
            if (currentListenerPlayer != null) currentListenerPlayer.removeListener(listener);
            currentListenerPlayer = newPlayer;
            newPlayer.addListener(listener);
            notifyState();
        }
    };

    public class LocalBinder extends Binder {

        public DLNARendererService getService() {
            return DLNARendererService.this;
        }
    }
}
