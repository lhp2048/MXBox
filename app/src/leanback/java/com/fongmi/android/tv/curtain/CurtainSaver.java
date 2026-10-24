package com.fongmi.android.tv.curtain;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;

import androidx.media3.common.Player;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.server.Server;
import com.fongmi.android.tv.service.PlaybackService;
import com.fongmi.android.tv.ui.activity.BiliActivity;
import com.fongmi.android.tv.ui.activity.CurtainActivity;
import com.fongmi.android.tv.ui.activity.PlaybackActivity;

public final class CurtainSaver {

    public static final long IDLE_MS = 5 * 60 * 1000L;

    private static final Handler handler = new Handler(Looper.getMainLooper());
    private static final Runnable fire = CurtainSaver::fire;
    private static Activity owner;
    private static boolean armed;

    private CurtainSaver() {
    }

    public static void arm(Activity activity) {
        owner = activity;
        if (activity instanceof CurtainActivity) {
            armed = false;
            handler.removeCallbacks(fire);
            return;
        }
        armed = true;
        schedule();
    }

    public static void disarm(Activity activity) {
        if (owner != activity) return;
        owner = null;
        armed = false;
        handler.removeCallbacks(fire);
    }

    public static void touch() {
        if (!armed) return;
        schedule();
    }

    private static void schedule() {
        handler.removeCallbacks(fire);
        handler.postDelayed(fire, IDLE_MS);
    }

    private static void fire() {
        if (!armed) return;
        Activity activity = App.activity();
        if (activity == null || activity.isFinishing() || !activity.hasWindowFocus()) {
            if (armed) schedule();
            return;
        }
        if (blocks(activity)) {
            schedule();
            return;
        }
        CurtainActivity.startSaver(activity);
    }

    private static boolean blocks(Activity activity) {
        if (activity instanceof PlaybackActivity playback && playback.blocksCurtainSaver()) return true;
        if (activity instanceof BiliActivity bili && bili.blocksCurtainSaver()) return true;
        PlaybackService service = Server.get().getService();
        if (service == null || service.player() == null || service.player().isReleased()) return false;
        Player player = service.player().getPlayer();
        if (player == null) return false;
        int state = player.getPlaybackState();
        return player.getPlayWhenReady() && state != Player.STATE_IDLE && state != Player.STATE_ENDED;
    }
}
