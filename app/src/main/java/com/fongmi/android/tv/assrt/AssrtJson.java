package com.fongmi.android.tv.assrt;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.List;

public final class AssrtJson {

    private AssrtJson() {
    }

    public static JsonArray searchItems(String body) throws AssrtException {
        JsonObject sub = child(object(body), "sub");
        JsonArray subs = sub.has("subs") && sub.get("subs").isJsonArray() ? sub.getAsJsonArray("subs") : new JsonArray();
        JsonArray items = new JsonArray();
        for (JsonElement element : subs) {
            if (!element.isJsonObject()) continue;
            JsonObject row = element.getAsJsonObject();
            boolean hasFileList = row.has("filelist") && row.get("filelist").isJsonArray();
            List<String> names = hasFileList ? fileNames(row.getAsJsonArray("filelist")) : List.of();
            String subtype = text(row, "subtype");
            JsonObject item = new JsonObject();
            item.addProperty("id", text(row, "id"));
            item.addProperty("name", text(row, "native_name"));
            item.addProperty("lang", lang(row));
            item.addProperty("format", subtype);
            item.addProperty("disabled", AssrtRules.disabled(hasFileList, names, subtype));
            items.add(item);
        }
        return items;
    }

    public static Pack detail(String body) throws AssrtException {
        JsonObject sub = child(object(body), "sub");
        JsonArray subs = sub.has("subs") && sub.get("subs").isJsonArray() ? sub.getAsJsonArray("subs") : new JsonArray();
        if (subs.isEmpty() || !subs.get(0).isJsonObject()) throw new AssrtException("没有找到字幕");
        JsonObject row = subs.get(0).getAsJsonObject();
        List<FileItem> files = null;
        if (row.has("filelist") && row.get("filelist").isJsonArray()) {
            files = new ArrayList<>();
            for (JsonElement element : row.getAsJsonArray("filelist")) {
                if (!element.isJsonObject()) continue;
                JsonObject file = element.getAsJsonObject();
                files.add(new FileItem(text(file, "f"), text(file, "url")));
            }
        }
        return new Pack(text(row, "filename"), text(row, "url"), files);
    }

    private static JsonObject child(JsonObject parent, String key) {
        if (!parent.has(key) || !parent.get(key).isJsonObject()) return new JsonObject();
        return parent.getAsJsonObject(key);
    }

    private static JsonObject object(String body) throws AssrtException {
        try {
            JsonObject root = JsonParser.parseString(body).getAsJsonObject();
            int status = root.has("status") ? root.get("status").getAsInt() : -1;
            if (status != 0) throw new AssrtException(AssrtRules.statusMessage(status));
            return root;
        } catch (AssrtException e) {
            throw e;
        } catch (Exception e) {
            throw new AssrtException("搜索失败");
        }
    }

    private static List<String> fileNames(JsonArray files) {
        List<String> names = new ArrayList<>();
        for (JsonElement element : files) {
            if (element.isJsonObject()) names.add(text(element.getAsJsonObject(), "f"));
            else if (element.isJsonPrimitive()) names.add(element.getAsString());
        }
        return names;
    }

    private static String lang(JsonObject row) {
        if (!row.has("lang") || !row.get("lang").isJsonObject()) return "";
        return text(row.getAsJsonObject("lang"), "desc");
    }

    private static String text(JsonObject object, String key) {
        if (!object.has(key) || object.get(key).isJsonNull()) return "";
        return object.get(key).getAsString();
    }

    public static final class Pack {

        public final String filename;
        public final String url;
        public final List<FileItem> files;

        Pack(String filename, String url, List<FileItem> files) {
            this.filename = filename;
            this.url = url;
            this.files = files;
        }
    }

    public static final class FileItem {

        public final String name;
        public final String url;

        FileItem(String name, String url) {
            this.name = name;
            this.url = url;
        }
    }

    public static final class AssrtException extends Exception {

        public AssrtException(String message) {
            super(message);
        }
    }
}
