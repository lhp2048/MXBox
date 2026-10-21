package androidx.media3.common.audio;

public final class AudioChannelMode {

    public static final int AUTO = 0;
    public static final int STEREO = 1;
    public static final int MONO = 2;
    public static final int REVERSE = 3;

    private AudioChannelMode() {
    }

    public static int clamp(int mode) {
        return mode >= AUTO && mode <= REVERSE ? mode : AUTO;
    }

    public static boolean isAuto(int mode) {
        return mode == AUTO;
    }

    public static boolean isAvailable(int channelCount) {
        return channelCount >= 2;
    }

    public static boolean isAvailable(int mode, int channelCount) {
        if (channelCount <= 0) return false;
        if (mode == STEREO || mode == REVERSE) return channelCount >= 2;
        return mode == AUTO || mode == MONO;
    }

    public static boolean canBalance(int mode) {
        return mode == AUTO || mode == STEREO;
    }

    public static int resolve(int mode, int channelCount) {
        mode = clamp(mode);
        if (!isAvailable(mode, channelCount)) return channelCount >= 2 ? STEREO : MONO;
        if (mode == AUTO) return channelCount >= 2 ? STEREO : MONO;
        return mode;
    }
}
