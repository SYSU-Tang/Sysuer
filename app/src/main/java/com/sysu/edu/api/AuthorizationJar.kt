package com.sysu.edu.api;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

public class AuthorizationJar {
    private final Context context;
    private final SharedPreferences authPreferences;
    private final SharedPreferences tokenPreferences;
    private final SharedPreferences privacyPreferences;
    private final CookieManager cookieManager;
    
    public AuthorizationJar(Context context) {
        this.context = context;
        cookieManager = new CookieManager(context);
        authPreferences = context.getSharedPreferences("authorization", Context.MODE_PRIVATE);
        tokenPreferences = context.getSharedPreferences("token", Context.MODE_PRIVATE);
        privacyPreferences = context.getSharedPreferences("privacy", Context.MODE_PRIVATE);
    }
    
    public String getAuthorization(String host) {
        return authPreferences.getString(host, "");
    }
    
    public void setAuthorization(String host, String authorization) {
        authPreferences.edit().putString(host, authorization).apply();
    }
    
    public String getToken(String host) {
        return tokenPreferences.getString(host, "");
    }
    
    public void setToken(String host, String token) {
        tokenPreferences.edit().putString(host, token).apply();
    }
    
    public String getUserName() {
        return privacyPreferences.getString("username", "");
    }
    
    public void setUserName(String userName) {
        privacyPreferences.edit().putString("username", userName).apply();
    }
    
    public String getPassword() {
        return privacyPreferences.getString("password", "");
    }
    
    public void setPassword(String password) {
        privacyPreferences.edit().putString("password", password).apply();
    }
    
    public String getCookie(String host) {
        return cookieManager.toSimpleString(host);
    }
    
    public Context getContext() {
        return context;
    }
    
    @NonNull
    @Override
    public String toString() {
        return authPreferences.toString();
    }
    
}
