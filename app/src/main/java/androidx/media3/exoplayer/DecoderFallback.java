package androidx.media3.exoplayer;

public final class DecoderFallback {

    public DecoderFallback(DecoderManager decoderManager) {
    }

    public void reset(int trackType) {
    }

    public boolean retry(ExoPlaybackException error) {
        return false;
    }
}
