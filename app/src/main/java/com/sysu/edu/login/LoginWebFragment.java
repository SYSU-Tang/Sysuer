package com.sysu.edu.login;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProvider;

import com.sysu.edu.api.TargetUrl;

import java.util.Objects;
import java.util.regex.Pattern;

public class LoginWebFragment extends Fragment {
    @SuppressLint("SetJavaScriptEnabled")
    @NonNull
    public static WebView getWebView(@NonNull FragmentActivity activity, LoginViewModel model, Runnable afterLoad) {
        WebView web = new WebView(activity);
        model.getUrl().observe(activity, web::loadUrl);
        web.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                //boolean reloadCap = Objects.equals(sessionId, CookieManager.getInstance().getCookie(url));
                //model.setSessionID(CookieManager.getInstance().getCookie(url));
                if (Pattern.compile("//cas.sysu.edu.cn/selfcare").matcher(url).find()) {
                    // model.setCookie(CookieManager.getInstance().getCookie(Objects.requireNonNull(model.getTarget().getValue())));
                    // model.setLogin(true);
                    //System.out.println(CookieManager.getInstance().getCookie(Objects.requireNonNull(model.getTarget().getValue())));
                    view.loadUrl(Objects.requireNonNull(model.getTarget().getValue()));
                }
                if (Pattern.compile(TargetUrl.LOGIN).matcher(url).find()) {
                    model.setLogin(false);
                }
                String element = "";
                if (Pattern.compile("//jwxt.sysu.edu.cn/jwxt/#/").matcher(url).find()) {
                    element = ".ant-btn.ant-btn-primary";
                } else if (Pattern.compile("//pay.sysu.edu.cn/#/").matcher(url).find()) {
                    element = ".el-button.login_btns.btn-netIdLogin.el-button--default.is-plain";
                } else if (Pattern.compile("//pjxt.sysu.edu.cn/").matcher(url).find()) {
                    element = ".log-g-iddl";
                } else if (Pattern.compile("portal.sysu.edu.cn/newClient/#/login").matcher(url).find()) {
                    element = ".ant-btn.index-submit-3jXSy.ant-btn-primary.ant-btn-lg";
                }
                if (!element.isEmpty()) {
                    web.evaluateJavascript("(function(){var needLogin = document.querySelector('" + element + "');if(needLogin!=null){needLogin.click();};return needLogin!=null;})()", s -> {
                        if (Boolean.parseBoolean(s)) {
                            model.setLogin(false);
                        }
                        new Handler(Looper.getMainLooper()).postDelayed(() -> {
                            model.setCookie(CookieManager.getInstance().getCookie(url));
                            model.setLogin(true);
                        }, 500);
                    });
                }else if (Pattern.compile(Objects.requireNonNull(model.getTarget().getValue())).matcher(url).find()) {
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        model.setCookie(CookieManager.getInstance().getCookie(url));
                        model.setLogin(true);
                    }, 500);
                    return;
                }
                model.setLogin(false);
                if (afterLoad != null) {
                    new Handler(Looper.getMainLooper()).postDelayed(afterLoad, 500);
                }
                //ar script=document.createElement('script');script.src='https://cdn.jsdelivr.net/npm/eruda';document.body.appendChild(script);script.onload=function(){eruda.init()};", s -> {});
            }
//            @Override
//            public void onLoadResource(WebView view, String url) {
//               // view.evaluateJavascript("document.querySelector('meta[name=\"viewport\"]').setAttribute('content', 'width=1024px, initial-scale=' + (document.documentElement.clientWidth / 1024));", null);
//            }

            /*@Override
            public void onLoadResource(WebView view, String url) {
                super.onLoadResource(view, url);
//                if (Pattern.compile("/api/sso/cas/login?pattern=student-login$").matcher(url).find()) {
//                    model.setCookie(CookieManager.getInstance().getCookie(view.getUrl()));
//                    model.setLogin(true);
//                }
            }*/
        });
        WebSettings webSettings = web.getSettings();
        webSettings.setDomStorageEnabled(true);
        webSettings.setDatabaseEnabled(true);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setUseWideViewPort(true);
        webSettings.setBlockNetworkImage(false);
        webSettings.setUserAgentString("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko)");
        webSettings.setCacheMode(WebSettings.LOAD_NO_CACHE);
        webSettings.setJavaScriptEnabled(true);
        webSettings.supportZoom();
        webSettings.setSupportZoom(true);
        webSettings.setDisplayZoomControls(false);
        webSettings.setBuiltInZoomControls(false);
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
        return web;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        return getWebView(requireActivity(), new ViewModelProvider(requireActivity()).get(LoginViewModel.class), null);
    }
}
