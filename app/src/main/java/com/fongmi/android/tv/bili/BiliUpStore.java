package com.fongmi.android.tv.bili;

import com.github.catvod.utils.Prefers;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

public final class BiliUpStore {

    private static final String KEY = "mx_bili_ups";

    private BiliUpStore() {
    }

    public static List<BiliUp> all() {
        List<BiliUp> items = new ArrayList<>();
        JsonArray array = array();
        for (int i = 0; i < array.size(); i++) {
            if (!array.get(i).isJsonObject()) continue;
            JsonObject obj = array.get(i).getAsJsonObject();
            items.add(new BiliUp(number(obj, "mid"), text(obj, "name"), text(obj, "face")));
        }
        return items;
    }

    public static BiliUp find(long mid) {
        if (mid <= 0) return null;
        for (BiliUp up : all()) if (up.getMid() == mid) return up;
        return null;
    }

    public static boolean isFavorite(long mid) {
        return find(mid) != null;
    }

    public static boolean toggle(BiliUp up) {
        if (up == null || up.getMid() <= 0) return false;
        JsonArray array = array();
        JsonArray next = new JsonArray();
        boolean removed = false;
        for (int i = 0; i < array.size(); i++) {
            if (!array.get(i).isJsonObject()) continue;
            JsonObject obj = array.get(i).getAsJsonObject();
            if (number(obj, "mid") == up.getMid()) {
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
        saved.add(toJson(up));
        saved.addAll(next);
        Prefers.put(KEY, saved.toString());
        return true;
    }

    public static void remember(BiliUp up) {
        if (up == null || up.getMid() <= 0) return;
        JsonArray array = array();
        boolean changed = false;
        for (int i = 0; i < array.size(); i++) {
            if (!array.get(i).isJsonObject()) continue;
            JsonObject obj = array.get(i).getAsJsonObject();
            if (number(obj, "mid") != up.getMid()) continue;
            if (!up.getName().isEmpty() && !up.getName().equals(text(obj, "name"))) {
                obj.addProperty("name", up.getName());
                changed = true;
            }
            if (!up.getFace().isEmpty() && !up.getFace().equals(text(obj, "face"))) {
                obj.addProperty("face", up.getFace());
                changed = true;
            }
        }
        if (changed) Prefers.put(KEY, array.toString());
    }

    private static JsonObject toJson(BiliUp up) {
        JsonObject obj = new JsonObject();
        obj.addProperty("mid", up.getMid());
        obj.addProperty("name", up.getName());
        obj.addProperty("face", up.getFace());
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
