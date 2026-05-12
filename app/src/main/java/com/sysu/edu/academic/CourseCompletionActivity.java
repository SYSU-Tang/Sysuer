package com.sysu.edu.academic;

import static com.sysu.edu.api.CommonUtil.extractValue;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.MenuItem;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewbinding.ViewBinding;

import com.alibaba.fastjson2.JSONObject;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.tabs.TabLayoutMediator;
import com.sysu.edu.R;
import com.sysu.edu.api.HttpManager;
import com.sysu.edu.api.Params;
import com.sysu.edu.api.TargetUrl;
import com.sysu.edu.databinding.ActivityPagerBinding;
import com.sysu.edu.databinding.ItemCardBinding;
import com.sysu.edu.view.AdapterListener;
import com.sysu.edu.view.Pager2Adapter;
import com.sysu.edu.view.StaggeredFragment;

import java.util.List;
import java.util.Objects;

public class CourseCompletionActivity extends AppCompatActivity {

    HttpManager http;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityPagerBinding binding = ActivityPagerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.toolbar.setNavigationOnClickListener(_ -> supportFinishAfterTransition());
        Params params = new Params(this);
        params.setCallback(this::getCreditHours);
        binding.toolbar.setTitle(R.string.course_completion);
        StaggeredFragment page1 = StaggeredFragment.newInstance(0);
        Pager2Adapter pager2Adapter = new Pager2Adapter(this).add(page1).add(new CourseCompletionFragment());
        binding.pager.setAdapter(pager2Adapter);
        binding.toolbar.getMenu().add(R.string.export).setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_IF_ROOM).setIcon(R.drawable.export).setOnMenuItemClickListener(_ -> {
            int currentItem = binding.pager.getCurrentItem();
            ((StaggeredFragment) pager2Adapter.get(currentItem)).export(binding.toolbar, Objects.requireNonNull(Objects.requireNonNull(binding.tabs.getTabAt(currentItem)).getText()).toString());
            return true;
        });
        new TabLayoutMediator(binding.tabs, binding.pager, (tab, position) -> tab.setText(List.of("学分学时情况", "课程完成情况").get(position))).attach();
        http = new HttpManager(new Handler(getMainLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                if (msg.what == -1) {
                    params.toast(R.string.no_net_connected);
                } else {
                    JSONObject response = JSONObject.parseObject((String) msg.obj);
                    if (response != null && response.getInteger("code").equals(200)) {
                        if (response.get("data") != null) {
                            if (msg.what == 0) {
                                response.getJSONArray("data").forEach(a -> {
                                    JSONObject item = (JSONObject) a;
                                    page1.setListener(new AdapterListener() {
                                        @Override
                                        public void onBind(RecyclerView.Adapter<RecyclerView.ViewHolder> adapter, RecyclerView.ViewHolder holder, int position) {
                                            List<String> item = ((StaggeredFragment.StaggeredAdapter) adapter).getValues(position);
                                            LinearProgressIndicator progress = holder.itemView.findViewById(R.id.progress);
                                            progress.setMax((int) Float.parseFloat(item.get(3)));
                                            progress.setProgress((int) Float.parseFloat(item.get(4)));
                                        }

                                        @Override
                                        public void onCreate(RecyclerView.Adapter<RecyclerView.ViewHolder> adapter, ViewBinding binding) {
                                            LinearProgressIndicator progress = new LinearProgressIndicator(CourseCompletionActivity.this);
                                            progress.setId(R.id.progress);
                                            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
                                            lp.setMargins(params.dpToPx(12), params.dpToPx(6), params.dpToPx(12), params.dpToPx(12));
                                            progress.setLayoutParams(lp);
                                            ((ItemCardBinding) binding).getRoot().addView(progress);
                                        }
                                    });
                                    page1.add(item.getString("courseCategoryName"), List.of("课程类别", "培养方案学分要求", "免修课程学分", "实际毕业学分要求", "实得"),
                                            extractValue(item, new String[]{"courseCategoryName", "trainingCredit", "exemptCredit", "actualCredit", "earnedCredit"}));
                                });
                            }
                        }
                    } else if (response != null && response.getInteger("code").equals(50030000)) {
                        params.toast(response.getString("message"));
                    } else {
                        params.toast(R.string.login_warning);
                        params.gotoLogin(TargetUrl.JWXT);
                    }
                }
            }
        });
        http.setParams(params);
        http.setReferrer("https://jwxt.sysu.edu.cn/jwxt/mk/gradua/");
        getCreditHours();
    }

    void getCreditHours() {
        http.postRequest("https://jwxt.sysu.edu.cn/jwxt/gradua-degree/graduatemsg/studentsGraduationExamination/creditHoursStu?cultureTypeCode=01", "", 0);
    }
}