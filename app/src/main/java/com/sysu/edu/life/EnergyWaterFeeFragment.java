package com.sysu.edu.life;

import static com.sysu.edu.api.CommonUtil.extractValue;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.ArraySet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.MutableLiveData;
import androidx.recyclerview.widget.ConcatAdapter;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.alibaba.fastjson2.JSONObject;
import com.haibin.calendarview.Calendar;
import com.sysu.edu.R;
import com.sysu.edu.api.AuthorizationJar;
import com.sysu.edu.api.CommonUtil;
import com.sysu.edu.api.ContextUtil;
import com.sysu.edu.api.HttpManager;
import com.sysu.edu.api.Params;
import com.sysu.edu.api.RequestQueue;
import com.sysu.edu.api.TargetUrl;
import com.sysu.edu.databinding.FragmentWaterFeeBinding;
import com.sysu.edu.todo.TitleAdapter;
import com.sysu.edu.view.ButtonAdapter;
import com.sysu.edu.view.FeeMonthView;
import com.sysu.edu.view.FeeWeekView;
import com.sysu.edu.view.PreferenceAdapter;
import com.sysu.edu.view.PreferenceDialog;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class EnergyWaterFeeFragment extends Fragment {

    HttpManager http;
    String name = "";
    final RequestQueue requestQueue = new RequestQueue();
    final ArraySet<CommonUtil.Tuple2<String, String>> rooms = new ArraySet<>();
    final MutableLiveData<String> roomCode = new MutableLiveData<>();
    ConcatAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Params params = new Params(this);
        ContextUtil contextUtil = new ContextUtil(requireContext());
        adapter = new ConcatAdapter();
        FragmentWaterFeeBinding binding = FragmentWaterFeeBinding.inflate(inflater, container, false);
        binding.list.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.list.setAdapter(adapter);
        binding.calendarView.setMonthView(FeeMonthView.class);
        binding.calendarView.setWeekView(FeeWeekView.class);
        PreferenceDialog detailDialog = new PreferenceDialog(requireContext());
        String[] paymentStatuses = getResources().getStringArray(R.array.payment_status);
        http = new HttpManager(new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                if (msg.what == -1)
                    params.toast(R.string.no_net_connected);
                else if (msg.getData().getBoolean("isJSON")) {
                    JSONObject response = JSONObject.parse((String) msg.obj);
                    if(msg.what == 5 && response.getInteger("status") == 404)
                        params.toast(response.getString("error"));
                    else if (response.getInteger("code") == 200) {
                        switch (msg.what) {
                            case 0 -> name = response.getJSONObject("data").getString("username");
                            case 1 -> {
                                ArrayAdapter<Object> items = new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1);
                                binding.spinner.setAdapter(items);
                                response.getJSONArray("data").forEach(e -> {
                                    JSONObject roomInfo = (JSONObject) e;
                                    rooms.add(new CommonUtil.Tuple2<>(roomInfo.getString("roomName"), roomInfo.getString("roomCode")));
                                    items.add(roomInfo.getString("roomName"));
                                });
                            }
                            case 2 -> {
                                PreferenceAdapter preferenceAdapter = new PreferenceAdapter();
                                response.getJSONObject("data").getJSONArray("waterUsageList").forEach(e -> {
                                    JSONObject item = (JSONObject) e;
                                    Object totalWaterUsage = item.get("totalWaterUsage");
                                    String content = totalWaterUsage == null ? getString(R.string.no_data_available) : totalWaterUsage.toString();
                                    Calendar calendar = new Calendar();
                                    calendar.setScheme(content);
                                    calendar.setYear(binding.calendarView.getSelectedCalendar().getYear());
                                    calendar.setMonth(binding.calendarView.getSelectedCalendar().getMonth());
                                    calendar.setDay(Integer.parseInt(item.getString("timeLabel")));
                                    binding.calendarView.addSchemeDate(calendar);
                                });
                                adapter.addAdapter(preferenceAdapter);
                            }
                            case 3 -> {
                                reset();
                                response.getJSONObject("data").getJSONArray("billList").forEach(e -> {
                                    JSONObject item = (JSONObject) e;
                                    String duration = String.format("%s~%s", item.getString("originalBillStartDate"), item.getString("originalBillEndDate"));
                                    adapter.addAdapter(new TitleAdapter(duration));
                                    PreferenceAdapter preferenceAdapter = new PreferenceAdapter();
                                    ArrayList<String> value = extractValue(item, new String[]{"billStartDate", "paymentStatus", "useWaterTypeName", "finalWaterUsage", "waterPayment", "paidPayment"});
                                    Integer paymentStatus = item.getInteger("paymentStatus");
                                    value.set(0, duration);
                                    value.set(1, paymentStatuses[paymentStatus - 1]);
                                    preferenceAdapter.set(List.of(R.string.bill_period, R.string.status, R.string.type, R.string.electricity_consumption, R.string.fee, R.string.paid_fee),
                                            value,
                                            List.of(R.drawable.calendar, paymentStatus == 3 || paymentStatus == 5 ? R.drawable.check : R.drawable.uncheck, R.drawable.water, R.drawable.water, R.drawable.money, R.drawable.money), requireContext());
                                    preferenceAdapter.setHideNull(true);
                                    ButtonAdapter buttonAdapter = new ButtonAdapter();
                                    buttonAdapter.add(getString(R.string.view_detail));
                                    if (paymentStatus == 1)
                                        buttonAdapter.add(getString(R.string.pay));
                                    buttonAdapter.setListener((button, position) -> {
                                        switch (position) {
                                            case 0 ->
                                                    button.setOnClickListener(_ -> requestQueue.addAndNext(() -> getDetail(item.getString("id"))));
                                            case 1 ->
                                                    button.setOnClickListener(_ ->
                                                        requestQueue.addAndNext(() -> recharge(item.getString("id"), roomCode.getValue(), item.getFloat("waterPayment")))
                                                    );
                                        }
                                    });
                                    adapter.addAdapter(preferenceAdapter);
                                    adapter.addAdapter(buttonAdapter);
                                });
                            }
                            case 4 -> {
                                detailDialog.clear();
                                JSONObject item = response.getJSONObject("data");
                                ArrayList<String> value = extractValue(item, new String[]{"billStartDate", "billStatus", "remark", "finalWaterUsage", "useWaterTypeName", "areaInfo", "unitPrice", "waterPayment", "paidPayment", "unpaidPayment", "createTime"});
                                Integer billStatus = item.getInteger("paymentStatus");
                                value.set(0, String.format("%s~%s", item.getString("billStartDate"), item.getString("billEndDate")));
                                value.set(2, item.getString("remark") == null ? "-" : item.getString("remark"));
                                value.set(1, paymentStatuses[billStatus - 1]);
                                value.set(3, String.format("%s-%s=%s", item.getString("currMeterReading"), item.getString("lastMeterReading"), item.getString("finalWaterUsage")));
                                detailDialog.getAdapter().set(List.of(R.string.bill_period, R.string.status, R.string.remark, R.string.water_consumption, R.string.type, R.string.dorm, R.string.price, R.string.fee, R.string.paid_fee, R.string.unpaid_fee, R.string.pay_time),
                                        value,
                                        List.of(R.drawable.calendar, billStatus == 3 || billStatus == 5 ? R.drawable.check : R.drawable.uncheck, R.drawable.text, R.drawable.menu, R.drawable.dashboard, R.drawable.home, R.drawable.money, R.drawable.money, R.drawable.money, R.drawable.money, R.drawable.time), requireContext());
                                detailDialog.show();
                            }
                            case 5 -> {
                                params.toast(response.getString("msg"));
                                requestQueue.addAndNext(() -> getWaterBill(roomCode.getValue()));
                            }
                        }
                        requestQueue.next();
                    } else contextUtil.login(TargetUrl.ZHNY, requestQueue::retry);
                }
                super.handleMessage(msg);
            }
        });
        http.setAuthorizationRequired(true);
        http.setParams(params);
        http.setAuthorizationJar(new AuthorizationJar(requireContext()));
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM");
        requestQueue.add(this::getUserInfo);
        requestQueue.add(() -> getRoom(name));
        roomCode.observe(getViewLifecycleOwner(), v -> {
            if (v != null) {
                requestQueue.add(() -> getWaterConsumption(v, LocalDate.of(binding.calendarView.getSelectedCalendar().getYear(), binding.calendarView.getSelectedCalendar().getMonth(), 1).format(formatter)));
                requestQueue.addAndNext(() -> getWaterBill(v));
            }
        });
        binding.calendarView.setOnMonthChangeListener((year, month) -> {
            requestQueue.addAndNext(() -> getWaterConsumption(roomCode.getValue(), LocalDate.of(year, month, 1).format(formatter)));
            binding.date.setText(LocalDate.of(year, month, 1).format(formatter));
        });
        binding.date.setText(LocalDate.now().format(formatter));
        binding.spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                reset();
                roomCode.setValue(rooms.valueAt(position).getSecond());
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        requestQueue.next();

        return binding.getRoot();
    }

    void reset() {
        adapter.getAdapters().forEach(adapter::removeAdapter);
    }

    void getUserInfo() {
        http.getRequest("https://zhny.sysu.edu.cn/kbp/auth/userInfo", 0);
    }

    void getRoom(String username) {
        http.postRequest("https://zhny.sysu.edu.cn/kbp/admin/sys/personRoom/list", "{\"username\":\"" + username + "\"}", 1);
    }

    void getWaterConsumption(String room, String date) {
        http.postRequest("https://zhny.sysu.edu.cn/kbp/cwbs/month/usage/stats", String.format("{\"roomCode\":\"%s\",\"staticsMonth\":\"%s\"}", room, date), 2);
    }

    void getWaterBill(String room) {
        http.postRequest("https://zhny.sysu.edu.cn/kbp/cwbs/mobile/room/bill/list", String.format("{\"roomCode\":\"%s\"}", room), 3);
    }

    void getDetail(String billId) {
        http.getRequest("https://zhny.sysu.edu.cn/kbp/cwbs/mobile/room/bill/get/" + billId, 4);
    }
    void recharge(String billId, String room, float amount) {
        http.postRequest("https://zhny.sysu.edu.cn/kbp/cwbs/mobile/room/bill/pay", String.format(Locale.getDefault(), "{\"roomCode\":\"%s\",\"billAmount\":%.2f,\"idList\":[\"%s\"],\"isMobile\":true,\"rechargeChannel\":6,\"rechargeMethod\":16}", room,amount,billId), 5);
    }
}
