package com.sysu.edu.extra;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.MenuItem;
import android.webkit.CookieManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.LazyHeaders;
import com.sysu.edu.R;
import com.sysu.edu.databinding.LoginBinding;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LoginActivity extends AppCompatActivity {

    String sessionId;
    Handler handler;
    WebView web;
    String username="";
    String password="";
    SharedPreferences privacy;
    LoginBinding binding;
    Pattern pattern = Pattern.compile("//cas.+?\\.sysu.edu\\.cn");
    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding=LoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        privacy = getSharedPreferences("privacy", 0);
        username=privacy.getString("username","");
        password=privacy.getString("password","");
        binding.username.setText(username);
        binding.password.setText(password);
        binding.tool.setNavigationOnClickListener(v->finishAfterTransition());
//        ViewCompat.setOnApplyWindowInsetsListener(binding.main, (v, insets) -> {
//            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
//            v.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom);
//            binding.appbar.setPadding(0,systemBars.top,0,0);
//            return insets;
//        });
        web=new WebView(this);
        binding.m.addView(web);
        String url = getIntent().getStringExtra("url");
        if(url==null){url="https://cas.sysu.edu.cn/cas/login?service=https%3A%2F%2Fjwxt.sysu.edu.cn%2Fjwxt%2Fapi%2Fsso%2Fcas%2Flogin%3Fpattern%3Dstudent-login";}
        web.loadUrl(url);
        web.setWebViewClient(new WebViewClient(){
            @Override
            public void onPageFinished(WebView view, String url) {
                sessionId=CookieManager.getInstance().getCookie(url);
                if(!pattern.matcher(url).find()){
                    if(isEmpty()){
                        SharedPreferences.Editor edit = privacy.edit();
                        edit.putString("Cookie",sessionId);
                        edit.putString("token",getToken(sessionId));
                        edit.putString("username", username);
                        edit.putString("password", password);
                        edit.apply();
                        setResult(RESULT_OK);
                       finishAfterTransition();
                    }
                }
                else if(pattern.matcher(url).find()||privacy.getString("Cookie","").isEmpty()){
                    binding.loginButton.setEnabled(true);
                    Glide.with(LoginActivity.this).load(new GlideUrl("https://cas.sysu.edu.cn/cas/captcha.jsp",new LazyHeaders.Builder().addHeader("Cookie",sessionId).build())).diskCacheStrategy(DiskCacheStrategy.NONE).skipMemoryCache(true).override(92*3,34*3).into(binding.ca);
                }
                //web.evaluateJavascript("var script=document.createElement('script');script.src='https://cdn.jsdelivr.net/npm/eruda';document.body.appendChild(script);script.onload=function(){eruda.init()};", s -> {});
            }
            @Override
            public void onLoadResource(WebView view, String url) {
                view.evaluateJavascript("document.querySelector('meta[name=\"viewport\"]').setAttribute('content', 'width=1024px, initial-scale=' + (document.documentElement.clientWidth / 1024));", null);
            }
        });
        binding.tool.getMenu().add("确认").setIcon(R.drawable.submit).setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_IF_ROOM).setOnMenuItemClickListener(menuItem -> {
            web.loadUrl("https://portal.sysu.edu.cn/newClient/#/newPortal/index");
            return false;
        });
        WebSettings webSettings = web.getSettings();
        webSettings.setDomStorageEnabled(true);
        webSettings.setDatabaseEnabled(true);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setUseWideViewPort(true);
        webSettings.setBlockNetworkImage(false);
        webSettings.setUserAgentString("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/134.0.0.0 Safari/537.36 Edg/134.0.0.0");
        webSettings.setCacheMode(WebSettings.LOAD_NO_CACHE);
        webSettings.setJavaScriptEnabled(true);
        webSettings.supportZoom();
        webSettings.setSupportZoom(true);
        webSettings.setDisplayZoomControls(false);
        webSettings.setBuiltInZoomControls(false);
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
        //web.getSettings().
        // captcha=(TextInputEditText) findViewById(R.id.captcha);
        binding.ca.setOnClickListener(v -> {binding.loginButton.setEnabled(true);Glide.with(LoginActivity.this).load(new GlideUrl("https://cas.sysu.edu.cn/cas/captcha.jsp",new LazyHeaders.Builder().addHeader("Cookie",sessionId).build())).diskCacheStrategy(DiskCacheStrategy.NONE).skipMemoryCache(true).into((ImageView)v);});
        handler=new Handler(getMainLooper()){
            @Override
            public void handleMessage(@NonNull Message msg) {
                if (msg.what == 1) {
                    sessionId = (String) msg.obj;
                    //Glide.with(Login.this).load(new GlideUrl("https://cas.sysu.edu.cn/cas/captcha.jsp",new LazyHeaders.Builder().addHeader("Cookie",sessionId).build())).diskCacheStrategy(DiskCacheStrategy.NONE).skipMemoryCache(true).into(binding.ca);
//                        Glide.with(Login.this).load(new GlideUrl("https://cas.sysu.edu.cn/cas/captcha.jsp",new LazyHeaders.Builder().addHeader("Cookie",sessionId).build())).diskCacheStrategy(DiskCacheStrategy.NONE).skipMemoryCache(true).into(new CustomTarget<Drawable>(){
//                            @Override
//                            public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
//                                ((TextInputLayout)findViewById(R.id.cac)).setEndIconDrawable(resource);
//                            }
//
//                            @Override
//                            public void onLoadCleared(@Nullable Drawable placeholder) {
//
//                            }
//                        });
                    // ((TextInputLayout)findViewById(R.id.cac)).setEndIconDrawable(Glide.with(Login.this).asDrawable().load(new GlideUrl("https://cas.sysu.edu.cn/cas/captcha.jsp",new LazyHeaders.Builder().addHeader("Cookie",sessionId).build())).diskCacheStrategy(DiskCacheStrategy.NONE).skipMemoryCache(true).submit().get());
                }
            }
        };
