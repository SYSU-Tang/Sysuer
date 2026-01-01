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
import com.sysu.edu.api.Params;
import com.sysu.edu.api.TargetUrl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class CourseCompletionFragment extends StaggeredFragment {
    String cookie;
    Handler handler;
    OkHttpClient http = new OkHttpClient.Builder().build();
    int total;
    int page;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        page = 0;
        /*binding.recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView v, int dx, int dy) {
                if (!v.canScrollVertically(1) && total / 10 + 1 >= page) {
                    //getStudentCourse();
                }
                super.onScrolled(v, dx, dy);
            }
        });*/
        Params params = new Params(requireActivity());
        cookie = params.getCookie();
        params.setCallback(this, () -> {
            cookie = params.getCookie();
            page = 0;
            getStudentCourse();
        });
        handler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                if (msg.what == -1) {
                    params.toast(R.string.no_wifi_warning);
                } else {
                    JSONObject response = JSONObject.parseObject((String) msg.obj);
                    if (response != null && response.getInteger("code").equals(200)) {
                        if (response.get("data") != null) {
                            if (msg.what == 1) {
                                JSONObject data = response.getJSONObject("data");
                                total = data.getInteger("total");
                                data.getJSONArray("rows").forEach(a -> {
                                    ArrayList<String> values = new ArrayList<>();
                                    for (String key : new String[]{"acadYearSemester", "courseNumber", "courseName", "courseCategoryName", "credit",/**/"acadYearSemester", "achievementCourseNumber", "achievementCourseName", "achievementCourseCategoryName", "achievementCredit", "ispassed", "achievementPoint"}) {
                                        values.add(((JSONObject) a).getString(key));
                                    }
                                    if (values.get(0) != null) {
                                        values.set(0, values.get(0).replace(",", "|"));
                                    }
                                    if (values.get(5) != null) {
                                        values.set(5, values.get(5).replace(",", "|"));
                                    }
                                    add(requireContext(), values.get(2), List.of("学年学期", "课程号", "课程名称", "课程类别", "学分", "成绩获取学年学期", "课程号", "课程名称", "课程类别", "学分", "是否及格", "成绩"), values);
                                });
                            }
                        }
                    } else if (response != null && response.getInteger("code").equals(50030000)) {
                        params.toast(response.getString("message"));
                    } else {
                        params.toast(R.string.login_warning);
                        params.gotoLogin(getView(), TargetUrl.JWXT);
                    }
                }
            }
        };
        getStudentCourse();
        return view;
    }

    void getStudentCourse() {
        page++;
        http.newCall(new Request.Builder().url("https://jwxt.sysu.edu.cn/jwxt/gradua-degree/graduatemsg/studentsGraduationExamination/studentCourse")
                .header("Cookie", cookie)
                .post(RequestBody.create(String.format(Locale.getDefault(), "{\"pageNo\":%d,\"pageSize\":10,\"total\":true,\"param\":{\"cultureTypeCode\":\"01\"}}", page), MediaType.parse("application/json")))
                .header("Referer", "https://jwxt.sysu.edu.cn/jwxt/mk/gradua/")
                .build()).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Message msg = new Message();
                msg.what = -1;
                handler.sendMessage(msg);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                Message msg = new Message();
                msg.what = 1;
                msg.obj = response.body().string();
                handler.sendMessage(msg);
            }
        });
    }
}
