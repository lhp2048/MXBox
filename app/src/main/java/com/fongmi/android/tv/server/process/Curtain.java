package com.fongmi.android.tv.server.process;

import android.text.TextUtils;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.curtain.CurtainHub;
import com.fongmi.android.tv.curtain.CurtainStore;
import com.fongmi.android.tv.server.impl.Process;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import fi.iki.elonen.NanoHTTPD.Response;

public class Curtain implements Process {

    private static final String OFFLINE = "请先在电视上打开幕布";

    @Override
    public boolean isRequest(IHTTPSession session, String url) {
        return "/curtain".equals(url);
    }

    @Override
    public Response doResponse(IHTTPSession session, String url, Map<String, String> files) {
        Map<String, String> params = session.getParms();
        String action = value(params, "do");
        if ("preview".equals(action)) return preview(value(params, "id"));
        if (action.isEmpty() || "state".equals(action)) return state();
        if (!CurtainHub.active()) return text(Response.Status.CONFLICT, OFFLINE);
        boolean refresh = true;
        try {
            switch (action) {
                case "color" -> CurtainStore.color(value(params, "value"));
                case "imageFit" -> CurtainStore.imageFit(value(params, "mode"));
                case "videoFit" -> CurtainStore.videoFit(value(params, "mode"));
                case "audio" -> CurtainStore.audio(value(params, "source"));
                case "volume" -> CurtainStore.volume(number(params, "value", 80));
                case "musicMode" -> CurtainStore.musicMode(value(params, "mode"));
                case "music" -> refresh = music(value(params, "action"));
                case "removeMusic" -> CurtainStore.removeMusic(value(params, "name"));
                case "clearImage" -> CurtainStore.clearImage();
                case "clearVideo" -> CurtainStore.clearVideo();
                case "resident" -> refresh = resident(params);
                case "clearText" -> {
                    onMain(() -> { if (CurtainHub.host() != null) CurtainHub.host().clearText(); });
                    return state();
                }
                case "removePush" -> {
                    List<String> ids = split(value(params, "ids"));
                    if (ids.isEmpty()) throw new IllegalArgumentException("请先选择");
                    onMain(() -> { if (CurtainHub.host() != null) CurtainHub.host().removePushes(ids); });
                    return state();
                }
                case "text" -> {
                    postText(params);
                    return state();
                }
                case "restore" -> CurtainStore.restore();
                case "clearCache" -> CurtainStore.clearCache();
                case "upload" -> upload(params, files);
                default -> { return text(Response.Status.BAD_REQUEST, "无法识别的指令"); }
            }
        } catch (IllegalArgumentException e) {
            return text(Response.Status.BAD_REQUEST, e.getMessage());
        } catch (Exception ignored) {
            return text(Response.Status.INTERNAL_ERROR, "处理失败");
        }
        if (refresh) App.post(this::publish);
        return state();
    }

    private boolean music(String action) {
        if ("next".equals(action)) {
            CurtainStore.next();
            App.post(() -> { if (CurtainHub.host() != null) CurtainHub.host().restartMusic(); });
            return false;
        }
        CurtainStore.musicPlaying("play".equals(action));
        return true;
    }

    private boolean resident(Map<String, String> params) {
        String text = value(params, "text");
        int seconds = number(params, "seconds", 0);
        int size = number(params, "size", 56);
        String color = value(params, "color");
        boolean stroke = !"0".equals(value(params, "stroke"));
        if (seconds > 0) {
            App.post(() -> { if (CurtainHub.host() != null) CurtainHub.host().showResident(text, size, CurtainStore.argb(color, 0xFFFFFFFF), stroke, seconds); });
            return false;
        }
        CurtainStore.resident(text, size, color, stroke);
        return true;
    }

    private void postText(Map<String, String> params) {
        String text = value(params, "text");
        if (text.isEmpty()) throw new IllegalArgumentException("请输入文字");
        onMain(() -> { if (CurtainHub.host() != null) CurtainHub.host().pushText(text); });
    }

    private void upload(Map<String, String> params, Map<String, String> files) {
        String kind = value(params, "kind");
        String key = "";
        for (String name : files.keySet()) if (!TextUtils.isEmpty(files.get(name))) key = name;
        if (key.isEmpty()) throw new IllegalArgumentException("没有文件");
        File temp = new File(files.get(key));
        String saved = CurtainStore.put(temp, value(params, key), kind);
        if ("push".equals(kind)) {
            File file = CurtainStore.media(saved);
            onMain(() -> { if (CurtainHub.host() != null && file != null) CurtainHub.host().pushImage(file); });
        }
    }

    private Response state() {
        JsonObject obj = JsonParser.parseString(CurtainStore.json(CurtainHub.active())).getAsJsonObject();
        JsonArray pushes = new JsonArray();
        CurtainHub.Host host = CurtainHub.host();
        if (host != null) {
            for (CurtainHub.Item item : host.pushes()) {
                JsonObject row = new JsonObject();
                row.addProperty("id", item.id);
                row.addProperty("kind", item.kind);
                row.addProperty("label", item.label);
                pushes.add(row);
            }
        }
        obj.add("pushes", pushes);
        return json(obj.toString());
    }

    private Response preview(String id) {
        File file = null;
        CurtainHub.Host host = CurtainHub.host();
        if (host != null) {
            for (CurtainHub.Item item : host.pushes()) {
                if (item.id.equals(id) && item.file != null && item.file.isFile()) file = item.file;
            }
        }
        if (file == null) return text(Response.Status.NOT_FOUND, "");
        try {
            return NanoHTTPD.newChunkedResponse(Response.Status.OK, NanoHTTPD.getMimeTypeForFile(file.getName()), new FileInputStream(file));
        } catch (Exception ignored) {
            return text(Response.Status.NOT_FOUND, "");
        }
    }

    private static void onMain(Runnable action) {
        CountDownLatch latch = new CountDownLatch(1);
        App.post(() -> {
            try {
                action.run();
            } finally {
                latch.countDown();
            }
        });
        try {
            latch.await(2, TimeUnit.SECONDS);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }

    private static List<String> split(String raw) {
        List<String> ids = new ArrayList<>();
        if (raw.isEmpty()) return ids;
        for (String part : raw.split(",")) {
            String id = part.trim();
            if (!id.isEmpty()) ids.add(id);
        }
        return ids;
    }

    private void publish() {
        CurtainHub.Host host = CurtainHub.host();
        if (host != null) host.apply();
    }

    private static Response json(String body) {
        return NanoHTTPD.newFixedLengthResponse(Response.Status.OK, "application/json; charset=utf-8", body);
    }

    private static Response text(Response.Status status, String body) {
        return NanoHTTPD.newFixedLengthResponse(status, "text/plain; charset=utf-8", body == null ? "" : body);
    }

    private static String value(Map<String, String> params, String key) {
        String text = params.get(key);
        return text == null ? "" : text.trim();
    }

    private static int number(Map<String, String> params, String key, int fallback) {
        try {
            return Integer.parseInt(value(params, key));
        } catch (Exception ignored) {
            return fallback;
        }
    }
}
