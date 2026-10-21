package androidx.media3.exoplayer.text;

import android.net.Uri;

import androidx.media3.common.Format;
import androidx.media3.common.MediaItem;
import androidx.media3.common.text.SubtitleContent;
import androidx.media3.datasource.DataSource;

import java.io.IOException;

public final class DefaultSubtitleContentLoader {

    public DefaultSubtitleContentLoader(DataSource.Factory factory) {
    }

    public SubtitleContent load(MediaItem item, Uri uri, Format format) throws IOException {
        return SubtitleContent.UNSUPPORTED;
    }
}
