package com.fongmi.android.tv.setting;

import android.text.TextUtils;

import com.fongmi.android.tv.utils.Util;
import com.fongmi.android.tv.MxBoxConstants;
import com.github.catvod.utils.Prefers;

public class CastSetting {

    private static final String KEY_ENABLED = "cast_enabled";
    private static final String KEY_DEVICE_NAME = "cast_device_name";

    public static boolean isEnabled() {
        return Prefers.getBoolean(KEY_ENABLED, true);
    }

    public static void putEnabled(boolean enabled) {
        Prefers.put(KEY_ENABLED, enabled);
    }

    public static String getDefaultDeviceName() {
        return MxBoxConstants.APP_NAME + "(" + Util.getDeviceName() + ")";
    }

    public static String getDeviceName() {
        String name = Prefers.getString(KEY_DEVICE_NAME);
        return TextUtils.isEmpty(name) ? getDefaultDeviceName() : name.trim();
    }

    public static void putDeviceName(String name) {
        Prefers.put(KEY_DEVICE_NAME, TextUtils.isEmpty(name) ? getDefaultDeviceName() : name.trim());
    }
}
