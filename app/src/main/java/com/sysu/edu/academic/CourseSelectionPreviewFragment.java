package com.sysu.edu.academic;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityOptionsCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.FragmentNavigator;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.alibaba.fastjson2.JSONObject;
import com.sysu.edu.R;
import com.sysu.edu.api.Params;
import com.sysu.edu.databinding.FragmentCourseSelectionPreviewBinding;
import com.sysu.edu.databinding.ItemEvaluationBinding;
import com.sysu.edu.model.JwxtModel;
import com.sysu.edu.view.RecyclerAdapter;

import org.commonmark.node.Node;

import java.util.List;
import java.util.Map;

import io.noties.markwon.AbstractMarkwonPlugin;
import io.noties.markwon.Markwon;
import io.noties.markwon.MarkwonVisitor;
import io.noties.markwon.ext.tables.TablePlugin;

public class CourseSelectionPreviewFragment extends Fragment {
    
    final MutableLiveData<Integer> type = new MutableLiveData<>();
    CourseSelectionViewModel vm;
    FragmentCourseSelectionPreviewBinding binding;
    int page = 1;
    int total = -1;
    CourseSelectionPreviewAdapter previewAdapter;
    JwxtModel model;
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        model.dispose();
    }
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (binding == null) {
            model = new JwxtModel(requireContext());
            binding = FragmentCourseSelectionPreviewBinding.inflate(inflater, container, false);
            Params params = new Params(this);
            vm = new ViewModelProvider(requireActivity()).get(CourseSelectionViewModel.class);
            binding.list.recyclerView.setLayoutManager(new StaggeredGridLayoutManager(params.getColumn(), StaggeredGridLayoutManager.VERTICAL));
            previewAdapter = new CourseSelectionPreviewAdapter();
            binding.list.recyclerView.setAdapter(previewAdapter);
            binding.list.recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                    if (!recyclerView.canScrollVertically(1) && dy > 0 && total / 10.0 > page - 1)
                        getList();
                }
            });
            binding.type.addOnButtonCheckedListener((_, _, _) -> type.setValue(binding.major.isChecked() ? 1 : binding.collegePublicSelective.isChecked() ? 4 : 2));
            model.getMessage().observe(requireActivity(), message -> {
                JSONObject response = message.getSecond();
                if (response.getInteger("code") == 200) {
                    if (message.getFirst() == 0) {
                        JSONObject data = response.getJSONObject("data");
                        total = data.getInteger("total");
                        data.getJSONArray("rows").forEach(e -> previewAdapter.add((JSONObject) e));
                    }
                }
            });
            type.observe(getViewLifecycleOwner(), _ -> regetList());
            vm.filterValue.observe(requireActivity(), _ -> regetList());
        }
        return binding.getRoot();
    }
    
    private void regetList() {
        page = 1;
        total = -1;
        previewAdapter.clear();
        getList();
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding.addFilter.setOnClickListener(v -> Navigation.findNavController(view).navigate(R.id.preview_to_filter2, null, new NavOptions.Builder()
                .setEnterAnim(android.R.animator.fade_in)
                .setExitAnim(android.R.animator.fade_out)
                .setLaunchSingleTop(true)
                .build(), new FragmentNavigator.Extras(Map.of(v, "miniapp"))));
    }
    
    void getList() {
        model.addAndNext("jwxt/choose-course-front-server/schoolCourse/pageList",
                String.format("{\"pageNo\":%s,\"pageSize\":10,\"total\":false,\"param\":{\"hiddenSelectedStatus\":\"0\",\"type\":\"%s\"%s}}",
                        page++, type.getValue() == null ? 1 : type.getValue(), vm.getReturnData()), 0);
    }
    
    static class CourseSelectionPreviewAdapter extends RecyclerAdapter<JSONObject> {
        
        final String[] key = new String[]{"courseName", "courseCategoryName", "courseUnitName", "scheduleExamTime", "examFormName", "credit", "teachingClassId", "teachingClassNum", "teachingClassName", "courseNum"};
        final String[] name = new String[]{"课程名称", "课程类别", "开设学院", "考试时间", "考核方式", "学分", "教学班ID", "教学班号", "教学班名", "课程号"};
        
        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new RecyclerView.ViewHolder(ItemEvaluationBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false).getRoot()) {
            };
        }
        
        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            ItemEvaluationBinding binding = ItemEvaluationBinding.bind(holder.itemView);
            binding.title.setText(data.get(position).getString("courseName"));
            StringBuilder md = new StringBuilder("|老师|时间|地点|\n|:-----:|:----:|:----:|\n|" + data.get(position).getString("teachingTimePlace").replace(";", " | ").replace(",", " |\n| ") + "|\n");
            for (int i = 0; i < key.length; i++)
                md.append(String.format("\n%s：**%s**\n", name[i], data.get(position).getString(key[i]) == null ? "无" : data.get(position).getString(key[i])));
            View.OnClickListener action = view -> view.getContext().startActivity(new Intent(view.getContext(), CourseDetailActivity.class).putExtra("id", data.get(position).getString("teachingClassId")).putExtra("code", data.get(position).getString("courseNum")).putExtra("class", data.get(position).getString("teachingClassNum")),
                    ActivityOptionsCompat.makeSceneTransitionAnimation((Activity) binding.getRoot().getContext(), binding.title, "miniapp").toBundle());
            binding.getRoot().setOnClickListener(action);
            binding.open.setOnClickListener(action);
            Markwon.builder(binding.getRoot().getContext()).usePlugins(List.of(new AbstractMarkwonPlugin() {
                @Override
                public void configureVisitor(@NonNull MarkwonVisitor.Builder builder) {
                    super.configureVisitor(builder);
                    builder.blockHandler(new MarkwonVisitor.BlockHandler() {
                        @Override
                        public void blockStart(@NonNull MarkwonVisitor visitor, @NonNull Node node) {
                        }
                        
                        @Override
                        public void blockEnd(@NonNull MarkwonVisitor visitor, @NonNull Node node) {
                            if (visitor.hasNext(node)) visitor.ensureNewLine();
                        }
                    });
                }
            }, TablePlugin.create(binding.getRoot().getContext()))).build().setMarkdown(binding.startTime, md.toString());
        }
    }
}
