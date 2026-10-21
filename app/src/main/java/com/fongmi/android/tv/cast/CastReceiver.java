package com.fongmi.android.tv.cast;

import android.content.Context;
import android.content.Intent;

import com.fongmi.android.tv.setting.CastSetting;
import com.fongmi.android.tv.utils.Util;

public final class CastReceiver {

    private static final String SERVICE = "com.fongmi.android.tv.service.DLNARendererService";

    private CastReceiver() {
    }

    public static void startIfEnabled(Context context) {
        if (!Util.isLeanback() || !CastSetting.isEnabled()) return;
        context.startService(new Intent().setClassName(context, SERVICE));
    }

    public static void stop(Context context) {
        if (!Util.isLeanback()) return;
        context.stopService(new Intent().setClassName(context, SERVICE));
    }

    public static void restart(Context context) {
        stop(context);
        startIfEnabled(context);
    }
}
