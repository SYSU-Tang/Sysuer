package com.sysu.edu.academic;

import androidx.recyclerview.widget.RecyclerView;
import androidx.viewbinding.ViewBinding;

public interface StaggeredListener {
    void onBind(StaggeredFragment.StaggeredAdapter a, RecyclerView.ViewHolder holder, int position);
    void onCreate(StaggeredFragment.StaggeredAdapter a, ViewBinding binding);

}
