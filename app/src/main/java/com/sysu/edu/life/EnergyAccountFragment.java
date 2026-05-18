package com.sysu.edu.life;

import static android.text.TextUtils.isEmpty;
import static com.sysu.edu.api.CommonUtil.extractValue;
import static com.sysu.edu.api.CommonUtil.toStringOrDefault;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.Editable;
import android.util.ArraySet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ConcatAdapter;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.alibaba.fastjson2.JSONObject;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.sysu.edu.R;
import com.sysu.edu.api.AuthorizationJar;
import com.sysu.edu.api.CommonUtil;
import com.sysu.edu.api.ContextUtil;
import com.sysu.edu.api.HttpManager;
import com.sysu.edu.api.Params;
import com.sysu.edu.api.RequestQueue;
import com.sysu.edu.api.TargetUrl;
import com.sysu.edu.databinding.DialogRechargeBinding;
import com.sysu.edu.databinding.FragmentEnergyOrderBinding;
import com.sysu.edu.todo.TitleAdapter;
import com.sysu.edu.view.ButtonAdapter;
import com.sysu.edu.view.PreferenceAdapter;

import java.io.IOException;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Response;

public class EnergyAccountFragment extends Fragment {

    HttpManager http;
    String roomCode;
    String username;
    FragmentEnergyOrderBinding binding;
    final RequestQueue requestQueue = new RequestQueue();
    final ArraySet<CommonUtil.Tuple2<String, String>> rooms = new ArraySet<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        if (binding == null) {
            Params params = new Params(this);
            ConcatAdapter adapter = new ConcatAdapter();
            ContextUtil contextUtil = new ContextUtil(requireContext());
            BottomSheetDialog rechargeDialog = new BottomSheetDialog(requireContext());
            DialogRechargeBinding rechargeBinding = DialogRechargeBinding.inflate(inflater);
            rechargeDialog.setContentView(rechargeBinding.getRoot());
            rechargeBinding.submit.setOnClickListener(_ -> {
                Editable money = rechargeBinding.rmb.getText();
                int rmb;
                if (!isEmpty(money) && (rmb = (int) (Float.parseFloat(money.toString()) * 100)) > 0) {
                    recharge(rmb, roomCode, toStringOrDefault(rechargeBinding.remark.getText()));
                }
            });
            http = new HttpManager(new Handler(Looper.getMainLooper()) {
                @Override
                public void handleMessage(@NonNull Message msg) {
                    if (msg.what == -1) params.toast(R.string.no_net_connected);
                    else if (msg.what == 4) {
                        System.out.println((String) msg.obj);
                        params.copy("recharge", (String) msg.obj);
                        Intent intent = Intent.createChooser(new Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, (String) msg.obj).putExtra(Intent.EXTRA_SUBJECT, getString(R.string.recharge)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), getString(R.string.share));
                        if (intent.resolveActivity(requireContext().getPackageManager()) != null)
                            startActivity(intent);
                    } else if (msg.getData().getBoolean("isJSON")) {
                        JSONObject response = JSONObject.parse((String) msg.obj);
                        if (response.getInteger("code") == 200) {
                            switch (msg.what) {
                                case 0 -> {
                                    JSONObject userInfo = response.getJSONObject("data");
                                    adapter.addAdapter(new TitleAdapter(getString(R.string.account)));
                                    PreferenceAdapter preferenceAdapter = new PreferenceAdapter();
                                    preferenceAdapter.set(List.of(R.string.name, R.string.student_id),
                                            extractValue(userInfo, new String[]{"name", "username"}), List.of(R.drawable.account, R.drawable.id), requireContext());
                                    adapter.addAdapter(preferenceAdapter);
                                    username = userInfo.getString("username");
                                }
                                case 1 -> response.getJSONArray("data").forEach(e -> {
                                    JSONObject roomInfo = (JSONObject) e;
                                    adapter.addAdapter(new TitleAdapter(getString(R.string.dorm)));
                                    PreferenceAdapter preferenceAdapter = new PreferenceAdapter();
                                    preferenceAdapter.set(List.of(R.string.location, R.string.room_name),
                                            extractValue(roomInfo, new String[]{"areaInfo", "roomName"}), List.of(R.drawable.location, R.drawable.home), requireContext());
                                    adapter.addAdapter(preferenceAdapter);
                                    rooms.add(new CommonUtil.Tuple2<>(roomInfo.getString("roomName"), roomInfo.getString("roomCode")));
                                });
                                case 2 -> {
                                    adapter.addAdapter(new TitleAdapter(getString(R.string.balance)));
                                    PreferenceAdapter preferenceAdapter = new PreferenceAdapter();
                                    preferenceAdapter.add(getString(R.string.balance), response.getJSONObject("data").getString("balance"), R.drawable.money);
                                    adapter.addAdapter(preferenceAdapter);
                                    ButtonAdapter buttonAdapter = new ButtonAdapter();
                                    buttonAdapter.add(getString(R.string.pay));
                                    buttonAdapter.setListener((button, _) -> button.setOnClickListener(_ -> rechargeDialog.show()));
                                    adapter.addAdapter(buttonAdapter);
                                }
                                case 3 -> {
                                    System.out.println(response);
                                    gotoWechat(response.getJSONObject("data").getJSONObject("data"));
                                    /*https://fee.sysu.edu.cn/gateway/cashier/app/order?orderno=1487527133151629312&scene=wx&showwxpaytitle=1&response_type=code&scope=snsapi_base&state=1&connect_redirect=1#wechat_redirect*/
                                    /*{"code":200,"msg":"操作成功","data":{"data":{"page_url":"https://zhny.sysu.edu.cn/h5/#/pages/indexModule/pages/electricityRecharge/electricityRecharge","charset":"utf-8","pay_info":"{\"items\":[{\"item_code\":\"138\",\"item_money\":0.01}],\"total_money\":0.01}","sign":"FCE7FB5850452D14095B03D0456E6AF0","mch_id":"sysuZHNYGLPT","notify_url":"https://zhny.sysu.edu.cn/kbp/pay/notify/zdpay","person_code":"24308152","version":"2.0","scene":"wx","out_trade_no":"75892026032818574230884387233138","timestamp":"20260328185742309"},"url":"https://fee.sysu.edu.cn/gateway/unifiedorder/pagepay","outTradeNo":"75892026032818574230884387233138"}}*/
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
            binding = FragmentEnergyOrderBinding.inflate(inflater, container, false);
            binding.recyclerViewScroll.getRoot().setLayoutManager(new LinearLayoutManager(requireContext()));
            binding.recyclerViewScroll.getRoot().setAdapter(adapter);
            requestQueue.add(this::getUserInfo);
            requestQueue.add(() -> getRoom(username));
            requestQueue.add(() -> {
                if (!rooms.isEmpty()) {
                    roomCode = rooms.valueAt(0).second;
                    getBalance(roomCode);
                }
            });
            requestQueue.next();
        }
        return binding.getRoot();
    }

    void getUserInfo() {
        http.getRequest("https://zhny.sysu.edu.cn/kbp/auth/userInfo", 0);
    }

    void getRoom(String username) {
        http.postRequest("https://zhny.sysu.edu.cn/kbp/admin/sys/personRoom/list", "{\"username\":\"" + username + "\"}", 1);
    }

    void getBalance(String room) {
        http.getRequest("https://zhny.sysu.edu.cn/kbp/pay/roomBalance?roomCode=" + room, 2);
    }

    void recharge(int amount, String room, String remark) {
        http.postRequest("https://zhny.sysu.edu.cn/kbp/pay/recharge/zdPay", String.format("{\"payAmount\":%s,\"body\":\"房间钱包充值\",\"rechargeChannel\":6,\"accountType\":7,\"rechargeType\":7,\"params\":{\"roomCode\":\"%s\"},\"remark\":\"%s\"}", amount, room, remark), 3);
    }

    void gotoWechat(JSONObject data) {
//        data.put("scene", "web");
        StringBuilder info = new StringBuilder();
        data.forEach((key, value) -> info.append(key).append("=").append(value).append("&"));
        new OkHttpClient.Builder().followRedirects(false).build().newCall(http.generateRequest("https://fee.sysu.edu.cn/gateway/unifiedorder/pagepay", info.toString(), "application/x-www-form-urlencoded").build()).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                http.sendFailure();
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                response.header("Location");
                if (!isEmpty(response.header("Location"))) {
                    Message message = new Message();
                    message.what = 4;
                    message.obj = response.header("Location");
                    http.getHandler().sendMessage(message);
                }
            }
        });
    }
}
