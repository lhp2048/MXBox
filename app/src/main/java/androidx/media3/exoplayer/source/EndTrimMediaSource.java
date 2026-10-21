package androidx.media3.exoplayer.source;

public final class EndTrimMediaSource extends WrappingMediaSource {

    public EndTrimMediaSource(MediaSource source, long endTrimDurationMs) {
        super(source);
    }

    public void setEndTrimDurationMs(long endTrimDurationMs) {
    }

    public boolean isEndTrimActive() {
        return false;
    }

    public long getOriginalDurationMs() {
        return androidx.media3.common.C.TIME_UNSET;
    }
}
