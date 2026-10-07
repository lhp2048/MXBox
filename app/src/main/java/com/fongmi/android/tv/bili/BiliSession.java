package com.fongmi.android.tv.bili;

import android.text.TextUtils;

import com.github.catvod.utils.Prefers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class BiliSession {

    private static final String KEY_COOKIE = "mx_bili_cookie";
    private static final String KEY_NAME = "mx_bili_name";
    private static final String KEY_QN = "mx_bili_qn";
    private static final String KEY_HISTORY = "mx_bili_search";

    private static String mixinKey = "";

    private BiliSession() {
    }

    public static boolean isLoggedIn() {
        return !cookie().isEmpty();
    }

    public static String cookie() {
        return Prefers.getString(KEY_COOKIE, "");
    }

    public static String nickname() {
        return Prefers.getString(KEY_NAME, "");
    }

    public static int wantedQn() {
        return Prefers.getInt(KEY_QN, 0);
    }

    public static void wantedQn(int qn) {
        Prefers.put(KEY_QN, qn);
    }

    public static void saveLogin(String cookie, String nickname) {
        Prefers.put(KEY_COOKIE, cookie == null ? "" : cookie);
        Prefers.put(KEY_NAME, nickname == null ? "" : nickname);
    }

    public static void nickname(String nickname) {
        Prefers.put(KEY_NAME, nickname == null ? "" : nickname);
    }

    public static void logout() {
        Prefers.put(KEY_COOKIE, "");
        Prefers.put(KEY_NAME, "");
        mixinKey = "";
    }

    public static String mixinKey() {
        return mixinKey;
    }

    public static void mixinKey(String key) {
        mixinKey = key == null ? "" : key;
    }

    public static List<String> searchHistory() {
        String raw = Prefers.getString(KEY_HISTORY, "");
        if (raw.isEmpty()) return new ArrayList<>();
        return new ArrayList<>(Arrays.asList(raw.split("\n")));
    }

    public static void rememberSearch(String word) {
        if (word == null || word.trim().isEmpty()) return;
        String text = word.trim();
        List<String> items = searchHistory();
        items.remove(text);
        items.add(0, text);
        if (items.size() > 12) items = new ArrayList<>(items.subList(0, 12));
        Prefers.put(KEY_HISTORY, TextUtils.join("\n", items));
    }
}
