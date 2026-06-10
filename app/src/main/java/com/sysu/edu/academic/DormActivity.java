package com.sysu.edu.academic;

import static com.sysu.edu.api.CommonUtil.extractValue;
import static com.sysu.edu.api.CommonUtil.toStringOrDefault;

import android.os.Bundle;
import android.view.MenuItem;

import com.alibaba.fastjson2.JSONObject;
import com.google.android.material.tabs.TabLayoutMediator;
import com.sysu.edu.BaseActivity;
import com.sysu.edu.R;
import com.sysu.edu.api.CommonUtil;
import com.sysu.edu.databinding.ActivityPagerBinding;
import com.sysu.edu.model.XgxtModel;
import com.sysu.edu.view.Pager2Adapter;
import com.sysu.edu.view.StaggeredFragment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class DormActivity extends BaseActivity {
    
    XgxtModel model;
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        model.dispose();
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityPagerBinding binding = ActivityPagerBinding.inflate(getLayoutInflater());
        final ArrayList<String> tabs = new ArrayList<>();
        setContentView(binding.getRoot());
        model = new XgxtModel(this);
        binding.toolbar.setTitle(R.string.dorm);
        binding.toolbar.setNavigationOnClickListener(_ -> supportFinishAfterTransition());
        Pager2Adapter pager2Adapter = new Pager2Adapter(this);
        binding.pager.setAdapter(pager2Adapter);
        binding.toolbar.getMenu().add(R.string.export).setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_IF_ROOM).setIcon(R.drawable.export).setOnMenuItemClickListener(_ -> {
            int currentItem;
            if (!pager2Adapter.isEmpty() && (currentItem = binding.pager.getCurrentItem()) < pager2Adapter.getItemCount())
                ((StaggeredFragment) pager2Adapter.get(currentItem)).export(binding.toolbar, toStringOrDefault(Objects.requireNonNull(binding.tabs.getTabAt(currentItem)).getText()));
            return true;
        });
        new TabLayoutMediator(binding.tabs, binding.pager, (tab, position) -> tab.setText(tabs.get(position))).attach();
        model.getMessage().observe(this, message -> {
            JSONObject data = message.getSecond();
            if (data.containsKey("code") && data.getInteger("code") == 200) {
                data = data.getJSONObject("data");
                StaggeredFragment list = new StaggeredFragment();
                tabs.add(getString(R.string.personal_info));
                pager2Adapter.add(list);
                list.add(getString(R.string.personal_info), Arrays.asList(CommonUtil.getString(this, List.of(R.string.name, R.string.student_id, R.string.gender, R.string.school, R.string.major, R.string.grade, R.string.training_level, R.string.stay_school_status, R.string.student_status, R.string.contact_number))),
                        extractValue(data, new String[]{"name", "studentNumber", "gender", "academy", "major", "grade", "trainingLevel", "staySchoolStatus", "studentStatus", "contactNumber"}));
                StaggeredFragment list1 = new StaggeredFragment();
                tabs.add(getString(R.string.dorm_info));
                pager2Adapter.add(list1);
                data.getJSONArray("stayRecordList").forEach(e -> list1.add(((JSONObject) e).getString("schoolYear"), Arrays.asList(CommonUtil.getString(this, List.of(R.string.year, R.string.campus, R.string.building, R.string.floor, R.string.room_number, R.string.bed_number, R.string.accommodation_fee, R.string.stay_start_date, R.string.stay_end_date))),
                        extractValue((JSONObject) e, new String[]{"schoolYear", "campus", "buildingName", "floorName", "roomNumber", "bedNumber", "accommodationFee", "startDate", "endDate"})));
                StaggeredFragment list2 = new StaggeredFragment();
                tabs.add(getString(R.string.dorm_fee));
                pager2Adapter.add(list2);
                data.getJSONArray("stayChargeRecordList").forEach(e -> list2.add(((JSONObject) e).getString("schoolYear"), Arrays.asList(CommonUtil.getString(this, List.of(R.string.year, R.string.accommodation_standard, R.string.should_pay_stay_charge, R.string.real_pay_stay_charge, R.string.arrears))),
                        extractValue((JSONObject) e, new String[]{"schoolYear", "shouldPayStayCharge", "realPayStayCharge", "charge", "arrears"})));
                
            }
        });
        getDormInfo();
    }
    
    void getDormInfo() {
        model.addAndNext("ssgl/api/sm-ssgl/stu-info", 0);
    }
    
}