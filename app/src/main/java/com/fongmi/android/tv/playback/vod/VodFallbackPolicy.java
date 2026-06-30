package com.fongmi.android.tv.playback.vod;

import androidx.media3.common.C;

import com.fongmi.android.tv.api.config.VodConfig;
import com.fongmi.android.tv.bean.Flag;
import com.fongmi.android.tv.bean.Result;
import com.fongmi.android.tv.bean.Site;
import com.fongmi.android.tv.bean.Vod;

import java.util.ArrayList;
import java.util.List;

public class VodFallbackPolicy {

    private final VodPlaybackController controller;
    private final VodPlaybackState state;
    private final VodPlaybackHost host;

    public VodFallbackPolicy(VodPlaybackController controller, VodPlaybackState state, VodPlaybackHost host) {
        this.controller = controller;
        this.state = state;
        this.host = host;
    }

    public boolean playbackError(String msg) {
        if (tryFallback(msg)) return true;
        host.resetPlaybackForError(msg);
        return false;
    }

    public void emptyFlag() {
        tryFallback("");
    }

    public void emptyDetail() {
        fallbackToNextSource(false);
    }

    public void manualSwitchSource() {
        fallbackToNextSource(true);
    }

    public void search(String keyword, boolean autoFallback) {
        state.setSearchKeyword(keyword);
        state.setAutoFallback(autoFallback);
        state.setSelectFirstSource(autoFallback);
        host.onSearchStarted(keyword);
        host.requestSearch(getSearchableSites(), keyword);
    }

    public void onSearchResult(Result result) {
        List<Vod> items = new ArrayList<>(result.getList());
        items.removeIf(this::mismatch);
        state.setSources(items);
        host.renderSources(state.getSources());
        if (state.isSelectFirstSource()) nextSource();
        if (items.isEmpty()) return;
        host.onSearchResult();
    }

    private boolean tryFallback(String msg) {
        boolean recoverable = VodPlayErrorPolicy.isRecoverable(msg);
        if ((recoverable || host.isSiteChangeable()) && fallbackToNextLine()) return true;
        if (host.isSiteChangeable()) {
            fallbackToNextSource(false);
            return true;
        }
        return false;
    }

    private boolean fallbackToNextLine() {
        int start = state.getFlagPosition() + 1;
        for (int i = start; i < state.getFlags().size(); i++) {
            Flag flag = state.getFlags().get(i);
            if (!flag.hasDirectPlay()) continue;
            return switchFallbackLine(flag);
        }
        for (int i = start; i < state.getFlags().size(); i++) {
            return switchFallbackLine(state.getFlags().get(i));
        }
        return false;
    }

    private boolean switchFallbackLine(Flag flag) {
        if (state.getHistory() != null) state.getHistory().setPosition(C.TIME_UNSET);
        host.showSwitchLine(flag);
        controller.selectFlag(flag);
        return true;
    }

    private void fallbackToNextSource(boolean force) {
        if (!state.hasSources()) search(host.getVodName(), true);
        else if (state.isAutoFallback() || force) nextSource();
    }

    private void nextSource() {
        if (!state.hasSources()) return;
        Vod item = state.removeFirstSource();
        host.renderSources(state.getSources());
        host.showSwitchSource(item);
        state.addFailedId(host.getVodId());
        state.setSelectFirstSource(false);
        controller.fallbackSource(item);
    }

    private List<Site> getSearchableSites() {
        List<Site> sites = new ArrayList<>();
        for (Site site : VodConfig.get().getSites()) if (isPass(site)) sites.add(site);
        return sites;
    }

    private boolean isPass(Site item) {
        if (state.isAutoFallback() && !item.isChangeable()) return false;
        return item.isSearchable();
    }

    private boolean mismatch(Vod item) {
        if (host.getVodId().equals(item.getId())) return true;
        if (state.hasFailedId(item.getId())) return true;
        if (state.isAutoFallback()) return !item.getName().equals(state.getSearchKeyword());
        return !item.getName().contains(state.getSearchKeyword());
    }
}
