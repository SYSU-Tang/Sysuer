package com.sysu.edu.academic;

import static com.sysu.edu.api.CommonUtil.isEmpty;
import static com.sysu.edu.api.CommonUtil.trim;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityOptionsCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.alibaba.fastjson2.JSONObject;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.snackbar.Snackbar;
import com.sysu.edu.R;
import com.sysu.edu.api.Config;
import com.sysu.edu.databinding.FragmentCourseSelectionSelectedBinding;
import com.sysu.edu.databinding.ItemCourseSelectionBinding;
import com.sysu.edu.model.JwxtModel;
import com.sysu.edu.view.RecyclerAdapter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class CourseSelectionSelectedFragment extends Fragment {
    
    CourseSelectedAdapter adapter;
    int page = 1;
    int success = 1;
    int failure = 1;
    int retired = 1;
    int waiting = 1;
    int total = -1;
    String category;
    StaggeredGridLayoutManager layoutManager;
    Config config;
    JwxtModel model;
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        model.dispose();
    }
    
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        FragmentCourseSelectionSelectedBinding binding = FragmentCourseSelectionSelectedBinding.inflate(inflater, container, false);
        config = new Config(this);
        model = new JwxtModel(requireContext());
        layoutManager = new StaggeredGridLayoutManager(config.getColumn(), StaggeredGridLayoutManager.VERTICAL);
        binding.list.getRoot().setLayoutManager(layoutManager);
        binding.list.getRoot().setAdapter(adapter = new CourseSelectedAdapter());
        binding.list.getRoot().addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView v, int dx, int dy) {
                if (!v.canScrollVertically(1) && total > (page - 1) * 10 && dy > 0)
                    getSelectedCourses();
                binding.head.setElevation(v.canScrollVertically(-1) ? config.dpToPx(2) : 0);
            }
        });
        
        binding.list.getRoot().addItemDecoration(new CourseSelectionMainFragment.SpacesItemDecoration(config.dpToPx(8)));
        binding.filter.setOnCheckedStateChangeListener((_, checkedId) -> {
            success = checkedId.contains(R.id.success) ? 1 : 0;
            failure = checkedId.contains(R.id.failure) ? 1 : 0;
            retired = checkedId.contains(R.id.retired) ? 1 : 0;
            waiting = checkedId.contains(R.id.to_filter) ? 1 : 0;
            regetSelectedCourses();
        });
        binding.category.setOnCheckedStateChangeListener((_, checkedId) -> {
            Integer i = checkedId.get(0);
            if (i == R.id.all)
                category = "";
            else if (i == R.id.public_compulsory)
                category = "10";
            else if (i == R.id.public_selective)
                category = "30";
            else if (i == R.id.major_compulsory)
                category = "11";
            else if (i == R.id.major_selective)
                category = "21";
            else if (i == R.id.cross_major)
                category = "kzy";
            else if (i == R.id.honor)
                category = "31";
            regetSelectedCourses();
        });
        adapter.setSelectAction(position -> {
            if (adapter.get(position).getInteger("selectedStatus") == 3 || adapter.get(position).getInteger("selectedStatus") == 4)
                unselect(adapter.convert(position, "courseId"), adapter.convert(position, "teachingClassId"),
                        adapter.get(position).getString("selectedType"));
            else
                select(adapter.convert(position, "teachingClassId"), adapter.convert(position, "selectedType"),
                        adapter.get(position).getString("courseCateCode"));
        });
        adapter.setLikeAction(this::setPNP);
        model.getMessage().observe(requireActivity(), message -> {
            JSONObject response = message.second;
            if (response.getIntValue("code") == 200) {
                switch (message.first) {
                    case 0 -> {
                        total = response.getJSONObject("data").getInteger("total");
                        response.getJSONObject("data").getJSONArray("rows").forEach(o -> adapter.add((JSONObject) o));
                    }
                    case 1 -> {
                        if (response.containsKey("data") && response.getString("data") != null)
                            config.toast(response.getString("data"));
                        regetSelectedCourses();
                    }
                }
            }
        });
        getSelectedCourses();
        return binding.getRoot();
    }
    
    
    void unselect(String classId, String code, String type) {
        model.addAndNext("jwxt/choose-course-front-server/classCourseInfo/course/back",
                String.format("{\"courseId\":\"%s\",\"clazzId\":\"%s\",\"selectedType\":\"%s\"}", classId, code, type),
                1);
        
    }
    
    void select(String code, String type, String category) {
        model.addAndNext("jwxt/choose-course-front-server/classCourseInfo/course/choose",
                String.format("{\"clazzId\":\"%s\",\"selectedType\":\"%s\",\"selectedCate\":\"%s\",\"check\":true}", code, type, category),
                1);
        
    }
    
    void getSelectedCourses() {
        JSONObject args = new JSONObject().fluentPut("successStatus", String.valueOf(success))
                .fluentPut("failureStatus", String.valueOf(failure))
                .fluentPut("retiredClass", String.valueOf(retired))
                .fluentPut("waitingScreen", String.valueOf(waiting));
        if (!isEmpty(category))
            args.put("courseCateCode", category);
        model.addAndNext("jwxt/choose-course-front-server/selectedCourse/list", String.format("{\"pageNo\":%s,\"pageSize\":10,\"total\":true,\"param\":%s}", page++, args.toJSONString()), 0);
    }
    
    void regetSelectedCourses() {
        page = 1;
        total = -1;
        adapter.clear();
        getSelectedCourses();
    }
    
    void setPNP(String type, String id) {
        model.addAndNext("jwxt/choose-course-front-server/selectedCourse/setTwoTier?type=" + type, String.format("{\"clazzId\":\"%s\"}", id), 1);
    }
    
    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        layoutManager.setSpanCount(config.getColumn());
    }
    
    static class CourseSelectedAdapter extends RecyclerAdapter<JSONObject> {
        
        final String[] info = new String[]{"credit", "teachingClassNum", "scheduleExamTime", "examFormName"};
        Consumer<? super Integer> selectAction;
        BiConsumer<? super String, ? super String> likeAction;
        
        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            Context context = parent.getContext();
            ItemCourseSelectionBinding binding = ItemCourseSelectionBinding.inflate(LayoutInflater.from(context), parent, false);
            for (int i = 0; i < info.length; i++) {
                Chip chip = (Chip) LayoutInflater.from(context).inflate(R.layout.item_action_chip, binding.courseInfo, false);
                chip.setOnLongClickListener(a -> {
                    ((ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("", ((Chip) a).getText()));
                    return false;
                });
                chip.setOnClickListener(a -> Snackbar.make(context, chip, ((Chip) a).getText(), Snackbar.LENGTH_LONG).show());
                binding.courseInfo.addView(chip);
            }
            return new RecyclerView.ViewHolder(binding.getRoot()) {
            };
        }
        
        public void setSelectAction(Consumer<? super Integer> action) {
            selectAction = action;
        }
        
        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            ItemCourseSelectionBinding binding = ItemCourseSelectionBinding.bind(holder.itemView);
            Context context = binding.getRoot().getContext();
            binding.courseName.setText(String.format("%s-%s", convert(position, "courseNum"), convert(position, "courseName")));
            JSONObject item = get(position);
            Integer status = item.getInteger("status");
            binding.select.setSelected(status == 3 || status == 4);
            boolean canPNP = status == 4 && Objects.equals(item.getString("isInTwoTierSet"), "1") && Arrays.asList(item.getString("courseCateList").split(",")).contains(item.getString("courseCateCode"));
            binding.select.setText(binding.select.isSelected() ? context.getString(R.string.drop_course) : context.getString(R.string.select_course));
            binding.filtering.setText(String.format("%s：%s", context.getString(R.string.status), "\n" + context.getString(status == 4 ? R.string.status_selected : status == 3 ? R.string.filtering : status == 1 ? R.string.retired : R.string.unselected)));
            binding.select.setOnClickListener(_ -> {
                if (selectAction != null)
                    selectAction.accept(position);
            });
            binding.open.setOnClickListener(v -> context.startActivity(new Intent(context, CourseDetailActivity.class).putExtra("code", convert(position, "courseNum")).putExtra("id", convert(position, "courseId")).putExtra("class", convert(position, "clazzNum")), ActivityOptionsCompat.makeSceneTransitionAnimation((Activity) context, v, "miniapp").toBundle()));
            binding.head.setText(convert(position, "teachingTimePlace").replace(";", " | ").replace(",", "\n"));
            binding.like.setVisibility(canPNP ? View.VISIBLE : View.GONE);
            if (canPNP) {
                boolean isPNP = item.getString("isTwoTier") == null || "0".equals(item.getString("isTwoTier"));
                binding.like.setText(isPNP ? R.string.set_pnp : R.string.cancel_pnp);
                binding.like.setOnClickListener(_ -> likeAction.accept(isPNP ? "1" : "0", item.getString("teachingClassId")));
            }
            String[] courseInfoLabels = context.getResources().getStringArray(R.array.course_info_labels);
            String[] seatInfoLabels = context.getResources().getStringArray(R.array.seat_info_labels);
            ArrayList<String> infoList = new ArrayList<>(Arrays.asList(seatInfoLabels));
            infoList.remove(1);
            for (int i = 0; i < info.length; i++)
                ((Chip) binding.courseInfo.getChildAt(i)).setText(String.format("%s：%s", courseInfoLabels[i], convert(position, info[i])));
            String[] seats = new String[]{"baseReceiveNum", "selectCount"};
            for (int i = 0; i < seats.length; i++)
                (new MaterialButton[]{binding.left, binding.selected}[i]).setText(String.format("%s\n%s", infoList.get(i), convert(position, seats[i])));
        }
        
        public String convert(int position, String key) {
            return trim(data.get(position).getString(key)).replace("\n\n", "\n");
        }
        
        public void setLikeAction(BiConsumer<? super String, ? super String> action) {
            likeAction = action;
        }
    }
}
