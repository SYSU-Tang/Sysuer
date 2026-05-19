package com.sysu.edu.academic;

import static com.sysu.edu.api.CommonUtil.extractValue;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import com.alibaba.fastjson2.JSONObject;
import com.sysu.edu.databinding.FragmentResultBinding;
import com.sysu.edu.model.JwxtModel;
import com.sysu.edu.view.StaggeredFragment;

import java.util.List;

public class AssistantEvaluationResultFragment extends StaggeredFragment {
    
    int page = 1;
    int total = -1;
    int order = 1;
    JwxtModel model;
    
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        FragmentResultBinding resultBinding = FragmentResultBinding.inflate(inflater, container, false);
        resultBinding.getRoot().addView(super.onCreateView(inflater, resultBinding.getRoot(), savedInstanceState));
        model = new JwxtModel(requireContext());
        setScrollBottom(() -> {
            if ((page - 1) * 10 < total) getResult();
        });
        model.getMessage().observe(requireActivity(), message -> {
            JSONObject response = message.getSecond();
            if (response.getInteger("code") == 200) {
                if (message.getFirst() == 0) {
                    JSONObject data = response.getJSONObject("data");
                    if (total == -1) total = data.getInteger("total");
                    data.getJSONArray("rows").forEach(
                            item -> add(String.valueOf(order++), List.of("学年学期", "助教学期", "助教姓名", "助教培养单位", "教学班号", "课程名称", "课程编码", "课程类别", "课程教学类型", "开课单位", "是否开班", "是否合班", "总教学班号", "任课教师", "课程学时", "助教承担的课程教学学时", "上课时间地点", "助教考核结论")
                                    , extractValue((JSONObject) item, new String[]{"yearTerm", "assistantNum", "assistantName", "assistantCollege", "classNum", "courseName", "courseNum", "courseType", "courseTeachingType", "courseCollege", "openClassFlag", "mergeClassFlag", "sumClassNum", "teacherName", "courseHours", "assistantHours", "teachingTimePlace", "conclusion"}))
                    );
                }
            }
        });
        getResult();
        return resultBinding.getRoot();
    }

//    void reset() {
//        page = 1;
//        total = -1;
//        order = 1;
//        clear();
//    }
    
    void getResult() {
        model.addAndNext("jwxt/assistant-manage/assistantEvaluation/evaluationResultPageList?code=jwxsd_zjpjck",
                String.format("{\"pageNo\":%s,\"pageSize\":10,\"total\":true,\"param\":%s}", page++, requireArguments().getString("params")),
                0);
    }
}