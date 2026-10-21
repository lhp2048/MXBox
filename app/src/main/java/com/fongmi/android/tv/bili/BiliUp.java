package com.fongmi.android.tv.bili;

public class BiliUp {

    private final long mid;
    private final String name;
    private final String face;

    public BiliUp(long mid, String name, String face) {
        this.mid = mid;
        this.name = name == null ? "" : name;
        this.face = face == null ? "" : face;
    }

    public long getMid() {
        return mid;
    }

    public String getName() {
        return name;
    }

    public String getFace() {
        return face.startsWith("//") ? "https:" + face : face;
    }
}
