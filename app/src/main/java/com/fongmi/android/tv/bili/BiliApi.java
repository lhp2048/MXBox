package com.fongmi.android.tv.bili;

import com.github.catvod.net.OkHttp;
import com.github.catvod.utils.Json;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import okhttp3.Response;

public final class BiliApi {

    public static final String UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";
    private static final String NAV = "https://api.bilibili.com/x/web-interface/nav";
    private static final String QR_GENERATE = "https://passport.bilibili.com/x/passport-login/web/qrcode/generate";
    private static final String QR_POLL = "https://passport.bilibili.com/x/passport-login/web/qrcode/poll";

    private BiliApi() {
    }

    public static BiliQr generateQr() throws IOException {
        JsonObject root = request(QR_GENERATE, false);
        JsonObject data = obj(root, "data");
        return new BiliQr(BiliQr.State.WAITING, text(data, "url"), text(data, "qrcode_key"), "");
    }

    public static BiliQr pollQr(String key) throws IOException {
        Map<String, String> headers = headers(false);
        try (Response response = OkHttp.newCall(QR_POLL + "?qrcode_key=" + key, headers).execute()) {
            String body = response.body() == null ? "" : response.body().string();
            JsonObject root = Json.parse(body).getAsJsonObject();
            requireOk(root);
            JsonObject data = obj(root, "data");
            int code = data.has("code") ? data.get("code").getAsInt() : -1;
            if (code == 0) {
                String cookie = cookies(response);
                if (cookie.isEmpty()) return new BiliQr(BiliQr.State.FAILED, "", key, "未取得登录态");
                BiliSession.saveLogin(cookie, "");
                refreshNav();
                return new BiliQr(BiliQr.State.SUCCESS, "", key, BiliSession.nickname());
            }
            if (code == 86090) return new BiliQr(BiliQr.State.SCANNED, "", key, "已扫码");
            if (code == 86038) return new BiliQr(BiliQr.State.EXPIRED, "", key, "二维码已过期");
            return new BiliQr(BiliQr.State.WAITING, "", key, "");
        }
    }

    public static void refreshNav() throws IOException {
        JsonObject root = request(NAV, true);
        int code = root.has("code") ? root.get("code").getAsInt() : -1;
        JsonObject data = obj(root, "data");
        JsonObject wbi = obj(data, "wbi_img");
        String mixin = BiliWbi.mixinKey(BiliWbi.imageKey(text(wbi, "img_url")), BiliWbi.imageKey(text(wbi, "sub_url")));
        if (!mixin.isEmpty()) BiliSession.mixinKey(mixin);
        if (code == -101) throw new IOException("auth");
        if (code != 0) throw new IOException("nav " + code);
        boolean login = data.has("isLogin") && data.get("isLogin").getAsBoolean();
        if (!login) throw new IOException("auth");
        BiliSession.nickname(text(data, "uname"));
    }

    public static BiliVideoPage recommend(int freshIdx) throws IOException {
        String url = "https://api.bilibili.com/x/web-interface/index/top/feed/rcmd?ps=" + BiliVideoPage.PAGE_SIZE + "&fresh_idx=" + freshIdx + "&fresh_idx_1h=" + freshIdx + "&fresh_type=3&feed_version=V8";
        JsonObject data = data(request(url, true));
        List<JsonElement> raw = array(data, "item");
        List<BiliVideo> items = new ArrayList<>();
        for (JsonElement element : raw) {
            if (!element.isJsonObject()) continue;
            JsonObject item = element.getAsJsonObject();
            String go = text(item, "goto");
            if (!go.isEmpty() && !"av".equals(go)) continue;
            BiliVideo video = video(text(item, "bvid"), text(item, "title"), firstText(item, "pic", "cover"), obj(item, "owner"));
            if (video.isNormal()) items.add(video);
        }
        return new BiliVideoPage(items, !raw.isEmpty(), freshIdx + 1, "");
    }

