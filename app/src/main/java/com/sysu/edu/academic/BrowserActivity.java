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
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

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
    private CookieManager c;
    BrowserBinding binding;
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
        c = CookieManager.getInstance();
        c.setAcceptCookie(true);
        web = findViewById(R.id.web);
        c.setAcceptThirdPartyCookies(web, true);
        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                //view.loadUrl(String.valueOf(request.getUrl()));
                return false;
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
        });
        binding.tool.setOnItemSelectedListener(menuItem -> {
            if(menuItem.getItemId()==R.id.js){
                ArrayList<String> j = js.searchJS(web.getUrl());
                new AlertDialog.Builder(BrowserActivity.this).setTitle("脚本").setItems(j.toArray(new String[]{}), (dialogInterface, i) -> {

                        web.evaluateJavascript(j.get(i), s -> {

                });
                System.out.println(i);}).create().show();
            }
            return false;
        });
        String url = getIntent().getDataString() != null ? getIntent().getDataString() : "https://www.sysu.edu.cn/";
        //c.setCookie(url,"LYSESSIONID=1e1f3aaf-9f78-43e6-a017-9afa4c283aba;user=eyJ1c2VyVHlwZSI6IjEiLCJ1c2VyTmFtZSI6IjI0MzA4MTUyIiwibmFtZSI6IuWUkOi0pOaghyIsImxvZ2luUGF0dGVybiI6InN0dWRlbnQtbG9naW4iLCJzc28iOnRydWV9; ssoUsername=gDzeK9sUezH5IwnImtM2c3WnwW+qqkUKoujpcC19UUx+fD0kp79drFFVWIf8U4tcsWp1ornU9tjcfn4QpsnQILtb8/HoXwSEy041MvQ71V/2YzeG4n6RHnZaVDxzLy4pxxsZauutoE+4UL7SZy+tHCafNPRHltjOgkwRf4TRAqo=;user=eyJ1c2VyVHlwZSI6IjEiLCJ1c2VyTmFtZSI6IjI0MzA4MTUyIiwibmFtZSI6IuWUkOi0pOaghyIsImxvZ2luUGF0dGVybiI6InN0dWRlbnQtbG9naW4iLCJzc28iOnRydWV9");
        // c.flush();
        binding.toolbar.getMenu().add("在浏览器中打开").setOnMenuItemClickListener(menuItem -> {
            startActivity(new Intent(Intent.ACTION_VIEW).setData(Uri.parse(url)));
            return false;
        });
        binding.toolbar.getMenu().add("刷新").setOnMenuItemClickListener(menuItem -> {
            web.reload();
            return false;
        });
        binding.toolbar.getMenu().add("退出").setOnMenuItemClickListener(menuItem -> {
            finishAfterTransition();
            return false;
        });
        WebSettings webSettings = web.getSettings();
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
        web.evaluateJavascript("", s -> {

        });
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