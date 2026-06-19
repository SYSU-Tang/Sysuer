package com.sysu.edu.academic;

import static com.sysu.edu.api.CommonUtil.extractValue;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import com.alibaba.fastjson2.JSONObject;
import com.sysu.edu.model.JwxtModel;
import com.sysu.edu.view.StaggeredFragment;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CourseCompletionFragment extends StaggeredFragment {
    
    int page = 0;
    JwxtModel model;
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        model.dispose();
    }
    
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        model = new JwxtModel(requireActivity());
        getStudentCourse();
        model.getMessage().observe(requireActivity(), message -> {
            JSONObject response = message.second;
            if (response.getInteger("code").equals(200)) {
                if (response.get("data") != null) {
                    if (message.first == 0) {
                        JSONObject data = response.getJSONObject("data");
                        data.getJSONArray("rows").forEach(a -> {
                            JSONObject item = (JSONObject) a;
                            ArrayList<String> values = extractValue(item, new String[]{"acadYearSemester", "courseNumber", "courseName", "courseCategoryName", "credit",/**/"acadYearSemester", "achievementCourseNumber", "achievementCourseName", "achievementCourseCategoryName", "achievementCredit", "ispassed", "achievementPoint"});
                            if (values.get(0) != null)
                                values.set(0, values.get(0).replace(",", "|"));
                            if (values.get(5) != null)
                                values.set(5, values.get(5).replace(",", "|"));
                            add(item.getString("courseName"), List.of("学年学期", "课程号", "课程名称", "课程类别", "学分", "成绩获取学年学期", "课程号", "课程名称", "课程类别", "学分", "是否及格", "成绩"), values);
                        });
                    }
                }
            }
        });
        model.next();
        return view;
    }
    
    void getStudentCourse() {
        model.add("jwxt/gradua-degree/graduatemsg/studentsGraduationExamination/studentCourse", String.format(Locale.getDefault(), "{\"pageNo\":%d,\"pageSize\":10,\"total\":true,\"param\":{\"cultureTypeCode\":\"01\"}}", ++page), 0);
    }
}
