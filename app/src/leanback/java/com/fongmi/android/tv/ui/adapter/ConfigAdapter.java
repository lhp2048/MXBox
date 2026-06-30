package com.fongmi.android.tv.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import android.text.TextUtils;

import com.fongmi.android.tv.bean.Config;
import com.fongmi.android.tv.databinding.AdapterConfigBinding;

import java.util.ArrayList;
import java.util.List;

public class ConfigAdapter extends RecyclerView.Adapter<ConfigAdapter.ViewHolder> {

    private final OnClickListener listener;
    private List<Config> mItems = new ArrayList<>();
    private String vodUrl;
    private String liveUrl;
    private boolean readOnly;

    public ConfigAdapter(OnClickListener listener) {
        this.listener = listener;
    }

    public interface OnClickListener {

        void onTextClick(Config item);

        void onDeleteClick(Config item);
    }

    public ConfigAdapter readOnly(boolean readOnly) {
        this.readOnly = readOnly;
        return this;
    }

    public ConfigAdapter addAll(int type) {
        mItems = new ArrayList<>(Config.getAll(type));
        if (!mItems.isEmpty() && !readOnly) mItems.remove(0);
        return this;
    }

    public ConfigAdapter setItems(List<Config> items) {
        mItems = new ArrayList<>(items);
        return this;
    }

    public ConfigAdapter setCurrent(String vodUrl, String liveUrl) {
        this.vodUrl = vodUrl;
        this.liveUrl = liveUrl;
        return this;
    }

    public ConfigAdapter setCurrent(String url) {
        return setCurrent(url, null);
    }

    public int remove(Config item) {
        int position = mItems.indexOf(item);
        if (position == -1) return -1;
        item.delete();
        mItems.remove(position);
        notifyItemRemoved(position);
        return getItemCount();
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(AdapterConfigBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Config item = mItems.get(position);
        boolean selected = isSelected(item);
        holder.binding.text.setText(item.getDesc());
        holder.binding.text.setSelected(selected);
        holder.binding.text.setOnClickListener(v -> listener.onTextClick(item));
        holder.binding.delete.setVisibility(readOnly ? View.GONE : View.VISIBLE);
        holder.binding.delete.setOnClickListener(v -> listener.onDeleteClick(item));
    }

    private boolean isSelected(Config item) {
        if (item.getType() == 1) return TextUtils.equals(item.getUrl(), liveUrl);
        return TextUtils.equals(item.getUrl(), vodUrl);
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        private final AdapterConfigBinding binding;

        public ViewHolder(@NonNull AdapterConfigBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
