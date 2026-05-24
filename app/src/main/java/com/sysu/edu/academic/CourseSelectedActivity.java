package com.sysu.edu.academic;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityOptionsCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.MutableLiveData;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.alibaba.fastjson2.JSONObject;
import com.google.android.material.button.MaterialButton;
import com.sysu.edu.R;
import com.sysu.edu.api.Params;
import com.sysu.edu.databinding.ActivityCourseSelectedBinding;
import com.sysu.edu.databinding.ItemCourseSelectedBinding;
import com.sysu.edu.model.JwxtModel;
import com.sysu.edu.view.RecyclerAdapter;

import java.util.ArrayList;
import java.util.Locale;
import java.util.regex.Pattern;

public class CourseSelectedActivity extends AppCompatActivity {
    
    int page = 0;
    JwxtModel model;
    @Override
    protected void onDestroy() {
        super.onDestroy();
        model.dispose();
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityCourseSelectedBinding binding = ActivityCourseSelectedBinding.inflate(getLayoutInflater());
        Params params = new Params(this);
        model = new JwxtModel(this);
        CourseSelectedAdapter courseAdapter = new CourseSelectedAdapter();
        setContentView(binding.getRoot());
        binding.toolbar.setNavigationOnClickListener(_ -> supportFinishAfterTransition());
        binding.toolbar.getMenu().add(R.string.export).setIcon(R.drawable.export).setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_IF_ROOM).setOnMenuItemClickListener(_ -> {
            startActivity(new Intent(this, MarkdownViewActivity.class).putExtra("content", courseAdapter.toMarkdown()).putExtra("title", getString(R.string.course_selected)),
                    ActivityOptionsCompat.makeSceneTransitionAnimation(this, binding.toolbar, "miniapp").toBundle());
            return true;
        });
        binding.search.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return true;
            }
            
            @Override
            public boolean onQueryTextChange(String newText) {
                page = 0;
                courseAdapter.clear();
                getSelectedCourses(newText);
                return true;
            }
        });
        binding.list.setLayoutManager(new StaggeredGridLayoutManager(params.getColumn(), StaggeredGridLayoutManager.VERTICAL));
        binding.list.setAdapter(courseAdapter);
        model.getMessage().observe(this, message -> {
            JSONObject response = message.getSecond();
            if (response.getInteger("code") == 200) {
                JSONObject data = response.getJSONObject("data");
                data.getJSONArray("rows").forEach(o -> courseAdapter.add((JSONObject) o));
                if (data.getInteger("total") > page * 10)
                    getSelectedCourses(binding.search.getQuery().toString());
            }
        });
        getSelectedCourses("");
    }
    
    public void getSelectedCourses(String courseName) {
        model.addAndNext("jwxt/choose-course-front-server/selectedCourse/list", String.format(Locale.getDefault(), "{\"pageNo\":%d,\"pageSize\":10,\"total\":true,\"param\":{\"courseName\":\"%s\",\"successStatus\":\"1\",\"failureStatus\":\"0\",\"retiredClass\":\"0\",\"waitingScreen\":\"0\"}}", ++page, courseName), 1);
    }
    
    public static class CourseSelectedAdapter extends RecyclerAdapter<JSONObject> {
        
        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ViewHolder vh = new ViewHolder(ItemCourseSelectedBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
            vh.setInfo(data.get(viewType));
            return vh;
        }
        
        public String toMarkdown() {
            String[] key = new String[]{"courseName", "courseCategoryName", "courseUnitName", "scheduleExamTime", "examFormName", "credit", "teachingClassId", "teachingClassNum", "teachingClassName", "courseNum"};
            StringBuilder md = new StringBuilder();
            md.append("| 课程名称 | 课程类别 | 开设学院 | 考试时间 | 考核方式 | 学分 | 班级ID | 班级号 | 班级名 | 课程号 |\n");
            md.append("| -------- | -------- | -------- | -------- | -------- | -------- | -------- | -------- | -------- | -------- |\n");
            data.forEach(item -> {
                for (String s : key)
                    md.append(item.getString(s) == null ? "无" : item.getString(s)).append(" | ");
                md.append("\n");
            });
            return md.toString();
        }
        
        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            ((ViewHolder) holder).setInfo(data.get(position));
            ((ViewHolder) holder).binding.getRoot().setOnClickListener(view -> view.getContext().startActivity(new Intent(view.getContext(), CourseDetailActivity.class).putExtra("id", data.get(position).getString("teachingClassId")).putExtra("code", data.get(position).getString("courseNum")).putExtra("class", data.get(position).getString("teachingClassNum")),
                    ActivityOptionsCompat.makeSceneTransitionAnimation((Activity) view.getContext(), ((ViewHolder) holder).binding.title, "miniapp").toBundle()));
            super.onBindViewHolder(holder, position);
        }
        
        @Override
        public int getItemViewType(int position) {
            return position;
        }
        
        static class ViewHolder extends RecyclerView.ViewHolder {
            
            final ItemCourseSelectedBinding binding;
            final ArrayList<Integer> ids = new ArrayList<>();
            final MutableLiveData<JSONObject> info = new MutableLiveData<>();
            
            public ViewHolder(ItemCourseSelectedBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
                info.observe((FragmentActivity) binding.getRoot().getContext(), this::loadInfo);
            }
            
            public void setInfo(JSONObject info) {
                this.info.postValue(info);
            }
            
            void loadInfo(JSONObject info) {
                String[] key = new String[]{"courseName", "courseCategoryName", "courseUnitName", "scheduleExamTime", "examFormName", "credit", "teachingClassId", "teachingClassNum", "teachingClassName", "courseNum"};
                String[] name = new String[]{"课程名称", "课程类别", "开设学院", "考试时间", "考核方式", "学分", "班级ID", "班级号", "班级名", "课程号"};
                ids.forEach(e -> binding.group.removeView(binding.group.findViewById(e)));
                ids.clear();
                binding.title.setText(info.getString("courseName"));
                
                String teachingTimePlace = info.getString("teachingTimePlace");
                if (teachingTimePlace == null || teachingTimePlace.isEmpty()) {
                    ids.add(addItem("无", "课程安排"));
                } else {
                    Pattern.compile(",").splitAsStream(teachingTimePlace).forEach(s -> ids.add(addItem(s.replace(";", "/"), "课程安排")));
                }
                for (int i = 0; i < key.length; i++) {
                    ids.add(addItem(info.getString(key[i]) == null ? "无" : info.getString(key[i]), name[i]));
                }
                binding.courseInfo.setReferencedIds(ids.stream().mapToInt(Integer::intValue).toArray());
            }
            
            int addItem(String value, String name) {
                MaterialButton item = new MaterialButton(binding.getRoot().getContext(), null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
                Params params = new Params((FragmentActivity) binding.getRoot().getContext());
                item.setTextAppearance(binding.getRoot().getContext(), com.google.android.material.R.style.TextAppearance_Material3_BodyMedium);
                item.setLayoutParams(new ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.WRAP_CONTENT, ConstraintLayout.LayoutParams.WRAP_CONTENT));
                item.setOnClickListener(_ -> params.copy(name, value));
                item.setText(String.format(Locale.getDefault(), "%s: %s", name, value));
                item.setCornerRadius(params.dpToPx(8));
                item.setPadding(params.dpToPx(8), params.dpToPx(6), params.dpToPx(8), params.dpToPx(6));
                item.setGravity(Gravity.CENTER);
                int id = View.generateViewId();
                item.setId(id);
                binding.group.addView(item);
                return id;
            }
        }
    }
}