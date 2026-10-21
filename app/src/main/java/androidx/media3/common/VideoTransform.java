package androidx.media3.common;

import java.util.Objects;

public final class VideoTransform {

    public static final float MAX_SCALE = 8.0f;
    public static final VideoTransform DEFAULT = new VideoTransform(1.0f, 1.0f, 0.0f, 0.0f);

    public final float scaleX;
    public final float scaleY;
    public final float panX;
    public final float panY;

    public VideoTransform(float scaleX, float scaleY, float panX, float panY) {
        this.scaleX = scaleX;
        this.scaleY = scaleY;
        this.panX = panX;
        this.panY = panY;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof VideoTransform transform)) return false;
        return scaleX == transform.scaleX && scaleY == transform.scaleY && panX == transform.panX && panY == transform.panY;
    }

    @Override
    public int hashCode() {
        return Objects.hash(scaleX, scaleY, panX, panY);
    }
}
