package com.sysu.edu.extra.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.alibaba.fastjson2.JSONObject;
import com.sysu.edu.R;
import com.sysu.edu.api.Params;
import com.sysu.edu.api.TargetUrl;
import com.sysu.edu.login.LoginActivity;

import java.io.IOException;
import java.util.Objects;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class PrivacyFragment extends PreferenceFragmentCompat {
    Params params;
    Handler handler;
    OkHttpClient http = new OkHttpClient.Builder().build();
    String token;

    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
        if (savedInstanceState == null) {
            setPreferencesFromResource(R.xml.privacy, rootKey);
            ((Preference) Objects.requireNonNull(findPreference("netId"))).setSummary(requireContext().getSharedPreferences("privacy", Context.MODE_PRIVATE).getString("username", ""));
            ((Preference) Objects.requireNonNull(findPreference("password")))
                    .setOnPreferenceClickListener(preference -> {
                        Toast.makeText(requireContext(), requireContext().getSharedPreferences("privacy", Context.MODE_PRIVATE).getString("password", ""), Toast.LENGTH_LONG).show();
                        return false;
                    });
            params = new Params(requireActivity());
            token = params.getToken();
            ActivityResultLauncher<Intent> launchLogin = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), o -> {
                if (o.getResultCode() == Activity.RESULT_OK) {
                    token = params.getToken();
                    getInfo();
                }
            });
            handler = new Handler(Looper.getMainLooper()) {
                @Override
                public void handleMessage(@NonNull Message msg) {
                    if (msg.what == -1) {
                        params.toast(R.string.no_wifi_warning);
                    } else {
                        JSONObject response = JSONObject.parseObject((String) msg.obj);
                        if (response != null && response.getInteger("code").equals(200)) {
                            if (response.get("data") != null) {
                                if (msg.what == 0) {
                                    JSONObject data = response.getJSONObject("data");
                                    String[] keyName = new String[]{"姓名", "学号", "证件类别", "证件号码", "电话", "邮箱"};
                                    for (int i = 0; i < keyName.length; i++) {
                                        Preference p = new Preference(requireContext());
                                        p.setTitle(keyName[i]);
                                        p.setSummary(data.getString(new String[]{"userName", "userCode", "idTypeStr", "idNum", "tele", "email"}[i]));
                                        p.setIcon(new int[]{R.drawable.name, R.drawable.id, R.drawable.card, R.drawable.account, R.drawable.phone, R.drawable.email}[i]);
                                        p.setOnPreferenceClickListener(preference -> {
                                            params.copy((String) preference.getTitle(), (String) preference.getSummary());
                                            params.toast("已复制");
                                            return false;
                                        });
                                        getPreferenceScreen().addPreference(p);
                                    }
                                }
                            }
                        } else if (response != null && response.getInteger("code").equals(1003)) {
                            params.toast(R.string.login_warning);
                            launchLogin.launch(new Intent(requireContext(), LoginActivity.class).putExtra("url", TargetUrl.PAY));
                        } else {
                            if (response != null) {
                                params.toast(response.getString("message"));
                            }
                            //launchLogin.launch(new Intent(requireContext(), LoginActivity.class).putExtra("url", TargetUrl.PAY));
                        }
                    }
                }
            };
            getInfo();
        }
    }

    void getInfo() {
        http.newCall(new Request.Builder().url("https://pay.sysu.edu.cn/client/api/client/person/get")
                .header("Referer", "https://pay.sysu.edu.cn/")
                .post(RequestBody.create("{}", MediaType.parse("application/json")))
                .header("token", token)
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