package com.fongmi.android.tv.api.config;

import android.text.TextUtils;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.bean.Result;

import java.util.LinkedHashMap;
import java.util.Map;

public final class HomeResultCache {

    private static final int MAX_ENTRIES = 8;
    private static final Map<String, Result> CACHE = new LinkedHashMap<>(MAX_ENTRIES, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Result> eldest) {
            return size() > MAX_ENTRIES;
        }
    };

    private HomeResultCache() {
    }

    public static String key(int cid, String siteKey) {
        return cid + "|" + (siteKey == null ? "" : siteKey);
    }

    public static synchronized Result get(int cid, String siteKey) {
        if (TextUtils.isEmpty(siteKey)) return null;
        return copy(CACHE.get(key(cid, siteKey)));
    }

    public static synchronized void put(int cid, String siteKey, Result result) {
        if (TextUtils.isEmpty(siteKey) || result == null) return;
        if (result.getTypes().isEmpty() && result.getList().isEmpty()) return;
        CACHE.put(key(cid, siteKey), copy(result));
    }

    public static synchronized void clear() {
        CACHE.clear();
    }

    private static Result copy(Result result) {
        if (result == null) return null;
        return App.gson().fromJson(App.gson().toJson(result), Result.class);
    }
}
