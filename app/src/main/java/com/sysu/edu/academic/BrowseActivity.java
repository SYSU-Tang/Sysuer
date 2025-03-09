package com.sysu.edu.academic;

import android.annotation.SuppressLint;
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

import com.sysu.edu.R;

public class BrowseActivity extends AppCompatActivity {
    WebView web;
    private CookieManager c;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.browser);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        c=CookieManager.getInstance();
        c.setAcceptCookie(true);
        web = findViewById(R.id.web);
        c.setAcceptThirdPartyCookies(web,true);
        web.setWebViewClient(new WebViewClient(){
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                //view.loadUrl(String.valueOf(request.getUrl()));
                return true;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                 super.onPageFinished(view, url);
            }
        });
        String url = getIntent().getDataString() != null ? getIntent().getDataString() : "https://www.sysu.edu.cn/";
        //c.setCookie(url,"LYSESSIONID=1e1f3aaf-9f78-43e6-a017-9afa4c283aba;user=eyJ1c2VyVHlwZSI6IjEiLCJ1c2VyTmFtZSI6IjI0MzA4MTUyIiwibmFtZSI6IuWUkOi0pOaghyIsImxvZ2luUGF0dGVybiI6InN0dWRlbnQtbG9naW4iLCJzc28iOnRydWV9; ssoUsername=gDzeK9sUezH5IwnImtM2c3WnwW+qqkUKoujpcC19UUx+fD0kp79drFFVWIf8U4tcsWp1ornU9tjcfn4QpsnQILtb8/HoXwSEy041MvQ71V/2YzeG4n6RHnZaVDxzLy4pxxsZauutoE+4UL7SZy+tHCafNPRHltjOgkwRf4TRAqo=;user=eyJ1c2VyVHlwZSI6IjEiLCJ1c2VyTmFtZSI6IjI0MzA4MTUyIiwibmFtZSI6IuWUkOi0pOaghyIsImxvZ2luUGF0dGVybiI6InN0dWRlbnQtbG9naW4iLCJzc28iOnRydWV9");
       // c.flush();
        WebSettings webSettings = web.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setSupportMultipleWindows(true);
        webSettings.setUseWideViewPort(true);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setSupportZoom(true);
        webSettings.setBuiltInZoomControls(true);
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