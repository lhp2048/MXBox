package com.fongmi.android.tv.api.config;

import android.text.TextUtils;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.api.Decoder;
import com.fongmi.android.tv.bean.Config;
import com.fongmi.android.tv.server.Server;
import com.fongmi.android.tv.utils.Task;
import com.fongmi.android.tv.utils.UrlUtil;
import com.github.catvod.net.OkHttp;
import com.github.catvod.utils.Path;
import com.github.catvod.utils.Prefers;
import com.github.catvod.utils.Util;
import com.google.common.net.HttpHeaders;
import com.google.gson.reflect.TypeToken;

import java.io.InputStream;
import java.lang.reflect.Type;
import java.net.IDN;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public final class MxBoxSourceProbe {

    public static final long PROBE_TIMEOUT_MS = 12000L;
    public static final int PROBE_PARALLEL = 5;
    private static final String PREF_RESULTS = "mx_source_probe_results";
    private static final String PREF_TIME = "mx_source_probe_time";
    private static final String TAG = "MxBoxSourceProbe";

    private static final AtomicBoolean RUNNING = new AtomicBoolean(false);
    private static final CopyOnWriteArrayList<Listener> LISTENERS = new CopyOnWriteArrayList<>();
    private static volatile Session SESSION;

    private MxBoxSourceProbe() {
    }

    public static String key(Config config) {
        return config.getType() + "|" + config.getUrl();
    }

    public static boolean isRunning() {
        return RUNNING.get();
    }

    public static Session getSession() {
        return SESSION;
    }

    public static long getLastTestTime() {
        return Prefers.getLong(PREF_TIME, 0L);
    }

    public static Map<String, Result> loadResults() {
        String json = Prefers.getString(PREF_RESULTS);
        if (TextUtils.isEmpty(json)) return new HashMap<>();
        Type type = new TypeToken<Map<String, Result>>() {}.getType();
        Map<String, Result> map = App.gson().fromJson(json, type);
        return map == null ? new HashMap<>() : map;
    }

    public static Map<String, Result> displayResults() {
        Session session = SESSION;
        if (session != null && session.running) return new HashMap<>(session.results);
        return loadResults();
    }

    public static void addListener(Listener listener) {
        if (listener == null) return;
        LISTENERS.addIfAbsent(listener);
        syncListener(listener);
    }

    public static void removeListener(Listener listener) {
        LISTENERS.remove(listener);
    }

    public static void probeAll() {
        if (!RUNNING.compareAndSet(false, true)) return;
        Task.submit(() -> {
            Session session = new Session();
            SESSION = session;
            session.running = true;
            ExecutorService pool = Executors.newFixedThreadPool(PROBE_PARALLEL);
            try {
                Server.get().start();
                List<Config> sources = MxBoxSourceCatalog.getPickerSources();
                session.total = sources.size();
                notifyStart(session.total);
                OkHttpClient client = OkHttp.client(PROBE_TIMEOUT_MS);
                CountDownLatch latch = new CountDownLatch(sources.size());
                for (Config config : sources) {
                    pool.execute(() -> {
                        String probeKey = key(config);
                        try {
                            session.beginTesting(probeKey);
                            notifyTesting(config, session.completed.get(), session.total);
                            Result result = probe(client, config);
                            session.complete(probeKey, result);
                            notifyProgress(session.completed.get(), session.total, config, result);
                        } catch (Throwable e) {
                            Result result = new Result();
                            result.success = false;
                            result.latencyMs = 1;
                            result.message = e.getMessage();
                            session.complete(probeKey, result);
                            notifyProgress(session.completed.get(), session.total, config, result);
                        } finally {
                            latch.countDown();
                        }
                    });
                }
                latch.await();
                Map<String, Result> results = new HashMap<>(session.results);
                long testedAt = System.currentTimeMillis();
                saveResults(results, testedAt);
                session.running = false;
                notifyComplete(testedAt, results);
            } catch (Throwable e) {
                session.running = false;
                notifyError(e.getMessage());
            } finally {
                pool.shutdown();
                RUNNING.set(false);
            }
        });
    }

    private static void syncListener(Listener listener) {
        Session session = SESSION;
        if (session == null || !session.running) return;
        App.post(() -> {
            if (!LISTENERS.contains(listener)) return;
            if (listener.syncSession(session)) return;
            listener.onProbeStart(session.total);
            for (Map.Entry<String, Result> entry : session.results.entrySet()) {
                Config config = findConfig(entry.getKey());
                if (config != null) listener.onProgress(session.completed.get(), session.total, config, entry.getValue());
            }
            for (String probeKey : session.testingKeys) {
                if (session.results.containsKey(probeKey)) continue;
                Config config = findConfig(probeKey);
                if (config != null) listener.onTesting(config, session.completed.get(), session.total);
            }
        });
    }

    private static Config findConfig(String probeKey) {
        for (Config config : MxBoxSourceCatalog.getPickerSources()) {
            if (probeKey.equals(key(config))) return config;
        }
        return null;
    }

    private static void saveResults(Map<String, Result> results, long testedAt) {
        Prefers.put(PREF_RESULTS, App.gson().toJson(results));
        Prefers.put(PREF_TIME, testedAt);
    }

    private static void notifyStart(int total) {
        App.post(() -> {
            for (Listener listener : LISTENERS) listener.onProbeStart(total);
        });
    }

    private static void notifyTesting(Config config, int progress, int total) {
        App.post(() -> {
            for (Listener listener : LISTENERS) listener.onTesting(config, progress, total);
        });
    }

    private static void notifyProgress(int progress, int total, Config config, Result result) {
        App.post(() -> {
            for (Listener listener : LISTENERS) listener.onProgress(progress, total, config, result);
        });
    }

    private static void notifyComplete(long testedAt, Map<String, Result> results) {
        App.post(() -> {
            for (Listener listener : LISTENERS) listener.onComplete(testedAt, results);
        });
    }

    private static void notifyError(String message) {
        App.post(() -> {
            for (Listener listener : LISTENERS) listener.onError(message);
        });
    }

    private static Result probe(OkHttpClient client, Config config) {
        long start = System.currentTimeMillis();
        Result result = new Result();
        try {
            String url = config.getUrl();
            if (TextUtils.isEmpty(url)) throw new Exception("empty url");
            url = url.trim();
            String scheme = UrlUtil.scheme(url);
            if ("assets".equals(scheme)) probeAssets(url);
            else if ("file".equals(scheme)) probeFile(url);
            else probeRemote(client, url, config.getType());
            result.success = true;
            result.latencyMs = Math.max(1L, System.currentTimeMillis() - start);
        } catch (Throwable e) {
            result.success = false;
            result.latencyMs = Math.max(1L, System.currentTimeMillis() - start);
            result.message = e.getMessage() == null ? "error" : e.getMessage();
        }
        return result;
    }

    private static void probeAssets(String url) throws Exception {
        int index = url.indexOf("://");
        if (index < 0) throw new Exception("invalid assets url");
        String path = url.substring(index + 3).trim();
        if (TextUtils.isEmpty(path)) throw new Exception("invalid assets url");
        try (InputStream in = App.get().getAssets().open(path)) {
            byte[] buffer = new byte[8192];
            while (in.read(buffer) != -1) {
            }
        }
    }

    private static void probeFile(String url) throws Exception {
        if (!Path.exists(Path.local(url))) throw new Exception("file missing");
    }

    private static void probeRemote(OkHttpClient client, String url, int type) throws Exception {
        String target = normalizeUrl(UrlUtil.convert(url));
        if (type != 1) {
            try {
                String json = Decoder.getJson(target, TAG);
                if (!TextUtils.isEmpty(json)) return;
            } catch (Throwable ignored) {
            }
        }
        probeHttp(client, target);
    }

    private static void probeHttp(OkHttpClient client, String target) throws Exception {
        Request request = new Request.Builder()
                .url(target)
                .tag(TAG)
                .header(HttpHeaders.USER_AGENT, Util.CHROME)
                .header(HttpHeaders.ACCEPT, "*/*")
                .get()
                .build();
        try (Response response = client.newCall(request).execute()) {
            int code = response.code();
            if (response.body() == null) throw new Exception("HTTP " + code);
            String body = response.body().string();
            if (code >= 400) throw new Exception("HTTP " + code);
            if (TextUtils.isEmpty(body)) throw new Exception("empty body");
        }
    }

    private static String normalizeUrl(String url) {
        if (TextUtils.isEmpty(url)) return url;
        HttpUrl httpUrl = HttpUrl.parse(url.trim());
        if (httpUrl == null) return url.trim();
        String host = httpUrl.host();
        if (TextUtils.isEmpty(host)) return httpUrl.toString();
        try {
            String ascii = IDN.toASCII(host, IDN.ALLOW_UNASSIGNED);
            if (!ascii.equals(host)) return httpUrl.newBuilder().host(ascii).build().toString();
        } catch (Exception ignored) {
        }
        return httpUrl.toString();
    }

    public interface Listener {

        void onProbeStart(int total);

        void onTesting(Config config, int progress, int total);

        void onProgress(int progress, int total, Config config, Result result);

        void onComplete(long testedAt, Map<String, Result> results);

        void onError(String message);

        default boolean syncSession(Session session) {
            return false;
        }
    }

    public static class Session {
        public int total;
        public final AtomicInteger completed = new AtomicInteger(0);
        public final Set<String> testingKeys = ConcurrentHashMap.newKeySet();
        public boolean running;
        public final Map<String, Result> results = new ConcurrentHashMap<>();

        void beginTesting(String probeKey) {
            testingKeys.add(probeKey);
        }

        void complete(String probeKey, Result result) {
            testingKeys.remove(probeKey);
            results.put(probeKey, result);
            completed.incrementAndGet();
        }
    }

    public static class Result {
        public long latencyMs;
        public boolean success;
        public String message;
    }
}