//        new OkHttpClient.Builder().build().newCall(new Request.Builder().url("https://cas.sysu.edu.cn/cas/login?service=https://jwxt.sysu.edu.cn/jwxt/api/sso/cas/login?pattern=student-login").build()).enqueue(new Callback() {
//            @Override
//            public void onFailure(@NonNull Call call, @NonNull IOException e) {
//
//            }
//
//            @Override
//            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
//                String cookie = response.header("Set-Cookie");
//                Message msg=new Message();
//                msg.what=1;
//                msg.obj=cookie;
//                handler.sendMessage(msg);
//            }
//        });
        binding.loginButton.setOnClickListener(v -> {
            if(isEmpty()){
                binding.loginButton.setEnabled(false);
                web.evaluateJavascript("(function(){document.querySelector(\"#username\").value=\""+username+"\";document.querySelector(\"#password\").value=\""+password+"\";document.querySelector(\"#captcha\").value=\""+ binding.captcha.getText() +"\";var sub=document.querySelector(\".btn-submit\");sub.removeAttribute(\"disabled\");sub.click();"+"})()", null);}
//                FormBody form = new FormBody.Builder().add("username", "tangxb6").add("password", "Tang@1245").add("captcha", String.valueOf(captcha.getText()))
//                        .add("execution", "b1f6d386-3bd3-4693-a303-dbe6cc85784f_ZXlKaGJHY2lPaUpJVXpVeE1pSjkuT0U1TFMyNUpjR2xXYzBZd2QyVlhlbGs1VEVOQlVHTk5PRUZxYWpkSk4xbEVaMnhOTTI4dlVUVkViRlZzY2tRNGVGSlVZMkozYjJ4YWEwOTJZelZMWVc1aFlpOW9ZUzlLVkVkeGVXbG5VbkoyTjBNdmExQXJNWFZrUzAxdmJrNXZSSEoxVDJ4NFIyRkRXVXhoSzBwU2VWaFlka2hPUmxkemRUZE5aMWNyTnk5SlpVRlpPV2hWUVZZMVpVYzVhbTVKVUdJeFp6VlVkMlpyYm5ncmEzaHhLMHQ0U0hOcEsxaHVhMUZRVWxremEwZzNWREZTWld0V05FRXhZaXRHYmtORlF6Tk5iR3M0WkV0SFFtOTRkMFowVjB4SFowTjRVVXhCV0RsdVJYWXhkWEp2Um1OTlFWZG1aa0Y2TUZaMUswY3dVRVpwWkRWalIyNVhLMGN2V0N0UVpFb3lURU5HUzJvMVNtWlNZbmgyWWtSTVNqRjJVSFZSZVdWc1JVdDNlRWhtYms5Q05WRjNPVWhaTlhSb09HdHZNMDk0Vkd0UGRqaFlWVUpPV2tWRlYzTjRRVTlVYlZGNVVVUkJRbEJ4YTBKU2VVVkNNMWhPTWtKM1F6WXpWRllyVUc1cGIwRlNRMDVqT1dabk1rNUtkR1ZIWmtaWVZucDFZblZzWjI5ekwzRXhPV2xRVFhRMk9VOUNaWGN3ZDNGck5GSjRRMGM0T1V0dmNsVmFSbEpuTVZVck5EZHNjbTFGWTNKRFZIQm1heTlEZW1GT1ptSkJNRE5pYzJOS2QwOVZVV2RaZGt0RlZtMHZPR0pEVEZCVGNFbGljMWRCSzJsaWJGTnlaVEpUVjFaVE4wVmliR1JGY0RKdmNVRndNazF5Um10RlYxSlFNRkl3TTFsdFNUTmhTbXRFTVVFeFlYUTVRM05LTUZWNWIydEZUMHR4TVU4M2FWQlFjMHByU0VORlpTOUhVbTgzUzNOQmNpdFZWVzVWVTNKdlNHRlFLMmwwVHpVelRGTlhVbGRzWlZkVFYzVmhRMGc1T0c1UGRGTk5ha3h4ZWpaMlJtdEJaV1ZJZEc5b0wzbEpSbFJ4UlRCa1NuTlpUVEkyWnpVd1JHczRXVXRXZDNjM1kyOWFlVVZXT0VvelMxZDNjMUp4UWtGaVdtUnZTVmxXTDJObmFrMDNWME5NYWsweE1URXhWMkpPTTJsVVJHdHdkRXB4TmtzMGNqSnFjelZpY25kcVIwOXRNMWMzWkUxU2RIbDFPVkJGYmpCSFVsWk1kblpRYUhOdVRuQjVjVlJvYVRKVk5GUmthR0l4Wm5GWGF6Qm1lR2hQVjJOSVZXMHJhMGxuUVU5SlYyRk5aV3RaYURobk5YTktlVVpVUm5JeVUzTk5TM0IyWWxWNmQzcFdabVV2VVd0M01IbzFSM3BMYmpWWk1qUnphMHRNUjJKd1VUTjJZMlZrUzB0bFJESlhWVWsxTWpkYUx6azVVRVprYVdkMVMzbE1lbk5HWW1vdllrZE1WVFp6VjI1R01rZzRNVUp6WkVWa2NWSnhabVJuWW5ZM1QybDFkSGhVUm0xbVNYQmpNR1ZPTTFGVVl6ZDNTR05UTDFCR1FXNUxUMWx3Y1dWMlQxTmhWemd5U0RSM05tNUVNMVl6V0ZRM1VFeFlNSHBzV1U5YWJVNUtjRzlzUm5RNE1HWmpkR0VyU0ZWSWQzb3ZWVnAwUkVSMlJXOW5UalYyYkhFMFpqZEVWRVpIVERoQk1IbFpjRWhuYlhkRk9YTlhObUZCYjJ0TFlqTmtVRVZuTmxOQ1FuUmFiWE50Y0dsMGEzTTFOM2hRVVN0ME1IWjBibmM0VERoQk56TXJka1ZPVUhWcGVpdENkVWxMYVdWdVZGazFObWd3VEZabk5rSkhiV2RDY2xWS1RXTmFRMkYyYjJKdmRGVXlia1JYTm1vdlpuZG5aRk5xUlVKc1IzUjZlVzVuYTFsVWVqUTBSbGdyVUhveVdtNTBaV3BMYUhGQ09VUmlPVFowV201UFpVMDBlVzFOYTFoSU1FUlZXbkJ6VmlzMlZuZzVjM0pRTkhSTU5YTjRVbnA0V25CR2Qyb3ZlRlJZYVdRNWVTOVZNMGhzT1VGRGRXZHJlVzUxZUZBMk4yVldWa0l4TWl0UWNHMDFVek5pY1RKMlUxWm9kbmxLZFRGc1RsZHJTVTF3YVRGWFQyMTZSRmhKZDNVeUwweGFiU3RJWjFodmNISnRSRlk1VHpsM1JsSjJiVVJGUm1ocFl6UlZUMjlRYWtKTmFraENUV1JVTjJSd1dEUXpSM2d4YlVadlNVUlRObkoxVVVkVGRXTTNSVko1YjJkcGFXaEVSRGd6ZVZCSmIzSmpPRVJhZURGcWFFVnBlbWxZZVdjclRXTm5ORk55Y0RReGVHRTBPVE5JVmtkVk5rczNSbGxQTW1NeVVVRjJaalptWVc1U0sxaEhSeTlJTmsxaFdHMVRaSEY2WjI0NGRXUjVkMkYxZGtObFNFZGlabkZKVVROblRVY3djME5GZUZKSmRHMW5heXRHY25GWVYzSlRjM0ZYYm1GWGIwcE5hbmRwYjNoU1ZXRlpSV0ZrYVV4eFYzTnhaV0kyVUV4M04ydFlaVFpPWkc5NFpVVXZNR3h4Um1WRGNWaDNRMDVGTVZNM2Nub3dWelJIUjBNeGVsUjFZbFpJVlRsSFZtcExkVXRsVjB4blRUbGxiUzl4TVcxR1lUUXpjM2x0T1daeWVHTnBMMjF4ZWxGT1JIaHpRVWwwUzJoVmJsTXJRVzQ0VGpKS1NVaDNabWh2WmxwcFJVeEhUazlxVEc1MmRtaExaMXBvTkdGQkwxTk1SRTFvZUhCcmNtVXJORk12UldaTGEwaHZhRGg0UlUxellVWktiVlZqYkhsNGNXbFpiVXhyWm5OcVNGTm5SME5ZTUVaV2NVMDRVSEpwUVdNeWJYQTJiQ3RHTDFWNlFVUlJVMVZGZVZNNGJqSjFlbE5JY0hRMVVFUkhkSFoyYjB4TGFVOTZPRVpLVkhNMk4zQTRaa3RSVG14WmJIQmlXa2xqWlhZclNXSnViWFJDU2xkMVpFSllibmhzTW04NVFWWlhMekJzUW1OUFNtaExTVGt3WWtwRFExTjRTV3R5WlZsNk1YaGlVSFZsVGpjd1dFZ3pRWEZ5TDNCME1qVnZkbkZIVnpoT1YxQTFOVlpCWTBGdEx6TlpiVkZVWlhBd2RsZDZhVU4wWkZsR1dYRlVZa05SWVcxTU0yVkhUQzk1VVdaV1FqZHFiRzVVWlZjMVUzUktWR05XZVcwMmJ6VTVPWGR1UldobmNrbE5ZMmxNVWxwdmVHVjJRekZuVjNnemJtRktWek5HV1ZZeGNHOUlXQzlWUzA5WmRrazFja0ZpVm1wNUwxZGxNRmhWYVdSalNTdEtZblJrTmsxTVpYSnNZMHhzZVVGa05qUkpXVUpsZEc5QlRGTlpSV050WlU5RVpXaE9NbXRHU0doTFVsTkNaMnN4U0hKM1lTczROMWx1WjNCcVMxWldhME5yVlhwTVozRXpXVGsxYjJOd2MwbHdZbmhvWnpSamVVTk9UMG8zV2xOcWFVTTRObnB0YnpkcVoxY3hjRFJZWVVwb05YSXZhSEo0WlRaSE9VdEVkR0ZSVDNVeGNHTklSelZxZVZWcWRVUmthV05HV2k5VmIwcGpXRkJKUlcwNVRGSktkRTFxY1djeUwyTjZWblJwZVZjeUwyNXFUWEZKSzIxeVFXRm1lVzltYUN0amMyZDBRa3gwU1dkcFNFdFNSRTVHVnpVNVdWZFdka0o2S3psc2IxSlNjazFtTVdGRVNXZzFVSGRPTjFCRVkyRnlXbmRUYldsdU5sRmxLMVo1VFRBM05EUTJSREZyUzBNM1duRk5aa3RzVlN0UVVsTkpjRU5SY0RsalpWSjBZa0ZJYTBzMmFWZGlZVFptVTJKSk9VUmxZbWQ0U3k5WFNYVnVkMmRFU0U0emJpdFZTVko2UzBreVJsZDNjVWhGUVhsTlNIQjJlazUyV0ZObVdVZHVlRU4wYTNwelFYbFpNM0pFTXpGQmMxWXhVbTVxWkdWeVNHSTJUbGMyUkRoSFp6SlVVVVYyWnpoRGJsRXJPRU5IT0VvNVptNWpPV0l4YTJoVmVuTXhiWEZHZFZaTlJUWjNjbFUxWmpWUVRYRnBSM05KY0VkVWFtSkljMFJOWjJ4MVNHUmhjVWcyVm5rMVRGWTNlbVZUZUhWWGMxVjZVV3hIWVN0aFIxSnNkMmRyV0RCMmNWSlFOVmRWVVdGWGJqbGlla2xQSzFWaVFWUm9kMlJaVUVsVFlsVkxOa1I0U0dSUWRtbGlRVmx1VW14TVpXTkRSMGxVUkZJM2FrcFFNekZuSzJGMGFIUkJZbVp6ZDJsVlFtVmpPVzEyYUhCb1JrcHJjbmxZZEhjNVdGZDBWRlJNZDJOSGQySnJURTl0ZDFWWk9EWnZaMEZaTm05T1QydEtabUpWYWtaSVdEZzNjRk5PUWpKaFZFeHlZM2RtYkd4VmNFdE5lR2cyWkRSM1lXWmhaalZsYlhCbE5XVjBkMlpUUjFCVGJEbE5UREZXYmxBMldtSjRlbFJ6UWpGRlpERXlkV05MUlVKR2VrcDBSbXRqVEZoRVFVMW1WRVJzZFROb1ZURXZNMVJFYVZrNU1qTnZNMDV5VTBGclIyUnBVR1l4WjI0elZsWXhPWFZyVmtKeFNrVTVkMmhCU0dndlJUQmxURkoxVFRnMGJTc3pjalEzU1VNemVYbHpOelpvYnpKdFptMWxSbTFKVWtwWVZYaHdkR1Z3T1hNelIwTkVVak5tUkRaS1RqUjFUbmx2WTNWUldXeEJWbkJZY0RoM2NtSTBkV04zU2xoM1lXOUNhR2xSWlhjNVpGaGtLek5oUzBwcWRFaDJlVlJsWTBwWFVYaFFWbmR1UzJkaE0xQndSbVZtYUZkT055dG9NamxRTm1RNVpsSmtVVWxWWmpGclVFOHpNWGMxSzJSbWRURXpVekJzVXpOb2QyNU9kamRuT0M5dVltRnlSRzVPTTNJNGVqVjFObXB2WlRWTFoxSm5TaTkyZG05bFUxbE9OMnR1YkcxdFFYaERaak5VT0ZkdGJtZEpUM05LVkU1cWRsazRNSE5qYXpoTFMxSldjMlJNT1c5cU16Vk5lSHBXTlZWUmEyNVhVWE01UkVSNGVVRXlWVWQxVmxOdVNVSTROVXB5UTNScVVGa3hVMEpJVkhJMlNTdENWUzkxZGxWM2FVVk9lWGRPUjBSck9TOHJVVFpZVDJNeWJqTm1Ta2Q0UW5sMVFURndVMmt4VERkUk9FRjNRVXBNTWtjMlVXRTBOVWhoU25rMmJFVnJVbWxxT0ZOTWVXc3dhMUUxVmxrNGEzZE9hMFZ2UVRrdk5FZFllR2hDYVdSSWFtWXJXWGgyUldOalpGUlBaVU53TldwV1dIQjFWM0ZsVVhSbVpEVkdhRnBUZEhWR2NDOHpLMjl3UWpGUWNFUnZiSFJpUmtkVGJtWXdMMDFXVEZSVGRuSnVlak5aU25OaVptaG5jMVlyUzFWNGRreExjUzluZHpVclJtRXdVR3BqWkVwVk5VUkNjRXBOZUVOdlpVZGxWMFJHSzNGMVdVWklWR2xZT0hwcGQxUlRUWFp5WVZseU0zWTJhVWN6ZVZSelp6TmhTVEZNUm5GWVlqWkxjemMyYURabFdpdEtURVZpTTNadlJHUm5VbGMzUlVkTGJHWmxTRFZaYWs4ek5rbEtURnBCUTNad2MyczJMM1JKVkV4S2MyTXZTM0ZLYTNwbmFHVmljRkJwTjNwVFVWVmlSMFZEYzBadVdsQkxSVGRYT1hkMWIwNWhUSE5WUW5sU2JrVnFSMk52ZW5WdmJFd3hUamt5YjJzMWIzTmhSVEZxTnpKaVRtSnlOMFIzZERscU4yUXdiVzlKYW1JMU9GcE9WbkpVV21KU1p6RmxiR1p4WW5kT2VsWnNiMWdyTTBkQ05FdHJVMjV2Tm5Wc09YQllOVzFUVldsR1NGQlpMMUpUT0ROblMyZzBMMnRNWjJKTU1EUnFXVm8xTDI0d1NuQkxObnBMY0c1aVpUWkNlVTFvVmtOdlpVWnpaazVZY0dwNGNFbzRWWGRyTWtORE5UWjJNa1JFVDFRdmFpdERka0p1Vm5Sd1NISkNZa3BOTjNKS1JqRjRSMUF2U0U5RVpUTm9SV1JtTm1OTWNtSjRPVXM1VTBZd0wySmlNVXR1T1ZCdFIxRlhlalZJUjNkamFrZzJlalZ1YUdjeVNXcHVkbmxNYjA5dGFWWnNOekoyUkROTUwwdEhZMWs5Lm96TXFBTElFbkhJTzhyV1czZVNlN1RVNl9hc2p1RmktTVBsVlRRaVhoYXZoRTVreEx3Y08tOHpBSnI4bFlNemxSSUdEanFSdy1OdHJlc0hkS0t4cTVR")
//                        .add("_eventId", "submit")
//                        .add("geolocation", "")
//                        .build();
//                new OkHttpClient.Builder().build().newCall(
//                        new Request.Builder().url("https://cas.sysu.edu.cn/cas/login?service=https%3A%2F%2Fjwxt.sysu.edu.cn%2Fjwxt%2Fapi%2Fsso%2Fcas%2Flogin%3Fpattern%3Dstudent-login")
//                               // .addHeader("Cookie", String.valueOf(((TextInputEditText)findViewById(R.id.password)).getText()))
//                                .addHeader("Referer","https://cas.sysu.edu.cn/cas/login?service=https%3A%2F%2Fjwxt.sysu.edu.cn%2Fjwxt%2Fapi%2Fsso%2Fcas%2Flogin%3Fpattern%3Dstudent-login")
//                                .addHeader("Sec-Fetch-Dest","document")
//                                .addHeader("Sec-Fetch-Mode","navigate")
//                                .addHeader("Origin","https://cas.sysu.edu.cn")
//                                .addHeader("Accept-Language","zh-CN,zh;q=0.9,en;q=0.8,en-GB;q=0.7,en-US;q=0.6")
//                                .addHeader("DNT","1")
//                                .addHeader("User-Agent","Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/133.0.0.0 Safari/537.36 Edg/133.0.0.0")
//                                .addHeader("Sec-Fetch-Site","same-origin")
//                                .addHeader("sec-ch-ua","\"Not A(Brand\";v=\"8\", \"Chromium\";v=\"132\", \"Microsoft Edge\";v=\"132\"")
//                                .addHeader("sec-ch-ua-mobile","?1")
//                                .addHeader("Sec-Fetch-User","?1")
//                                .addHeader("sec-ch-ua-platform","\"Android\"")
//                                .addHeader("sec-gpc","1")
//                                .addHeader("Content-Type","application/x-www-form-urlencoded")
//                                .addHeader("Cookie", sessionId)
//                                .addHeader("Accept","text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7")
//                                .post(form).build()).enqueue(new Callback() {
//                    @Override
//                    public void onFailure(@NonNull Call call, @NonNull IOException e) {
//                        System.out.println("失败");
//                    }
//
//                    @Override
//                    public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
//                        if (response.body() != null) {
//                            System.out.println(response.code());
//                            System.out.println(response.body().string());
//                        }
//                    }
//                });
//
        });
    }
    String getToken(String cookie) {
        Matcher matcher = Pattern.compile("ibps-1.0.1-token=(.+?);").matcher(cookie + ";");
        String token="";
        if(matcher.find()) {
            token = matcher.group(1);
        }
        return token==null?"":token;
    }
    boolean isEmpty(){
        username = String.valueOf(binding.username.getText());
        password = String.valueOf(binding.password.getText());
        return !username.isEmpty() && !password.isEmpty();
    }
}