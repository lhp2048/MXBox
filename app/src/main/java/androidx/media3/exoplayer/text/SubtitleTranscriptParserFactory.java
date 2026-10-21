package androidx.media3.exoplayer.text;

import androidx.media3.common.Format;
import androidx.media3.extractor.text.SubtitleParser;

public final class SubtitleTranscriptParserFactory implements SubtitleParser.Factory {

    public SubtitleTranscriptParserFactory(SubtitleTranscript transcript) {
    }

    @Override
    public boolean supportsFormat(Format format) {
        return false;
    }

    @Override
    public int getCueReplacementBehavior(Format format) {
        return 1;
    }

    @Override
    public SubtitleParser create(Format format) {
        throw new UnsupportedOperationException();
    }
}
