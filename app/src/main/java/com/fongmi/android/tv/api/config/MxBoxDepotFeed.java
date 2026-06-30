package com.fongmi.android.tv.api.config;

import android.text.TextUtils;

import com.fongmi.android.tv.bean.Live;
import com.fongmi.android.tv.utils.UrlUtil;
import com.github.catvod.utils.Json;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** 解析 MXBox feed：{@code urls} 点播源列表 + {@code lives} 自定义直播结构。 */
public final class MxBoxDepotFeed {

    public static final class VodEntry {
        public final String name;
        public final String url;

        public VodEntry(String name, String url) {
            this.name = name == null ? "" : name;
            this.url = url;
        }
    }

    public static final class FeedResult {
        public final List<VodEntry> vods;
        public final List<Live> lives;

        public FeedResult(List<VodEntry> vods, List<Live> lives) {
            this.vods = vods == null ? Collections.emptyList() : vods;
            this.lives = lives == null ? Collections.emptyList() : lives;
        }

        public boolean isEmpty() {
            return vods.isEmpty() && lives.isEmpty();
        }
    }

    private MxBoxDepotFeed() {
    }

    public static FeedResult parse(String json) throws Exception {
        if (TextUtils.isEmpty(json)) throw new Exception("empty feed");
        JsonElement parsed = Json.parse(json);
        if (!parsed.isJsonObject()) throw new Exception("feed must be a json object");
        JsonObject object = parsed.getAsJsonObject();
        List<VodEntry> vods = parseUrls(object.get("urls"));
        List<Live> lives = object.has("lives")
                ? MxBoxFeedLives.parse(object.getAsJsonArray("lives"))
                : Collections.emptyList();
        return new FeedResult(vods, lives);
    }

    static List<VodEntry> parseUrls(JsonElement element) {
        if (element == null || !element.isJsonArray()) return Collections.emptyList();
        JsonArray array = element.getAsJsonArray();
        List<VodEntry> items = new ArrayList<>();
        for (JsonElement item : array) {
            if (item == null || !item.isJsonObject()) continue;
            JsonObject obj = item.getAsJsonObject();
            String url = Json.safeString(obj, "url");
            if (TextUtils.isEmpty(url)) continue;
            String name = Json.safeString(obj, "name");
            items.add(new VodEntry(pickName(name, url), url.trim()));
        }
        return items;
    }

    private static String pickName(String name, String url) {
        return TextUtils.isEmpty(name) ? UrlUtil.getName(url) : name.trim();
    }
}
