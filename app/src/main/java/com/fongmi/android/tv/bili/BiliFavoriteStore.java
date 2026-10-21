package com.fongmi.android.tv.bili;

import com.github.catvod.utils.Prefers;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

public final class BiliFavoriteStore {

    private static final String KEY = "mx_bili_favorites";

    private BiliFavoriteStore() {
    }

    public static List<BiliVideo> all() {
        List<BiliVideo> items = new ArrayList<>();
        JsonArray array = array();
        for (int i = 0; i < array.size(); i++) {
            if (!array.get(i).isJsonObject()) continue;
            JsonObject obj = array.get(i).getAsJsonObject();
            items.add(new BiliVideo(text(obj, "bvid"), text(obj, "title"), text(obj, "cover"), number(obj, "mid"), text(obj, "author")));
        }
        return items;
    }

    public static BiliVideoPage page(int page) {
        List<BiliVideo> all = all();
        int from = Math.max(0, (page - 1) * BiliVideoPage.PAGE_SIZE);
        if (from >= all.size()) return BiliVideoPage.empty();
        int to = Math.min(all.size(), from + BiliVideoPage.PAGE_SIZE);
        boolean hasMore = to < all.size();
        return new BiliVideoPage(new ArrayList<>(all.subList(from, to)), hasMore, page + 1, "");
    }

    public static boolean isFavorite(String bvid) {
        for (BiliVideo video : all()) if (video.getBvid().equals(bvid)) return true;
        return false;
    }

    public static boolean toggle(BiliVideo video) {
        JsonArray array = array();
        JsonArray next = new JsonArray();
        boolean removed = false;
        for (int i = 0; i < array.size(); i++) {
            if (!array.get(i).isJsonObject()) continue;
            JsonObject obj = array.get(i).getAsJsonObject();
            if (video.getBvid().equals(text(obj, "bvid"))) {
                removed = true;
                continue;
            }
            next.add(obj);
        }
        if (removed) {
            Prefers.put(KEY, next.toString());
            return false;
        }
        JsonArray saved = new JsonArray();
        saved.add(toJson(video));
        saved.addAll(next);
        Prefers.put(KEY, saved.toString());
        return true;
    }

    private static JsonObject toJson(BiliVideo video) {
        JsonObject obj = new JsonObject();
        obj.addProperty("bvid", video.getBvid());
        obj.addProperty("title", video.getTitle());
        obj.addProperty("cover", video.getCover());
        obj.addProperty("mid", video.getMid());
        obj.addProperty("author", video.getAuthor());
        return obj;
    }

    private static JsonArray array() {
        try {
            String raw = Prefers.getString(KEY, "[]");
            if (raw == null || raw.isEmpty()) return new JsonArray();
            return com.github.catvod.utils.Json.parse(raw).getAsJsonArray();
        } catch (Exception e) {
            return new JsonArray();
        }
    }

    private static String text(JsonObject obj, String key) {
        if (!obj.has(key) || obj.get(key).isJsonNull()) return "";
        return obj.get(key).getAsString();
    }

    private static long number(JsonObject obj, String key) {
        try {
            if (!obj.has(key) || obj.get(key).isJsonNull()) return 0;
            return obj.get(key).getAsLong();
        } catch (Exception e) {
            return 0;
        }
    }
}
