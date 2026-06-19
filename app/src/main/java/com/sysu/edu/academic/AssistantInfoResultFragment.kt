package com.sysu.edu.academic;

import static com.sysu.edu.api.CommonUtil.extractValue;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.alibaba.fastjson2.JSONObject;
import com.sysu.edu.model.JwxtModel;
import com.sysu.edu.view.StaggeredFragment;

import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;

public class AssistantInfoResultFragment extends StaggeredFragment {
    int page = 1;
    int total = -1;
    JwxtModel model;
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        model.dispose();
    }
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        model = new JwxtModel(requireContext());
        setScrollBottom(() -> {
            if (page * 10 < total) getResult();
        });
        model.getMessage().observe(requireActivity(), message -> {
            JSONObject response = message.second;
            if (response.getInteger("code") == 200) {
                if (message.first == 0) {
                    total = response.getJSONObject("data").getInteger("total");
                    response.getJSONObject("data").getJSONArray("rows").forEach(o -> {
                        JSONObject item = (JSONObject) o;
                        add(item.getString("courseName"), List.of("序号", "学年学期", "校区", "开设单位", "课程名称", "课程编号", "课程学时", "班级编号", "实选人数", "任课教师", "上课时间地点", "修读对象", "上课学生名单", "助教信息", "助教职责"),
                                extractValue(item, new String[]{"rowNum", "semester", "studyCampus", "openUnitName", "courseName", "courseNum", "courseHour", "classNumber", "apersonNum", "teacherName", "teachingTimePlace", "studyObj", "stuList", "assistantInfo", "jobDuty"}));
                    });
                }
            }
        });
        getResult();
        return view;
    }
    
    void getResult(String query) {
        model.addAndNext("jwxt/assistant-manage/assistantInfoQuery/pageList?code=jwxsd_zjxxck", String.format("{\"pageNo\":%s,\"pageSize\":10,\"total\":true,\"param\":%s}", page++, query), 0);
    }
    
    void getResult() {
        JSONObject filter = new JSONObject();
        BiConsumer<String, String> setFilter = (key, value) -> {
            if (requireArguments().containsKey(key) && requireArguments().getString(key) != null && !Objects.requireNonNull(requireArguments().getString(key)).isEmpty())
                filter.put(value, requireArguments().getString(key));
        };
        setFilter.accept("term", "semester");
        setFilter.accept("campus", "studyCampusCode");
        setFilter.accept("courseNumber", "courseNum");
        setFilter.accept("courseName", "courseName");
        setFilter.accept("teacherName", "teacherName");
        getResult(filter.toString());
    }
}
