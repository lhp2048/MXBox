package androidx.media3.exoplayer.trackselection;

import android.content.Context;

public final class SecondaryTextTrackSelector {

    private SecondaryTextTrackSelector() {
    }

    public static final class Factory implements TrackSelector.Factory {

        private final TrackSelector.Factory factory;

        public Factory(TrackSelector.Factory factory) {
            this.factory = factory;
        }

        @Override
        public TrackSelector createTrackSelector(Context context) {
            return factory.createTrackSelector(context);
        }
    }
}
