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
import com.sysu.edu.api.AuthorizationManager;
import com.sysu.edu.api.HttpManager;
import com.sysu.edu.api.Params;
import com.sysu.edu.api.TargetUrl;
import com.sysu.edu.databinding.ActivityLeaveReturnRegistrationBinding;

import java.util.ArrayList;

@SuppressWarnings("ALL")
public class LeaveReturnRegistrationActivity extends AppCompatActivity {

    HttpManager http;
    AuthorizationManager authorizationManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityLeaveReturnRegistrationBinding binding = ActivityLeaveReturnRegistrationBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.toolbar.setNavigationOnClickListener(_ -> supportFinishAfterTransition());
        Params params = new Params(this);
        params.setCallback(this::getYears);
        LeaveReturnRegistrationViewModel viewModel = new ViewModelProvider(this).get(LeaveReturnRegistrationViewModel.class);
        authorizationManager = new AuthorizationManager("https://xgxt.sysu.edu.cn/", "https://xgxt-443.webvpn.sysu.edu.cn/");
        http = new HttpManager(new Handler(getMainLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                if (msg.what == -1) {
                    params.toast(R.string.no_net_connected);
                } else if (msg.what == 0) {
                    int code = msg.getData().getInt("code");
                    if (code == 200) {
                        String response = (String) msg.obj;
                        if (msg.getData().getBoolean("isJSON")) {
                            JSONObject json = JSONObject.parse(response);
                            JSONArray data;
                            if (json != null && json.getInteger("code") == 200 && (data = json.getJSONArray("data")) != null && !data.isEmpty()) {
                                ArrayList<String> years = new ArrayList<>();
                                data.forEach(o -> years.add(((JSONObject) o).getString("label") == null ? "" : ((JSONObject) o).getString("label")));
                                binding.years.setSimpleItems(years.toArray(new String[0]));
                                binding.years.setOnItemClickListener((_, _, position, _) -> viewModel.year.setValue(data.getJSONObject(position).getString("value")));
                                //noinspection SequencedCollectionMethodCanBeUsed
                                binding.years.setText(years.isEmpty() ? "" : years.get(0), false);
                                viewModel.year.setValue(data.getJSONObject(0).getString("value"));
                            } /*else {
                                params.toast(data.getString("message"));
                            }*/
                        } else {
                            if (!authorizationManager.isAuthorized(response)) {
                                params.toast(R.string.login_warning);
                                params.gotoLogin(authorizationManager.isAccessible() ? TargetUrl.XGXT : TargetUrl.XGXT_WEBVPN);
                            } else if (!authorizationManager.isAccessible(response)) {
                                params.toast(R.string.educational_wifi_warning);
                                getYears();
                            }
                        }
                    } else if (code == 302) {
                        params.gotoLogin(authorizationManager.isAccessible() ? TargetUrl.XGXT : TargetUrl.XGXT_WEBVPN);
                    } else {
                        params.toast(R.string.educational_wifi_warning);
                    }
                }
            }
        });
        http.setParams(params);
        getYears();
    }

    void getYears() {
        http.getRequest(authorizationManager.getBaseUrl() + "jjrlfx/api/sm-jjrlfx/student/school-year", 0);
    }
}