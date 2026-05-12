package com.sysu.edu.api;

import static com.sysu.edu.api.CommonUtil.toStringOrDefault;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class HttpManager {
    final OkHttpClient http = new OkHttpClient.Builder()
            .build(); // 全局 OkHttpClient 实例
    CookieManager cookieManager; // 全局 CookieManager 实例
    Handler handler; // 处理消息的 Handler 对象
    String referrer; // Referer 头字段值
    String cookie; // Cookie 头字段值
    String authorization; // Authorization 头字段值
    Params params; // 请求参数对象
    String ua; // User-Agent 头字段值
    String target; // 目标 URL
    boolean isAuthorizationRequired; // 是否需要 Authorization 头字段
    boolean isTokenRequired; // 是否需要 token 头字段
    Map<String, String> header;// 自定义请求头字段
    AuthorizationJar authorizationJar;// 自定义 Authorization 头字段

    /**
     * 构造函数
     *
     * @param handler 处理消息的 Handler 对象
     */
    public HttpManager(Handler handler) {
        setHandler(handler);
    }

    /**
     * 获取处理消息的 Handler 对象
     *
     * @return 处理消息的 Handler 对象
     */
    public Handler getHandler() {
        return handler;
    }

    /**
     * 设置处理消息的 Handler 对象
     *
     * @param handler 处理消息的 Handler 对象
     */
    public void setHandler(Handler handler) {
        this.handler = handler;
    }

    /**
     * 设置请求参数
     *
     * @param params 请求参数对象
     */
    public void setParams(Params params) {
        this.params = params;
        this.cookieManager = new CookieManager(params.getContext());
    }

    public void setCookieManager(CookieManager cookieManager){
        this.cookieManager = cookieManager;
    }

    /**
     * 设置 Referer 头字段
     *
     * @param referrer Referer 头字段值
     */
    public void setReferrer(String referrer) {
        this.referrer = referrer;
    }

    /**
     * 设置 Cookie 头字段，优先级最高
     *
     * @param cookie Cookie 头字段值
     */
    public void setCookie(String cookie) {
        this.cookie = cookie;
    }

    /**
     * 设置是否需要 Authorization 头字段
     *
     * @param isAuthorizationRequired 是否需要使用 Params 中的 Authorization 头字段
     */
    public void setAuthorizationRequired(boolean isAuthorizationRequired) {
        this.isAuthorizationRequired = isAuthorizationRequired;
    }

    /**
     * 设置是否需要 token 头字段
     *
     * @param isTokenRequired 是否需要使用 Params 中的 token 头字段
     */
    public void setTokenRequired(boolean isTokenRequired) {
        this.isTokenRequired = isTokenRequired;
    }

    /**
     * 设置 User-Agent 头字段
     *
     * @param ua User-Agent 头字段值
     */
    public void setUA(String ua) {
        this.ua = ua;
    }

    /**
     * 设置请求目标 URL
     *
     * @param target 请求目标 URL作为Cookie的提供者
     */
    public void setTarget(String target) {
        this.target = target;
    }

    /**
     * 设置请求头字段
     *
     * @param header 请求头字段映射
     *
     */
    public void setHeader(Map<String, String> header) {
        this.header = header;
    }

    /**
     * 设置 AuthorizationJar 对象
     *
     * @param authorizationJar AuthorizationJar 对象
     *
     */
    public void setAuthorizationJar(AuthorizationJar authorizationJar) {
        this.authorizationJar = authorizationJar;
    }


    /**
     * 发送网络请求
     *
     * @param url  请求 URL
     * @param data 请求数据
     * @param type 请求数据类型
     * @param what 消息标识
     */
    private void sendRequest(@NonNull String url, String data, String type, int what, String method) {
        Request.Builder request = generateRequest(url, data, type, method);
        sendRequest(request.build(), what);
    }

    /**
     * 发送网络请求
     *
     * @param request 请求对象
     * @param what    消息标识
     */
    public void sendRequest(Request request, int what) {
        http.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
//                System.out.println(request.url());
                sendFailure();
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                Message msg = new Message();
                msg.what = what;
                msg.obj = response.body().string();
                Bundle bundle = new Bundle();
                bundle.putInt("code", response.code());
                String type = response.header("Content-Type");
                bundle.putBoolean("isJSON", type != null && type.contains("application/json"));
                bundle.putString("data", (String) msg.obj);
                msg.setData(bundle);
                handler.sendMessage(msg);
            }
        });
    }

    /**
     * 发送失败消息
     */
    public void sendFailure() {
        handler.sendEmptyMessage(-1);
    }

    /**
     * 获取请求构建器
     *
     * @param url    请求 URL
     * @param data   请求数据
     * @param type   请求数据类型
     * @param method 请求方法
     * @return 请求构建器
     */
    public Request.Builder generateRequest(@NonNull String url, String data, String type, String method) {
        Request.Builder request = new Request.Builder().url(url);
        String host = HttpUrl.get(url).host();
        if (target != null)
            host = HttpUrl.get(target).host();
        if (params != null) request.header("Cookie", params.getCookie());
        if (cookieManager.get(host) != null) {
            request.header("Cookie", cookieManager.toString(host));
        }
        if (cookie != null) request.header("Cookie", cookie);
        if (isAuthorizationRequired && authorizationJar != null)
            request.header("Authorization", authorizationJar.getAuthorization(host));
        if (authorization != null) request.header("Authorization", authorization);
        if (referrer != null) request.header("Referer", referrer);
        if (ua != null) request.header("User-Agent", ua);
        if (data != null)
            request.post(RequestBody.create(data, MediaType.get(toStringOrDefault(type, "application/json"))));
        if (isTokenRequired && authorizationJar != null)
            request.header("token", authorizationJar.getToken(host));
        if (header != null) header.forEach(request::header);
        if ("DELETE".equals(method)) request.delete();
        return request;
    }

    /**
     * 发送 POST 请求
     *
     * @param url  请求 URL
     * @param data 请求 JSON 数据
     * @param what 消息标识
     */
    public void postRequest(@NonNull String url, String data, int what) {
        sendRequest(url, data, "application/json", what, "POST");
    }

    /**
     * 发送 POST 请求
     *
     * @param url  请求 URL
     * @param data 请求数据
     * @param type 请求数据类型
     * @param what 消息标识
     */
    public void postRequest(@NonNull String url, String data, String type, int what) {
        sendRequest(url, data, type, what, "POST");
    }

    /**
     * 发送 GET 请求
     *
     * @param url  请求 URL
     * @param what 消息标识
     */
    public void getRequest(@NonNull String url, int what) {
        sendRequest(url, null, null, what, "GET");
    }

    /**
     * 发送 DELETE 请求
     *
     * @param url  请求 URL
     * @param what 消息标识
     */
    public void deleteRequest(@NonNull String url, int what) {
        sendRequest(url, null, null, what, "DELETE");
    }
}
