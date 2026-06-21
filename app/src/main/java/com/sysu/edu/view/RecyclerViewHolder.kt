package com.sysu.edu.view;

import androidx.annotation.NonNull;
import androidx.databinding.ViewDataBinding;
import androidx.recyclerview.widget.RecyclerView;

public class RecyclerViewHolder<T extends ViewDataBinding> extends RecyclerView.ViewHolder {
    
    public final T binding;
    
    public RecyclerViewHolder(@NonNull T binding) {
        super(binding.getRoot());
        this.binding = binding;
    }
}
