package androidx.media3.mpvplayer;

import android.net.Uri;

import androidx.media3.common.Format;
import androidx.media3.common.MediaItem;
import androidx.media3.common.text.SubtitleContent;

import java.io.IOException;

public interface MpvSubtitleContentLoader {
    SubtitleContent load(MediaItem item, Uri uri, Format format) throws IOException;
}
