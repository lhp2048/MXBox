package com.fongmi.android.tv.bili;

import android.net.Uri;
import android.util.Log;

import androidx.media3.common.MediaItem;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.Player;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.exoplayer.source.MergingMediaSource;
import androidx.media3.exoplayer.source.MediaSource;
import androidx.media3.exoplayer.source.ProgressiveMediaSource;

import com.fongmi.android.tv.player.engine.PlayerEngine;
import com.fongmi.android.tv.player.exo.ExoUtil;

import java.util.HashMap;

public class BiliPlayback {

    public static final String TAG = "BiliPlay";

    private final androidx.media3.exoplayer.ExoPlayer player;

    public BiliPlayback(Player.Listener listener) {
        player = ExoUtil.buildPlayer(PlayerEngine.HARD, listener);
    }

    public androidx.media3.exoplayer.ExoPlayer player() {
        return player;
    }

    public void play(BiliStreams streams, long positionMs) {
        player.stop();
        DefaultHttpDataSource.Factory sourceFactory = new DefaultHttpDataSource.Factory().setUserAgent(BiliApi.UA).setAllowCrossProtocolRedirects(true).setDefaultRequestProperties(new HashMap<>(streams.getHeaders()));
        ProgressiveMediaSource.Factory progressive = new ProgressiveMediaSource.Factory(sourceFactory);
        MediaSource video = progressive.createMediaSource(item(streams.getVideoUrl(), MimeTypes.VIDEO_MP4));
        MediaSource source = video;
        if (!streams.getAudioUrl().isEmpty()) {
            MediaSource audio = progressive.createMediaSource(item(streams.getAudioUrl(), MimeTypes.AUDIO_MP4));
            source = new MergingMediaSource(true, true, video, audio);
        }
        Log.i(TAG, "open qn=" + streams.getSelectedQn() + " audio=" + !streams.getAudioUrl().isEmpty() + " host=" + host(streams.getVideoUrl()));
        player.setMediaSource(source, Math.max(0, positionMs));
        player.prepare();
        player.play();
    }

    public void seekBy(long deltaMs) {
        long duration = player.getDuration();
        long target = Math.max(0, player.getCurrentPosition() + deltaMs);
        if (duration > 0 && target > duration) target = duration;
        player.seekTo(target);
    }

    public void toggle() {
        if (player.isPlaying()) player.pause();
        else player.play();
    }

    public void stop() {
        player.stop();
    }

    public void release() {
        player.release();
    }

    private static MediaItem item(String url, String mime) {
        return new MediaItem.Builder().setUri(url).setMimeType(mime).build();
    }

    private static String host(String url) {
        String value = Uri.parse(url).getHost();
        return value == null ? "" : value;
    }
}
