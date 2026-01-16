package com.sysu.edu.academic;

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
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewbinding.ViewBinding;

import com.alibaba.fastjson2.JSONObject;
import com.google.android.material.button.MaterialButton;
import com.sysu.edu.R;
import com.sysu.edu.databinding.ItemCardBinding;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class LeaveReturnListFragment extends StaggeredFragment {

    View view;
    Handler handler;
    OkHttpClient http = new OkHttpClient();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (view == null) {
            view = super.onCreateView(inflater, container, savedInstanceState);
        }
        LeaveReturnRegistrationViewModel viewModel = new ViewModelProvider(requireActivity()).get(LeaveReturnRegistrationViewModel.class);

        viewModel.year.observe(getViewLifecycleOwner(), this::getList);
        handler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                if (msg.what == -1) {
                    params.toast(R.string.no_wifi_warning);
                } else if (msg.what == 0) {
                    int code = msg.getData().getInt("code");
                    if (code == 200) {
                        JSONObject json = JSONObject.parse(msg.getData().getString("response"));
                        if (json != null && json.getInteger("code") == 200) {
                            clear();
                            json.getJSONArray("data").forEach(e -> {
                                ArrayList<String> value = new ArrayList<>();
                                for (String i : new String[]{"blxn", "lxdjsj", "gzsm","jjrmc", "jjrrq", "gzzt","zt"})
                                    value.add(((JSONObject) e).getString(i));

                                add(((JSONObject) e).getString("gzmc"), ((JSONObject) e).getInteger("gzztm") == 1 ? R.drawable.uncheck : R.drawable.check, List.of("办理学年", "离校登记时间", "工作说明", "节假日名称", "节假日日期", "工作状态","状态"), value);

                            });
                            staggeredAdapter.setListener(new StaggeredListener() {
                                @Override
                                public void onBind(RecyclerView.Adapter<RecyclerView.ViewHolder> a, RecyclerView.ViewHolder holder, int position) {
                                    boolean isRegistering = json.getJSONArray("data").getJSONObject(position).getInteger("gzztm") == 1;
                                    String status = json.getJSONArray("data").getJSONObject(position).getString("zt");
                                    MaterialButton button = holder.itemView.findViewById(R.id.button);
                                    button.setText(isRegistering ? status.equals("registering")? "开始登记":"修改登记" : "查看详情");
                                    button.setOnClickListener(v -> {
//                                        if (isRegistering) {
                                            Bundle arg = new Bundle();
                                            arg.putString("Id", json.getJSONArray("data").getJSONObject(position).getString("cjlfxgzId"));

                                            requireActivity().getSupportFragmentManager()
                                                    .beginTransaction()
                                                    .replace(R.id.leave_return_list_fragment, LeaveReturnRegistrationFragment.class, arg)
                                                    .addToBackStack(null)
                                                    .commit();
//                                        }
                                    });
                                }

                                @Override
                                public void onCreate(RecyclerView.Adapter<RecyclerView.ViewHolder> a, ViewBinding binding) {
                                    MaterialButton button = new MaterialButton(requireContext(), null, com.google.android.material.R.attr.materialButtonTonalStyle);
                                    button.setId(R.id.button);
                                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                                    lp.gravity = Gravity.END;
                                    lp.setMargins(0, 0, params.dpToPx(16), params.dpToPx(16));
                                    button.setLayoutParams(lp);
                                    ((ItemCardBinding) binding).getRoot().addView(button);
                                }
                            });
                        } else if (json != null) {
                            params.toast(json.getString("msg"));
                        }
                    } else {
                        params.toast(R.string.educational_wifi_warning);
                    }
                }
            }
        };
        return view;
    }

    void getList(String year) {
        sendRequest("https://xgxt-443.webvpn.sysu.edu.cn/jjrlfx/api/sm-jjrlfx/student/work-list?blxn=" + year, 0);
    }

    void sendRequest(String url, int what) {
        http.newCall(new Request.Builder().url(url)
                .header("Cookie", params.getCookie())
                .build()).enqueue(new Callback() {

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                Message msg = new Message();
                msg.what = what;
                Bundle bundle = new Bundle();
                bundle.putInt("code", response.code());
                bundle.putString("response", response.body().string());
                msg.setData(bundle);
                handler.sendMessage(msg);
            }

            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Message msg = new Message();
                msg.what = -1;
                handler.sendMessage(msg);
            }
        });
    }

}
