package com.fongmi.android.tv.api.config;

import android.text.TextUtils;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.api.Decoder;
import com.fongmi.android.tv.bean.Config;
import com.fongmi.android.tv.event.ConfigEvent;
import com.fongmi.android.tv.event.ConfigLoadEvent;
import com.fongmi.android.tv.impl.Callback;
import com.fongmi.android.tv.server.Server;
import com.fongmi.android.tv.utils.Notify;
import com.fongmi.android.tv.utils.Task;
import com.fongmi.android.tv.utils.UrlUtil;
import com.github.catvod.bean.Header;
import com.github.catvod.bean.Proxy;
import com.github.catvod.net.OkHttp;
import com.github.catvod.utils.Json;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.InterruptedIOException;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

abstract class BaseConfig {

    public static final int VOD = 0;
    public static final int LIVE = 1;
    public static final int WALL = 2;

    private final AtomicInteger taskId = new AtomicInteger(0);

    protected boolean sync;
    protected volatile Config config;
    private volatile boolean forceNetwork;
    private volatile Future<?> future;

    protected abstract String getTag();

    protected abstract Config defaultConfig();

    protected abstract void load(Config config) throws Throwable;

    protected abstract boolean isLoaded();

    public synchronized void ensureLoaded() {
        try {
            if (isLoaded()) return;
            if (config == null) config = defaultConfig();
            Server.get().start();
            load(config);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    protected void postEvent() {
        ConfigEvent.common();
    }

    public boolean needSync(String url) {
        return sync || config == null || TextUtils.isEmpty(config.getUrl()) || url.equals(config.getUrl());
    }

    public Config getConfig() {
        return config == null ? defaultConfig() : config;
    }

    protected void setHeaders(List<Header> headers) {
        OkHttp.responseInterceptor().addAll(headers);
    }

    protected void setProxy(List<Proxy> proxy) {
        OkHttp.selector().addAll(proxy);
    }

    protected void setHosts(List<String> hosts) {
        OkHttp.dns().addAll(hosts);
    }

    public void load(Callback callback) {
        load(callback, false);
    }

    public void load(Callback callback, boolean forceNetwork) {
        load(callback, forceNetwork, null);
    }

    public void load(Callback callback, boolean forceNetwork, Runnable prepare) {
        int id = taskId.incrementAndGet();
        if (future != null && !future.isDone()) future.cancel(true);
        future = Task.submit(() -> {
            if (prepare != null) prepare.run();
            loadConfig(id, config, callback, forceNetwork);
        });
        callback.start();
    }

    public void cancelLoad() {
        taskId.incrementAndGet();
        if (future != null && !future.isDone()) future.cancel(true);
        OkHttp.cancel(getTag());
    }

    protected void loadConfig(int id, Config config, Callback callback, boolean forceNetwork) {
        ConfigLoadEvent.start(config.getDesc(), config.getType());
        try {
            Server.get().start();
            OkHttp.cancel(getTag());
            if (!forceNetwork && ConfigCache.isValid(config)) {
                try {
                    this.forceNetwork = false;
                    ConfigLoadEvent.cacheHit(config.getType());
                    load(config);
                    if (taskId.get() != id) return;
                    if (config.equals(this.config)) config.update();
                    App.post(() -> Notify.show(config.getNotice()));
                    App.post(callback::success);
                    refreshInBackground(config);
                    return;
                } catch (Throwable cacheError) {
                    cacheError.printStackTrace();
                    if (isCanceled(cacheError)) return;
                }
            }
            this.forceNetwork = forceNetwork;
            ConfigLoadEvent.fetchConfig(config.getType());
            load(config);
            if (taskId.get() != id) return;
            if (config.equals(this.config)) config.update();
            App.post(() -> Notify.show(config.getNotice()));
            App.post(callback::success);
        } catch (Throwable e) {
            e.printStackTrace();
            if (isCanceled(e)) return;
            if (taskId.get() != id) return;
            if (isLoaded()) {
                if (config.equals(this.config)) config.update();
                App.post(callback::success);
                return;
            }
            if (TextUtils.isEmpty(config.getUrl())) {
                App.post(() -> callback.error(""));
                ConfigLoadEvent.error("");
            } else {
                String msg = Notify.getError(R.string.error_config_get, e);
                App.post(() -> callback.error(msg));
                ConfigLoadEvent.error(msg);
            }
        } finally {
            this.forceNetwork = false;
            if (taskId.get() == id) postEvent();
        }
    }

    protected String fetchConfigJson(Config config) throws Exception {
        if (!forceNetwork && !TextUtils.isEmpty(config.getJson())) return config.getJson();
        String json = Decoder.getJson(UrlUtil.convert(config.getUrl()), getTag());
        config.setJson(json);
        config.save();
        return json;
    }

    private void refreshInBackground(Config config) {
        final String url = config.getUrl();
        if (TextUtils.isEmpty(url)) return;
        Task.submit(() -> {
            try {
                if (!url.equals(getConfig().getUrl())) return;
                Server.get().start();
                OkHttp.cancel(getTag());
                forceNetwork = true;
                load(config);
                forceNetwork = false;
                if (!url.equals(getConfig().getUrl())) return;
                if (config.equals(this.config)) config.update();
                postEvent();
            } catch (Throwable e) {
                forceNetwork = false;
                if (!isCanceled(e)) e.printStackTrace();
            }
        });
    }

    protected boolean isCanceled(Throwable e) {
        if ("Canceled".equals(e.getMessage())) return true;
        if (e instanceof InterruptedException) return true;
        if (e instanceof InterruptedIOException) return true;
        return e.getCause() instanceof InterruptedIOException;
    }

    protected JsonArray fetchArray(JsonObject object, String key) {
        if (!object.has(key)) return new JsonArray();
        JsonElement element = object.get(key);
        if (element.isJsonObject()) return new JsonArray();
        if (element.isJsonPrimitive()) element = fetch(element.getAsString());
        JsonArray result = new JsonArray();
        for (JsonElement item : element.getAsJsonArray()) {
            if (item.isJsonPrimitive()) result.addAll(fetch(item.getAsString()));
            else if (item.isJsonObject()) result.add(item);
        }
        return result;
    }

    private JsonArray fetch(String url) {
        try {
            JsonElement parsed = Json.parse(OkHttp.string(UrlUtil.convert(url)));
            return parsed.isJsonArray() ? parsed.getAsJsonArray() : new JsonArray();
        } catch (Exception e) {
            return new JsonArray();
        }
    }
}
