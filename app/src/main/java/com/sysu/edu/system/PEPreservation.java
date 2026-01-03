package com.sysu.edu.system;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.google.android.material.chip.Chip;
import com.google.android.material.textview.MaterialTextView;
import com.sysu.edu.R;
import com.sysu.edu.databinding.ActivityPePreservationBinding;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class PEPreservation extends AppCompatActivity {
    final HashMap<String, ArrayList<String>> fields = new HashMap<>();
    Handler handler;
    final HashMap<String,Map<String,String>> fieldsInfo = new HashMap<>();
    HashMap<String,ArrayList<Chip>> fields_items=new HashMap<>();
    final String authorization="Bearer eyJhbGciOiJSUzI1NiIsImtpZCI6IjI1QjExQTk3MDQ0Qjc5MUVGN0I2MDAzOTdDMzk2MDJDQzA1RjY5NTYiLCJ4NXQiOiJKYkVhbHdSTGVSNzN0Z0E1ZkRsZ0xNQmZhVlkiLCJ0eXAiOiJKV1QifQ.eyJodHRwOi8vc2NoZW1hcy54bWxzb2FwLm9yZy93cy8yMDA1LzA1L2lkZW50aXR5L2NsYWltcy9uYW1lIjoidGFuZ3hiNiIsImh0dHA6Ly9zY2hlbWFzLnhtbHNvYXAub3JnL3dzLzIwMDUvMDUvaWRlbnRpdHkvY2xhaW1zL25hbWVpZGVudGlmaWVyIjoidGFuZ3hiNiIsImh0dHA6Ly9zY2llbnRpYS5jb20vY2xhaW1zL0luc3RpdHV0aW9uIjoiNGZiMzdlMjgtMGE5MC00YmIwLWIwZTAtYjc5OGVjZTZmMzIwIiwiaHR0cDovL3NjaWVudGlhLmNvbS9jbGFpbXMvSW5zdGl0dXRpb25OYW1lIjoibWFpbC5zeXN1IiwiaHR0cDovL3NjaGVtYXMueG1sc29hcC5vcmcvd3MvMjAwNS8wNS9pZGVudGl0eS9jbGFpbXMvZW1haWxhZGRyZXNzIjoidGFuZ3hiNkBtYWlsLnN5c3UuZWR1LmNuIiwiaHR0cDovL3NjaGVtYXMueG1sc29hcC5vcmcvd3MvMjAwNS8wNS9pZGVudGl0eS9jbGFpbXMvZ2l2ZW5uYW1lIjoidGFuZ3hiNiIsImh0dHA6Ly9zY2llbnRpYS5jb20vY2xhaW1zL1Njb3BlIjoiUkIiLCJuYmYiOjE3NDUxMzY4MjksImV4cCI6MTc0NTE0MDQyOSwiaXNzIjoiaHR0cHM6Ly9sb2NhbGhvc3QvIiwiYXVkIjoiaHR0cDovL3Jlc291cmNlYm9va2VyYXBpLmNsb3VkYXBwLm5ldC8ifQ.AW2OnNqjRuiDqhLPMIncoaVrcdagL4H4t0QNYVxvShxMq2ewbfkZeMVc7Kigd8le_xqD2ulQG_MMkvZqefzqIMEXbOWe4chQH-FIoMIpd6dZ0NeLA2RsFjSXwTkJZdj-CIPVojw3u1EDx9ZUnuQUIklYBUZ4V_VlNQJ_T3X-iIL6Spt2L-NkoXVbF1cbAfKMvAk10DEwN8smXgZIMw2rq9sc1vJNxcxS8oqobbcia39ltJRwpjNSr3J4dulA5sei-_dkm4DPJmoOg_Rd5DpazXpLfGoFYJ5xGR3YOz4r4JhEgQXkmkAZfLfZPVB_eEhRv7eeUtC_zw6wOTbTaGcZ6A";
    final String cookie="login_token_ec583190dcd12bca757dd13df10f59c3=9c0a3eee1a15dde87192b6f0429ac7d7; username_ec583190dcd12bca757dd13df10f59c3=tangxb6;login_sn_ec583190dcd12bca757dd13df10f59c3=410cc2c6354c94a787be8820dee8043d;";
    final OkHttpClient http=new OkHttpClient.Builder().build();
    ActivityPePreservationBinding binding;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding= ActivityPePreservationBinding.inflate(getLayoutInflater());
        EdgeToEdge.enable(this);
        setContentView(binding.getRoot());
        binding.tool.setNavigationOnClickListener(v -> finishAfterTransition());
        ViewCompat.setOnApplyWindowInsetsListener(binding.main, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        LinearLayoutManager linear = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL,false);
        binding.panel.setLayoutManager(linear);
        DateAdapter adp = new DateAdapter(this);
        binding.panel.setAdapter(adp);
        handler = new Handler(getMainLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                Bundle rdata = msg.getData();
                boolean isJson = !rdata.getBoolean("isJson");
                String json = rdata.getString("data");
                JSONArray data;
                if(isJson){return;}else{data = JSON.parseArray(json);}
                switch (msg.what) {
                    case 1:
                        if (data != null) {
                            data.forEach(e-> {
                                String id = ((JSONObject) e).getString("Identity");
                                fields.put(id,new ArrayList<>());
                                Chip chip= (Chip) getLayoutInflater().inflate(R.layout.item_filter_chip,binding.campus,false);
                                chip.setOnCheckedChangeListener((buttonView, isChecked) -> {
                                    if(isChecked){
                                        for(int i=0;i<binding.field.getChildCount();i++){binding.field.getChildAt(i).setVisibility(View.GONE);}
                                        if(fields_items.containsKey(id)){Objects.requireNonNull(fields_items.get(id)).forEach(f->f.setVisibility(View.VISIBLE));}else{
                                        ArrayList<Chip> items = new ArrayList<>();
                                        Objects.requireNonNull(fields.get(((JSONObject) e).getString("Identity"))).forEach(e1 ->{
                                            Chip field= (Chip) getLayoutInflater().inflate(R.layout.item_filter_chip,binding.field,false);
                                            field.setText(Objects.requireNonNull(fieldsInfo.get(e1)).get("name"));
//                                            field.setOnCheckedChangeListener((buttonView1, isChecked1) -> {
//                                                //fieldsInfo.get(e1).get("");
//                                            });
                                            items.add(field);
                                            binding.field.addView(field);
                                        });
                                            fields_items=new HashMap<>(Map.of(id,items));
                                        }
                                    }
                                });
                                chip.setText((((JSONObject) e).getString("Name")));
                                binding.campus.addView(chip);
                            });
                        }
                        getField(); break;
                    case 2:
                        if (data != null) {
                            data.forEach(e-> {
                            Objects.requireNonNull(fields.get(((JSONObject) e).getString("Campus"))).add(((JSONObject) e).getString("Identity"));
                            fieldsInfo.put(((JSONObject) e).getString("Identity"),Map.of("name",((JSONObject) e).getString("Name")));
                            });
                            ((Chip)binding.campus.getChildAt(0)).setChecked(true);
                        } break;
                }
            }
        };
        getCampus();
    }
    void getCampus(){
        http.newCall(new Request.Builder().url("https://gym.sysu.edu.cn/api/Campus/active")
                        .header("Cookie",cookie)
                        .header("Authorization", authorization)
                .build()).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Message message = new Message();
                message.what=-1;
                handler.sendMessage(message);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                Message msg = new Message();
                msg.what = 1;
                Bundle data = new Bundle();
                String dataString = response.body().string();
                data.putBoolean("isJson", Objects.requireNonNull(response.header("Content-Type", "")).startsWith("application/json"));
                data.putString("data",dataString);
                msg.setData(data);
                handler.sendMessage(msg);
            }
        });
    }
    void getField(){
        http.newCall(new Request.Builder().url("https://gym.sysu.edu.cn/api/venuetype/all")
                .header("Cookie",cookie)
                .header("Authorization", authorization)
                .build()).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Message message = new Message();
                message.what=-1;
                handler.sendMessage(message);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                Message msg = new Message();
                msg.what = 2;
                Bundle data = new Bundle();
                data.putBoolean("isJson", Objects.requireNonNull(response.header("Content-Type", "")).startsWith("application/json"));
                data.putString("data",response.body().string());
                msg.setData(data);
                handler.sendMessage(msg);
            }
        });
    }

}
class DateAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>{
    final Context context;
    public DateAdapter(Context context){
        super();
        this.context=context;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View item = LayoutInflater.from(context).inflate(R.layout.item_date, parent, false);

        return new RecyclerView.ViewHolder(item) {
        };
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ((MaterialTextView)holder.itemView.findViewById(R.id.date)).setText(getDate(position));
        ((MaterialTextView)holder.itemView.findViewById(R.id.week)).setText(String.format("星期%s", getWeek(position)));
    }

    @Override
    public int getItemCount() {
        return 3;
    }
    String getDate(int distanceDay) {
        Calendar date = Calendar.getInstance();
        date.add(Calendar.DATE,distanceDay);
        return new SimpleDateFormat("MM月dd日", Locale.CHINESE).format(date.getTime());
    }
    String getWeek(int distanceDay) {
        Calendar date = Calendar.getInstance();
        date.add(Calendar.DATE,distanceDay);
        int week = date.get(Calendar.DAY_OF_WEEK);
        week=(week==1)?6:week-1;
        return context.getResources().getStringArray(R.array.weeks)[week];
    }
}