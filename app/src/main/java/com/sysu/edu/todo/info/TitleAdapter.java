package com.sysu.edu.todo.info;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textview.MaterialTextView;
import com.sysu.edu.databinding.ItemTitleBinding;

public class TitleAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    final Context context;
    String title = "";
    int n=0;
    public TitleAdapter(Context context){
        super();
        this.context = context;
    }

    public TitleAdapter(Context context,String title){
        super();
        this.context = context;
        setTitle(title);
    }
    public String getTitle() {
        return title;
    }

    public void setTitle(String title){
        this.title = title;
        notifyItemInserted(0);
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new RecyclerView.ViewHolder(ItemTitleBinding.inflate(LayoutInflater.from(context), parent, false).getRoot()){};
    }

    public void setHeader(int n){
        this.n = n;
    }
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        MaterialTextView titleView = ItemTitleBinding.bind(holder.itemView).title;
        titleView.setText(title);
        titleView.setTextAppearance(new int[]{com.google.android.material.R.style.TextAppearance_Material3_TitleMedium,com.google.android.material.R.style.TextAppearance_Material3_TitleLarge_Emphasized,com.google.android.material.R.style.TextAppearance_Material3_TitleLarge}[n]);
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
