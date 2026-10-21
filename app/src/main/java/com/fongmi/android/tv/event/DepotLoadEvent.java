package com.fongmi.android.tv.event;

import com.fongmi.android.tv.App;

import org.greenrobot.eventbus.EventBus;

public class DepotLoadEvent {

    public enum Phase {
        LOADING, DONE
    }

    public final Phase phase;
    public final int current;
    public final int total;
    public final String depotName;
    public final int siteCount;

    private static volatile Phase sPhase = Phase.DONE;
    private static volatile int sCurrent;
    private static volatile int sTotal;
    private static volatile int sSiteCount;
    private static volatile String sDepotName = "";

    private DepotLoadEvent(Phase phase, int current, int total, String depotName, int siteCount) {
        this.phase = phase;
        this.current = current;
        this.total = total;
        this.depotName = depotName == null ? "" : depotName;
        this.siteCount = siteCount;
    }

    public static boolean isLoading() {
        return sPhase == Phase.LOADING;
    }

    public static int getCurrent() {
        return sCurrent;
    }

    public static int getTotal() {
        return sTotal;
    }

    public static int getSiteCount() {
        return sSiteCount;
    }

    public static String getDepotName() {
        return sDepotName;
    }

    public static void start(int total) {
        sPhase = Phase.LOADING;
        sCurrent = 0;
        sTotal = Math.max(total, 1);
        sSiteCount = 0;
        sDepotName = "";
        post(new DepotLoadEvent(Phase.LOADING, 0, sTotal, "", 0));
    }

    public static void progress(int current, int total, String depotName, int siteCount) {
        sCurrent = current;
        sTotal = Math.max(total, 1);
        sDepotName = depotName == null ? "" : depotName;
        sSiteCount = siteCount;
        post(new DepotLoadEvent(Phase.LOADING, current, sTotal, sDepotName, siteCount));
    }

    public static void done(int siteCount) {
        sPhase = Phase.DONE;
        sSiteCount = siteCount;
        post(new DepotLoadEvent(Phase.DONE, sTotal, sTotal, "", siteCount));
    }

    public static void error() {
        sPhase = Phase.DONE;
        post(new DepotLoadEvent(Phase.DONE, 0, 0, "", 0));
    }

    private static void post(DepotLoadEvent event) {
        App.post(() -> EventBus.getDefault().post(event));
    }
}
