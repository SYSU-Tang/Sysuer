package com.sysu.edu.academic;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.alibaba.fastjson2.JSONObject;
import com.google.android.material.chip.Chip;
import com.google.android.material.snackbar.Snackbar;
import com.sysu.edu.databinding.FragmentCourseDetailBinding;
import com.sysu.edu.databinding.ItemActionChipBinding;

public class CourseDetailFragment extends Fragment {


    FragmentCourseDetailBinding binding;
    JSONObject data;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentCourseDetailBinding.inflate(inflater);
        return binding.getRoot();
    }

    @Override
    public void setArguments(@Nullable Bundle args) {
        if (args != null) {
            JSONObject data = JSONObject.parse(args.getString("data"));
            if (data != null) {
                switch (args.getInt("what")) {
                    case 1:
                        this.data = data;
                        break;
                    case 2:
                        try {
                            binding.intro.setText(this.data.getString("courseContentInChinese"));
                            binding.goal.setText(this.data.getString("courseObjectiveAndRequirement"));
                            binding.method.setText(this.data.getString("teachMethod"));
                            binding.evaluationMethod.setText(this.data.getString("evaluationMethod"));
                            binding.reference.setText(this.data.getString("referenceBook"));
                            binding.resource.setText(this.data.getString("courseResource"));
                            String[] info = new String[]{"courseName", "faceProfessionName", "courseTypeName", "courseNum", "courseId", "subCourseTypeName", "subTypeModuleName", "courseTextBook", "credit", "totalHours", "lecturesCreHours", "labCreHours", "weekHours", "totalHoursComment", "languageName", "establishUnitNumberName", "planClassSize", "teacherName", "intendedAcadYear", "intendedCampusName"};
                            for (int i = 0; i < info.length; i++) {
                                String content = (i == 9 | i == 10 ? data : this.data).getString(info[i]);
                                if (content == null) {
                                    content = "";
                                }
                                Chip chip = ItemActionChipBinding.inflate(getLayoutInflater()).getRoot();
                                chip.setText(String.format("%s：%s", (new String[]{"课程名称", "面向专业", "课程类别", "课程编码", "课程id", "课程细类", "艺术教育板块", "课本", "学分", "总学时", "理论学时", "实践(含实验)学时", "周学时", "总学时备注", "授课语种", "开课单位", "接收人数", "主讲教师", "意向开课学期", "意向线下上课校区", "是否有意向开设跨校园同步课程"})[i], content));
                                chip.setOnLongClickListener(a -> {
                                    ((ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("", ((Chip) a).getText()));
                                    return false;
                                });
                                chip.setOnClickListener(a -> Snackbar.make(requireContext(), chip, ((Chip) a).getText(), Snackbar.LENGTH_LONG).show());
                                binding.detail.addView(chip);
                            }
                        } catch (Exception ignored) {}
                        break;
                }

            }
        }
        super.setArguments(args);
    }
}