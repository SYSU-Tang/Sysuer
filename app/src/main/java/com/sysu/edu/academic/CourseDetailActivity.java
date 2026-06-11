package com.sysu.edu.academic;

import android.os.Bundle;
import android.view.View;

import com.alibaba.fastjson2.JSONObject;
import com.google.android.material.tabs.TabLayoutMediator;
import com.sysu.edu.BaseActivity;
import com.sysu.edu.R;
import com.sysu.edu.databinding.ActivityCourseDetailBinding;
import com.sysu.edu.model.JwxtModel;
import com.sysu.edu.view.Pager2Adapter;

public class CourseDetailActivity extends BaseActivity {
    
    JwxtModel model;
    String code;
    String id;
    String classNum;
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        model.dispose();
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityCourseDetailBinding binding = ActivityCourseDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        model = new JwxtModel(this);
        binding.toolbar.setNavigationOnClickListener(_ -> supportFinishAfterTransition());
        Pager2Adapter courseDetailPageAdapter = new Pager2Adapter(this);
        binding.pager.setAdapter(courseDetailPageAdapter);
        courseDetailPageAdapter.add(new CourseDetailFragment()).add(new CourseOutlineFragment());
        new TabLayoutMediator(binding.tabs, binding.pager, (tab, i) -> tab.setText(getString(new int[]{R.string.course_detail, R.string.course_draft}[i]))).attach();
        code = getIntent().getStringExtra("code");
        id = getIntent().getStringExtra("id");
        classNum = getIntent().getStringExtra("class");
        // code: EIT228, id: null, classNum: 202511441
        getDetail();
        model.getMessage().observe(this, message -> {
            JSONObject response = message.second;
            if (response.getInteger("code").equals(200)) {
                JSONObject data = response.getJSONObject("data");
                if (data != null) {
                    switch (message.first) {
                        case 1 -> {
                            Bundle bundle = new Bundle();
                            bundle.putInt("what", 1);
                            bundle.putString("data", data.getJSONObject("outlineInfo").toJSONString());
                            courseDetailPageAdapter.get(0).setArguments(bundle);
                            Bundle bundle2 = new Bundle();
                            bundle2.putString("data", data.getJSONArray("scheduleList").toJSONString());
                            courseDetailPageAdapter.get(1).setArguments(bundle2);
                            id = data.getJSONObject("outlineInfo").getString("courseId");
                            getCourseOutline2();
                        }
                        case 2 -> {
                            Bundle bundle = new Bundle();
                            bundle.putInt("what", 2);
                            bundle.putString("data", data.toString());
                            courseDetailPageAdapter.get(0).setArguments(bundle);
                        }
                        
                    }
                }
                model.nextAll();
            } else if (response.getInteger("code").equals(52000000))
                binding.pager.setVisibility(View.GONE);
        });
        model.next();
    }
    
    private void getDetail() {
        if (code == null || classNum == null) getCourseOutline2();
        else getCourseOutline();
    }
    
    void getCourseOutline() {
        model.add(String.format("jwxt/training-programe/courseoutline/getalloutlineinfo?courseNum=%s&auditStatus=99", code), 1);
    }
    
    void getCourseOutline2() {
        model.add(String.format("jwxt/base-info/courseLibrary/findById?id=%s", id), 2);
    }
}