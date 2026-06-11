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
import androidx.recyclerview.widget.RecyclerView;

import com.alibaba.fastjson2.JSONObject;
import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.CompositeDateValidator;
import com.google.android.material.datepicker.DateValidatorPointBackward;
import com.google.android.material.datepicker.DateValidatorPointForward;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.sysu.edu.R;
import com.sysu.edu.api.AuthorizationJar;
import com.sysu.edu.api.HttpManager;
import com.sysu.edu.api.Params;
import com.sysu.edu.api.TargetUrl;
import com.sysu.edu.databinding.FragmentGymOrderBinding;
import com.sysu.edu.todo.TitleAdapter;
import com.sysu.edu.view.PreferenceAdapter;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

public class GymOrderFragment extends Fragment {
    
    final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    HttpManager http;
    GymReservationViewModel viewModel;
    private int total = -1;
    private int page = 0;
    private ConcatAdapter concatAdapter;
    private FragmentGymOrderBinding binding;
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentGymOrderBinding.inflate(inflater, container, false);
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        viewModel = new ViewModelProvider(requireActivity()).get(GymReservationViewModel.class);
        concatAdapter = new ConcatAdapter(new ConcatAdapter.Config.Builder().setIsolateViewTypes(true).build());
        binding.recyclerView.setAdapter(concatAdapter);
        binding.recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                if (dy > 0 && total > 0 && page * 10 < total)
                    getOrder();
            }
        });
        Params params = new Params(this);
        params.setCallback(this::reset);
        http = new HttpManager(new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                super.handleMessage(msg);
                // 处理错误
                if (msg.what == -1) params.toast(R.string.no_net_connected);
                else {
                    String response = (String) msg.obj;
                    if (msg.getData().getBoolean("isJSON")) {
                        JSONObject json = JSONObject.parseObject(response);
                        if (msg.what == 0) {
                            json.getJSONArray("Transactions").forEach((i) -> {
                                JSONObject item = (JSONObject) i;
                                PreferenceAdapter preferenceAdapter = new PreferenceAdapter();
                                TitleAdapter titleAdapter = new TitleAdapter(item.getString("Description"));
                                titleAdapter.setHeader(1);
                                concatAdapter.addAdapter(titleAdapter);
                                concatAdapter.addAdapter(preferenceAdapter);
                                preferenceAdapter.set(List.of(getString(R.string.date), getString(R.string.type), getString(R.string.money), getString(R.string.balance)),
                                        extractValue(item, new String[]{"Date", "TransactionType", "Amount", "Balance"}),
                                        List.of(R.drawable.calendar, R.drawable.text, R.drawable.money, R.drawable.money));
                            });
                            total = json.getInteger("TotalCount");
                        }
                    } else {
                        if (!viewModel.authorizationManager.isAuthorized(response)) {
                            params.toast(R.string.login_warning);
//                            viewModel.loginRequired.setValue(true);
                            params.gotoLogin(viewModel.authorizationManager.isAccessible() ? TargetUrl.GYM : TargetUrl.GYM_WEBVPN);
                        } else if (Pattern.compile("人机识别检测").matcher(response).find()) {
                            params.gotoLogin(viewModel.authorizationManager.isAccessible() ? TargetUrl.GYM : TargetUrl.GYM_WEBVPN);
                        } else if (!viewModel.authorizationManager.isAccessible(response)) {
                            getOrder();
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
        MaterialDatePicker.Builder<Long> picker = MaterialDatePicker.Builder.datePicker();
        binding.from.setOnClickListener(_ -> {
            MaterialDatePicker<Long> datePicker = picker
                    .setSelection(viewModel.from)
                    .setCalendarConstraints(new CalendarConstraints.Builder().setValidator(CompositeDateValidator.allOf(List.of(DateValidatorPointBackward.before(viewModel.to)))).build())
                    .build();
            datePicker.show(getParentFragmentManager(), "datePicker");
            datePicker.addOnPositiveButtonClickListener(selection -> {
                viewModel.from = selection;
                binding.from.setText(datePicker.getHeaderText());
                regetOrder();
            });
        });
        binding.from.setText(dateFormat.format(viewModel.from));
        binding.to.setText(dateFormat.format(viewModel.to));
        binding.to.setOnClickListener(_ -> {
            MaterialDatePicker<Long> datePicker = picker
                    .setSelection(viewModel.to)
                    .setCalendarConstraints(new CalendarConstraints.Builder().setValidator(CompositeDateValidator.allOf(List.of(DateValidatorPointForward.from(viewModel.from)))).build())
                    .build();
            datePicker.show(getParentFragmentManager(), "datePicker");
            datePicker.addOnPositiveButtonClickListener(selection -> {
                viewModel.to = selection;
                binding.to.setText(datePicker.getHeaderText());
                regetOrder();
            });
        });
        getOrder();
        return binding.getRoot();
    }
    
    private void regetOrder() {
        reset();
        getOrder();
    }
    
    void reset() {
        total = -1;
        page = 0;
        concatAdapter.getAdapters().forEach(adapter -> concatAdapter.removeAdapter(adapter));
    }
    
    void getOrder() {
        http.getRequest(viewModel.authorizationManager.getHost() + String.format("api/transaction/Me?StartDate=%s&EndDate=%s&Page=%s&PageSize=10",
                dateFormat.format(viewModel.from), dateFormat.format(viewModel.to), ++page), 0);
    }
}
