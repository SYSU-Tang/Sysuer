package com.sysu.edu.academic;

import static com.sysu.edu.api.CommonUtil.extractValue;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.google.android.material.tabs.TabLayoutMediator;
import com.sysu.edu.R;
import com.sysu.edu.api.HttpManager;
import com.sysu.edu.api.Params;
import com.sysu.edu.api.TargetUrl;
import com.sysu.edu.databinding.ActivityPagerBinding;
import com.sysu.edu.view.Pager2Adapter;
import com.sysu.edu.view.StaggeredFragment;

import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

public class RegistrationActivity extends AppCompatActivity {

    HttpManager http;
    int page = 0;
    Pager2Adapter adp;
    boolean changeYear = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityPagerBinding binding = ActivityPagerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        Params params = new Params(this);
        params.setCallback(() -> getNextPage(0));
        adp = new Pager2Adapter(this);
        binding.pager.setAdapter(adp);
        Stream.of("2024", "2025", "2026").forEach(i -> binding.toolbar.getMenu().add(i).setOnMenuItemClickListener(_ -> {
            ((StaggeredFragment) adp.get(1)).clear();
            getPay(i);
            return false;
        }));
        new TabLayoutMediator(binding.tabs, binding.pager, (tab, position) -> tab.setText(getResources().getStringArray(R.array.registration_info)[position])).attach();
        binding.toolbar.setNavigationOnClickListener(_ -> supportFinishAfterTransition());
        binding.toolbar.setTitle(R.string.register_info);
        http = new HttpManager(new Handler(getMainLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                if (msg.what == -1) {
                    params.toast(R.string.no_net_connected);
                } else {
                    JSONObject response = JSONObject.parseObject((String) msg.obj);
                    if (response != null && response.getInteger("code").equals(200)) {
                        if (response.get("data") != null) {
                            switch (msg.what) {
                                case 2: {
                                    JSONObject data = response.getJSONObject("data");
                                    int total = data.getInteger("total");
                                    data.getJSONArray("rows").forEach(a -> ((StaggeredFragment) adp.get(2)).add(((JSONObject) a).getString("academicYearTerm"), R.drawable.calendar, List.of(new String[]{"学年学期", "校区", "学院", "年级专业", "缴费状态", "报到状态", "注册状态", "报到日期", "注册日期"}),
                                            extractValue((JSONObject) a, new String[]{"academicYearTerm", "campusName", "collegeName", "gradeMajorName", "payedStatusName", "checkInStatusName", "registerStatusName", "checkInDate", "registerDate"})));
                                    if (total / 10 > page - 1) {
                                        getList();
                                    } else {
                                        getNextPage(msg.what + 1);
                                    }
                                    break;
                                }
                                case 0: {
                                    ((StaggeredFragment) adp.get(0)).add("学生报到信息", R.drawable.calendar, List.of("学号", "注册学年学期", "报到状态", "注册状态", "缴费状态"),
                                            extractValue(response.getJSONObject("data"), new String[]{"stuNum", "academicYearTerm", "checkInStatusName", "registerStatusName", "payedStatusName"}));
                                    getNextPage(msg.what + 1);
                                    break;
                                }
                                case 1: {
                                    JSONArray d = response.getJSONArray("data");
                                    d.forEach(v -> {
                                        StaggeredFragment page2 = (StaggeredFragment) adp.get(1);
                                        page2.setHideNull(true);
                                        page2.add(((JSONObject) v).getString("acadYear"), R.drawable.money, List.of(new String[]{"年份", "类别", "项目名称", "金额（元）", "区间", "时间"}),
                                                extractValue((JSONObject) v, new String[]{"acadYear", "typeName", "feeTypeName", "payedItemAmount", "feeTimeSection", "editeTime"}));
                                    });
                                    getNextPage(msg.what + 1);
                                    break;
                                }
                            }
                        }
                    } else {
                        params.toast(R.string.login_warning);
                        params.gotoLogin(TargetUrl.JWXT);
                    }
                }
            }
        });
        http.setParams(params);
        http.setReferrer("https://jwxt.sysu.edu.cn/jwxt/mk/studentWeb/");
        getNextPage(0);
    }

    void getNextPage(int what) {
        if (!changeYear) {
            if (what < 3) adp.add(StaggeredFragment.newInstance(what));
            switch (what) {
                case 0 -> getInfo();
                case 1 -> getPay();
                case 2 -> getList();
                default -> changeYear = true;
            }
        }
    }

    void getInfo() {
        http.getRequest("https://jwxt.sysu.edu.cn/jwxt/reports-register/stuRegistration/getSelfRegisterInfo", 0);
    }

    void getPay() {
        getPay("2025");
    }

    void getPay(String year) {
        http.getRequest("https://jwxt.sysu.edu.cn/jwxt/reports-register/stuRegistration/getSelfPayInfoDetail?acadYear=" + year, 1);

    }

    void getList() {
        http.postRequest("https://jwxt.sysu.edu.cn/jwxt/reports-register/stuRegistration/getSelfRegisterList", String.format(Locale.getDefault(), "{\"pageNo\":%d,\"pageSize\":10,\"total\":true,\"param\":{}}", ++page), 2);
    }
}