package com.fongmi.android.tv.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.bili.BiliUp;

import java.util.ArrayList;
import java.util.List;

public class BiliUpAdapter extends RecyclerView.Adapter<BiliUpAdapter.Holder> {

    public interface Listener {
        void onClick(BiliUp up);
    }

    private final List<BiliUp> items = new ArrayList<>();
    private final Listener listener;
    private int selected = -1;
    private boolean armed;

    public BiliUpAdapter(Listener listener) {
        this.listener = listener;
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

    public void setItems(List<BiliUp> ups) {
        items.clear();
        if (ups != null) items.addAll(ups);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_bili_card, parent, false);
        return new Holder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        BiliUp up = items.get(position);
        holder.title.setText(up.getName());
        holder.author.setText("");
        holder.itemView.setSelected(position == selected);
        holder.itemView.setActivated(armed && position == selected);
        holder.itemView.setBackgroundResource(R.drawable.bili_list_item_bg);
        Glide.with(holder.cover).load(up.getFace()).into(holder.cover);
        holder.itemView.setOnClickListener(v -> listener.onClick(up));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        private final ImageView cover;
        private final TextView title;
        private final TextView author;

        Holder(@NonNull View itemView) {
            super(itemView);
            cover = itemView.findViewById(R.id.cover);
            title = itemView.findViewById(R.id.title);
            author = itemView.findViewById(R.id.author);
        }
    }
}
