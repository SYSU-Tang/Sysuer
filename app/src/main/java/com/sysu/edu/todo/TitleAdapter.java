package com.sysu.edu.todo;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textview.MaterialTextView;
import com.sysu.edu.databinding.ItemTitleBinding;

public class TitleAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    public static final int[] headers = {com.google.android.material.R.style.TextAppearance_Material3_TitleMedium, com.google.android.material.R.style.TextAppearance_Material3_TitleLarge_Emphasized, com.google.android.material.R.style.TextAppearance_Material3_TitleLarge};
    String title = "";
    int n = 0;

    public TitleAdapter(String title) {
        setTitle(title);
    }

    public TitleAdapter(String title, int n) {
        setTitle(title, n);
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
        notifyItemInserted(0);
    }

    public void setTitle(String title, int n) {
        this.title = title;
        setHeader(n);
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new RecyclerView.ViewHolder(ItemTitleBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false).getRoot()) {
        };
    }

    /**
     * 设置标题样式
     * 0: TextAppearance_Material3_TitleMedium
     * 1: TextAppearance_Material3_TitleLarge_Emphasized
     * 2: TextAppearance_Material3_TitleLarge
     *
     */
    public void setHeader(int n) {
        this.n = n;
        notifyItemChanged(0);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        MaterialTextView titleView = ItemTitleBinding.bind(holder.itemView).title;
        titleView.setText(title);
        titleView.setTextAppearance(0 <= n && n < headers.length ? headers[n] : 0);
    }

    @Override
    public int getItemCount() {
        return 1;
    }

    @Override
    public int getItemViewType(int position) {
        return 2;
    }
}
