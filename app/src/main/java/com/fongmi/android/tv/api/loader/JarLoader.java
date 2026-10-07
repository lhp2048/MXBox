package com.fongmi.android.tv.api.loader;

import android.content.Context;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.utils.Download;
import com.fongmi.android.tv.utils.Task;
import com.fongmi.android.tv.utils.UrlUtil;
import com.github.catvod.crawler.Spider;
import com.github.catvod.crawler.SpiderNull;
import com.github.catvod.net.OkHttp;
import com.github.catvod.utils.Path;
import com.github.catvod.utils.Util;

import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.ZipFile;

import dalvik.system.DexClassLoader;

public class JarLoader {

    private static final int MAX_LOADERS = 3;

    private final ConcurrentHashMap<String, DexClassLoader> loaders;
    private final ConcurrentHashMap<String, Method> methods;
    private final ConcurrentHashMap<String, Spider> spiders;
    private final ConcurrentHashMap<String, Object> locks;
    private volatile String recent;

    public JarLoader() {
        loaders = new ConcurrentHashMap<>();
        methods = new ConcurrentHashMap<>();
        spiders = new ConcurrentHashMap<>();
        locks = new ConcurrentHashMap<>();
    }

    public void clear() {
        spiders.values().forEach(Spider::destroy);
        loaders.clear();
        methods.clear();
        spiders.clear();
        locks.clear();
        recent = null;
    }

    public void onConfigSwitch() {
        releaseSpiders();
        trimLoaders();
    }

    private void releaseSpiders() {
        spiders.values().forEach(Spider::destroy);
        spiders.clear();
    }

    private void trimLoaders() {
        if (loaders.size() <= MAX_LOADERS) return;
        for (String key : loaders.keySet().toArray(new String[0])) {
            if (loaders.size() <= MAX_LOADERS) break;
            if (key.equals(recent)) continue;
            loaders.remove(key);
            methods.remove(key);
            locks.remove(key);
        }
    }

    public void setRecent(String recent) {
        this.recent = recent;
    }

    private void verifyJarLater(String key, String jar, String md5Url) {
        Task.submit(() -> {
            try {
                String md5 = OkHttp.string(md5Url).trim();
                if (md5.isEmpty() || Util.equals(jar, md5)) return;
                Object lock = locks.computeIfAbsent(key, k -> new Object());
                synchronized (lock) {
                    loaders.remove(key);
                    spiders.keySet().removeIf(spKey -> spKey.startsWith(key));
                    load(key, Download.create(jar, Path.jar(jar)).get());
                }
            } catch (Throwable e) {
                e.printStackTrace();
            }
        });
    }

    private boolean load(String key, File file) {
        if (Thread.interrupted()) return false;
        if (!Path.exists(file)) return false;
        if (!isValidJar(file)) {
            file.delete();
            return false;
        }
        if (!file.setReadOnly()) return false;
        try {
            String cachePath = Path.jar().getAbsolutePath();
            DexClassLoader loader = new DexClassLoader(file.getAbsolutePath(), cachePath, cachePath, App.get().getClassLoader());
            invokeInit(loader);
            invokeProxy(key, loader);
            loaders.put(key, loader);
            return true;
        } catch (Throwable e) {
            e.printStackTrace();
            file.delete();
            loaders.remove(key);
            methods.remove(key);
            return false;
        }
    }

    private boolean isValidJar(File file) {
        if (file.length() < 22) return false;
        try (FileInputStream in = new FileInputStream(file)) {
            byte[] head = new byte[4];
            if (in.read(head) != 4) return false;
            if (head[0] != 'P' || head[1] != 'K') return false;
        } catch (IOException e) {
            return false;
        }
        try (ZipFile zip = new ZipFile(file)) {
            return zip.getEntry("classes.dex") != null || zip.getEntry("classes2.dex") != null;
        } catch (IOException e) {
            return false;
        }
    }

