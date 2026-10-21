package com.fongmi.android.tv.bean;

import android.text.TextUtils;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.utils.UrlUtil;
import com.github.catvod.utils.Json;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Depot {

    @SerializedName("url")
    private String url;
    @SerializedName("name")
    private String name;

    public static List<Depot> arrayFrom(JsonArray array) {
        if (array == null) return Collections.emptyList();
        List<Depot> items = new ArrayList<>();
        for (JsonElement element : array) {
            Depot item = objectFrom(element);
            if (item != null && !TextUtils.isEmpty(item.getUrl())) items.add(item);
        }
        return items;
    }

    public static List<Depot> arrayFrom(String str) {
        try {
            JsonElement parsed = Json.parse(str);
            if (parsed.isJsonArray()) return arrayFrom(parsed.getAsJsonArray());
        } catch (Exception ignored) {
        }
        Type listType = TypeToken.getParameterized(List.class, Depot.class).getType();
        List<Depot> items = App.gson().fromJson(str, listType);
        return items == null ? Collections.emptyList() : items;
    }

    private static Depot objectFrom(JsonElement element) {
        if (element == null) return null;
        if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
            return create(element.getAsString(), null);
        }
        if (!element.isJsonObject()) return null;
        JsonObject obj = element.getAsJsonObject();
        String url = Json.safeString(obj, "url");
        if (TextUtils.isEmpty(url)) url = Json.safeString(obj, "sourceUrl");
        String name = Json.safeString(obj, "name");
        if (TextUtils.isEmpty(name)) name = Json.safeString(obj, "sourceName");
        return create(url, name);
    }

    private static Depot create(String url, String name) {
        if (TextUtils.isEmpty(url)) return null;
        Depot depot = new Depot();
        depot.url = url.trim();
        depot.name = TextUtils.isEmpty(name) ? UrlUtil.getName(depot.url) : name.trim();
        return depot;
    }

    public String getUrl() {
        return TextUtils.isEmpty(url) ? "" : url;
    }

    public String getName() {
        return TextUtils.isEmpty(name) ? getUrl() : name;
    }
}
