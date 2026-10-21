package com.fongmi.android.tv.api.config;

import android.text.TextUtils;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.bean.Live;
import com.fongmi.android.tv.utils.UrlUtil;
import com.github.catvod.utils.Prefers;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** 缓存 feed 解析结果：点播 url 索引 + 合并用直播源。 */
public final class MxBoxFeedStore {

    private static final String PREF_FEED_VOD_URLS = "mx_feed_vod_urls";
    private static final String PREF_FEED_VODS = "mx_feed_vods";

    private static volatile List<MxBoxDepotFeed.VodEntry> vods = Collections.emptyList();
    private static volatile List<Live> lives = Collections.emptyList();
    private static volatile Set<String> feedVodUrls = Collections.emptySet();

    private MxBoxFeedStore() {
    }

    public static List<MxBoxDepotFeed.VodEntry> getVods() {
        if (!vods.isEmpty()) return vods;
        List<MxBoxDepotFeed.VodEntry> items = new ArrayList<>();
        for (String url : feedVodUrls) {
            if (TextUtils.isEmpty(url)) continue;
            items.add(new MxBoxDepotFeed.VodEntry(UrlUtil.getName(url), url));
        }
        return items;
    }

    public static List<Live> getLives() {
        return lives;
    }

    public static boolean hasLives() {
        return !lives.isEmpty();
    }

    public static Set<String> getFeedVodUrls() {
        return feedVodUrls;
    }

    public static void apply(MxBoxDepotFeed.FeedResult result) {
        vods = copyVods(result.vods);
        lives = copyLives(result.lives);
        LinkedHashSet<String> urls = new LinkedHashSet<>();
        for (MxBoxDepotFeed.VodEntry entry : vods) urls.add(entry.url);
        feedVodUrls = urls;
        Prefers.put(PREF_FEED_VOD_URLS, App.gson().toJson(new ArrayList<>(urls)));
        Prefers.put(PREF_FEED_VODS, App.gson().toJson(toSnapshots(vods)));
    }

    public static void restore() {
        String json = Prefers.getString(PREF_FEED_VOD_URLS);
        Type type = new TypeToken<List<String>>() {}.getType();
        List<String> urls = App.gson().fromJson(json, type);
        feedVodUrls = urls == null ? Collections.emptySet() : new LinkedHashSet<>(urls);
        vods = restoreVods();
    }

    public static boolean isFeedVod(String url) {
        return url != null && feedVodUrls.contains(url);
    }

    public static List<Live> copyLives(List<Live> source) {
        if (source == null || source.isEmpty()) return Collections.emptyList();
        List<Live> items = new ArrayList<>();
        for (Live live : source) {
            if (live == null || live.isEmpty()) continue;
            items.add(live.sync());
        }
        return items;
    }

    private static List<MxBoxDepotFeed.VodEntry> copyVods(List<MxBoxDepotFeed.VodEntry> source) {
        if (source == null || source.isEmpty()) return Collections.emptyList();
        List<MxBoxDepotFeed.VodEntry> items = new ArrayList<>();
        for (MxBoxDepotFeed.VodEntry entry : source) {
            if (entry == null || TextUtils.isEmpty(entry.url)) continue;
            items.add(new MxBoxDepotFeed.VodEntry(entry.name, entry.url));
        }
        return items;
    }

    private static List<MxBoxDepotFeed.VodEntry> restoreVods() {
        String json = Prefers.getString(PREF_FEED_VODS);
        if (TextUtils.isEmpty(json)) return Collections.emptyList();
        Type type = new TypeToken<List<VodSnapshot>>() {}.getType();
        List<VodSnapshot> snapshots = App.gson().fromJson(json, type);
        if (snapshots == null || snapshots.isEmpty()) return Collections.emptyList();
        List<MxBoxDepotFeed.VodEntry> items = new ArrayList<>();
        for (VodSnapshot snapshot : snapshots) {
            if (snapshot == null || TextUtils.isEmpty(snapshot.url)) continue;
            items.add(new MxBoxDepotFeed.VodEntry(snapshot.name, snapshot.url));
        }
        return items;
    }

    private static List<VodSnapshot> toSnapshots(List<MxBoxDepotFeed.VodEntry> source) {
        List<VodSnapshot> items = new ArrayList<>();
        for (MxBoxDepotFeed.VodEntry entry : source) {
            if (entry == null || TextUtils.isEmpty(entry.url)) continue;
            VodSnapshot snapshot = new VodSnapshot();
            snapshot.name = entry.name;
            snapshot.url = entry.url;
            items.add(snapshot);
        }
        return items;
    }

    private static final class VodSnapshot {
        private String name;
        private String url;
    }
}
