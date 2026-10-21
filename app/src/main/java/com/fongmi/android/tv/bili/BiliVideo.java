package com.fongmi.android.tv.bili;

public class BiliVideo {

    private final String bvid;
    private final String title;
    private final String cover;
    private final long mid;
    private final String author;

    public BiliVideo(String bvid, String title, String cover, long mid, String author) {
        this.bvid = bvid == null ? "" : bvid;
        this.title = title == null ? "" : title;
        this.cover = cover == null ? "" : cover;
        this.mid = mid;
        this.author = author == null ? "" : author;
    }

    public String getBvid() {
        return bvid;
    }

    public String getTitle() {
        return title;
    }

    public String getCover() {
        return cover.startsWith("//") ? "https:" + cover : cover;
    }

    public long getMid() {
        return mid;
    }

    public String getAuthor() {
        return author;
    }

    public boolean isNormal() {
        return bvid.startsWith("BV");
    }
}
