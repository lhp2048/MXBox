package androidx.media3.effect;

import androidx.media3.common.Effect;

public final class VideoAdjustment {

    public final float saturation;
    public final float contrast;
    public final float brightness;
    public final float gamma;
    public final float hue;
    public final float redGain;
    public final float blueGain;
    public final float sharpness;
    public final float threshold;
    public final float shadowLift;

    private VideoAdjustment(Builder builder) {
        this.saturation = builder.saturation;
        this.contrast = builder.contrast;
        this.brightness = builder.brightness;
        this.gamma = builder.gamma;
        this.hue = builder.hue;
        this.redGain = builder.redGain;
        this.blueGain = builder.blueGain;
        this.sharpness = builder.sharpness;
        this.threshold = builder.threshold;
        this.shadowLift = builder.shadowLift;
    }

    public static final class Builder {
        private float saturation = 1.0f;
        private float contrast = 1.0f;
        private float brightness;
        private float gamma = 1.0f;
        private float hue;
        private float redGain = 1.0f;
        private float blueGain = 1.0f;
        private float sharpness;
        private float threshold;
        private float shadowLift;

        public Builder setSaturation(float value) {
            saturation = value;
            return this;
        }

        public Builder setContrast(float value) {
            contrast = value;
            return this;
        }

        public Builder setBrightness(float value) {
            brightness = value;
            return this;
        }

        public Builder setGamma(float value) {
            gamma = value;
            return this;
        }

        public Builder setHue(float value) {
            hue = value;
            return this;
        }

        public Builder setRedGain(float value) {
            redGain = value;
            return this;
        }

        public Builder setBlueGain(float value) {
            blueGain = value;
            return this;
        }

        public Builder setSharpness(float value) {
            sharpness = value;
            return this;
        }

        public Builder setThreshold(float value) {
            threshold = value;
            return this;
        }

        public Builder setShadowLift(float value) {
            shadowLift = value;
            return this;
        }

        public VideoAdjustment build() {
            return new VideoAdjustment(this);
        }
    }
}
