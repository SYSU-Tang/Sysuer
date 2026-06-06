package com.sysu.edu.life;

import static com.sysu.edu.api.CommonUtil.extractValue;

import android.os.Bundle;
import android.view.MenuItem;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.MutableLiveData;

import com.alibaba.fastjson2.JSONObject;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.tabs.TabLayoutMediator;
import com.sysu.edu.R;
import com.sysu.edu.databinding.ActivityPagerBinding;
import com.sysu.edu.databinding.ItemSchoolBusNoticeBinding;
import com.sysu.edu.model.PortalModel;
import com.sysu.edu.view.Pager2Adapter;
import com.sysu.edu.view.StaggeredFragment;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

public class SchoolBusActivity extends AppCompatActivity {
    
    final MutableLiveData<Boolean> day = new MutableLiveData<>(true);
    JSONObject data;
    PortalModel model;
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        model.dispose();
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityPagerBinding binding = ActivityPagerBinding.inflate(getLayoutInflater());
        model = new PortalModel(this);
        setContentView(binding.getRoot());
        final ArrayList<String> routes = new ArrayList<>();
        binding.toolbar.setNavigationOnClickListener(_ -> supportFinishAfterTransition());
        binding.toolbar.setTitle(R.string.school_bus);
        Pager2Adapter pager2Adapter = new Pager2Adapter(this);
        binding.pager.setAdapter(pager2Adapter);
        ItemSchoolBusNoticeBinding header = ItemSchoolBusNoticeBinding.inflate(getLayoutInflater(), binding.appBarLayout, false);
//        header.day.setOnClickListener(_ -> day.setValue(Boolean.FALSE.equals(day.getValue())));
        header.date.addOnButtonCheckedListener((_, i, b) -> {
            if (i == R.id.workday)
                day.setValue(b);
        });
        AlertDialog notice = new MaterialAlertDialogBuilder(this).setTitle(R.string.notice).setPositiveButton(R.string.confirm, null).create();
        header.notice.setOnClickListener(_ -> notice.show());
        header.option.setOnItemClickListener((_, _, position, _) -> binding.pager.setCurrentItem(position));
        day.observe(this, b -> {
            String key = Boolean.TRUE.equals(b) ? "workDay" : "holiday";
            if (data != null) {
                if (data.getJSONArray(key).isEmpty())
                    IntStream.range(0, pager2Adapter.getItemCount()).forEach(j -> ((StaggeredFragment) pager2Adapter.get(j)).clear());
                else {
                    AtomicInteger i = new AtomicInteger(0);
                    data.getJSONArray(key).forEach(a -> {
                        JSONObject item = (JSONObject) a;
                        StaggeredFragment fragment;
                        if (pager2Adapter.getItemCount() > i.get()) {
                            fragment = (StaggeredFragment) pager2Adapter.get(i.get());
                            fragment.clear();
                        } else {
                            routes.add(item.getString("drivingDirectionName"));
                            fragment = new StaggeredFragment();
                            pager2Adapter.add(fragment);
                        }
                        i.getAndIncrement();
                        notice.setMessage(item.getString("note"));
                        fragment.add(getString(R.string.route_detail), R.drawable.bus, List.of(getString(R.string.route), getString(R.string.start), getString(R.string.end)),
                                extractValue(item, new String[]{"drivingDirectionName", "startStation", "endStation"}));
                        item.getJSONArray("schoolBusShuttleMomentList").forEach(o -> fragment.add(((JSONObject) o).getString("time"), R.drawable.bus, List.of(getString(R.string.passenger), getString(R.string.vehicles), getString(R.string.time), getString(R.string.route)),
                                extractValue(((JSONObject) o), new String[]{"passenger", "vehiclesType", "time", "drivingRoute"})));
                    });
                    header.option.setSimpleItems(routes.toArray(new String[0]));
                }
            }
        });
        binding.toolbar.getMenu().add(R.string.export).setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_IF_ROOM).setIcon(R.drawable.export).setOnMenuItemClickListener(_ -> {
            if (pager2Adapter.getItemCount() > 0) {
                int currentItem = binding.pager.getCurrentItem();
                ((StaggeredFragment) pager2Adapter.get(currentItem)).export(binding.toolbar, Objects.requireNonNull(Objects.requireNonNull(binding.tabs.getTabAt(currentItem)).getText()).toString());
            }
            return true;
        });
        binding.appBarLayout.addView(header.getRoot());
        new TabLayoutMediator(binding.tabs, binding.pager, (tab, position) -> tab.setText(routes.get(position))).attach();
        model.getMessage().observe(this, message -> {
            JSONObject response = message.getSecond();
            if (response.getJSONObject("meta").getInteger("statusCode").equals(200)) {
                if (message.getFirst() == 0) {
                    data = response.getJSONObject("data");
                    day.setValue(Boolean.TRUE);
                }
            }
        });
        getData();
    }
    
    void getData() {
        model.addAndNext("newClient/api/extraCard/schoolBusShuttleInfo/selectSchoolBusMap", 0);
    }
}