package com.sysu.edu.academic;

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

import java.util.ArrayList;
import java.util.List;

public class CourseQueryResultFragment extends StaggeredFragment {

    HttpManager http;
    int page = 1;
    int total = -1;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        params = new Params(requireActivity());
        params.setCallback(this, () -> {
            reset();
            getCourses();
        });
        Handler handler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                super.handleMessage(msg);
                if (msg.what == -1) {
                    params.toast(R.string.no_wifi_warning);
                } else {
                    JSONObject response = JSONObject.parseObject((String) msg.obj);
                    Integer code = response.getInteger("code");
                    if (code == 200) {
                        if (total == -1)
                            total = response.getJSONObject("data").getInteger("total");
                        response.getJSONObject("data").getJSONArray("rows").forEach(e -> {
                            JSONObject row = (JSONObject) e;
                            ArrayList<String> values = new ArrayList<>();
                            for (String i : new String[]{"yearTerm", "courseName", "courseNum", "openingUnitName", "courseCategoryName", "score", "teachingName", "limitNumber", "selectedNumber", "examMode", "teachingTimePlaceStr", "openingSchoolName", "readObj", "classNumber"})
                                values.add(row.getString(i));
                            if (values.get(10) != null)
                                values.set(10, values.get(10).replaceAll(",", "\n").replaceAll("/", " | "));
                            add(values.get(1), List.of("学年学期", "课程名称", "课程编号", "开课单位", "课程类别", "学分", "主讲教师", "限选人数", "已选人数", "考试方式", "上课信息", "上课校区", "修读对象", "教学班号"), values);
                        });
                    } else if (code == 53000007) {
                        params.toast(R.string.login_warning);
                        params.gotoLogin(getView(), TargetUrl.JWXT);
                    } else {
                        params.toast(response.getString("message"));
                    }
                }
            }
        };
        http = new HttpManager(handler);
        http.setParams(params);
        http.setReferrer("https://jwxt.sysu.edu.cn/jwxt/mk/");
        setScrollBottom(() -> {
            if ((page - 1) * 10 < total)
                getCourses();
        });
        clear();
        getCourses();
        return view;
    }

    void getCourses() {
        http.postRequest("https://jwxt.sysu.edu.cn/jwxt/schedule/agg/schoolOpeningCoursesSchedule/querySchoolOpeningCourses", String.format("{\"pageNo\":%s,\"pageSize\":10,\"total\":true,\"param\":%s}", page++, requireArguments().getString("params")), 0);
    }

    public void reset() {
        clear();
        page = 1;
        total = -1;
    }
}