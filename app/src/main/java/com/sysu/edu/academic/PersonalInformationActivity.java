package com.sysu.edu.academic;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.alibaba.fastjson2.JSONObject;
import com.google.android.material.tabs.TabLayoutMediator;
import com.sysu.edu.R;
import com.sysu.edu.api.AuthorizationManager;
import com.sysu.edu.api.HttpManager;
import com.sysu.edu.api.Params;
import com.sysu.edu.api.TargetUrl;
import com.sysu.edu.databinding.ActivityPagerBinding;
import com.sysu.edu.view.Pager2Adapter;
import com.sysu.edu.view.StaggeredFragment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class PersonalInformationActivity extends AppCompatActivity {


    HttpManager http;
    final AuthorizationManager auth = new AuthorizationManager("https://xgxt.sysu.edu.cn/", "https://xgxt-443.webvpn.sysu.edu.cn/");
    final ArrayList<String> tabs = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityPagerBinding binding = ActivityPagerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        Params params = new Params(this);
        params.setCallback(this::getPersonalInfo);
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
        http = new HttpManager(new Handler(getMainLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                super.handleMessage(msg);
                boolean isJSON = msg.getData().getBoolean("isJSON");
                int code = msg.getData().getInt("code");
                if (code == 0) {
                    auth.setAccessible(false);
                    getPersonalInfo();
                    return;
                }
                String response = (String) msg.obj;
                if (response == null) {
                    params.toast(R.string.no_net_connected);
                    return;
                }
                if (!isJSON) {
                    if (!auth.isAuthorized(response)) {
                        params.toast(R.string.login_warning);
                        params.gotoLogin(auth.isAccessible() ? TargetUrl.XGXT : TargetUrl.XGXT_WEBVPN);
                        return;
                    }
                    if (!auth.isAccessible(response)) {
                        params.toast(R.string.educational_wifi_warning);
                        getPersonalInfo();
                        return;
                    }
                }
                if (msg.what == -1) {
                    params.toast(R.string.no_net_connected);
                } else {
                    JSONObject data = JSONObject.parseObject(response);
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
                                        if ("gx".equals(k) || "gxrzzmm".equals(k) || "qdxl".equals(k)) {
                                            values.add(((JSONObject) v).getString("label"));
                                        } else {
                                            values.add(v == null ? "" : v.toString());
                                        }
                                    });
                                    list.add(String.valueOf(count.getAndIncrement()), keys, values);
                                });
                            } else {
                                ArrayList<String> keys = new ArrayList<>();
                                ArrayList<String> values = new ArrayList<>();
                                item.getJSONObject("data").forEach((k, v) -> {
                                    keys.add(dict.getOrDefault(k, k));
                                    values.add(v == null ? "" : v.toString());
                                });
                                list.add(item.getString("zdflmc"), keys, values);
                            }
                        });
                    } else if (data.getJSONObject("meta").getInteger("statusCode") == 302) {
                        params.toast(R.string.login_warning);
                        params.gotoLogin(auth.isAccessible() ? TargetUrl.XGXT : TargetUrl.XGXT_WEBVPN);
                    }
                }
            }
        });
        http.setParams(params);
        getPersonalInfo();
    }

    void getPersonalInfo() {
        http.getRequest(auth.getBaseUrl() + "xsxx/api/sm-xsxx/info/student/view", 0);
    }
}