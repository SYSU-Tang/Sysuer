package com.sysu.edu.life;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewbinding.ViewBinding;

import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONObject;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.sysu.edu.R;
import com.sysu.edu.academic.StaggeredFragment;
import com.sysu.edu.academic.StaggeredListener;
import com.sysu.edu.api.TargetUrl;
import com.sysu.edu.databinding.ItemCardBinding;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class NetOrderFragment extends StaggeredFragment {

    final OkHttpClient http = new OkHttpClient.Builder().build();
    Handler handler;
    View view;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (view == null) {
            view = super.onCreateView(inflater, container, savedInstanceState);
            params.setCallback(this, this::getInfo);

            handler = new Handler(Looper.getMainLooper()) {
                @Override
                public void handleMessage(@NonNull Message msg) {
                    super.handleMessage(msg);
                    if (msg.what == -1) {
                        params.toast(R.string.no_wifi_warning);
                    } else if (msg.what == 0 || msg.what == 1) {
                        try {
                            JSONObject json = JSONObject.parse((String) msg.obj);
                            if (!json.getBoolean("success")) {
                                params.toast(R.string.login_warning);
                                params.gotoLogin(getView(), TargetUrl.NETPAY);
                            }
                        } catch (JSONException ignored) {
                            Matcher matcher = Pattern.compile("<tr .+?>(.+?)</tr>", Pattern.DOTALL).matcher((String) msg.obj);
                            while (matcher.find()) {
                                ArrayList<String> orderDetail = new ArrayList<>();
                                String item = matcher.group(1);
                                if (item != null) {
                                    boolean isStop = false;
                                    Matcher matcher2 = Pattern.compile("<td .+?>(.+?)</td>", Pattern.DOTALL).matcher(item);
                                    while (matcher2.find()) {
                                        orderDetail.add(Objects.requireNonNull(matcher2.group(1)).replaceAll("<.+?>", "").trim());
                                    }
                                    if (requireArguments().getInt("code") == 1) {
                                        Matcher action = Pattern.compile("onclick='(.+?)\\((.+?)\\)'>(.+?)</a>").matcher(item);
                                        if (action.find()) {
                                            isStop = Objects.equals(action.group(1), "stop");
                                            final Matcher actionMatcher = Pattern.compile("(.+?),(.+?),").matcher((action.group(2) + ",").replace("\"", ""));
                                            if (actionMatcher.find()) {
                                                String leftDay = actionMatcher.group(2);
                                                orderDetail.set(9, leftDay);
                                                staggeredAdapter.setListener(new StaggeredListener() {

                                                    @Override
                                                    public void onBind(RecyclerView.Adapter<RecyclerView.ViewHolder> a, RecyclerView.ViewHolder holder, int position) {

                                                    }

                                                    @Override
                                                    public void onCreate(RecyclerView.Adapter<RecyclerView.ViewHolder> a, ViewBinding binding) {
                                                        MaterialButton button = new MaterialButton(requireContext(), null, com.google.android.material.R.attr.materialButtonTonalStyle);
                                                        button.setText(action.group(3));
                                                        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                                                        lp.gravity = Gravity.END;
                                                        lp.setMargins(0, 0, params.dpToPx(16), params.dpToPx(16));
                                                        button.setLayoutParams(lp);
                                                        boolean isStop = Objects.equals(action.group(1), "stop");
                                                        button.setOnClickListener(v -> {
                                                            if (leftDay != null) {
                                                                Snackbar.make(v, isStop ? "暂停网络将即时生效，暂停最小时长：7天。是否确定要暂停？" : Integer.parseInt(leftDay) < 7 ? "网络服务已暂停" + leftDay + "天，不足暂停最小时长（7天），提前恢复本次暂停作废，过期日期不顺延！是否仍要提前恢复网络？" : "网络服务已暂停" + leftDay + "天，执行恢复将即时生效，是否确定要恢复？", Snackbar.LENGTH_SHORT).setAction(R.string.confirm, v1 -> {
                                                                    if (isStop) {
                                                                        stop(actionMatcher.group(1));
                                                                    } else {
                                                                        resume(actionMatcher.group(1));
                                                                    }
                                                                }).show();
                                                            }
                                                        });
                                                        ((ItemCardBinding) binding).getRoot().addView(button);

                                                    }
                                                });
                                            }
                                        }
                                    }
                                    add(orderDetail.get(msg.what == 0 ? 4 : 0), msg.what == 0 ? List.of("订单号",
                                            "所有者",
                                            "金额",
                                            "支付方式",
                                            "订单时间",
                                            "订单状态",
                                            "服务",
                                            "代支付者") : List.of("序号",
                                            "服务",
                                            "地址",
                                            "MAC地址",
                                            "部门",
                                            "使用者",
                                            "状态",
                                            "过期日期",
                                            "暂停日期",
                                            isStop ? "暂停时长" : "剩余时长"), orderDetail);
                                }
                            }
                        }
                    } else if (msg.what == 2 || msg.what == 3) {
                        try {
//                            System.out.println(msg.obj);
                            /*JSONObject json = JSONObject.parse((String) msg.obj);
                            if (json.getBoolean("success")) {
                                params.toast(json.getString("message"));
                            }*/
                            clear();
                            getInfo();
                        } catch (JSONException ignored) {
                        }
                    }
                }
            };
            getInfo();
        }
        return view;
    }

    void getOrder() {
        postRequest("https://netpay.sysu.edu.cn/netpay/c/site/orders", "", 0);
    }

    void getNet() {
        postRequest("https://netpay.sysu.edu.cn/netpay/c/site/stopAndResumeList", "personal=1", 1);
    }

    void getInfo() {
        new Runnable[]{this::getOrder, this::getNet}[requireArguments().getInt("code")].run();
    }

    void stop(String serviceId) {
        postRequest("https://netpay.sysu.edu.cn/netpay/c/site/stop", "serviceId=" + serviceId, 2);
    }

    void resume(String serviceId) {
        postRequest("https://netpay.sysu.edu.cn/netpay/c/site/resume", "serviceId=" + serviceId, 3);
    }

    void postRequest(String url, String data, int what) {
        http.newCall(new Request.Builder()
                .url(url)
                .header("Cookie", params.getCookie())
                .post(RequestBody.create(data, MediaType.parse("application/x-www-form-urlencoded")))
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
}
