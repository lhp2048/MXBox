package androidx.media3.common.audio;

import java.util.Arrays;

public final class AudioEffectParameters {

    public static final AudioEffectParameters DEFAULT = new Builder().build();

    private final int[] frequencies;
    private final short[] levels;
    private final float stability;
    private final int boostMillibels;
    private final int preampMillibels;
    private final int centerGainMillibels;
    private final float balance;
    private final int channelMode;
    private final boolean loudnessEnabled;

    private AudioEffectParameters(Builder builder) {
        this.frequencies = builder.frequencies;
        this.levels = builder.levels;
        this.stability = builder.stability;
        this.boostMillibels = builder.boostMillibels;
        this.preampMillibels = builder.preampMillibels;
        this.centerGainMillibels = builder.centerGainMillibels;
        this.balance = builder.balance;
        this.channelMode = builder.channelMode;
        this.loudnessEnabled = builder.loudnessEnabled;
    }

    public static boolean isCenterGainAvailable(int channelCount) {
        return channelCount >= 6;
    }

    public boolean hasBands() {
        return levels.length > 0;
    }

    public boolean hasEffect() {
        return hasBands() || stability > 0 || boostMillibels != 0 || preampMillibels != 0 || centerGainMillibels != 0 || balance != 0 || channelMode != AudioChannelMode.AUTO || loudnessEnabled;
    }

    public boolean hasCenterGain(int channelCount) {
        return centerGainMillibels != 0 && isCenterGainAvailable(channelCount);
    }

    public boolean shouldLimitOutput(int channelCount) {
        return hasBands() || boostMillibels > 0 || preampMillibels > 0;
    }

    public boolean isLoudnessEnabled() {
        return loudnessEnabled;
    }

    public int getChannelMode() {
        return channelMode;
    }

    public float getBalance() {
        return balance;
    }

    public float getStability() {
        return stability;
    }

    public int getBoostMillibels() {
        return boostMillibels;
    }

    public int getPreampMillibels() {
        return preampMillibels;
    }

    public float getCenterGainFactor() {
        return (float) Math.pow(10.0, centerGainMillibels / 2000.0);
    }

    public short[] getEqualizerLevelsMillibels() {
        return Arrays.copyOf(levels, levels.length);
    }

    public static final class Builder {
        private int[] frequencies = new int[0];
        private short[] levels = new short[0];
        private float stability;
        private int boostMillibels;
        private int preampMillibels;
        private int centerGainMillibels;
        private float balance;
        private int channelMode = AudioChannelMode.AUTO;
        private boolean loudnessEnabled;

        public Builder setEqualizerBands(int[] frequencies, short[] levels) {
            this.frequencies = frequencies == null ? new int[0] : frequencies;
            this.levels = levels == null ? new short[0] : levels;
            return this;
        }

        public Builder setStability(float stability) {
            this.stability = stability;
            return this;
        }

        public Builder setBoostMillibels(int boostMillibels) {
            this.boostMillibels = boostMillibels;
            return this;
        }

        public Builder setPreampMillibels(int preampMillibels) {
            this.preampMillibels = preampMillibels;
            return this;
        }

        public Builder setCenterGainMillibels(int centerGainMillibels) {
            this.centerGainMillibels = centerGainMillibels;
            return this;
        }

        public Builder setBalance(float balance) {
            this.balance = balance;
            return this;
        }

        public Builder setChannelMode(int channelMode) {
            this.channelMode = channelMode;
            return this;
        }

        public Builder setLoudnessEnabled(boolean loudnessEnabled) {
            this.loudnessEnabled = loudnessEnabled;
            return this;
        }

        public AudioEffectParameters build() {
            return new AudioEffectParameters(this);
        }
    }
}
