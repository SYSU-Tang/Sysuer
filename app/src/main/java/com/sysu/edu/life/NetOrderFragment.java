package com.sysu.edu.life;

import static android.text.TextUtils.isEmpty;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewbinding.ViewBinding;

import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONObject;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonGroup;
import com.google.android.material.snackbar.Snackbar;
import com.sysu.edu.R;
import com.sysu.edu.api.HttpManager;
import com.sysu.edu.api.TargetUrl;
import com.sysu.edu.databinding.DialogNetPayBinding;
import com.sysu.edu.databinding.ItemButtonGroupBinding;
import com.sysu.edu.databinding.ItemButtonOutlineBinding;
import com.sysu.edu.databinding.ItemCardBinding;
import com.sysu.edu.view.AdapterListener;
import com.sysu.edu.view.StaggeredFragment;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Response;

public class NetOrderFragment extends StaggeredFragment {

    HttpManager http;
    LocalDate oldDate;
    int fee;
    Number time;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-M-d");
        View view = super.onCreateView(inflater, container, savedInstanceState);
        params.setCallback(this::getInfo);
        DialogNetPayBinding dialogNetBinding = DialogNetPayBinding.inflate(inflater);
        dialogNetBinding.service.key.setText(R.string.service);
        dialogNetBinding.oldOutDate.key.setText(R.string.old_out_date);
        dialogNetBinding.newOutDate.key.setText(R.string.new_out_date);
        dialogNetBinding.fee.key.setText(R.string.fee);
        dialogNetBinding.time.key.setText(R.string.time);
        dialogNetBinding.time.value.setText(R.string.click_to_select);
        PopupMenu popupMenu = new PopupMenu(requireActivity(), dialogNetBinding.time.value, 0, 0, com.google.android.material.R.style.Widget_Material3_PopupMenu_Overflow);
        Menu menu = popupMenu.getMenu();
        dialogNetBinding.time.getRoot().setOnClickListener(_ -> popupMenu.show());
        String[] strings = new String[]{"15天", "1个月", "2个月", "3个月", "4个月", "5个月", "6个月", "7个月", "8个月", "9个月", "10个月", "11个月", "1年", "2年"};
        long[] months = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 24, 48};
        int[] fees = {15, 30, 60, 90, 120, 150, 180, 210, 240, 270, 300, 330, 300, 600};
        for (int i = 0; i < strings.length; i++) {
            int finalI = i;
            menu.add(0, 0, 0, strings[i]).setOnMenuItemClickListener(_ -> {
                dialogNetBinding.time.value.setText(strings[finalI]);
                dialogNetBinding.newOutDate.value.setText((finalI == 0 ? oldDate.plusDays(15) : oldDate.plusMonths(months[finalI])).format(formatter));
                time = finalI == 0 ? 0.5 : months[finalI];
                fee = fees[finalI];
                dialogNetBinding.fee.value.setText(String.format(Locale.getDefault(), "%d元", fee));
                popupMenu.dismiss();
                return true;
            });
        }
        BottomSheetDialog payDialog = new BottomSheetDialog(requireActivity());
        payDialog.setContentView(dialogNetBinding.getRoot());
        http = new HttpManager(new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                super.handleMessage(msg);
                String response = (String) msg.obj;
                switch (msg.what) {
                    case -1 -> params.toast(R.string.no_net_connected);
                    case 5 -> {
                        params.copy("recharge", (String) msg.obj);
                        Intent intent = Intent.createChooser(new Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, (String) msg.obj).putExtra(Intent.EXTRA_SUBJECT, getString(R.string.recharge)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), getString(R.string.share));
                        if (intent.resolveActivity(requireContext().getPackageManager()) != null)
                            startActivity(intent);
                    }
                    case 0, 1, 6 -> {
                        staggeredAdapter.clear();
                        try {
                            JSONObject json = JSONObject.parse(response);
                            if (!json.getBoolean("success")) {
                                params.toast(R.string.login_warning);
                                params.gotoLogin(TargetUrl.NETPAY);
                            }
                        } catch (JSONException _) {
                            Matcher matcher = Pattern.compile("<tr .*?>(.+?)</tr>", Pattern.DOTALL).matcher(response);
                            while (matcher.find()) {
                                ArrayList<String> orderDetail = new ArrayList<>();
                                String item = matcher.group(1);
                                if (item != null) {
                                    ArrayList<String> keys = new ArrayList<>(List.of("序号",
                                            "服务",
                                            "地址",
                                            "MAC地址",
                                            "部门",
                                            "使用者",
                                            "状态",
                                            "过期日期",
                                            "暂停日期"));
                                    boolean isStop;
                                    Matcher matcher2 = Pattern.compile("<td .*?>(.+?)</td>", Pattern.DOTALL).matcher(item);
                                    while (matcher2.find())
                                        orderDetail.add(Objects.requireNonNull(matcher2.group(1)).replaceAll("<.+?>", "").trim());
                                    if (requireArguments().getInt("code") == 1) {
                                        if (msg.what == 1) {
                                            Matcher action = Pattern.compile("onclick='(.+?)\\((.+?)\\)'>(.+?)</a>").matcher(item);
                                            if (action.find()) {
                                                isStop = Objects.equals(action.group(1), "stop");
                                                final Matcher actionMatcher = Pattern.compile("(.+?),(.+?),").matcher((action.group(2) + ",").replace("\"", ""));
                                                if (actionMatcher.find()) {
                                                    staggeredAdapter.setListener(new AdapterListener() {

                                                        @Override
                                                        public void onBind(RecyclerView.Adapter<RecyclerView.ViewHolder> adapter, RecyclerView.ViewHolder holder, int position) {
                                                        }

                                                        @Override
                                                        public void onCreate(RecyclerView.Adapter<RecyclerView.ViewHolder> adapter, ViewBinding b) {
                                                            MaterialButtonGroup line = ItemButtonGroupBinding.inflate(inflater, ((ItemCardBinding) b).getRoot(), false).getRoot();
                                                            String leftDay = actionMatcher.group(2);
                                                            orderDetail.set(9, leftDay);
                                                            String serviceId = actionMatcher.group(1);
                                                            boolean isStop = Objects.equals(action.group(1), "stop");
                                                            getMaterialButton(inflater, line, action.group(3), v -> {
                                                                if (leftDay != null) {
                                                                    Snackbar.make(v, isStop ? "暂停网络将即时生效，暂停最小时长：7天。是否确定要暂停？" : Integer.parseInt(leftDay) < 7 ? "网络服务已暂停" + leftDay + "天，不足暂停最小时长（7天），提前恢复本次暂停作废，过期日期不顺延！是否仍要提前恢复网络？" : "网络服务已暂停" + leftDay + "天，执行恢复将即时生效，是否确定要恢复？", Snackbar.LENGTH_SHORT).setAction(R.string.confirm, _ -> {
                                                                        if (isStop) stop(serviceId);
                                                                        else resume(serviceId);
                                                                    }).show();
                                                                }
                                                            });
                                                            getMaterialButton(inflater, line, getString(R.string.pay), _ -> {
                                                                payDialog.show();
                                                                oldDate = LocalDate.parse(orderDetail.get(7), formatter);
                                                                dialogNetBinding.oldOutDate.value.setText(orderDetail.get(7));
                                                                dialogNetBinding.service.value.setText(orderDetail.get(1));
                                                                dialogNetBinding.submit.setOnClickListener(_ -> order(time, fee, serviceId));
                                                            });
                                                            ((ItemCardBinding) b).getRoot().addView(line);
                                                        }
                                                    });
                                                }
                                            } else {
                                                getServiceId();
                                                break;
                                            }
                                            keys.add(isStop ? getString(R.string.left_time) : getString(R.string.pause_time));
                                        } else {
                                            Matcher action = Pattern.compile("<select .*? s='(.*?)'").matcher(item);
                                            if (action.find()) {
                                                String serviceId = action.group(1);
                                                staggeredAdapter.setListener(new AdapterListener() {

                                                    @Override
                                                    public void onBind(RecyclerView.Adapter<RecyclerView.ViewHolder> adapter, RecyclerView.ViewHolder holder, int position) {
                                                    }

                                                    @Override
                                                    public void onCreate(RecyclerView.Adapter<RecyclerView.ViewHolder> adapter, ViewBinding b) {
                                                        MaterialButtonGroup line = ItemButtonGroupBinding.inflate(inflater, ((ItemCardBinding) b).getRoot(), false).getRoot();
                                                        getMaterialButton(inflater, line, getString(R.string.pay), _ -> {
                                                            payDialog.show();
                                                            oldDate = LocalDate.parse(orderDetail.get(8), formatter);
                                                            dialogNetBinding.oldOutDate.value.setText(orderDetail.get(8));
                                                            dialogNetBinding.service.value.setText(orderDetail.get(1));
                                                            dialogNetBinding.submit.setOnClickListener(_ -> order(time, fee, serviceId));
                                                        });
                                                        ((ItemCardBinding) b).getRoot().addView(line);
                                                    }
                                                });
                                            }
                                        }
                                    }
                                    add(orderDetail.get(msg.what == 0 ? 4 : 0), msg.what == 0 ? List.of(
                                            "订单号",
                                            "所有者",
                                            "金额",
                                            "支付方式",
                                            "订单时间",
                                            "订单状态",
                                            "服务",
                                            "代支付者") : keys, orderDetail);

                                }
                            }
                        }
                    }
                    case 2, 3 -> {
                        try {
                            clear();
                            getInfo();
                        } catch (JSONException _) {
                        }
                    }
                    case 4 -> {
                        try {
                            System.out.println(response);
                            JSONObject json = JSONObject.parse(response);
                            if (json.getBoolean("success")) {
                                params.toast(R.string.order_success);
                                clear();
                                getInfo();
                            } else {
                                params.toast(R.string.order_fail);
                            }
                        } catch (JSONException _) {
                            params.toast(R.string.order_success);
                            JSONObject data = new JSONObject();
                            Matcher matcher = Pattern.compile("<input.*?name='(.*?)' value='(.*?)'/>", Pattern.DOTALL).matcher(response);
                            while (matcher.find()) data.put(matcher.group(1), matcher.group(2));
                            gotoWechat(data);
                            clear();
                            getInfo();
                        }
                    }
                }
            }
        });
        http.setHeader(Map.of("accept-language", "zh-CN,zh;q=0.9,en-US;q=0.8,en;q=0.7"));
        http.setParams(params);
        getInfo();
        return view;
    }

    private void getMaterialButton(LayoutInflater inflater, MaterialButtonGroup parent, String fun, View.OnClickListener onClick) {
        MaterialButton button = ItemButtonOutlineBinding.inflate(inflater, parent, false).getRoot();
        button.setText(fun);
        MaterialButtonGroup.LayoutParams lp = new MaterialButtonGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, params.dpToPx(16), params.dpToPx(16));

        button.setLayoutParams(lp);
        button.setOnClickListener(onClick);
        parent.addView(button);
    }

    void getOrder() {
        http.postRequest("https://netpay.sysu.edu.cn/netpay/c/site/orders", "", 0);
    }

    void getNet() {
        http.postRequest("https://netpay.sysu.edu.cn/netpay/c/site/stopAndResumeList", "personal=1", "application/x-www-form-urlencoded", 1);
    }

    void getInfo() {
        new Runnable[]{this::getOrder, this::getNet}[requireArguments().getInt("code")].run();
    }

    void stop(String serviceId) {
        http.postRequest("https://netpay.sysu.edu.cn/netpay/c/site/stop", "serviceId=" + serviceId, "application/x-www-form-urlencoded", 2);
    }

    void resume(String serviceId) {
        http.postRequest("https://netpay.sysu.edu.cn/netpay/c/site/resume", "serviceId=" + serviceId, "application/x-www-form-urlencoded", 3);
    }

    void order(Number time, int fee, String serviceId) {
        http.postRequest("https://netpay.sysu.edu.cn/netpay/c/site/prepareOrder", String.format(Locale.getDefault(), "type=web&months=%s&moneys=%d&serviceIds=%s", time.floatValue() < 1 ? time.floatValue() : time.intValue(), fee, serviceId), "application/x-www-form-urlencoded", 4);
    }

    void gotoWechat(JSONObject data) {
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
                    message.what = 5;
                    message.obj = response.header("Location");
                    http.getHandler().sendMessage(message);
                }
            }
        });
    }

    void getServiceId() {
        http.postRequest("https://netpay.sysu.edu.cn/netpay/c/site/bills", "personal=1", "application/x-www-form-urlencoded", 6);
    }
}
