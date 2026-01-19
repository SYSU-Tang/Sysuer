package com.sysu.edu.academic;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.sysu.edu.R;
import com.sysu.edu.api.Params;
import com.sysu.edu.api.TargetUrl;
import com.sysu.edu.databinding.ActivityLeaveReturnRegistrationBinding;

import java.io.IOException;
import java.util.ArrayList;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class LeaveReturnRegistrationActivity extends AppCompatActivity {

    Handler handler;
    final OkHttpClient http = new OkHttpClient();
    Params params;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityLeaveReturnRegistrationBinding binding = ActivityLeaveReturnRegistrationBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.toolbar.setNavigationOnClickListener(v -> supportFinishAfterTransition());
        params = new Params(this);
        params.setCallback(this::getYears);
        LeaveReturnRegistrationViewModel viewModel = new ViewModelProvider(this).get(LeaveReturnRegistrationViewModel.class);
        handler = new Handler(getMainLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                if (msg.what == -1) {
                    params.toast(R.string.no_wifi_warning);
                } else if (msg.what == 0) {
                    int code = msg.getData().getInt("code");
                    //System.out.println(msg.getData().getString("response"));
                    if (code == 200) {
                        //System.out.println(msg.getData().getString("response"));
                        if (msg.getData().getBoolean("isJSON")) {
                            JSONObject json = JSONObject.parse(msg.getData().getString("response"));
                            JSONArray data;
                            if (json != null && json.getInteger("code") == 200 && (data = json.getJSONArray("data")) != null && !data.isEmpty()) {
                                ArrayList<String> years = new ArrayList<>();
                                data.forEach(o -> years.add(((JSONObject) o).getString("label") == null ? "" : ((JSONObject) o).getString("label")));
                                binding.years.setSimpleItems(years.toArray(new String[0]));
                                binding.years.setOnItemClickListener((parent, view, position, id) -> viewModel.year.setValue(data.getJSONObject(position).getString("value")));
                                binding.years.setText(years.isEmpty() ? "" : years.get(0), false);
                                viewModel.year.setValue(data.getJSONObject(0).getString("value"));
                            } else {
                                params.toast(msg.getData().getString("message"));
                            }
                        } else {
                            params.gotoLogin(binding.toolbar, TargetUrl.XGXT);
                        }
                    } else if (code == 302) {
                        params.gotoLogin(binding.toolbar, TargetUrl.XGXT);
                    } else {
                        params.toast(R.string.educational_wifi_warning);
                    }
                }
            }
        };
        getYears();
    }

    void getYears() {
        sendRequest("https://xgxt-443.webvpn.sysu.edu.cn/jjrlfx/api/sm-jjrlfx/student/school-year", 0);
    }

    void sendRequest(String url, int what) {
        http.newCall(new Request.Builder().url(url)
                .header("Cookie", params.getCookie())
                .build()).enqueue(new Callback() {

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                Message msg = new Message();
                msg.what = what;
                Bundle bundle = new Bundle();
                bundle.putInt("code", response.code());
                bundle.putString("response", response.body().string());
                bundle.putBoolean("isJSON", response.headers("Content-Type").contains("application/json"));
                msg.setData(bundle);
                handler.sendMessage(msg);
            }

            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Message msg = new Message();
                msg.what = -1;
                handler.sendMessage(msg);
            }
        });
    }
}