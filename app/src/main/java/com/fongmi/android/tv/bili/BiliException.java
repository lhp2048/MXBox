package com.fongmi.android.tv.bili;

public class BiliException extends Exception {

    private final boolean auth;

    public BiliException(String message, boolean auth) {
        super(message);
        this.auth = auth;
    }

    public boolean isAuth() {
        return auth;
    }
}
