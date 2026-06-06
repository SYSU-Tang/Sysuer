package com.sysu.edu.view;

import androidx.databinding.ViewDataBinding;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewbinding.ViewBinding;

public interface AdapterListener {
    void onBind(RecyclerView.Adapter<RecyclerView.ViewHolder> adapter, RecyclerView.ViewHolder holder, int position);

    void onCreate(RecyclerView.Adapter<RecyclerView.ViewHolder> adapter, ViewBinding binding);

}
interface Adapter2Listener<V extends ViewDataBinding> {
    void onBind(RecyclerView.Adapter<RecyclerViewHolder<V>> adapter, RecyclerViewHolder<V> holder, int position);
    
    void onCreate(RecyclerView.Adapter<RecyclerViewHolder<V>> adapter, V binding);
    
}