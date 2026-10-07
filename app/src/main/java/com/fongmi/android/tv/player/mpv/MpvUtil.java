package com.fongmi.android.tv.player.mpv;

import android.text.TextUtils;

import androidx.media3.common.Player;
import androidx.media3.common.util.Util;
import androidx.media3.mpvplayer.MpvAndroidOptions;
import androidx.media3.mpvplayer.MpvPlayer;
import androidx.media3.mpvplayer.MpvPlayerConfig;
import androidx.media3.mpvplayer.MpvSubtitleOptions;
import androidx.media3.ui.SubtitleView;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.player.track.LangUtil;
import com.fongmi.android.tv.player.util.PlayerHelper;
import com.fongmi.android.tv.setting.PlayerSetting;
import com.fongmi.android.tv.setting.PreloadSetting;
import com.fongmi.android.tv.setting.Setting;
import com.github.catvod.utils.Path;

import java.io.File;

public final class MpvUtil {

    private static final String ASSET_CA_FILE = "cacert.pem";
    private static final double DEFAULT_SUB_POS = 100.0;
    private static final double DEFAULT_SUB_SCALE = 1.0;
    private static final double MIN_SUB_SCALE = 0.5;
    private static final double MAX_SUB_SCALE = 3.0;
    private static final double MIN_SUB_POS = 0.0;
    private static final double MAX_SUB_POS = 150.0;
    public static boolean isAvailable() {
        try {
            return MpvPlayer.isAvailable();
        } catch (Throwable e) {
            return false;
        }
    }

    public static MpvPlayer buildPlayer(int decode, Player.Listener listener) {
        MpvPlayer player = new MpvPlayer.Builder(App.get()).setDecode(decode).setConfig(buildConfig()).build();
        player.setTrackSelectionParameters(player.getTrackSelectionParameters().buildUpon().setPreferredTextLanguages(LangUtil.getPreferredTextLanguages()).build());
        player.addListener(listener);
        return player;
    }

    public static void setSubtitleStyle(MpvPlayer player) {
        player.setSubtitleOptions(buildSubtitleOptions());
    }

    private static MpvPlayerConfig buildConfig() {
        MpvPlayerConfig.Builder builder = newConfigBuilder();
        addAndroidOptions(builder);
        addSubtitleStyleOptions(builder);
        return builder.build();
    }

    private static MpvPlayerConfig.Builder newConfigBuilder() {
        return new MpvPlayerConfig.Builder().setDefaultUserAgent(getDefaultUserAgent());
    }

    private static void addAndroidOptions(MpvPlayerConfig.Builder builder) {
        addAndroidDefaultOptions(builder);
        addTlsCaFile(builder);
        addPreloadOptions(builder);
    }

    private static void addAndroidDefaultOptions(MpvPlayerConfig.Builder builder) {
        File configDir = Path.mpv();
        File cacheDir = Path.mpvCache();
        MpvAndroidOptions options = new MpvAndroidOptions.Builder().setShaderCacheDirectory(cacheDir).setGpuNextEnabled(PlayerSetting.isMpvGpuNext()).setVulkanEnabled(PlayerSetting.isMpvVulkan()).build();
        builder.addConfigDirectory(configDir).addAndroidFontConfig(configDir, cacheDir).addAndroidDefaults(options);
    }

    private static void addTlsCaFile(MpvPlayerConfig.Builder builder) {
        builder.addTlsCaFileFromAsset(App.get(), ASSET_CA_FILE, Path.files(ASSET_CA_FILE));
    }

    private static void addPreloadOptions(MpvPlayerConfig.Builder builder) {
        if (!PreloadSetting.isPreload()) return;
        builder.addDiskCacheOptions(Path.mpvCache(), PreloadSetting.getPreloadTimeSeconds());
    }

    private static void addSubtitleStyleOptions(MpvPlayerConfig.Builder builder) {
        builder.addAndroidSubtitleOptions(App.get(), buildSubtitleOptions());
    }

    private static MpvSubtitleOptions buildSubtitleOptions() {
        MpvSubtitleOptions.Builder builder = new MpvSubtitleOptions.Builder();
        if (PlayerSetting.isCaption()) builder.setSystemCaptionStyle();
        if (PlayerSetting.getSubtitlePosition() != 0) builder.setPosition(getSubtitlePosition());
        if (PlayerSetting.getSubtitleTextSize() != 0) builder.setScale(getSubtitleScale());
        return builder.build();
    }

    private static String getDefaultUserAgent() {
        String userAgent = Setting.getUa();
        return TextUtils.isEmpty(userAgent) ? PlayerHelper.getDefaultUa() : userAgent;
    }

    private static double getSubtitlePosition() {
        float position = PlayerSetting.getSubtitlePosition();
        if (position == 0) return DEFAULT_SUB_POS;
        return Util.constrainValue(DEFAULT_SUB_POS - position * 100.0, MIN_SUB_POS, MAX_SUB_POS);
    }

    private static double getSubtitleScale() {
        float textSize = PlayerSetting.getSubtitleTextSize();
        if (textSize == 0) return DEFAULT_SUB_SCALE;
        return Util.constrainValue(textSize / SubtitleView.DEFAULT_TEXT_SIZE_FRACTION, MIN_SUB_SCALE, MAX_SUB_SCALE);
    }
}