    public static BiliVideoPage follow(String offset) throws IOException {
        String url = "https://api.bilibili.com/x/polymer/web-dynamic/v1/feed/all?type=video&features=itemOpusStyle&page_size=" + BiliVideoPage.PAGE_SIZE;
        if (offset != null && !offset.isEmpty()) url += "&offset=" + offset;
        JsonObject data = data(request(url, true));
        List<BiliVideo> items = new ArrayList<>();
        for (JsonElement element : array(data, "items")) {
            if (!element.isJsonObject()) continue;
            JsonObject item = element.getAsJsonObject();
            JsonObject modules = obj(item, "modules");
            JsonObject major = obj(obj(modules, "module_dynamic"), "major");
            if (!"MAJOR_TYPE_ARCHIVE".equals(text(major, "type")) && !obj(major, "archive").has("bvid")) continue;
            JsonObject archive = obj(major, "archive");
            if (!archive.has("bvid")) continue;
            JsonObject author = obj(modules, "module_author");
            BiliVideo video = new BiliVideo(text(archive, "bvid"), text(archive, "title"), text(archive, "cover"), number(author, "mid"), text(author, "name"));
            if (video.isNormal()) items.add(video);
        }
        boolean hasMore = data.has("has_more") && data.get("has_more").getAsBoolean();
        return new BiliVideoPage(items, hasMore && !text(data, "offset").isEmpty(), 1, text(data, "offset"));
    }

    public static BiliVideoPage space(long mid, int page) throws IOException {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("mid", String.valueOf(mid));
        params.put("pn", String.valueOf(page));
        params.put("ps", String.valueOf(BiliVideoPage.PAGE_SIZE));
        params.put("order", "pubdate");
        JsonObject data = data(request(sign("https://api.bilibili.com/x/space/wbi/arc/search", params), true));
        List<BiliVideo> items = new ArrayList<>();
        JsonArray videos = data.has("list") && obj(data, "list").has("vlist") && obj(data, "list").get("vlist").isJsonArray() ? obj(data, "list").getAsJsonArray("vlist") : new JsonArray();
        for (JsonElement element : videos) {
            if (!element.isJsonObject()) continue;
            JsonObject item = element.getAsJsonObject();
            BiliVideo video = new BiliVideo(text(item, "bvid"), text(item, "title"), text(item, "pic"), number(item, "mid"), text(item, "author"));
            if (video.isNormal()) items.add(video);
        }
        int count = obj(data, "page").has("count") ? obj(data, "page").get("count").getAsInt() : items.size();
        boolean hasMore = page * BiliVideoPage.PAGE_SIZE < count;
        return new BiliVideoPage(items, hasMore, page + 1, "");
    }

    public static List<String> suggest(String term) throws IOException {
        String url = "https://s.search.bilibili.com/main/suggest?term=" + URLEncoder.encode(term, "UTF-8");
        JsonObject root = request(url, true);
        List<String> items = new ArrayList<>();
        JsonObject result = obj(root, "result");
        for (JsonElement element : array(result, "tag")) {
            if (!element.isJsonObject()) continue;
            String value = text(element.getAsJsonObject(), "value");
            if (!value.isEmpty() && !items.contains(value)) items.add(value);
            if (items.size() >= 12) break;
        }
        return items;
    }

    public static BiliSearchResult search(String keyword, int page) throws IOException {
        BiliVideoPage videos = searchVideos(keyword, page);
        List<BiliUp> ups = searchUps(keyword, page);
        boolean upMore = ups.size() >= BiliVideoPage.PAGE_SIZE;
        return new BiliSearchResult(videos.getItems(), ups, videos.hasMore(), upMore, page + 1);
    }

