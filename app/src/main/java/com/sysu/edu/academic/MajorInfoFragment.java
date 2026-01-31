package com.sysu.edu.academic;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.alibaba.fastjson2.JSONObject;
import com.sysu.edu.R;
import com.sysu.edu.api.Params;
import com.sysu.edu.api.TargetUrl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MajorInfoFragment extends StaggeredFragment {

    final OkHttpClient http = new OkHttpClient.Builder().build();
    String cookie;
    Handler handler;
    int page = 0;
    int total = -1;
    String code;

    public static MajorInfoFragment newInstance(Bundle args) {
        MajorInfoFragment fragment = new MajorInfoFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        if (getArguments() != null) {
            code = getArguments().getString("code");
        }
        View view = super.onCreateView(inflater, container, savedInstanceState);
        Params params = new Params(requireActivity());
        params.setCallback(this, () -> {
            cookie = params.getCookie();
            page = 0;
            total = -1;
            getList();
        });
        cookie = params.getCookie();
        binding.recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView v, int dx, int dy) {
                if (!v.canScrollVertically(1) && total / 10 + 1 >= page) {
                    getList();
                }
                super.onScrolled(v, dx, dy);
            }
        });
        getList();
        handler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                if (msg.what == -1) {
                    Toast.makeText(requireContext(), getString(R.string.no_wifi_warning), Toast.LENGTH_LONG).show();
                } else {
                    JSONObject response = JSONObject.parseObject((String) msg.obj);
                    if (response != null && response.getInteger("code").equals(200)) {
                        if (response.get("data") != null) {
                            JSONObject data = response.getJSONObject("data");
                            switch (msg.what) {
                                case 0:
                                    if (total == -1) {
                                        total = data.getInteger("total");
                                    }
                                    data.getJSONArray("rows").forEach(a -> {
                                        ArrayList<String> values = new ArrayList<>();
                                        String[] keyName = new String[]{"专业代码", "专业名称", "学制", "修业年限", "学科门类", "学位授予门类"};
                                        for (int i = 0; i < keyName.length; i++) {
                                            values.add(((JSONObject) a).getString(new String[]{"code", "name", "educationalSystem", "maxStudyYear", "disciplineCateName", "degreeGrantName"}[i]));
                                        }
                                        add(values.get(1), List.of(keyName), values);
                                    });
                                    break;
                                case 1:
                                    break;
                            }
                        }
                    } else {
                        params.toast(R.string.login_warning);
                        params.gotoLogin(getView(), TargetUrl.JWXT);
                    }
                }
            }
        };
        return view;
    }

    void getList() {
        page++;
        http.newCall(new Request.Builder().url("https://jwxt.sysu.edu.cn/jwxt/base-info/profession-direction/list")
                .header("Cookie", cookie)
                .post(RequestBody.create(String.format(Locale.getDefault(), "{\"pageNo\":%d,\"pageSize\":10,\"total\":true,\"param\":{\"majorProfessionDircetion\":\"0\",\"disciplineCateCode\":\"%s\"}}", page, code), MediaType.parse("application/json")))
                .header("Referer", "https://jwxt.sysu.edu.cn/jwxt/mk/studentWeb/")
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
                msg.what = 0;
                msg.obj = response.body().string();
                handler.sendMessage(msg);
            }
        });
    }
}