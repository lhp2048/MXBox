package com.fongmi.android.tv.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.fongmi.android.tv.App;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.bili.BiliApi;
import com.fongmi.android.tv.bili.BiliException;
import com.fongmi.android.tv.bili.BiliFavoriteStore;
import com.fongmi.android.tv.bili.BiliPlayback;
import com.fongmi.android.tv.bili.BiliQn;
import com.fongmi.android.tv.bili.BiliQr;
import com.fongmi.android.tv.bili.BiliSearchResult;
import com.fongmi.android.tv.bili.BiliSession;
import com.fongmi.android.tv.bili.BiliStreams;
import com.fongmi.android.tv.bili.BiliUp;
import com.fongmi.android.tv.bili.BiliVideo;
import com.fongmi.android.tv.bili.BiliVideoPage;
import com.fongmi.android.tv.databinding.ActivityBiliBinding;
import com.fongmi.android.tv.event.ServerEvent;
import com.fongmi.android.tv.server.Server;
import com.fongmi.android.tv.ui.adapter.BiliKeyAdapter;
import com.fongmi.android.tv.ui.adapter.BiliUpAdapter;
import com.fongmi.android.tv.ui.adapter.BiliVideoAdapter;
import com.fongmi.android.tv.ui.adapter.BiliWordAdapter;
import com.fongmi.android.tv.ui.custom.SpaceItemDecoration;
import com.fongmi.android.tv.ui.base.BaseActivity;
import com.fongmi.android.tv.utils.Notify;
import com.fongmi.android.tv.utils.Util;
import com.fongmi.android.tv.utils.QRCode;
import com.fongmi.android.tv.utils.Task;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class BiliActivity extends BaseActivity implements Player.Listener, BiliKeyAdapter.Listener, BiliWordAdapter.Listener {

    private static final int SEARCH = 1;
    private static final int SETTING = 2;
    private static final int TABS = 3;
    private static final int LIST = 4;
    private static final int SEEK = 5;
    private static final int FAV = 6;
    private static final int MORE = 7;
    private static final int QUALITY = 8;
    private static final int RESULT_TAB = 9;
    private static final int RESULT_LIST = 10;
    private static final int CODE = 11;
    private static final int RESULT_CODE = 12;
    private static final int KEYS = 13;
    private static final int WORDS = 14;
    private static final int RESULT_SPAN = 4;
    private static final int RESULT_VIDEO = 0;
    private static final int RESULT_UP = 1;
    private static final int RECOMMEND = 0;
    private static final int FOLLOW = 1;
    private static final int FAVORITE = 2;
    private static final int KIND_RECOMMEND = 0;
    private static final int KIND_FOLLOW = 1;
    private static final int KIND_FAVORITE = 2;
    private static final int KIND_SPACE = 3;
    private static final int KIND_SEARCH = 4;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable idleTask = this::onIdle;
    private final Runnable tickTask = this::tick;
    private final Runnable hideMetaTask = this::hideMeta;
    private final Runnable previewTask = this::startResultPreview;
    private final List<BiliVideo> queue = new ArrayList<>();
    private final List<BiliVideo> recommendCache = new ArrayList<>();
    private final List<BiliVideo> gridItems = new ArrayList<>();
    private final List<BiliVideo> searchVideos = new ArrayList<>();
    private final List<BiliUp> searchUps = new ArrayList<>();
    private final List<BiliQn> qualities = new ArrayList<>();

    private ActivityBiliBinding binding;
    private BiliPlayback playback;
    private BiliVideoAdapter listAdapter;
    private BiliVideoAdapter gridAdapter;
    private BiliVideoAdapter searchVideoAdapter;
    private BiliUpAdapter searchUpAdapter;
    private int channel = RECOMMEND;
    private int kind = KIND_RECOMMEND;
    private int focus = TABS;
    private int index;
    private int playingIndex;
    private int playingKind = KIND_RECOMMEND;
    private BiliVideo playingVideo;
    private int page = 1;
    private int gridPage = 1;
    private int searchPage = 1;
    private int resultKind = RESULT_VIDEO;
    private int resultIndex;
    private boolean searchSelectAppended;
    private int selectedQn;
    private int loadToken;
    private int playToken;
    private int playingToken;
    private long spaceMid;
    private String offset = "";
    private String keyword = "";
    private String searchQuery = "";
    private String qrKey = "";
    private boolean hasMore;
    private boolean gridMore;
    private boolean searchVideoMore;
    private boolean searchUpMore;
    private boolean loading;
    private boolean polling;
    private boolean typing;
    private boolean playbackReady;
    private boolean recommendLoaded;
    private boolean resultsOpen;
    private int boardLayoutTries;
    private boolean selectAppended;
    private int recommendIndex;
    private int recommendPage = 1;
    private boolean recommendHasMore;
    private int pendingIndex;
    private int wordIndex = -1;
    private int suggestToken;
    private BiliWordAdapter wordAdapter;
    private final List<String> words = new ArrayList<>();
    private boolean previewing;
    private boolean previewWindow;
    private boolean searchPaused;
    private long searchPausePosition;
    private boolean upFromSearch;
    private int previewIndex = -1;
    private int previewToken;
    private BiliVideo previewRestoreVideo;
    private int previewRestoreIndex;
    private int previewRestoreKind;
    private long previewRestorePosition;
    private static boolean memoryReady;
    private static int memoryChannel = RECOMMEND;
    private static int memoryIndex;

    public static void start(Context context) {
        context.startActivity(new Intent(context, BiliActivity.class));
    }

    @Override
    protected ActivityBiliBinding getBinding() {
        return binding = ActivityBiliBinding.inflate(getLayoutInflater());
    }

    @Override
    protected boolean customWall() {
        return false;
    }

    @Override
    protected void initView(Bundle savedInstanceState) {
        playback = new BiliPlayback(this);
        binding.player.setPlayer(playback.player());
        listAdapter = new BiliVideoAdapter(R.layout.item_bili_row, this::onListClick, true);
        gridAdapter = new BiliVideoAdapter(R.layout.item_bili_space, this::onGridClick);
        searchVideoAdapter = new BiliVideoAdapter(R.layout.item_bili_card, this::onSearchVideo, true);
        searchUpAdapter = new BiliUpAdapter(this::onSearchUp);
        binding.list.setLayoutManager(new LinearLayoutManager(this));
        binding.list.setAdapter(listAdapter);
        binding.grid.setLayoutManager(new LinearLayoutManager(this));
        binding.grid.setAdapter(gridAdapter);
        binding.searchList.setLayoutManager(new GridLayoutManager(this, RESULT_SPAN));
        binding.searchList.setAdapter(searchVideoAdapter);
        wordAdapter = new BiliWordAdapter(this);
        binding.biliWordList.setLayoutManager(new LinearLayoutManager(this));
        binding.biliWordList.setAdapter(wordAdapter);
        binding.biliKeys.setLayoutManager(new GridLayoutManager(this, 6));
        binding.biliKeys.addItemDecoration(new SpaceItemDecoration(6, 8));
        binding.biliKeys.setAdapter(new BiliKeyAdapter(this));
        binding.grid.addOnScrollListener(new EndScroll(() -> { if (gridMore) loadGrid(true); }));
        binding.searchList.addOnScrollListener(new EndScroll(() -> { if (resultHasMore()) loadSearch(true); }));
        binding.position.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) playback.player().seekTo(progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                bumpIdle();
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                playback.player().seekTo(seekBar.getProgress());
            }
        });
        binding.search.setShowSoftInputOnFocus(false);
        binding.search.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN)) {
                stopTyping();
                searchQuery = v.getText().toString().trim();
                openBiliSearch();
                if (!searchQuery.isEmpty()) submitSearch();
                return true;
            }
            return false;
        });
        focus(TABS);
        if (BiliSession.isLoggedIn()) enter();
        else showQr();
    }

    private void enter() {
        Task.execute(() -> {
            try {
                BiliApi.refreshNav();
                App.post(() -> { if (!isFinishing()) openRemembered(); });
            } catch (IOException e) {
                App.post(() -> { if (!isFinishing()) showQr(); });
            }
        });
    }

    private void showQr() {
        playback.stop();
        hide(binding.browse);
        hide(binding.control);
        hideMeta();
        hide(binding.gridPanel);
        hide(binding.searchPanel);
        hide(binding.searchQr);
        show(binding.qr);
        binding.qrText.setText(R.string.bili_qr);
        polling = true;
        Task.execute(() -> {
            try {
                BiliQr qr = BiliApi.generateQr();
                App.post(() -> {
                    if (isFinishing()) return;
                    qrKey = qr.getKey();
                    binding.qrImage.setImageBitmap(QRCode.getBitmap(qr.getUrl(), 220, 1));
                    schedulePoll();
                });
            } catch (IOException e) {
                App.post(() -> Notify.show(getString(R.string.bili_load_fail)));
            }
        });
    }

    private void schedulePoll() {
        handler.postDelayed(this::pollQr, 2000);
    }

    private void pollQr() {
        if (!polling || qrKey.isEmpty() || isFinishing()) return;
        String key = qrKey;
        Task.execute(() -> {
            try {
                BiliQr qr = BiliApi.pollQr(key);
                App.post(() -> applyPoll(qr));
            } catch (IOException e) {
                App.post(this::schedulePoll);
            }
        });
    }

    private void applyPoll(BiliQr qr) {
        if (!polling || isFinishing() || !qrKey.equals(qr.getKey())) return;
        if (qr.getState() == BiliQr.State.SUCCESS) {
            polling = false;
            hide(binding.qr);
            openChannel(RECOMMEND, 0, true);
            return;
        }
        if (qr.getState() == BiliQr.State.EXPIRED) {
            showQr();
            return;
        }
        if (qr.getState() == BiliQr.State.SCANNED) binding.qrText.setText(R.string.bili_qr_scanned);
        schedulePoll();
    }

    private void openRemembered() {
        if (memoryReady) openChannel(memoryChannel, memoryIndex, true);
        else openChannel(RECOMMEND, 0, true);
    }

    private void openChannel(int channel, int restoreIndex, boolean play) {
        if (kind == KIND_RECOMMEND && channel != RECOMMEND) snapshotRecommend();
        this.channel = channel;
        kind = channel;
        if (channel == RECOMMEND && recommendLoaded) {
            restoreRecommend();
            return;
        }
        page = 1;
        offset = "";
        index = 0;
        pendingIndex = play ? Math.max(0, restoreIndex) : 0;
        loading = false;
        queue.clear();
        paintTabs();
        showBrowse();
        loadQueue(false, play);
    }

    private void snapshotRecommend() {
        if (kind != KIND_RECOMMEND) return;
        recommendCache.clear();
        recommendCache.addAll(queue);
        recommendIndex = index;
        recommendPage = page;
        recommendHasMore = hasMore;
        recommendLoaded = !recommendCache.isEmpty();
    }

    private void restoreRecommend() {
        page = recommendPage;
        hasMore = recommendHasMore;
        index = recommendCache.isEmpty() ? 0 : Math.min(recommendIndex, recommendCache.size() - 1);
        pendingIndex = 0;
        loading = false;
        queue.clear();
        queue.addAll(recommendCache);
        listAdapter.setItems(queue);
        listAdapter.setSelected(index);
        binding.empty.setText(queue.isEmpty() ? emptyText() : "");
        binding.empty.setVisibility(queue.isEmpty() ? View.VISIBLE : View.GONE);
        paintTabs();
        showBrowse();
    }

    private void refreshRecommend() {
        recommendLoaded = false;
        recommendCache.clear();
        openChannel(RECOMMEND, 0, false);
    }

    private void loadQueue(boolean append, boolean playFirst) {
        if (loading) return;
        loading = true;
        int token = ++loadToken;
        int requestPage = append ? page : 1;
        String requestOffset = append ? offset : "";
        int requestKind = kind;
        long requestMid = spaceMid;
        String requestKeyword = keyword;
        Task.execute(() -> {
            try {
                BiliVideoPage result = fetch(requestKind, requestPage, requestOffset, requestMid, requestKeyword);
                App.post(() -> applyQueue(token, result, append, playFirst));
            } catch (IOException e) {
                App.post(() -> failLoad(token, e));
            }
        });
    }

    private BiliVideoPage fetch(int requestKind, int requestPage, String requestOffset, long requestMid, String requestKeyword) throws IOException {
        return switch (requestKind) {
            case KIND_FOLLOW -> BiliApi.follow(requestOffset);
            case KIND_FAVORITE -> BiliFavoriteStore.page(requestPage);
            case KIND_SPACE -> BiliApi.space(requestMid, requestPage);
            case KIND_SEARCH -> searchPage(requestKeyword, requestPage);
            default -> BiliApi.recommend(requestPage);
        };
    }

    private BiliVideoPage searchPage(String requestKeyword, int requestPage) throws IOException {
        BiliSearchResult result = BiliApi.search(requestKeyword, requestPage);
        return new BiliVideoPage(result.getVideos(), result.hasVideoMore(), result.getNextPage(), "");
    }

    private void applyQueue(int token, BiliVideoPage result, boolean append, boolean playFirst) {
        if (token != loadToken || isFinishing()) return;
        loading = false;
        hasMore = result.hasMore();
        page = result.getNextPage();
        offset = result.getNextOffset();
        int before = queue.size();
        if (!append) {
            queue.clear();
            selectAppended = false;
        }
        addQueue(result.getItems());
        boolean landed = false;
        if (selectAppended) {
            selectAppended = false;
            if (append && queue.size() > before) {
                index = before;
                landed = true;
                if (kind == KIND_RECOMMEND) recommendIndex = index;
            }
        }
        listAdapter.setItems(queue);
        listAdapter.setSelected(index);
        if (landed) binding.list.scrollToPosition(index);
        binding.empty.setText(queue.isEmpty() ? emptyText() : "");
        binding.empty.setVisibility(queue.isEmpty() ? View.VISIBLE : View.GONE);
        binding.list.setVisibility(queue.isEmpty() || isGone(binding.browse) ? View.GONE : View.VISIBLE);
        if (kind == KIND_RECOMMEND) snapshotRecommend();
        if (playFirst && !queue.isEmpty()) {
            if (pendingIndex >= queue.size() && hasMore) {
                loadQueue(true, true);
                return;
            }
            int at = Math.min(pendingIndex, queue.size() - 1);
            pendingIndex = 0;
            playIndex(at, 0);
        } else if (queue.isEmpty()) {
            Notify.show(emptyText());
        }
    }

    private void addQueue(List<BiliVideo> items) {
        for (BiliVideo video : items) {
            if (kind == KIND_RECOMMEND && hasBvid(video.getBvid())) continue;
            queue.add(video);
        }
    }

    private boolean hasBvid(String bvid) {
        for (BiliVideo video : queue) if (video.getBvid().equals(bvid)) return true;
        return false;
    }

    private void failLoad(int token, IOException e) {
        if (token != loadToken || isFinishing()) return;
        loading = false;
        selectAppended = false;
        if ("auth".equals(e.getMessage())) showQr();
        else Notify.show(getString(R.string.bili_load_fail));
    }

    private String emptyText() {
        if (channel == FOLLOW) return getString(R.string.bili_empty_follow);
        if (channel == FAVORITE) return getString(R.string.bili_empty_fav);
        return getString(R.string.bili_empty);
    }

    private void moveList(int delta) {
        int next = index + delta;
        if (next < 0) {
            focus(TABS);
            return;
        }
        if (next >= queue.size()) {
            if (delta > 0 && hasMore) {
                selectAppended = true;
                loadQueue(true, false);
            }
            return;
        }
        index = next;
        if (kind == KIND_RECOMMEND) recommendIndex = index;
        listAdapter.setSelected(index);
        binding.list.scrollToPosition(index);
        if (next >= queue.size() - 1 && hasMore) loadQueue(true, false);
    }

    private void playIndex(int target, long position) {
        if (target < 0 || target >= queue.size()) {
            if (target >= queue.size() && hasMore) loadQueue(true, true);
            return;
        }
        boolean followHighlight = kind == playingKind && index == playingIndex;
        playingKind = kind;
        playingIndex = target;
        playingVideo = queue.get(target);
        if (followHighlight) {
            index = target;
            if (kind == KIND_RECOMMEND) recommendIndex = index;
            listAdapter.setSelected(index);
            binding.list.scrollToPosition(index);
        }
        resolvePlay(playingVideo, position);
        if (target >= queue.size() - 1 && hasMore) loadQueue(true, false);
    }

    private void replayPlaying(long position) {
        if (playingVideo == null) return;
        resolvePlay(playingVideo, position);
    }

    private void resolvePlay(BiliVideo video, long position) {
        int token = ++playToken;
        Task.execute(() -> {
            try {
                BiliStreams streams = BiliApi.resolve(video.getBvid(), BiliSession.wantedQn());
                App.post(() -> startPlay(token, streams, position, video));
            } catch (BiliException e) {
                App.post(() -> Notify.show(getString(R.string.bili_unsupported)));
            } catch (IOException e) {
                App.post(() -> {
                    if ("auth".equals(e.getMessage())) showQr();
                    else Notify.show(getString(R.string.bili_play_fail));
                });
            }
        });
    }

    private void startPlay(int token, BiliStreams streams, long position, BiliVideo video) {
        if (token != playToken || isFinishing()) return;
        if (previewing && token == previewToken) {
            playbackReady = false;
            playingToken = token;
            playback.play(streams, 0);
            return;
        }
        qualities.clear();
        qualities.addAll(streams.getQualities());
        selectedQn = streams.getSelectedQn();
        BiliSession.wantedQn(selectedQn);
        binding.quality.setText(streams.labelOf(selectedQn));
        updateFav();
        playbackReady = false;
        playingToken = token;
        Log.i(BiliPlayback.TAG, "start " + video.getBvid() + " pos=" + position);
        playback.play(streams, position);
        if (searchPaused && isVisible(binding.searchPanel)) playback.pause();
        if (position == 0 && !searchPaused) showMeta(video);
    }

    private void showMeta(BiliVideo video) {
        binding.metaTitle.setText(video.getTitle());
        binding.metaAuthor.setText(video.getAuthor());
        binding.metaAuthor.setVisibility(video.getAuthor().isEmpty() ? View.GONE : View.VISIBLE);
        show(binding.meta);
        handler.removeCallbacks(hideMetaTask);
        handler.postDelayed(hideMetaTask, 4000);
    }

    private void hideMeta() {
        handler.removeCallbacks(hideMetaTask);
        hide(binding.meta);
    }

    private void updateFav() {
        if (playingVideo == null) return;
        boolean fav = BiliFavoriteStore.isFavorite(playingVideo.getBvid());
        binding.fav.setText(fav ? R.string.bili_unfav : R.string.bili_fav);
    }

    private void toggleFav() {
        if (playingVideo == null) return;
        boolean now = BiliFavoriteStore.toggle(playingVideo);
        binding.fav.setText(now ? R.string.bili_unfav : R.string.bili_fav);
        if (channel == FAVORITE && kind == KIND_FAVORITE) openChannel(FAVORITE, 0, false);
    }

    private void showBrowse() {
        hide(binding.control);
        hide(binding.gridPanel);
        hide(binding.searchPanel);
        hide(binding.qr);
        show(binding.browse);
        paintTabs();
        focus(TABS);
        bumpIdle();
    }

    private void showControl() {
        hide(binding.browse);
        show(binding.control);
        focus(SEEK);
        tick();
        bumpIdle();
    }

    private void showPure() {
        hide(binding.browse);
        hide(binding.control);
        hide(binding.gridPanel);
        hide(binding.searchPanel);
    }

    private void paintTabs() {
        binding.tabRecommend.setSelected(channel == RECOMMEND);
        binding.tabFollow.setSelected(channel == FOLLOW);
        binding.tabFavorite.setSelected(channel == FAVORITE);
        binding.list.setVisibility(queue.isEmpty() || isGone(binding.browse) ? View.GONE : View.VISIBLE);
    }

    private void focus(int target) {
        if (target == LIST && queue.isEmpty()) target = TABS;
        if (target == SEARCH) unlockSearch();
        else lockSearch();
        focus = target;
        listAdapter.setArmed(target == LIST);
        if (target == LIST) {
            binding.list.scrollToPosition(index);
            if (binding.list.requestFocus()) return;
            focus = TABS;
            listAdapter.setArmed(false);
            currentTab().requestFocus();
            return;
        }
        targetView(target).requestFocus();
    }

    private View targetView(int target) {
        return switch (target) {
            case SEARCH -> binding.search;
            case CODE -> binding.searchCode;
            case SETTING -> binding.setting;
            case LIST -> binding.list;
            case FAV -> binding.fav;
            case MORE -> binding.more;
            case QUALITY -> binding.quality;
            case SEEK -> binding.position;
            default -> currentTab();
        };
    }

    private void focusGrid() {
        binding.grid.scrollToPosition(0);
        focusGridItem(4);
    }

    private void focusGridItem(int tries) {
        binding.grid.post(() -> {
            View focused = binding.grid.findFocus();
            if (isFinishing() || !isVisible(binding.gridPanel) || gridAdapter.getItemCount() == 0 || (focused != null && focused != binding.grid)) return;
            RecyclerView.ViewHolder holder = binding.grid.findViewHolderForAdapterPosition(0);
            if (holder != null) {
                holder.itemView.requestFocus();
                return;
            }
            if (tries > 0) focusGridItem(tries - 1);
        });
    }

    private void unlockSearch() {
        binding.search.setFocusable(true);
        binding.search.setFocusableInTouchMode(true);
    }

    private void lockSearch() {
        stopTyping();
        binding.search.setFocusable(false);
        binding.search.setFocusableInTouchMode(false);
    }

    private void startTyping() {
        typing = true;
        unlockSearch();
        binding.search.requestFocus();
        InputMethodManager manager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (manager != null) manager.showSoftInput(binding.search, InputMethodManager.SHOW_IMPLICIT);
    }

    private void stopTyping() {
        if (!typing && binding.search.getWindowToken() == null) return;
        typing = false;
        InputMethodManager manager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (manager != null) manager.hideSoftInputFromWindow(binding.search.getWindowToken(), 0);
    }

    private TextView currentTab() {
        if (channel == FOLLOW) return binding.tabFollow;
        if (channel == FAVORITE) return binding.tabFavorite;
        return binding.tabRecommend;
    }

    private void switchTab(int delta) {
        int next = channel + delta;
        if (next < RECOMMEND || next > FAVORITE || next == channel) return;
        openChannel(next, 0, false);
    }

    private void openMore() {
        if (playingVideo == null) return;
        openSpace(playingVideo.getMid(), playingVideo.getAuthor(), "", false);
    }

    private void openSpace(long mid, String name, String face, boolean fromSearch) {
        if (mid <= 0) return;
        if (fromSearch) endPreview(true);
        upFromSearch = fromSearch;
        spaceMid = mid;
        gridItems.clear();
        gridPage = 1;
        gridMore = false;
        binding.gridTitle.setText(name == null || name.isEmpty() ? getString(R.string.bili_more) : name);
        if (face == null || face.isEmpty()) {
            binding.upFace.setVisibility(View.GONE);
        } else {
            binding.upFace.setVisibility(View.VISIBLE);
            Glide.with(binding.upFace).load(face).circleCrop().into(binding.upFace);
        }
        hide(binding.searchPanel);
        hide(binding.browse);
        hide(binding.control);
        show(binding.gridPanel);
        loadGrid(false);
    }

    private void loadGrid(boolean append) {
        if (loading) return;
        loading = true;
        int requestPage = append ? gridPage : 1;
        long mid = spaceMid;
        Task.execute(() -> {
            try {
                BiliVideoPage result = BiliApi.space(mid, requestPage);
                App.post(() -> {
                    loading = false;
                    if (!append) gridItems.clear();
                    gridItems.addAll(result.getItems());
                    gridMore = result.hasMore();
                    gridPage = result.getNextPage();
                    gridAdapter.setItems(gridItems);
                    if (!append) focusGrid();
                });
            } catch (IOException e) {
                App.post(() -> {
                    loading = false;
                    if ("auth".equals(e.getMessage())) showQr();
                    else Notify.show(getString(R.string.bili_load_fail));
                });
            }
        });
    }

    private void onGridClick(int position) {
        if (position < 0 || position >= gridItems.size()) return;
        if (kind == KIND_RECOMMEND) snapshotRecommend();
        searchPaused = false;
        if (upFromSearch) clearSearchInput();
        upFromSearch = false;
        queue.clear();
        queue.addAll(gridItems);
        kind = KIND_SPACE;
        hasMore = gridMore;
        page = gridPage;
        listAdapter.setItems(queue);
        hide(binding.gridPanel);
        hide(binding.searchPanel);
        showPure();
        playIndex(position, 0);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onServerEvent(ServerEvent event) {
        if (event.type() != ServerEvent.Type.BILI || isFinishing()) return;
        String word = event.text() == null ? "" : event.text().trim();
        if (word.isEmpty()) return;
        searchQuery = word;
        binding.search.setText(word);
        stopTyping();
        if (!BiliSession.isLoggedIn()) {
            Notify.show(getString(R.string.bili_qr));
            return;
        }
        hide(binding.gridPanel);
        openBiliSearch();
        submitSearch();
    }

    private void openBiliSearch() {
        stopTyping();
        pauseForSearch();
        show(binding.searchPanel);
        hide(binding.browse);
        hide(binding.control);
        hide(binding.gridPanel);
        resultsOpen = false;
        boardLayoutTries = 0;
        binding.biliBoard.setTranslationX(0f);
        binding.biliResultPane.setTranslationX(binding.searchPanel.getWidth() > 0 ? binding.searchPanel.getWidth() : 4000f);
        binding.biliResultPane.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
        binding.biliBoard.post(this::layoutBiliBoard);
        paintQuery();
        showWords();
        focusBiliKeys();
    }

    private void pauseForSearch() {
        if (!searchPaused && playback.player().isPlaying()) {
            searchPaused = true;
            searchPausePosition = playback.player().getCurrentPosition();
        }
        playback.pause();
    }

    private void resumeFromSearch() {
        if (!searchPaused) return;
        searchPaused = false;
        int state = playback.player().getPlaybackState();
        if (playingVideo != null && state != Player.STATE_READY && state != Player.STATE_BUFFERING) {
            resolvePlay(playingVideo, searchPausePosition);
            return;
        }
        playback.resume();
    }

    private void submitSearch() {
        String text = searchQuery.trim();
        if (text.isEmpty()) return;
        keyword = text;
        binding.search.setText(text);
        BiliSession.rememberSearch(text);
        searchPage = 1;
        resultKind = RESULT_VIDEO;
        resultIndex = 0;
        searchVideos.clear();
        searchUps.clear();
        binding.searchList.setAdapter(searchVideoAdapter);
        loadSearch(false);
        binding.biliBoard.post(() -> showBiliResults(true));
    }

    private void paintQuery() {
        binding.biliQuery.setText(searchQuery.isEmpty() ? getString(R.string.bili_search_hint) : searchQuery);
        binding.search.setText(searchQuery);
    }

    private void clearSearchInput() {
        searchQuery = "";
        paintQuery();
    }

    private void showWords() {
        paintQuery();
        if (searchQuery.isEmpty()) {
            suggestToken++;
            binding.biliWordTitle.setText(R.string.bili_search_history);
            words.clear();
            words.addAll(BiliSession.searchHistory());
            wordIndex = words.isEmpty() ? -1 : Math.min(wordIndex < 0 ? 0 : wordIndex, words.size() - 1);
            wordAdapter.setItems(words);
            wordAdapter.setSelected(wordIndex);
            wordAdapter.setArmed(focus == WORDS);
            return;
        }
        binding.biliWordTitle.setText(R.string.bili_search_suggest);
        int token = ++suggestToken;
        String term = searchQuery;
        Task.execute(() -> {
            try {
                List<String> items = BiliApi.suggest(term);
                App.post(() -> applySuggest(token, items));
            } catch (IOException ignored) {
            }
        });
    }

    private void applySuggest(int token, List<String> items) {
        if (token != suggestToken || isFinishing()) return;
        words.clear();
        words.addAll(items);
        wordIndex = words.isEmpty() ? -1 : 0;
        wordAdapter.setItems(words);
        wordAdapter.setSelected(wordIndex);
        wordAdapter.setArmed(focus == WORDS);
    }

    private int resultShift() {
        return binding.biliKeyPane.getWidth() + binding.biliWordPane.getWidth();
    }

    private void layoutBiliBoard() {
        int shift = resultShift();
        if (shift <= 0) {
            if (boardLayoutTries++ < 10 && isVisible(binding.searchPanel)) binding.biliBoard.post(this::layoutBiliBoard);
            else boardLayoutTries = 0;
            return;
        }
        boardLayoutTries = 0;
        if (!resultsOpen) binding.biliResultPane.setTranslationX(shift);
    }

    private void showBiliResults(boolean open) {
        int shift = resultShift();
        if (shift <= 0) {
            if (boardLayoutTries++ < 10 && isVisible(binding.searchPanel)) binding.biliBoard.post(() -> showBiliResults(open));
            else boardLayoutTries = 0;
            return;
        }
        boardLayoutTries = 0;
        if (resultsOpen == open) {
            binding.biliResultPane.setTranslationX(open ? 0f : shift);
            if (open) focusResultTab();
            else focusBiliWords();
            return;
        }
        resultsOpen = open;
        binding.biliResultPane.setDescendantFocusability(open ? ViewGroup.FOCUS_AFTER_DESCENDANTS : ViewGroup.FOCUS_BLOCK_DESCENDANTS);
        DecelerateInterpolator interpolator = new DecelerateInterpolator();
        binding.biliBoard.animate().translationX(open ? -shift : 0f).setDuration(280).setInterpolator(interpolator).start();
        binding.biliResultPane.animate().translationX(open ? 0f : shift).setDuration(280).setInterpolator(interpolator).withEndAction(() -> {
            if (!open || isFinishing()) return;
            binding.searchList.requestLayout();
        }).start();
        if (open) binding.searchList.requestLayout();
        if (open) focusResultTab();
        else {
            endPreview(true);
            focusBiliWords();
        }
    }

    private void focusBiliKeys() {
        focus = KEYS;
        wordAdapter.setArmed(false);
        binding.biliKeys.post(() -> {
            View child = binding.biliKeys.getChildAt(0);
            if (child != null) child.requestFocus();
        });
    }

    private void focusBiliWords() {
        if (words.isEmpty()) {
            focusBiliKeys();
            return;
        }
        focus = WORDS;
        if (wordIndex < 0) wordIndex = 0;
        wordAdapter.setArmed(true);
        wordAdapter.setSelected(wordIndex);
        binding.biliWordList.scrollToPosition(wordIndex);
        binding.biliWordList.requestFocus();
    }

    private void selectWord(int next) {
        wordIndex = next;
        wordAdapter.setSelected(wordIndex);
        binding.biliWordList.scrollToPosition(wordIndex);
    }

    @Override
    public void onText(String text) {
        if (text.isEmpty()) searchQuery = "";
        else if (searchQuery.length() < 30) searchQuery += text;
        showWords();
    }

    @Override
    public void onBackspace() {
        if (searchQuery.isEmpty()) return;
        searchQuery = searchQuery.substring(0, searchQuery.length() - 1);
        showWords();
    }

    @Override
    public void onSearchKey() {
        submitSearch();
    }

    @Override
    public void onClick(String text) {
        searchQuery = text;
        paintQuery();
        submitSearch();
    }

    private void loadSearch(boolean append) {
        if (loading) return;
        loading = true;
        int requestPage = append ? searchPage : 1;
        String requestKeyword = keyword;
        Task.execute(() -> {
            try {
                BiliSearchResult result = BiliApi.search(requestKeyword, requestPage);
                App.post(() -> {
                    loading = false;
                    if (!append) {
                        searchVideos.clear();
                        searchUps.clear();
                    }
                    searchVideos.addAll(result.getVideos());
                    searchUps.addAll(result.getUps());
                    searchVideoMore = result.hasVideoMore();
                    searchUpMore = result.hasUpMore();
                    searchPage = result.getNextPage();
                    int videoBefore = append ? searchVideos.size() - result.getVideos().size() : 0;
                    int upBefore = append ? searchUps.size() - result.getUps().size() : 0;
                    searchVideoAdapter.setItems(searchVideos);
                    searchUpAdapter.setItems(searchUps);
                    boolean jump = searchSelectAppended;
                    searchSelectAppended = false;
                    if (!append) resultIndex = 0;
                    else if (jump) {
                        int before = resultKind == RESULT_VIDEO ? videoBefore : upBefore;
                        if (resultCount() > before) resultIndex = before;
                    }
                    if (resultIndex >= resultCount()) resultIndex = Math.max(0, resultCount() - 1);
                    paintResult();
                    if (!append) focusResultTab();
                    else if (jump && focus == RESULT_LIST) {
                        binding.searchList.scrollToPosition(resultIndex);
                        nudgePreview();
                    }
                });
            } catch (IOException e) {
                App.post(() -> {
                    loading = false;
                    searchSelectAppended = false;
                    if ("auth".equals(e.getMessage())) showQr();
                    else Notify.show(getString(R.string.bili_load_fail));
                });
            }
        });
    }

    private void showResultKind(int kind) {
        if (kind != RESULT_VIDEO) endPreview(true);
        resultKind = kind;
        resultIndex = 0;
        binding.searchList.setAdapter(kind == RESULT_VIDEO ? searchVideoAdapter : searchUpAdapter);
        paintResult();
        if (focus == RESULT_LIST && resultCount() > 0) focusResultList();
        else focusResultTab();
    }

    private void paintResult() {
        binding.searchTabVideo.setSelected(resultKind == RESULT_VIDEO);
        binding.searchTabUp.setSelected(resultKind == RESULT_UP);
        boolean onList = focus == RESULT_LIST;
        searchVideoAdapter.setSelected(resultKind == RESULT_VIDEO ? resultIndex : -1);
        searchUpAdapter.setSelected(resultKind == RESULT_UP ? resultIndex : -1);
        searchVideoAdapter.setArmed(onList && resultKind == RESULT_VIDEO);
        searchUpAdapter.setArmed(onList && resultKind == RESULT_UP);
        boolean empty = resultCount() == 0;
        binding.searchEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        binding.searchList.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    private void focusResultTab() {
        endPreview(true);
        focus = RESULT_TAB;
        paintResult();
        (resultKind == RESULT_VIDEO ? binding.searchTabVideo : binding.searchTabUp).requestFocus();
    }

    private void focusResultCode() {
        focus = RESULT_CODE;
        paintResult();
        binding.searchPanelCode.requestFocus();
    }

    private void showSearchQr() {
        binding.searchQrImage.setImageBitmap(QRCode.getBitmap(Server.get().getAddress(1) + "&target=bili", 220, 1));
        show(binding.searchQr);
        binding.searchQr.bringToFront();
        bumpIdle();
    }

    private void focusResultList() {
        if (resultCount() == 0) return;
        focus = RESULT_LIST;
        paintResult();
        binding.searchList.scrollToPosition(resultIndex);
        binding.searchList.requestFocus();
        nudgePreview();
    }

    private void moveResultTo(int next) {
        if (next < 0 || next >= resultCount()) return;
        resultIndex = next;
        paintResult();
        binding.searchList.scrollToPosition(resultIndex);
        nudgePreview();
    }

    private void moveResultHorizontal(int delta) {
        int column = resultIndex % RESULT_SPAN;
        if (delta < 0 && column == 0) {
            showBiliResults(false);
            return;
        }
        if (delta > 0 && (column == RESULT_SPAN - 1 || resultIndex + 1 >= resultCount())) return;
        moveResultTo(resultIndex + delta);
    }

    private void moveResultUp() {
        if (resultIndex < RESULT_SPAN) {
            focusResultTab();
            return;
        }
        moveResultTo(resultIndex - RESULT_SPAN);
    }

    private void moveResultDown() {
        int count = resultCount();
        int next = resultIndex + RESULT_SPAN;
        if (next < count) {
            moveResultTo(next);
            return;
        }
        int lastRowStart = count == 0 ? 0 : ((count - 1) / RESULT_SPAN) * RESULT_SPAN;
        if (resultIndex < lastRowStart) {
            moveResultTo(count - 1);
            return;
        }
        if (resultHasMore()) {
            searchSelectAppended = true;
            loadSearch(true);
        }
    }

    private int resultCount() {
        return resultKind == RESULT_VIDEO ? searchVideos.size() : searchUps.size();
    }

    private boolean resultHasMore() {
        return resultKind == RESULT_VIDEO ? searchVideoMore : searchUpMore;
    }

    private void activateResult() {
        if (resultKind == RESULT_VIDEO) onSearchVideo(resultIndex);
        else if (resultIndex >= 0 && resultIndex < searchUps.size()) onSearchUp(searchUps.get(resultIndex));
    }

    private boolean onSearchPanelKey(int key) {
        View current = getCurrentFocus();
        View inKeys = current == null ? null : binding.biliKeys.findContainingItemView(current);
        if (inKeys != null || focus == KEYS) return onBiliKey(key, inKeys);
        if (focus == WORDS) return onBiliWord(key);
        return onResultKey(key);
    }

    private boolean onBiliKey(int key, View item) {
        if (item == null) {
            focusBiliKeys();
            return true;
        }
        if (key == KeyEvent.KEYCODE_DPAD_CENTER || key == KeyEvent.KEYCODE_ENTER) {
            item.performClick();
            return true;
        }
        if (key == KeyEvent.KEYCODE_DPAD_UP && isFirstRow(binding.biliKeys, item)) return true;
        if (key == KeyEvent.KEYCODE_DPAD_DOWN && isLastRow(binding.biliKeys, item)) return true;
        if (key == KeyEvent.KEYCODE_DPAD_LEFT && isFirstInRow(binding.biliKeys, item)) return true;
        if (key == KeyEvent.KEYCODE_DPAD_RIGHT && isLastInRow(binding.biliKeys, item)) {
            focusBiliWords();
            return true;
        }
        return false;
    }

    private boolean onBiliWord(int key) {
        if (key == KeyEvent.KEYCODE_DPAD_LEFT) {
            focusBiliKeys();
            return true;
        }
        if (key == KeyEvent.KEYCODE_DPAD_RIGHT) {
            if (!searchQuery.trim().isEmpty() && searchQuery.trim().equals(keyword) && resultCount() > 0) showBiliResults(true);
            else if (!searchQuery.trim().isEmpty()) submitSearch();
            else if (wordIndex >= 0 && wordIndex < words.size()) {
                searchQuery = words.get(wordIndex);
                paintQuery();
                submitSearch();
            }
            return true;
        }
        if (key == KeyEvent.KEYCODE_DPAD_UP && wordIndex > 0) {
            selectWord(wordIndex - 1);
            return true;
        }
        if (key == KeyEvent.KEYCODE_DPAD_DOWN && wordIndex + 1 < words.size()) {
            selectWord(wordIndex + 1);
            return true;
        }
        if ((key == KeyEvent.KEYCODE_DPAD_CENTER || key == KeyEvent.KEYCODE_ENTER) && wordIndex >= 0 && wordIndex < words.size()) {
            searchQuery = words.get(wordIndex);
            paintQuery();
            submitSearch();
        }
        return true;
    }

    private boolean isFirstRow(RecyclerView rv, View item) {
        View first = rv.getChildAt(0);
        return first != null && item.getTop() == first.getTop();
    }

    private boolean isLastRow(RecyclerView rv, View item) {
        View last = rv.getChildAt(rv.getChildCount() - 1);
        return last != null && item.getTop() == last.getTop();
    }

    private boolean isFirstInRow(RecyclerView rv, View focused) {
        int top = focused.getTop();
        int left = focused.getLeft();
        for (int i = 0; i < rv.getChildCount(); i++) {
            View child = rv.getChildAt(i);
            if (child.getTop() == top && child.getLeft() < left) return false;
        }
        return true;
    }

    private boolean isLastInRow(RecyclerView rv, View focused) {
        int top = focused.getTop();
        int right = focused.getRight();
        for (int i = 0; i < rv.getChildCount(); i++) {
            View child = rv.getChildAt(i);
            if (child.getTop() == top && child.getRight() > right) return false;
        }
        return true;
    }

    private boolean onResultKey(int key) {
        if (key == KeyEvent.KEYCODE_DPAD_LEFT) {
            if (focus == RESULT_LIST) moveResultHorizontal(-1);
            else if (focus == RESULT_TAB && resultKind == RESULT_VIDEO) showBiliResults(false);
            else if (focus == RESULT_CODE) focusResultTab();
            else if (focus == RESULT_TAB && resultKind == RESULT_UP) showResultKind(RESULT_VIDEO);
            return true;
        }
        if (key == KeyEvent.KEYCODE_DPAD_RIGHT) {
            if (focus == RESULT_LIST) moveResultHorizontal(1);
            else if (focus == RESULT_TAB && resultKind == RESULT_VIDEO) showResultKind(RESULT_UP);
            else if (focus == RESULT_TAB && resultKind == RESULT_UP) focusResultCode();
            return true;
        }
        if (key == KeyEvent.KEYCODE_DPAD_UP) {
            if (focus == RESULT_LIST) moveResultUp();
            return true;
        }
        if (key == KeyEvent.KEYCODE_DPAD_DOWN) {
            if ((focus == RESULT_TAB || focus == RESULT_CODE) && resultCount() > 0) focusResultList();
            else if (focus == RESULT_LIST) moveResultDown();
            return true;
        }
        if (key == KeyEvent.KEYCODE_DPAD_CENTER || key == KeyEvent.KEYCODE_ENTER) {
            if (focus == RESULT_CODE) showSearchQr();
            else if (focus == RESULT_LIST) activateResult();
            return true;
        }
        return true;
    }

    private boolean canPreview() {
        return resultsOpen && isVisible(binding.searchPanel) && focus == RESULT_LIST && resultKind == RESULT_VIDEO && resultIndex >= 0 && resultIndex < searchVideos.size();
    }

    private void nudgePreview() {
        handler.removeCallbacks(previewTask);
        if (previewing) {
            previewIndex = -1;
            playToken++;
            playback.stop();
            restorePlayerWindow();
        }
        if (canPreview()) handler.postDelayed(previewTask, 2000);
    }

    private void startResultPreview() {
        if (!canPreview() || isFinishing()) return;
        if (previewing && previewIndex == resultIndex) return;
        BiliVideo video = searchVideos.get(resultIndex);
        if (!previewing) {
            previewRestoreVideo = playingVideo;
            previewRestoreIndex = playingIndex;
            previewRestoreKind = playingKind;
            previewRestorePosition = playingVideo == null ? 0 : playback.player().getCurrentPosition();
        }
        previewing = true;
        previewIndex = resultIndex;
        hideMeta();
        showPreviewWindow();
        int token = ++playToken;
        previewToken = token;
        Task.execute(() -> {
            try {
                BiliStreams streams = BiliApi.resolve(video.getBvid(), BiliSession.wantedQn());
                App.post(() -> startPlay(token, streams, 0, video));
            } catch (BiliException e) {
                App.post(() -> {
                    if (token == playToken) Notify.show(getString(R.string.bili_unsupported));
                });
            } catch (IOException e) {
                App.post(() -> {
                    if (token != playToken) return;
                    if ("auth".equals(e.getMessage())) showQr();
                    else Notify.show(getString(R.string.bili_play_fail));
                });
            }
        });
    }

    private void endPreview(boolean restore) {
        handler.removeCallbacks(previewTask);
        boolean was = previewing;
        BiliVideo restoreVideo = previewRestoreVideo;
        int restoreIndex = previewRestoreIndex;
        int restoreKind = previewRestoreKind;
        long restorePosition = previewRestorePosition;
        previewing = false;
        previewIndex = -1;
        restorePlayerWindow();
        if (!was) return;
        if (restore && restoreVideo != null) {
            playingVideo = restoreVideo;
            playingIndex = restoreIndex;
            playingKind = restoreKind;
            resolvePlay(restoreVideo, restorePosition);
            return;
        }
        playToken++;
        if (restore) playback.stop();
    }

    private void showPreviewWindow() {
        if (previewWindow) return;
        previewWindow = true;
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(dp(420), dp(236), Gravity.END | Gravity.BOTTOM);
        int margin = dp(28);
        params.setMargins(margin, margin, margin, margin);
        binding.player.setLayoutParams(params);
        binding.player.bringToFront();
    }

    private void restorePlayerWindow() {
        if (!previewWindow) return;
        previewWindow = false;
        ViewGroup parent = (ViewGroup) binding.player.getParent();
        if (parent == null) return;
        parent.removeView(binding.player);
        parent.addView(binding.player, 0, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private void onSearchVideo(int position) {
        if (position < 0 || position >= searchVideos.size()) return;
        boolean keep = previewing && previewIndex == position;
        searchPaused = false;
        clearSearchInput();
        handler.removeCallbacks(previewTask);
        previewing = false;
        previewIndex = -1;
        restorePlayerWindow();
        if (!keep) playToken++;
        if (kind == KIND_RECOMMEND) snapshotRecommend();
        queue.clear();
        queue.addAll(searchVideos);
        kind = KIND_SEARCH;
        hasMore = searchVideoMore;
        page = searchPage;
        listAdapter.setItems(queue);
        upFromSearch = false;
        hide(binding.searchPanel);
        showPure();
        if (keep) {
            playingKind = kind;
            playingIndex = position;
            playingVideo = queue.get(position);
            updateFav();
            showMeta(playingVideo);
            return;
        }
        playIndex(position, 0);
    }

    private void onSearchUp(BiliUp up) {
        openSpace(up.getMid(), up.getName(), up.getFace(), true);
    }

    private void onListClick(int position) {
        playIndex(position, 0);
        showControl();
    }

    private void showQuality() {
        if (qualities.isEmpty()) return;
        String[] labels = new String[qualities.size()];
        int checked = 0;
        for (int i = 0; i < qualities.size(); i++) {
            labels[i] = qualities.get(i).getLabel();
            if (qualities.get(i).getValue() == selectedQn) checked = i;
        }
        new MaterialAlertDialogBuilder(this).setTitle(R.string.bili_quality).setSingleChoiceItems(labels, checked, (dialog, which) -> {
            BiliSession.wantedQn(qualities.get(which).getValue());
            dialog.dismiss();
            long position = playback.player().getCurrentPosition();
            replayPlaying(position);
        }).show();
    }

    private void showSettings() {
        new MaterialAlertDialogBuilder(this).setTitle(BiliSession.nickname()).setPositiveButton(R.string.bili_logout, (dialog, which) -> {
            polling = false;
            memoryReady = false;
            BiliSession.logout();
            showQr();
        }).setNegativeButton(R.string.bili_close, null).show();
    }

    private void tick() {
        handler.removeCallbacks(tickTask);
        if (isGone(binding.control)) return;
        paintClock();
        handler.postDelayed(tickTask, 500);
    }

    private void paintClock() {
        long duration = playback.player().getDuration();
        long current = Math.max(0, playback.player().getCurrentPosition());
        if (duration > 0) {
            binding.position.setMax((int) Math.min(duration, Integer.MAX_VALUE));
            binding.position.setProgress((int) Math.min(current, duration));
        }
        binding.timeCurrent.setText(Util.timeMs(current));
        binding.timeDuration.setText(duration > 0 ? Util.timeMs(duration) : "--:--");
        boolean playing = playback.player().isPlaying();
        binding.playState.setImageResource(playing ? R.drawable.ic_widget_play : R.drawable.ic_widget_pause);
        binding.playState.setContentDescription(getString(playing ? R.string.bili_playing : R.string.bili_paused));
        binding.playStateText.setText(playing ? R.string.bili_playing : R.string.bili_paused);
    }

    private void bumpIdle() {
        handler.removeCallbacks(idleTask);
        handler.postDelayed(idleTask, 10000);
    }

    private void onIdle() {
        if (typing) {
            bumpIdle();
            return;
        }
        if (isVisible(binding.qr) || isVisible(binding.searchQr) || isVisible(binding.gridPanel) || isVisible(binding.searchPanel)) return;
        showPure();
    }

    @Override
    public void onPlaybackStateChanged(int state) {
        Log.i(BiliPlayback.TAG, "state=" + state);
        if (state == Player.STATE_READY) playbackReady = true;
        if (state != Player.STATE_ENDED || playingToken != playToken || previewing) return;
        if (!playbackReady) {
            Log.w(BiliPlayback.TAG, "ended before ready");
            Notify.show(getString(R.string.bili_play_fail));
            return;
        }
        playbackReady = false;
        if (kind != playingKind) return;
        playIndex(playingIndex + 1, 0);
    }

    @Override
    public void onPlayerError(PlaybackException error) {
        playbackReady = false;
        Log.e(BiliPlayback.TAG, "error " + error.errorCode + " " + error.getErrorCodeName() + " " + error.getMessage(), error);
        Notify.show(getString(R.string.bili_play_fail));
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() != KeyEvent.ACTION_DOWN) return super.dispatchKeyEvent(event);
        if (typing) return super.dispatchKeyEvent(event);
        bumpIdle();
        int key = event.getKeyCode();
        if (key == KeyEvent.KEYCODE_BACK || key == KeyEvent.KEYCODE_ESCAPE) return super.dispatchKeyEvent(event);
        if (isVisible(binding.searchQr)) return true;
        if (isVisible(binding.searchPanel)) {
            if (onSearchPanelKey(key)) return true;
            return super.dispatchKeyEvent(event);
        }
        if (isVisible(binding.qr) || isVisible(binding.gridPanel)) return super.dispatchKeyEvent(event);
        if (isVisible(binding.control)) return onControlKey(key) || super.dispatchKeyEvent(event);
        if (isVisible(binding.browse)) return onBrowseKey(key) || super.dispatchKeyEvent(event);
        return onPureKey(key) || super.dispatchKeyEvent(event);
    }

    private boolean onPureKey(int key) {
        if (key == KeyEvent.KEYCODE_DPAD_UP || key == KeyEvent.KEYCODE_DPAD_DOWN) {
            showBrowse();
            focus(LIST);
            return true;
        }
        if (key == KeyEvent.KEYCODE_DPAD_LEFT) {
            playback.seekBy(-10000);
            return true;
        }
        if (key == KeyEvent.KEYCODE_DPAD_RIGHT) {
            playback.seekBy(10000);
            return true;
        }
        if (key == KeyEvent.KEYCODE_DPAD_CENTER || key == KeyEvent.KEYCODE_ENTER) {
            showControl();
            return true;
        }
        if (key == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE) {
            playback.toggle();
            return true;
        }
        return false;
    }

    private boolean onBrowseKey(int key) {
        if (key == KeyEvent.KEYCODE_DPAD_LEFT) {
            if (focus == SETTING) focus(CODE);
            else if (focus == CODE) focus(SEARCH);
            else if (focus == TABS) switchTab(-1);
            return true;
        }
        if (key == KeyEvent.KEYCODE_DPAD_RIGHT) {
            if (focus == SEARCH) focus(CODE);
            else if (focus == CODE) focus(SETTING);
            else if (focus == TABS) switchTab(1);
            return true;
        }
        if (key == KeyEvent.KEYCODE_DPAD_UP) {
            if (focus == TABS) focus(SEARCH);
            else if (focus == LIST) moveList(-1);
            return true;
        }
        if (key == KeyEvent.KEYCODE_DPAD_DOWN) {
            if (focus == SEARCH || focus == CODE || focus == SETTING) focus(TABS);
            else if (focus == TABS) focus(LIST);
            else if (focus == LIST) moveList(1);
            return true;
        }
        if (key == KeyEvent.KEYCODE_DPAD_CENTER || key == KeyEvent.KEYCODE_ENTER) {
            if (focus == SETTING) showSettings();
            else if (focus == CODE) showSearchQr();
            else if (focus == SEARCH) openBiliSearch();
            else if (focus == LIST && index != playingIndex) playIndex(index, 0);
            else if (focus == LIST) showControl();
            else if (focus == TABS && channel == RECOMMEND) refreshRecommend();
            else showControl();
            return true;
        }
        return false;
    }

    private boolean onControlKey(int key) {
        if (key == KeyEvent.KEYCODE_DPAD_LEFT) {
            if (focus == SEEK) playback.seekBy(-10000);
            else if (focus == QUALITY) focus(MORE);
            else if (focus == MORE) focus(FAV);
            tick();
            return true;
        }
        if (key == KeyEvent.KEYCODE_DPAD_RIGHT) {
            if (focus == SEEK) playback.seekBy(10000);
            else if (focus == FAV) focus(MORE);
            else if (focus == MORE) focus(QUALITY);
            tick();
            return true;
        }
        if (key == KeyEvent.KEYCODE_DPAD_UP) {
            focus(SEEK);
            return true;
        }
        if (key == KeyEvent.KEYCODE_DPAD_DOWN) {
            if (focus == SEEK) focus(FAV);
            return true;
        }
        if (key == KeyEvent.KEYCODE_DPAD_CENTER || key == KeyEvent.KEYCODE_ENTER) {
            if (focus == FAV) toggleFav();
            else if (focus == MORE) openMore();
            else if (focus == QUALITY) showQuality();
            else {
                playback.toggle();
                paintClock();
            }
            return true;
        }
        return false;
    }

    @Override
    protected void onBackInvoked() {
        if (typing) {
            stopTyping();
            return;
        }
        if (isVisible(binding.searchQr)) {
            hide(binding.searchQr);
            return;
        }
        if (previewing && isVisible(binding.searchPanel)) {
            endPreview(true);
            return;
        }
        if (isVisible(binding.gridPanel)) {
            hide(binding.gridPanel);
            if (upFromSearch) {
                upFromSearch = false;
                show(binding.searchPanel);
                if (resultsOpen) focusResultList();
                else focusBiliKeys();
            } else {
                showPure();
            }
        } else if (isVisible(binding.searchPanel)) {
            if (resultsOpen) showBiliResults(false);
            else {
                hide(binding.searchPanel);
                clearSearchInput();
                showBrowse();
                resumeFromSearch();
            }
        } else if (isVisible(binding.control)) {
            showPure();
        } else if (isVisible(binding.browse)) {
            if (focus == LIST) focus(TABS);
            else finish();
        } else {
            showBrowse();
        }
    }

    @Override
    protected void onDestroy() {
        polling = false;
        if (BiliSession.isLoggedIn()) {
            memoryReady = true;
            memoryChannel = channel;
            memoryIndex = playingIndex;
        }
        handler.removeCallbacksAndMessages(null);
        if (playback != null) playback.release();
        super.onDestroy();
    }

    private void show(View view) {
        view.setVisibility(View.VISIBLE);
        paintDock();
    }

    private void hide(View view) {
        view.setVisibility(View.GONE);
        paintDock();
    }

    private void paintDock() {
        boolean on = isVisible(binding.meta) || isVisible(binding.control);
        binding.dock.setBackground(on ? getDrawable(R.drawable.bili_dock_bg) : null);
    }

    private static class EndScroll extends RecyclerView.OnScrollListener {
        private final Runnable end;

        private EndScroll(Runnable end) {
            this.end = end;
        }

        @Override
        public void onScrolled(@androidx.annotation.NonNull RecyclerView recyclerView, int dx, int dy) {
            if (dy <= 0) return;
            RecyclerView.LayoutManager manager = recyclerView.getLayoutManager();
            if (!(manager instanceof LinearLayoutManager linear)) return;
            if (linear.findLastVisibleItemPosition() >= linear.getItemCount() - 1) end.run();
        }
    }
}