    public static BiliStreams resolve(String bvid, int wantedQn) throws IOException, BiliException {
        JsonObject view = data(request("https://api.bilibili.com/x/web-interface/view?bvid=" + bvid, true));
        String redirect = text(view, "redirect_url");
        if (redirect.contains("bangumi") || redirect.contains("cheese") || redirect.contains("ep")) throw new BiliException("暂不支持", false);
        JsonArray pages = view.has("pages") && view.get("pages").isJsonArray() ? view.getAsJsonArray("pages") : new JsonArray();
        if (pages.size() == 0) throw new BiliException("暂不支持", false);
        long cid = pages.get(0).getAsJsonObject().get("cid").getAsLong();
        Map<String, String> params = new LinkedHashMap<>();
        params.put("bvid", bvid);
        params.put("cid", String.valueOf(cid));
        params.put("qn", String.valueOf(wantedQn > 0 ? wantedQn : 127));
        params.put("fnval", "4048");
        params.put("fourk", "1");
        params.put("fnver", "0");
        JsonObject play = data(request(sign("https://api.bilibili.com/x/player/wbi/playurl", params), true));
        List<Integer> values = new ArrayList<>();
        List<BiliQn> qualities = new ArrayList<>();
        JsonArray accept = play.has("accept_quality") && play.get("accept_quality").isJsonArray() ? play.getAsJsonArray("accept_quality") : new JsonArray();
        JsonArray names = play.has("accept_description") && play.get("accept_description").isJsonArray() ? play.getAsJsonArray("accept_description") : new JsonArray();
        for (int i = 0; i < accept.size(); i++) {
            int qn = accept.get(i).getAsInt();
            values.add(qn);
            String label = i < names.size() ? names.get(i).getAsString() : BiliQn.fallback(qn);
            qualities.add(new BiliQn(qn, label));
        }
        int selected = BiliQn.pick(wantedQn, values);
        String videoUrl = "";
        String audioUrl = "";
        JsonObject dash = obj(play, "dash");
        if (dash.has("video")) {
            JsonObject stream = chooseVideo(dash.getAsJsonArray("video"), selected);
            if (stream != null) videoUrl = firstText(stream, "baseUrl", "base_url");
            if (dash.has("audio") && dash.get("audio").isJsonArray() && dash.getAsJsonArray("audio").size() > 0) {
                JsonObject audio = bestAudio(dash.getAsJsonArray("audio"));
                audioUrl = firstText(audio, "baseUrl", "base_url");
            }
        }
        if (videoUrl.isEmpty() && play.has("durl") && play.get("durl").isJsonArray() && play.getAsJsonArray("durl").size() > 0) {
            videoUrl = text(play.getAsJsonArray("durl").get(0).getAsJsonObject(), "url");
            audioUrl = "";
        }
        if (videoUrl.isEmpty()) throw new IOException("empty playurl");
        Map<String, String> headers = new HashMap<>();
        headers.put("User-Agent", UA);
        headers.put("Referer", "https://www.bilibili.com/video/" + bvid);
        if (!BiliSession.cookie().isEmpty()) headers.put("Cookie", BiliSession.cookie());
        return new BiliStreams(videoUrl, audioUrl, qualities, selected, headers);
    }

    private static BiliVideoPage searchVideos(String keyword, int page) throws IOException {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("search_type", "video");
        params.put("keyword", keyword);
        params.put("page", String.valueOf(page));
        params.put("page_size", String.valueOf(BiliVideoPage.PAGE_SIZE));
        JsonObject data = data(request(sign("https://api.bilibili.com/x/web-interface/wbi/search/type", params), true));
        List<BiliVideo> items = new ArrayList<>();
        if (data.has("result") && data.get("result").isJsonArray()) {
            for (JsonElement element : data.getAsJsonArray("result")) {
                if (!element.isJsonObject()) continue;
                JsonObject item = element.getAsJsonObject();
                String title = text(item, "title").replaceAll("<[^>]+>", "");
                BiliVideo video = new BiliVideo(text(item, "bvid"), title, firstText(item, "pic", "cover"), number(item, "mid"), text(item, "author"));
                if (video.isNormal()) items.add(video);
            }
        }
        int num = data.has("numResults") ? data.get("numResults").getAsInt() : items.size();
        return new BiliVideoPage(items, page * BiliVideoPage.PAGE_SIZE < num, page + 1, "");
    }

    private static List<BiliUp> searchUps(String keyword, int page) throws IOException {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("search_type", "bili_user");
        params.put("keyword", keyword);
        params.put("page", String.valueOf(page));
        params.put("page_size", String.valueOf(BiliVideoPage.PAGE_SIZE));
        JsonObject data = data(request(sign("https://api.bilibili.com/x/web-interface/wbi/search/type", params), true));
        List<BiliUp> items = new ArrayList<>();
        if (data.has("result") && data.get("result").isJsonArray()) {
            for (JsonElement element : data.getAsJsonArray("result")) {
                if (!element.isJsonObject()) continue;
                JsonObject item = element.getAsJsonObject();
                long mid = number(item, "mid");
                if (mid <= 0) continue;
                items.add(new BiliUp(mid, text(item, "uname"), firstText(item, "upic", "face")));
            }
        }
        return items;
    }

    private static JsonObject chooseVideo(JsonArray videos, int qn) {
        JsonObject fallback = null;
        JsonObject matched = null;
        int nearest = -1;
        for (JsonElement element : videos) {
            if (!element.isJsonObject()) continue;
            JsonObject item = element.getAsJsonObject();
            int id = item.has("id") ? item.get("id").getAsInt() : 0;
            if (fallback == null) fallback = item;
            if (id == qn) {
                String codec = text(item, "codecs");
                if (matched == null || (codec.startsWith("avc") && !text(matched, "codecs").startsWith("avc"))) matched = item;
            } else if (id < qn && id > nearest) {
                nearest = id;
            }
        }
        if (matched != null) return matched;
        if (nearest > 0) {
            for (JsonElement element : videos) {
                if (!element.isJsonObject()) continue;
                JsonObject item = element.getAsJsonObject();
                if (item.has("id") && item.get("id").getAsInt() == nearest) return item;
            }
        }
        return fallback;
    }

