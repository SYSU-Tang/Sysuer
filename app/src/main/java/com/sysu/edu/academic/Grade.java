package com.sysu.edu.academic;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.sysu.edu.R;
import com.sysu.edu.api.Params;
import com.sysu.edu.databinding.GradeBinding;
import com.sysu.edu.extra.LoginActivity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class Grade extends AppCompatActivity {

    GradeBinding binding;
    Handler handler;
    String cookie="";
    OkHttpClient http;

    ActivityResultLauncher<Intent> launch;
    PopupMenu termPop;
    PopupMenu yearPop;
    PopupMenu typePop;
    String year;
    String type;
    int term;
    String[] terms;
    ScoreAdp adp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding= GradeBinding.inflate(getLayoutInflater());
        EdgeToEdge.enable(this);
        setContentView(binding.getRoot());
        termPop = new PopupMenu(Grade.this, binding.term,0, 0, com.google.android.material.R.style.Widget_Material3_PopupMenu_Overflow);
        terms = new String[]{"第一学期", "第二学期", "第三学期"};
        for (int i = 0; i < terms.length; i++) {
            String s = terms[i];
            int finalI = i+1;
            termPop.getMenu().add(s).setOnMenuItemClickListener(menuItem -> {
                if(finalI!=term){
                    setNow(year, finalI , type);
                }
                return false;
            });
        }
        binding.tabs.setHorizontalScrollBarEnabled(false);
        yearPop = new PopupMenu(this, binding.year,0, 0, com.google.android.material.R.style.Widget_Material3_PopupMenu_Overflow);
        typePop = new PopupMenu(this, binding.type,0, 0, com.google.android.material.R.style.Widget_Material3_PopupMenu_Overflow);
        binding.toolbar.setNavigationOnClickListener(view -> supportFinishAfterTransition());
        binding.scores.setLayoutManager(new GridLayoutManager(this,new Params(this).getColumn()));
        binding.term.setOnClickListener(view -> termPop.show());
        binding.year.setOnClickListener(view -> yearPop.show());
        binding.type.setOnClickListener(view -> typePop.show());
        adp = new ScoreAdp(this);
        binding.scores.setAdapter(adp);
        Params params = new Params(this);
        cookie=params.getCookie();
        http=getHttp();
        launch = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), o -> {
            if(o.getResultCode()==RESULT_OK){
                cookie=params.getCookie();
                http=getHttp();
                getPull();
            }
        });
        StaggeredFragment header = binding.header.getFragment();
        header.setNested(false);
        handler = new Handler(getMainLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                if(msg.what==-1){
                    Toast.makeText(Grade.this,(String)msg.obj,Toast.LENGTH_LONG).show();
                    return;
                }
                JSONObject dataString = JSON.parseObject((String) msg.obj);
                if(dataString.getInteger("code") == 200){
                    switch (msg.what) {
                        case 1:
                            adp.clear();
                            dataString.getJSONArray("data").forEach(a -> adp.add((JSONObject) a));
                            break;
                        case 2: {
                            JSONObject pull = dataString.getJSONObject("data");
                            pull.getJSONArray("selectTrainType").forEach(a -> typePop.getMenu().add(((JSONObject) a).getString("dataName")));
                            pull.getJSONArray("selectYearPull").forEach(a ->
                                yearPop.getMenu().add(((JSONObject) a).getString("dataName")).setOnMenuItemClickListener(menuItem -> {
                                    setNow(((JSONObject) a).getString("dataName"), term, type);
                                    return false;
                                }));
                            getNow();
                            break;
                        }
                        case 3: {
                            JSONObject pull = dataString.getJSONObject("data");
                            setNow(pull.getString("acadYear"), pull.getInteger("acadSemester"), pull.getString("sequence"));
                            yearPop.getMenu().add(pull.getString("acadYear")).setOnMenuItemClickListener(menuItem -> {
                                setNow(pull.getString("acadYear"),term,type);
                                return false;
                            });
                            break;
                        }
                        case 4: {
                            JSONObject pull = dataString.getJSONObject("data");
                            System.out.println(pull);
                            String totalRank = pull.getJSONArray("compulsorySelectTotal").getJSONObject(0).getString("rank");
                            String totalPoint = pull.getJSONArray("compulsorySelectTotal").getJSONObject(0).getString("vegPoint");
                            String totalCredit = pull.getJSONArray("compulsorySelectTotal").getJSONObject(0).getString("totalCredit");
                            String rank = "";
                            String point = "";
                            if (!pull.getJSONArray("compulsorySelectList").isEmpty()) {
                                rank = pull.getJSONArray("compulsorySelectList").getJSONObject(0).getString("rank");
                                point = pull.getJSONArray("compulsorySelectList").getJSONObject(0).getString("vegPoint");

                            }
                            String total = pull.getString("stuTotal");
                            JSONObject stuCredit = pull.getJSONObject("stuCredit");
//                            binding.detail.setText(String.format("总排名：%s/%s\n总学分：%s\n总绩点：%s", totalRank, total, totalCredit, totalPoint));
//                            binding.detail2.setText(String.format("当前排名：%s/%s\n当前绩点：%s", rank, total, point));
//                            binding.detail3.setText(String.format("学期学分：%s\n公必学分：%s\n公选学分：%s\n专必学分：%s\n专选学分：%s\n荣誉学分：%s",
//                                    stuCredit.getString("allGetCredit"),
//                                    stuCredit.getString("publicGetCredit"),
//                                    stuCredit.getString("publicSelectGetCredit"),
//                                    stuCredit.getString("majorGetCredit"),
//                                    stuCredit.getString("majorSelectGetCredit"),
//                                    stuCredit.getString("honorCourseGetCredit")
//                            ));
                            ArrayList<String> values = new ArrayList<>();
                            for (String key : new String[]{"allGetCredit","publicGetCredit","publicSelectGetCredit","majorGetCredit","majorSelectGetCredit","honorCourseGetCredit"}){
                                values.add(stuCredit.getString(key));
                            }
                            header.clear();
                            header.add("总学年", List.of("总排名","总学分","总绩点"),List.of(String.format("%s/%s",totalRank,total),totalCredit, totalPoint));
                            header.add(String.format("%s学期",term), List.of("当前排名","当前绩点"),List.of(String.format("%s/%s",rank,total), point));
                            header.add("学分", List.of("学期学分","公必学分","公选学分","专必学分","专选学分","荣誉学分"),values);
                            break;
                        }
                    }
                }
                else {
                    Toast.makeText(Grade.this,"请先登录",Toast.LENGTH_LONG).show();
                    launch.launch(new Intent(Grade.this, LoginActivity.class));
                }
            }
        };
        getPull();
    }
    public OkHttpClient getHttp(){
        return new OkHttpClient.Builder().addInterceptor(new Interceptor() {
            @NonNull
            @Override
            public Response intercept(@NonNull Chain chain) throws IOException {
                Request origin = chain.request();
                return chain.proceed(origin.newBuilder()
                        //.header("Accept-Language","zh-CN,zh;q=0.9,en;q=0.8,en-GB;q=0.7,en-US;q=0.6")
                        .header("Cookie", cookie)
                        .header("Referer","https://jwxt.sysu.edu.cn/jwxt/mk/studentWeb/")
                        .method(origin.method(),origin.body())
                        .build());
            }
        }).build();
    }
    public void setNow(String year,int term,String type){
        this.term=term;
        this.year=year;
        this.type=type;
        binding.term.setText(terms[term-1]);
        binding.year.setText(year);
        binding.type.setText("主修");
        getScore(year,term,"01");
        getTotalScore(year,term,"01");
    }
    public void getNow(){
        http.newCall(new Request.Builder().url("https://jwxt.sysu.edu.cn/jwxt/base-info/acadyearterm/showNewAcadlist").build()).enqueue(
                new  Callback(){
                    @Override
                    public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                        Message message = new Message();
                        message.what=3;
                        message.obj=response.body().string();
                        handler.sendMessage(message);
                    }
                    @Override
                    public void onFailure(@NonNull Call call, @NonNull IOException e) {
                        Message message = new Message();
                        message.what=-1;
                        handler.sendMessage(message);
                    }
                }
        );
    }
    public void getScore(String year,int term,String type){
        http.newCall(new Request.Builder().url(String.format(Locale.CHINA,"https://jwxt.sysu.edu.cn/jwxt/achievement-manage/score-check/list?scoSchoolYear=%s&trainTypeCode=%s&addScoreFlag=true&scoSemester=%d",year,type,term)).build()).enqueue(
                new  Callback(){
                    @Override
                    public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                        Message message = new Message();
                        message.what=1;
                        message.obj=response.body().string();
                        handler.sendMessage(message);
                    }
                    @Override
                    public void onFailure(@NonNull Call call, @NonNull IOException e) {
                        Message message = new Message();
                        message.what=-1;
                        handler.sendMessage(message);
                    }
                }
        );
    }
    public void getTotalScore(String year,int term,String type){
        http.newCall(new Request.Builder().url(String.format(Locale.CHINA,"https://jwxt.sysu.edu.cn/jwxt/achievement-manage/score-check/getSortByYear?scoSchoolYear=%s&trainTypeCode=%s&addScoreFlag=true&scoSemester=%d",year,type,term)).build()).enqueue(
                new  Callback(){
                    @Override
                    public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                        Message message = new Message();
                        message.what=4;
                        message.obj=response.body().string();
                        handler.sendMessage(message);
                    }
                    @Override
                    public void onFailure(@NonNull Call call, @NonNull IOException e) {
                        Message message = new Message();
                        message.what=-1;
                        handler.sendMessage(message);
                    }
                }
        );
    }
    public void getPull(){
        http.newCall(new Request.Builder().url("https://jwxt.sysu.edu.cn/jwxt/achievement-manage/score-check/getPull").build()).enqueue(
                new  Callback(){
                    @Override
                    public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                        Message message = new Message();
                        message.what=2;
                        message.obj=response.body().string();
                        handler.sendMessage(message);
                    }
                    @Override
                    public void onFailure(@NonNull Call call, @NonNull IOException e) {
                        Message message = new Message();
                        message.what=-1;
                        handler.sendMessage(message);
                    }
                }
        );
    }
}

