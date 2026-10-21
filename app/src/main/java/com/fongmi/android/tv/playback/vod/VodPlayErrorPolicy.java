package com.fongmi.android.tv.playback.vod;

import android.text.TextUtils;

import com.fongmi.android.tv.bean.Result;

import java.util.Locale;

public final class VodPlayErrorPolicy {

    private static final String[] KEYWORDS = {
            "cookie", "网盘", "token", "登录", "登陆", "授权",
            "夸克", "阿里", "百度", "天翼", "115", "uc",
            "pan", "失效", "过期"
    };

    private VodPlayErrorPolicy() {
    }

    public static boolean isRecoverable(String msg) {
        if (TextUtils.isEmpty(msg)) return false;
        String lower = msg.toLowerCase(Locale.ROOT);
        for (String keyword : KEYWORDS) {
            if (lower.contains(keyword.toLowerCase(Locale.ROOT))) return true;
        }
        return false;
    }

    public static boolean shouldFallback(Result result) {
        if (result == null) return false;
        if (!result.hasMsg() && !TextUtils.isEmpty(result.getRealUrl())) return false;
        return isRecoverable(result.getMsg());
    }
}
