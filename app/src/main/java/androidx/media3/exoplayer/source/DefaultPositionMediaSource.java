package androidx.media3.exoplayer.source;

public final class DefaultPositionMediaSource extends WrappingMediaSource {

    public DefaultPositionMediaSource(MediaSource source, long startPositionMs) {
        super(source);
    }
}
