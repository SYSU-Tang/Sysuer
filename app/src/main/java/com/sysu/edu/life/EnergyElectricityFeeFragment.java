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
import com.sysu.edu.view.PreferenceAdapter;
import com.sysu.edu.view.PreferenceDialog;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class EnergyElectricityFeeFragment extends Fragment {
    
    final RequestQueue requestQueue = new RequestQueue();
    final ArraySet<CommonUtil.Tuple2<String, String>> rooms = new ArraySet<>();
    final MutableLiveData<String> roomCode = new MutableLiveData<>();
    HttpManager http;
    String name = "";
    ConcatAdapter adapter = new ConcatAdapter();
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Params params = new Params(this);
        ContextUtil contextUtil = new ContextUtil(requireContext());
        FragmentWaterFeeBinding binding = FragmentWaterFeeBinding.inflate(inflater, container, false);
        
        String[] paymentStatus = getResources().getStringArray(R.array.payment_status);
        binding.list.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.list.setAdapter(adapter);
        PreferenceDialog detailDialog = new PreferenceDialog(requireContext());
        http = new HttpManager(new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                if (msg.what == -1)
                    params.toast(R.string.no_net_connected);
                else if (msg.getData().getBoolean("isJSON")) {
                    JSONObject response = JSONObject.parse((String) msg.obj);
                    if (response.getInteger("code") == 200) {
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
                            case 2 ->
                                    response.getJSONObject("data").getJSONArray("useEleByDayList").forEach(e -> {
                                        JSONObject item = (JSONObject) e;
                                        Object useElectric = item.get("useElectric");
                                        String content = useElectric == null ? "暂无数据" : useElectric.toString();
                                        Calendar calendar = new Calendar();
                                        LocalDate date = LocalDate.parse(item.getString("date"), DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                                        calendar.setScheme(content);
                                        calendar.setYear(date.getYear());
                                        calendar.setMonth(date.getMonthValue());
                                        calendar.setDay(date.getDayOfMonth());
                                        binding.calendarView.addSchemeDate(calendar);
                                    });
                            case 3 -> {
                                reset();
                                response.getJSONObject("data").getJSONArray("list").forEach(e -> {
                                    JSONObject item = (JSONObject) e;
                                    adapter.addAdapter(new TitleAdapter(item.getString("billPeriod")));
                                    PreferenceAdapter preferenceAdapter = new PreferenceAdapter();
                                    ArrayList<String> value = extractValue(item, new String[]{"billPeriod", "billStatus", "remark", "useElectric", "name", "campusName", "areaInfo", "unitPrice", "totalUseAmount", "payedUseAmount", "billTime"});
                                    Integer billStatus = item.getInteger("billStatus");
                                    value.set(1, paymentStatus[billStatus - 1]);
                                    value.set(3, String.format("%s-%s=%s", item.getString("currReportElectric"), item.getString("lastReportElectric"), item.getString("useElectric")));
                                    preferenceAdapter.set(List.of(R.string.bill_period, R.string.status, R.string.remark, R.string.electricity_consumption, R.string.payer, R.string.campus, R.string.dorm, R.string.price, R.string.fee, R.string.paid_fee, R.string.pay_time),
                                            value,
                                            List.of(R.drawable.calendar, billStatus == 3 || billStatus == 5 ? R.drawable.check : R.drawable.uncheck, R.drawable.text, R.drawable.flash, R.drawable.account, R.drawable.location, R.drawable.home, R.drawable.money, R.drawable.money, R.drawable.money, R.drawable.time), requireContext());
                                    preferenceAdapter.setHideNull(true);
                                    ButtonAdapter buttonAdapter = new ButtonAdapter();
                                    buttonAdapter.add(getString(R.string.view_detail));
                                    if (billStatus == 1)
                                        buttonAdapter.add(getString(R.string.pay));
                                    buttonAdapter.setListener((button, position) -> {
                                        switch (position) {
                                            case 0 ->
                                                    button.setOnClickListener(_ -> requestQueue.addAndNext(() -> getDetail(item.getString("id"), roomCode.getValue())));
                                            case 1 ->
                                                    button.setOnClickListener(_ -> requestQueue.addAndNext(() -> recharge(item.getString("id"), roomCode.getValue(), item.getFloat("totalUseAmount"))));
                                        }
                                    });
                                    adapter.addAdapter(preferenceAdapter);
                                    adapter.addAdapter(buttonAdapter);
                                });
                            }
                            case 4 -> {
                                detailDialog.clear();
                                response.getJSONObject("data").getJSONArray("list").forEach(e -> {
                                    JSONObject item = (JSONObject) e;
                                    ArrayList<String> value = extractValue(item, new String[]{"billPeriod", "billStatus", "remark", "useElectric", "name", "campusName", "areaInfo", "unitPrice", "totalUseAmount", "payedUseAmount", "useAmount", "billTime"});
                                    Integer billStatus = item.getInteger("billStatus");
                                    value.set(1, paymentStatus[billStatus - 1]);
                                    value.set(3, String.format("%s-%s=%s", item.getString("currReportElectric"), item.getString("lastReportElectric"), item.getString("useElectric")));
                                    detailDialog.getAdapter().set(List.of(R.string.bill_period, R.string.status, R.string.remark, R.string.electricity_consumption, R.string.payer, R.string.campus, R.string.dorm, R.string.price, R.string.fee, R.string.paid_fee, R.string.unpaid_fee, R.string.pay_time),
                                            value,
                                            List.of(R.drawable.calendar, billStatus == 3 || billStatus == 5 ? R.drawable.check : R.drawable.uncheck, R.drawable.text, R.drawable.flash, R.drawable.account, R.drawable.location, R.drawable.home, R.drawable.money, R.drawable.money, R.drawable.money, R.drawable.money, R.drawable.time), requireContext());
                                    detailDialog.getAdapter().setHideNull(true);
                                });
                                detailDialog.show();
                            }
                            case 5 -> {
                                params.toast(response.getString("msg"));
                                requestQueue.addAndNext(() -> getElectricityBill(roomCode.getValue()));
                            }
                        }
                        requestQueue.next();
                    } else if (response.getInteger("errorCode") == 500)
                        params.toast(response.getString("msg"));
                    else contextUtil.login(TargetUrl.ZHNY, requestQueue::retry);
                }
            }
        });
        http.setAuthorizationRequired(true);
        http.setParams(params);
        http.setAuthorizationJar(new AuthorizationJar(requireContext()));
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        requestQueue.add(this::getUserInfo);
        requestQueue.add(() -> getRoom(name));
        roomCode.observe(getViewLifecycleOwner(), v -> {
            if (v != null) {
                LocalDate date = LocalDate.of(binding.calendarView.getSelectedCalendar().getYear(), binding.calendarView.getSelectedCalendar().getMonth(), 1);
                requestQueue.add(() -> getElectricityConsumption(v, date.with(TemporalAdjusters.firstDayOfMonth()).format(formatter), date.with(TemporalAdjusters.lastDayOfMonth()).format(formatter)));
                requestQueue.addAndNext(() -> getElectricityBill(v));
            }
        });
        requestQueue.next();
        binding.calendarView.setOnMonthChangeListener((year, month) -> {
            requestQueue.addAndNext(() -> getElectricityConsumption(roomCode.getValue(), LocalDate.of(year, month, 1).with(TemporalAdjusters.firstDayOfMonth()).format(formatter), LocalDate.of(year, month, 1).with(TemporalAdjusters.lastDayOfMonth()).format(formatter)));
            binding.date.setText(LocalDate.of(year, month, 1).format(formatter));
        });
        binding.date.setText(LocalDate.now().format(formatter));
        binding.spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                requestQueue.addAndNext(() -> roomCode.setValue(rooms.valueAt(position).second));
            }
            
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            
            }
        });
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
    
    void getElectricityConsumption(String roomCode, String startDate, String endDate) {
        http.postRequest("https://zhny.sysu.edu.cn/kbp/ele/wechat/eleConsume", String.format("{\"roomCode\":\"%s\",\"startDate\":\"%s\",\"endDate\":\"%s\"}", roomCode, startDate, endDate), 2);
    }
    
    void getElectricityBill(String roomCode) {
        http.postRequest("https://zhny.sysu.edu.cn/kbp/ele/mobile/billRecord", String.format("{\"roomCode\":\"%s\",\"billType\":1}", roomCode), 3);
    }
    
    void getDetail(String id, String room) {
        http.postRequest("https://zhny.sysu.edu.cn/kbp/ele/mobile/billRecord", String.format("{\"id\":\"%s\",\"roomCode\":\"%s\"}", id, room), 4);
    }
    
    void recharge(String id, String roomCode, float amount) {
        http.postRequest("https://zhny.sysu.edu.cn/kbp/ele/mobile/pay/bill/recharge", String.format(Locale.getDefault(), "{\"roomCode\":\"%s\",\"actualBillAmount\":%.2f,\"useTypeEleAndMoneyList\":[{\"billAmount\":%.2f,\"useEleType\":1,\"idList\":[\"%s\"]}],\"rechargeType\":16}", roomCode, amount, amount, id), 5);
    }
}
