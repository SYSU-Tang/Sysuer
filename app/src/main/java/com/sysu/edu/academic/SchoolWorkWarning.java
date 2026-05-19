package com.sysu.edu.academic;

import static com.sysu.edu.api.CommonUtil.extractValue;
import static com.sysu.edu.api.CommonUtil.isEmpty;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.alibaba.fastjson2.JSONObject;
import com.sysu.edu.R;
import com.sysu.edu.databinding.ActivityListBinding;
import com.sysu.edu.model.JwxtModel;
import com.sysu.edu.view.StaggeredFragment;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

public class SchoolWorkWarning extends AppCompatActivity {
    
    String alarmOperationTerm;
    String alarmTerm;
    int page = 0;
    int total = -1;
    StaggeredFragment fragment;
    JwxtModel model;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityListBinding binding = ActivityListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        model = new JwxtModel(this);
        fragment = binding.list.getFragment();
        fragment.setScrollBottom(() -> {
            if (total > page * 10)
                getWarning();
        });
        fragment.setViewTableMenu(binding.toolbar);
        binding.toolbar.setTitle(R.string.school_work_warning);
        binding.toolbar.setNavigationOnClickListener(_ -> supportFinishAfterTransition());
        model.getMessage().observe(this, message -> {
            JSONObject response = message.getSecond();
            if (response != null && response.getInteger("code").equals(200)) {
                JSONObject data = response.getJSONObject("data");
                if (data != null) {
                    if (total == -1)
                        total = data.getInteger("total");
                    AtomicInteger order = new AtomicInteger(0);
                    data.getJSONArray("rows").forEach(a -> fragment.add(String.valueOf(order.incrementAndGet()), R.drawable.warning, List.of(new String[]{"预警结果", "预警操作学期", "预警学期", "生成预警档案时间", "档案ID", "警告程度"}),
                            extractValue((JSONObject) a, new String[]{"alarmResultName", "alarmOperationTerm", "alarmTerm", "createTime", "archivceID", "alarmResult"})));
                }
            }
        });
        getWarning();
    }

//    void clear() {
//        page = 0;
//        total = -1;
//        fragment.clear();
//    }
    
    void getWarning() {
        model.addAndNext("jwxt/alarm/alarm-archives/student/archives",
                String.format(Locale.getDefault(), "{\"pageNo\":%d,\"pageSize\":10,\"total\":true,\"param\":{\"publicationStatus\":\"1\"%s%s}}", ++page, getTerm(alarmTerm), getTerm(alarmOperationTerm)),
                0);
    }
    
    String getTerm(String s) {
        return isEmpty(s) ? "" : String.format(",\"alarmOperationTerm\":\"%s\"", s);
    }
}