package com.arc.s;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

final class AnimationPickerAdapter extends RecyclerView.Adapter<AnimationPickerAdapter.TileViewHolder> {

    interface OnAnimationSelectedListener {
        void onSelected(RevealAnimation animation);
    }

    private final RevealAnimation[] animations = RevealAnimation.values();
    private final int selectedOrdinal;
    private final OnAnimationSelectedListener listener;

    AnimationPickerAdapter(int selectedOrdinal, OnAnimationSelectedListener listener) {
        this.selectedOrdinal = selectedOrdinal;
        this.listener = listener;
    }

    @NonNull
    @Override
    public TileViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_animation_tile, parent, false);
        return new TileViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TileViewHolder holder, int position) {
        RevealAnimation animation = animations[position];
        holder.label.setText(animation.getTileLabel());
        holder.label.setSelected(position == selectedOrdinal);
        holder.itemView.setOnClickListener(v -> listener.onSelected(animation));
    }

    @Override
    public int getItemCount() {
        return animations.length;
    }

    static final class TileViewHolder extends RecyclerView.ViewHolder {
        final TextView label;

        TileViewHolder(@NonNull View itemView) {
            super(itemView);
            label = itemView.findViewById(R.id.tile_label);
        }
    }
}
