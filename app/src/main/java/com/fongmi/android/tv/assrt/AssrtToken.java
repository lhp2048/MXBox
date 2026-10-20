package com.fongmi.android.tv.assrt;

import com.github.catvod.utils.Prefers;

public final class AssrtToken {

    static final String KEY = "mx_assrt_token";

    private AssrtToken() {
    }

    public static boolean saved() {
        return !get().isEmpty();
    }

    public static String get() {
        String token = Prefers.getString(KEY, "");
        return AssrtRules.validToken(token) ? token : "";
    }

    public static boolean save(String raw) {
        String token = raw == null ? "" : raw.trim();
        if (!AssrtRules.validToken(token)) return false;
        Prefers.put(KEY, token);
        return true;
    }
}
