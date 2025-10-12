package com.sysu.edu.life;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.alibaba.fastjson2.JSONObject;
import com.google.android.material.tabs.TabLayoutMediator;
import com.sysu.edu.R;
import com.sysu.edu.academic.Pager2Adapter;
import com.sysu.edu.academic.StaggeredFragment;
import com.sysu.edu.api.Params;
import com.sysu.edu.databinding.ActivityPagerBinding;
import com.sysu.edu.databinding.ItemSchoolBusNoticeBinding;
import com.sysu.edu.extra.LoginActivity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class SchoolBus extends AppCompatActivity {
    ActivityPagerBinding binding;
    String cookie;
    Handler handler;
    OkHttpClient http = new OkHttpClient.Builder().build();
    ArrayList<String> routes = new ArrayList<>();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPagerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.toolbar.setTitle(R.string.major_info);
        binding.toolbar.setNavigationOnClickListener(v->supportFinishAfterTransition());
        Params params = new Params(this);
        cookie  = params.getCookie();
        cookie = "sid=8354ef8c-2b83-4d63-913b-24098560543a;";
        ActivityResultLauncher<Intent> launch = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), o -> {
            if (o.getResultCode() == Activity.RESULT_OK) {
                cookie = params.getCookie();
                getData();
            }
        });
        binding.toolbar.setTitle(R.string.school_bus);
        Pager2Adapter adp = new Pager2Adapter(this);
        binding.pager.setAdapter(adp);
        ItemSchoolBusNoticeBinding notice = ItemSchoolBusNoticeBinding.inflate(getLayoutInflater(),binding.appBarLayout,false);
        binding.appBarLayout.addView(notice.getRoot());
        notice.card.setOnClickListener(v -> notice.note.setVisibility(notice.note.getVisibility() == View.GONE ? View.VISIBLE : View.GONE));
        new TabLayoutMediator(binding.tabs, binding.pager, (tab, position) -> tab.setText(routes.get(position))).attach();
        handler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                if (msg.what == -1) {
                    params.toast(R.string.no_wifi_warning);
                }else if(msg.what == 0){
                    params.toast(R.string.login_warning);
                    launch.launch(new Intent(SchoolBus.this, LoginActivity.class).putExtra("url","https://cas.sysu.edu.cn/cas/login?service=https://portal.sysu.edu.cn/newClient/shiro-cas"));
                }
                else{
                    JSONObject response = JSONObject.parseObject((String) msg.obj);
                    if (response != null && response.getJSONObject("meta").getInteger("statusCode").equals(200)) {
                        if (response.get("data") != null) {
                            if (msg.what == 1) {
                                response.getJSONObject("data").getJSONArray("workDay").forEach(a -> {
                                    StaggeredFragment fr = new StaggeredFragment();
                                    routes.add(((JSONObject)a).getString("drivingDirectionName"));
                                    ArrayList<String> infos = new ArrayList<>();
                                    for (String i:new String[]{"drivingDirectionName","startStation","endStation"}){
                                        infos.add(((JSONObject)a).getString(i));
                                    }
                                    //fr.setNote(((JSONObject)a).getString("note"));
                                    notice.note.setText(((JSONObject)a).getString("note"));
                                    fr.add(SchoolBus.this, "路线详情", List.of("路线","起点","终点"),infos);
                                    adp.add(fr);((JSONObject)a).getJSONArray("schoolBusShuttleMomentList").forEach(b->{
                                        ArrayList<String> values = new ArrayList<>();
                                        for (String i:new String[]{"passenger","vehiclesType","time","drivingRoute"}){
                                            values.add(((JSONObject)b).getString(i));
                                        }
                                        fr.add(SchoolBus.this, values.get(2),R.drawable.round_rect, List.of("乘客","车辆","时间","路线"),values);
                                    });
                                });
                            }
                        }
                    }
                    else {
                        params.toast(getString(R.string.login_warning));
                        launch.launch(new Intent(SchoolBus.this, LoginActivity.class));
                    }
                }
            }
        };
        getData();
    }

    void getData(){
        http.newCall(new Request.Builder().url("https://portal.sysu.edu.cn/newClient/api/extraCard/schoolBusShuttleInfo/selectSchoolBusMap")
                //.header("Cookie","_webvpn_key=eyJhbGciOiJIUzI1NiJ9.eyJ1c2VyIjoidGFuZ3hiNiIsImdyb3VwcyI6WzNdLCJpYXQiOjE3NTYyMTI2MDQsImV4cCI6MTc1NjI5OTAwNH0.G11ylaLHOLdtAaTp_LfR4iu24qAvPxr7hwrtEhgLKP0; webvpn_username=tangxb6%7C1756212604%7C1b647b3c1e605483a66a7cd98ea79994c09c772c; sid=edf90703-05f4-49d7-bdb2-81a8f769117c")
                //.header("User-Agent","SYSU")
                .header("Cookie",cookie)
                .build()).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Message msg = new Message();
                msg.what=-1;
                handler.sendMessage(msg);
            }
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                Message msg = new Message();
                msg.what = Objects.requireNonNull(response.header("Content-Type")).startsWith("application/json")?1:0;
                msg.obj=response.body().string();
                handler.sendMessage(msg);
            }
        });
    }
}