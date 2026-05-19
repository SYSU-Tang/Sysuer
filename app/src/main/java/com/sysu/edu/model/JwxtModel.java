package com.sysu.edu.model;

import static com.sysu.edu.api.CommonUtil.toStringOrDefault;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;

import com.alibaba.fastjson2.JSONObject;
import com.sysu.edu.R;
import com.sysu.edu.api.AuthorizationManager;
import com.sysu.edu.api.CommonUtil;
import com.sysu.edu.api.ContextUtil;
import com.sysu.edu.api.CookieManager;
import com.sysu.edu.api.HttpManager;
import com.sysu.edu.api.TargetUrl;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.HashSet;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Request;
import okhttp3.Response;

public class JwxtModel {
    
    private final ContextUtil contextUtil;
    private final AuthorizationManager authorizationManager = new AuthorizationManager("jwxt.sysu.edu.cn", "jwxt-443.webvpn.sysu.edu.cn");
    private final HttpManager http = new HttpManager(new Handler(Looper.getMainLooper()));
    private final ArrayDeque<CommonUtil.Tuple2<Request, Integer>> queue = new ArrayDeque<>();
    private final MutableLiveData<CommonUtil.Tuple2<Integer, JSONObject>> message = new MutableLiveData<>();
    private final HashSet<CommonUtil.Tuple2<Request, Integer>> afterLoginRequest = new HashSet<>();
    
    public JwxtModel(Context context) {
        contextUtil = new ContextUtil(context);
        http.setCookieManager(new CookieManager(context));
        http.setReferrer("https://jwxt.sysu.edu.cn/");
    }
    
    public void add(Request request, int what) {
        queue.add(new CommonUtil.Tuple2<>(request, what));
    }
    
    public void add(String path, int what) {
        add(path, null, null, what);
    }
    
    public void add(String path, String data, int what) {
        add(path, data, null, what);
    }
    
    public void add(String path, String data, String type, int what) {
        queue.add(new CommonUtil.Tuple2<>(http.generateRequest("https://" + authorizationManager.getBaseUrl() + "/" + path, data, type).build(), what));
    }
    
    public void next() {
        CommonUtil.Tuple2<Request, Integer> request = getNextRequest();
        if (request != null) request(request);
    }
    public CommonUtil.Tuple2<Request, Integer> getNextRequest() {
        return queue.poll();
    }
    
    public void nextAll() {
        while (!queue.isEmpty()) next();
    }
    
    public void addAndNext(String path, String data, String type, int code) {
        add(path, data, type, code);
        next();
    }
    
    public void addAndNext(String path, String data, int code) {
        addAndNext(path, data, null, code);
    }
    
    public void addAndNext(String path, int code) {
        addAndNext(path, null, code);
    }
    
    public void login(CommonUtil.Tuple2<Request, Integer> request) {
        boolean empty = afterLoginRequest.isEmpty();
        afterLoginRequest.add(request);
        if (empty)
            contextUtil.login(authorizationManager.isAccessible() ? TargetUrl.JWXT : TargetUrl.JWXT_WEBVPN, () -> afterLoginRequest.forEach(this::retry));
        
    }
    
    public AuthorizationManager getAuthorizationManager() {
        return authorizationManager;
    }
    
    public void request(CommonUtil.Tuple2<Request, Integer> request) {
        http.getClient().newCall(request.getFirst()).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                handleFailure();
            }
            
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                handleResponse(request, response);
            }
        });
    }
    
    private void handleFailure() {
        http.getHandler().post(() -> contextUtil.toast(R.string.no_net_connected));
    }
    
    public CommonUtil.Tuple2<Integer, JSONObject> execute(CommonUtil.Tuple2<Request, Integer> request) {
        try {
            Response response = http.getClient().newCall(request.getFirst()).execute();
            return handleResponse(request, response);
        } catch (IOException e) {
            handleFailure();
            return null;
        }
    }
    
    public CommonUtil.Tuple2<Integer, JSONObject> execute(Request request, Integer code) {
        try {
            Response response = http.getClient().newCall(request).execute();
            handleResponse(new CommonUtil.Tuple2<>(request, code), response);
            return message.getValue();
        } catch (IOException e) {
            handleFailure();
            return null;
        }
    }
    
    private CommonUtil.Tuple2<Integer, JSONObject> handleResponse(CommonUtil.Tuple2<Request, Integer> request, Response response) throws IOException {
        String type = response.header("Content-Type");
        String content = response.body().string();
        if (type != null && type.contains("application/json")) {
            JSONObject contentJSON = JSONObject.parse(content);
            Integer code = contentJSON.getInteger("code");
            if (code.equals(53000007))
                login(request);
            else {
                if (!code.equals(200))
                    http.getHandler().post(() -> contextUtil.toast(toStringOrDefault(contentJSON.getString("message"))));
                CommonUtil.Tuple2<Integer, JSONObject> result = new CommonUtil.Tuple2<>(request.getSecond(), contentJSON);
                message.postValue(result);
                afterLoginRequest.remove(request);
                return result;
            }
        } else {
            if (!authorizationManager.isAuthorized(content))
                login(request);
            else if (!authorizationManager.isAccessible(content)) retry(request);
        }
        return null;
    }
    
    protected void retry(CommonUtil.Tuple2<Request, Integer> request) {
        request.setFirst(updateRequest(request.getFirst()));
        request(request);
    }
    
    public void request(Request request, int code) {
        request(new CommonUtil.Tuple2<>(request, code));
    }
    
    public ContextUtil getContextUtil() {
        return contextUtil;
    }
    
    public MutableLiveData<CommonUtil.Tuple2<Integer, JSONObject>> getMessage() {
        return message;
    }
    
    public String getHost() {
        return authorizationManager.getBaseUrl();
    }
    
    public CookieManager getCookieManager() {
        return http.getCookieManager();
    }
    
    public Request updateRequest(Request request) {
        return request.newBuilder().url(request.url().newBuilder().host(authorizationManager.getBaseUrl()).build()).header("Cookie", http.getCookieManager().toSimpleString(authorizationManager.getBaseUrl())).build();
    }
}
