package com.fongmi.android.tv.bili;

import java.util.ArrayList;
import java.util.List;

public class BiliSearchResult {

    private final List<BiliVideo> videos;
    private final List<BiliUp> ups;
    private final boolean videoMore;
    private final boolean upMore;
    private final int nextPage;

    public BiliSearchResult(List<BiliVideo> videos, List<BiliUp> ups, boolean videoMore, boolean upMore, int nextPage) {
        this.videos = videos == null ? new ArrayList<>() : videos;
        this.ups = ups == null ? new ArrayList<>() : ups;
        this.videoMore = videoMore;
        this.upMore = upMore;
        this.nextPage = nextPage;
    }

    public static BiliSearchResult empty(int page) {
        return new BiliSearchResult(new ArrayList<>(), new ArrayList<>(), false, false, page);
    }

    public List<BiliVideo> getVideos() {
        return videos;
    }

    public List<BiliUp> getUps() {
        return ups;
    }

    public boolean hasVideoMore() {
        return videoMore;
    }

    public boolean hasUpMore() {
        return upMore;
    }

    public int getNextPage() {
        return nextPage;
    }
}
