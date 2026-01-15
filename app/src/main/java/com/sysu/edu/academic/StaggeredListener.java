package com.sysu.edu.academic;

import androidx.recyclerview.widget.RecyclerView;
import androidx.viewbinding.ViewBinding;

public interface StaggeredListener {
    void onBind(RecyclerView.Adapter<RecyclerView.ViewHolder> a, RecyclerView.ViewHolder holder, int position);
    void onCreate(RecyclerView.Adapter<RecyclerView.ViewHolder> a, ViewBinding binding);

}
