package com.sysu.edu.academic;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.MenuItem;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.sysu.edu.R;
import com.sysu.edu.databinding.TrainingScheduleBinding;

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
    TrainingScheduleBinding binding;
    OkHttpClient http = new OkHttpClient.Builder().build();
    String cookie="";
    Handler handler;
    ActivityResultLauncher<Intent> launch;
    TrainingScheduleFragment schedule;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding=TrainingScheduleBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        cookie=getSharedPreferences("privacy",0).getString("Cookie","");
        setSupportActionBar(binding.tool);
        schedule = (TrainingScheduleFragment) (Objects.requireNonNull(getSupportFragmentManager().findFragmentById(R.id.fragment))).getChildFragmentManager().getFragments().get(0);
        launch = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), o -> {
            if (o.getResultCode() == RESULT_OK) {
                cookie = getSharedPreferences("privacy", 0).getString("Cookie", "");
                getColleges("");
                getGrades();
                getTypes();
                getProfessions("");
            }
        });

       // System.out.println(getSupportFragmentManager().findFragmentById(R.id.fragment));
      //  NavController navController = ((NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.fragment)).getNavController();
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        handler=new Handler(getMainLooper()){
            @Override
            public void handleMessage(@NonNull Message msg) {
                JSONObject data = JSON.parseObject((String) msg.obj);
                if(data.getInteger("code")==200){
                    schedule.deal(msg.what,data);
                }else {
                    launch.launch(new Intent(TrainingSchedule.this, LoginActivity.class));
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

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if(item.getItemId()==android.R.id.home){
            finishAfterTransition();
        }
        return super.onOptionsItemSelected(item);
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