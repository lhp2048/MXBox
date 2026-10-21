package com.fongmi.android.tv.server.process;

import com.fongmi.android.tv.assrt.AssrtApi;
import com.fongmi.android.tv.assrt.AssrtJson;
import com.fongmi.android.tv.assrt.AssrtJson.AssrtException;
import com.fongmi.android.tv.assrt.AssrtJson.FileItem;
import com.fongmi.android.tv.assrt.AssrtJson.Pack;
import com.fongmi.android.tv.assrt.AssrtPlayback;
import com.fongmi.android.tv.assrt.AssrtRules;
import com.fongmi.android.tv.assrt.AssrtToken;
import com.fongmi.android.tv.assrt.AssrtZip;
import com.fongmi.android.tv.event.RefreshEvent;
import com.fongmi.android.tv.server.impl.Process;
import com.github.catvod.utils.Path;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import fi.iki.elonen.NanoHTTPD.Method;
import fi.iki.elonen.NanoHTTPD.Response;

public class Assrt implements Process {

    private final AssrtRules.Window searches = new AssrtRules.Window();

    @Override
    public boolean isRequest(IHTTPSession session, String url) {
        return "/assrt".equals(url);
    }

    @Override
    public Response doResponse(IHTTPSession session, String url, Map<String, String> files) {
        Map<String, String> params = session.getParms();
        String action = value(params, "do");
        return switch (action) {
            case "token" -> saveToken(session, params);
            case "state" -> state();
            case "playing" -> playing();
            case "search" -> search(params);
            case "mount" -> mount(params);
            default -> message(Response.Status.BAD_REQUEST, "搜索失败");
        };
    }

    private Response saveToken(IHTTPSession session, Map<String, String> params) {
        if (session.getMethod() != Method.POST) return message(Response.Status.BAD_REQUEST, "请输入密钥");
        String raw = value(params, "token");
        if (raw.isEmpty()) return message(Response.Status.BAD_REQUEST, "请输入密钥");
        if (!AssrtToken.save(raw)) return message(Response.Status.BAD_REQUEST, "密钥格式不对");
        JsonObject body = new JsonObject();
        body.addProperty("set", true);
        return json(Response.Status.OK, body);
    }

    private Response state() {
        JsonObject body = new JsonObject();
        body.addProperty("set", AssrtToken.saved());
        return json(Response.Status.OK, body);
    }

    private Response playing() {
        AssrtPlayback.Snapshot snapshot = AssrtPlayback.get();
        JsonObject body = new JsonObject();
        body.addProperty("playing", snapshot.playing);
        body.addProperty("title", snapshot.title);
        body.addProperty("episode", snapshot.episode);
        return json(Response.Status.OK, body);
    }

    private Response search(Map<String, String> params) {
        if (!AssrtToken.saved()) return message(Response.Status.CONFLICT, "请先保存密钥");
        String keyword = value(params, "q").trim();
        String keywordError = AssrtRules.keywordError(keyword);
        if (!keywordError.isEmpty()) return message(Response.Status.BAD_REQUEST, keywordError);
        if (!searches.tryAcquire(System.currentTimeMillis())) return message(Response.Status.CONFLICT, "每分钟最多搜索 5 次");
        try {
            JsonArray items = AssrtJson.searchItems(AssrtApi.search(AssrtToken.get(), keyword));
            if (items.isEmpty()) return message(Response.Status.OK, "没有找到字幕");
            JsonObject body = new JsonObject();
            body.add("items", items);
            return json(Response.Status.OK, body);
        } catch (AssrtException e) {
            return message(Response.Status.OK, e.getMessage());
        } catch (IOException e) {
            return message(Response.Status.OK, "搜索失败");
        }
    }

    private Response mount(Map<String, String> params) {
        if (!AssrtToken.saved()) return message(Response.Status.CONFLICT, "请先保存密钥");
        AssrtPlayback.Snapshot snapshot = AssrtPlayback.get();
        if (!snapshot.playing) return message(Response.Status.CONFLICT, "请先在电视上播放点播");
        String id = value(params, "id");
        if (!AssrtRules.validId(id)) return message(Response.Status.BAD_REQUEST, "没有找到字幕");
        try {
            Pack pack = AssrtJson.detail(AssrtApi.detail(AssrtToken.get(), id));
            return mountPack(pack, value(params, "file"), snapshot.episode);
        } catch (AssrtException e) {
            return message(Response.Status.OK, e.getMessage());
        } catch (IOException e) {
            return message(Response.Status.OK, "字幕下载失败");
        }
    }