    private static JsonObject bestAudio(JsonArray audios) {
        JsonObject best = audios.get(0).getAsJsonObject();
        int bandwidth = best.has("bandwidth") ? best.get("bandwidth").getAsInt() : 0;
        for (JsonElement element : audios) {
            if (!element.isJsonObject()) continue;
            JsonObject item = element.getAsJsonObject();
            int value = item.has("bandwidth") ? item.get("bandwidth").getAsInt() : 0;
            if (value > bandwidth) {
                best = item;
                bandwidth = value;
            }
        }
        return best;
    }

    private static String sign(String url, Map<String, String> params) throws IOException {
        if (BiliSession.mixinKey().isEmpty()) refreshMixin();
        return BiliWbi.sign(url, params, BiliSession.mixinKey());
    }

    private static void refreshMixin() throws IOException {
        JsonObject root = request(NAV, true);
        JsonObject wbi = obj(obj(root, "data"), "wbi_img");
        String mixin = BiliWbi.mixinKey(BiliWbi.imageKey(text(wbi, "img_url")), BiliWbi.imageKey(text(wbi, "sub_url")));
        if (!mixin.isEmpty()) BiliSession.mixinKey(mixin);
    }

    private static JsonObject request(String url, boolean api) throws IOException {
        String body = OkHttp.string(url, headers(api));
        if (body == null || body.isEmpty()) throw new IOException("empty");
        JsonObject root = Json.parse(body).getAsJsonObject();
        requireOk(root);
        return root;
    }

    private static void requireOk(JsonObject root) throws IOException {
        int code = root.has("code") ? root.get("code").getAsInt() : -1;
        if (code == -101) throw new IOException("auth");
        if (code != 0) throw new IOException(text(root, "message"));
    }

    private static JsonObject data(JsonObject root) {
        return obj(root, "data");
    }

    private static Map<String, String> headers(boolean api) {
        Map<String, String> headers = new HashMap<>();
        headers.put("User-Agent", UA);
        headers.put("Referer", api ? "https://www.bilibili.com" : "https://passport.bilibili.com/login");
        if (!BiliSession.cookie().isEmpty()) headers.put("Cookie", BiliSession.cookie());
        return headers;
    }

    private static String cookies(Response response) {
        StringBuilder builder = new StringBuilder();
        for (String header : response.headers("Set-Cookie")) {
            int split = header.indexOf(';');
            String pair = split > 0 ? header.substring(0, split).trim() : header.trim();
            if (!pair.contains("=")) continue;
            String name = pair.substring(0, pair.indexOf('='));
            if (!"SESSDATA".equals(name) && !"bili_jct".equals(name) && !"DedeUserID".equals(name) && !"DedeUserID__ckMd5".equals(name) && !"sid".equals(name)) continue;
            if (builder.length() > 0) builder.append("; ");
            builder.append(pair);
        }
        return builder.toString();
    }

    private static BiliVideo video(String bvid, String title, String cover, JsonObject owner) {
        return new BiliVideo(bvid, title, cover, number(owner, "mid"), text(owner, "name"));
    }

    private static JsonObject obj(JsonObject parent, String key) {
        if (parent == null || !parent.has(key) || parent.get(key).isJsonNull() || !parent.get(key).isJsonObject()) return new JsonObject();
        return parent.getAsJsonObject(key);
    }

    private static List<JsonElement> array(JsonObject parent, String key) {
        if (parent == null || !parent.has(key) || !parent.get(key).isJsonArray()) return new ArrayList<>();
        List<JsonElement> items = new ArrayList<>();
        for (JsonElement element : parent.getAsJsonArray(key)) items.add(element);
        return items;
    }

    private static String text(JsonObject obj, String key) {
        if (obj == null || !obj.has(key) || obj.get(key).isJsonNull()) return "";
        try {
            return obj.get(key).getAsString();
        } catch (Exception e) {
            return "";
        }
    }

    private static String firstText(JsonObject obj, String first, String second) {
        String value = text(obj, first);
        return value.isEmpty() ? text(obj, second) : value;
    }

    private static long number(JsonObject obj, String key) {
        try {
            if (obj == null || !obj.has(key) || obj.get(key).isJsonNull()) return 0;
            return obj.get(key).getAsLong();
        } catch (Exception e) {
            return 0;
        }
    }
}
