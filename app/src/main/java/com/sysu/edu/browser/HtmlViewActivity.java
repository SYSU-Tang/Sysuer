package com.sysu.edu.browser;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Message;
import android.webkit.JsResult;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.datastore.preferences.core.MutablePreferences;
import androidx.datastore.preferences.core.Preferences;
import androidx.datastore.preferences.core.PreferencesKeys;
import androidx.datastore.preferences.rxjava3.RxPreferenceDataStoreBuilder;
import androidx.datastore.rxjava3.RxDataStore;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.sysu.edu.BaseActivity;
import com.sysu.edu.R;
import com.sysu.edu.databinding.ActivityHtmlViewBinding;

import java.io.IOException;
import java.util.regex.Pattern;

import io.reactivex.rxjava3.core.Single;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class HtmlViewActivity extends BaseActivity {
    
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityHtmlViewBinding binding = ActivityHtmlViewBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        WebView web = binding.web;
        android.webkit.CookieManager cookie = android.webkit.CookieManager.getInstance();
        cookie.setAcceptCookie(true);
        cookie.setAcceptThirdPartyCookies(web, true);
        cookie.acceptThirdPartyCookies(web);
        RxDataStore<Preferences> dataStore = new RxPreferenceDataStoreBuilder(
                getApplicationContext(),
                "html_view"
        ).build();
        dataStore.updateDataAsync(prefs -> {
            MutablePreferences mutable = prefs.toMutablePreferences();
            mutable.set(PreferencesKeys.stringKey("file_path"), "");
            return Single.just(mutable);
        });
        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url1 = String.valueOf(request.getUrl());
                if (url1.startsWith("https://") || url1.startsWith("http://")) view.loadUrl(url1);
                else
                    startActivity(new Intent(Intent.ACTION_VIEW).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK).setData(Uri.parse(url1)));
                return true;
            }
            
            @Nullable
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                String url1 = String.valueOf(request.getUrl());
                if (Pattern.compile("//jwxt.sysu.edu.cn/jwxt/system-manage/infoRelease/downloadFile", Pattern.DOTALL).matcher(url1).find()) {
                    try {
                        Response response = new OkHttpClient().newCall(new Request.Builder().url(url1)
                                .header("Cookie", cookie.getCookie(url1))
                                .header("Referer", "https://jwxt.sysu.edu.cn/jwxt/")
                                .build()).execute();
                        MediaType mediaType = response.body().contentType();
                        return new WebResourceResponse(mediaType == null ? "application/octet-stream" : mediaType.type(), "utf-8", response.body().byteStream());
                    } catch (IOException _) {
                        
                    }
                }
                return super.shouldInterceptRequest(view, request);
            }
            
            @Override
            public void onPageFinished(WebView view, String link) {
               /* if (Pattern.compile("//cas.+?sysu\\.edu\\.cn/esc-sso/login/page").matcher(link).find())
                    disposable.add(params.getContextUtil().getAccountManager().getActiveAccountAsync("sysu.edu.cn").subscribe(account -> web.evaluateJavascript(String.format("""
                            javascript:(function(){\
                            function waitElement(selector, callback) {\
                            const element = document.querySelector(selector);\
                            if (element) {callback();}else{setTimeout(() => {waitElement(selector,callback);}, 100);}}\
                            waitElement('.para-widget-account-psw', () => {\
                            var component=document.querySelector('.para-widget-account-psw');var data=component[Object.keys(component).filter(k => k.startsWith('jQuery') && k.endsWith('2'))[0]].widget_accountPsw;data.loginModel.dataField.username='%s';data.loginModel.dataField.password='%s';data.passwordInputVal='password';data.$loginBtn.click();});})()""", account.first, account.second), _ -> {
                    })));
                else if (Pattern.compile("://appgw.sysu.edu.cn/").matcher(link).find()) {
                    web.stopLoading();
                    web.loadUrl(url.replace(".sysu.edu.cn/", "-443.webvpn.sysu.edu.cn/"));
                } else if (Pattern.compile("://cas.*?sysu.edu.cn/login/mfaLogin.html").matcher(link).find()) {
                    cookie.setCookie("https://cas.sysu.edu.cn", "device_trust_Cookie=true; Path=/esc-sso; Domain=cas.sysu.edu.cn;");
                    try {
                        web.loadUrl(toStringOrDefault(Uri.parse(URLDecoder.decode(link, "utf-8")).getQueryParameter("appUrl")));
                    } catch (UnsupportedEncodingException _) {
                    }
                } else if (preference.isPC())
                    view.evaluateJavascript("document.querySelector('meta[name=\"viewport\"]').setAttribute('content', 'width=1024px, initial-scale=' + (document.documentElement.clientWidth / 1024));", null);
                js.searchJS(link, true).forEach(a -> view.evaluateJavascript(a.getString("script"), null));
               */
                super.onPageFinished(view, link);
            }
        });
        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onJsConfirm(WebView view, String url, String message, JsResult result) {
                new MaterialAlertDialogBuilder(HtmlViewActivity.this).setMessage(message).setPositiveButton(R.string.confirm, (_, _) -> result.confirm()).create().show();
                return true;
            }
            
            @Override
            public boolean onJsAlert(WebView view, String url, String message, JsResult result) {
                new MaterialAlertDialogBuilder(HtmlViewActivity.this).setMessage(message).setPositiveButton(R.string.confirm, (_, _) -> result.confirm()).create().show();
                return true;
            }
            
            @Override
            public boolean onCreateWindow(WebView view, boolean isDialog, boolean isUserGesture, Message resultMsg) {
                WebView newWebView = new WebView(HtmlViewActivity.this);
                newWebView.setWebViewClient(new WebViewClient() {
                    @Override
                    public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                        web.loadUrl(String.valueOf(request.getUrl()));
                        newWebView.destroy();
                        return super.shouldOverrideUrlLoading(view, request);
                    }
                });
                WebView.WebViewTransport transport = (WebView.WebViewTransport) resultMsg.obj;
                transport.setWebView(newWebView);
                resultMsg.sendToTarget();
                return true;
            }
            
            @Override
            public void onReceivedTitle(WebView view, String title) {
                binding.toolbar.setTitle(title);
                binding.toolbar.setSubtitle(view.getUrl());
                super.onReceivedTitle(view, title);
            }
            
            @Override
            public void onReceivedIcon(WebView view, Bitmap icon) {
                binding.toolbar.setLogo(new BitmapDrawable(getResources(), icon));
                binding.toolbar.setLogoAdjustViewBounds(true);
                binding.toolbar.setLogoScaleType(ImageView.ScaleType.FIT_CENTER);
                super.onReceivedIcon(view, icon);
            }
            
        });
    }
}