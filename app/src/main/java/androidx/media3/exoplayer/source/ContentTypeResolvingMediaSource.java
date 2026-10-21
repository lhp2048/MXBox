package androidx.media3.exoplayer.source;

public final class ContentTypeResolvingMediaSource {

    private ContentTypeResolvingMediaSource() {
    }

    public static final class Factory implements MediaSource.Factory {

        private final MediaSource.Factory factory;

        public Factory(MediaSource.Factory factory, androidx.media3.datasource.DataSource.Factory dataSourceFactory) {
            this.factory = factory;
        }

        @Override
        public MediaSource.Factory setDrmSessionManagerProvider(androidx.media3.exoplayer.drm.DrmSessionManagerProvider drmSessionManagerProvider) {
            factory.setDrmSessionManagerProvider(drmSessionManagerProvider);
            return this;
        }

        @Override
        public MediaSource.Factory setLoadErrorHandlingPolicy(androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy loadErrorHandlingPolicy) {
            factory.setLoadErrorHandlingPolicy(loadErrorHandlingPolicy);
            return this;
        }

        @Override
        public int[] getSupportedTypes() {
            return factory.getSupportedTypes();
        }

        @Override
        public MediaSource createMediaSource(androidx.media3.common.MediaItem mediaItem) {
            return factory.createMediaSource(mediaItem);
        }
    }
}
