package com.sysu.edu.life;

import static com.sysu.edu.api.CommonUtil.extractValue;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ConcatAdapter;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.sysu.edu.R;
import com.sysu.edu.api.AuthorizationJar;
import com.sysu.edu.api.ContextUtil;
import com.sysu.edu.api.HttpManager;
import com.sysu.edu.api.Params;
import com.sysu.edu.api.TargetUrl;
import com.sysu.edu.databinding.RecyclerViewScrollBinding;
import com.sysu.edu.todo.TitleAdapter;
import com.sysu.edu.view.PreferenceAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class GymAccountFragment extends Fragment {
    
    HttpManager http;
    GymReservationViewModel viewModel;
    RecyclerViewScrollBinding binding;
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = RecyclerViewScrollBinding.inflate(inflater, container, false);
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        viewModel = new ViewModelProvider(requireActivity()).get(GymReservationViewModel.class);
        ConcatAdapter concatAdapter = new ConcatAdapter(new ConcatAdapter.Config.Builder().setIsolateViewTypes(true).build());
        binding.recyclerView.setAdapter(concatAdapter);
        Params params = new Params(this);
        params.setCallback(this::getAccount);
        ContextUtil contextUtils = new ContextUtil(requireContext());
        binding.recyclerView.setBackgroundColor(contextUtils.getColorFromAttr(com.google.android.material.R.attr.colorSurfaceContainer));
        http = new HttpManager(new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                super.handleMessage(msg);
                if (msg.what == -1) {
                    params.toast(R.string.no_net_connected);
                    // 处理错误
                } else {
                    String response = (String) msg.obj;
                    if (msg.getData().getBoolean("isJSON")) {
                        switch (msg.what) {
                            case 0 -> {
                                JSONObject json = JSONObject.parseObject(response);
                                PreferenceAdapter preferenceAdapter = new PreferenceAdapter();
                                preferenceAdapter.set(List.of(R.string.type, R.string.name, R.string.student_id, R.string.net_id), extractValue(json, new String[]{"Type", "Name", "HostKey", "UserId"}), List.of(R.drawable.help, R.drawable.text, R.drawable.school, R.drawable.id), requireContext());
                                concatAdapter.addAdapter(new TitleAdapter(getString(R.string.account)));
                                concatAdapter.addAdapter(preferenceAdapter);
                                
                                PreferenceAdapter cashAdapter = new PreferenceAdapter();
                                cashAdapter.set(List.of(R.string.sport_credit, R.string.wallet), extractValue(json, new String[]{"Credits", "CashWallet"}), List.of(R.drawable.dashboard, R.drawable.money), requireContext());
                                concatAdapter.addAdapter(new TitleAdapter(getString(R.string.wallet)));
                                concatAdapter.addAdapter(cashAdapter);
                                
                                String[] id_keys = {"validSwimmer", "IsAdmin"};
                                PreferenceAdapter idAdapter = new PreferenceAdapter();
                                for (int i = 0; i < id_keys.length; i++)
                                    idAdapter.add(getString(List.of(R.string.is_swimmer_valid, R.string.admin).get(i)), json.getBoolean(id_keys[i]) ? getString(R.string.yes) : getString(R.string.no), R.drawable.help);
                                concatAdapter.addAdapter(new TitleAdapter(getString(R.string.other)));
                                concatAdapter.addAdapter(idAdapter);
                                
                                getSwimmer();
                            }
                            case 1 -> JSONArray.parseArray(response).forEach(i -> {
                                JSONObject item = (JSONObject) i;
                                PreferenceAdapter certAdapter = new PreferenceAdapter();
                                ArrayList<String> list = extractValue(item, new String[]{"Status", "ValidUntil", "PhysicalExamDate"});
                                list.set(0, "approved".equals(list.get(0)) ? getString(R.string.approved) : getString(R.string.disapproved));
                                certAdapter.set(List.of(R.string.status, R.string.valid_date, R.string.physical_exam_date), list, List.of("approved".equals(list.get(0)) ? R.drawable.uncheck : R.drawable.check, R.drawable.calendar, R.drawable.calendar), requireContext());
                                concatAdapter.addAdapter(new TitleAdapter(getString(R.string.health_proof)));
                                concatAdapter.addAdapter(certAdapter);
                            });
                        }
                    } else {
                        if (!viewModel.authorizationManager.isAuthorized(response)) {
                            params.gotoLogin(viewModel.authorizationManager.isAccessible() ? TargetUrl.GYM : TargetUrl.GYM_WEBVPN);
                        } else if (Pattern.compile("人机识别检测").matcher(response).find()) {
                            params.gotoLogin(viewModel.authorizationManager.isAccessible() ? TargetUrl.GYM : TargetUrl.GYM_WEBVPN);
                        } else if (!viewModel.authorizationManager.isAccessible(response)) {
                            getAccount();
                        }
                    }
                }
            }
        });
        http.setParams(params);
        http.setHeader(Map.of("Accept", "application/json, text/plain, */*"));
//        http.setCookie(viewModel.cookie);
        http.setUA(viewModel.ua);
        http.setAuthorizationRequired(true);
        http.setAuthorizationJar(new AuthorizationJar(requireContext()));
        
        getAccount();
        return binding.getRoot();
    }
    
    void getAccount() {
        http.getRequest(viewModel.authorizationManager.getHost() + "api/Credit/Me", 0);
    }
    
    void getSwimmer() {
        http.getRequest(viewModel.authorizationManager.getHost() + "api/swimmer/me", 1);
    }
    
}