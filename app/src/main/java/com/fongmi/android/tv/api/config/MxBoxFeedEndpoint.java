package com.fongmi.android.tv.api.config;

import android.net.Uri;
import android.text.TextUtils;

import com.github.catvod.utils.Prefers;

import java.util.Locale;

/** 手机网页写入的 feed 地址。只填主机时补默认路径，完整 JSON 地址原样使用。 */
public final class MxBoxFeedEndpoint {

    public static final String DEFAULT_PATH = "/api/v1/tvbox/feed.json";
    private static final String PREF_FEED_ENDPOINT = "mx_feed_endpoint";

    private MxBoxFeedEndpoint() {
    }

    public static String get() {
        return Prefers.getString(PREF_FEED_ENDPOINT, "");
    }

    public static String save(String input) {
        String url = resolve(input);
        if (TextUtils.isEmpty(url)) return "";
        Prefers.put(PREF_FEED_ENDPOINT, url);
        MxBoxSourceCatalog.refreshFeedAsync();
        return url;
    }

    public static String resolve(String input) {
        if (input == null) return "";
        String raw = input.trim();
        if (raw.isEmpty()) return "";
        String withScheme = raw.contains("://") ? raw : "http://" + raw;
        Uri uri = Uri.parse(withScheme);
        if (TextUtils.isEmpty(uri.getHost())) return "";
        String path = uri.getPath();
        if (path != null && path.toLowerCase(Locale.US).endsWith(".json")) return withScheme;
        return originOf(uri) + DEFAULT_PATH;
    }

    private static String originOf(Uri uri) {
        String scheme = TextUtils.isEmpty(uri.getScheme()) ? "http" : uri.getScheme();
        int port = uri.getPort();
        if (port > 0) return scheme + "://" + uri.getHost() + ":" + port;
        return scheme + "://" + uri.getHost();
    }
}
