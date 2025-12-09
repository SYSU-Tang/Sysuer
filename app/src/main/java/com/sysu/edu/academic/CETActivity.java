package com.sysu.edu.academic;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.alibaba.fastjson2.JSONObject;
import com.sysu.edu.R;
import com.sysu.edu.api.Params;
import com.sysu.edu.databinding.ActivityListBinding;
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

public class CETActivity extends AppCompatActivity {

    ActivityListBinding binding;
    Params params;
    String cookie;
    Handler handler;
    int order=0;
    OkHttpClient http = new OkHttpClient.Builder().build();
    int page;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        page=0;
        binding = ActivityListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        params = new Params(this);
        cookie = params.getCookie();
        StaggeredFragment fr = binding.list.getFragment();
        ActivityResultLauncher<Intent> launch = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), o -> {
            if (o.getResultCode() == Activity.RESULT_OK) {
                page=0;
                fr.clear();
                cookie = params.getCookie();
                getExchange();
            }
        });

        binding.toolbar.setNavigationOnClickListener(v->supportFinishAfterTransition());
        handler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                if (msg.what == -1) {
                    params.toast(getString(R.string.no_wifi_warning));
                }else {
                    JSONObject response = JSONObject.parseObject((String) msg.obj);
                    if (response != null && response.getInteger("code").equals(200)) {
                        JSONObject d = response.getJSONObject("data");
                        if (d != null) {
                            int total=d.getInteger("total");
                            d.getJSONArray("rows").forEach(a->{
                                order++;
                                ArrayList<String> values = new ArrayList<>();
                                String[] keyName = new String[]{"考试年份","上/下半年","语言级别","学号","姓名","笔试考试时间","笔试准考证号","笔试成绩总分","听力分数","阅读分数","综合分数","写作分数","口试考试时间","口试准考证号","口语成绩","所属学校","院系","专业","年级","班级","笔试科目名称","笔试报名号","笔试报名学校","笔试报名校区","是否缺考","是否违纪","违纪类型","是否听力障碍","口试科目名称","口试报名号","口试报名学校","口试报名校区"};
                                for(int i=0;i<keyName.length;i++){
                                    values.add(((JSONObject)a).getString(new String[]{"examYear","thePastOrNextHalfYearName","languageLevel","stuNum","stuName","writtenExaminationTime","writtenExaminationNumber","writtenExaminationTotalScore","hearingScore","readingScore","comprehensiveScore","writingScore","oralExamTime","oralExamNumber","oralExamAchievement","schoolName","collegeName","professionName","grade","stuClassName","writtenExaminationSubject","writtenExaminationApplyNumber","writtenExaminationApplySchool","writtenExaminationApplyCampus","whetherMissingTest","whetherViolation","violationType","whetherHearingObstacle","oralExamSubject","oralExamApplyNumber","oralExamApplySchool","oralExamApplyCampus"}[i]));
                                }
                                fr.add(CETActivity.this,String.valueOf(order),List.of(keyName), values);
                            });
                            if(total/10>page-1){
                                getExchange();
                            }
                        }
                    }
                    else {
                        params.toast(getString(R.string.login_warning));
                        launch.launch(new Intent(CETActivity.this, LoginActivity.class));
                    }
                }
            }
        };
        getExchange();
    }
    void getExchange(){
        page++;
        http.newCall(new Request.Builder().url("https://jwxt.sysu.edu.cn/jwxt/achievement-manage/englishGradeAchievement/stuPageList")
                .header("Cookie",cookie)
                .post(RequestBody.create(String.format(Locale.getDefault(),"{\"pageNo\":%d,\"pageSize\":10,\"total\":true,\"param\":{}}",page), MediaType.parse("application/json")))
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
}