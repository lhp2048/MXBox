package com.fongmi.android.tv.bili;

import com.github.catvod.utils.Prefers;
import com.google.gson.JsonObject;

import java.util.Locale;

public final class BiliTitleFilter {

    private static final String ENABLED = "mx_bili_filter_on";
    private static final String WORDS = "mx_bili_filter_words";

    private BiliTitleFilter() {
    }

    public static boolean enabled() {
        return Prefers.getBoolean(ENABLED, false);
    }

    public static void enabled(boolean on) {
        Prefers.put(ENABLED, on);
    }

    public static String words() {
        String value = Prefers.getString(WORDS, "");
        return value == null ? "" : value;
    }

    public static void words(String value) {
        Prefers.put(WORDS, value == null ? "" : value.trim());
    }

    public static boolean blocked(String title) {
        if (!enabled() || title == null || title.isEmpty()) return false;
        String raw = words().trim();
        if (raw.isEmpty()) return false;
        String hay = title.toLowerCase(Locale.ROOT);
        for (String word : raw.split("\\s+")) {
            if (word.isEmpty()) continue;
            if (hay.contains(word.toLowerCase(Locale.ROOT))) return true;
        }
        return false;
    }

    public static String json() {
        JsonObject obj = new JsonObject();
        obj.addProperty("enabled", enabled());
        obj.addProperty("words", words());
        return obj.toString();
    }
}
