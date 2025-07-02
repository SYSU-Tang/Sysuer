package com.sysu.edu.academic;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.google.android.material.textview.MaterialTextView;
import com.sysu.edu.R;

import java.io.IOException;
import java.util.ArrayList;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class Evaluation extends AppCompatActivity {

    Handler handler;
    String cookie;
    ArrayList<JSONObject> evals=new ArrayList<>();
    private ActivityResultLauncher<Intent> launch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.evaluation);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        launch = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), o -> {
            if(o.getResultCode()== Activity.RESULT_OK){
                cookie= getSharedPreferences("privacy",0).getString("Cookie","");

            }
        });
        cookie=getSharedPreferences("privacy",0).getString("Cookie","");
        setSupportActionBar(findViewById(R.id.toolbar));
        RecyclerView list = findViewById(R.id.evaluation_list);
        StaggeredGridLayoutManager sgm=new StaggeredGridLayoutManager(2,1);
        list.setLayoutManager(sgm);
        EvalAdapter adp = new EvalAdapter(this);
        list.setAdapter(adp);
        getEvaluation();
        handler=new Handler(Looper.getMainLooper()){
            @Override
            public void handleMessage(@NonNull Message msg) {
                if (msg.what == 1) {
                    JSONObject data = JSON.parseObject((String) msg.obj);
                    if (data.get("code").equals("200")) {
                        data.getJSONObject("result").getJSONArray("list").forEach(e -> {
                            evals.add((JSONObject) e);
                        });
                        adp.set(evals);
                    }
                    else {
                        launch.launch(new Intent(Evaluation.this, Login.class));
                    }
                }
            }
        };
    }
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if(item.getItemId()==android.R.id.home){
            finishAfterTransition();
        }
        return true;
    }
    public void getEvaluation(){
        new OkHttpClient.Builder().build().newCall(new Request.Builder().url("https://pjxt.sysu.edu.cn/personnelEvaluation/listObtainPersonnelEvaluationTasks?yhdm=tangxb6&rwmc=&sfyp=0&pageNum=1&pageSize=10")
                .addHeader("Cookie","JSESSIONID=F547A1B2729098E0B101716397DC48DC;INCO=9b1595d95278e78f17d51a5f35287020;")
               // .post(RequestBody.create("{\"acadYear\":\"2024-2\",\"examWeekId\":\"1864116471884476417\",\"examWeekName\":\"18-19周期末考\",\"examDate\":\"\"}", MediaType.parse("application/json")))
                .build()).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {

                System.out.println("失败");
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                Message msg = new Message();
                msg.what=1;
                if (response.body() != null) {
                    msg.obj=response.body().string();
                }
                System.out.println(msg.obj);
                handler.sendMessage(msg);
            }
        });
    }
}
class EvalAdapter extends  RecyclerView.Adapter<RecyclerView.ViewHolder>{
    Context context;
    ArrayList<JSONObject> data=new ArrayList<>();
    public EvalAdapter(Context context)
    {
        super();
        this.context=context;
    }
    public void set(ArrayList<JSONObject> mData){
        clear();
        data.addAll(mData);
        notifyItemRangeInserted(0,mData.size());
    }
    public void clear(){
        int tmp=getItemCount();
        data.clear();
        notifyItemMoved(0,tmp);
    }
    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new RecyclerView.ViewHolder(LayoutInflater.from(context).inflate(R.layout.evaluation_item, parent,false)){};
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        holder.itemView.findViewById(R.id.open).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });
        ((MaterialTextView)holder.itemView.findViewById(R.id.title)).setText(data.get(position).getString("rwmc"));
        ((MaterialTextView)holder.itemView.findViewById(R.id.start_time)).setText(String.format("起始时间：%s", data.get(position).getString("rwkssj")));
        ((MaterialTextView)holder.itemView.findViewById(R.id.end_time)).setText(String.format("结束时间：%s",data.get(position).getString("rwjssj")));
        ((MaterialTextView)holder.itemView.findViewById(R.id.total)).setText(String.format("总评数：%s", data.get(position).getString("pjsl")));
        ((MaterialTextView)holder.itemView.findViewById(R.id.total_for)).setText(String.format("已评数：%s", data.get(position).getString("ypsl")));
          }

    @Override
    public int getItemCount() {
        return data.size();
    }
}
