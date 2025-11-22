package com.sysu.edu.extra;

import android.annotation.SuppressLint;
import android.os.Bundle;
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
import androidx.lifecycle.ViewModelProvider;

import java.util.Objects;
import java.util.regex.Pattern;

public class LoginWebFragment extends Fragment {
    @SuppressLint("SetJavaScriptEnabled")
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        WebView web = new WebView(requireContext());
        LoginViewModel model = new ViewModelProvider(requireActivity()).get(LoginViewModel.class);
        model.getUrl().observe(getViewLifecycleOwner(), web::loadUrl);
        //System.out.println(CookieManager.getInstance().getCookie("https://portal.sysu.edu.cn/#/index"));
        web.setWebViewClient(new WebViewClient(){
            @Override
            public void onPageFinished(WebView view, String url) {
                //boolean reloadCap = Objects.equals(sessionId, CookieManager.getInstance().getCookie(url));
                model.setSessionID(CookieManager.getInstance().getCookie(url));
                if(Pattern.compile("//cas.sysu.edu.cn/selfcare").matcher(url).find()){
                   // model.setCookie(CookieManager.getInstance().getCookie(Objects.requireNonNull(model.getTarget().getValue())));
                   // model.setLogin(true);
                    //System.out.println(CookieManager.getInstance().getCookie(Objects.requireNonNull(model.getTarget().getValue())));
                    view.loadUrl(Objects.requireNonNull(model.getTarget().getValue()));
                }
                if(Pattern.compile(Objects.requireNonNull(model.getTarget().getValue())).matcher(url).find()){
                    model.setCookie(CookieManager.getInstance().getCookie(url));
                    model.setLogin(true);
                }
                //web.evaluateJavascript("var script=document.createElement('script');script.src='https://cdn.jsdelivr.net/npm/eruda';document.body.appendChild(script);script.onload=function(){eruda.init()};", s -> {});
            }
//            @Override
//            public void onLoadResource(WebView view, String url) {
//               // view.evaluateJavascript("document.querySelector('meta[name=\"viewport\"]').setAttribute('content', 'width=1024px, initial-scale=' + (document.documentElement.clientWidth / 1024));", null);
//            }
        });
        WebSettings webSettings = web.getSettings();
        webSettings.setDomStorageEnabled(true);
        webSettings.setDatabaseEnabled(true);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setUseWideViewPort(true);
        webSettings.setBlockNetworkImage(false);
        //webSettings.setUserAgentString("Windows");
        webSettings.setCacheMode(WebSettings.LOAD_NO_CACHE);
        webSettings.setJavaScriptEnabled(true);
        webSettings.supportZoom();
        webSettings.setSupportZoom(true);
        webSettings.setDisplayZoomControls(false);
        webSettings.setBuiltInZoomControls(false);
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
        return web;
    }
}
