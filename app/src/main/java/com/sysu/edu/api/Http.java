package com.sysu.edu.api;

import android.content.Context;

import androidx.annotation.NonNull;

import com.sysu.edu.preference.Language;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class Http {
    final Context context;
    public Http(Context context){
        this.context=context;
    }
    public OkHttpClient getJwxtHttp(){
        return new OkHttpClient.Builder().build();
    }
    public OkHttpClient getJwxtHttpWithReferer(String referer){
        return new OkHttpClient.Builder().addInterceptor(new Interceptor() {
            @NonNull
            @Override
            public Response intercept(@NonNull Chain chain) throws IOException {
                Request origin = chain.request();
                return chain.proceed(origin.newBuilder()
                        .header("Accept-Language", Language.getLanguageCode(context))//,zh;q=0.9,en;q=0.8,en-GB;q=0.7,en-US;q=0.6
                        .header("Referer",referer)
                        .method(origin.method(),origin.body())
                        .build());
            }
        }).build();
    }
}
