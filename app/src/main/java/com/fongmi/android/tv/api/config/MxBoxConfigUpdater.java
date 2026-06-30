package com.fongmi.android.tv.api.config;

import android.text.TextUtils;

import com.fongmi.android.tv.BuildConfig;
import com.fongmi.android.tv.MxBoxBootstrap;
import com.fongmi.android.tv.MxBoxConstants;
import com.fongmi.android.tv.api.config.VodConfig;
import com.fongmi.android.tv.bean.Config;
import com.fongmi.android.tv.utils.Task;
import com.fongmi.android.tv.impl.Callback;
import com.github.catvod.net.OkHttp;
import com.github.catvod.utils.Json;
import com.google.gson.JsonObject;

public final class MxBoxConfigUpdater {

    private MxBoxConfigUpdater() {
    }

    public static void checkRemoteAsync() {
        if (!MxBoxBootstrap.hasRemoteConfigUrl() || MxBoxBootstrap.isUserConfigured()) return;
        Task.submit(MxBoxConfigUpdater::checkRemote);
    }

    private static void checkRemote() {
        try {
            String url = BuildConfig.REMOTE_CONFIG_URL;
            if (TextUtils.isEmpty(url)) return;
            String json = OkHttp.string(url);
            JsonObject object = Json.parse(json).getAsJsonObject();
            if (!object.has("version")) return;
            int remoteVersion = object.get("version").getAsInt();
            if (remoteVersion <= MxBoxBootstrap.getLocalConfigVersion()) return;
            Config config = Config.find(url, 0);
            config.setName(MxBoxConstants.DEFAULT_CONFIG_NAME);
            config.save();
            MxBoxBootstrap.setLocalConfigVersion(remoteVersion);
            VodConfig.load(config, new Callback());
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
}
