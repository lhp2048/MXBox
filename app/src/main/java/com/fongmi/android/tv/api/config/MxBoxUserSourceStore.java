package com.fongmi.android.tv.api.config;

import android.text.TextUtils;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.bean.Config;
import com.github.catvod.utils.Prefers;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Logo 源中心：用户手动添加的点播/直播配置（不含 feed 下发与内置 TEST）。 */
public final class MxBoxUserSourceStore {

    private static final String PREF_KEYS = "mx_user_source_keys";

    private MxBoxUserSourceStore() {
    }

    public static String key(Config config) {
        return key(config.getType(), config.getUrl());
    }

    public static String key(int type, String url) {
        return type + "|" + url;
    }

    public static void add(Config config) {
        if (config == null || TextUtils.isEmpty(config.getUrl())) return;
        if (config.getType() == 0 && MxBoxFeedStore.isFeedVod(config.getUrl())) return;
        if (MxBoxSourceCatalog.isBuiltIn(config.getUrl())) return;
        if (MxBoxSourceCatalog.isFeedEndpoint(config.getUrl())) return;
        LinkedHashSet<String> keys = loadKeys();
        if (keys.add(key(config))) saveKeys(keys);
    }

    public static void remove(Config config) {
        if (config == null) return;
        LinkedHashSet<String> keys = loadKeys();
        if (keys.remove(key(config))) saveKeys(keys);
    }

    public static boolean contains(Config config) {
        return config != null && loadKeys().contains(key(config));
    }

    public static List<Config> getConfigs(int type) {
        List<Config> items = new ArrayList<>();
        for (String entry : loadKeys()) {
            Config config = toConfig(entry);
            if (config == null || config.getType() != type) continue;
            items.add(config.save());
        }
        return items;
    }

    public static List<Config> getAll() {
        List<Config> items = new ArrayList<>();
        for (String entry : loadKeys()) {
            Config config = toConfig(entry);
            if (config == null) continue;
            items.add(config.save());
        }
        return items;
    }

    private static Config toConfig(String entry) {
        if (TextUtils.isEmpty(entry)) return null;
        int split = entry.indexOf('|');
        if (split <= 0 || split >= entry.length() - 1) return null;
        try {
            int type = Integer.parseInt(entry.substring(0, split));
            String url = entry.substring(split + 1);
            if (TextUtils.isEmpty(url)) return null;
            if (type == 0 && MxBoxFeedStore.isFeedVod(url)) return null;
            if (MxBoxSourceCatalog.isBuiltIn(url)) return null;
            return Config.find(url, type);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static LinkedHashSet<String> loadKeys() {
        String json = Prefers.getString(PREF_KEYS);
        Type type = new TypeToken<LinkedHashSet<String>>() {}.getType();
        LinkedHashSet<String> keys = App.gson().fromJson(json, type);
        return keys == null ? new LinkedHashSet<>() : keys;
    }

    private static void saveKeys(Set<String> keys) {
        Prefers.put(PREF_KEYS, App.gson().toJson(keys));
    }
}
