package com.fongmi.android.tv.bili;

public class BiliQr {

    public enum State {
        WAITING, SCANNED, SUCCESS, EXPIRED, FAILED
    }

    private final State state;
    private final String url;
    private final String key;
    private final String message;

    public BiliQr(State state, String url, String key, String message) {
        this.state = state;
        this.url = url == null ? "" : url;
        this.key = key == null ? "" : key;
        this.message = message == null ? "" : message;
    }

    public State getState() {
        return state;
    }

    public String getUrl() {
        return url;
    }

    public String getKey() {
        return key;
    }

    public String getMessage() {
        return message;
    }
}
