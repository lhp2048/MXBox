package com.fongmi.android.tv.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fongmi.android.tv.R;

import java.util.ArrayList;
import java.util.List;

public class BiliWordAdapter extends RecyclerView.Adapter<BiliWordAdapter.Holder> {

    public interface Listener {
        void onClick(String text);
    }

    private final List<String> items = new ArrayList<>();
    private final Listener listener;
    private int selected = -1;
    private boolean armed;

    public BiliWordAdapter(Listener listener) {
        this.listener = listener;
    }

    public void setItems(List<String> words) {
        items.clear();
        if (words != null) items.addAll(words);
        if (selected >= items.size()) selected = items.isEmpty() ? -1 : 0;
        notifyDataSetChanged();
    }

    public void setSelected(int selected) {
        this.selected = selected;
        notifyDataSetChanged();
    }

    public void setArmed(boolean armed) {
        if (this.armed == armed) return;
        this.armed = armed;
        if (getItemCount() > 0) notifyItemRangeChanged(0, getItemCount());
    }

    public String get(int position) {
        return items.get(position);
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_bili_word, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        String text = items.get(position);
        holder.text.setText(text);
        holder.itemView.setSelected(position == selected);
        holder.itemView.setActivated(armed && position == selected);
        holder.itemView.setOnClickListener(v -> listener.onClick(text));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        private final TextView text;

        Holder(@NonNull View itemView) {
            super(itemView);
            text = itemView.findViewById(R.id.text);
        }
    }
}
