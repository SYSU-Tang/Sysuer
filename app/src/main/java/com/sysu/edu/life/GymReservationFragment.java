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
import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.CompositeDateValidator;
import com.google.android.material.datepicker.DateValidatorPointBackward;
import com.google.android.material.datepicker.DateValidatorPointForward;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.sysu.edu.R;
import com.sysu.edu.api.AuthorizationJar;
import com.sysu.edu.api.CommonUtil;
import com.sysu.edu.api.HttpManager;
import com.sysu.edu.api.Params;
import com.sysu.edu.api.TargetUrl;
import com.sysu.edu.databinding.FragmentGymOrderBinding;
import com.sysu.edu.todo.TitleAdapter;
import com.sysu.edu.view.ButtonAdapter;
import com.sysu.edu.view.PreferenceAdapter;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

public class GymReservationFragment extends Fragment {
    final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    HttpManager http;
    GymReservationViewModel viewModel;
    Handler handler;
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
        Params params = new Params(this);
        params.setCallback(this::reset);
        handler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                if (msg.what == -1) params.toast(R.string.no_net_connected);
                else {
                    String response = (String) msg.obj;
                    if (msg.getData().getBoolean("isJSON")) {
                        switch (msg.what) {
                            case 0 -> JSONArray.parseArray(response).forEach((i) -> {
                                JSONObject item = (JSONObject) i;
                                PreferenceAdapter preferenceAdapter = new PreferenceAdapter();
                                TitleAdapter titleAdapter = new TitleAdapter(item.getString("Description"));
                                titleAdapter.setHeader(1);
                                ButtonAdapter buttonAdapter = new ButtonAdapter();
                                buttonAdapter.add(getString(R.string.cancel_reservation));
                                buttonAdapter.setListener((button, _) -> {
                                    button.setOnClickListener(_ -> deleteReservation(item.getString("Identity")));
                                    handler.sendEmptyMessage(1);
                                });
                                concatAdapter.addAdapter(titleAdapter);
                                concatAdapter.addAdapter(preferenceAdapter);
                                concatAdapter.addAdapter(buttonAdapter);
                                ArrayList<String> value = extractValue(item, new String[]{"VenueName", "StartDateTime", "EndDateTime", "Charge", "CreatedAt"});
//                                System.out.println(value.get(1).substring(0, 19));
                                try {
                                    DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");
                                    value.set(1, LocalDateTime.parse(value.get(1), FORMATTER).atZone(ZoneId.of("UTC")).withZoneSameInstant(ZoneId.systemDefault()).format(FORMATTER));
//                                    value.set(4, LocalDateTime.parse(value.get(4), FORMATTER).atZone(ZoneId.of("UTC")).withZoneSameInstant(ZoneId.systemDefault()).format(FORMATTER));
                                    value.set(2, LocalDateTime.parse(value.get(2), FORMATTER).atZone(ZoneId.of("UTC")).withZoneSameInstant(ZoneId.systemDefault()).format(FORMATTER));
                                } catch (DateTimeParseException e) {
                                    throw new IllegalArgumentException("Invalid Time, which is required to format as yyyy-MM-dd'T'HH:mm:ss'Z'", e);
                                }
                                preferenceAdapter.set(List.of(getString(R.string.venue), getString(R.string.start_time), getString(R.string.end_time), getString(R.string.money), getString(R.string.order_time)),
                                        value,
                                        List.of(R.drawable.location, R.drawable.time, R.drawable.alarm, R.drawable.money));
                                preferenceAdapter.add(getString(R.string.pay_way), item.getBoolean("IsCash") ? getString(R.string.cash) : getString(R.string.pe_credit), R.drawable.money);
                                
                            });
                            case 1 -> regetReservation();
                        }
                    } else {
                        if (!viewModel.authorizationManager.isAuthorized(response)) {
                            params.gotoLogin(viewModel.authorizationManager.isAccessible() ? TargetUrl.GYM : TargetUrl.GYM_WEBVPN);
                        } else if (Pattern.compile("人机识别检测").matcher(response).find())
                            params.gotoLogin(viewModel.authorizationManager.isAccessible() ? TargetUrl.GYM : TargetUrl.GYM_WEBVPN);
                        else if (!viewModel.authorizationManager.isAccessible(response)) {
                            regetReservation();
                        }
                    }
                }
            }
        };
        http = new HttpManager(handler);
        http.setParams(params);
        http.setHeader(Map.of("Accept", "application/json, text/plain, */*"));
        http.setUA(viewModel.ua);
        http.setAuthorizationRequired(true);
        http.setAuthorizationJar(new AuthorizationJar(requireContext()));
        MaterialDatePicker.Builder<Long> picker = MaterialDatePicker.Builder.datePicker();
        binding.from.setOnClickListener(_ -> {
            Long value;
            if (viewModel.reservationFromTo.getValue() != null && (value = viewModel.reservationFromTo.getValue().getSecond()) != null) {
                MaterialDatePicker<Long> datePicker = picker
                        .setSelection(viewModel.reservationFromTo.getValue().getFirst())
                        .setCalendarConstraints(new CalendarConstraints.Builder().setValidator(CompositeDateValidator.allOf(List.of(DateValidatorPointBackward.before(value)))).build())
                        .build();
                datePicker.show(getParentFragmentManager(), "datePicker");
                datePicker.addOnPositiveButtonClickListener(selection -> viewModel.reservationFromTo.setValue(new CommonUtil.Tuple2<>(selection, value)));
            }
        });
        viewModel.reservationFromTo.observe(getViewLifecycleOwner(), o -> {
            if (o != null && o.getSecond() != null && o.getFirst() != null) {
                binding.from.setText(dateFormat.format(o.getFirst()));
                binding.to.setText(dateFormat.format(o.getSecond()));
                regetReservation();
            }
        });
        binding.to.setOnClickListener(_ -> {
            Long value;
            if (viewModel.reservationFromTo.getValue() != null && (value = viewModel.reservationFromTo.getValue().getFirst()) != null) {
                MaterialDatePicker<Long> datePicker = picker
                        .setSelection(viewModel.reservationFromTo.getValue().getSecond())
                        .setCalendarConstraints(new CalendarConstraints.Builder().setValidator(CompositeDateValidator.allOf(List.of(DateValidatorPointForward.from(value)))).build())
                        .build();
                datePicker.show(getParentFragmentManager(), "datePicker");
                datePicker.addOnPositiveButtonClickListener(selection -> viewModel.reservationFromTo.setValue(new CommonUtil.Tuple2<>(value, selection)));
            }
        });
        return binding.getRoot();
    }
    
    private void regetReservation() {
        reset();
        getReservation();
    }
    
    void reset() {
        concatAdapter.getAdapters().forEach(adapter -> concatAdapter.removeAdapter(adapter));
    }
    
    void getReservation() {
        if (viewModel.reservationFromTo.getValue() != null && viewModel.reservationFromTo.getValue().getSecond() != null && viewModel.reservationFromTo.getValue().getFirst() != null)
            http.getRequest(viewModel.authorizationManager.getBaseUrl() + String.format("api/BookingRequestVenue?all=false&startDate=%s&endDate=%s&waitingList=false", dateFormat.format(viewModel.reservationFromTo.getValue().getFirst()), dateFormat.format(viewModel.reservationFromTo.getValue().getSecond())), 0);
    }
    
    void deleteReservation(String bookingId) {
        http.deleteRequest(viewModel.authorizationManager.getBaseUrl() + String.format("api/BookingRequestVenue/%s", bookingId), 1);
    }
    
}
