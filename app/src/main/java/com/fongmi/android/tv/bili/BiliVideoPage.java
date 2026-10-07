package com.fongmi.android.tv.bili;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BiliVideoPage {

    public static final int PAGE_SIZE = 15;

    private final List<BiliVideo> items;
    private final boolean hasMore;
    private final int nextPage;
    private final String nextOffset;

    public BiliVideoPage(List<BiliVideo> items, boolean hasMore, int nextPage, String nextOffset) {
        this.items = items == null ? new ArrayList<>() : items;
        this.hasMore = hasMore;
        this.nextPage = nextPage;
        this.nextOffset = nextOffset == null ? "" : nextOffset;
    }

    public static BiliVideoPage empty() {
        return new BiliVideoPage(Collections.emptyList(), false, 1, "");
    }

    public List<BiliVideo> getItems() {
        return items;
    }

    public boolean hasMore() {
        return hasMore;
    }

    public int getNextPage() {
        return nextPage;
    }

    public String getNextOffset() {
        return nextOffset;
    }
}