    private void invokeInit(DexClassLoader loader) {
        try {
            Class<?> clz = loader.loadClass("com.github.catvod.spider.Init");
            Method method = clz.getMethod("init", Context.class);
            method.invoke(clz, App.get());
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    private void invokeProxy(String key, DexClassLoader loader) {
        try {
            Class<?> clz = loader.loadClass("com.github.catvod.spider.Proxy");
            Method method = clz.getMethod("proxy", Map.class);
            methods.put(key, method);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public boolean parseJar(String key, String jar) {
        if (loaders.containsKey(key)) return true;
        if (jar.startsWith("assets")) jar = UrlUtil.convert(jar);
        Object lock = locks.computeIfAbsent(key, k -> new Object());
        synchronized (lock) {
            if (loaders.containsKey(key)) return true;
            String[] texts = jar.split(";md5;");
            String md5 = texts.length > 1 ? texts[1].trim() : "";
            jar = texts[0];
            if (jar.startsWith("http")) {
                File file = Path.jar(jar);
                if (file.exists()) {
                    boolean loaded = load(key, file);
                    if (loaded && md5.startsWith("http")) verifyJarLater(key, jar, md5);
                    if (loaded) return true;
                }
            }
            if (md5.startsWith("http")) md5 = OkHttp.string(md5).trim();
            if (!md5.isEmpty() && Util.equals(jar, md5)) {
                return load(key, Path.jar(jar));
            } else if (jar.startsWith("http")) {
                return load(key, Download.create(jar, Path.jar(jar)).get());
            } else if (jar.startsWith("file")) {
                return load(key, Path.local(jar));
            }
        }
        return loaders.containsKey(key);
    }

    public DexClassLoader dex(String jar) {
        try {
            String jaKey = Util.md5(jar);
            parseJar(jaKey, jar);
            return loaders.get(jaKey);
        } catch (Throwable e) {
            e.printStackTrace();
            return null;
        }
    }

    public Spider getSpider(String key, String api, String ext, String jar) {
        String jaKey = Util.md5(jar);
        String spKey = jaKey + key;
        return spiders.computeIfAbsent(spKey, k -> {
            try {
                parseJar(jaKey, jar);
                DexClassLoader loader = loaders.get(jaKey);
                if (loader == null) return new SpiderNull();
                Spider spider = (Spider) loader.loadClass("com.github.catvod.spider." + api.split("csp_")[1]).newInstance();
                spider.siteKey = key;
                spider.init(App.get(), ext);
                return spider;
            } catch (Throwable e) {
                e.printStackTrace();
                return new SpiderNull();
            }
        });
    }

    private DexClassLoader requireRecentLoader() {
        DexClassLoader loader = loaders.get(recent);
        if (loader == null) throw new IllegalStateException("No jar loaded for recent key: " + recent);
        return loader;
    }

    public JSONObject jsonExt(String key, LinkedHashMap<String, String> jxs, String url) throws Throwable {
        Class<?> clz = requireRecentLoader().loadClass("com.github.catvod.parser.Json" + key);
        Method method = clz.getMethod("parse", LinkedHashMap.class, String.class);
        return (JSONObject) method.invoke(null, jxs, url);
    }

    public JSONObject jsonExtMix(String flag, String key, String name, LinkedHashMap<String, HashMap<String, String>> jxs, String url) throws Throwable {
        Class<?> clz = requireRecentLoader().loadClass("com.github.catvod.parser.Mix" + key);
        Method method = clz.getMethod("parse", LinkedHashMap.class, String.class, String.class, String.class);
        return (JSONObject) method.invoke(null, jxs, name, flag, url);
    }

    public Object[] proxy(Map<String, String> params) throws Exception {
        Method method = recent != null ? methods.get(recent) : null;
        Object[] result = proxyInvoke(method, params);
        if (result != null) return result;
        return tryOthers(params);
    }

    private Object[] tryOthers(Map<String, String> p) {
        return methods.entrySet().stream().filter(e -> !e.getKey().equals(recent)).map(e -> proxyInvoke(e.getValue(), p)).filter(Objects::nonNull).findFirst().orElse(null);
    }

    private Object[] proxyInvoke(Method method, Map<String, String> params) {
        try {
            return method == null ? null : (Object[]) method.invoke(null, params);
        } catch (Throwable e) {
            e.printStackTrace();
            return null;
        }
    }
}
