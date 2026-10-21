package androidx.media3.mpvplayer.media;

import androidx.media3.common.MediaItem;

public final class MpvDrmSupport {

    private MpvDrmSupport() {
    }

    public static boolean supports(MediaItem item) {
        return false;
    }
}
