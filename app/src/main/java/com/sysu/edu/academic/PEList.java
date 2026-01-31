package com.sysu.edu.academic;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.tabs.TabLayoutMediator;
import com.sysu.edu.R;
import com.sysu.edu.api.Params;
import com.sysu.edu.databinding.ActivityPagerBinding;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class PEList extends AppCompatActivity {

    final OkHttpClient http = new OkHttpClient.Builder().build();
    ActivityPagerBinding binding;
    Params params;
    String cookie;
    Handler handler;
    Pager2Adapter adp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPagerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        params = new Params(this);
        cookie = params.getCookie();
        params.setCallback(() -> cookie = params.getCookie());
        adp = new Pager2Adapter(this);
        adp.add(StaggeredFragment.newInstance(0));
        adp.add(StaggeredFragment.newInstance(0));
        adp.add(StaggeredFragment.newInstance(0));
        binding.pager.setAdapter(adp);
        new TabLayoutMediator(binding.tabs, binding.pager, (tab, position) -> tab.setText(new String[]{"体测查询", "课外积分", "游泳"}[position])).attach();
        binding.toolbar.setNavigationOnClickListener(v -> supportFinishAfterTransition());
        handler = new Handler(getMainLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                if (msg.what == -1) {
                    params.toast(R.string.no_wifi_warning);
                }/*else {
//                    JSONObject response = JSONObject.parseObject((String) msg.obj);
//                    if (response != null && response.getInteger("code").equals(200)) {
//                        if (response.get("data") != null) {
//                            if (msg.what == 0) {
//
////                                JSONObject d = response.getJSONObject("data");
////                                ArrayList<String> values = new ArrayList<>();
////                                String[] keyName = new String[]{"学号", "注册学年学期", "报到状态", "注册状态", "缴费状态"};
////                                for (int i = 0; i < keyName.length; i++) {
////                                    values.add(d.getString(new String[]{"stuNum", "academicYearTerm", "checkInStatusName", "registerStatusName", "payedStatusName"}[i]));
////                                }
////                                ((StaggeredFragment) adp.getItem(0)).add(PEList.this, "学生报到信息", List.of(keyName), values);
////
//                            }
//                        }
//                    }
//                    else {
//                        Toast.makeText(PEList.this, getString(R.string.login_warning), Toast.LENGTH_LONG).show();
//                        launch.launch(new Intent(PEList.this, LoginActivity.class));
//                    }
                }*/
            }
        };
        getTest();
    }

    void getTest() {
        http.newCall(new Request.Builder().url("https://tice.sysu.edu.cn/m/tice")
                .header("Cookie", cookie)
                .build()).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Message msg = new Message();
                msg.what = -1;
                handler.sendMessage(msg);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                Message msg = new Message();
                msg.what = 0;
                msg.obj = response.body().string();
                System.out.println(msg.obj);
                handler.sendMessage(msg);
            }
        });
    }
}