package com.fongmi.android.tv.curtain;

import com.github.catvod.utils.Path;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public final class CurtainStore {

    public static final int FIT = 0;
    public static final int FILL = 1;
    public static final int AUDIO_VIDEO = 0;
    public static final int AUDIO_MUSIC = 1;
    public static final int PLAY_LIST = 0;
    public static final int PLAY_SINGLE = 1;
    public static final int PLAY_SHUFFLE = 2;
    public static final String DEFAULT_COLOR = "101114";

    private static final long IMAGE_LIMIT = 20L * 1024 * 1024;
    private static final long AUDIO_LIMIT = 30L * 1024 * 1024;
    private static final long VIDEO_LIMIT = 200L * 1024 * 1024;
    private static final long TOTAL_LIMIT = 1024L * 1024 * 1024;
    private static final Gson GSON = new Gson();
    private static final Random RANDOM = new Random();
    private static CurtainStore data;

    private String color = DEFAULT_COLOR;
    private String image = "";
    private String video = "";
    private int imageFit;
    private int videoFit;
    private int audio;
    private int volume = 80;
    private int musicMode;
    private int musicIndex;
    private boolean musicPlaying;
    private List<String> music = new ArrayList<>();
    private String resident = "";
    private int residentSize = 56;
    private String residentColor = "FFFFFF";
    private boolean residentStroke = true;

    private CurtainStore() {
    }

    public static synchronized File dir() {
        File dir = new File(Path.files(), "curtain");
        if (!dir.exists()) dir.mkdirs();
        return dir;
    }

    public static synchronized File media(String name) {
        if (name == null || name.isEmpty() || name.contains("/") || name.contains("\\") || name.contains("..")) return null;
        return new File(dir(), name);
    }

    public static synchronized String json(boolean active) {
        CurtainStore store = data();
        JsonObject obj = new JsonObject();
        obj.addProperty("active", active);
        obj.addProperty("reason", active ? "" : "请先在电视上打开幕布");
        obj.addProperty("color", store.color);
        obj.addProperty("image", store.image);
        obj.addProperty("video", store.video);
        obj.addProperty("imageFit", store.imageFit == FILL ? "fill" : "fit");
        obj.addProperty("videoFit", store.videoFit == FILL ? "fill" : "fit");
        obj.addProperty("audio", store.audio == AUDIO_MUSIC ? "music" : "video");
        obj.addProperty("volume", store.volume);
        obj.addProperty("musicMode", modeName(store.musicMode));
        obj.addProperty("musicPlaying", store.musicPlaying);
        obj.addProperty("musicIndex", store.musicIndex);
        JsonArray tracks = new JsonArray();
        for (String name : store.music) tracks.add(name);
        obj.add("music", tracks);
        obj.addProperty("resident", store.resident);
        obj.addProperty("residentSize", store.residentSize);
        obj.addProperty("residentColor", store.residentColor);
        obj.addProperty("residentStroke", store.residentStroke);
        obj.addProperty("cache", cacheSize());
        return obj.toString();
    }

    public static synchronized String color() {
        return data().color;
    }

    public static synchronized int imageFit() {
        return data().imageFit;
    }

    public static synchronized int videoFit() {
        return data().videoFit;
    }

    public static synchronized boolean hearsVideo() {
        return data().audio != AUDIO_MUSIC;
    }

    public static synchronized int volume() {
        return data().volume;
    }

    public static synchronized int musicMode() {
        return data().musicMode;
    }

    public static synchronized boolean musicPlaying() {
        return data().musicPlaying;
    }

    public static synchronized String imageName() {
        return data().image;
    }

    public static synchronized String videoName() {
        return data().video;
    }

    public static synchronized String currentMusic() {
        CurtainStore store = data();
        if (store.music.isEmpty()) return "";
        if (store.musicIndex < 0 || store.musicIndex >= store.music.size()) store.musicIndex = 0;
        return store.music.get(store.musicIndex);
    }

    public static synchronized String resident() {
        return data().resident;
    }

    public static synchronized int residentSize() {
        return data().residentSize;
    }

    public static synchronized String residentColor() {
        return data().residentColor;
    }

    public static synchronized boolean residentStroke() {
        return data().residentStroke;
    }

    public static synchronized void color(String value) {
        data().color = normalizeColor(value, DEFAULT_COLOR);
        save();
    }

    public static synchronized void imageFit(String mode) {
        data().imageFit = "fill".equals(mode) ? FILL : FIT;
        save();
    }

    public static synchronized void videoFit(String mode) {
        data().videoFit = "fill".equals(mode) ? FILL : FIT;
        save();
    }

    public static synchronized void audio(String source) {
        boolean music = "music".equals(source);
        data().audio = music ? AUDIO_MUSIC : AUDIO_VIDEO;
        data().musicPlaying = music && !data().music.isEmpty();
        save();
    }

    public static synchronized void volume(int value) {
        data().volume = Math.max(0, Math.min(100, value));
        save();
    }

    public static synchronized void musicMode(String mode) {
        if ("single".equals(mode)) data().musicMode = PLAY_SINGLE;
        else if ("shuffle".equals(mode)) data().musicMode = PLAY_SHUFFLE;
        else data().musicMode = PLAY_LIST;
        save();
    }

    public static synchronized void musicPlaying(boolean playing) {
        data().musicPlaying = playing;
        if (playing) data().audio = AUDIO_MUSIC;
        save();
    }

    public static synchronized void next() {
        CurtainStore store = data();
        int count = store.music.size();
        if (count == 0) return;
        if (store.musicMode == PLAY_SHUFFLE && count > 1) {
            int next = store.musicIndex;
            while (next == store.musicIndex) next = RANDOM.nextInt(count);
            store.musicIndex = next;
        } else if (store.musicMode != PLAY_SINGLE) {
            store.musicIndex = (store.musicIndex + 1) % count;
        }
        store.musicPlaying = true;
        store.audio = AUDIO_MUSIC;
        save();
    }

    public static synchronized String put(File temp, String original, String kind) {
        String ext = extension(original);
        long limit = limit(kind);
        if (limit < 0) throw new IllegalArgumentException("无法识别的文件");
        if (!allowed(kind, ext)) throw new IllegalArgumentException(reject(kind));
        if (temp == null || !temp.exists()) throw new IllegalArgumentException("没有文件");
        if (temp.length() > limit) throw new IllegalArgumentException(tooBig(kind));
        if (cacheSize() + temp.length() > TOTAL_LIMIT) throw new IllegalArgumentException("缓存已满，请先清理");
        String name = System.currentTimeMillis() + "_" + safe(original, ext);
        File dest = new File(dir(), name);
        Path.copy(temp, dest);
        temp.delete();
        if (!dest.exists() || dest.length() == 0) {
            dest.delete();
            throw new IllegalArgumentException("保存失败");
        }
        CurtainStore store = data();
        if ("image".equals(kind)) replace(store.image, name, true);
        else if ("video".equals(kind)) replace(store.video, name, false);
        else if ("music".equals(kind)) {
            store.music.add(name);
            if (store.music.size() == 1) store.musicIndex = 0;
        }
        save();
        return name;
    }

    public static synchronized void clearImage() {
        delete(data().image);
        data().image = "";
        save();
    }

    public static synchronized void clearVideo() {
        delete(data().video);
        data().video = "";
        save();
    }

    public static synchronized void removeMusic(String name) {
        CurtainStore store = data();
        int index = store.music.indexOf(name);
        if (index < 0) return;
        store.music.remove(index);
        delete(name);
        if (index < store.musicIndex) store.musicIndex--;
        if (store.music.isEmpty()) {
            store.musicIndex = 0;
            store.musicPlaying = false;
        } else if (store.musicIndex >= store.music.size()) store.musicIndex = store.music.size() - 1;
        save();
    }

    public static synchronized void resident(String text, int size, String color, boolean stroke) {
        CurtainStore store = data();
        store.resident = text == null ? "" : text.trim();
        store.residentSize = clamp(size, 24, 96);
        store.residentColor = normalizeColor(color, "FFFFFF");
        store.residentStroke = stroke;
        save();
    }

    public static synchronized void clearResident() {
        data().resident = "";
        save();
    }

    public static synchronized void restore() {
        CurtainStore store = data();
        delete(store.image);
        delete(store.video);
        store.color = DEFAULT_COLOR;
        store.image = "";
        store.video = "";
        store.imageFit = FIT;
        store.videoFit = FIT;
        store.audio = AUDIO_VIDEO;
        store.resident = "";
        store.musicPlaying = false;
        save();
    }

    public static synchronized void clearCache() {
        CurtainStore store = data();
        File dir = dir();
        File[] children = dir.listFiles();
        if (children != null) for (File child : children) if (!"state.json".equals(child.getName())) child.delete();
        store.image = "";
        store.video = "";
        store.music.clear();
        store.musicIndex = 0;
        store.musicPlaying = false;
        store.audio = AUDIO_VIDEO;
        save();
    }

    public static int argb(String hex, int fallback) {
        try {
            return (int) (0xFF000000L | Integer.parseInt(normalizeColor(hex, ""), 16));
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static void replace(String previous, String name, boolean image) {
        if (!name.equals(previous)) delete(previous);
        if (image) data().image = name;
        else data().video = name;
    }

    private static CurtainStore data() {
        if (data == null) data = read();
        if (data.music == null) data.music = new ArrayList<>();
        return data;
    }

    private static CurtainStore read() {
        File file = new File(dir(), "state.json");
        try {
            if (!file.exists()) return new CurtainStore();
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            try (FileInputStream input = new FileInputStream(file)) {
                byte[] buffer = new byte[4096];
                int read;
                while ((read = input.read(buffer)) != -1) output.write(buffer, 0, read);
            }
            CurtainStore store = GSON.fromJson(new String(output.toByteArray(), StandardCharsets.UTF_8), CurtainStore.class);
            return store == null ? new CurtainStore() : store;
        } catch (Exception ignored) {
            return new CurtainStore();
        }
    }

    private static void save() {
        File file = new File(dir(), "state.json");
        try (FileOutputStream output = new FileOutputStream(file)) {
            output.write(GSON.toJson(data()).getBytes(StandardCharsets.UTF_8));
        } catch (Exception ignored) {
        }
    }

    private static long cacheSize() {
        long total = 0;
        File[] children = dir().listFiles();
        if (children == null) return 0;
        for (File child : children) if (child.isFile() && !"state.json".equals(child.getName())) total += child.length();
        return total;
    }

    private static void delete(String name) {
        File file = media(name);
        if (file != null && file.exists()) file.delete();
    }

    private static long limit(String kind) {
        if ("image".equals(kind) || "push".equals(kind)) return IMAGE_LIMIT;
        if ("music".equals(kind)) return AUDIO_LIMIT;
        if ("video".equals(kind)) return VIDEO_LIMIT;
        return -1;
    }

    private static String tooBig(String kind) {
        if ("music".equals(kind)) return "音乐不能超过 30MB";
        if ("video".equals(kind)) return "视频不能超过 200MB";
        return "图片不能超过 20MB";
    }

    private static String reject(String kind) {
        if ("music".equals(kind)) return "只接受常见音频";
        if ("video".equals(kind)) return "只接受常见视频";
        return "只接受常见图片";
    }

    private static boolean allowed(String kind, String ext) {
        if ("image".equals(kind) || "push".equals(kind)) return "jpg".equals(ext) || "jpeg".equals(ext) || "png".equals(ext) || "webp".equals(ext) || "gif".equals(ext);
        if ("music".equals(kind)) return "mp3".equals(ext) || "m4a".equals(ext) || "aac".equals(ext) || "wav".equals(ext) || "flac".equals(ext) || "ogg".equals(ext);
        if ("video".equals(kind)) return "mp4".equals(ext) || "mkv".equals(ext) || "webm".equals(ext) || "mov".equals(ext);
        return false;
    }

    private static String extension(String name) {
        if (name == null) return "";
        int dot = name.lastIndexOf('.');
        if (dot < 0 || dot == name.length() - 1) return "";
        return name.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private static String safe(String original, String ext) {
        String base = original == null ? "file" : original;
        int slash = Math.max(base.lastIndexOf('/'), base.lastIndexOf('\\'));
        if (slash >= 0) base = base.substring(slash + 1);
        base = base.replaceAll("[^\\w.\\u4e00-\\u9fa5-]", "_");
        if (base.length() > 48) base = base.substring(base.length() - 48);
        if (base.isEmpty()) base = "file." + ext;
        return base;
    }

    private static String modeName(int mode) {
        if (mode == PLAY_SINGLE) return "single";
        if (mode == PLAY_SHUFFLE) return "shuffle";
        return "list";
    }

    static String normalizeColor(String value, String fallback) {
        if (value == null) return fallback;
        String text = value.trim();
        if (text.startsWith("#")) text = text.substring(1);
        if (text.length() == 3) text = "" + text.charAt(0) + text.charAt(0) + text.charAt(1) + text.charAt(1) + text.charAt(2) + text.charAt(2);
        if (text.length() != 6) return fallback;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            boolean hex = (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
            if (!hex) return fallback;
        }
        return text.toUpperCase(Locale.ROOT);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
