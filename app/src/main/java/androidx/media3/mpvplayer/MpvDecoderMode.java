package androidx.media3.mpvplayer;

import androidx.media3.common.C;

public enum MpvDecoderMode {
    AUTO,
    HARDWARE,
    SOFTWARE,
    FFMPEG;

    public static MpvDecoderMode fromCommonDecoderMode(androidx.media3.common.DecoderMode mode) {
        return mode == null ? AUTO : valueOf(mode.name());
    }

    public androidx.media3.common.DecoderMode toCommonDecoderMode() {
        return androidx.media3.common.DecoderMode.valueOf(name());
    }

    public int toDecodeConstant() {
        return this == SOFTWARE || this == FFMPEG ? C.DECODE_SOFTWARE : C.DECODE_HARDWARE;
    }
}
