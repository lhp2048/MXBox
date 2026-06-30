package com.fongmi.android.tv;

import android.text.TextUtils;

import com.fongmi.android.tv.BuildConfig;
import com.fongmi.android.tv.api.config.MxBoxConfigUpdater;
import com.fongmi.android.tv.api.config.MxBoxSourceCatalog;
import com.fongmi.android.tv.api.config.VodConfig;
import com.fongmi.android.tv.api.loader.BaseLoader;
import com.fongmi.android.tv.bean.Config;
import com.fongmi.android.tv.setting.PlayerSetting;
import com.fongmi.android.tv.setting.Setting;
import com.fongmi.android.tv.utils.Task;
import com.github.catvod.utils.Prefers;

public final class MxBoxBootstrap {

    private MxBoxBootstrap() {
    }

    public static void init() {
        applyDefaults();
        MxBoxSourceCatalog.ensureBuiltIns();
        ensureDefaultConfig();
        MxBoxSourceCatalog.refreshFeedAsync();
        MxBoxConfigUpdater.checkRemoteAsync();
        Task.submit(BaseLoader::get);
    }

    public static void onUserConfigChanged(Config config) {
        if (config == null || config.getType() != 0 || config.isEmpty()) return;
        if (MxBoxConstants.DEFAULT_CONFIG_URL.equals(config.getUrl())) return;
        Prefers.put(MxBoxConstants.PREF_USER_CONFIGURED, true);
    }

    private static void applyDefaults() {
        if (Prefers.getBoolean(MxBoxConstants.PREF_INITIALIZED, false)) return;
        PlayerSetting.putEngine(PlayerSetting.ENGINE_EXO);
        Setting.putReset(1);
        Prefers.put(MxBoxConstants.PREF_INITIALIZED, true);
    }

    private static void ensureDefaultConfig() {
        Config config = Config.vod();
        if (!config.isEmpty()) return;
        Config.create(0, MxBoxConstants.DEFAULT_CONFIG_URL, MxBoxConstants.DEFAULT_CONFIG_NAME).save();
        Prefers.put(MxBoxConstants.PREF_CONFIG_VERSION, BuildConfig.DEFAULT_CONFIG_VERSION);
    }

    public static boolean isUserConfigured() {
        return Prefers.getBoolean(MxBoxConstants.PREF_USER_CONFIGURED, false);
    }

    public static int getLocalConfigVersion() {
        return Prefers.getInt(MxBoxConstants.PREF_CONFIG_VERSION, BuildConfig.DEFAULT_CONFIG_VERSION);
    }

    public static void setLocalConfigVersion(int version) {
        Prefers.put(MxBoxConstants.PREF_CONFIG_VERSION, version);
    }

    public static boolean hasRemoteConfigUrl() {
        return !TextUtils.isEmpty(BuildConfig.REMOTE_CONFIG_URL);
    }
}
