package com.sysu.edu.academic;

import static com.sysu.edu.api.CommonUtil.toStringOrDefault;

import android.os.Bundle;
import android.view.MenuItem;

import androidx.appcompat.app.AppCompatActivity;

import com.alibaba.fastjson2.JSONObject;
import com.google.android.material.tabs.TabLayoutMediator;
import com.sysu.edu.R;
import com.sysu.edu.databinding.ActivityPagerBinding;
import com.sysu.edu.model.XgxtModel;
import com.sysu.edu.view.Pager2Adapter;
import com.sysu.edu.view.StaggeredFragment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class PersonalInformationActivity extends AppCompatActivity {
    
    
    XgxtModel model;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityPagerBinding binding = ActivityPagerBinding.inflate(getLayoutInflater());
        model = new XgxtModel(this);
        setContentView(binding.getRoot());
        final ArrayList<String> tabs = new ArrayList<>();
        Pager2Adapter pager2Adapter = new Pager2Adapter(this);
        binding.toolbar.setTitle(R.string.personal_info);
        binding.toolbar.setNavigationOnClickListener(_ -> supportFinishAfterTransition());
        binding.toolbar.getMenu().add(R.string.export).setIcon(R.drawable.export).setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_IF_ROOM).setOnMenuItemClickListener(
                _ -> {
                    if (pager2Adapter.getItemCount() > 0) {
                        int currentItem = binding.pager.getCurrentItem();
                        ((StaggeredFragment) pager2Adapter.get(currentItem)).export(binding.toolbar, tabs.get(currentItem));
                    }
                    return true;
                }
        );
        binding.pager.setAdapter(pager2Adapter);
        new TabLayoutMediator(binding.tabs, binding.pager, (tab, position) -> tab.setText(tabs.get(position))).attach();
        model.getMessage().observe(this, message -> {
            JSONObject data = message.getSecond();
            if (data.containsKey("code") && data.getInteger("code") == 200) {
                HashMap<String, String> dict = new HashMap<>();
                dict.put("bmmc", "部门");
                dict.put("id", "ID");
                dict.put("jgmc", "籍贯");
                dict.put("hjszdText", "高中所在地");
                dict.put("zjxymc", "宗教信仰");
                dict.put("sfzszdmc", "身份证所在地");
                dict.put("jkzkmc", "健康状况");
                dict.put("csd", "出生地");
                dict.put("kslbmc", "考生类别");
                dict.put("hyzk", "婚姻状况");
                dict.put("cjrbjText", "残疾人标记");
                dict.put("xxmc", "学校");
                dict.put("hyzkmc", "婚姻状况描述");
                data.getJSONArray("data").forEach(i -> {
                    JSONObject item = (JSONObject) i;
                    item.getJSONArray("fields").forEach(o -> {
                        JSONObject field = (JSONObject) o;
                        dict.put(field.getString("zdmc"), field.getString("zdzwm"));
                    });
                    StaggeredFragment list = new StaggeredFragment();
                    tabs.add(item.getString("zdflmc"));
                    pager2Adapter.add(list);
                    if (item.getJSONObject("data").isEmpty()) {
                        AtomicInteger count = new AtomicInteger(1);
                        item.getJSONArray("dataList").forEach(j -> {
                            ArrayList<String> keys = new ArrayList<>();
                            ArrayList<String> values = new ArrayList<>();
                            ((JSONObject) j).forEach((k, v) -> {
                                keys.add(dict.getOrDefault(k, k));
                                if ("gx".equals(k) || "gxrzzmm".equals(k) || "qdxl".equals(k))
                                    values.add(((JSONObject) v).getString("label"));
                                else values.add(toStringOrDefault(v));
                            });
                            list.add(String.valueOf(count.getAndIncrement()), keys, values);
                        });
                    } else {
                        ArrayList<String> keys = new ArrayList<>();
                        ArrayList<String> values = new ArrayList<>();
                        item.getJSONObject("data").forEach((k, v) -> {
                            keys.add(dict.getOrDefault(k, k));
                            values.add(toStringOrDefault(v));
                        });
                        list.add(item.getString("zdflmc"), keys, values);
                    }
                });
            }
        });
        getPersonalInfo();
    }
    
    void getPersonalInfo() {
        model.addAndNext("xsxx/api/sm-xsxx/info/student/view", 0);
    }
}