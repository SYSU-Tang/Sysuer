package com.sysu.edu.academic;

import android.content.res.Configuration;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.alibaba.fastjson2.JSONObject;
import com.sysu.edu.api.Params;
import com.sysu.edu.databinding.ItemNewsBinding;
import com.sysu.edu.databinding.RecyclerViewScrollBinding;
import com.sysu.edu.view.AdapterListener;
import com.sysu.edu.view.RecyclerAdapter;

public class NewsFragment extends Fragment {
    final NewsAdapter newsAdapter = new NewsAdapter();
    StaggeredGridLayoutManager staggeredGridLayoutManager;
    Params params;
    
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        RecyclerViewScrollBinding binding = RecyclerViewScrollBinding.inflate(inflater);
        params = new Params(this);
        staggeredGridLayoutManager = new StaggeredGridLayoutManager(params.getColumn(), StaggeredGridLayoutManager.VERTICAL);
        binding.getRoot().setLayoutManager(staggeredGridLayoutManager);
        binding.getRoot().setAdapter(newsAdapter);
        return binding.getRoot();
    }
    
    public void add(JSONObject json) {
        newsAdapter.add(json);
    }
    
    public void setListener(AdapterListener listener) {
        newsAdapter.setListener(listener);
    }
    
    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        staggeredGridLayoutManager.setSpanCount(params.getColumn());
    }
    
    public static class NewsAdapter extends RecyclerAdapter<JSONObject> {
        
        AdapterListener listener;
        
        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new RecyclerView.ViewHolder(ItemNewsBinding.inflate(LayoutInflater.from(parent.getContext())).getRoot()) {
            };
        }
        
        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            ItemNewsBinding binding = ItemNewsBinding.bind(holder.itemView);
            // AppCompatImageView image = holder.itemView.findViewById(R.id.image);
            if (listener != null) listener.onBind(this, holder, position);
            //        if(Objects.equals(data.get(position).getString("newDeliveryMark"), "1")){
            //            Drawable latest = AppCompatResources.getDrawable(context,R.drawable.latest);
            //            if (latest != null) {
            //                latest.setBounds(0,0,72,72);
            //            }
            //            title.setCompoundDrawablePadding(12);
            //            title.setCompoundDrawables(latest,null,null,null);
            //        }
            binding.title.setText(data.get(position).getString("title"));
            binding.content.setText(data.get(position).getString("deliveryDate"));
            //        String img = data.get(position).get("image");
            //        if (img != null && !img.isEmpty()) {
            //            Glide.with(context).load(img)
            //                    // .diskCacheStrategy(DiskCacheStrategy.NONE).skipMemoryCache(true)
            //                    .placeholder(R.drawable.logo)
            //                    .override(400).fitCenter().transform(new RoundedCorners(16))
            //                    .into(image);
            //        }
            super.onBindViewHolder(holder, position);
        }
    }
}
