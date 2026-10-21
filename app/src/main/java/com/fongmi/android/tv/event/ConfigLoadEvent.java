package com.fongmi.android.tv.event;

import com.fongmi.android.tv.App;

import org.greenrobot.eventbus.EventBus;

public class ConfigLoadEvent {

    public enum Phase {
        IDLE, START, FETCH_CONFIG, CACHE_HIT, PARSE_JAR, DEPOT_MERGE, SYNC_LIVE, HOME_CONTENT, DONE, ERROR
    }

    private static final int VOD = 0;
    public static final int TYPE_VOD = 0;
    public static final int TYPE_LIVE = 1;

    public final Phase phase;
    public final int configType;
    public final String label;
    public final int current;
    public final int total;
    public final String detail;
    public final String message;

    private static volatile Phase sPhase = Phase.IDLE;
    private static volatile int sConfigType;
    private static volatile String sLabel = "";
    private static volatile int sCurrent;
    private static volatile int sTotal;
    private static volatile String sDetail = "";
    private static volatile String sMessage = "";

    private ConfigLoadEvent(Phase phase, int configType, String label, int current, int total, String detail, String message) {
        this.phase = phase;
        this.configType = configType;
        this.label = label == null ? "" : label;
        this.current = current;
        this.total = total;
        this.detail = detail == null ? "" : detail;
        this.message = message == null ? "" : message;
    }

    public static Phase getPhase() {
        return sPhase;
    }

    public static String getLabel() {
        return sLabel;
    }

    public static int getCurrent() {
        return sCurrent;
    }

    public static int getTotal() {
        return sTotal;
    }

    public static String getDetail() {
        return sDetail;
    }

    public static String getMessage() {
        return sMessage;
    }

    public static boolean isActive() {
        return sPhase != Phase.IDLE && sPhase != Phase.DONE && sPhase != Phase.ERROR;
    }

    public static void start(String label, int configType) {
        if (configType != VOD && isActive() && sConfigType == VOD) return;
        sPhase = Phase.START;
        sConfigType = configType;
        sLabel = label == null ? "" : label;
        sCurrent = 0;
        sTotal = 0;
        sDetail = "";
        sMessage = "";
        post(new ConfigLoadEvent(Phase.START, sConfigType, sLabel, 0, 0, "", ""));
    }

    public static void fetchConfig(int sourceType) {
        if (shouldSkipUpdate(sourceType)) return;
        sPhase = Phase.FETCH_CONFIG;
        post(new ConfigLoadEvent(Phase.FETCH_CONFIG, sConfigType, sLabel, 0, 0, "", ""));
    }

    public static void cacheHit(int sourceType) {
        if (shouldSkipUpdate(sourceType)) return;
        sPhase = Phase.CACHE_HIT;
        post(new ConfigLoadEvent(Phase.CACHE_HIT, sConfigType, sLabel, 0, 0, "", ""));
    }

    public static void parseJar(int sourceType) {
        if (shouldSkipUpdate(sourceType)) return;
        sPhase = Phase.PARSE_JAR;
        post(new ConfigLoadEvent(Phase.PARSE_JAR, sConfigType, sLabel, 0, 0, "", ""));
    }

    public static void depotMerge(int sourceType, int current, int total, String depotName, int siteCount) {
        if (shouldSkipUpdate(sourceType)) return;
        sPhase = Phase.DEPOT_MERGE;
        sCurrent = current;
        sTotal = Math.max(total, 1);
        sDetail = depotName == null ? "" : depotName;
        post(new ConfigLoadEvent(Phase.DEPOT_MERGE, sConfigType, sLabel, sCurrent, sTotal, sDetail, String.valueOf(siteCount)));
    }

    public static void syncLive(int sourceType) {
        if (shouldSkipUpdate(sourceType)) return;
        sPhase = Phase.SYNC_LIVE;
        post(new ConfigLoadEvent(Phase.SYNC_LIVE, sConfigType, sLabel, 0, 0, "", ""));
    }

    public static void homeContent() {
        sPhase = Phase.HOME_CONTENT;
        post(new ConfigLoadEvent(Phase.HOME_CONTENT, sConfigType, sLabel, 0, 0, "", ""));
    }

    public static void done() {
        sPhase = Phase.DONE;
        post(new ConfigLoadEvent(Phase.DONE, sConfigType, sLabel, sTotal, sTotal, "", ""));
    }

    public static void error(String message) {
        sPhase = Phase.ERROR;
        sMessage = message == null ? "" : message;
        post(new ConfigLoadEvent(Phase.ERROR, sConfigType, sLabel, 0, 0, "", sMessage));
    }

    public static void reset() {
        sPhase = Phase.IDLE;
        sLabel = "";
        sDetail = "";
        sMessage = "";
        sCurrent = 0;
        sTotal = 0;
    }

    public static ConfigLoadEvent current() {
        return new ConfigLoadEvent(sPhase, sConfigType, sLabel, sCurrent, sTotal, sDetail, sMessage);
    }

    private static boolean shouldSkipUpdate(int configType) {
        return configType != VOD && isActive() && sConfigType == VOD;
    }

    private static void post(ConfigLoadEvent event) {
        App.post(() -> EventBus.getDefault().post(event));
    }
}
