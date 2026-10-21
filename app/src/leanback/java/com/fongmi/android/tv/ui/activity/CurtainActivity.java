package com.fongmi.android.tv.ui.activity;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.AspectRatioFrameLayout;
import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.curtain.CurtainHub;
import com.fongmi.android.tv.curtain.CurtainStore;
import com.fongmi.android.tv.databinding.ActivityCurtainBinding;
import com.fongmi.android.tv.server.Server;
import com.fongmi.android.tv.service.PlaybackService;
import com.fongmi.android.tv.ui.base.BaseActivity;
import com.fongmi.android.tv.utils.QRCode;
import com.fongmi.android.tv.utils.ResUtil;
import com.github.catvod.utils.Task;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

public class CurtainActivity extends BaseActivity implements CurtainHub.Host {

    private ActivityCurtainBinding binding;
    private ExoPlayer videoPlayer;
    private ExoPlayer musicPlayer;
    private String shownVideo = "";
    private String failedVideo = "";
    private String shownImage = "";
    private String musicKey = "";
    private boolean suppressMusicEnd;
    private boolean timedResident;
    private int musicFailures;
    private int fadeGen;
    private Bitmap imageBitmap;
    private int pushGen;
    private int pushSeq;
    private final List<Line> lines = new ArrayList<>();
    private final List<CurtainHub.Item> records = new ArrayList<>();
    private final Random random = new Random();
    private final Runnable tick = this::tickLines;
    private final Runnable hideResident = () -> {
        timedResident = false;
        applyResident();
    };

    public static void start(Activity activity) {
        activity.startActivity(new Intent(activity, CurtainActivity.class));
    }

    @Override
    protected ViewBinding getBinding() {
        return binding = ActivityCurtainBinding.inflate(getLayoutInflater());
    }

    @Override
    protected boolean customWall() {
        return false;
    }

    @Override
    protected void initView(Bundle savedInstanceState) {
        Server.get().start();
        PlaybackService service = Server.get().getService();
        if (service != null) service.player().pause();
        videoPlayer = new ExoPlayer.Builder(this).build();
        videoPlayer.setRepeatMode(Player.REPEAT_MODE_ONE);
        videoPlayer.addListener(new Player.Listener() {
            @Override
            public void onPlayerError(PlaybackException error) {
                failedVideo = shownVideo;
                binding.player.setVisibility(View.GONE);
                videoPlayer.stop();
            }
        });
        binding.player.setPlayer(videoPlayer);
        binding.player.setShutterBackgroundColor(Color.TRANSPARENT);
        binding.player.post(() -> clearLetterbox(binding.player));
        musicPlayer = new ExoPlayer.Builder(this).build();
        musicPlayer.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int state) {
                if (state == Player.STATE_READY) {
                    musicFailures = 0;
                    suppressMusicEnd = false;
                }
                if (state != Player.STATE_ENDED || suppressMusicEnd) return;
                if (CurtainStore.musicMode() == CurtainStore.PLAY_SINGLE) return;
                String before = CurtainStore.currentMusic();
                CurtainStore.next();
                if (before.equals(CurtainStore.currentMusic())) {
                    musicPlayer.seekTo(0);
                    musicPlayer.play();
                    return;
                }
                musicKey = "";
                applyMusic();
            }

