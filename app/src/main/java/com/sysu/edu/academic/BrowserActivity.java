package com.sysu.edu.academic;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.alibaba.fastjson2.JSONObject;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.sysu.edu.R;
import com.sysu.edu.databinding.ActivityBrowserBinding;
import com.sysu.edu.databinding.ItemPreferenceBinding;
import com.sysu.edu.extra.JavaScript;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.regex.Pattern;

public class BrowserActivity extends AppCompatActivity {
    WebView web;
    ActivityBrowserBinding binding;
    WebSettings webSettings;
    CookieManager cookie;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityBrowserBinding.inflate(getLayoutInflater());
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
//        c = CookieManager.getInstance();
//        c.setAcceptCookie(true);
        web = binding.web;

        //c.setAcceptThirdPartyCookies(web, true);
        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                view.loadUrl(String.valueOf(request.getUrl()));
                return true;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                //System.out.println(url);
                Pattern pattern = Pattern.compile("//cas.+.sysu.edu.cn");
                if (pattern.matcher(url).find()) {
//                    web.evaluateJavascript(String.format("document.querySelector('#username').value='%s';document.querySelector('#password').value='%s';",username,password), s -> {
//                    });
                    //String.format("javascript:(function(){var component=document.querySelector('.para-widget-account-psw');var data=component[Object.keys(component).filter(k => k.startsWith('jQuery') && k.endsWith('2'))[0]].widget_accountPsw;data.loginModel.dataField.username='%s';data.loginModel.dataField.password='%s';data.passwordInputVal='password';data.$loginBtn.click()})()", account, password)
                    web.evaluateJavascript(String.format("(function(){var component=document.querySelector('.para-widget-account-psw');var data=component[Object.keys(component).filter(k => k.startsWith('jQuery') && k.endsWith('2'))[0]].widget_accountPsw;data.loginModel.dataField.username='%s';data.loginModel.dataField.password='%s';data.passwordInputVal='password';data.$loginBtn.click()})()", username, password), s -> {
                    });
                }
                super.onPageFinished(view, url);
            }

            @Override
            public void onLoadResource(WebView view, String url) {

                view.evaluateJavascript("document.querySelector('meta[name=\"viewport\"]').setAttribute('content', 'width=1024px, initial-scale=' + (document.documentElement.clientWidth / 1024));", null);
            }
        });
        /*binding.tool.setOnMenuItemClickListener(menuItem -> {
            if (menuItem.getItemId() == R.id.js) {
                String url = web.getUrl();
                url = url == null ? "" : url;
                ArrayList<JSONObject> j = js.searchJS(url);
                new MaterialAlertDialogBuilder(BrowserActivity.this).setTitle("脚本").setItems(js.getTitles(j), (dialogInterface, i) -> web.evaluateJavascript(j.get(i).getString("script"), s -> {
                })).create().show();
            }
            return false;
        });*/
        BottomSheetDialog dialog = new BottomSheetDialog(BrowserActivity.this);
        dialog.setContentView(R.layout.recycler_view);
        dialog.setTitle(R.string.js);
        RecyclerView recyclerView = dialog.findViewById(R.id.recycler_view);
        JSAdapter adp = new JSAdapter(web);
        if (recyclerView != null) {
            recyclerView.setLayoutManager(new LinearLayoutManager(BrowserActivity.this));
            recyclerView.setAdapter(adp);
        }
        binding.js.setOnClickListener(v -> {
            String url = web.getUrl();
            url = url == null ? "" : url;
            ArrayList<JSONObject> j = js.searchJS(url);

            adp.setJS(j);
            dialog.show();
/*

            new MaterialAlertDialogBuilder(BrowserActivity.this).setTitle("脚本").setItems(js.getTitles(j), (dialogInterface, i) -> web.evaluateJavascript(j.get(i).getString("script"), s -> {
            })).create().show();*/
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
        binding.back.setOnClickListener(v -> {
            if (web.canGoBack()) {
                web.goBack();
            }
        });
        binding.forward.setOnClickListener(v -> {
            if (web.canGoForward()) {
                web.goForward();
            }
        });

        webSettings = web.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setSupportMultipleWindows(true);
        webSettings.setUseWideViewPort(true);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setSupportZoom(true);
        webSettings.setBuiltInZoomControls(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setUserAgentString("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/134.0.0.0 Safari/537.36 Edg/134.0.0.0");
        webSettings.setDisplayZoomControls(false);
        webSettings.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
        webSettings.setAllowFileAccess(true);
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
        webSettings.setLoadsImagesAutomatically(true);
        webSettings.setDefaultTextEncodingName("utf-8");
        web.loadUrl(url);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && web.canGoBack()) {
            web.goBack();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    protected void onDestroy() {
        if (web != null) {
            web.stopLoading();
            ((ViewGroup) web.getParent()).removeView(web);
            web.destroy();
            web = null;
        }
        super.onDestroy();
    }

    static class JSAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        ArrayList<JSONObject> j = new ArrayList<>();
        final WebView web;

        public JSAdapter(WebView web) {
            super();
            this.web = web;
        }

        public void setJS(ArrayList<JSONObject> j) {
            clear();
            this.j = j;
            notifyItemRangeInserted(0, getItemCount());
        }

        public void clear() {
            int size = j.size();
            j.clear();
            notifyItemRangeRemoved(0, size);
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new RecyclerView.ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_preference, parent, false)) {
            };
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            ItemPreferenceBinding binding = ItemPreferenceBinding.bind(holder.itemView);
            binding.itemTitle.setText(j.get(position).getString("title"));
            binding.itemContent.setText(j.get(position).getString("description"));
            binding.itemIcon.setImageResource(R.drawable.js);
            binding.getRoot().setOnClickListener(v -> web.evaluateJavascript(j.get(position).getString("script"), s -> {
            }));
        }


        @Override
        public int getItemCount() {
            return j.size();
        }

    }
}