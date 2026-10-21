package androidx.media3.exoplayer.libass;

public final class LibassConfiguration {

    public final String fontConfig;
    public final String fontsDirectory;
    public final String defaultFontFamily;

    private LibassConfiguration(Builder builder) {
        this.fontConfig = builder.fontConfig;
        this.fontsDirectory = builder.fontsDirectory;
        this.defaultFontFamily = builder.defaultFontFamily;
    }

    public static final class Builder {
        private String fontConfig;
        private String fontsDirectory;
        private String defaultFontFamily;

        public Builder setFontConfig(String fontConfig) {
            this.fontConfig = fontConfig;
            return this;
        }

        public Builder setFontsDirectory(String fontsDirectory) {
            this.fontsDirectory = fontsDirectory;
            return this;
        }

        public Builder setDefaultFontFamily(String defaultFontFamily) {
            this.defaultFontFamily = defaultFontFamily;
            return this;
        }

        public Builder setMaximumRenderPixels(int pixels) {
            return this;
        }

        public Builder setMaximumGlyphCount(int count) {
            return this;
        }

        public Builder setMaximumBitmapCacheSizeMb(int sizeMb) {
            return this;
        }

        public LibassConfiguration build() {
            return new LibassConfiguration(this);
        }
    }
}
