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
import com.sysu.edu.databinding.CourseDetailFragmentBinding;
import com.sysu.edu.databinding.ServiceItemBinding;

public class CourseDetailFragment extends Fragment {


    CourseDetailFragmentBinding binding;
    JSONObject data;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = CourseDetailFragmentBinding.inflate(inflater);
        return binding.getRoot();
    }

    @Override
    public void setArguments(@Nullable Bundle args) {
        if (args != null) {
            //System.out.println(args.getString("data"));
            JSONObject data0 = JSONObject.parse(args.getString("data"));
            if (data0 != null) {
                switch (args.getInt("what")) {
                    case 1:
                        data = data0;
                        break;
                    case 2:
                        binding.intro.setText(data.getString("courseContentInChinese"));
                        binding.goal.setText(data.getString("courseObjectiveAndRequirement"));
                        binding.method.setText(data.getString("teachMethod"));
                        binding.evaluationMethod.setText(data.getString("evaluationMethod"));
                        binding.reference.setText(data.getString("referenceBook"));
                        binding.resource.setText(data.getString("courseResource"));
                        String[] info = new String[]{"courseName", "courseTypeName", "courseNum", "subCourseTypeName", "subTypeModuleName", "credit", "totalHours", "lecturesCreHours", "labCreHours", "weekHours","totalHoursComment","languageName", "establishUnitNumberName", "planClassSize", "teacherName", "intendedAcadYear", "intendedCampusName"};
                        for (int i = 0; i < info.length; i++) {
                            String content = (i==9|i==10?data0:data).getString(info[i]);
                            if (content == null) {content = "";}
                            Chip chip = ServiceItemBinding.inflate(getLayoutInflater()).getRoot();
                            chip.setText(String.format("%s：%s", (new String[]{"课程名称", "课程类别", "课程编码", "课程细类", "艺术教育板块", "学分", "总学时", "理论学时", "实践(含实验)学时", "周学时","总学时备注", "授课语种", "开课单位", "接收人数", "主讲教师", "意向开课学期", "意向线下上课校区", "是否有意向开设跨校园同步课程"})[i], content));
                            chip.setOnLongClickListener(a -> {
                                ((ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("", ((Chip) a).getText()));
                                return false;
                            });
                            chip.setOnClickListener(a -> Snackbar.make(requireContext(), chip, ((Chip) a).getText(), Snackbar.LENGTH_LONG).show());
                            binding.detail.addView(chip);
                        }
                        break;
                }

            }
        }
        super.setArguments(args);
    }
}