package androidx.media3.common.audio;

public final class AudioChannelMix {

    private AudioChannelMix() {
    }

    public static float[][] createFrontCenterGainMix(int channelCount, float gain) {
        return identity(channelCount);
    }

    public static float[][] createStereoMix(int channelCount, boolean reverse) {
        return identity(channelCount);
    }

    public static float[][] createMonoMix(int channelCount) {
        return identity(channelCount);
    }

    public static float[][] createFrontBalanceMix(int channelCount, float balance) {
        return identity(channelCount);
    }

    public static float[][] compose(float[][] current, float[][] next) {
        return next != null ? next : current;
    }

    private static float[][] identity(int channelCount) {
        int count = Math.max(channelCount, 1);
        float[][] mix = new float[count][count];
        for (int channel = 0; channel < count; channel++) mix[channel][channel] = 1.0f;
        return mix;
    }
}
