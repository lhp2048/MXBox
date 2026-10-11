package com.fongmi.android.tv.bili;

import com.github.catvod.utils.Json;
import com.github.catvod.utils.Prefers;
import com.google.gson.JsonObject;

public final class BiliRecommendMemory {

    private static final String KEY = "mx_bili_last_recommend";

    private BiliRecommendMemory() {
    }

    public static void save(BiliVideo video) {
        if (video == null || !video.isNormal()) return;
        JsonObject obj = new JsonObject();
        obj.addProperty("bvid", video.getBvid());
        obj.addProperty("title", video.getTitle());
        obj.addProperty("cover", video.getCover());
        obj.addProperty("mid", video.getMid());
        obj.addProperty("author", video.getAuthor());
        Prefers.put(KEY, obj.toString());
    }

    public static BiliVideo get() {
        try {
            String raw = Prefers.getString(KEY, "");
            if (raw == null || raw.isEmpty()) return null;
            JsonObject obj = Json.parse(raw).getAsJsonObject();
            BiliVideo video = new BiliVideo(text(obj, "bvid"), text(obj, "title"), text(obj, "cover"), number(obj, "mid"), text(obj, "author"));
            return video.isNormal() ? video : null;
        } catch (Exception e) {
            return null;
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
