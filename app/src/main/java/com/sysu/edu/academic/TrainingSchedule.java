package com.sysu.edu.academic;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.sysu.edu.R;
import com.sysu.edu.api.Params;
import com.sysu.edu.api.TargetUrl;
import com.sysu.edu.databinding.ActivityTrainingScheduleBinding;

import java.io.IOException;
import java.util.Objects;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class TrainingSchedule extends AppCompatActivity {
    ActivityTrainingScheduleBinding binding;
    OkHttpClient http = new OkHttpClient.Builder().build();
    String cookie="";
    Handler handler;
    TrainingScheduleFragment schedule;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding=ActivityTrainingScheduleBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        Params params = new Params(this);
        params.setCallback(o -> {
            if (o.getResultCode() == RESULT_OK) {
                cookie = params.getCookie();
                getColleges("");
                getGrades();
                getTypes();
                getProfessions("");
            }
        });
        cookie=params.getCookie();
        binding.tool.setNavigationOnClickListener(view -> supportFinishAfterTransition());
        schedule = (TrainingScheduleFragment) (Objects.requireNonNull(getSupportFragmentManager().findFragmentById(R.id.fragment))).getChildFragmentManager().getFragments().get(0);

       // System.out.println(getSupportFragmentManager().findFragmentById(R.id.fragment));
      //  NavController navController = ((NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.fragment)).getNavController();
         handler=new Handler(getMainLooper()){
            @Override
            public void handleMessage(@NonNull Message msg) {
                JSONObject data = JSON.parseObject((String) msg.obj);
                if(data.getInteger("code")==200){
                    schedule.deal(msg.what,data);
                }else {
                    params.toast(R.string.login_warning);
                    params.gotoLogin(binding.tool, TargetUrl.JWXT);
                }
                super.handleMessage(msg);
            }
        };
        getColleges("");
        getGrades();
        getTypes();
        getProfessions("");
    }

    void getProfessions(String keyword) {
        http.newCall(new Request.Builder().url("https://jwxt.sysu.edu.cn/jwxt/base-info/profession-direction/pull?majorProfessionDircetion=1&nameCode="+keyword).header("Cookie",cookie).header("Referer", "https://jwxt.sysu.edu.cn/jwxt/mk/").build()).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                Message msg = new Message();
                msg.what=4;
                msg.obj = response.body().string();
                handler.sendMessage(msg);
            }
        });
    }

    void getTypes() {
        http.newCall(new Request.Builder()
                .url("https://jwxt.sysu.edu.cn/jwxt/base-info/codedata/findcodedataNames?datableNumber=97").header("Cookie",cookie).header("Referer", "https://jwxt.sysu.edu.cn/jwxt/mk/").build()).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                Message msg = new Message();
                msg.what=3;
                msg.obj = response.body().string();
                handler.sendMessage(msg);
            }
        });
    }
    void getColleges(String keyword){
        http.newCall(new Request.Builder().url("https://jwxt.sysu.edu.cn/jwxt/base-info/department/recruitUnitPull").post(RequestBody.create("{\"departmentName\":\""+keyword+"\",\"subordinateDepartmentNumber\":null,\"id\":null}",MediaType.parse("application/json"))).header("Cookie",cookie).header("Referer", "https://jwxt.sysu.edu.cn/jwxt/mk/").build()).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                Message msg = new Message();
                msg.what=1;
                msg.obj = response.body().string();
                handler.sendMessage(msg);
            }
        });
    }
    void getGrades(){
        http.newCall(new Request.Builder()
                .url("https://jwxt.sysu.edu.cn/jwxt/base-info/codedata/findcodedataNames?datableNumber=127").header("Cookie",cookie).header("Referer", "https://jwxt.sysu.edu.cn/jwxt/mk/").build()).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
            }
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                Message msg = new Message();
                msg.what=2;
                msg.obj = response.body().string();
                handler.sendMessage(msg);
            }
        });
    }
//    void getInfo(int page,String college,String grade,String profession,int trainCode,String trainName){
//        http.newCall(new Request.Builder().url("https://jwxt.sysu.edu.cn/jwxt/training-programe/training-programe/undergradute/profession-info").post(RequestBody.create(String.format("{\"pageNo\":%d,\"pageSize\":10,\"total\":true,\"param\":{\"manageUnitNum\":\"%s\",\"grade\":\"%s\",\"professionCode\":\"%s\",\"trainTypeCode\":\"%01d\",\"trainingSchemeCategoryName\":\"%s\"}}",page,college,grade,profession,trainCode,trainName),MediaType.parse("application/json"))).header("Cookie",cookie).header("Referer", "https://jwxt.sysu.edu.cn/jwxt/mk/").build()).enqueue(new Callback() {
//            @Override
//            public void onFailure(@NonNull Call call, @NonNull IOException e) {
//            }
//            @Override
//            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
//                Message msg = new Message();
//                msg.what=5;
//                if (response.body() != null) {
//                    msg.obj=response.body().string();
//                }
//                handler.sendMessage(msg);
//            }
//        });
//    }
}