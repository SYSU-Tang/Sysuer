package com.sysu.edu.academic;

import android.app.Activity;
import android.content.Intent;
import android.icu.util.Calendar;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.alibaba.fastjson2.JSONObject;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.CompositeDateValidator;
import com.google.android.material.datepicker.DateValidatorPointBackward;
import com.google.android.material.datepicker.DateValidatorPointForward;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.sysu.edu.R;
import com.sysu.edu.api.Params;
import com.sysu.edu.databinding.ChipBinding;
import com.sysu.edu.databinding.PayNeedFragmentBinding;
import com.sysu.edu.databinding.PayRecordFragmentBinding;
import com.sysu.edu.databinding.PaySituationFragmentBinding;
import com.sysu.edu.extra.LoginActivity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class PayFragment extends StaggeredFragment {
    public View view;
    Handler handler;
    OkHttpClient http = new OkHttpClient.Builder().build();
    String token;
    Params params;
    int order = 0;

    public static PayFragment newInstance(int position) {
        PayFragment f = new PayFragment();
        f.position = position;
        return f;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        params = new Params(requireActivity());
        token = params.getToken();
        switch (position) {
            case 0:
                PayNeedFragmentBinding b0 = PayNeedFragmentBinding.inflate(inflater);
                b0.getRoot().addView(view);
                b0.pay.setOnClickListener(a-> params.browse("https://pay.sysu.edu.cn/#/confirm/pay-ticket?type=1"));
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
                ArrayList<String> years = new ArrayList<>(List.of("全部", "无区间年度"));
                ArrayList<String> yearCodes = new ArrayList<>(List.of("null", "-1"));
                for (int i = 0; i < 6; i++) {
                    years.add(Params.getYear() + 1 - i + "年");
                    yearCodes.add(String.valueOf(Params.getYear() + 1 - i));
                }
                PaySituationFragmentBinding b1 = PaySituationFragmentBinding.inflate(getLayoutInflater());
                b1.getRoot().addView(view);
                binding.recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
                    @Override
                    public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                        b1.p.setElevation(recyclerView.canScrollVertically(-1) ? 6 : 0);
                        super.onScrolled(recyclerView, dx, dy);
                    }
                });
                b1.spinner.setText(String.valueOf(Params.getYear()));
                b1.spinner.setSimpleItems(years.toArray(new String[]{}));
                b1.spinner.setOnItemClickListener((adapterView, view1, i, l) -> {
                    clear();
                    getFeeList(String.valueOf(yearCodes.get(i)));
                });
                view = b1.getRoot();
                break;
            case 3:

                DateManager dm = new DateManager();
                dm.fromDate = Params.getFirstOfMonth().getTime();
                dm.toDate = Params.getEndOfMonth().getTime();
                PayRecordFragmentBinding b2 = PayRecordFragmentBinding.inflate(inflater);
                b2.getRoot().addView(view);
                view = b2.getRoot();
                b2.from.setText(dm.getFromDateString());
                b2.from.setOnClickListener(view2 -> {
                    MaterialDatePicker<Long> fromDatePicker = MaterialDatePicker.Builder.datePicker().setSelection(dm.getFromDateTimeMillis()).setCalendarConstraints(new CalendarConstraints.Builder()
                            .setValidator(CompositeDateValidator.allOf(List.of(DateValidatorPointBackward.before(dm.getToDateTimeMillis())))).build()).build();
                    fromDatePicker.addOnPositiveButtonClickListener(selection -> {
                        dm.fromDate = new Date(selection);
                        fromDatePicker.dismissAllowingStateLoss();
                        b2.from.setText(dm.getFromDateString());
                        dm.getData();
                    });
                    fromDatePicker.show(requireActivity().getSupportFragmentManager(), null);
                });
                b2.to.setText(dm.getToDateString());
                b2.to.setOnClickListener(view2 -> {
                    MaterialDatePicker<Long> toDatePicker = MaterialDatePicker.Builder.datePicker().setSelection(dm.getToDateTimeMillis()).setCalendarConstraints(new CalendarConstraints.Builder().setValidator(CompositeDateValidator.allOf(List.of(DateValidatorPointForward.from(dm.getFromDateTimeMillis())))).build()).build();
                    toDatePicker.addOnPositiveButtonClickListener(selection -> {
                        dm.toDate=new Date(selection);
                        toDatePicker.dismissAllowingStateLoss();
                        b2.to.setText(dm.getToDateString());
                        dm.getData();
                    });
                    toDatePicker.show(requireActivity().getSupportFragmentManager(), null);
                });
                binding.recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
                    @Override
                    public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                        b2.row.setElevation(recyclerView.canScrollVertically(-1) ? 6 : 0);
                        super.onScrolled(recyclerView, dx, dy);
                    }
                });
                break;
        }
        ActivityResultLauncher<Intent> launch = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), o -> {
            if (o.getResultCode() == Activity.RESULT_OK) {
                token = params.getToken();
                getToPayList();
            }
        });
        handler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                if (msg.what == -1) {
                    Toast.makeText(requireContext(), getString(R.string.no_wifi_warning), Toast.LENGTH_LONG).show();
                } else {
                    JSONObject response = JSONObject.parseObject((String) msg.obj);
                    if (response != null && response.getInteger("code").equals(200)) {
                        if (response.get("data") != null) {
                            switch (msg.what) {
                                case 0:
                                case 1:
                                    clear();
                                    response.getJSONArray("data").forEach(a -> {
                                        ArrayList<String> values = new ArrayList<>();
                                        String[] keyName = new String[]{"学号", "交费区间", "当前应交", "本次交费"};
                                        for (int i = 0; i < keyName.length; i++) {
                                            values.add(((JSONObject) a).getString(new String[]{"personCode", "intervalName", "nowMoney", "needMoney"}[i]));
                                        }
                                        add(((JSONObject) a).getString("itemName"), List.of(keyName), values);
                                    });
                                    break;
                                case 2:
                                    clear();
                                    response.getJSONArray("data").forEach(a -> {
                                        ArrayList<String> values = new ArrayList<>();
                                        String[] keyName = new String[]{"学号", "收费项目", "交费区间", "应交", "缓交", "实交"};
                                        for (int i = 0; i < keyName.length; i++) {
                                            values.add(((JSONObject) a).getString(new String[]{"personCode", "itemName", "intervalName", "needPay", "laterPay", "realPay"}[i]));
                                        }
                                        add(((JSONObject) a).getString("itemName"), List.of(keyName), values);
                                    });
                                    break;
                                case 3:
                                    clear();
                                    response.getJSONArray("data").forEach(a -> {
                                        ArrayList<String> values = new ArrayList<>();
                                        String[] keyName = new String[]{"订单编号", "金额", "支付方式", "支付时间", "支付编号"};
                                        for (int i = 0; i < keyName.length; i++) {
                                            values.add(((JSONObject) a).getString(new String[]{"orderNo", "money", "payTypeName", "payTime", "outPayNo"}[i]));
                                        }
                                        add(String.valueOf(++order), List.of(keyName), values);
                                    });
                                    break;
                                case 4:
                                    clear();
                                    response.getJSONArray("data").forEach(a -> {
                                        ArrayList<String> values = new ArrayList<>();
                                        String[] keyName = new String[]{"收费项目", "收费区间", "退费金额", "退费日期", "退费状态"};
                                        for (int i = 0; i < keyName.length; i++) {
                                            values.add(((JSONObject) a).getString(new String[]{"itemName", "intervalName", "refundMoney", "refundDate", "refundStateStr"}[i]));
                                        }
                                        add(String.valueOf(++order), List.of(keyName), values);
                                    });
                                    break;
                            }
                        }
                    } else if (response != null && response.getInteger("code").equals(4002)) {
                        Toast.makeText(requireContext(), response.getString("message"), Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(requireContext(), getString(R.string.login_warning), Toast.LENGTH_LONG).show();
                        launch.launch(new Intent(requireContext(), LoginActivity.class).putExtra("url", "https://cas.sysu.edu.cn/cas/login?service=https://pay.sysu.edu.cn/sso"));
                    }
                }
            }
        };
        getPage();
        return view;
    }

    @Override
    public void add(String title, @Nullable Integer icon, List<String> keys, List<String> values) {
        super.add(title, icon, keys, values);
        if (position==0){
            ChipGroup chips = view.findViewById(R.id.chips);
            Chip chip = ChipBinding.inflate(getLayoutInflater(), chips, false).getRoot();
            chip.setText(title);
            chips.addView(chip,chips.getChildCount()-1);
        }

    }

    void getPage(){
        switch (position){
            case 0:getToPayList();break;
            case 1:getSelectivePayList();break;
            case 2:getFeeList(String.valueOf(Params.getYear()));break;
            case 3:getPaymentList();break;
            case 4:getRefundList();break;
        }

    }
    void getToPayList() {
        getList("https://pay.sysu.edu.cn/client/api/client/necessary/list","{}",0);
    }

    void getSelectivePayList() {
        getList("https://pay.sysu.edu.cn/client/api/client/chooce/list","{}",1);
    }

    void getFeeList(String year) {
        getList("https://pay.sysu.edu.cn/client/api/client/record/feelist",String.format("{\"year\":%s}", year),2);
    }

    void getPaymentList(String from,String to) {
        getList("https://pay.sysu.edu.cn/client/api/client/record/paymentlist",String.format("{\"startTime\":\"%s\",\"overTime\":\"%s\"}",from,to),3);
    }
    void getPaymentList(){
        getPaymentList(Params.getDateTime(Params.getFirstOfMonth()),Params.getDateTime(Params.getEndOfMonth()));
    }

    void getRefundList() {
        getList("https://pay.sysu.edu.cn/client/api/client/refund/list","{}",4);
    }
    void getList(String url,String body,int what) {
        http.newCall(new Request.Builder().url(url)
                .header("token", token)
                .header("Accept-language", "zh-CN")
                .post(RequestBody.create(body, MediaType.parse("application/json")))
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
                msg.what = what;
                msg.obj = response.body().string();
                handler.sendMessage(msg);
            }
        });
    }
    class DateManager{
        final Calendar c = Calendar.getInstance();
        public Date fromDate;
        public Date toDate;
        public DateManager(){}
        public String getFromDateString(){
            return Params.toDate(fromDate);
        }
        public String getToDateString(){
            return Params.toDate(toDate);
        }
        public long getFromDateTimeMillis(){
            c.setTime(fromDate);
            return c.getTimeInMillis();
        }
        public long getToDateTimeMillis(){
            c.setTime(toDate);
            return c.getTimeInMillis();
        }
        public void getData(){
            getPaymentList(Params.getDateTime(fromDate),Params.getDateTime(toDate));
        }
    }
}
