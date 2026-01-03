package com.sysu.edu.api;

import androidx.annotation.NonNull;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class Account {
    final String sessionId="a332e4ca-1746-4605-b8a1-eed071992d94";
    public Account(){

    }

    public OkHttpClient getClient(@NonNull String cookie){
        return new OkHttpClient.Builder().addInterceptor(new Interceptor() {
            @NonNull
            @Override
            public Response intercept(@NonNull Chain chain) throws IOException {
                Request origin = chain.request();
                return chain.proceed(origin.newBuilder()
                        .header("Cookie", cookie)
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
