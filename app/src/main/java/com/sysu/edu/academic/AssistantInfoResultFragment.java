package com.sysu.edu.academic;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.alibaba.fastjson2.JSONObject;
import com.sysu.edu.R;
import com.sysu.edu.api.HttpManager;
import com.sysu.edu.api.Params;
import com.sysu.edu.api.TargetUrl;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;

public class AssistantInfoResultFragment extends StaggeredFragment {

    HttpManager http;

    int page = 1;
    int total = -1;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        Params params = new Params(requireActivity());
        params.setCallback(this, this::getResult);
        Handler handler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                super.handleMessage(msg);
                if (msg.what == -1) {
                    params.toast(R.string.login_warning);
                } else {
                    System.out.println(msg.obj);
                    JSONObject response = JSONObject.parseObject((String) msg.obj);
                    if (response.getInteger("code") == 200) {
                        if (msg.what == 0) {
                            total = response.getJSONObject("data").getInteger("total");
                            response.getJSONObject("data").getJSONArray("rows").forEach(o -> {
                                ArrayList<String> values = new ArrayList<>();
                                for (String i : new String[]{"rowNum", "semester", "studyCampus", "openUnitName", "courseName", "courseNum", "courseHour", "classNumber", "apersonNum", "teacherName", "teachingTimePlace", "studyObj", "stuList", "assistantInfo", "jobDuty"}) {
                                    values.add(((JSONObject) o).getString(i));
                                }
                                add(values.get(4), List.of("序号", "学年学期", "校区", "开设单位", "课程名称", "课程编号", "课程学时", "班级编号", "实选人数", "任课教师", "上课时间地点", "修读对象", "上课学生名单", "助教信息", "助教职责"), values);
                            });
                        }
                    } else if (response.getInteger("code") == 50041000) {
                        params.toast(response.getString("message"));
                    }
                    else{
                        params.toast(R.string.login_warning);
                        params.gotoLogin(view, TargetUrl.JWXT);
                    }
                }
            }
        };
        http = new HttpManager(handler);
        http.setParams(params);
        http.setReferrer("https://jwxt.sysu.edu.cn/jwxt/mk/zjgl/");
        getResult();
        setScrollBottom(() -> {
            if (page * 10 >= total) return;
            getResult();
        });
        return view;
    }

    void getResult(String query) {
        http.postRequest("https://jwxt.sysu.edu.cn/jwxt/assistant-manage/assistantInfoQuery/pageList?code=jwxsd_zjxxck", String.format("{\"pageNo\":%s,\"pageSize\":10,\"total\":true,\"param\":%s}", page++, query), 0);
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
    }//{"semester":"2025-1","studyCampusCode":"5063559","courseNum":"1","courseName":"1","teacherName":"1"}
}
