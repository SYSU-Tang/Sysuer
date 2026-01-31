package com.sysu.edu.api;

import android.os.Bundle;
import android.os.Message;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.util.Objects;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class PEPreservation {

    private final OkHttpClient http;
    String cookie;
    String authorization;

    public PEPreservation() {
        http = new OkHttpClient.Builder().build();
    }

    void getCampus() {
        http.newCall(new Request.Builder().url("https://gym.sysu.edu.cn/api/Campus/active")
                .header("Cookie", cookie)
                .header("Authorization", authorization)
                .build()).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {

            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                Message msg = new Message();
                msg.what = 1;
                Bundle data = new Bundle();
                String responseString = response.body().string();
                data.putBoolean("isJson", Objects.requireNonNull(response.header("Content-Type", "")).startsWith("application/json"));
                data.putString("data", responseString);
                msg.setData(data);
                //handler.sendMessage(msg);
            }
        });
    }

    void getField() {
        http.newCall(new Request.Builder().url("https://gym.sysu.edu.cn/api/venuetype/all")
                .header("Cookie", cookie)
                .header("Authorization", authorization)
                .build()).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {

            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                Message msg = new Message();
                msg.what = 2;
                Bundle data = new Bundle();
                data.putBoolean("isJson", Objects.requireNonNull(response.header("Content-Type", "")).startsWith("application/json"));
                data.putString("data", response.body().string());
                msg.setData(data);
                // handler.sendMessage(msg);
            }
        });
    }
}
