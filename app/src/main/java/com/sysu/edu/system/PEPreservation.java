package com.sysu.edu.system;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.sysu.edu.databinding.PePreservationBinding;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class PEPreservation extends AppCompatActivity {
    PePreservationBinding binding;
    Handler handler;
    OkHttpClient http=new OkHttpClient.Builder().build();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding=PePreservationBinding.inflate(getLayoutInflater());
        EdgeToEdge.enable(this);
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(binding.main, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        handler = new Handler(getMainLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                Bundle rdata = msg.getData();
                boolean isJson = !rdata.getBoolean("isJson");
                String json = rdata.getString("data");

                JSONObject data;
                if(isJson){return;}else{data = JSON.parseObject(json);}
                switch (msg.what) {
                    case 1:System.out.println(data);break;
                }
            }
        };
        getCampus();
    }
    void getCampus(){
        http.newCall(new Request.Builder().url("https://gym.sysu.edu.cn/api/Campus/active")
                        .header("Cookie","login_token_ec583190dcd12bca757dd13df10f59c3=215c49df0de73dbbd3400bc4f03eee6e; username_ec583190dcd12bca757dd13df10f59c3=tangxb6;login_sn_ec583190dcd12bca757dd13df10f59c3=1f855f2cae741cdbf6f8253eee26a271;")
                        .header("Authorization", "Bearer eyJhbGciOiJSUzI1NiIsImtpZCI6IjI1QjExQTk3MDQ0Qjc5MUVGN0I2MDAzOTdDMzk2MDJDQzA1RjY5NTYiLCJ4NXQiOiJKYkVhbHdSTGVSNzN0Z0E1ZkRsZ0xNQmZhVlkiLCJ0eXAiOiJKV1QifQ.eyJodHRwOi8vc2NoZW1hcy54bWxzb2FwLm9yZy93cy8yMDA1LzA1L2lkZW50aXR5L2NsYWltcy9uYW1lIjoidGFuZ3hiNiIsImh0dHA6Ly9zY2hlbWFzLnhtbHNvYXAub3JnL3dzLzIwMDUvMDUvaWRlbnRpdHkvY2xhaW1zL25hbWVpZGVudGlmaWVyIjoidGFuZ3hiNiIsImh0dHA6Ly9zY2llbnRpYS5jb20vY2xhaW1zL0luc3RpdHV0aW9uIjoiNGZiMzdlMjgtMGE5MC00YmIwLWIwZTAtYjc5OGVjZTZmMzIwIiwiaHR0cDovL3NjaWVudGlhLmNvbS9jbGFpbXMvSW5zdGl0dXRpb25OYW1lIjoibWFpbC5zeXN1IiwiaHR0cDovL3NjaGVtYXMueG1sc29hcC5vcmcvd3MvMjAwNS8wNS9pZGVudGl0eS9jbGFpbXMvZ2l2ZW5uYW1lIjoi5ZSQ6LSk5qCHIiwiaHR0cDovL3NjaWVudGlhLmNvbS9jbGFpbXMvZ3JvdXAiOlsi55S15a2Q5LiO5L-h5oGv5bel56iL5a2m6Zmi77yI5b6u55S15a2Q5a2m6Zmi77yJIiwiMjQzMDgxNTIiLCLlrabnlJ8iLCLmnKznp5HnlJ8iLCLnlLXlrZDkuI7kv6Hmga_lt6XnqIvlrabpmaLvvIjlvq7nlLXlrZDlrabpmaLvvIktMjAyNCIsIueUteWtkOS4juS_oeaBr-W3peeoi-WtpumZou-8iOW-rueUteWtkOWtpumZou-8iS3mnKznp5HnlJ8iXSwiaHR0cDovL3NjaGVtYXMueG1sc29hcC5vcmcvd3MvMjAwNS8wNS9pZGVudGl0eS9jbGFpbXMvZW1haWxhZGRyZXNzIjoidGFuZ3hiNkBtYWlsLnN5c3UuZWR1LmNuIiwiaHR0cDovL3NjaWVudGlhLmNvbS9jbGFpbXMvU2NvcGUiOiJSQiIsIm5iZiI6MTc0NDQ2NTkzNiwiZXhwIjoxNzQ0NDY5NTM2LCJpc3MiOiJodHRwczovL2xvY2FsaG9zdC8iLCJhdWQiOiJodHRwOi8vcmVzb3VyY2Vib29rZXJhcGkuY2xvdWRhcHAubmV0LyJ9.AM756RbJ0c8nVxXYiwAa4NjNwxWW9X8fKSrvOslC4td8ICp9Y2uNw5aRgIAQ5bXlkHDWxTU1zL4i-8X5xVhSZt6sqtbNx_VdRH_DBm-YQXTst8j-AfthQGRAsD6z_qJyDTQeL_q4SBFpV1GdNIqkvt5VmdK253IWoyGCylUvkDnwaj7bbn3qH-BAbznOc1cLmKF-qDmnUXkT3CQpupWf3W_PnqtG91K9tH6V86qO-Kc-_fzf2Nbis2V7iU5ViUOdt6otEEQPWP1Az2itQ40HF4MzRvyaEubrN8gbc4awuq9bPsCw3uENFZdT7Fddr-97ZtDCY0CpJ_EEZWvq4OEDNA")
                .build()).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                System.out.println("失败");
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.body() != null) {
                    Message msg = new Message();
                    msg.what = 1;
                    Bundle data = new Bundle();
                    String datas = response.body().string();
                    data.putBoolean("isJson",response.header("Content-Type","").startsWith("application/json"));
                    data.putString("data",datas);
                    System.out.println(datas);
                    msg.setData(data);
                    handler.sendMessage(msg);
                }
            }
        });
    }
}