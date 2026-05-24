package com.sysu.edu.api;


import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.DisplayMetrics;
import android.view.View;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import com.sysu.edu.browser.BrowserActivity;

public class Params {

    final FragmentActivity activity; // 关联的 FragmentActivity 对象
//    private AccountViewModel accountViewModel;
    Fragment fragment; // 关联的 Fragment 对象
    Runnable afterLogin; // 登录成功后的回调 Runnable 对象
    private ContextUtil contextUtil;

    /**
     * 构造函数，用于初始化 Params 对象
     *
     * @param activity 关联的 FragmentActivity 对象
     */
    public Params(FragmentActivity activity) {
        this.activity = activity;
        contextUtil = new ContextUtil(activity);
//        accountViewModel = new ViewModelProvider(activity).get(AccountViewModel.class);
    }

    /**
     * 构造函数，用于初始化 Params 对象
     *
     * @param fragment 关联的 Fragment 对象
     */
    public Params(Fragment fragment) {
        this.fragment = fragment;
        contextUtil = new ContextUtil(fragment.requireContext());
        this(fragment.requireActivity());
//        accountViewModel = new ViewModelProvider(fragment).get(AccountViewModel.class);
    }

    /**
     * 设置登录回调
     *
     * @param afterLogin 登录成功后的回调 Runnable 对象
     */
    public void setCallback(Runnable afterLogin) {
        this.afterLogin = afterLogin;
    }

    /**
     * 将 dp 值转换为 px 值
     *
     * @param dps dp 值
     * @return 对应的 px 值
     */
    public int dpToPx(int dps) {
        return contextUtil.dpToPx(dps);
    }


    /**
     * 获取屏幕宽度
     *
     * @return 屏幕宽度（px）
     */
    public int getWidth() {
        DisplayMetrics dm = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getRealMetrics(dm);
        return dm.widthPixels;
    }

    /**
     * 获取列数，根据屏幕宽度动态调整，手机屏幕为一列，以此类推
     *
     * @return 列数（1、2 或 3）
     */
    public int getColumn() {
        return (getWidth() < dpToPx(540)) ? 1 : (getWidth() < dpToPx(900)) ? 2 : 3;
    }

    /**
     * 获取 Cookie
     *
     * @return Cookie
     */
//    public String getCookie() {
//        return contextUtil.getCookie();
//    }

    public ContextUtil getContextUtil() {
        return contextUtil;
    }

    public Context getContext(){
        return getContextUtil().getContext();
    }

    //    /**
//     * 获取 Authorization
//     *
//     * @return Authorization
//     */
//    public String getAuthorization() {
//        return contextUtil.getAuthorization();
//    }

//    public void setAuthorization(String auth) {
//        contextUtil.setAuthorization(auth);
//    }

//    /**
//     * 获取用户名
//     *
//     * @return 用户名
//     */
//    public String getUserName() {
//        return contextUtil.getUserName();
//    }
//
//    /**
//     * 设置用户名
//     *
//     * @param userName 用户名
//     */
//    public void setUserName(String userName) {
//        contextUtil.setUserName(userName);
//    }
//
//    /**
//     * 获取密码
//     *
//     * @return 密码
//     */
//    public String getPassword() {
//        return contextUtil.getPassword();
//    }
//
//    /**
//     * 设置密码
//     *
//     * @param password 密码
//     */
//    public void setPassword(String password) {
//        contextUtil.setPassword(password);
//    }

//    /**
//     * 获取 SharedPreferences 对象
//     *
//     * @return SharedPreferences 对象
//     */
//    public SharedPreferences getSharedPreferences() {
//        return contextUtil.getSharedPreferences();
//    }

//    /**
//     * 获取 Token
//     *
//     * @return Token
//     */
//    public String getToken() {
//        return contextUtil.getToken();
//    }

    /**
     * 获取是否为开发者
     *
     * @return 是否为开发者
     */
    public boolean isDeveloper() {
        return contextUtil.isDeveloper();
    }

    /**
     * 设置是否为开发者
     *
     * @param developer 是否为开发者
     */
    public void setDeveloper(boolean developer) {
        contextUtil.setDeveloper(developer);
    }

    /**
     * 打开浏览器
     *
     * @param url 要打开的 URL
     * @return 点击事件监听器
     */
    public View.OnClickListener browse(String url) {
        return (View v) -> v.getContext().startActivity(new Intent(activity, BrowserActivity.class).setData(Uri.parse(url)));
    }

    /**
     * 复制文本到剪贴板
     *
     * @param tag  剪贴板标签
     * @param text 要复制的文本
     */
    public void copy(String tag, String text) {
        contextUtil.copy(tag, text);
    }

    /**
     * 显示 Toast 消息
     *
     * @param resource 字符串资源 ID
     */
    public void toast(int resource) {
        contextUtil.toast(resource);
    }

    /**
     * 显示 Toast 消息
     *
     * @param toast 要显示的文本
     */
    public void toast(String toast) {
        contextUtil.toast(toast);
    }

//    /**
//     * 获取登录模式
//     *
//     * @return 登录模式（"0"：弹窗登录；"1"：主页弹窗、其他跳转登录；"2"：跳转登录；"3"：自动登录）
//     */
//    public String getLoginMode() {
//        return contextUtil.getLoginMode();
//    }

    /**
     * 跳转登录页面
     *
     * @param url 登录 URL，建议使用 TargeterURL 中的默认登录 URL
     */
    public void gotoLogin(String url) {
        contextUtil.login(url, afterLogin);
    }
}
