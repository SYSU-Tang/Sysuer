package com.sysu.edu.life;

import static com.sysu.edu.api.CommonUtil.extractValue;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.MutableLiveData;

import com.alibaba.fastjson2.JSONObject;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.tabs.TabLayoutMediator;
import com.sysu.edu.R;
import com.sysu.edu.api.HttpManager;
import com.sysu.edu.api.Params;
import com.sysu.edu.api.TargetUrl;
import com.sysu.edu.databinding.ActivityPagerBinding;
import com.sysu.edu.databinding.ItemSchoolBusNoticeBinding;
import com.sysu.edu.view.Pager2Adapter;
import com.sysu.edu.view.StaggeredFragment;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

public class SchoolBusActivity extends AppCompatActivity {
    HttpManager http;
    JSONObject data;
    final MutableLiveData<Boolean> day = new MutableLiveData<>(true);
    final ArrayList<String> routes = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityPagerBinding binding = ActivityPagerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.toolbar.setNavigationOnClickListener(_ -> supportFinishAfterTransition());
        Params params = new Params(this);
        params.setCallback(this::getData);
        binding.toolbar.setTitle(R.string.school_bus);
        Pager2Adapter pager2Adapter = new Pager2Adapter(this);
        binding.pager.setAdapter(pager2Adapter);
        ItemSchoolBusNoticeBinding header = ItemSchoolBusNoticeBinding.inflate(getLayoutInflater(), binding.appBarLayout, false);
        header.day.setOnClickListener(_ -> day.setValue(Boolean.FALSE.equals(day.getValue())));
        AlertDialog notice = new MaterialAlertDialogBuilder(this).setTitle(R.string.notice).create();
        header.notice.setOnClickListener(_ -> notice.show());
        header.option.setOnItemClickListener((_, _, position, _) -> binding.pager.setCurrentItem(position));
        day.observe(this, b -> {
            b = Boolean.TRUE.equals(b);
            loadRoute(b ? "workDay" : "holiday", header, notice, pager2Adapter);
            header.day.setText(b ? R.string.workday : R.string.holiday);
        });
        binding.toolbar.getMenu().add(R.string.export).setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_IF_ROOM).setIcon(R.drawable.export).setOnMenuItemClickListener(_ -> {
            int currentItem = binding.pager.getCurrentItem();
            ((StaggeredFragment) pager2Adapter.get(currentItem)).export(binding.toolbar, Objects.requireNonNull(Objects.requireNonNull(binding.tabs.getTabAt(currentItem)).getText()).toString());
            return true;
        });
        binding.appBarLayout.addView(header.getRoot());
        new TabLayoutMediator(binding.tabs, binding.pager, (tab, position) -> tab.setText(routes.get(position))).attach();
        http = new HttpManager(new Handler(getMainLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                System.out.println(msg);
                if (msg.what == -1) params.toast(R.string.no_net_connected);
                else if (!msg.getData().getBoolean("isJSON")) {
                    params.toast(R.string.login_warning);
                    params.gotoLogin(TargetUrl.PORTAL);
                } else {
                    JSONObject response = JSONObject.parseObject((String) msg.obj);
                    if (response != null && response.getJSONObject("meta").getInteger("statusCode").equals(200) && response.get("data") != null) {
                        if (msg.what == 0) {
                            data = response.getJSONObject("data");
                            day.setValue(Boolean.TRUE);
                        }
                    } else {
                        params.toast(getString(R.string.login_warning));
//                        params.gotoLogin(TargetUrl.PORTAL);
                    }
                }
            }
        });
        http.setParams(params);
        getData();
    }

    void loadRoute(String day, ItemSchoolBusNoticeBinding header, AlertDialog notice, Pager2Adapter adp) {
        AtomicInteger i = new AtomicInteger(0);
        if (data != null) {
            if (data.getJSONArray(day).isEmpty()) {
                IntStream.range(0, adp.getItemCount()).forEach(j -> ((StaggeredFragment) adp.get(j)).clear());
            } else {
                data.getJSONArray(day).forEach(a -> {
                    JSONObject item = (JSONObject) a;
                    StaggeredFragment fragment;
                    if (adp.getItemCount() > i.get()) {
                        fragment = (StaggeredFragment) adp.get(i.get());
                        fragment.clear();
                    } else {
                        routes.add(item.getString("drivingDirectionName"));
                        fragment = new StaggeredFragment();
                        adp.add(fragment);
                    }
                    i.getAndIncrement();
                    notice.setMessage(item.getString("note"));
                    fragment.add(getString(R.string.route_detail), R.drawable.bus, List.of("路线", "起点", "终点"),
                            extractValue(item, new String[]{"drivingDirectionName", "startStation", "endStation"}));
                    item.getJSONArray("schoolBusShuttleMomentList").forEach(b -> fragment.add(((JSONObject) b).getString("time"), R.drawable.bus, List.of("乘客", "车辆", "时间", "路线"),
                            extractValue(((JSONObject) b), new String[]{"passenger", "vehiclesType", "time", "drivingRoute"})));
                });
                header.option.setSimpleItems(routes.toArray(new String[0]));
            }
        }
    }

    void getData() {
        http.getRequest("https://portal.sysu.edu.cn/newClient/api/extraCard/schoolBusShuttleInfo/selectSchoolBusMap", 0);
    }
}