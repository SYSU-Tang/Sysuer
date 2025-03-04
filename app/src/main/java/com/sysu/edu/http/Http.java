package com.sysu.edu.http;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class Http {
    String sessionId="a332e4ca-1746-4605-b8a1-eed071992d94";
    public Http(){

    }

    public OkHttpClient getClient(@NonNull String cookie){
        return new OkHttpClient.Builder().addInterceptor(new Interceptor() {
            @NonNull
            @Override
            public Response intercept(@NonNull Chain chain) throws IOException {
                Request origin = chain.request();
                return chain.proceed(origin.newBuilder()
                        .header("Accept","text/css,*/*;q=0.1")
                        .header("Accept-Language","zh-CN,zh;q=0.9,en;q=0.8,en-GB;q=0.7,en-US;q=0.6")
                        .header("Connection","keep-alive")
                        .header("Cookie", cookie)
                        .header("DNT","1")
                        .header("Host","jwxt.sysu.edu.cn")
                        .header("If-Modified-Since","Tue, 07 Jan 2025 08:33:01 GMT")
                        .header("If-None-Match","W/\"677ce6bd-110f\"")
                        .header("Referer","https://jwxt.sysu.edu.cn/jwxt//yd/studyRoom/")
                        .header("Sec-Fetch-Dest","style")
                        .header("Sec-Fetch-Mode","no-cors")
                        .header("Sec-Fetch-Site","same-origin")
                        .header("sec-ch-ua","\"Not A(Brand\";v=\"8\", \"Chromium\";v=\"132\", \"Microsoft Edge\";v=\"132\"")
                        .header("sec-ch-ua-mobile","?1")
                        .header("sec-ch-ua-platform","\"Android\"")
                        .header("sec-gpc","1")
                        .method(origin.method(),origin.body())
                        .build());
            }
        }).build();
    }public OkHttpClient getClientOfSession(@NonNull String sessionId){
        return getClient("LYSESSIONID="+sessionId+";user=eyJ1c2VyVHlwZSI6IjEiLCJ1c2VyTmFtZSI6IjI0MzA4MTUyIiwibmFtZSI6IuWUkOi0pOaghyIsImxvZ2luUGF0dGVybiI6InN0dWRlbnQtbG9naW4iLCJzc28iOnRydWV9");
    }
    public String getSessionId(){
        return sessionId;
    }
}