            @Override
            public void onPlayerError(PlaybackException error) {
                musicFailures++;
                if (musicFailures > 8) {
                    CurtainStore.musicPlaying(false);
                    musicPlayer.pause();
                    return;
                }
                CurtainStore.next();
                musicKey = "";
                applyMusic();
            }
        });
        binding.stage.requestFocus();
        apply();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (videoPlayer == null || isFinishing()) return;
        if (!shownVideo.isEmpty() && !shownVideo.equals(failedVideo)) videoPlayer.play();
        applyMusic();
        if (!lines.isEmpty()) binding.texts.postOnAnimation(tick);
    }

    @Override
    protected void onStop() {
        if (videoPlayer != null) videoPlayer.pause();
        binding.texts.removeCallbacks(tick);
        if (musicPlayer != null) {
            fadeGen++;
            musicPlayer.pause();
        }
        super.onStop();
    }

    @Override
    protected void onResume() {
        super.onResume();
        CurtainHub.attach(this);
    }

    @Override
    protected void onPause() {
        CurtainHub.detach(this);
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        binding.texts.removeCallbacks(tick);
        binding.resident.removeCallbacks(hideResident);
        clearLines();
        if (videoPlayer != null) videoPlayer.release();
        if (musicPlayer != null) musicPlayer.release();
        super.onDestroy();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() != KeyEvent.ACTION_DOWN) return super.dispatchKeyEvent(event);
        if (event.getKeyCode() == KeyEvent.KEYCODE_BACK && binding.qr.getVisibility() == View.VISIBLE) {
            hideQr();
            return true;
        }
        if (event.getKeyCode() == KeyEvent.KEYCODE_MENU) {
            if (event.getRepeatCount() > 0) return true;
            if (binding.qr.getVisibility() == View.VISIBLE) hideQr();
            else showQr();
            return true;
        }
        return super.dispatchKeyEvent(event);
    }

    @Override
    protected void onBackInvoked() {
        if (binding.qr.getVisibility() == View.VISIBLE) hideQr();
        else super.onBackInvoked();
    }

    @Override
    public void apply() {
        if (isFinishing()) return;
        binding.stage.setBackgroundColor(CurtainStore.argb(CurtainStore.color(), 0xFF101114));
        applyImage();
        applyVideo();
        applyResident();
        applyMusic();
    }

    @Override
    public void restartMusic() {
        musicKey = "";
        apply();
    }

    @Override
    public void pushText(String text) {
        if (isFinishing() || text == null || text.trim().isEmpty()) return;
        String value = text.trim();
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextColor(PUSH_COLORS[random.nextInt(PUSH_COLORS.length)]);
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, 28 + random.nextInt(37));
        view.setTypeface(Typeface.DEFAULT_BOLD);
        view.setSingleLine(true);
        view.setFocusable(false);
        view.setShadowLayer(ResUtil.dp2px(4), 0, 0, 0xCC000000);
        view.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        String id = nextId();
        remember(id, "text", value, null);
        addLine(id, view, view.getMeasuredWidth(), null, null, 9000L + Math.min(value.length(), 40) * 120L);
    }

    @Override
    public void showResident(String text, int size, int color, boolean stroke, int seconds) {
        if (isFinishing()) return;
        binding.resident.removeCallbacks(hideResident);
        timedResident = seconds > 0;
        if (text == null || text.trim().isEmpty()) {
            timedResident = false;
            binding.resident.setVisibility(View.GONE);
            return;
        }
        binding.resident.setText(text.trim());
        binding.resident.setTextColor(color);
        binding.resident.setTextSize(TypedValue.COMPLEX_UNIT_SP, clamp(size, 24, 96));
        binding.resident.getPaint().setStyle(stroke ? android.graphics.Paint.Style.FILL_AND_STROKE : android.graphics.Paint.Style.FILL);
        binding.resident.getPaint().setStrokeWidth(stroke ? ResUtil.dp2px(2) : 0);
        binding.resident.setShadowLayer(ResUtil.dp2px(3), 0, 0, 0xCC000000);
        binding.resident.setVisibility(View.VISIBLE);
        if (seconds > 0) binding.resident.postDelayed(hideResident, seconds * 1000L);
    }

    @Override
    public void pushImage(File file) {
        if (file == null) return;
        String id = nextId();
        remember(id, "image", imageLabel(file), file);
        int gen = pushGen;
        Task.execute(() -> {
            Bitmap bitmap = decode(file, 1080);
            App.post(() -> presentImage(gen, id, file, bitmap));
        });
    }

    @Override
    public void clearText() {
        clearLines();
    }

    @Override
    public void removePushes(List<String> ids) {
        if (ids == null || ids.isEmpty()) return;
        HashSet<String> wanted = new HashSet<>(ids);
        for (int i = lines.size() - 1; i >= 0; i--) {
            if (wanted.contains(lines.get(i).id)) release(lines.remove(i));
        }
        synchronized (records) {
            for (int i = records.size() - 1; i >= 0; i--) {
                CurtainHub.Item item = records.get(i);
                if (!wanted.contains(item.id)) continue;
                records.remove(i);
                if (item.file != null) item.file.delete();
            }
        }
    }

    @Override
    public List<CurtainHub.Item> pushes() {
        synchronized (records) {
            return new ArrayList<>(records);
        }
    }

    private void applyImage() {
        String name = CurtainStore.imageName();
        binding.image.setScaleType(CurtainStore.imageFit() == CurtainStore.FILL ? android.widget.ImageView.ScaleType.CENTER_CROP : android.widget.ImageView.ScaleType.FIT_CENTER);
        if (name.equals(shownImage)) return;
        shownImage = name;
        File file = CurtainStore.media(name);
        if (file == null || !file.exists()) {
            binding.image.setImageBitmap(null);
            binding.image.setVisibility(View.GONE);
            return;
        }
        Task.execute(() -> {
            Bitmap bitmap = decode(file, 1920);
            App.post(() -> {
                if (isFinishing() || !name.equals(shownImage)) {
                    if (bitmap != null) bitmap.recycle();
                    return;
                }
                Bitmap previous = imageBitmap;
                imageBitmap = bitmap;
                binding.image.setImageBitmap(bitmap);
                binding.image.setVisibility(bitmap == null ? View.GONE : View.VISIBLE);
                if (previous != null) binding.image.post(previous::recycle);
            });
        });
    }

    private void applyVideo() {
        String name = CurtainStore.videoName();
        binding.player.setResizeMode(CurtainStore.videoFit() == CurtainStore.FILL ? AspectRatioFrameLayout.RESIZE_MODE_ZOOM : AspectRatioFrameLayout.RESIZE_MODE_FIT);
        videoPlayer.setVolume(CurtainStore.hearsVideo() ? volume() : 0f);
        if (name.equals(failedVideo)) {
            binding.player.setVisibility(View.GONE);
            return;
        }
        if (name.equals(shownVideo)) return;
        shownVideo = name;
        failedVideo = "";
        File file = CurtainStore.media(name);
        if (file == null || !file.exists()) {
            videoPlayer.stop();
            binding.player.setVisibility(View.GONE);
            return;
        }
        binding.player.setVisibility(View.VISIBLE);
        binding.player.post(() -> clearLetterbox(binding.player));
        videoPlayer.setMediaItem(MediaItem.fromUri(Uri.fromFile(file)));
        videoPlayer.prepare();
        videoPlayer.play();
    }

    private void applyResident() {
        if (timedResident) return;
        if (CurtainStore.resident().isEmpty()) {
            binding.resident.setVisibility(View.GONE);
            return;
        }
        showResident(CurtainStore.resident(), CurtainStore.residentSize(), CurtainStore.argb(CurtainStore.residentColor(), Color.WHITE), CurtainStore.residentStroke(), 0);
    }

    private void applyMusic() {
        boolean play = CurtainStore.musicPlaying() && !CurtainStore.hearsVideo();
        String name = CurtainStore.currentMusic();
        String key = CurtainStore.musicMode() + "|" + name + "|" + play;
        musicPlayer.setRepeatMode(CurtainStore.musicMode() == CurtainStore.PLAY_SINGLE ? Player.REPEAT_MODE_ONE : Player.REPEAT_MODE_OFF);
        if (name.isEmpty()) {
            musicKey = "";
            fadeMusic(0f, true);
            return;
        }
        if (!key.equals(musicKey)) {
            File file = CurtainStore.media(name);
            if (file == null || !file.exists()) {
                musicKey = "";
                return;
            }
            musicKey = key;
            suppressMusicEnd = true;
            musicPlayer.stop();
            musicPlayer.setMediaItem(MediaItem.fromUri(Uri.fromFile(file)));
            musicPlayer.prepare();
            if (play) musicPlayer.play();
            else musicPlayer.pause();
        } else if (play) {
            musicPlayer.play();
        }
        fadeMusic(play ? volume() : 0f, !play);
    }

    private void fadeMusic(float target, boolean pauseAfter) {
        int gen = ++fadeGen;
        float start = musicPlayer.getVolume();
        long began = SystemClock.uptimeMillis();
        Runnable step = new Runnable() {
            @Override
            public void run() {
                if (gen != fadeGen || isFinishing()) return;
                float progress = Math.min(1f, (SystemClock.uptimeMillis() - began) / 300f);
                musicPlayer.setVolume(start + (target - start) * progress);
                if (progress < 1f) binding.stage.postDelayed(this, 30);
                else if (pauseAfter) musicPlayer.pause();
            }
        };
        binding.stage.post(step);
    }

    private void clearLetterbox(View view) {
        if (view instanceof TextureView) {
            ((TextureView) view).setOpaque(false);
            return;
        }
        if (view != binding.player) view.setBackgroundColor(Color.TRANSPARENT);
        if (!(view instanceof ViewGroup)) return;
        ViewGroup group = (ViewGroup) view;
        for (int i = 0; i < group.getChildCount(); i++) clearLetterbox(group.getChildAt(i));
    }

    private void showQr() {
        Server.get().start();
        String address = Server.get().getPageAddress("7");
        binding.qrUrl.setText(address);
        binding.qrImage.setImageBitmap(QRCode.getBitmap(address, 240, 1));
        binding.qr.setVisibility(View.VISIBLE);
    }

    private void hideQr() {
        binding.qr.setVisibility(View.GONE);
    }

    private void tickLines() {
        int width = binding.texts.getWidth();
        int height = binding.texts.getHeight();
        if (width == 0 || height == 0) {
            binding.texts.postOnAnimation(tick);
            return;
        }
        long now = SystemClock.elapsedRealtime();
        for (int i = 0; i < lines.size(); i++) {
            Line line = lines.get(i);
            float progress = (now - line.born) % line.duration / (float) line.duration;
            int room = Math.max(0, height - line.view.getHeight());
            line.view.setTranslationX(width - progress * (width + line.width));
            line.view.setTranslationY(room * line.y / 100f);
        }
        if (!lines.isEmpty()) binding.texts.postOnAnimation(tick);
    }

    private void addLine(String id, View view, int width, File file, Bitmap bitmap, long duration) {
        Line line = new Line();
        line.id = id;
        line.view = view;
        line.width = Math.max(1, width);
        line.file = file;
        line.bitmap = bitmap;
        line.born = SystemClock.elapsedRealtime();
        line.duration = duration;
        line.y = random.nextInt(101);
        lines.add(line);
        binding.texts.addView(view, view.getLayoutParams() instanceof FrameLayout.LayoutParams
                ? (FrameLayout.LayoutParams) view.getLayoutParams()
                : new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        binding.texts.removeCallbacks(tick);
        binding.texts.postOnAnimation(tick);
    }

    private void presentImage(int gen, String id, File file, Bitmap bitmap) {
        if (gen != pushGen || isFinishing() || bitmap == null || !alive(id)) {
            if (bitmap != null) bitmap.recycle();
            if (alive(id)) {
                forget(id);
                if (file != null) file.delete();
            }
            return;
        }
        int screenW = binding.texts.getWidth();
        int screenH = binding.texts.getHeight();
        if (screenW <= 0 || screenH <= 0) {
            binding.texts.post(() -> presentImage(gen, id, file, bitmap));
            return;
        }
        int viewW = screenW * (22 + random.nextInt(24)) / 100;
        float ratio = bitmap.getHeight() / (float) Math.max(1, bitmap.getWidth());
        int viewH = Math.max(1, Math.round(viewW * ratio));
        int maxH = Math.max(1, screenH * 42 / 100);
        if (viewH > maxH) {
            viewH = maxH;
            viewW = Math.max(1, Math.round(viewH / ratio));
        }
        ImageView view = new ImageView(this);
        view.setImageBitmap(bitmap);
        view.setScaleType(ImageView.ScaleType.FIT_CENTER);
        view.setFocusable(false);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(viewW, viewH);
        view.setLayoutParams(params);
        addLine(id, view, viewW, file, bitmap, 12000L);
    }

    private void clearLines() {
        pushGen++;
        binding.texts.removeCallbacks(tick);
        for (int i = lines.size() - 1; i >= 0; i--) release(lines.get(i));
        lines.clear();
        binding.texts.removeAllViews();
        synchronized (records) {
            for (CurtainHub.Item item : records) if (item.file != null) item.file.delete();
            records.clear();
        }
    }

    private void release(Line line) {
        forget(line.id);
        binding.texts.removeView(line.view);
        if (line.view instanceof ImageView) ((ImageView) line.view).setImageDrawable(null);
        if (line.bitmap != null && !line.bitmap.isRecycled()) line.bitmap.recycle();
        if (line.file != null) line.file.delete();
    }

    private String nextId() {
        return String.valueOf(++pushSeq);
    }

    private void remember(String id, String kind, String label, File file) {
        synchronized (records) {
            records.add(new CurtainHub.Item(id, kind, label, file));
        }
    }

    private void forget(String id) {
        synchronized (records) {
            for (int i = records.size() - 1; i >= 0; i--) if (id.equals(records.get(i).id)) records.remove(i);
        }
    }

    private boolean alive(String id) {
        synchronized (records) {
            for (CurtainHub.Item item : records) if (id.equals(item.id)) return true;
        }
        return false;
    }

    private static String imageLabel(File file) {
        String name = file.getName();
        int cut = name.indexOf('_');
        String label = cut >= 0 ? name.substring(cut + 1) : name;
        int dot = label.lastIndexOf('.');
        if (dot > 0) label = label.substring(0, dot);
        return label.isEmpty() ? "图片" : label;
    }

    private static Bitmap decode(File file, int maxSize) {
        if (file == null || !file.exists()) return null;
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(file.getAbsolutePath(), bounds);
        int sample = 1;
        while (bounds.outWidth / sample > maxSize || bounds.outHeight / sample > maxSize) sample *= 2;
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = sample;
        return BitmapFactory.decodeFile(file.getAbsolutePath(), options);
    }

    private static float volume() {
        return CurtainStore.volume() / 100f;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static final int[] PUSH_COLORS = {
            0xFFFFFFFF, 0xFFFFE566, 0xFFFF8A65, 0xFF80D8FF, 0xFFB9F6CA,
            0xFFFF80AB, 0xFFEA80FC, 0xFFFFF59D, 0xFF84FFFF, 0xFFFFCC80
    };

    private static final class Line {
        private String id;
        private View view;
        private int width;
        private long born;
        private long duration;
        private int y;
        private File file;
        private Bitmap bitmap;
    }
}
