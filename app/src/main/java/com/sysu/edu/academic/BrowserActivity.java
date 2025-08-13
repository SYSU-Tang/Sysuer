package com.sysu.edu.academic;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.alibaba.fastjson2.JSONObject;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.sysu.edu.R;
import com.sysu.edu.databinding.BrowserBinding;
import com.sysu.edu.extra.JavaScript;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.regex.Pattern;

public class BrowserActivity extends AppCompatActivity {
    WebView web;
    BrowserBinding binding;
    WebSettings webSettings;
    CookieManager cookie;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = BrowserBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.toolbar.setNavigationOnClickListener(v -> finishAfterTransition());
        SharedPreferences privacy = getSharedPreferences("privacy", 0);
        String username = privacy.getString("username", "");
        String password = privacy.getString("password", "");
        StringBuilder result;
        try {
            InputStreamReader input = new InputStreamReader(getAssets().open("js.json"));
            //input = new InputStreamReader(BufferedInputStream);
            BufferedReader buffer = new BufferedReader(input);
            String line;
            result = new StringBuilder();
            while ((line = buffer.readLine()) != null) {
                result.append(line);
            }
            input.close();
            buffer.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        JavaScript js = new JavaScript(result.toString());
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
//        c = CookieManager.getInstance();
//        c.setAcceptCookie(true);
        web = binding.web;

        //c.setAcceptThirdPartyCookies(web, true);
        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                 System.out.println(request.getUrl());
                 webSettings.setUserAgentString("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/134.0.0.0 Safari/537.36 Edg/134.0.0.0");
                // view.loadUrl(String.valueOf(request.getUrl()));
                return true;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                //System.out.println(url);
                Pattern pattern = Pattern.compile("//cas.+.sysu.edu.cn");
                if(pattern.matcher(url).find()){
                    web.evaluateJavascript(String.format("document.querySelector('#username').value='%s';document.querySelector('#password').value='%s';",username,password), s -> {
                    });
                }

                super.onPageFinished(view, url);
            }
            @Override
            public void onLoadResource(WebView view, String url) {
                view.evaluateJavascript("document.querySelector('meta[name=\"viewport\"]').setAttribute('content', 'width=1024px, initial-scale=' + (document.documentElement.clientWidth / 1024));", null);
            }
        });
        binding.tool.setOnItemSelectedListener(menuItem -> {
            if(menuItem.getItemId()==R.id.js){
                ArrayList<JSONObject> j = js.searchJS(web.getUrl());
                new MaterialAlertDialogBuilder(BrowserActivity.this).setTitle("脚本").setItems(js.getTitles(j), (dialogInterface, i) -> {
                        web.evaluateJavascript(j.get(i).getString("script"), s -> {

                });}).create().show();
            }
            return false;
        });
        String url = getIntent().getDataString() != null ? getIntent().getDataString() : "https://www.sysu.edu.cn/";
        cookie = CookieManager.getInstance();
        binding.toolbar.getMenu().add("在浏览器中打开").setOnMenuItemClickListener(menuItem -> {
            startActivity(new Intent(Intent.ACTION_VIEW).setData(Uri.parse(url)));
            return false;
        });
        binding.toolbar.getMenu().add("刷新").setOnMenuItemClickListener(menuItem -> {
            web.reload();
            return false;
        });
        binding.toolbar.getMenu().add("清除Cookie").setOnMenuItemClickListener(menuItem -> {
            cookie.removeAllCookies(aBoolean -> {

            });
            cookie.flush();
            return false;
        });
        binding.toolbar.getMenu().add("退出").setOnMenuItemClickListener(menuItem -> {
            finishAfterTransition();
            return false;
        });
        webSettings = web.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setSupportMultipleWindows(true);
        webSettings.setUseWideViewPort(true);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setSupportZoom(true);
        webSettings.setBuiltInZoomControls(true);
        webSettings.setAllowFileAccess(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setUserAgentString("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/134.0.0.0 Safari/537.36 Edg/134.0.0.0");
        webSettings.setDisplayZoomControls(false);
        webSettings.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
        webSettings.setAllowFileAccess(true);
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
        webSettings.setLoadsImagesAutomatically(true);
        webSettings.setDefaultTextEncodingName("utf-8");
       /* web.setWebChromeClient(new WebChromeClient() {
        });*/
        web.loadUrl(url);

    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_BACK) && web.canGoBack()) {
            web.goBack();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
    protected void onDestroy() {
        if (web != null) {
            ((ViewGroup)web.getParent()).removeView(web);
            web.destroy();
            web = null;
        }
        super.onDestroy();
    }
}