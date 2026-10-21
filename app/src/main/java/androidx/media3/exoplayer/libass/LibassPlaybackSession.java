package androidx.media3.exoplayer.libass;

import androidx.media3.common.MediaItem;
import androidx.media3.exoplayer.Renderer;
import androidx.media3.exoplayer.text.SubtitleTranscript;
import androidx.media3.extractor.ExtractorsFactory;
import androidx.media3.extractor.text.SubtitleParser;

public final class LibassPlaybackSession {

    public static final class MediaComponents {
        public final ExtractorsFactory extractorsFactory;
        public final SubtitleParser.Factory subtitleParserFactory;

        public MediaComponents(ExtractorsFactory extractorsFactory, SubtitleParser.Factory subtitleParserFactory) {
            this.extractorsFactory = extractorsFactory;
            this.subtitleParserFactory = subtitleParserFactory;
        }
    }

    public LibassPlaybackSession(LibassConfiguration configuration, boolean enabled) {
    }

    public boolean isAvailable() {
        return false;
    }

    public void setPreloadMediaItem(MediaItem mediaItem) {
    }

    public void close() {
    }

    public Renderer createClockRenderer() {
        throw new UnsupportedOperationException();
    }

    public MediaComponents createMediaComponents(MediaItem mediaItem, ExtractorsFactory extractorsFactory, SubtitleTranscript transcript) {
        return new MediaComponents(extractorsFactory, null);
    }

    public void setBottomPositionFraction(float fraction) {
    }

    public void setSecondaryBottomPositionFraction(float fraction) {
    }

    public void setFontScale(float scale, boolean apply) {
    }
}
