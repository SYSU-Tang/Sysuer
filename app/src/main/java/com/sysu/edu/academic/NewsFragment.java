package com.sysu.edu.academic;

import android.content.Context;
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
import com.google.android.material.textview.MaterialTextView;
import com.sysu.edu.R;
import com.sysu.edu.api.Params;
import com.sysu.edu.databinding.ItemNewsBinding;
import com.sysu.edu.databinding.RecyclerViewScrollBinding;

import java.util.ArrayList;

public class NewsFragment extends Fragment {
    public NewsAdp adp;
    StaggeredGridLayoutManager lm;
    Params params;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        RecyclerViewScrollBinding binding = RecyclerViewScrollBinding.inflate(inflater);
        params = new Params(requireActivity());
        lm = new StaggeredGridLayoutManager(params.getColumn(), StaggeredGridLayoutManager.VERTICAL);
        binding.getRoot().setLayoutManager(lm);
        if(adp==null){
            adp = new NewsAdp(requireActivity());
        }
        binding.getRoot().setAdapter(adp);
        return binding.getRoot();
    }

    public void add(Context context, JSONObject json){
        if(adp==null){
           adp = new NewsAdp(context);
        }
        adp.add(json);
    }
    public void setListener(Context context, StaggeredListener l){
        if(adp==null){
            adp = new NewsAdp(context);
        }
        adp.setListener(l);
    }
    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        lm.setSpanCount(params.getColumn());
        super.onConfigurationChanged(newConfig);
    }

    public static class NewsAdp extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        public ArrayList<JSONObject> data = new ArrayList<>();
        Context context;
        StaggeredListener listener;
        // String cookie;

        public NewsAdp(Context context) {
            super();
            this.context = context;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new RecyclerView.ViewHolder(ItemNewsBinding.inflate(LayoutInflater.from(context)).getRoot()) {
            };
        }

        public void add(JSONObject json) {
            data.add(json);
            notifyItemInserted(getItemCount() - 1);
        }

        public void setListener(StaggeredListener l) {
            this.listener = l;
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            MaterialTextView title = holder.itemView.findViewById(R.id.title);
            MaterialTextView content = holder.itemView.findViewById(R.id.content);

            // AppCompatImageView image = holder.itemView.findViewById(R.id.image);
            if(listener!=null){listener.onBind(this,holder,position);}
    //        if(Objects.equals(data.get(position).getString("newDeliveryMark"), "1")){
    //            Drawable latest = AppCompatResources.getDrawable(context,R.drawable.latest);
    //            if (latest != null) {
    //                latest.setBounds(0,0,72,72);
    //            }
    //            title.setCompoundDrawablePadding(12);
    //            title.setCompoundDrawables(latest,null,null,null);
    //        }
            title.setText(data.get(position).getString("title"));
            content.setText(data.get(position).getString("deliveryDate"));
    //        String img = data.get(position).get("image");
    //        if (img != null && !img.isEmpty()) {
    //            Glide.with(context).load(img)
    //                    // .diskCacheStrategy(DiskCacheStrategy.NONE).skipMemoryCache(true)
    //                    .placeholder(R.drawable.logo)
    //                    .override(400).fitCenter().transform(new RoundedCorners(16))
    //                    .into(image);
    //        }
        }

        @Override
        public int getItemCount() {
            return data.size();
        }
    }
}
