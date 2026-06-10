package com.sysu.edu.view;

import static android.text.TextUtils.isEmpty;
import static com.sysu.edu.api.CommonUtil.toStringOrDefault;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.alibaba.fastjson2.JSONObject;
import com.sysu.edu.R;
import com.sysu.edu.databinding.ItemPreferenceBinding;

import java.util.List;
import java.util.stream.IntStream;

public class PreferenceAdapter extends RecyclerAdapter<JSONObject> {
    
    boolean hideNull = false;
    
    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new RecyclerView.ViewHolder(ItemPreferenceBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false).getRoot()) {
        };
    }
    
    public void set(List<Integer> titles, List<String> contents, List<Integer> icons, Context context) {
        clear();
        IntStream.range(0, titles.size()).forEach(i -> add(context.getString(titles.get(i)), contents.get(i), icons.get(i)));
    }
    
    public void add(String title, String content, Integer icon) {
        add(JSONObject.of("title", title, "content", content, "icon", icon));
        notifyItemInserted(getItemCount() - 1);
    }
    
    public void set(List<String> titles, List<String> contents, List<Integer> icons) {
        clear();
        IntStream.range(0, titles.size()).forEach(i -> add(titles.get(i), contents.get(i), icons.get(i)));
    }
    
    public void setHideNull(boolean hideNull) {
        this.hideNull = hideNull;
    }
    
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        int pos = holder.getBindingAdapterPosition();
        ItemPreferenceBinding binding = ItemPreferenceBinding.bind(holder.itemView);
        JSONObject item = get(position);
        binding.itemTitle.setText(toStringOrDefault(item.getString("title")));
        binding.itemContent.setText(isEmpty(item.getString("content")) ? holder.itemView.getContext().getString(R.string.none) : item.getString("content"));
        binding.getRoot().setOnClickListener(_ -> {
            // params.toast(titles.get(pos) + ": " + contents.get(pos));
        });
        binding.itemContent.setVisibility(hideNull && isEmpty(item.getString("content")) ? View.GONE : View.VISIBLE);
        if (item.getInteger("icon") != null)
            binding.itemIcon.setImageResource(item.getInteger("icon"));
//        else binding.itemIcon.setImageResource(R.drawable.account);
        binding.getRoot().updateAppearance(pos, getItemCount());
        super.onBindViewHolder(holder, position);
    }
}
