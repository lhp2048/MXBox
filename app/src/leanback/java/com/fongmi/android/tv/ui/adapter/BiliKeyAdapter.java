package com.fongmi.android.tv.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.databinding.AdapterKeyboardIconBinding;
import com.fongmi.android.tv.databinding.AdapterKeyboardTextBinding;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BiliKeyAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public static final int BACKSPACE = R.drawable.ic_keyboard_back;
    public static final int SEARCH = R.drawable.ic_keyboard_search;

    private final List<Object> items = new ArrayList<>();
    private final Listener listener;

    public BiliKeyAdapter(Listener listener) {
        this.listener = listener;
        items.add(BACKSPACE);
        items.add(SEARCH);
        items.addAll(Arrays.asList("A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z", "0", "1", "2", "3", "4", "5", "6", "7", "8", "9"));
    }

    public interface Listener {
        void onText(String text);

        void onBackspace();

        void onSearchKey();
    }

    @Override
    public int getItemViewType(int position) {
        return items.get(position) instanceof String ? 0 : 1;
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == 0) return new TextHolder(AdapterKeyboardTextBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
        return new IconHolder(AdapterKeyboardIconBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof TextHolder text) text.binding.text.setText(items.get(position).toString());
        else ((IconHolder) holder).binding.icon.setImageResource((int) items.get(position));
    }

    class TextHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        private final AdapterKeyboardTextBinding binding;

        TextHolder(@NonNull AdapterKeyboardTextBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            String text = items.get(getBindingAdapterPosition()).toString();
            listener.onText(text.toLowerCase());
        }
    }

    class IconHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {
        private final AdapterKeyboardIconBinding binding;

        IconHolder(@NonNull AdapterKeyboardIconBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
            itemView.setOnClickListener(this);
            itemView.setOnLongClickListener(this);
        }

        @Override
        public void onClick(View view) {
            int icon = (int) items.get(getBindingAdapterPosition());
            if (icon == BACKSPACE) listener.onBackspace();
            else listener.onSearchKey();
        }

        @Override
        public boolean onLongClick(View view) {
            if ((int) items.get(getBindingAdapterPosition()) != BACKSPACE) return false;
            listener.onText("");
            return true;
        }
    }
}
