package com.fongmi.android.tv.home;

import com.github.catvod.utils.Prefers;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

public final class HomeButtons {

    private static final String ORDER = "mx_home_order";
    private static final String HIDDEN = "mx_home_hidden";
    private static final String[] DEFAULT = {"bili", "curtain", "vod", "live", "search", "keep", "push", "setting"};

    private HomeButtons() {
    }

    public static List<String> order() {
        LinkedHashSet<String> keys = new LinkedHashSet<>();
        for (String key : split(Prefers.getString(ORDER, ""))) if (label(key) != null) keys.add(key);
        for (String key : DEFAULT) keys.add(key);
        return new ArrayList<>(keys);
    }

    public static boolean hidden(String key) {
        for (String item : split(Prefers.getString(HIDDEN, ""))) if (item.equals(key)) return true;
        return false;
    }

    public static void save(String order, String hidden) {
        if (order != null) Prefers.put(ORDER, join(normalize(order)));
        if (hidden != null) Prefers.put(HIDDEN, join(normalize(hidden)));
    }

    public static String json() {
        JsonArray items = new JsonArray();
        for (String key : order()) {
            JsonObject item = new JsonObject();
            item.addProperty("key", key);
            item.addProperty("label", label(key));
            item.addProperty("hidden", hidden(key));
            items.add(item);
        }
        JsonObject root = new JsonObject();
        root.add("items", items);
        return root.toString();
    }

    public static String label(String key) {
        if (key == null) return null;
        return switch (key) {
            case "bili" -> "Bilibili";
            case "curtain" -> "幕布";
            case "vod" -> "点播";
            case "live" -> "直播";
            case "search" -> "搜索";
            case "keep" -> "收藏";
            case "push" -> "推送";
            case "setting" -> "设置";
            default -> null;
        };
    }

    private static List<String> normalize(String raw) {
        LinkedHashSet<String> keys = new LinkedHashSet<>();
        for (String key : split(raw)) if (label(key) != null) keys.add(key);
        return new ArrayList<>(keys);
    }

    private static List<String> split(String raw) {
        List<String> keys = new ArrayList<>();
        if (raw == null || raw.isEmpty()) return keys;
        for (String part : raw.split(",")) {
            String key = part.trim();
            if (!key.isEmpty()) keys.add(key);
        }
        return keys;
    }

    private static String join(List<String> keys) {
        StringBuilder builder = new StringBuilder();
        for (String key : keys) {
            if (builder.length() > 0) builder.append(',');
            builder.append(key);
        }
        return builder.toString();
    }
}
