package androidx.media3.exoplayer;

import android.content.Context;

import androidx.media3.exoplayer.trackselection.DecodeTrackSelector;

public final class DecoderManager {

    private DecoderMode audioMode;
    private DecoderMode videoMode;

    public DecoderManager(DecoderMode audioMode, DecoderMode videoMode) {
        this.audioMode = audioMode == null ? DecoderMode.AUTO : audioMode;
        this.videoMode = videoMode == null ? DecoderMode.AUTO : videoMode;
    }

    public DecodeTrackSelector createTrackSelector(Context context) {
        return new DecodeTrackSelector(context);
    }

    public void attach(ExoPlayer player) {
    }

    public void detach() {
    }

    public DecoderMode getAudioMode() {
        return audioMode;
    }

    public DecoderMode getVideoMode() {
        return videoMode;
    }

    public void selectAudioDecoder(DecoderMode mode) {
        audioMode = mode == null ? DecoderMode.AUTO : mode;
    }

    public void selectVideoDecoder(DecoderMode mode) {
        videoMode = mode == null ? DecoderMode.AUTO : mode;
    }

    public void setDecoderModes(DecoderMode audioMode, DecoderMode videoMode) {
        selectAudioDecoder(audioMode);
        selectVideoDecoder(videoMode);
    }
}
