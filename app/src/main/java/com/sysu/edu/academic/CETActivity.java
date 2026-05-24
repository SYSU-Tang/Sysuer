package com.sysu.edu.academic;

import static com.sysu.edu.api.CommonUtil.extractValue;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.alibaba.fastjson2.JSONObject;
import com.sysu.edu.databinding.ActivityListBinding;
import com.sysu.edu.model.JwxtModel;
import com.sysu.edu.view.StaggeredFragment;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

public class CETActivity extends AppCompatActivity {
    
    int page = 0;
    JwxtModel model;
    @Override
    protected void onDestroy() {
        super.onDestroy();
        model.dispose();
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityListBinding binding = ActivityListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        model = new JwxtModel(this);
        StaggeredFragment fragment = binding.list.getFragment();
        fragment.setViewTableMenu(binding.toolbar);
        binding.toolbar.setNavigationOnClickListener(_ -> supportFinishAfterTransition());
        getExchange();
        model.getMessage().observe(this, message -> {
            JSONObject response = message.getSecond();
            if (response.getInteger("code").equals(200)) {
                if (message.getFirst() == 0) {
                    JSONObject data = response.getJSONObject("data");
                    if (data != null) {
                        int total = data.getInteger("total");
                        AtomicInteger order = new AtomicInteger(1);
                        data.getJSONArray("rows").forEach(a -> fragment.add(String.valueOf(order.getAndIncrement()), List.of("考试年份", "上/下半年", "语言级别", "学号", "姓名", "笔试考试时间", "笔试准考证号", "笔试成绩总分", "听力分数", "阅读分数", "综合分数", "写作分数", "口试考试时间", "口试准考证号", "口语成绩", "所属学校", "院系", "专业", "年级", "班级", "笔试科目名称", "笔试报名号", "笔试报名学校", "笔试报名校区", "是否缺考", "是否违纪", "违纪类型", "是否听力障碍", "口试科目名称", "口试报名号", "口试报名学校", "口试报名校区"),
                                extractValue((JSONObject) a, new String[]{"examYear", "thePastOrNextHalfYearName", "languageLevel", "stuNum", "stuName", "writtenExaminationTime", "writtenExaminationNumber", "writtenExaminationTotalScore", "hearingScore", "readingScore", "comprehensiveScore", "writingScore", "oralExamTime", "oralExamNumber", "oralExamAchievement", "schoolName", "collegeName", "professionName", "grade", "stuClassName", "writtenExaminationSubject", "writtenExaminationApplyNumber", "writtenExaminationApplySchool", "writtenExaminationApplyCampus", "whetherMissingTest", "whetherViolation", "violationType", "whetherHearingObstacle", "oralExamSubject", "oralExamApplyNumber", "oralExamApplySchool", "oralExamApplyCampus"})));
                        if (total > page * 10) getExchange();
                    }
                }
                model.nextAll();
            } 
        });
        model.next();
    }
    
    void getExchange() {
        model.add("jwxt/achievement-manage/englishGradeAchievement/stuPageList", String.format(Locale.getDefault(), "{\"pageNo\":%s,\"pageSize\":10,\"total\":true,\"param\":{}}", ++page), 0);
    }
}