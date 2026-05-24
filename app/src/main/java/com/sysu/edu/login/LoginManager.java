package com.sysu.edu.login;

import static android.text.TextUtils.isEmpty;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.webkit.CookieManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import com.sysu.edu.api.Params;
import com.sysu.edu.api.TargetUrl;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LoginManager {

    static int time = 0;

    /**
     * 初始化登录模型
     *
     * @param activity   活动上下文
     * @param model      登录视图模型
     * @param target     目标登录网址
     * @param afterLogin 登录成功后的回调
     */
    public static void initLoginModel(FragmentActivity activity, LoginViewModel model, String target, Runnable afterLogin) {
//        Params params = new Params(activity);
//        model.getPassword().observe(activity, params::setPassword);
//        model.getAccount().observe(activity, params::setUserName);
//        model.setAccount(params.getUserName());
//        model.setPassword(params.getPassword());
//        model.setTarget(target);
//        model.setUrl(TargetUrl.LOGIN);
//        model.getLogin().observe(activity, b -> {
//            if (b) {
//                SharedPreferences.Editor edit = params.getSharedPreferences().edit();
//                String cookie = model.getCookie().getValue();
//                Matcher token = Pattern.compile("ibps-1.0.1-token=(.+?);").matcher(cookie + ";");
//                Matcher authorization = Pattern.compile("authorization=(.+?);").matcher(cookie + ";");
//                if (token.find()) edit.putString("token", token.group(1));
//                if (authorization.find())
//                    edit.putString("authorization", Objects.requireNonNull(authorization.group(1)).replace("%20", " "));
//                edit.putString("Cookie", cookie);
//                edit.apply();
//                if (afterLogin != null)
//                    afterLogin.run();
//            }
//        });
    }

    /**
     * 初始化登录 WebView
     *
     * @param activity 活动上下文
     * @param model    登录视图模型
     * @return 初始化好的 WebView
     */
    @SuppressLint("SetJavaScriptEnabled")
    public static WebView initLoginWebView(@NonNull FragmentActivity activity, LoginViewModel model, boolean isAutoLogin) {
        WebView web = new WebView(activity);
        Params params = new Params(activity);
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        cookieManager.setAcceptThirdPartyCookies(web, true);
        cookieManager.acceptThirdPartyCookies(web);
        Handler handler = new Handler(Looper.getMainLooper());
        model.getUrl().observe(activity, web::loadUrl);
        time = 0;
        web.setWebViewClient(new WebViewClient() {

            @Override
            public void onPageFinished(WebView view, String url) {
                time++;
                if (time >= 100) {
                    web.stopLoading();
                    time = 0;
                    return;
                }
                System.out.println("结束加载" + url);
                if (Pattern.compile("//cas.+?sysu\\.edu\\.cn/esc-sso/login/page").matcher(url).find()) {
                    //System.out.println("登录中");
                    model.setLogin(false);
                    if (isAutoLogin)
//                        web.evaluateJavascript(String.format("""
//                                (function(){\
//                                function waitElement(selector, callback) {\
//                                const element = document.querySelector(selector);\
//                                if (element){callback();}else{setTimeout(() => {waitElement(selector,callback);}, 100);}}\
//                                waitElement('.para-widget-account-psw', () => {\
//                                var component=document.querySelector('.para-widget-account-psw');
//                                var data=component[Object.keys(component).filter(k => k.startsWith('jQuery') && k.endsWith('2'))[0]].widget_accountPsw;
//                                data.loginModel.dataField.username='%s';
//                                data.loginModel.dataField.password='%s';
//                                data.passwordInputVal='password';
//                                data.$loginBtn.click();});})()""", params.getUserName(), params.getPassword()), _ -> {
//                        });
                    return;
                }
                if (Pattern.compile("//cas.+?sysu\\.edu\\.cn/selfcare/#").matcher(url).find()) {
                    view.loadUrl(Objects.requireNonNull(model.getTarget().getValue()));
                    return;
                }
                String element;
                if (Pattern.compile("//jwxt.sysu.edu.cn/jwxt/#/login").matcher(url).find()) {
                    element = ".ant-btn.ant-btn-primary.ant-btn-block";
                } else if (Pattern.compile("//jwxt.sysu.edu.cn/jwxt/#/student").matcher(url).find()) {
                    element = ".ant-btn.ant-btn-primary";
                } else if (Pattern.compile("//pay.sysu.edu.cn/#/$").matcher(url).find()) {
                    element = ".el-button.login_btns.btn-netIdLogin.el-button--default.is-plain";
                } else if (Pattern.compile("//pjxt.sysu.edu.cn/").matcher(url).find()) {
                    element = ".log-g-iddl";
                } else if (Pattern.compile("portal.sysu.edu.cn/newClient/#/login").matcher(url).find()) {
                    element = ".ant-btn.index-submit-3jXSy.ant-btn-primary.ant-btn-lg";
                } else if (Pattern.compile("tice.sysu.edu.cn").matcher(url).find()) {
                    element = "#netid-login";
                } else {
                    element = "";
                }

                if (isEmpty(element)) {
                    if (Pattern.compile(Objects.requireNonNull(model.getTarget().getValue())).matcher(url).find())
                        handler.postDelayed(() -> {
                            System.out.println("登录成功");
//                            System.out.println("Cookie：" + cookieManager.getCookie(url));
                            model.setCookie(cookieManager.getCookie(url));
                            System.out.println("登录状态：" + model.getLogin().getValue());
                            if (!Boolean.TRUE.equals(model.getLogin().getValue()))
                                model.setLogin(true);
                        }, 500);
                } else {
                    handler.postDelayed(() -> web.evaluateJavascript("(function(){var needLogin = document.querySelector('" + element + "');if(needLogin!=null){needLogin.click();};return needLogin!=null;})()", needLogin -> {
                        System.out.println(needLogin);
                        if (!Boolean.parseBoolean(needLogin)) {
                            model.setCookie(cookieManager.getCookie(url));
                            if (!Boolean.TRUE.equals(model.getLogin().getValue()))
                                model.setLogin(true);
                        }
                    }), 500);
                }
            }
        });
        WebSettings webSettings = web.getSettings();
        webSettings.setSupportMultipleWindows(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setDatabaseEnabled(true);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setLoadsImagesAutomatically(true);
        webSettings.setUseWideViewPort(true);
        webSettings.setBlockNetworkImage(true);
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

}