class ScoreAdp extends RecyclerView.Adapter<RecyclerView.ViewHolder>{
    Context context;
    ArrayList<JSONObject> data=new ArrayList<>();
    public ScoreAdp (Context context){
        this.context=context;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new RecyclerView.ViewHolder(LayoutInflater.from(context).inflate(R.layout.score_item,parent,false)) {
        };
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        holder.itemView.setOnClickListener(view -> {

        });
        JSONObject info = data.get(position);
        String grade = "";
        for (Object a : info.getJSONArray("scoreList")) {
            grade = String.format("%s（%s）%s×%s%%+",grade,((JSONObject) a).getString("FXMC"), ((JSONObject) a).getString("FXCJ"), ((JSONObject) a).getString("MRQZ"));
        }
        ((TextView)holder.itemView.findViewById(R.id.subject)).setText(info.getString("scoCourseName"));
        ((TextView)holder.itemView.findViewById(R.id.score)).setText(String.format("%s/%s",info.getString("scoFinalScore"),info.getString("scoPoint")));
        ((TextView)holder.itemView.findViewById(R.id.info)).setText(String.format("学分：%s\n排名：%s\n课程类别：%s\n老师：%s\n是否通过：%s\n考试性质：%s\n成绩：%s",
                info.getString("scoCredit"),
                info.getString("teachClassRank"),
                info.getString("scoCourseCategoryName"),
                info.getString("scoTeacherName"),
                info.getString("accessFlag"),
                info.getString("examCharacter"),
                //
                grade.substring(0,grade.length()-1)+"="+info.getString("originalScore")));
    }
    public void add(JSONObject a){
        int tmp=getItemCount();
        data.add(a);
        notifyItemInserted(tmp);
    }
    public void clear(){
        int tmp=getItemCount();
        data.clear();
        notifyItemRangeRemoved(0,tmp);
    }
    @Override
    public int getItemCount() {
        return data.size();
    }
}