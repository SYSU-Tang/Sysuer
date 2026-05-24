package com.sysu.edu.extra;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.alibaba.fastjson2.JSONObject;
import com.sysu.edu.R;
import com.sysu.edu.api.AuthorizationJar;
import com.sysu.edu.api.HttpManager;
import com.sysu.edu.api.Params;
import com.sysu.edu.api.TargetUrl;

import java.util.Objects;

import io.reactivex.rxjava3.disposables.CompositeDisposable;

public class PrivacyFragment extends PreferenceFragmentCompat {
    
    HttpManager http;
    
    CompositeDisposable disposable = new CompositeDisposable();
    
    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
        if (savedInstanceState == null) {
            setPreferencesFromResource(R.xml.privacy, rootKey);
            Params params = new Params(this);
            disposable.add(params.getContextUtil().getAccountManager().getActiveAccountAsync("sysu.edu.cn").subscribe(
                    activeAccount -> {
                        ((Preference) Objects.requireNonNull(findPreference("netId"))).setSummary(activeAccount.first);
                        ((Preference) Objects.requireNonNull(findPreference("password"))).setOnPreferenceClickListener(_ -> {
                            params.toast(activeAccount.second);
                            return false;
                        });
                    }));
            params.setCallback(this::getInfo);
            http = new HttpManager(new Handler(Looper.getMainLooper()) {
                @Override
                public void handleMessage(@NonNull Message msg) {
                    if (msg.what == -1) params.toast(R.string.no_net_connected);
                    else {
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
                                            params.toast(R.string.copy_successfully);
                                            return false;
                                        });
                                        getPreferenceScreen().addPreference(p);
                                    }
                                }
                            }
                        } else if (response != null && response.getInteger("code").equals(1003))
                            params.gotoLogin(TargetUrl.PAY);
                        else if (response != null) params.toast(response.getString("message"));
                    }
                }
            });
            http.setParams(params);
            http.setAuthorizationJar(new AuthorizationJar(requireContext()));
            http.setTokenRequired(true);
            http.setReferrer("https://pay.sysu.edu.cn/");
            getInfo();
        }
    }
    
    void getInfo() {
        http.postRequest("https://pay.sysu.edu.cn/client/api/client/person/get", "{}", 0);
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        disposable.clear();
    }
}