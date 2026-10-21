package androidx.media3.datasource;

import android.net.Uri;

import androidx.media3.datasource.cache.Cache;
import androidx.media3.datasource.cache.CacheDataSource;

public final class ProgressiveIsoCache {

    private ProgressiveIsoCache() {
    }

    public static DataSource.Factory withDiskCache(CacheDataSource.Factory factory, Cache cache) {
        return factory;
    }
}
