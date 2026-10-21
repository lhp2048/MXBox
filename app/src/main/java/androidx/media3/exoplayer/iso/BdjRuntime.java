package androidx.media3.exoplayer.iso;

import android.content.Context;
import android.os.CancellationSignal;

public final class BdjRuntime {

    public static final class Status {
        public final boolean restartRequired;

        public Status(boolean restartRequired) {
            this.restartRequired = restartRequired;
        }
    }

    private BdjRuntime() {
    }

    public static void prepare(Context context, CancellationSignal signal) {
    }

    public static Status getStatus(Context context) {
        return new Status(false);
    }
}
