package com.fongmi.android.tv.ui.adapter;

import android.text.TextUtils;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.api.config.MxBoxSourceCatalog;
import com.fongmi.android.tv.api.config.MxBoxSourceProbe;
import com.fongmi.android.tv.bean.Config;
import com.fongmi.android.tv.databinding.AdapterMxSourceBinding;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MxBoxSourceAdapter extends RecyclerView.Adapter<MxBoxSourceAdapter.ViewHolder> {

    public enum ProbeState {
        NONE, PENDING, TESTING, OK, FAIL
    }

    private final OnClickListener listener;
    private final List<Config> items = new ArrayList<>();
    private final Map<String, MxBoxSourceProbe.Result> results = new java.util.HashMap<>();
    private final Set<String> testingKeys = new HashSet<>();
    private String vodUrl;
    private String liveUrl;
    private boolean probing;

    public MxBoxSourceAdapter(OnClickListener listener) {
        this.listener = listener;
    }

    public interface OnClickListener {
        void onItemClick(Config item);
    }

    public MxBoxSourceAdapter setItems(List<Config> configs) {
        items.clear();
        items.addAll(configs);
        notifyDataSetChanged();
        return this;
    }

    public MxBoxSourceAdapter setCurrent(String vodUrl, String liveUrl) {
        this.vodUrl = vodUrl;
        this.liveUrl = liveUrl;
        notifyDataSetChanged();
        return this;
    }

    public void setResults(Map<String, MxBoxSourceProbe.Result> probeResults) {
        results.clear();
        if (probeResults != null) results.putAll(probeResults);
        probing = false;
        testingKeys.clear();
        notifyDataSetChanged();
    }

    public void beginProbe() {
        probing = true;
        testingKeys.clear();
        results.clear();
        notifyDataSetChanged();
    }

    public void addTesting(Config config) {
        testingKeys.add(MxBoxSourceProbe.key(config));
        notifyDataSetChanged();
    }

    public void completeProbe(Config config, MxBoxSourceProbe.Result result) {
        String key = MxBoxSourceProbe.key(config);
        testingKeys.remove(key);
        results.put(key, result);
        notifyDataSetChanged();
    }

    public void restoreSession(MxBoxSourceProbe.Session session) {
        if (session == null) return;
        probing = session.running;
        testingKeys.clear();
        testingKeys.addAll(session.testingKeys);
        results.clear();
        results.putAll(session.results);
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(AdapterMxSourceBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Config item = items.get(position);
        String key = MxBoxSourceProbe.key(item);
        boolean selected = isSelected(item);
        holder.binding.getRoot().setSelected(selected);
        holder.binding.current.setVisibility(selected ? View.VISIBLE : View.GONE);
        holder.binding.type.setText(MxBoxSourceCatalog.getTypeLabel(item.getType()));
        holder.binding.type.setTextColor(holder.itemView.getContext().getColor(item.getType() == 1 ? R.color.mx_accent : R.color.mx_primary));
        holder.binding.type.setBackgroundResource(item.getType() == 1 ? R.drawable.shape_mx_type_live : R.drawable.shape_mx_type_vod);
        holder.binding.name.setText(MxBoxSourceCatalog.getDisplayName(item));
        holder.binding.name.setTextColor(holder.itemView.getContext().getColor(selected ? R.color.mx_primary : R.color.white));
        holder.binding.name.setTypeface(null, selected ? Typeface.BOLD : Typeface.NORMAL);
        holder.binding.getRoot().setOnClickListener(v -> listener.onItemClick(item));
        bindProbe(holder, key);
    }

    private void bindProbe(ViewHolder holder, String key) {
        ProbeState state = getProbeState(key);
        int textColor = holder.itemView.getContext().getColor(R.color.white_60);
        int background = 0;
        String text = "";
        switch (state) {
            case PENDING -> {
                text = holder.itemView.getContext().getString(R.string.mx_source_probe_pending);
                background = R.drawable.shape_mx_probe_pending;
            }
            case TESTING -> {
                text = holder.itemView.getContext().getString(R.string.mx_source_probe_testing);
                textColor = holder.itemView.getContext().getColor(R.color.mx_accent);
                background = R.drawable.shape_mx_probe_testing;
            }
            case OK -> {
                MxBoxSourceProbe.Result result = results.get(key);
                text = result.latencyMs + "ms";
                textColor = holder.itemView.getContext().getColor(R.color.mx_primary);
                background = R.drawable.shape_mx_probe_ok;
            }
            case FAIL -> {
                MxBoxSourceProbe.Result result = results.get(key);
                text = holder.itemView.getContext().getString(R.string.mx_source_probe_fail, result.latencyMs);
                textColor = holder.itemView.getContext().getColor(R.color.mx_probe_fail);
                background = R.drawable.shape_mx_probe_fail;
            }
            default -> text = "";
        }
        holder.binding.probe.setText(text);
        holder.binding.probe.setTextColor(textColor);
        holder.binding.probe.setBackgroundResource(background);
        holder.binding.probe.setVisibility(TextUtils.isEmpty(text) ? View.GONE : View.VISIBLE);
    }

    private ProbeState getProbeState(String key) {
        if (results.containsKey(key)) {
            return results.get(key).success ? ProbeState.OK : ProbeState.FAIL;
        }
        if (probing) {
            if (testingKeys.contains(key)) return ProbeState.TESTING;
            return ProbeState.PENDING;
        }
        return ProbeState.NONE;
    }

    private boolean isSelected(Config item) {
        if (item.getType() == 1) return TextUtils.equals(item.getUrl(), liveUrl);
        return TextUtils.equals(item.getUrl(), vodUrl);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        private final AdapterMxSourceBinding binding;

        ViewHolder(@NonNull AdapterMxSourceBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
