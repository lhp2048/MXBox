package androidx.media3.common.util;

import androidx.media3.common.MediaItem;

import java.util.Collections;
import java.util.List;

public final class LocalClearKeyLicense {

    private LocalClearKeyLicense() {
    }

    public static List<String> parse(String key) {
        return Collections.emptyList();
    }

    public static List<String> parse(MediaItem.DrmConfiguration configuration) {
        return Collections.emptyList();
    }
}
