package com.fongmi.android.tv.api.config;

import android.text.TextUtils;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.api.loader.BaseLoader;
import com.fongmi.android.tv.api.node.NodeRuntime;
import com.fongmi.android.tv.bean.Config;
import com.fongmi.android.tv.bean.Depot;
import com.fongmi.android.tv.bean.Parse;
import com.fongmi.android.tv.bean.Rule;
import com.fongmi.android.tv.bean.Site;
import com.fongmi.android.tv.event.ConfigEvent;
import com.fongmi.android.tv.event.ConfigLoadEvent;
import com.fongmi.android.tv.event.DepotLoadEvent;
import com.fongmi.android.tv.event.RefreshEvent;
import com.fongmi.android.tv.impl.Callback;
import com.fongmi.android.tv.utils.UrlUtil;
import com.github.catvod.bean.Doh;
import com.github.catvod.bean.Header;
import com.github.catvod.bean.Proxy;
import com.github.catvod.runtime.NodeRuntimeApi;
import com.github.catvod.utils.Decoder;
import com.github.catvod.utils.Json;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class VodConfig extends BaseConfig {

    private static final String TAG = VodConfig.class.getSimpleName();

    private Site home;
    private String wall;
    private Parse parse;
    private List<Doh> doh;
    private List<Rule> rules;
    private List<Site> sites;
    private List<String> ads;
    private List<String> flags;
    private List<Parse> parses;

    public static VodConfig get() {
        return Loader.INSTANCE;
    }

    public static int getCid() {
        return get().getConfig().getId();
    }

    public static String getUrl() {
        return get().getConfig().getUrl();
    }

    public static String getDesc() {
        return get().getConfig().getDesc();
    }

    public static int getHomeIndex() {
        return get().getSites().indexOf(get().getHome());
    }

    public static boolean hasParse() {
        return !get().getParses().isEmpty();
    }

    public static void load(Config config, Callback callback) {
        get().clear().config(config).load(callback);
    }

    public VodConfig init() {
        return config(Config.vod());
    }

    public VodConfig config(Config config) {
        this.config = config;
        return this;
    }

    public VodConfig clear() {
        ads = null;
        doh = null;
        home = null;
        wall = null;
        parse = null;
        sites = null;
        flags = null;
        rules = null;
        parses = null;
        BaseLoader.get().clear();
        RuleConfig.get().invalidate();
        return this;
    }

    @Override
    protected String getTag() {
        return TAG;
    }

    @Override
    protected Config defaultConfig() {
        return Config.vod();
    }

    @Override
    protected void cancelLoad() {
        NodeRuntime.get().cancelLoad();
    }

    @Override
    protected void postEvent() {
        super.postEvent();
        ConfigEvent.vod();
    }

    @Override
    protected void load(Config config) throws Throwable {
        boolean node = NodeRuntime.isConfig(config.getUrl());
        NodeRuntime runtime = NodeRuntime.get();
        if (!node) runtime.clear();
        NodeRuntimeApi.LoadedConfig loaded = null;
        try {
            if (node) loaded = runtime.loadConfig(config.getUrl());
            String json = node ? loaded.json() : Decoder.getJson(UrlUtil.convert(config.getUrl()), TAG);
            runtime.prune(loaded, Config.getAll(VOD).stream().map(Config::getUrl).toList(), config.getUrl());
            checkJson(config, Json.parse(json).getAsJsonObject(), loaded);
        } catch (Throwable e) {
            if (node) runtime.fail(loaded);
            throw e;
        }
    }

    @Override
    protected boolean isLoaded() {
        return !getSites().isEmpty();
    }

    private void checkJson(Config config, JsonObject object, NodeRuntimeApi.LoadedConfig loaded) throws Throwable {
        if (object.has("msg")) throw new Exception(object.get("msg").getAsString());
        if (object.has("storeHouse")) {
            if (loaded != null) NodeRuntime.get().accept(loaded);
            parseStoreHouse(config, object);
            return;
        }
        if (object.has("urls")) {
            List<Depot> items = Depot.arrayFrom(object.getAsJsonArray("urls").toString());
            if (items.isEmpty()) throw new Exception("Depot urls is empty");
            if (loaded != null) NodeRuntime.get().accept(loaded);
            parseDepot(config, items);
            return;
        }
        parseConfig(config, object);
        if (loaded != null) NodeRuntime.get().accept(loaded);
    }

    private void parseStoreHouse(Config config, JsonObject object) throws Throwable {
        JsonArray storeHouse = object.getAsJsonArray("storeHouse");
        JsonArray urls = new JsonArray();
        for (JsonElement element : storeHouse) {
            if (!element.isJsonObject()) continue;
            JsonObject item = element.getAsJsonObject();
            JsonObject url = new JsonObject();
            url.addProperty("name", Json.safeString(item, "sourceName"));
            url.addProperty("url", Json.safeString(item, "sourceUrl"));
            urls.add(url);
        }
        List<Depot> items = Depot.arrayFrom(urls);
        if (items.isEmpty()) throw new Exception("Depot urls is empty");
        parseDepot(config, items);
    }

    private void parseDepot(Config config, List<Depot> items) throws Throwable {
        List<Config> configs = new ArrayList<>();
        for (Depot item : items) configs.add(Config.find(item, VOD));
        if (configs.size() <= 1) {
            load(this.config = configs.get(0));
            Config.delete(config.getUrl());
            return;
        }
        parseDepotMerge(config, configs);
    }

    private void parseDepotMerge(Config feedConfig, List<Config> configs) throws Throwable {
        int total = configs.size();
        DepotLoadEvent.start(total);
        LinkedHashMap<String, Site> merged = new LinkedHashMap<>();
        JsonObject primaryObject = null;
        Config primary = configs.get(0);
        try {
            for (int i = 0; i < configs.size(); i++) {
                if (Thread.currentThread().isInterrupted()) throw new InterruptedException("Canceled");
                Config depotConfig = configs.get(i);
                String json = Decoder.getJson(UrlUtil.convert(depotConfig.getUrl()), TAG);
                JsonObject obj = Json.parse(json).getAsJsonObject();
                if (obj.has("msg") || obj.has("urls") || obj.has("storeHouse")) {
                    DepotLoadEvent.progress(i + 1, total, depotConfig.getDesc(), merged.size());
                    ConfigLoadEvent.depotMerge(ConfigLoadEvent.TYPE_VOD, i + 1, total, depotConfig.getDesc(), merged.size());
                    continue;
                }
                if (primaryObject == null) {
                    primaryObject = obj;
                    primary = depotConfig;
                }
                String spider = Json.safeString(obj, "spider");
                if (!TextUtils.isEmpty(spider)) BaseLoader.get().parseJar(spider, primaryObject == obj);
                for (JsonElement element : Json.safeListElement(obj, "sites")) {
                    Site site = Site.objectFrom(element, spider);
                    if (!site.isEmpty()) merged.putIfAbsent(site.getKey(), site);
                }
                DepotLoadEvent.progress(i + 1, total, depotConfig.getDesc(), merged.size());
                ConfigLoadEvent.depotMerge(ConfigLoadEvent.TYPE_VOD, i + 1, total, depotConfig.getDesc(), merged.size());
            }
            if (primaryObject == null || merged.isEmpty()) throw new Exception("No valid depot config");
            this.config = primary;
            parseConfig(primary, primaryObject);
            setSites(new ArrayList<>(merged.values()));
            Map<String, Site> saved = Site.findAll().stream().collect(Collectors.toMap(Site::getKey, Function.identity()));
            getSites().forEach(site -> site.sync(saved.get(site.getKey())));
            Config selected = primary;
            setHome(selected, getSites().stream().filter(item -> item.getKey().equals(selected.getHome())).findFirst().orElse(getSites().get(0)), false);
            selected.update();
            Config.delete(feedConfig.getUrl());
            DepotLoadEvent.done(merged.size());
        } catch (Throwable e) {
            DepotLoadEvent.error();
            throw e;
        }
    }

    public void reloadLiveMerge() throws Throwable {
        Config config = getConfig();
        if (config == null || config.isEmpty()) return;
        String json = Decoder.getJson(UrlUtil.convert(config.getUrl()), TAG);
        if (!Json.isObj(json)) return;
        Config temp = Config.find(config, LIVE).save();
        LiveConfig.get().config(temp.update()).parse(Json.parse(json).getAsJsonObject());
    }

    private void parseConfig(Config config, JsonObject object) {
        initList(object);
        initLive(config, object);
        initWall(config, object);
        initSite(config, object);
        initParse(config, object);
        config.setLogo(Json.safeString(object, "logo"));
        config.setAssrt(Json.safeString(object, "assrt"));
        config.setNotice(Json.safeString(object, "notice"));
        config.setDanmaku(Json.safeString(object, "danmaku"));
    }

    private void initList(JsonObject object) {
        setHeaders(Header.arrayFrom(fetchArray(object, "headers")));
        setProxy(Proxy.arrayFrom(fetchArray(object, "proxy")));
        setRules(Rule.arrayFrom(fetchArray(object, "rules")));
        setDoh(Doh.arrayFrom(fetchArray(object, "doh")));
        setFlags(Json.safeListString(object, "flags"));
        setHosts(Json.safeListString(object, "hosts"));
        setAds(Json.safeListString(object, "ads"));
    }

    private void initLive(Config config, JsonObject object) {
        if (Json.isEmpty(object, "lives") && !MxBoxFeedStore.hasLives()) return;
        Config temp = Config.find(config, LIVE).save();
        boolean sync = LiveConfig.get().needSync(config.getUrl());
        if (sync || MxBoxFeedStore.hasLives()) LiveConfig.get().config(temp.update()).parse(object);
    }

    private void initWall(Config config, JsonObject object) {
        if (Json.isEmpty(object, "wallpaper")) return;
        this.wall = Json.safeString(object, "wallpaper");
        Config temp = Config.find(wall, config.getName(), WALL).save();
        boolean sync = WallConfig.get().needSync(wall);
        if (sync) WallConfig.get().config(temp.update());
    }

    private void initSite(Config config, JsonObject object) {
        String spider = Json.safeString(object, "spider");
        BaseLoader.get().parseJar(spider, true);
        setSites(Json.safeListElement(object, "sites").stream().map(e -> Site.objectFrom(e, spider)).distinct().collect(Collectors.toCollection(ArrayList::new)));
        Map<String, Site> items = Site.findAll().stream().collect(Collectors.toMap(Site::getKey, Function.identity()));
        getSites().forEach(site -> site.sync(items.get(site.getKey())));
        setHome(config, getSites().isEmpty() ? new Site() : getSites().stream().filter(item -> item.getKey().equals(config.getHome())).findFirst().orElse(getSites().get(0)), false);
    }

    private void initParse(Config config, JsonObject object) {
        setParses(Json.safeListElement(object, "parses").stream().map(Parse::objectFrom).distinct().collect(Collectors.toCollection(ArrayList::new)));
        setParse(config, getParses().isEmpty() ? new Parse() : getParses().stream().filter(item -> item.getName().equals(config.getParse())).findFirst().orElse(getParses().get(0)), false);
    }

    public List<Site> getSites() {
        return sites == null ? Collections.emptyList() : sites;
    }

    private void setSites(List<Site> sites) {
        this.sites = sites;
    }

    public List<Parse> getParses() {
        return parses == null ? Collections.emptyList() : parses;
    }

    private void setParses(List<Parse> parses) {
        if (!parses.isEmpty()) parses.add(0, Parse.god());
        this.parses = parses;
    }

    public List<Doh> getDoh() {
        List<Doh> items = Doh.get(App.get());
        if (doh == null) return items;
        items.removeAll(doh);
        items.addAll(doh);
        return items;
    }

    private void setDoh(List<Doh> doh) {
        this.doh = doh;
    }

    public List<Rule> getRules() {
        return rules == null ? Collections.emptyList() : rules;
    }

    private void setRules(List<Rule> rules) {
        this.rules = rules;
        RuleConfig.get().invalidate();
    }

    public List<Parse> getParses(int type) {
        return getParses().stream().filter(item -> item.getType() == type).toList();
    }

    public List<Parse> getParses(int type, String flag) {
        List<Parse> items = getParses(type);
        List<Parse> filter = items.stream().filter(item -> item.getExt().getFlag().contains(flag)).toList();
        return filter.isEmpty() ? items : filter;
    }

    public List<String> getFlags() {
        return flags == null ? Collections.emptyList() : flags;
    }

    private void setFlags(List<String> flags) {
        this.flags = flags;
    }

    public List<String> getAds() {
        return ads == null ? Collections.emptyList() : ads;
    }

    private void setAds(List<String> ads) {
        this.ads = ads;
        RuleConfig.get().invalidate();
    }

    public Parse getParse() {
        return parse == null ? new Parse() : parse;
    }

    public void setParse(Parse parse) {
        setParse(getConfig(), parse, true);
    }

    public Site getHome() {
        return home == null ? new Site() : home;
    }

    public void setHome(Site site) {
        setHome(getConfig(), site, true);
        RefreshEvent.home();
    }

    public String getWall() {
        return TextUtils.isEmpty(wall) ? "" : wall;
    }

    public Parse getParse(String name) {
        return getParses().stream().filter(item -> item.getName().equals(name)).findFirst().orElse(new Parse());
    }

    public Site getSite(String key) {
        return getSites().stream().filter(item -> item.getKey().equals(key)).findFirst().orElse(new Site());
    }

    private void setParse(Config config, Parse parse, boolean save) {
        this.parse = parse;
        this.parse.setSelected(true);
        config.setParse(parse.getName());
        getParses().forEach(item -> item.setSelected(parse));
        if (save) config.save();
    }

    private void setHome(Config config, Site site, boolean save) {
        home = site;
        home.setSelected(true);
        config.setHome(home.getKey());
        if (save) config.save();
        getSites().forEach(item -> item.setSelected(home));
    }

    private static class Loader {
        static volatile VodConfig INSTANCE = new VodConfig();
    }
}
