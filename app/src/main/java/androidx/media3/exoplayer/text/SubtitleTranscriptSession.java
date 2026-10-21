package androidx.media3.exoplayer.text;

import androidx.media3.common.Format;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.common.text.SubtitleContent;
import androidx.media3.mpvplayer.MpvSubtitleContentLoader;

public final class SubtitleTranscriptSession {

    public void setLoader(MpvSubtitleContentLoader loader) {
    }

    public void setPlayer(Player player) {
    }

    public void setPreloadMediaItem(MediaItem mediaItem) {
    }

    public void close() {
    }

    public SubtitleTranscript forMediaItem(MediaItem mediaItem) {
        return SubtitleTranscript.empty();
    }

    public SubtitleContent getContent(Format format, long offsetMs) {
        return SubtitleContent.UNSUPPORTED;
    }

    public void setContentEnabled(boolean enabled) {
    }

    public boolean canRetryContent(Format format) {
        return false;
    }

    public void retryContent(Format format) {
    }
}
