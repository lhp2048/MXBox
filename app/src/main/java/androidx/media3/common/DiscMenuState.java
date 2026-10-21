package androidx.media3.common;

public final class DiscMenuState {

    public static final int AVAILABILITY_AVAILABLE = 1;
    public static final int PHASE_IDLE = 0;
    public static final int PHASE_OPENING = 1;
    public static final int PHASE_ACTIVE = 2;
    public static final DiscMenuState EMPTY = new DiscMenuState();

    public final int availability;
    public final int phase;
    public final boolean navigationPlaybackOngoing;

    public DiscMenuState() {
        this.availability = 0;
        this.phase = PHASE_IDLE;
        this.navigationPlaybackOngoing = false;
    }
}
