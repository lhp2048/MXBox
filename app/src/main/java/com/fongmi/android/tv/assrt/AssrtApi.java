package com.fongmi.android.tv.assrt;

import com.github.catvod.net.OkHttp;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.LinkedHashMap;
import java.util.Map;

import okhttp3.HttpUrl;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public final class AssrtApi {

    private static final String[] HOSTS = {"https://api.assrt.net", "https://api.makedie.me"};

    private AssrtApi() {
    }

    public static String search(String token, String keyword) throws IOException {
        Map<String, String> query = new LinkedHashMap<>();
        query.put("q", keyword);
        query.put("cnt", "15");
        query.put("pos", "0");
        query.put("filelist", "1");
        return get("/v1/sub/search", query, token);
    }

    public static String detail(String token, String id) throws IOException {
        Map<String, String> query = new LinkedHashMap<>();
        query.put("id", id);
        return get("/v1/sub/detail", query, token);
    }

    public static void download(String url, File dest) throws IOException {
        Request request = new Request.Builder().url(url).header("User-Agent", "MXBox").build();
        try (Response response = OkHttp.client(60_000).newCall(request).execute()) {
            ResponseBody body = response.body();
            if (!response.isSuccessful() || body == null) throw new IOException("download");
            File parent = dest.getParentFile();
            if (parent != null && !parent.exists() && !parent.mkdirs() && !parent.exists()) throw new IOException("dir");
            try (InputStream input = body.byteStream(); OutputStream output = new FileOutputStream(dest)) {
                byte[] buffer = new byte[16384];
                int read;
                while ((read = input.read(buffer)) != -1) output.write(buffer, 0, read);
            }
        }
    }

    private static String get(String path, Map<String, String> query, String token) throws IOException {
        IOException last = new IOException("搜索失败");
        for (String host : HOSTS) {
            try {
                return fetch(host, path, query, token);
            } catch (IOException e) {
                last = e;
            }
        }
        throw last;
    }

    private static String fetch(String host, String path, Map<String, String> query, String token) throws IOException {
        HttpUrl base = HttpUrl.parse(host + path);
        if (base == null) throw new IOException("url");
        HttpUrl.Builder builder = base.newBuilder();
        for (Map.Entry<String, String> entry : query.entrySet()) builder.addQueryParameter(entry.getKey(), entry.getValue());
        Request request = new Request.Builder().url(builder.build()).header("Authorization", "Bearer " + token).header("User-Agent", "MXBox").header("Accept", "application/json").build();
        try (Response response = OkHttp.client(20_000).newCall(request).execute()) {
            ResponseBody body = response.body();
            if (body == null) throw new IOException("empty");
            return body.string();
        }
    }
}
