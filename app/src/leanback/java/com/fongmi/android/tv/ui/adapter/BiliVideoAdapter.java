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
import com.fongmi.android.tv.bili.BiliVideo;

import java.util.ArrayList;
import java.util.List;

public class BiliVideoAdapter extends RecyclerView.Adapter<BiliVideoAdapter.Holder> {

    public interface Listener {
        void onClick(int position);
    }

    private final List<BiliVideo> items = new ArrayList<>();
    private final int layout;
    private final Listener listener;
    private final boolean scrim;
    private int selected = -1;
    private boolean armed;

    public BiliVideoAdapter(int layout, Listener listener) {
        this(layout, listener, false);
    }

    public BiliVideoAdapter(int layout, Listener listener, boolean scrim) {
        this.layout = layout;
        this.listener = listener;
        this.scrim = scrim;
    }

    public void setItems(List<BiliVideo> videos) {
        items.clear();
        if (videos != null) items.addAll(videos);
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

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(LayoutInflater.from(parent.getContext()).inflate(layout, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        BiliVideo video = items.get(position);
        holder.title.setText(video.getTitle());
        if (holder.author != null) holder.author.setText(video.getAuthor());
        holder.itemView.setSelected(position == selected);
        if (scrim) {
            holder.itemView.setActivated(armed && position == selected);
            holder.itemView.setBackgroundResource(R.drawable.bili_list_item_bg);
        }
        Glide.with(holder.cover).load(video.getCover()).into(holder.cover);
        holder.itemView.setOnClickListener(v -> listener.onClick(holder.getBindingAdapterPosition()));
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
