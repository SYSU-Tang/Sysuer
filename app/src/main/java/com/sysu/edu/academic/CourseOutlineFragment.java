package com.sysu.edu.academic;

import static com.sysu.edu.api.CommonUtil.trim;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.sysu.edu.R;
import com.sysu.edu.databinding.FragmentCourseOutlineBinding;
import com.sysu.edu.databinding.ItemCourseOutlineBinding;
import com.sysu.edu.view.RecyclerAdapter;

public class CourseOutlineFragment extends Fragment {

    JSONArray data;
    View root;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (root == null) {
            FragmentCourseOutlineBinding binding = FragmentCourseOutlineBinding.inflate(inflater);
            CourseOutlineAdapter adp = new CourseOutlineAdapter();
            binding.recyclerViewScroll.recyclerView.setLayoutManager(new GridLayoutManager(requireContext(), 1));
            binding.recyclerViewScroll.recyclerView.setAdapter(adp);
            binding.fab.setOnClickListener(_ -> startActivity(new Intent(requireContext(), MarkdownViewActivity.class).putExtra("content", adp.toMarkdown()).putExtra("title", getString(R.string.course_outline))));
            if (data != null) data.forEach(e -> {
                if (e != null) adp.add((JSONObject) e);
            });
            root = binding.getRoot();
        }
        return root;
    }

    @Override
    public void setArguments(@Nullable Bundle args) {
        if (args != null) data = JSONArray.parse(args.getString("data"));
        super.setArguments(args);
    }

    static class CourseOutlineAdapter extends RecyclerAdapter<JSONObject> {
        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new RecyclerView.ViewHolder(ItemCourseOutlineBinding.inflate(LayoutInflater.from(parent.getContext())).getRoot()) {};
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            ItemCourseOutlineBinding binding = ItemCourseOutlineBinding.bind(holder.itemView);
            binding.title.setText(String.format("%s（%s%s）", convert(position, "sectionDesignation"), convert(position, "teachingHours"), holder.itemView.getResources().getString(R.string.study_hour)));
            binding.intro.setText(String.format("教学内容：%s\n育人元素：%s\n重点、难点：%s", convert(position, "teachingMainContent"), convert(position, "courseElements"), convert(position, "keyPoints")));
            super.onBindViewHolder(holder, position);
        }

        String convert(int position, String key) {
            return trim(data.get(position).getString(key)).replaceAll("\n\n", "\n");
        }

        public String toMarkdown() {
            StringBuilder md = new StringBuilder();
            md.append("|章节|学时|教学内容|育人元素|重点、难点|\n|---|---|---|---|---|\n");
            data.forEach(e -> {
                if (e != null) {
                    md.append(trim(e.getString("sectionDesignation")).replace("\n", ">")).append("|");
                    md.append(trim(e.getString("teachingHours"))).append("|");
                    md.append(trim(e.getString("teachingMainContent")).replace("\n", "")).append("|");
                    md.append(trim(e.getString("courseElements")).replace("\n", "")).append("|");
                    md.append(trim(e.getString("keyPoints")).replace("\n", "")).append("|\n");
                }
            });
            return md.toString();
        }
    }
}
