package com.fongmi.android.tv.bili;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class BiliStreams {

    private final String videoUrl;
    private final String audioUrl;
    private final List<BiliQn> qualities;
    private final int selectedQn;
    private final Map<String, String> headers;

    public BiliStreams(String videoUrl, String audioUrl, List<BiliQn> qualities, int selectedQn, Map<String, String> headers) {
        this.videoUrl = videoUrl == null ? "" : videoUrl;
        this.audioUrl = audioUrl == null ? "" : audioUrl;
        this.qualities = qualities == null ? new ArrayList<>() : qualities;
        this.selectedQn = selectedQn;
        this.headers = headers == null ? Collections.emptyMap() : headers;
    }

    public String getVideoUrl() {
        return videoUrl;
    }

    public String getAudioUrl() {
        return audioUrl;
    }

    public List<BiliQn> getQualities() {
        return qualities;
    }

    public int getSelectedQn() {
        return selectedQn;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public String labelOf(int qn) {
        for (BiliQn item : qualities) if (item.getValue() == qn) return item.getLabel();
        return BiliQn.fallback(qn);
    }
}
