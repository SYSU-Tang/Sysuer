package com.sysu.edu.academic;

import static com.sysu.edu.api.CommonUtil.extractValue;

import android.os.Bundle;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.google.android.material.tabs.TabLayoutMediator;
import com.sysu.edu.BaseActivity;
import com.sysu.edu.R;
import com.sysu.edu.databinding.ActivityPagerBinding;
import com.sysu.edu.model.JwxtModel;
import com.sysu.edu.view.Pager2Adapter;
import com.sysu.edu.view.StaggeredFragment;

import java.util.List;
import java.util.Locale;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class RegistrationActivity extends BaseActivity {
    
    JwxtModel model;
    int page = 0;
    Pager2Adapter adp;
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        model.dispose();
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityPagerBinding binding = ActivityPagerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        model = new JwxtModel(this);
        adp = new Pager2Adapter(this);
        binding.pager.setAdapter(adp);
        Stream.of("2024", "2025", "2026", "2027").forEach(i -> binding.toolbar.getMenu().add(i).setOnMenuItemClickListener(_ -> {
            ((StaggeredFragment) adp.get(1)).clear();
            getPay(i);
            model.nextAll();
            return false;
        }));
        IntStream.range(0, 3).forEach(i -> {
            adp.add(StaggeredFragment.newInstance(i));
            getNextPage(i);
        });
        new TabLayoutMediator(binding.tabs, binding.pager, (tab, position) -> tab.setText(getResources().getStringArray(R.array.registration_info)[position])).attach();
        binding.toolbar.setNavigationOnClickListener(_ -> supportFinishAfterTransition());
        binding.toolbar.setTitle(R.string.register_info);
        model.getMessage().observe(this, msg -> {
            JSONObject response = msg.getSecond();
            if (response != null && response.getInteger("code").equals(200)) {
                if (response.get("data") != null) {
                    Integer what = msg.getFirst();
                    switch (what) {
                        case 2 -> {
                            JSONObject data = response.getJSONObject("data");
                            int total = data.getInteger("total");
                            data.getJSONArray("rows").forEach(a -> ((StaggeredFragment) adp.get(2)).add(((JSONObject) a).getString("academicYearTerm"), R.drawable.calendar, List.of(new String[]{"学年学期", "校区", "学院", "年级专业", "缴费状态", "报到状态", "注册状态", "报到日期", "注册日期"}),
                                    extractValue((JSONObject) a, new String[]{"academicYearTerm", "campusName", "collegeName", "gradeMajorName", "payedStatusName", "checkInStatusName", "registerStatusName", "checkInDate", "registerDate"})));
                            if (total / 10 > page - 1) getList();
                        }
                        case 0 ->
                                ((StaggeredFragment) adp.get(0)).add("学生报到信息", R.drawable.calendar, List.of("学号", "注册学年学期", "报到状态", "注册状态", "缴费状态"),
                                        extractValue(response.getJSONObject("data"), new String[]{"stuNum", "academicYearTerm", "checkInStatusName", "registerStatusName", "payedStatusName"}));
                        case 1 -> {
                            JSONArray d = response.getJSONArray("data");
                            d.forEach(v -> {
                                StaggeredFragment page2 = (StaggeredFragment) adp.get(1);
                                page2.setHideNull(true);
                                page2.add(((JSONObject) v).getString("acadYear"), R.drawable.money, List.of(new String[]{"年份", "类别", "项目名称", "金额（元）", "区间", "时间"}),
                                        extractValue((JSONObject) v, new String[]{"acadYear", "typeName", "feeTypeName", "payedItemAmount", "feeTimeSection", "editeTime"}));
                            });
                        }
                    }
                    model.nextAll();
                }
            }
        });
        model.next();
    }
    
    void getNextPage(int what) {
        switch (what) {
            case 0 -> getInfo();
            case 1 -> getPay();
            case 2 -> getList();
        }
    }
    
    void getInfo() {
        model.add("jwxt/reports-register/stuRegistration/getSelfRegisterInfo", 0);
    }
    
    void getPay() {
        getPay("2025");
    }
    
    void getPay(String year) {
        model.add("jwxt/reports-register/stuRegistration/getSelfPayInfoDetail?acadYear=" + year, 1);
        
    }
    
    void getList() {
        model.add("jwxt/reports-register/stuRegistration/getSelfRegisterList", String.format(Locale.getDefault(), "{\"pageNo\":%d,\"pageSize\":10,\"total\":true,\"param\":{}}", ++page), 2);
    }
}