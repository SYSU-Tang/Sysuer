package com.sysu.edu.academic;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.google.android.material.snackbar.Snackbar;
import com.sysu.edu.R;
import com.sysu.edu.api.Params;
import com.sysu.edu.api.TargetUrl;
import com.sysu.edu.databinding.ActivityExamBinding;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ExamActivity extends AppCompatActivity {

    final OkHttpClient http = new OkHttpClient.Builder().build();
    Params params;
    Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityExamBinding binding = ActivityExamBinding.inflate(getLayoutInflater());
        params = new Params(this);
        params.setCallback(this::getTerms);
        ExamViewModel model = new ViewModelProvider(this).get(ExamViewModel.class);
        model.getTermList().observe(this, terms -> binding.terms.setSimpleItems(terms.toArray(new String[]{})));
        model.getTerm().observe(this, term -> {
            binding.terms.setText(term, false);
            getExamWeek(term);
        });
        model.getExamWeekList().observe(this, examWeeks -> binding.examWeeks.setSimpleItems(examWeeks.toArray(new String[]{})));
        setContentView(binding.getRoot());
        binding.toolbar.setNavigationOnClickListener(view -> supportFinishAfterTransition());
        binding.fab.setOnClickListener(view -> {
            if (model.getTerm().getValue() == null || model.getExamWeekId().getValue() == null) {
                Snackbar.make(view, "请选择考试周", Snackbar.LENGTH_LONG)
                        .setAnchorView(R.id.fab).show();
            } else {
                Snackbar.make(view, "查询中...", Snackbar.LENGTH_LONG)
                        .setAnchorView(R.id.fab).show();
                getResult(model.getTerm().getValue(), model.getExamWeekId().getValue());
            }
        });
        binding.terms.setOnItemClickListener((adapterView, view, i, l) -> model.setTerm(String.valueOf(binding.terms.getText())));
        binding.examWeeks.setOnItemClickListener((adapterView, view, i, l) -> {
            model.setExamWeekId(Objects.requireNonNull(model.getExamWeekInfo().getValue()).get(i).getString("examWeekId"));
            binding.date.setText(String.format("%s~%s", model.getExamWeekInfo().getValue().get(i).getString("startDate"), model.getExamWeekInfo().getValue().get(i).getString("endDate")));
            model.setExamWeek(Objects.requireNonNull(model.getExamWeekList().getValue()).get(i));
        });
        model.getExamResult().observe(this, result -> {
            ((StaggeredFragment) binding.examFragment.getFragment()).clear();
            JSONArray.parse(result).forEach(a -> ((JSONObject) a).getJSONObject("timetable").forEach((time, detail) -> {
                if (detail != null) {
                    ArrayList<String> values = new ArrayList<>();
                    ((JSONArray) detail).forEach(o -> {
                        for (String i : new String[]{"examSubjectName", "classroomNumber", "durationTime", "examDate", "acadYear"}) {
                            values.add(((JSONObject) o).getString(i));
                        }
                    });
                    ((StaggeredFragment) binding.examFragment.getFragment()).add(this, time, List.of("科目", "考场", "时长", "日期", "学年"), values);
                }
            }));
        });
        handler = new Handler(getMainLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                super.handleMessage(msg);
                if (msg.what == -1) {
                    params.toast(R.string.no_wifi_warning);
                } else {
                    JSONObject response = JSON.parseObject((String) msg.obj);
                    if (response.getInteger("code").equals(200)) {
                        switch (msg.what) {
                            case 1:
                                ArrayList<String> terms = new ArrayList<>();
                                response.getJSONArray("data").forEach(item -> terms.add(((JSONObject) item).getString("acadYearSemester")));
                                model.setTermList(terms);
                                getTerm();
                                break;
                            case 2:
                                model.setTerm(response.getJSONObject("data").getString("acadYearSemester"));
                                break;
                            case 3:
                                ArrayList<String> examWeeks = new ArrayList<>();
                                ArrayList<JSONObject> examWeekInfo = new ArrayList<>();
                                response.getJSONArray("data").forEach(item -> {
                                    examWeeks.add(((JSONObject) item).getString("examWeekName"));
                                    examWeekInfo.add((JSONObject) item);
                                });
                                model.setExamWeekInfo(examWeekInfo);
                                model.setExamWeekList(examWeeks);
                                //binding.examWeek.setText(response.getJSONObject("data").getString("examWeekName"),false);
                                break;
                            case 4:
                                model.setExamResult(response.getJSONArray("data").toJSONString());
                                break;
                        }
                    } else if (response.getInteger("code").equals(53000007)) {
                        params.toast(R.string.login_warning);
                        params.gotoLogin(binding.toolbar, TargetUrl.JWXT);
                    }
                }
            }
        };
        getTerms();
    }

    void getTerms() {
        http.newCall(new Request.Builder().url("https://jwxt.sysu.edu.cn/jwxt/base-info/acadyearterm/findAcadyeartermNamesBox")
                .header("Cookie", params.getCookie())
                .header("Referer", "https://jwxt.sysu.edu.cn/jwxt/mk/").build()).enqueue(
                new Callback() {
                    @Override
                    public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                        Message message = new Message();
                        message.what = 1;
                        message.obj = response.body().string();
                        handler.sendMessage(message);
                    }

                    @Override
                    public void onFailure(@NonNull Call call, @NonNull IOException e) {
                        Message message = new Message();
                        message.what = -1;
                        handler.sendMessage(message);
                    }
                }
        );
    }

    void getTerm() {
        http.newCall(new Request.Builder().url("https://jwxt.sysu.edu.cn/jwxt/base-info/acadyearterm/showNewAcadlist")
                .header("Cookie", params.getCookie())
                .header("Referer", "https://jwxt.sysu.edu.cn/jwxt/mk/").build()).enqueue(
                new Callback() {
                    @Override
                    public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                        Message message = new Message();
                        message.what = 2;
                        message.obj = response.body().string();
                        handler.sendMessage(message);
                    }

                    @Override
                    public void onFailure(@NonNull Call call, @NonNull IOException e) {
                        Message message = new Message();
                        message.what = -1;
                        handler.sendMessage(message);
                    }
                }
        );
    }

    void getExamWeek(String term) {
        http.newCall(new Request.Builder().url("https://jwxt.sysu.edu.cn/jwxt/schedule/agg/commonScheduleExamTime/queryExamWeekName?yearTerm=" + term)
                .header("Cookie", params.getCookie())
                .header("Referer", "https://jwxt.sysu.edu.cn/jwxt/mk/").build()).enqueue(
                new Callback() {
                    @Override
                    public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                        Message message = new Message();
                        message.what = 3;
                        message.obj = response.body().string();
                        handler.sendMessage(message);
                    }

                    @Override
                    public void onFailure(@NonNull Call call, @NonNull IOException e) {
                        Message message = new Message();
                        message.what = -1;
                        handler.sendMessage(message);
                    }
                }
        );
    }

    void getResult(String term, String examWeek) {
        String body = "";
        if (term != null) {
            body += "\"acadYear\":\"" + term + "\"";
        }
        if (examWeek != null) {
            body += ",\"examWeekName\":\"" + examWeek + "\"";
        }
        http.newCall(new Request.Builder().url("https://jwxt.sysu.edu.cn/jwxt/examination-manage/classroomResource/queryStuEaxmInfo")
                .header("Cookie", params.getCookie())
                .post(RequestBody.create(String.format("{%s}", body), MediaType.parse("application/json")))
                .header("Referer", "https://jwxt.sysu.edu.cn/jwxt/mk/").build()).enqueue(
                new Callback() {
                    @Override
                    public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                        Message message = new Message();
                        message.what = 4;
                        message.obj = response.body().string();
                        handler.sendMessage(message);
                    }

                    @Override
                    public void onFailure(@NonNull Call call, @NonNull IOException e) {
                        Message message = new Message();
                        message.what = -1;
                        handler.sendMessage(message);
                    }
                }
        );
    }
}