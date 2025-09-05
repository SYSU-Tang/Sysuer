package com.sysu.edu.academic;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.widget.LinearLayout;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewbinding.ViewBinding;

import com.alibaba.fastjson2.JSONObject;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.tabs.TabLayoutMediator;
import com.sysu.edu.R;
import com.sysu.edu.api.Params;
import com.sysu.edu.databinding.ActivityPagerBinding;
import com.sysu.edu.databinding.CardItemBinding;
import com.sysu.edu.extra.LoginActivity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class CourseCompletion extends AppCompatActivity {

    ActivityPagerBinding binding;
    String cookie;
    Handler handler;
    OkHttpClient http = new OkHttpClient.Builder().build();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPagerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.toolbar.setNavigationOnClickListener(v -> supportFinishAfterTransition());
        Params params = new Params(this);
        cookie = params.getCookie();
        ActivityResultLauncher<Intent> launch = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), o -> {
            if (o.getResultCode() == Activity.RESULT_OK) {
                cookie = params.getCookie();
                getCreditHours();
            }
        });
        binding.toolbar.setTitle(R.string.course_completion);
        Pager2Adapter adp = new Pager2Adapter(this).add(StaggeredFragment.newInstance(0)).add(new CourseCompletionFragment());
        binding.pager.setAdapter(adp);
        new TabLayoutMediator(binding.tabs, binding.pager, (tab, position) -> tab.setText(List.of("学分学时情况", "课程完成情况").get(position))).attach();
        handler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                if (msg.what == -1) {
                    params.toast(R.string.no_wifi_warning);
                } else {
                    JSONObject response = JSONObject.parseObject((String) msg.obj);
                    if (response != null && response.getInteger("code").equals(200)) {
                        if (response.get("data") != null) {
                            if (msg.what == 0) {
                                response.getJSONArray("data").forEach(a -> {
                                    ArrayList<String> values = new ArrayList<>();
                                    for (String key : new String[]{"courseCategoryName", "trainingCredit", "exemptCredit", "actualCredit", "earnedCredit"}) {
                                        values.add(((JSONObject) a).getString(key));
                                    }
                                    ((StaggeredFragment) adp.getItem(0)).staggeredAdapter.setListener(new StaggeredListener() {
                                        @Override
                                        public void onBind(StaggeredFragment.StaggeredAdapter a, RecyclerView.ViewHolder holder, int position) {
                                            LinearProgressIndicator progress = holder.itemView.findViewById(R.id.progress);
                                            progress.setMax((int) Float.parseFloat(a.values.get(position).get(3)));
                                            progress.setProgress((int) Float.parseFloat(a.values.get(position).get(4)));
                                        }

                                        @Override
                                        public void onCreate(StaggeredFragment.StaggeredAdapter a, ViewBinding binding) {
                                            LinearProgressIndicator progress = new LinearProgressIndicator(CourseCompletion.this);
                                            progress.setId(R.id.progress);
                                            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
                                            lp.setMargins(params.dpToPx(12), params.dpToPx(6), params.dpToPx(12), params.dpToPx(12));
                                            progress.setLayoutParams(lp);
                                            ((CardItemBinding) binding).getRoot().addView(progress);
                                        }
                                    });
                                    ((StaggeredFragment) adp.getItem(0)).add(CourseCompletion.this, values.get(0), List.of("课程类别", "培养方案学分要求", "免修课程学分", "实际毕业学分要求", "实得"), values);
                                });
                            }
                        }
                    } else if (response != null && response.getInteger("code").equals(50030000)) {
                        params.toast(response.getString("message"));
                    } else {
                        params.toast(R.string.login_warning);
                        launch.launch(new Intent(CourseCompletion.this, LoginActivity.class));
                    }
                }
            }
        };
        getCreditHours();
    }

    void getCreditHours() {
        http.newCall(new Request.Builder().url("https://jwxt.sysu.edu.cn/jwxt/gradua-degree/graduatemsg/studentsGraduationExamination/creditHoursStu?cultureTypeCode=01")
                .header("Cookie", cookie)
                .post(RequestBody.create("", null))
                .header("Referer", "https://jwxt.sysu.edu.cn/jwxt/mk/gradua/")
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