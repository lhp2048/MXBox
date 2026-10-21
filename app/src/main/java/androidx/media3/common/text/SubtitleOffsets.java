package androidx.media3.common.text;

import java.util.Objects;

public final class SubtitleOffsets {

    public static final SubtitleOffsets ZERO = new SubtitleOffsets(0, 0);

    public final long primaryMs;
    public final long secondaryMs;

    public SubtitleOffsets(long primaryMs, long secondaryMs) {
        this.primaryMs = primaryMs;
        this.secondaryMs = secondaryMs;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof SubtitleOffsets offsets)) return false;
        return primaryMs == offsets.primaryMs && secondaryMs == offsets.secondaryMs;
    }

    @Override
    public int hashCode() {
        return Objects.hash(primaryMs, secondaryMs);
    }
}
