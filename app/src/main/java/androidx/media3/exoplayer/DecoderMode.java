package androidx.media3.exoplayer;

public enum DecoderMode {
    AUTO,
    HARDWARE,
    SOFTWARE,
    FFMPEG;

    public static DecoderMode fromCommonDecoderMode(androidx.media3.common.DecoderMode mode) {
        return mode == null ? AUTO : valueOf(mode.name());
    }

    public androidx.media3.common.DecoderMode toCommonDecoderMode() {
        return androidx.media3.common.DecoderMode.valueOf(name());
    }
}
