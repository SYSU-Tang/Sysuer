package com.sysu.edu.academic;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.google.android.material.tabs.TabLayoutMediator;
import com.sysu.edu.R;
import com.sysu.edu.api.Params;
import com.sysu.edu.api.TargetUrl;
import com.sysu.edu.databinding.ActivityPagerBinding;
import com.sysu.edu.extra.LoginActivity;

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

public class RegisterInfo extends AppCompatActivity {

    ActivityPagerBinding binding;
    Params params;
    String cookie;
    Handler handler;
    OkHttpClient http = new OkHttpClient.Builder().build();
    int page = 1;
    Pager2Adapter adp;
    boolean changeYear=false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPagerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        params = new Params(this);
        cookie = params.getCookie();
        ActivityResultLauncher<Intent> launch = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), o -> {
            if (o.getResultCode() == Activity.RESULT_OK) {
                cookie = params.getCookie();
                getNextPage(0);
            }
        });
        adp = new Pager2Adapter(this);
        binding.pager.setAdapter(adp);
        for(String i : new String[]{"2024","2025"}){
            binding.toolbar.getMenu().add(i).setOnMenuItemClickListener(menuItem -> {
                getPay(i);
                ((StaggeredFragment)adp.getItem(1)).clear();
                return false;
            });
        }
        new TabLayoutMediator(binding.tabs, binding.pager, (tab, position) -> tab.setText(new String[]{"今年注册记录","缴费信息","往年注册记录"}[position])).attach();
        binding.toolbar.setNavigationOnClickListener(v->supportFinishAfterTransition());
        handler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                if (msg.what == -1) {
                    Toast.makeText(RegisterInfo.this, getString(R.string.no_wifi_warning), Toast.LENGTH_LONG).show();
                }else {
                    JSONObject response = JSONObject.parseObject((String) msg.obj);
                    if (response != null && response.getInteger("code").equals(200)) {
                        if (response.get("data") != null) {
                            switch (msg.what) {
                                case 2: {
                                    JSONObject d = response.getJSONObject("data");
                                    int total = d.getInteger("total");
                                    d.getJSONArray("rows").forEach(a -> {
                                        ArrayList<String> values = new ArrayList<>();
                                        String[] keyName = new String[]{"学年学期","校区","学院","年级专业","缴费状态","报到状态","注册状态","报到日期","注册日期"};
                                        for (int i = 0; i < keyName.length; i++) {
                                            values.add(((JSONObject) a).getString(new String[]{"academicYearTerm","campusName","collegeName","gradeMajorName","payedStatusName","checkInStatusName","registerStatusName","checkInDate","registerDate","","","",""}[i]));
                                        }
                                        ((StaggeredFragment) adp.getItem(2)).add(RegisterInfo.this,values.get(0),R.drawable.calendar,List.of(keyName), values);

                                    });
                                    if (total / 10 > page - 1) {
                                        page++;
                                        getList();
                                    } else {
                                        getNextPage(msg.what + 1);
                                    }
                                    break;
                                }
                                case 0: {
                                    JSONObject d = response.getJSONObject("data");
                                    ArrayList<String> values = new ArrayList<>();
                                    String[] keyName = new String[]{"学号", "注册学年学期", "报到状态", "注册状态", "缴费状态"};
                                    for (int i = 0; i < keyName.length; i++) {
                                        values.add(d.getString(new String[]{"stuNum", "academicYearTerm", "checkInStatusName", "registerStatusName", "payedStatusName"}[i]));
                                    }
                                    ((StaggeredFragment) adp.getItem(0)).add(RegisterInfo.this,"学生报到信息", List.of(keyName), values);
                                    getNextPage(msg.what + 1);
                                    break;
                                }
                                case 1: {
                                    JSONArray d = response.getJSONArray("data");
                                    d.forEach(v -> {
                                        ArrayList<String> values = new ArrayList<>();
                                        String[] keyName = new String[]{"年份", "类别","项目名称" ,"金额（元）", "区间", "时间"};
                                        for (int i = 0; i < keyName.length; i++) {
                                            values.add(((JSONObject) v).getString(new String[]{"acadYear", "typeName", "feeTypeName", "payedItemAmount", "feeTimeSection", "editeTime"}[i]));
                                        }
                                        ((StaggeredFragment) adp.getItem(1)).setHideNull(RegisterInfo.this,true);
                                        ((StaggeredFragment) adp.getItem(1)).add(RegisterInfo.this,values.get(1),R.drawable.money, List.of(keyName), values);
                                    });
                                    getNextPage(msg.what + 1);
                                    break;
                                }
                            }
                        }
                    }
                    else {
                        Toast.makeText(RegisterInfo.this, getString(R.string.login_warning), Toast.LENGTH_LONG).show();
                        launch.launch(new Intent(RegisterInfo.this, LoginActivity.class).putExtra("url", TargetUrl.JWXT));
                    }
                }
            }
        };
        getNextPage(0);
    }
    void getNextPage(int what){
        if(changeYear){
            return;
        }
        if(what<3){
            adp.add(StaggeredFragment.newInstance(what));
        }
        switch (what){
            case 0:
                getInfo();
                break;
            case 1:
                getPay();
                break;
            case 2:
                getList();
                break;
            default:changeYear=true;
        }
    }
    void getInfo(){
        http.newCall(new Request.Builder().url("https://jwxt.sysu.edu.cn/jwxt/reports-register/stuRegistration/getSelfRegisterInfo")
                .header("Cookie",cookie)
                .header("Referer","https://jwxt.sysu.edu.cn/jwxt/mk/studentWeb/")
                .build()).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Message msg = new Message();
                msg.what=-1;
                handler.sendMessage(msg);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                Message msg = new Message();
                msg.what= 0;
                msg.obj=response.body().string();
                handler.sendMessage(msg);
            }
        });
    }
    void getPay(){
        getPay("2025");
    }
    void getPay(String year){
        http.newCall(new Request.Builder().url("https://jwxt.sysu.edu.cn/jwxt/reports-register/stuRegistration/getSelfPayInfoDetail?acadYear="+year)
                .header("Cookie",cookie)
                .header("Referer","https://jwxt.sysu.edu.cn/jwxt/mk/studentWeb/")
                .build()).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Message msg = new Message();
                msg.what=-1;
                handler.sendMessage(msg);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                Message msg = new Message();
                msg.what= 1;
                msg.obj=response.body().string();
                handler.sendMessage(msg);
            }
        });
    }
    void getList(){
        http.newCall(new Request.Builder().url("https://jwxt.sysu.edu.cn/jwxt/reports-register/stuRegistration/getSelfRegisterList")
                .header("Cookie",cookie)
                .post(RequestBody.create(String.format(Locale.CHINA,"{\"pageNo\":%d,\"pageSize\":10,\"total\":true,\"param\":{}}",page), MediaType.parse("application/json")))
                .header("Referer","https://jwxt.sysu.edu.cn/jwxt/mk/studentWeb/")
                .build()).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Message msg = new Message();
                msg.what=-1;
                handler.sendMessage(msg);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                Message msg = new Message();
                msg.what= 2;
                msg.obj=response.body().string();
                handler.sendMessage(msg);
            }
        });
    }
}