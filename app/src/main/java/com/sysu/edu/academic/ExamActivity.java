package com.sysu.edu.academic;

import static com.sysu.edu.api.CommonUtil.extractValue;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.google.android.material.snackbar.Snackbar;
import com.sysu.edu.R;
import com.sysu.edu.api.Params;
import com.sysu.edu.databinding.ActivityExamBinding;
import com.sysu.edu.model.JwxtModel;
import com.sysu.edu.view.StaggeredFragment;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ExamActivity extends AppCompatActivity {

    JwxtModel model;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityExamBinding binding = ActivityExamBinding.inflate(getLayoutInflater());
        Params params = new Params(this);
        model = new JwxtModel(this);
        params.setCallback(this::getTerms);
        ExamViewModel examViewModel = new ViewModelProvider(this).get(ExamViewModel.class);
        examViewModel.getTermList().observe(this, terms -> binding.terms.setSimpleItems(terms.toArray(new String[]{})));
        examViewModel.getTerm().observe(this, term -> {
            binding.terms.setText(term, false);
            getExamWeek(term);
        });
        examViewModel.getExamWeekList().observe(this, examWeeks -> binding.examWeeks.setSimpleItems(examWeeks.toArray(new String[]{})));
        setContentView(binding.getRoot());
        binding.toolbar.setNavigationOnClickListener(_ -> supportFinishAfterTransition());
        binding.fab.setOnClickListener(view -> {
            if (examViewModel.getTerm().getValue() == null || examViewModel.getExamWeekId().getValue() == null)
                Snackbar.make(view, "请选择考试周", Snackbar.LENGTH_LONG)
                        .setAnchorView(R.id.fab).show();
            else {
                Snackbar.make(view, "查询中...", Snackbar.LENGTH_LONG)
                        .setAnchorView(R.id.fab).show();
                getResult(examViewModel.getTerm().getValue(), examViewModel.getExamWeekId().getValue());
            }
        });
        binding.terms.setOnItemClickListener((_, _, _, _) -> examViewModel.setTerm(String.valueOf(binding.terms.getText())));
        binding.examWeeks.setOnItemClickListener((_, _, i, _) -> {
            examViewModel.setExamWeekId(Objects.requireNonNull(examViewModel.getExamWeekInfo().getValue()).get(i).getString("examWeekId"));
            binding.date.setText(String.format("%s~%s", examViewModel.getExamWeekInfo().getValue().get(i).getString("startDate"), examViewModel.getExamWeekInfo().getValue().get(i).getString("endDate")));
            examViewModel.setExamWeek(Objects.requireNonNull(examViewModel.getExamWeekList().getValue()).get(i));
        });
        examViewModel.getExamResult().observe(this, result -> {
            ((StaggeredFragment) binding.examFragment.getFragment()).clear();
            JSONArray.parse(result).forEach(a -> ((JSONObject) a).getJSONObject("timetable").forEach((time, detail) -> {
                if (detail != null) {
                    ArrayList<String> values = new ArrayList<>();
                    ((JSONArray) detail).forEach(o -> {
                        for (String i : new String[]{"examSubjectName", "classroomNumber", "durationTime", "examDate", "acadYear"})
                            values.add(((JSONObject) o).getString(i));
                    });
                    ((StaggeredFragment) binding.examFragment.getFragment()).add(time, List.of("科目", "考场", "时长", "日期", "学年"),
                            values);
                }
            }));
        });
        model.getMessage().observe(this,message->{
            JSONObject response = message.getSecond();
            if (response.getInteger("code").equals(200)) {
                switch (message.getFirst()) {
                    case 1->{
                        examViewModel.setTermList(extractValue(response.getJSONArray("data"), "acadYearSemester"));
                        getTerm();
                    }
                    case 2-> examViewModel.setTerm(response.getJSONObject("data").getString("acadYearSemester"));
                    case 3->{   
                        ArrayList<String> examWeeks = new ArrayList<>();
                        ArrayList<JSONObject> examWeekInfo = new ArrayList<>();
                        response.getJSONArray("data").forEach(item -> {
                            examWeeks.add(((JSONObject) item).getString("examWeekName"));
                            examWeekInfo.add((JSONObject) item);
                        });
                        examViewModel.setExamWeekInfo(examWeekInfo);
                        examViewModel.setExamWeekList(examWeeks);
                        //binding.examWeek.setText(response.getJSONObject("data").getString("examWeekName"),false);
                    }
                    case 4-> examViewModel.setExamResult(response.getJSONArray("data").toJSONString());
                }
            }
        });
        getTerms();
    }

    void getTerms() {
        model.addAndNext("jwxt/base-info/acadyearterm/findAcadyeartermNamesBox", 1);
    }

    void getTerm() {
        model.addAndNext("jwxt/base-info/acadyearterm/showNewAcadlist", 2);
    }

    void getExamWeek(String term) {
        model.addAndNext("jwxt/schedule/agg/commonScheduleExamTime/queryExamWeekName?yearTerm=" + term, 3);
    }

    void getResult(String term, String examWeek) {
        String body = "";
        if (term != null) body += "\"acadYear\":\"" + term + "\"";
        if (examWeek != null) body += ",\"examWeekName\":\"" + examWeek + "\"";
        model.addAndNext("jwxt/examination-manage/classroomResource/queryStuEaxmInfo", String.format("{%s}", body), 4);
    }
}