package com.sysu.edu.academic;

import static com.sysu.edu.api.CommonUtil.toStringOrDefault;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.sysu.edu.databinding.ActivityLeaveReturnRegistrationBinding;
import com.sysu.edu.model.XgxtModel;

import java.util.ArrayList;

public class LeaveReturnRegistrationActivity extends AppCompatActivity {
    
    XgxtModel model;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityLeaveReturnRegistrationBinding binding = ActivityLeaveReturnRegistrationBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        model = new XgxtModel(this);
        binding.toolbar.setNavigationOnClickListener(_ -> supportFinishAfterTransition());
        LeaveReturnRegistrationViewModel viewModel = new ViewModelProvider(this).get(LeaveReturnRegistrationViewModel.class);
        model.getMessage().observe(this, message -> {
            JSONObject response = message.getSecond();
            if (response != null && response.getInteger("code") == 200) {
                if (message.getFirst() == 0) {
                    JSONArray data;
                    if ((data = response.getJSONArray("data")) != null && !data.isEmpty()) {
                        ArrayList<String> years = new ArrayList<>();
                        data.forEach(o -> years.add(toStringOrDefault(((JSONObject) o).getString("label"), "")));
                        binding.years.setSimpleItems(years.toArray(new String[0]));
                        binding.years.setOnItemClickListener((_, _, position, _) -> viewModel.year.setValue(data.getJSONObject(position).getString("value")));
                        binding.years.setText(years.get(0), false);
                        viewModel.year.setValue(data.getJSONObject(0).getString("value"));
                    }
                }
            }
        });
        getYears();
    }
    
    void getYears() {
        model.addAndNext("jjrlfx/api/sm-jjrlfx/student/school-year", 0);
    }
}