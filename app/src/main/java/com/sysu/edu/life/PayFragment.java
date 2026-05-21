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
import androidx.recyclerview.widget.RecyclerView;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.CompositeDateValidator;
import com.google.android.material.datepicker.DateValidatorPointBackward;
import com.google.android.material.datepicker.DateValidatorPointForward;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.sysu.edu.R;
import com.sysu.edu.api.AuthorizationJar;
import com.sysu.edu.api.CalendarManager;
import com.sysu.edu.api.HttpManager;
import com.sysu.edu.api.Params;
import com.sysu.edu.api.TargetUrl;
import com.sysu.edu.databinding.FragmentPayNeedBinding;
import com.sysu.edu.databinding.FragmentPayRecordBinding;
import com.sysu.edu.databinding.FragmentPaySituationBinding;
import com.sysu.edu.databinding.ItemFilterChipBinding;
import com.sysu.edu.view.StaggeredFragment;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class PayFragment extends StaggeredFragment {

    public View view;
    int order = 0;
    HttpManager http;
    CalendarManager calendarManager;

    public static PayFragment newInstance(int position) {
        PayFragment payFragment = new PayFragment();
        payFragment.position = position;
        return payFragment;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        Params params = new Params(this);
        calendarManager = new CalendarManager();
        params.setCallback(this::getPage);
        switch (position) {
            case 0:
                FragmentPayNeedBinding b0 = FragmentPayNeedBinding.inflate(inflater);
                b0.getRoot().addView(view);
                b0.pay.setOnClickListener(params.browse("https://pay.sysu.edu.cn/#/confirm/pay-ticket?type=1"));
                binding.recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
                    @Override
                    public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                        b0.chips.setElevation(recyclerView.canScrollVertically(-1) ? 6 : 0);
                        super.onScrolled(recyclerView, dx, dy);
                    }
                });
                this.view = b0.getRoot();
                view = b0.getRoot();
                break;
            case 2:
                ArrayList<String> years = new ArrayList<>(List.of(getString(R.string.all), getString(R.string.no_interval_year)));
                ArrayList<String> yearCodes = new ArrayList<>(List.of("null", "-1"));
                for (int i = 0; i < 6; i++) {
                    String year = String.valueOf(calendarManager.getYear() + 1 - i);
                    years.add(year);
                    yearCodes.add(year);
                }
                FragmentPaySituationBinding fragmentPaySituationBinding = FragmentPaySituationBinding.inflate(getLayoutInflater());
                fragmentPaySituationBinding.getRoot().addView(view);
                binding.recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
                    @Override
                    public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                        fragmentPaySituationBinding.p.setElevation(recyclerView.canScrollVertically(-1) ? 6 : 0);
                        super.onScrolled(recyclerView, dx, dy);
                    }
                });
                fragmentPaySituationBinding.spinner.setText(String.valueOf(calendarManager.getYear()));
                fragmentPaySituationBinding.spinner.setSimpleItems(years.toArray(new String[]{}));
                fragmentPaySituationBinding.spinner.setOnItemClickListener((_, _, i, _) -> {
                    clear();
                    getFeeList(String.valueOf(yearCodes.get(i)));
                });
                view = fragmentPaySituationBinding.getRoot();
                break;
            case 3:
                DateManager dm = new DateManager();
                dm.fromDate = calendarManager.getFirstOfMonth();
                dm.toDate = calendarManager.getEndOfMonth();
                FragmentPayRecordBinding fragmentPayRecordBinding = FragmentPayRecordBinding.inflate(inflater);
                fragmentPayRecordBinding.getRoot().addView(view);
                view = fragmentPayRecordBinding.getRoot();
                fragmentPayRecordBinding.from.setText(dm.getFromDateString());
                fragmentPayRecordBinding.from.setOnClickListener(_ -> {
                    MaterialDatePicker<Long> fromDatePicker = MaterialDatePicker.Builder.datePicker().setSelection(dm.getFromDateTimeMillis()).setCalendarConstraints(new CalendarConstraints.Builder()
                            .setValidator(CompositeDateValidator.allOf(List.of(DateValidatorPointBackward.before(dm.getToDateTimeMillis())))).build()).build();
                    fromDatePicker.addOnPositiveButtonClickListener(selection -> {
                        dm.setFromDateTimeMillis(selection);
                        fromDatePicker.dismissAllowingStateLoss();
                        fragmentPayRecordBinding.from.setText(dm.getFromDateString());
                        dm.getData();
                    });
                    fromDatePicker.show(requireActivity().getSupportFragmentManager(), null);
                });
                fragmentPayRecordBinding.to.setText(dm.getToDateString());
                fragmentPayRecordBinding.to.setOnClickListener(_ -> {
                    MaterialDatePicker<Long> toDatePicker = MaterialDatePicker.Builder.datePicker().setSelection(dm.getToDateTimeMillis()).setCalendarConstraints(new CalendarConstraints.Builder().setValidator(CompositeDateValidator.allOf(List.of(DateValidatorPointForward.from(dm.getFromDateTimeMillis())))).build()).build();
                    toDatePicker.addOnPositiveButtonClickListener(selection -> {
                        dm.setToDateTimeMillis(selection);
                        toDatePicker.dismissAllowingStateLoss();
                        fragmentPayRecordBinding.to.setText(dm.getToDateString());
                        dm.getData();
                    });
                    toDatePicker.show(requireActivity().getSupportFragmentManager(), null);
                });
                binding.recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
                    @Override
                    public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                        fragmentPayRecordBinding.row.setElevation(recyclerView.canScrollVertically(-1) ? params.dpToPx(2) : 0);
                    }
                });
                break;
        }
        http = new HttpManager(new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                if (msg.what == -1) params.toast(getString(R.string.no_net_connected));
                else {
                    JSONObject response = JSONObject.parseObject((String) msg.obj);
                    if (response != null && response.getInteger("code").equals(200)) {
                        if (response.get("data") != null) {
                            clear();
                            JSONArray data = response.getJSONArray("data");
                            switch (msg.what) {
                                case 0, 1 ->
                                        data.forEach(a -> add(((JSONObject) a).getString("itemName"), List.of("学号", "交费区间", "当前应交", "本次交费"),
                                                extractValue((JSONObject) a, new String[]{"personCode", "intervalName", "nowMoney", "needMoney"})));

                                case 2 ->
                                        data.forEach(a -> add(((JSONObject) a).getString("itemName"), List.of("学号", "收费项目", "交费区间", "应交", "缓交", "实交"),
                                                extractValue((JSONObject) a, new String[]{"personCode", "itemName", "intervalName", "needPay", "laterPay", "realPay"})));

                                case 3 ->
                                        data.forEach(a -> add(String.valueOf(++order), List.of("订单编号", "金额", "支付方式", "支付时间", "支付编号"),
                                                extractValue((JSONObject) a, new String[]{"orderNo", "money", "payTypeName", "payTime", "outPayNo"})));

                                case 4 ->
                                        data.forEach(a -> add(String.valueOf(++order), List.of("收费项目", "收费区间", "退费金额", "退费日期", "退费状态"),
                                                extractValue((JSONObject) a, new String[]{"itemName", "intervalName", "refundMoney", "refundDate", "refundStateStr"})));
                            }
                        }
                    } else if (response != null && response.getInteger("code").equals(4002)) {
                        params.toast(response.getString("message"));
                    } else {
                        params.toast(R.string.login_warning);
                        params.gotoLogin(TargetUrl.PAY);
                    }
                }
            }
        });
        http.setParams(params);
        http.setReferrer("https://pay.sysu.edu.cn/");
        http.setAuthorizationJar(new AuthorizationJar(requireContext()));
        http.setTokenRequired(true);
        getPage();
        return view;
    }

    @Override
    public void add(String title, @Nullable Integer icon, List<String> keys, List<String> values) {
        super.add(title, icon, keys, values);
        if (position == 0) {
            ChipGroup chips = view.findViewById(R.id.chips);
            Chip chip = ItemFilterChipBinding.inflate(getLayoutInflater(), chips, false).getRoot();
            chip.setText(title);
            chips.addView(chip, chips.getChildCount() - 1);
        }
    }

    void getPage() {
        switch (position) {
            case 0 -> getToPayList();
            case 1 -> getSelectivePayList();
            case 2 -> getFeeList(String.valueOf(calendarManager.getYear()));
            case 3 -> getPaymentList();
            case 4 -> getRefundList();
        }
    }

    void getToPayList() {
        http.postRequest("https://pay.sysu.edu.cn/client/api/client/necessary/list", "{}", 0);
    }

    void getSelectivePayList() {
        http.postRequest("https://pay.sysu.edu.cn/client/api/client/chooce/list", "{}", 1);
    }

    void getFeeList(String year) {
        http.postRequest("https://pay.sysu.edu.cn/client/api/client/record/feelist", String.format("{\"year\":%s}", year), 2);
    }

    void getPaymentList(String from, String to) {
        http.postRequest("https://pay.sysu.edu.cn/client/api/client/record/paymentlist", String.format("{\"startTime\":\"%s\",\"overTime\":\"%s\"}", from, to), 3);
    }

    void getPaymentList() {
        getPaymentList(calendarManager.toDateTimeString(calendarManager.getFirstOfMonth().atStartOfDay()), calendarManager.toDateTimeString(calendarManager.getEndOfMonth().atStartOfDay()));
    }

    void getRefundList() {
        http.postRequest("https://pay.sysu.edu.cn/client/api/client/refund/list", "{}", 4);
    }

    class DateManager {

        LocalDate fromDate;
        LocalDate toDate;

        public String getFromDateString() {
            return calendarManager.toDateString(fromDate);
        }

        public String getToDateString() {
            return calendarManager.toDateString(toDate);
        }

        public long getFromDateTimeMillis() {
            return calendarManager.toMillis(fromDate);
        }

        public void setFromDateTimeMillis(long from) {
            fromDate = calendarManager.toDate(from);
        }

        public long getToDateTimeMillis() {
            return calendarManager.toMillis(toDate);
        }
        
        public void setToDateTimeMillis(long to) {
            toDate = calendarManager.toDate(to);
        }
        
        public void getData() {
            getPaymentList(calendarManager.toDateTimeString(fromDate.atStartOfDay()), calendarManager.toDateTimeString(toDate.atStartOfDay()));
        }
    }
}