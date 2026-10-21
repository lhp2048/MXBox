package androidx.media3.exoplayer.iso;

import android.content.Context;
import android.os.CancellationSignal;

import androidx.media3.common.MediaItem;

public final class IsoDiscPreparation {

    private IsoDiscPreparation() {
    }

    public static boolean isIso(MediaItem item) {
        return false;
    }
}
