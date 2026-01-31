package com.sysu.edu.academic;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.alibaba.fastjson2.JSONObject;
import com.sysu.edu.R;
import com.sysu.edu.api.Params;
import com.sysu.edu.api.TargetUrl;
import com.sysu.edu.databinding.ActivityListBinding;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class SchoolWorkWarning extends AppCompatActivity {
    final OkHttpClient http = new OkHttpClient.Builder().build();
    ActivityListBinding binding;
    Params params;
    String cookie;
    Handler handler;
    int order = 0;
    String alarmOperationTerm;
    String alarmTerm;
    int page;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        page = 0;
        binding = ActivityListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        params = new Params(this);
        cookie = params.getCookie();
        StaggeredFragment fr = binding.list.getFragment();
        binding.toolbar.setTitle(R.string.school_work_warning);
        binding.toolbar.setNavigationOnClickListener(v -> supportFinishAfterTransition());
        handler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                if (msg.what == -1) {
                    Toast.makeText(SchoolWorkWarning.this, getString(R.string.no_wifi_warning), Toast.LENGTH_LONG).show();
                } else {
                    JSONObject response = JSONObject.parseObject((String) msg.obj);
                    if (response != null && response.getInteger("code").equals(200)) {
                        JSONObject d = response.getJSONObject("data");
                        if (d != null) {
                            int total = d.getInteger("total");
                            d.getJSONArray("rows").forEach(a -> {
                                order++;
                                ArrayList<String> values = new ArrayList<>();
                                String[] keyName = new String[]{"预警结果", "预警操作学期", "预警学期", "生成预警档案时间", "档案ID", "警告程度"};
                                for (int i = 0; i < keyName.length; i++) {
                                    values.add(((JSONObject) a).getString(new String[]{"alarmResultName", "alarmOperationTerm", "alarmTerm", "createTime", "archivceID", "alarmResult"}[i]));
                                }
                                fr.add(SchoolWorkWarning.this, String.valueOf(order), List.of(keyName), values);
                            });
                            if (total / 10 > page - 1) {
                                getWarning();
                            }
                        }
                    } else {
                        params.toast(R.string.login_warning);
                        params.gotoLogin(binding.toolbar, TargetUrl.JWXT);
                    }
                }
            }
        };
        getWarning();
    }

    void getWarning() {
        page++;
        http.newCall(new Request.Builder().url("https://jwxt.sysu.edu.cn/jwxt/alarm/alarm-archives/student/archives")
                .header("Cookie", cookie)
                .post(RequestBody.create(String.format(Locale.getDefault(), "{\"pageNo\":%d,\"pageSize\":10,\"total\":true,\"param\":{\"publicationStatus\":\"1\"%s%s}}", page, getTerm(alarmTerm), getTerm(alarmOperationTerm)), MediaType.parse("application/json")))
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

    String getTerm(String s) {
        if (s == null || s.isEmpty()) {
            return "";
        } else {
            return String.format(",\"alarmOperationTerm\":\"%s\"", s);
        }
    }
}