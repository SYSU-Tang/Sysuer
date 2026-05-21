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
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ConcatAdapter;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.alibaba.fastjson2.JSONObject;
import com.sysu.edu.R;
import com.sysu.edu.api.AuthorizationJar;
import com.sysu.edu.api.CommonUtil;
import com.sysu.edu.api.ContextUtil;
import com.sysu.edu.api.HttpManager;
import com.sysu.edu.api.Params;
import com.sysu.edu.api.RequestQueue;
import com.sysu.edu.api.TargetUrl;
import com.sysu.edu.databinding.FragmentEnergyDashboardBinding;
import com.sysu.edu.todo.TitleAdapter;
import com.sysu.edu.view.PreferenceAdapter;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public class EnergyDashboardFragment extends Fragment {

    HttpManager http;
    String name;
    final RequestQueue requestQueue = new RequestQueue();
    final ArraySet<CommonUtil.Tuple2<String, String>> rooms = new ArraySet<>();
    final ConcatAdapter adapter = new ConcatAdapter();
    FragmentEnergyDashboardBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (binding == null) {
            Params params = new Params(this);
            ContextUtil contextUtil = new ContextUtil(requireContext());
            binding = FragmentEnergyDashboardBinding.inflate(inflater, container, false);
            binding.list.setLayoutManager(new LinearLayoutManager(requireContext()));
            binding.list.setAdapter(adapter);
            http = new HttpManager(new Handler(Looper.getMainLooper()) {
                @Override
                public void handleMessage(@NonNull Message msg) {
                    if (msg.what == -1)
                        params.toast(R.string.no_net_connected);
                    else if (msg.getData().getBoolean("isJSON")) {
                        JSONObject response = JSONObject.parse((String) msg.obj);
                        if (response.getInteger("code") == 200) {
                            JSONObject data = response.getJSONObject("data");
                            switch (msg.what) {
                                case 0 -> name = data.getString("username");
                                case 1 -> {
                                    binding.lastMonthElectricity.setText(String.format(Locale.getDefault(), "上月用电\n%.2f", data.getDouble("lastMonthUsage")));
                                    binding.thisMonthElectricity.setText(String.format(Locale.getDefault(), "本月用电\n%.2f", data.getDouble("currentMonthUsage")));
                                    binding.deltaElectricity.setText(String.format(Locale.getDefault(), "用电变化\n%.2f", data.getDouble("usageChange")));
                                }
                                case 2 -> {
                                    binding.lastMonthWater.setText(String.format(Locale.getDefault(), "上月用水\n%.2f", data.getDouble("lastMonthUsage")));
                                    binding.thisMonthWater.setText(String.format(Locale.getDefault(), "本月用水\n%.2f", data.getDouble("thisMonthUsage")));
                                    binding.deltaWater.setText(String.format(Locale.getDefault(), "用水变化\n%.2f", data.getDouble("growthUsage")));
                                }
                                case 3 -> {
                                    reset();
                                    response.getJSONObject("data").getJSONArray("records").forEach(e -> {
                                        JSONObject item = (JSONObject) e;
                                        TitleAdapter titleAdapter = new TitleAdapter(item.getString("date"));
                                        titleAdapter.setHeader(2);
                                        adapter.addAdapter(titleAdapter);
                                        item.getJSONArray("detailRecords").forEach(o -> {
                                            JSONObject detail = (JSONObject) o;
                                            adapter.addAdapter(new TitleAdapter(detail.getString("tradeTypeDesc")));
                                            PreferenceAdapter preferenceAdapter = new PreferenceAdapter();
                                            preferenceAdapter.set(List.of(R.string.type, R.string.time, R.string.fee, R.string.payer, R.string.student_id),
                                                    extractValue(detail, new String[]{"tradeTypeDesc", "tradeTime", "tradeAmount", "name", "username", "paidPayment"}),
                                                    List.of(R.drawable.menu, R.drawable.time, R.drawable.money, R.drawable.account, R.drawable.id), requireContext());
                                            preferenceAdapter.setHideNull(true);
                                            adapter.addAdapter(preferenceAdapter);
                                        });
                                    });
                                }
                                case 4 -> {
                                    ArrayAdapter<Object> items = new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1);
                                    response.getJSONArray("data").forEach(e -> {
                                        JSONObject roomInfo = (JSONObject) e;
                                        rooms.add(new CommonUtil.Tuple2<>(roomInfo.getString("roomName"), roomInfo.getString("roomCode")));
                                        items.add(roomInfo.getString("roomName"));
                                    });
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
            requestQueue.add(this::getUserInfo);
            requestQueue.add(this::getWaterInfo);
            requestQueue.add(() -> getElectricityInfo(name));
            requestQueue.add(() -> getRoom(name));
            requestQueue.add(() -> {
                if (!rooms.isEmpty())
                    getOrderInfo(rooms.valueAt(0).getSecond(), LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM")));
            });
            requestQueue.next();
        }
        return binding.getRoot();
    }

    void getUserInfo() {
        http.getRequest("https://zhny.sysu.edu.cn/kbp/auth/userInfo", 0);
    }

    void getElectricityInfo(String username) {
        http.getRequest("https://zhny.sysu.edu.cn/kbp/ele/wechat/eleSituation?username=" + username, 1);
    }

    void getWaterInfo() {
        http.postRequest("https://zhny.sysu.edu.cn/kbp/cwbs/user/usage/stats", "", 2);
    }

    void getOrderInfo(String room, String date) {
        http.postRequest("https://zhny.sysu.edu.cn/kbp/record/roomBalance/detail", String.format("{\"dateType\":\"month\",\"roomCode\":\"%s\",\"dateRange\":\"%s\",\"id\":null,\"tradeTime\":\"\"}", room, date), 3);
    }

    void getRoom(String username) {
        http.postRequest("https://zhny.sysu.edu.cn/kbp/admin/sys/personRoom/list", "{\"username\":\"" + username + "\"}", 4);
    }

    void reset() {
        adapter.getAdapters().forEach(adapter::removeAdapter);
    }
}
