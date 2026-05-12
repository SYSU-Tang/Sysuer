package com.sysu.edu.academic;

import static com.sysu.edu.api.CommonUtil.extractValue;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import com.alibaba.fastjson2.JSONObject;
import com.sysu.edu.R;
import com.sysu.edu.api.HttpManager;
import com.sysu.edu.api.Params;
import com.sysu.edu.api.TargetUrl;
import com.sysu.edu.view.StaggeredFragment;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@SuppressWarnings("ALL")
public class CourseCompletionFragment extends StaggeredFragment {
    HttpManager http;
    int page = 0;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        Params params = new Params(this);
        params.setCallback(() -> {
            page = 0;
            getStudentCourse();
        });
        http = new HttpManager(new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                if (msg.what == -1) {
                    params.toast(R.string.no_net_connected);
                } else {
                    JSONObject response = JSONObject.parseObject((String) msg.obj);
                    if (response != null && response.getInteger("code").equals(200)) {
                        if (response.get("data") != null) {
                            if (msg.what == 0) {
                                JSONObject data = response.getJSONObject("data");
                                data.getJSONArray("rows").forEach(a -> {
                                    ArrayList<String> values = extractValue((JSONObject) a, new String[]{"acadYearSemester", "courseNumber", "courseName", "courseCategoryName", "credit",/**/"acadYearSemester", "achievementCourseNumber", "achievementCourseName", "achievementCourseCategoryName", "achievementCredit", "ispassed", "achievementPoint"});
                                    if (values.get(0) != null) {
                                        values.set(0, values.get(0).replace(",", "|"));
                                    }
                                    if (values.get(5) != null) {
                                        values.set(5, values.get(5).replace(",", "|"));
                                    }
                                    add(values.get(2), List.of("学年学期", "课程号", "课程名称", "课程类别", "学分", "成绩获取学年学期", "课程号", "课程名称", "课程类别", "学分", "是否及格", "成绩"), values);
                                });
                            }
                        }
                    } else if (response != null && response.getInteger("code").equals(50030000)) {
                        params.toast(response.getString("message"));
                    } else {
                        params.toast(R.string.login_warning);
                        params.gotoLogin(TargetUrl.JWXT);
                    }
                }
            }
        });
        http.setParams(params);
        http.setReferrer("https://jwxt.sysu.edu.cn/jwxt/mk/gradua/");
        getStudentCourse();
        return view;
    }

    void getStudentCourse() {
        http.postRequest("https://jwxt.sysu.edu.cn/jwxt/gradua-degree/graduatemsg/studentsGraduationExamination/studentCourse", String.format(Locale.getDefault(), "{\"pageNo\":%d,\"pageSize\":10,\"total\":true,\"param\":{\"cultureTypeCode\":\"01\"}}", ++page), 0);
    }
}
