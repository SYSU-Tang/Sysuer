package com.sysu.edu.api;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import java.net.HttpCookie;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import okhttp3.Cookie;

/*
 * Cookie管理类
 * 用于管理host的Cookie
 * */
public class CookieManager {
    private final SharedPreferences cookiePreference;

//    private final Set<String> toDelete = new HashSet<>();

    public CookieManager(Context context) {
        cookiePreference = context.getSharedPreferences("cookie", Context.MODE_PRIVATE);
    }

    public Set<String> get(String host) {
        return cookiePreference.getStringSet(host, null);
    }

    @NonNull
    public String toString(String host) {
        Set<String> strings = get(host);
        if (strings == null || strings.isEmpty()) return "";
        return String.join(";", strings);
    }

    public void set(String host, Set<String> cookieSet) {
        cookiePreference.edit().putStringSet(host, cookieSet.stream().filter(c -> !c.startsWith("rememberMe=")).collect(Collectors.toSet())).apply();
    }

    public void clear(String host) {
        cookiePreference.edit().remove(host).apply();
    }

    public void clearAll() {
        cookiePreference.edit().clear().apply();
    }

    public void add(String host, Cookie cookie) {
        if ("rememberMe".equals(cookie.name())) return;
        Set<String> cookieSet = get(host);
        if (cookieSet == null) cookieSet = new java.util.HashSet<>();
        for (String o : cookieSet) {
            HttpCookie c = HttpCookie.parse(o).get(0);
            if (Objects.equals(c.getName(), cookie.name())) cookieSet.remove(o);
        }
        cookieSet.add(cookie.toString());
        set(host, cookieSet);
    }

    public void add(String host, String cookie) {
        String[] parts = cookie.split("=");
        if ("rememberMe".equals(parts[0])) return;
        add(host, new Cookie.Builder().domain(host).name(parts[0]).value(parts[1]).build());
    }

    public void remove(String host, String cookieName) {
        Set<String> cookieSet = get(host);
        if (cookieSet != null) {
            for (String o : cookieSet) {
                HttpCookie c = HttpCookie.parse(o).get(0);
                if (Objects.equals(c.getName(), cookieName)) cookieSet.remove(o);
            }
            set(host, cookieSet);
        }
    }
}
