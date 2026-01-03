package com.sysu.edu.academic;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.alibaba.fastjson2.JSONObject;
import com.google.android.material.tabs.TabLayoutMediator;
import com.sysu.edu.R;
import com.sysu.edu.api.Params;
import com.sysu.edu.api.TargetUrl;
import com.sysu.edu.databinding.ActivityCourseDetailBinding;

import java.io.IOException;
import java.util.ArrayList;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class CourseDetail extends AppCompatActivity {

    ActivityCourseDetailBinding binding;
    final OkHttpClient http = new OkHttpClient.Builder().build();
    Handler handler;
    String cookie;
    String code;
    String id;
    String classNum;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCourseDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.toolbar.setNavigationOnClickListener(v -> supportFinishAfterTransition());
        CourseDetailPageAdapter courseDetailPageAdapter = new CourseDetailPageAdapter(this);
        Params params = new Params(this);
        params.setCallback(() -> {
            cookie = this.getSharedPreferences("privacy", Context.MODE_PRIVATE).getString("Cookie", "");
            getCourseOutline();
        });
        binding.pager.setAdapter(courseDetailPageAdapter);
        courseDetailPageAdapter.add(new CourseDetailFragment());
        courseDetailPageAdapter.add(new CourseOutlineFragment());
        new TabLayoutMediator(binding.tabs, binding.pager, (tab, i) -> tab.setText(new String[]{getString(R.string.course_detail), getString(R.string.course_draft)}[i])).attach();
        code = getIntent().getStringExtra("code");
        id = getIntent().getStringExtra("id");
        classNum = getIntent().getStringExtra("class");
        // code: EIT228, id: null, classNum: 202511441
        cookie = this.getSharedPreferences("privacy", Context.MODE_PRIVATE).getString("Cookie", "");
        handler = new Handler(getMainLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                JSONObject response = JSONObject.parseObject((String) msg.obj);
                //System.out.println(response);
                if (response.getInteger("code").equals(200)) {
                    switch (msg.what) {
                        case -1:
                            Toast.makeText(CourseDetail.this, getString(R.string.no_wifi_warning), Toast.LENGTH_LONG).show();
                            break;
                        case 1:
                            JSONObject data = response.getJSONObject("data");
                            if (data != null) {
                                Bundle bundle = new Bundle();
                                bundle.putInt("what", 1);
                                bundle.putString("data", data.getJSONObject("outlineInfo").toJSONString());
                                courseDetailPageAdapter.getPage(0).setArguments(bundle);
                                Bundle bundle2 = new Bundle();
                                bundle2.putString("data", data.getJSONArray("scheduleList").toJSONString());
                                courseDetailPageAdapter.getPage(1).setArguments(bundle2);
                            }
                            if (data != null) {
                                id = data.getJSONObject("outlineInfo").getString("courseId");
                            }
                            getCourseOutline2();
                            break;
                        case 2:
                            JSONObject data2 = response.getJSONObject("data");
                            if (data2 != null) {
                                Bundle bundle = new Bundle();
                                bundle.putInt("what", 2);
                                bundle.putString("data", data2.toString());
                                courseDetailPageAdapter.getPage(0).setArguments(bundle);
                            }

                            break;
//                        case 3:
//                            id=response.getJSONObject("data").getJSONArray("rows").getJSONObject(0).getString("courseId");
//                            getCourseOutline();
//                            break;
                    }
                } else if (response.getInteger("code").equals(52000000)) {
                    binding.pager.setVisibility(View.GONE);
                    params.toast(response.getString("message"));
                } else if (response.getInteger("code").equals(53000007)) {
                    params.toast(R.string.login_warning);
                    params.gotoLogin(binding.toolbar, TargetUrl.JWXT);
                } else {
                    params.toast(response.getString("message"));
                }
                super.handleMessage(msg);
            }
        };
        if (code == null || classNum == null) {
            getCourseOutline2();
        } else {
            getCourseOutline();
        }
//        if(id==null) {
//
//        }
//        else{
//            //getId();
//        }
    }
//    void getId() {
//        http.newCall(new Request.Builder().url("https://jwxt.sysu.edu.cn/jwxt/schedule/agg/schoolOpeningCoursesSchedule/querySchoolOpeningCourses")
//                .header("Cookie",cookie)
//                .post(RequestBody.create(String.format("{\"pageNo\":1,\"pageSize\":10,\"total\":true,\"param\":{\"classNumber\":\"%s\"}}",classNum), MediaType.parse("application/json")))
//                .header("Referer","https://jwxt.sysu.edu.cn/jwxt/mk/")
//                .build()).enqueue(new Callback() {
//            @Override
//            public void onFailure(@NonNull Call call, @NonNull IOException e) {
//                Message msg = new Message();
//                msg.what=-1;
//                handler.sendMessage(msg);
//            }
//
//            @Override
//            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
//                Message msg = new Message();
//                msg.what=3;
//                msg.obj=response.body().string();
//                handler.sendMessage(msg);
//            }
//        });
//    }


    void getCourseOutline() {
        http.newCall(new Request.Builder().url(String.format("https://jwxt.sysu.edu.cn/jwxt/training-programe/courseoutline/getalloutlineinfo?courseNum=%s&auditStatus=99", code))
                .header("Cookie", cookie)
                .header("Referer", "https://jwxt.sysu.edu.cn/jwxt/mk/courseSelection/?code=jwxsd_xk&resourceName=%25E9%2580%2589%25E8%25AF%25BE")
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
                msg.what = 1;
                msg.obj = response.body().string();
                handler.sendMessage(msg);
            }
        });
    }

    void getCourseOutline2() {
        http.newCall(new Request.Builder().url(String.format("https://jwxt.sysu.edu.cn/jwxt/base-info/courseLibrary/findById?id=%s", id))
                .header("Cookie", cookie)
                .header("Referer", "https://jwxt.sysu.edu.cn/jwxt/mk/courseSelection/?code=jwxsd_xk&resourceName=%25E9%2580%2589%25E8%25AF%25BE")
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
                msg.what = 2;
                msg.obj = response.body().string();
                handler.sendMessage(msg);
            }
        });
    }
}

class CourseDetailPageAdapter extends FragmentStateAdapter {
    final ArrayList<Fragment> fragments = new ArrayList<>();
    final FragmentActivity activity;

    public CourseDetailPageAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
        this.activity = fragmentActivity;
    }

    void add(Fragment fragment) {
        fragments.add(fragment);
        notifyItemInserted(getItemCount());
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        return fragments.get(position);
    }

    Fragment getPage(int i) {
        return fragments.get(i);
    }

    @Override
    public int getItemCount() {
        return fragments.size();
    }
}