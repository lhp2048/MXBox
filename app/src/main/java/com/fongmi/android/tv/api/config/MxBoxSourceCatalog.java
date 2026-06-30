package com.fongmi.android.tv.api.config;

import android.text.TextUtils;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.MxBoxBootstrap;
import com.fongmi.android.tv.MxBoxConstants;
import com.fongmi.android.tv.bean.Config;
import com.fongmi.android.tv.bean.Live;
import com.fongmi.android.tv.event.ConfigEvent;
import com.fongmi.android.tv.impl.Callback;
import com.fongmi.android.tv.utils.Task;
import com.fongmi.android.tv.utils.UrlUtil;
import com.github.catvod.net.OkHttp;
import com.google.gson.annotations.SerializedName;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class MxBoxSourceCatalog {

    private static final int VOD = 0;
    private static final int LIVE = 1;

    private static volatile List<SourceEntry> builtIn;

    private MxBoxSourceCatalog() {
    }

    /** 注册 assets 内置 urls；远程 feed 地址本身不写入 Config。 */
    public static void ensureBuiltIns() {
        MxBoxFeedStore.restore();
        for (SourceEntry source : getBuiltIn()) {
            if (source.isRemoteFeed()) continue;
            Config.find(source.url, source.name, VOD).save();
        }
        purgeRemoteFeedFromConfig();
    }

    /** 异步拉取 feed：解析 urls 点播列表 + lives 直播合并。 */
    public static void refreshFeedAsync() {
        List<SourceEntry> feeds = getRemoteFeeds();
        if (feeds.isEmpty()) return;
        Task.submit(() -> {
            boolean changed = false;
            for (SourceEntry feed : feeds) {
                changed |= refreshRemoteFeed(feed);
            }
            if (changed) App.post(ConfigEvent::common);
        });
    }

    /** @deprecated use {@link #refreshFeedAsync()} */
    @Deprecated
    public static void refreshDepotFeedsAsync() {
        refreshFeedAsync();
    }

    /** Logo 源中心：feed 点播 + 用户手动添加 + 内置 TEST（置底）；feed 直播仅合并到直播页。 */
    public static List<Config> getPickerSources() {
        ensureBuiltIns();
        List<Config> items = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (MxBoxDepotFeed.VodEntry entry : MxBoxFeedStore.getVods()) {
            if (entry == null || TextUtils.isEmpty(entry.url)) continue;
            Config item = Config.find(entry.url, entry.name, VOD).save();
            if (seen.add(VOD + "|" + item.getUrl())) items.add(item);
        }
        for (Config item : MxBoxUserSourceStore.getAll()) {
            if (seen.add(item.getType() + "|" + item.getUrl())) items.add(item);
        }
        for (SourceEntry source : getBuiltIn()) {
            if (source.isRemoteFeed()) continue;
            Config item = Config.find(source.url, source.name, VOD).save();
            if (seen.add(VOD + "|" + item.getUrl())) items.add(item);
        }
        return items;
    }

    public static List<Config> getSources() {
        return getPickerSources();
    }

    public static List<Config> getVodSources() {
        ensureBuiltIns();
        List<Config> items = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (MxBoxDepotFeed.VodEntry entry : MxBoxFeedStore.getVods()) {
            if (entry == null || TextUtils.isEmpty(entry.url)) continue;
            Config item = Config.find(entry.url, entry.name, VOD);
            if (seen.add(item.getUrl())) items.add(item);
        }
        for (Config item : MxBoxUserSourceStore.getConfigs(VOD)) {
            if (seen.add(item.getUrl())) items.add(item);
        }
        for (SourceEntry source : getBuiltIn()) {
            if (source.isRemoteFeed()) continue;
            Config item = Config.find(source.url, source.name, VOD);
            if (seen.add(item.getUrl())) items.add(item);
        }
        return items;
    }

    public static boolean isBuiltIn(String url) {
        if (url == null) return false;
        for (SourceEntry source : getBuiltIn()) {
            if (!source.isRemoteFeed() && url.equals(source.url)) return true;
        }
        return false;
    }

    public static boolean isFeedEndpoint(String url) {
        return getRemoteFeedUrls().contains(url);
    }

    /** @deprecated use {@link #isFeedEndpoint(String)} */
    @Deprecated
    public static boolean isDepotFeed(String url) {
        return isFeedEndpoint(url);
    }

    public static String getTypeLabel(int type) {
        return type == LIVE ? "直播" : "点播";
    }

    /** Logo 源列表/标题：优先 feed urls[].name，其次 Config.name，最后 url。 */
    public static String getDisplayName(Config config) {
        if (config == null || config.isEmpty()) return "";
        if (config.getType() == VOD) {
            for (MxBoxDepotFeed.VodEntry entry : MxBoxFeedStore.getVods()) {
                if (entry == null || TextUtils.isEmpty(entry.url)) continue;
                if (config.getUrl().equals(entry.url) && !TextUtils.isEmpty(entry.name)) return entry.name;
            }
        } else if (config.getType() == LIVE) {
            for (Live live : MxBoxFeedStore.getLives()) {
                if (live == null || live.isEmpty() || TextUtils.isEmpty(live.getUrl())) continue;
                if (config.getUrl().equals(live.getUrl()) && !TextUtils.isEmpty(live.getName())) return live.getName();
            }
        }
        for (SourceEntry source : getBuiltIn()) {
            if (source.isRemoteFeed()) continue;
            if (config.getUrl().equals(source.url) && !TextUtils.isEmpty(source.name)) return source.name;
        }
        return config.getDesc();
    }

    private static boolean refreshRemoteFeed(SourceEntry feed) {
        try {
            MxBoxDepotFeed.FeedResult result = MxBoxDepotFeed.parse(fetchFeedJson(feed.url));
            Set<String> previous = new LinkedHashSet<>(MxBoxFeedStore.getFeedVodUrls());
            MxBoxFeedStore.apply(result);
            syncFeedVodConfigs(result, previous);
            purgeRemoteFeedFromConfig(feed.url);
            applyDefaultFeedVod(result.vods);
            reloadLiveMerge();
            return !result.isEmpty();
        } catch (Throwable e) {
            e.printStackTrace();
            return false;
        }
    }

    private static void syncFeedVodConfigs(MxBoxDepotFeed.FeedResult result, Set<String> previous) {
        Set<String> current = new LinkedHashSet<>();
        for (MxBoxDepotFeed.VodEntry entry : result.vods) {
            if (entry == null || TextUtils.isEmpty(entry.url)) continue;
            current.add(entry.url);
            Config.find(entry.url, entry.name, VOD).save();
        }
        for (String url : previous) {
            if (current.contains(url) || isBuiltIn(url)) continue;
            if (MxBoxConstants.DEFAULT_CONFIG_URL.equals(url)) continue;
            Config.delete(url, VOD);
        }
    }

    private static void applyDefaultFeedVod(List<MxBoxDepotFeed.VodEntry> vods) {
        if (vods.isEmpty() || MxBoxBootstrap.isUserConfigured()) return;
        Config current = Config.vod();
        if (!current.isEmpty() && !MxBoxConstants.DEFAULT_CONFIG_URL.equals(current.getUrl())) return;
        MxBoxDepotFeed.VodEntry first = vods.get(0);
        Config config = Config.find(first.url, first.name, VOD);
        config.save();
        VodConfig.load(config, new Callback());
    }

    private static void reloadLiveMerge() {
        Config vod = VodConfig.get().getConfig();
        if (vod.isEmpty()) return;
        try {
            VodConfig.get().reloadLiveMerge();
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    private static String fetchFeedJson(String url) throws Exception {
        if (TextUtils.isEmpty(url)) throw new Exception("empty feed url");
        String scheme = UrlUtil.scheme(url.trim());
        if ("assets".equals(scheme)) return readAsset(url.trim());
        if ("file".equals(scheme)) throw new Exception("file feed is unsupported");
        return OkHttp.string(url.trim());
    }

    private static String readAsset(String url) throws Exception {
        int index = url.indexOf("://");
        if (index < 0) throw new Exception("invalid assets url");
        String path = url.substring(index + 3).trim();
        if (TextUtils.isEmpty(path)) throw new Exception("invalid assets url");
        try (InputStream in = App.get().getAssets().open(path);
             BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
            return sb.toString();
        }
    }

    private static void purgeRemoteFeedFromConfig() {
        for (String url : getRemoteFeedUrls()) purgeRemoteFeedFromConfig(url);
    }

    private static void purgeRemoteFeedFromConfig(String url) {
        Config.delete(url, VOD);
        Config.delete(url, LIVE);
    }

    private static List<SourceEntry> getRemoteFeeds() {
        List<SourceEntry> items = new ArrayList<>();
        for (SourceEntry source : getBuiltIn()) {
            if (source.isRemoteFeed()) items.add(source);
        }
        return items;
    }

    private static Set<String> getRemoteFeedUrls() {
        Set<String> urls = new HashSet<>();
        for (SourceEntry source : getRemoteFeeds()) urls.add(source.url);
        return urls;
    }

    private static List<SourceEntry> getBuiltIn() {
        if (builtIn != null) return builtIn;
        builtIn = loadBuiltIn();
        return builtIn;
    }

    private static List<SourceEntry> loadBuiltIn() {
        try (InputStream in = App.get().getAssets().open(MxBoxConstants.BUILT_IN_SOURCES_ASSET);
             BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
            BuiltInRoot root = App.gson().fromJson(sb.toString(), BuiltInRoot.class);
            if (root == null) return Collections.emptyList();
            List<SourceEntry> items = new ArrayList<>();
            if (!TextUtils.isEmpty(root.feed)) {
                items.add(SourceEntry.remoteFeed(root.feed.trim()));
            }
            if (root.urls != null) {
                for (FeedUrl item : root.urls) {
                    if (item == null || TextUtils.isEmpty(item.url)) continue;
                    items.add(SourceEntry.localVod(item.name, item.url.trim()));
                }
            }
            return items;
        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    /** assets/built_in_sources.json：feed + 本地 urls。 */
    private static final class BuiltInRoot {
        @SerializedName("feed")
        private String feed;
        @SerializedName("urls")
        private List<FeedUrl> urls;
    }

    private static final class FeedUrl {
        @SerializedName("name")
        private String name;
        @SerializedName("url")
        private String url;
    }

    private static final class SourceEntry {
        private final boolean remoteFeed;
        private final String name;
        private final String url;

        private SourceEntry(boolean remoteFeed, String name, String url) {
            this.remoteFeed = remoteFeed;
            this.name = name == null ? "" : name;
            this.url = url;
        }

        private static SourceEntry remoteFeed(String url) {
            return new SourceEntry(true, "", url);
        }

        private static SourceEntry localVod(String name, String url) {
            return new SourceEntry(false, name, url);
        }

        private boolean isRemoteFeed() {
            return remoteFeed;
        }
    }
}
