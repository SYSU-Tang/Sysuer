package com.sysu.edu.academic;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.sysu.edu.R;
import com.sysu.edu.databinding.ItemCourseOutlineBinding;
import com.sysu.edu.databinding.RecyclerViewBinding;

import java.util.ArrayList;

public class CourseOutlineFragment extends Fragment {


    CourseOutlineAdp adp;
    JSONArray data;
    View root;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if(root == null) {
            RecyclerViewBinding binding = RecyclerViewBinding.inflate(inflater);
            adp = new CourseOutlineAdp(requireContext());
            binding.recyclerView.setLayoutManager(new GridLayoutManager(requireContext(), 1));
            binding.recyclerView.setAdapter(adp);
            if(data!=null){
                data.forEach(e -> {
                    if (e != null && adp != null) {
                        adp.add((JSONObject) e);
                    }
                });
            }
            root=binding.getRoot();
        }
        return root;
    }

    @Override
    public void setArguments(@Nullable Bundle args) {
        if (args != null) {
            data = JSONArray.parse(args.getString("data"));
        }
        super.setArguments(args);
    }
}
class CourseOutlineAdp extends RecyclerView.Adapter<RecyclerView.ViewHolder>{
    final Context context;
    final ArrayList<JSONObject> data =new ArrayList<>();
    public CourseOutlineAdp(Context con){
        this.context=con;
    }
    public void add(JSONObject json){
        data.add(json);
        notifyItemInserted(getItemCount());
    }
    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemCourseOutlineBinding binding = ItemCourseOutlineBinding.inflate(LayoutInflater.from(context));
        return new RecyclerView.ViewHolder(binding.getRoot()) {
        };
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ((TextView)holder.itemView.findViewById(R.id.title)).setText(String.format("%s（%s学时）", convert(position,"sectionDesignation"), convert(position,"teachingHours")));
        ((TextView)holder.itemView.findViewById(R.id.intro)).setText(String.format("教学内容：%s\n育人元素：%s\n重点、难点：%s", convert(position,"teachingMainContent"), convert(position,"courseElements"),convert(position,"keyPoints")));
    }

    @Override
    public int getItemCount() {
        return data.size();
    }
    String convert(int position,String key){
        String a = data.get(position).getString(key);
        return (a==null?"":a).replace("\n\n","\n");
    }
}