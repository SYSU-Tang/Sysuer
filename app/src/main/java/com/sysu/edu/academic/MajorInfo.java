package com.sysu.edu.academic;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.alibaba.fastjson2.JSONObject;
import com.google.android.material.tabs.TabLayoutMediator;
import com.sysu.edu.R;
import com.sysu.edu.api.Params;
import com.sysu.edu.api.TargetUrl;
import com.sysu.edu.databinding.ActivityPagerBinding;

import java.io.IOException;
import java.util.ArrayList;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MajorInfo extends AppCompatActivity {

    ActivityPagerBinding binding;
    String cookie;
    Handler handler;
    OkHttpClient http = new OkHttpClient.Builder().build();
    ArrayList<String> categories;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPagerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.toolbar.setTitle(R.string.major_info);
        binding.toolbar.setNavigationOnClickListener(v -> supportFinishAfterTransition());
        Params params = new Params(this);
        params.setCallback(o -> {
            if (o.getResultCode() == Activity.RESULT_OK) {
                cookie = params.getCookie();
                getCategory();
            }
        });
        cookie = params.getCookie();
        Pager2Adapter adp = new Pager2Adapter(this);
        binding.pager.setAdapter(adp);
        new TabLayoutMediator(binding.tabs, binding.pager, (tab, position) -> tab.setText(categories.get(position))).attach();
        handler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                if (msg.what == -1) {
                    Toast.makeText(MajorInfo.this, getString(R.string.no_wifi_warning), Toast.LENGTH_LONG).show();
                } else {
                    JSONObject response = JSONObject.parseObject((String) msg.obj);
                    if (response != null && response.getInteger("code").equals(200)) {
                        if (response.get("data") != null) {
                            switch (msg.what) {
                                case 0:
                                    categories = new ArrayList<>();
                                    response.getJSONArray("data").forEach(a -> {
                                        categories.add(((JSONObject) a).getString("dataName"));
                                        Bundle args = new Bundle();
                                        args.putString("code", ((JSONObject) a).getString("dataNumber"));
                                        adp.add(MajorInfoFragment.newInstance(args));
                                    });
                                    break;
                                case 1:

                                    break;
                            }
                        }
                    } else {
                        params.toast(R.string.login_warning);
                        params.gotoLogin(binding.getRoot(), TargetUrl.JWXT);
                    }
                }
            }
        };
        getCategory();
    }

    void getCategory() {
        http.newCall(new Request.Builder().url("https://jwxt.sysu.edu.cn/jwxt/base-info/codedata/findcodedataNames?datableNumber=135")
                .header("Cookie", cookie)
                .header("Referer", "https://jwxt.sysu.edu.cn/jwxt/mk/studentWeb/")
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
                handler.sendMessage(msg);
            }
        });
    }
}