    private Response mountPack(Pack pack, String wanted, String episode) throws IOException, AssrtException {
        if (pack.files != null) return mountListed(pack.files, wanted, episode);
        String kind = AssrtRules.archiveKind(pack.filename);
        if ("text".equals(kind)) {
            if (!wanted.isEmpty() && !wanted.equals(pack.filename)) return message(Response.Status.BAD_REQUEST, "请重新选择");
            if (pack.url.isEmpty()) return message(Response.Status.OK, "字幕下载失败");
            return mounted(download(pack.url, baseName(pack.filename)));
        }
        if (!"zip".equals(kind)) return message(Response.Status.OK, "请换一条，暂不支持该格式");
        return mountZip(pack.url, wanted, episode);
    }

    private Response mountListed(List<FileItem> files, String wanted, String episode) throws IOException {
        List<String> names = new ArrayList<>();
        for (FileItem file : files) names.add(file.name);
        List<String> text = AssrtRules.textNames(names);
        if (text.isEmpty()) return message(Response.Status.OK, "请换一条，暂不支持该格式");
        String chosen = wanted.isEmpty() ? AssrtRules.pick(text, episode) : wanted;
        if (chosen.isEmpty()) return choose(text);
        if (!text.contains(chosen)) return message(Response.Status.BAD_REQUEST, "请重新选择");
        FileItem file = find(files, chosen);
        if (file == null || file.url.isEmpty()) return message(Response.Status.OK, "字幕下载失败");
        return mounted(download(file.url, baseName(file.name)));
    }

    private Response mountZip(String url, String wanted, String episode) throws IOException {
        if (url.isEmpty()) return message(Response.Status.OK, "字幕下载失败");
        File zip = new File(cacheDir(), "pack.zip");
        try {
            AssrtApi.download(url, zip);
            List<String> text = AssrtZip.listText(zip);
            if (text.isEmpty()) return message(Response.Status.OK, "请换一条，暂不支持该格式");
            String chosen = wanted.isEmpty() ? AssrtRules.pick(text, episode) : wanted;
            if (chosen.isEmpty()) return choose(text);
            if (!text.contains(chosen)) return message(Response.Status.BAD_REQUEST, "请重新选择");
            File root = new File(cacheDir(), "out");
            if (!root.exists() && !root.mkdirs() && !root.exists()) return message(Response.Status.OK, "字幕下载失败");
            clear(root);
            return mounted(AssrtZip.extract(zip, chosen, root));
        } catch (AssrtZip.Invalid e) {
            return message(Response.Status.OK, "字幕包无效");
        } finally {
            zip.delete();
        }
    }

    private Response mounted(File file) {
        RefreshEvent.subtitle(file.getAbsolutePath());
        JsonObject body = new JsonObject();
        body.addProperty("ok", true);
        body.addProperty("name", file.getName());
        return json(Response.Status.OK, body);
    }

    private Response choose(List<String> names) {
        JsonObject body = new JsonObject();
        body.addProperty("ok", false);
        JsonArray choose = new JsonArray();
        for (String name : names) choose.add(name);
        body.add("choose", choose);
        return json(Response.Status.OK, body);
    }

    private File download(String url, String name) throws IOException {
        File dest = new File(cacheDir(), name);
        AssrtApi.download(url, dest);
        return dest;
    }

    private static File cacheDir() throws IOException {
        File dir = new File(Path.cache(), "assrt");
        if (!dir.exists() && !dir.mkdirs() && !dir.exists()) throw new IOException("dir");
        return dir;
    }

    private static FileItem find(List<FileItem> files, String name) {
        for (FileItem file : files) if (name.equals(file.name)) return file;
        return null;
    }

    private static String baseName(String name) {
        String normalized = name.replace('\\', '/');
        int slash = normalized.lastIndexOf('/');
        String base = slash >= 0 ? normalized.substring(slash + 1) : normalized;
        if (base.isEmpty() || AssrtRules.unsafeZipName(base) || !AssrtRules.textSubtitle(base)) return "subtitle.srt";
        return base;
    }

    private static void clear(File dir) {
        File[] children = dir.listFiles();
        if (children == null) return;
        for (File child : children) {
            if (child.isDirectory()) clear(child);
            child.delete();
        }
    }

    private static String value(Map<String, String> params, String key) {
        String value = params.get(key);
        return value == null ? "" : value;
    }

    private static Response json(Response.Status status, JsonObject body) {
        return NanoHTTPD.newFixedLengthResponse(status, "application/json; charset=utf-8", body.toString());
    }

    private static Response message(Response.Status status, String text) {
        JsonObject body = new JsonObject();
        body.addProperty("message", text);
        return json(status, body);
    }
}
