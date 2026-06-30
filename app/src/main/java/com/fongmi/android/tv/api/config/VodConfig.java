package com.fongmi.android.tv.api.config;

import android.text.TextUtils;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.api.loader.BaseLoader;
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
import com.fongmi.android.tv.utils.Notify;
import com.fongmi.android.tv.utils.UrlUtil;
import com.github.catvod.bean.Doh;
import com.github.catvod.bean.Header;
import com.github.catvod.bean.Proxy;
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
        VodConfig vod = get();
        vod.config(config);
        vod.load(callback, false, () -> vod.clear(false));
    }

    public VodConfig init() {
        return config(Config.vod());
    }

    public VodConfig config(Config config) {
        this.config = config;
        return this;
    }

    public VodConfig clear() {
        return clear(true);
    }

    public VodConfig clear(boolean full) {
        ads = null;
        doh = null;
        home = null;
        wall = null;
        parse = null;
        sites = null;
        flags = null;
        rules = null;
        parses = null;
        if (full) BaseLoader.get().clear();
        else BaseLoader.get().onConfigSwitch();
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
    protected void postEvent() {
        super.postEvent();
        ConfigEvent.vod();
    }

    @Override
    protected void load(Config config) throws Throwable {
        String json = fetchConfigJson(config);
        checkJson(config, Json.parse(json).getAsJsonObject());
    }

    @Override
    protected boolean isLoaded() {
        return !getSites().isEmpty();
    }

    private void checkJson(Config config, JsonObject object) throws Throwable {
        if (object.has("msg")) {
            throw new Exception(object.get("msg").getAsString());
        } else if (object.has("storeHouse")) {
            parseStoreHouse(config, object);
        } else if (object.has("urls")) {
            parseDepot(config, object);
        } else {
            parseConfig(config, object);
        }
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
        JsonObject wrapper = new JsonObject();
        wrapper.add("urls", urls);
        parseDepot(config, wrapper);
    }

    private void parseDepot(Config config, JsonObject object) throws Throwable {
        List<Depot> items = Depot.arrayFrom(object.getAsJsonArray("urls"));
        List<Config> configs = new ArrayList<>();
        for (Depot item : items) configs.add(Config.find(item, VOD));
        if (configs.isEmpty()) throw new Exception("Depot urls is empty");
        if (configs.size() == 1) {
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
                String json = fetchConfigJson(depotConfig);
                JsonObject obj = Json.parse(json).getAsJsonObject();
                if (obj.has("msg") || obj.has("urls") || obj.has("storeHouse")) {
                    DepotLoadEvent.progress(i + 1, total, depotConfig.getDesc(), merged.size());
                ConfigLoadEvent.depotMerge(ConfigLoadEvent.TYPE_VOD, i + 1, total, depotConfig.getDesc(), merged.size());
                    continue;
                }
                boolean isPrimary = primaryObject == null;
                if (isPrimary) {
                    primaryObject = obj;
                    primary = depotConfig;
                }
                String spider = Json.safeString(obj, "spider");
                parseSpiderSafe(spider, isPrimary);
                for (JsonElement element : Json.safeListElement(obj, "sites")) {
                    Site site = Site.objectFrom(element, spider);
                    if (!site.isEmpty()) merged.putIfAbsent(site.getKey(), site);
                }
                applyPartialSites(new ArrayList<>(merged.values()));
                DepotLoadEvent.progress(i + 1, total, depotConfig.getDesc(), merged.size());
                ConfigLoadEvent.depotMerge(ConfigLoadEvent.TYPE_VOD, i + 1, total, depotConfig.getDesc(), merged.size());
            }
            if (primaryObject == null || merged.isEmpty()) throw new Exception("No valid depot config");
            this.config = primary;
            parseConfigMerged(primary, primaryObject, new ArrayList<>(merged.values()));
            primary.update();
            Config.delete(feedConfig.getUrl());
            DepotLoadEvent.done(merged.size());
        } catch (Throwable e) {
            DepotLoadEvent.error();
            throw e;
        }
    }

    private void parseConfigMerged(Config config, JsonObject object, List<Site> mergedSites) {
        initSiteMerged(config, object, mergedSites);
        initListSafe(object);
        initWallSafe(config, object);
        initParseSafe(config, object);
        initLiveSafe(config, object);
        config.setLogo(Json.safeString(object, "logo"));
        config.setNotice(Json.safeString(object, "notice"));
        config.setDanmaku(Json.safeString(object, "danmaku"));
    }

    private void applyPartialSites(List<Site> sites) {
        setSites(new ArrayList<>(sites));
        Map<String, Site> items = Site.findAll().stream().collect(Collectors.toMap(Site::getKey, Function.identity()));
        getSites().forEach(site -> site.sync(items.get(site.getKey())));
    }

    private void initSiteMerged(Config config, JsonObject object, List<Site> mergedSites) {
        String spider = Json.safeString(object, "spider");
        LinkedHashMap<String, Site> deduped = new LinkedHashMap<>();
        for (Site site : mergedSites) {
            if (site.isEmpty()) continue;
            deduped.putIfAbsent(site.getKey(), site);
        }
        setSites(new ArrayList<>(deduped.values()));
        Map<String, Site> items = Site.findAll().stream().collect(Collectors.toMap(Site::getKey, Function.identity()));
        getSites().forEach(site -> site.sync(items.get(site.getKey())));
        setHome(config, pickDefaultHome(config, new ArrayList<>(deduped.values())), false);
        parseSpiderSafe(spider, true);
    }

    private void parseConfig(Config config, JsonObject object) throws Exception {
        initSite(config, object);
        initListSafe(object);
        initWallSafe(config, object);
        initParseSafe(config, object);
        initLiveSafe(config, object);
        config.setLogo(Json.safeString(object, "logo"));
        config.setNotice(Json.safeString(object, "notice"));
        config.setDanmaku(Json.safeString(object, "danmaku"));
    }

    private void initListSafe(JsonObject object) {
        try {
            initList(object);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    private void initWallSafe(Config config, JsonObject object) {
        try {
            initWall(config, object);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    private void initParseSafe(Config config, JsonObject object) {
        try {
            initParse(config, object);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    private void parseSpiderSafe(String spider, boolean recent) {
        if (TextUtils.isEmpty(spider)) return;
        ConfigLoadEvent.parseJar(ConfigLoadEvent.TYPE_VOD);
        try {
            if (!BaseLoader.get().parseJar(spider, recent)) {
                App.post(() -> Notify.show(R.string.mx_load_spider_invalid));
            }
        } catch (Throwable e) {
            e.printStackTrace();
            App.post(() -> Notify.show(R.string.mx_load_spider_invalid));
        }
    }

    private List<Site> parseSites(JsonObject object, String spider) {
        LinkedHashMap<String, Site> items = new LinkedHashMap<>();
        for (JsonElement element : Json.safeListElement(object, "sites")) {
            Site site = Site.objectFrom(element, spider);
            if (site.isEmpty()) continue;
            items.putIfAbsent(site.getKey(), site);
        }
        return new ArrayList<>(items.values());
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
        ConfigLoadEvent.syncLive(ConfigLoadEvent.TYPE_VOD);
        Config temp = Config.find(config, LIVE).save();
        boolean sync = LiveConfig.get().needSync(config.getUrl());
        if (sync) LiveConfig.get().config(temp.update()).parse(object);
    }

    public void reloadLiveMerge() throws Throwable {
        Config config = getConfig();
        if (config.isEmpty()) return;
        ConfigLoadEvent.syncLive(ConfigLoadEvent.TYPE_VOD);
        String json = fetchConfigJson(config);
        if (!Json.isObj(json)) return;
        Config temp = Config.find(config, LIVE).save();
        LiveConfig.get().config(temp.update()).parse(Json.parse(json).getAsJsonObject());
    }

    private void initLiveSafe(Config config, JsonObject object) {
        try {
            initLive(config, object);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    private void initWall(Config config, JsonObject object) {
        if (Json.isEmpty(object, "wallpaper")) return;
        this.wall = Json.safeString(object, "wallpaper");
        Config temp = Config.find(wall, config.getName(), WALL).save();
        boolean sync = WallConfig.get().needSync(wall);
        if (sync) WallConfig.get().config(temp.update());
    }

    private void initSite(Config config, JsonObject object) throws Exception {
        String spider = Json.safeString(object, "spider");
        setSites(parseSites(object, spider));
        if (getSites().isEmpty()) throw new Exception("sites is empty");
        Map<String, Site> items = Site.findAll().stream().collect(Collectors.toMap(Site::getKey, Function.identity()));
        getSites().forEach(site -> site.sync(items.get(site.getKey())));
        setHome(config, pickDefaultHome(config, getSites()), false);
        parseSpiderSafe(spider, true);
    }

    private Site pickDefaultHome(Config config, List<Site> sites) {
        if (sites.isEmpty()) return new Site();
        String key = config.getHome();
        if (!TextUtils.isEmpty(key)) {
            for (Site site : sites) {
                if (site.getKey().equals(key) && site.isSearchable()) return site;
            }
        }
        for (Site site : sites) {
            if (site.isSearchable()) return site;
        }
        return sites.get(0);
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
