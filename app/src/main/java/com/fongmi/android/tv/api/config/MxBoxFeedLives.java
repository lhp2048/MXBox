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

/** 将 feed.lives 自定义结构转换为开源 Live 列表。 */
public final class MxBoxFeedLives {

    private MxBoxFeedLives() {
    }

    public static List<Live> parse(JsonArray array) {
        if (array == null || array.size() == 0) return Collections.emptyList();
        List<Live> lives = new ArrayList<>();
        for (JsonElement element : array) {
            if (element == null || !element.isJsonObject()) continue;
            JsonObject groupObj = element.getAsJsonObject();
            if (groupObj.has("channels")) {
                lives.addAll(fromGroupChannels(groupObj));
                continue;
            }
            lives.addAll(fromLiveObject(groupObj));
        }
        return lives;
    }

    private static List<Live> fromGroupChannels(JsonObject groupObj) {
        String groupName = Json.safeString(groupObj, "group");
        JsonArray channels = groupObj.getAsJsonArray("channels");
        if (channels == null) return Collections.emptyList();
        List<Live> lives = new ArrayList<>();
        for (JsonElement element : channels) {
            if (element == null || !element.isJsonObject()) continue;
            JsonObject channel = element.getAsJsonObject();
            String url = Json.safeString(channel, "url");
            if (TextUtils.isEmpty(url)) continue;
            String name = Json.safeString(channel, "name");
            lives.add(buildLive(pickLiveName(groupName, name, url), url));
        }
        return lives;
    }

    private static List<Live> fromLiveObject(JsonObject object) {
        String url = Json.safeString(object, "url");
        if (TextUtils.isEmpty(url)) return Collections.emptyList();
        String name = Json.safeString(object, "name");
        return List.of(buildLive(pickLiveName("", name, url), url));
    }

    private static Live buildLive(String name, String url) {
        return new Live(name, url.trim()).sync();
    }

    private static String pickLiveName(String groupName, String channelName, String url) {
        if (!TextUtils.isEmpty(channelName) && !TextUtils.isEmpty(groupName)) return groupName + " · " + channelName;
        if (!TextUtils.isEmpty(channelName)) return channelName.trim();
        if (!TextUtils.isEmpty(groupName)) return groupName.trim();
        return UrlUtil.getName(url);
    }
}
