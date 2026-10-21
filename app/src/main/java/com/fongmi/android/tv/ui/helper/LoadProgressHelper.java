package com.fongmi.android.tv.ui.helper;

import android.content.Context;
import android.text.TextUtils;
import android.view.View;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.databinding.ViewProgressBinding;
import com.fongmi.android.tv.event.ConfigLoadEvent;
import com.fongmi.android.tv.event.DepotLoadEvent;
import com.google.android.material.progressindicator.LinearProgressIndicator;

public final class LoadProgressHelper {

    private LoadProgressHelper() {
    }

    public static void reset(ViewProgressBinding binding) {
        if (binding == null) return;
        if (binding.status != null) {
            binding.status.setVisibility(View.GONE);
            binding.status.setText("");
        }
        if (binding.detail != null) {
            binding.detail.setVisibility(View.GONE);
            binding.detail.setText("");
        }
        if (binding.bar != null) binding.bar.setVisibility(View.GONE);
    }

    public static void bind(ViewProgressBinding binding, ConfigLoadEvent event, Context context) {
        if (binding == null || context == null) return;
        if (event == null || event.phase == ConfigLoadEvent.Phase.IDLE) {
            reset(binding);
            return;
        }
        if (event.phase == ConfigLoadEvent.Phase.DONE) {
            reset(binding);
            return;
        }
        if (DepotLoadEvent.isLoading()) {
            bindDepot(binding, context);
            return;
        }
        String status = statusText(context, event);
        String detail = detailText(context, event);
        if (binding.status != null) {
            binding.status.setVisibility(TextUtils.isEmpty(status) ? View.GONE : View.VISIBLE);
            binding.status.setText(status);
        }
        if (binding.detail != null) {
            binding.detail.setVisibility(TextUtils.isEmpty(detail) ? View.GONE : View.VISIBLE);
            binding.detail.setText(detail);
        }
        LinearProgressIndicator bar = binding.bar;
        if (bar != null) {
            if (event.phase == ConfigLoadEvent.Phase.DEPOT_MERGE && event.total > 0) {
                bar.setVisibility(View.VISIBLE);
                bar.setIndeterminate(false);
                bar.setMax(event.total);
                bar.setProgress(Math.min(event.current, event.total));
            } else {
                bar.setVisibility(View.GONE);
            }
        }
    }

    public static void bindActive(ViewProgressBinding binding, Context context) {
        if (binding == null || context == null) return;
        if (DepotLoadEvent.isLoading()) {
            bindDepot(binding, context);
            return;
        }
        if (!ConfigLoadEvent.isActive() && ConfigLoadEvent.getPhase() != ConfigLoadEvent.Phase.ERROR) {
            reset(binding);
            return;
        }
        bind(binding, ConfigLoadEvent.current(), context);
    }

    public static void bindDepot(ViewProgressBinding binding, Context context) {
        int total = Math.max(DepotLoadEvent.getTotal(), 1);
        int current = Math.min(DepotLoadEvent.getCurrent(), total);
        if (binding.status != null) {
            binding.status.setVisibility(View.VISIBLE);
            binding.status.setText(context.getString(R.string.mx_load_depot_merge, current, total));
        }
        if (binding.detail != null) {
            binding.detail.setVisibility(View.VISIBLE);
            binding.detail.setText(context.getString(
                    R.string.mx_depot_loading,
                    current,
                    total,
                    DepotLoadEvent.getDepotName(),
                    DepotLoadEvent.getSiteCount()
            ));
        }
        LinearProgressIndicator bar = binding.bar;
        if (bar != null) {
            bar.setVisibility(View.VISIBLE);
            bar.setIndeterminate(current <= 0);
            bar.setMax(total);
            bar.setProgress(current);
        }
    }

    private static String statusText(Context context, ConfigLoadEvent event) {
        return switch (event.phase) {
            case START -> context.getString(R.string.mx_load_start, event.label);
            case FETCH_CONFIG -> context.getString(R.string.mx_load_fetch_config);
            case CACHE_HIT -> context.getString(R.string.mx_load_cache_hit);
            case PARSE_JAR -> context.getString(R.string.mx_load_parse_jar);
            case DEPOT_MERGE -> context.getString(R.string.mx_load_depot_merge, event.current, event.total);
            case SYNC_LIVE -> context.getString(R.string.mx_load_sync_live);
            case HOME_CONTENT -> context.getString(R.string.mx_load_home_content);
            case ERROR -> context.getString(R.string.mx_load_error, event.message);
            default -> "";
        };
    }

    private static String detailText(Context context, ConfigLoadEvent event) {
        if (event.phase == ConfigLoadEvent.Phase.DEPOT_MERGE) {
            return context.getString(
                    R.string.mx_depot_loading,
                    event.current,
                    event.total,
                    event.detail,
                    parseInt(event.message)
            );
        }
        if (event.phase == ConfigLoadEvent.Phase.START && !TextUtils.isEmpty(event.label)) {
            return event.label;
        }
        if (event.phase == ConfigLoadEvent.Phase.ERROR && !TextUtils.isEmpty(event.message)) {
            return event.message;
        }
        return "";
    }

    private static int parseInt(String value) {
        try {
            return Integer.parseInt(value);
        } catch (Exception e) {
            return 0;
        }
    }
}
