package com.sysu.edu.api;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import androidx.annotation.NonNull;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class HttpManager {
    Handler handler;
    String referrer;
    String cookie;
    String authorization;
    OkHttpClient http;
    Params params;

    public HttpManager(Handler handler) {
        http = new OkHttpClient();
        setHandler(handler);
    }

    public void setParams(Params params) {
        this.params = params;
    }

    public void setHandler(Handler handler) {
        this.handler = handler;
    }

    public void setReferrer(String referrer) {
        this.referrer = referrer;
    }

    public void setCookie(String cookie) {
        this.cookie = cookie;
    }

    public void setAuthorization(String authorization) {
        this.authorization = authorization;
    }

    private void sendRequest(@NonNull String url, String data, String type, int what) {
        if (handler == null) {
            Log.e("HttpManager", "Handler Requested");
            return;
        }
        Request.Builder request = new Request.Builder()
                .url(url);
        if (params != null) request.header("Cookie", params.getCookie());
        if (cookie != null) request.header("Cookie", cookie);
        if (authorization != null) request.header("Authorization", authorization);
        if (referrer != null) request.header("Referer", referrer);
        if (data != null) request.post(RequestBody.create(data, MediaType.get(type)));
        http.newCall(request.build()).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Message msg = new Message();
                msg.what = -1;
                handler.sendMessage(msg);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                Message msg = new Message();
                msg.what = what;
                msg.obj = response.body().string();
                handler.sendMessage(msg);
            }
        });
    }

    public void postRequest(@NonNull String url, String data, int what) {
        sendRequest(url, data, "application/json", what);
    }

    public void postRequest(@NonNull String url, String data, String type, int what) {
        sendRequest(url, data, type, what);
    }

    public void getRequest(@NonNull String url, int what) {
        sendRequest(url, null, null, what);
    }
}
