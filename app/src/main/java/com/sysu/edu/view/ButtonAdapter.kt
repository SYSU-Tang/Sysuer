package com.sysu.edu.view;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButtonGroup;
import com.sysu.edu.databinding.ItemButtonGroupBinding;
import com.sysu.edu.databinding.ItemButtonOutlineBinding;

import java.util.ArrayList;

public class ButtonAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    
    private final ArrayList<String> data = new ArrayList<>();
    private onBindListener onBindListener;
    
    public void setListener(onBindListener onBindListener) {
        this.onBindListener = onBindListener;
    }
    
    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        MaterialButtonGroup itemView = ItemButtonGroupBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false).getRoot();
        for (int i = 0; i < data.size(); i++) {
            ItemButtonOutlineBinding binding = ItemButtonOutlineBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
            binding.getRoot().setText(data.get(i));
            if (onBindListener != null) onBindListener.onBind(binding.getRoot(), i);
            itemView.addView(binding.getRoot());
        }
        return new RecyclerView.ViewHolder(itemView) {
        };
    }
    
    public void add(String text) {
        data.add(text);
        notifyItemChanged(0);
    }
    
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
    }
    
    @Override
    public int getItemCount() {
        return 1;
    }
    
    public interface onBindListener {
        void onBind(Button button, int position);
    }
}
