package com.sysu.edu.academic;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.sysu.edu.R;
import com.sysu.edu.api.Params;
import com.sysu.edu.databinding.CardItemBinding;
import com.sysu.edu.databinding.RecyclerViewBinding;
import com.sysu.edu.databinding.RecyclerViewScrollBinding;
import com.sysu.edu.databinding.TwoColumnBinding;

import java.util.ArrayList;
import java.util.List;

public class StaggeredFragment extends Fragment {

    RecyclerViewScrollBinding binding;
    Params params;
    int position;
    StaggeredAdapter staggeredAdapter;
    StaggeredGridLayoutManager lm;
    int orientation=StaggeredGridLayoutManager.VERTICAL;
    boolean nested = true;

    public static StaggeredFragment newInstance(int position){
        StaggeredFragment s=new StaggeredFragment();
        s.position=position;
        return s;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState) {
        params = new Params(requireActivity());
        binding = RecyclerViewScrollBinding.inflate(inflater);
        lm  = new StaggeredGridLayoutManager(params.getColumn(), orientation);
        binding.recyclerView.setLayoutManager(lm);
        if(staggeredAdapter==null){
            staggeredAdapter = new StaggeredAdapter(requireContext());
        }
        binding.recyclerView.setAdapter(staggeredAdapter);
        binding.recyclerView.setNestedScrollingEnabled(nested);
        //binding.recyclerView
        return binding.getRoot();
    }
    public void setOrientation(int o){
        this.orientation = o;
        if(lm!=null){
            lm.setOrientation(orientation);
        }
    }
    public void setNested(boolean nested){
        this.nested = nested;
        if(binding!=null){
            binding.recyclerView.setNestedScrollingEnabled(nested);
        }
    }
    public void setHideNull(boolean hide){
        staggeredAdapter.setHideNull(hide);
    }
    public void setHideNull(Context context,boolean hide){
        if (staggeredAdapter==null) {
            staggeredAdapter = new StaggeredAdapter(context);
        }
        setHideNull(hide);
    }
    public void add(String title,@Nullable Integer icon,List<String> keys,List<String> values){
        staggeredAdapter.add(title, keys, values,icon);
    }
    public void add(String title,List<String> keys,List<String> values){
        add(title,null, keys, values);
    }
    public void add(Context context, String title,Integer icon, List<String> keys, List<String> values){
        if (staggeredAdapter==null) {
            staggeredAdapter = new StaggeredAdapter(context);
        }
        add(title, icon,keys, values);
    }
    public void add(Context context, String title, List<String> keys, List<String> values){
        add(context,title, null,keys, values);
    }
    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        lm.setSpanCount(params.getColumn());
    }
    void clear(){
        staggeredAdapter.clear();
    }

    static class ColumnAdp extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        boolean hideNull;
        List<String> value;
        Context context;
        List<String> key;

        public ColumnAdp(Context context, List<String> data, List<String> value, boolean hideNull) {
            super();
            this.key = data;
            this.hideNull = hideNull;
            this.value = value;
            this.context = context;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            TwoColumnBinding b = TwoColumnBinding.inflate(LayoutInflater.from(context), parent, false);
            b.item.setOnClickListener(view -> {
            });
            return new RecyclerView.ViewHolder(b.getRoot()) {
            };
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            ((TextView) holder.itemView.findViewById(R.id.key)).setText(key.get(position));
            holder.itemView.setOnClickListener(v -> {
                ClipboardManager clip = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                clip.setPrimaryClip(ClipData.newPlainText(key.get(position), value.get(position)));
            });
            if (position < value.size() && value.get(position) != null) {
                ((TextView) holder.itemView.findViewById(R.id.value)).setText(value.get(position));
            } else if (hideNull) {
                holder.itemView.setVisibility(View.GONE);
                holder.itemView.getLayoutParams().height = 0;
            }
        }

        @Override
        public int getItemCount() {
            return key.size();
        }
    }

    public static class StaggeredAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        public ArrayList<String> titles = new ArrayList<>();
        boolean hideNull;
        Context context;
        ArrayList<List<String>> keys = new ArrayList<>();
        ArrayList<Integer> icons = new ArrayList<>();
        ArrayList<List<String>> values = new ArrayList<>();
        StaggeredListener staggeredListener;

        public StaggeredAdapter(Context c) {
            super();
            this.context = c;
            this.hideNull = false;
        }
        /*public StaggeredAdapter(Context c,boolean hideNull){
            super();
            this.context = c;
            this.hideNull=hideNull;
        }*/

        public void setHideNull(boolean hideNull) {
            this.hideNull = hideNull;
        }

        public void setListener(StaggeredListener listener) {
            this.staggeredListener = listener;
        }

        public void add(String title, List<String> keys, List<String> values, Integer icon) {
            titles.add(title);
            this.icons.add(icon);
            this.keys.add(keys);
            this.values.add(values);
            notifyItemInserted(getItemCount());
        }

        public void clear() {
            int tmp = getItemCount();
            titles.clear();
            icons.clear();
            keys.clear();
            values.clear();
            notifyItemRangeRemoved(0, tmp);
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            CardItemBinding item = CardItemBinding.inflate(LayoutInflater.from(context), parent, false);
            RecyclerView list = RecyclerViewBinding.inflate(LayoutInflater.from(context), item.getRoot(), false).getRoot();
            list.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false));
            list.setNestedScrollingEnabled(false);
            item.card.addView(list);
            if (staggeredListener != null) {
                staggeredListener.onCreate(this, item);
            }
            return new RecyclerView.ViewHolder(item.getRoot()) {
            };
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            TextView title = holder.itemView.findViewById(R.id.title);
            title.setText(titles.get(position));
            if (icons.get(position) != null) {
                title.setCompoundDrawablePadding(new Params((Activity) context).dpToPx(4));
                Drawable icon = AppCompatResources.getDrawable(context, icons.get(position));
                if (icon != null) {
                    System.out.println(icon.getBounds());
                    icon.setBounds(0, 0, 72, 72);
                    title.setCompoundDrawables(icon, null, null, null);
                }
            }
            ((TextView) holder.itemView.findViewById(R.id.title)).setText(titles.get(position));
            ColumnAdp adp = new ColumnAdp(context, keys.get(position), values.get(position), hideNull);
            ((RecyclerView) holder.itemView.findViewById(R.id.recycler_view)).setAdapter(adp);
            if (staggeredListener != null) {
                staggeredListener.onBind(this, holder,position);
            }
        }

        @Override
        public int getItemCount() {
            return titles.size();
        }
    }
}