package com.fongmi.android.tv.ui.custom;

import android.app.Activity;
import android.graphics.Typeface;
import android.os.SystemClock;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.fongmi.android.tv.utils.ResUtil;

import java.util.ArrayList;
import java.util.List;

public final class GlobalDanmaku {

    private static final int MODE_BOTTOM = 4;
    private static final int MODE_TOP = 5;
    private static final int MODE_REVERSE = 6;
    private static final int LANES = 8;
    private static final int MAX = 24;

    private static final List<Item> items = new ArrayList<>();
    private static FrameLayout host;
    private static int nextLane;

    private GlobalDanmaku() {
    }

    public static void show(String raw) {
        Item item = parse(raw);
        if (item == null) return;
        if (items.size() >= MAX) items.remove(0);
        items.add(item);
        tick();
    }

    public static void attach(Activity activity) {
        if (activity.isFinishing()) return;
        ViewGroup content = activity.findViewById(android.R.id.content);
        if (content == null) return;
        if (host != null && host.getContext() == activity) {
            content.bringChildToFront(host);
            tick();
            return;
        }
        detach(null);
        host = new FrameLayout(activity);
        host.setClickable(false);
        host.setFocusable(false);
        host.setFocusableInTouchMode(false);
        host.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
        content.addView(host, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        tick();
    }

    public static void detach(Activity activity) {
        if (host == null) return;
        if (activity != null && host.getContext() != activity) return;
        host.removeAllViews();
        ViewGroup parent = (ViewGroup) host.getParent();
        if (parent != null) parent.removeView(host);
        host = null;
        for (Item item : items) item.view = null;
    }

    private static void tick() {
        if (host == null) return;
        host.removeCallbacks(TICK);
        host.postOnAnimation(TICK);
    }

    private static final Runnable TICK = new Runnable() {
        @Override
        public void run() {
            if (host == null) return;
            int width = host.getWidth();
            int height = host.getHeight();
            if (width == 0 || height == 0) {
                host.postOnAnimation(this);
                return;
            }
            long now = SystemClock.elapsedRealtime();
            boolean alive = false;
            for (int i = items.size() - 1; i >= 0; i--) {
                Item item = items.get(i);
                float progress = (now - item.born) / (float) item.duration;
                if (progress >= 1f) {
                    if (item.view != null) host.removeView(item.view);
                    items.remove(i);
                    continue;
                }
                alive = true;
                place(item, width, height, progress);
            }
            if (alive) host.postOnAnimation(this);
        }
    };

    private static void place(Item item, int width, int height, float progress) {
        ensureView(item);
        if (item.view == null) return;
        int gap = ResUtil.dp2px(10);
        int row = item.view.getHeight() > 0 ? item.view.getHeight() : item.view.getMeasuredHeight();
        int y = ResUtil.dp2px(28) + item.lane * (row + gap);
        if (item.mode == MODE_BOTTOM) y = height - ResUtil.dp2px(36) - row - item.lane * (row + gap);
        float x;
        if (item.mode == MODE_TOP || item.mode == MODE_BOTTOM) {
            x = (width - item.textWidth) / 2f;
            item.view.setAlpha(progress > 0.82f ? (1f - progress) / 0.18f : 1f);
        } else if (item.mode == MODE_REVERSE) {
            x = -item.textWidth + progress * (width + item.textWidth);
        } else {
            x = width - progress * (width + item.textWidth);
        }
        item.view.setTranslationX(x);
        item.view.setTranslationY(Math.max(0, y));
    }

    private static void ensureView(Item item) {
        if (item.view != null || host == null) return;
        TextView view = new TextView(host.getContext());
        view.setText(item.text);
        view.setTextColor(item.color);
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, item.sizeSp);
        view.setTypeface(Typeface.DEFAULT_BOLD);
        view.setShadowLayer(ResUtil.dp2px(3), 0, 0, 0xE0000000);
        view.setSingleLine(true);
        view.setFocusable(false);
        view.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        host.addView(view, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        view.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        item.textWidth = view.getMeasuredWidth();
        item.view = view;
    }

    private static Item parse(String raw) {
        if (raw == null) return null;
        String text = raw;
        int mode = 1;
        int size = 25;
        int color = 0xFFFFFF;
        if (raw.startsWith("[")) {
            int end = raw.indexOf(']');
            if (end > 1) {
                String[] parts = raw.substring(1, end).split(",");
                text = raw.substring(end + 1);
                if (parts.length > 1) mode = number(parts[1], 1);
                if (parts.length > 2) size = number(parts[2], 25);
                if (parts.length > 3) color = number(parts[3], 0xFFFFFF);
            }
        }
        text = text.trim();
        if (text.isEmpty()) return null;
        if (size < 14) size = 14;
        if (size > 72) size = 72;
        boolean fixed = mode == MODE_TOP || mode == MODE_BOTTOM;
        long duration = fixed ? 4000 : Math.min(14000, 7000L + text.length() * 80L);
        return new Item(text, mode, size, (color & 0x00FFFFFF) | 0xFF000000, nextLane++ % LANES, SystemClock.elapsedRealtime(), duration);
    }

    private static int number(String value, int fallback) {
        try {
            return Integer.parseInt(value.trim());
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static final class Item {

        private final String text;
        private final int mode;
        private final int sizeSp;
        private final int color;
        private final int lane;
        private final long born;
        private final long duration;
        private TextView view;
        private int textWidth;

        private Item(String text, int mode, int sizeSp, int color, int lane, long born, long duration) {
            this.text = text;
            this.mode = mode;
            this.sizeSp = sizeSp;
            this.color = color;
            this.lane = lane;
            this.born = born;
            this.duration = duration;
        }
    }
}
