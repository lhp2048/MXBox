package com.fongmi.android.tv.bili;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;
import java.util.TreeMap;

public final class BiliWbi {

    private static final int[] MIXIN = {46, 47, 18, 2, 53, 8, 23, 32, 15, 50, 10, 31, 58, 3, 45, 35, 27, 43, 5, 49, 33, 9, 42, 19, 29, 28, 14, 39, 12, 38, 41, 13, 37, 48, 7, 16, 24, 55, 40, 61, 26, 17, 0, 1, 60, 51, 30, 4, 22, 25, 54, 21, 56, 59, 6, 63, 57, 62, 11, 36, 20, 34, 44, 52};

    private BiliWbi() {
    }

    public static String mixinKey(String imgKey, String subKey) {
        String raw = (imgKey == null ? "" : imgKey) + (subKey == null ? "" : subKey);
        StringBuilder builder = new StringBuilder();
        for (int index : MIXIN) if (index < raw.length()) builder.append(raw.charAt(index));
        return builder.length() >= 32 ? builder.substring(0, 32) : builder.toString();
    }

    public static String imageKey(String url) {
        if (url == null || url.isEmpty()) return "";
        int slash = url.lastIndexOf('/');
        int dot = url.lastIndexOf('.');
        if (slash < 0 || dot <= slash) return "";
        return url.substring(slash + 1, dot);
    }

    public static String sign(String url, Map<String, String> params, String mixinKey) {
        TreeMap<String, String> sorted = new TreeMap<>();
        if (params != null) {
            for (Map.Entry<String, String> entry : params.entrySet()) sorted.put(entry.getKey(), filter(entry.getValue()));
        }
        sorted.put("wts", String.valueOf(System.currentTimeMillis() / 1000));
        String query = query(sorted);
        sorted.put("w_rid", md5(query + mixinKey));
        return url + (url.contains("?") ? "&" : "?") + query(sorted);
    }

    private static String query(TreeMap<String, String> sorted) {
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<String, String> entry : sorted.entrySet()) {
            if (builder.length() > 0) builder.append('&');
            builder.append(encode(entry.getKey())).append('=').append(encode(entry.getValue()));
        }
        return builder.toString();
    }

    private static String filter(String value) {
        if (value == null) return "";
        return value.replaceAll("[!'()*]", "");
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private static String md5(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte item : bytes) builder.append(String.format("%02x", item));
            return builder.toString();
        } catch (Exception e) {
            return "";
        }
    }
}
