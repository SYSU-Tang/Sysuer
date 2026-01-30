package com.sysu.edu.api;


import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.icu.text.SimpleDateFormat;
import android.net.Uri;
import android.util.DisplayMetrics;
import android.view.View;
import android.webkit.WebView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityOptionsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;

import com.google.android.material.snackbar.Snackbar;
import com.sysu.edu.MainActivity;
import com.sysu.edu.R;
import com.sysu.edu.academic.BrowserActivity;
import com.sysu.edu.login.LoginActivity;
import com.sysu.edu.login.LoginViewModel;
import com.sysu.edu.login.LoginWebFragment;

import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class Params {
    static final Calendar c = Calendar.getInstance();
    final SharedPreferences sharedPreferences;
    final FragmentActivity activity;
    ActivityResultLauncher<Intent> launch;
    Runnable afterLogin;

    public Params(FragmentActivity activity) {
        this.activity = activity;
        sharedPreferences = activity.getSharedPreferences("privacy", Context.MODE_PRIVATE);

    }

    public static int getYear() {
        return c.get(Calendar.YEAR);
    }

    public static int getMonth() {
        return c.get(Calendar.MONTH);
    }

    public static int getDay() {
        return c.get(Calendar.DAY_OF_MONTH);
    }

    public static String toDate() {
        return toDate(Calendar.getInstance());
    }

    public static String getDateTime() {
        return toDate(Calendar.getInstance());
    }

    public static String toDate(Calendar calendar) {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.getTime());
    }

    public static String toDate(Date date) {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(date);
    }

    public static String getDateTime(Calendar calendar) {
        return getDateTime(calendar.getTime());
    }

    public static String getDateTime(Date date) {
        return new SimpleDateFormat("yyyy-MM-dd hh:mm:ss", Locale.getDefault()).format(date);
    }

    public static Calendar getFirstOfMonth() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_MONTH, c.getActualMinimum(Calendar.DAY_OF_MONTH));
        return calendar;
    }

    public static Calendar getFirstOfMonth(int month) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.MONTH, month);
        calendar.set(Calendar.DAY_OF_MONTH, c.getActualMinimum(Calendar.DAY_OF_MONTH));
        return calendar;
    }

    public static Calendar getEndOfMonth() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_MONTH, c.getActualMaximum(Calendar.DAY_OF_MONTH));
        return calendar;
    }

    public static Calendar getEndOfMonth(int month) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.MONTH, month);
        calendar.set(Calendar.DAY_OF_MONTH, c.getActualMaximum(Calendar.DAY_OF_MONTH));
        return calendar;
    }

  /*  public void setCallback(ActivityResultCallback<ActivityResult> callback) {
        launch = activity.registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), callback);
    }

    public void setCallback(Fragment fragment, ActivityResultCallback<ActivityResult> callback) {
        launch = fragment.registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), callback);
    }*/

    public void setCallback(Runnable afterLogin) {
        this.afterLogin = afterLogin;
        this.launch = activity.registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), o -> {
            if (o.getResultCode() == FragmentActivity.RESULT_OK) {
                afterLogin.run();
            }
        });
    }

    public void setCallback(Fragment fragment, Runnable afterLogin) {
        this.afterLogin = afterLogin;
        this.launch = fragment.registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), o -> {
            if (o.getResultCode() == FragmentActivity.RESULT_OK) {
                afterLogin.run();
            }
        });
    }

    public int dpToPx(int dps) {
        return Math.round(activity.getResources().getDisplayMetrics().density * dps);
    }

    public int getWidth() {
        DisplayMetrics dm = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getRealMetrics(dm);
        return dm.widthPixels;
    }

    public int getColumn() {
        return (getWidth() < dpToPx(540)) ? 1 : (getWidth() < dpToPx(900)) ? 2 : 3;
    }

    public String getCookie() {
        return sharedPreferences.getString("Cookie", "");
    }

    public String getAuthorization() {
        return sharedPreferences.getString("authorization", "");
    }

    public String getAccount() {
        return sharedPreferences.getString("username", "");
    }

    public String getPassword() {
        return sharedPreferences.getString("password", "");
    }

    public boolean isFirstLaunch() {
        return sharedPreferences.getBoolean("isFirstLaunch", true);
    }

    public void setIsFirstLaunch(boolean i) {
        sharedPreferences.edit().putBoolean("isFirstLaunch", i).apply();
    }

    public String getToken() {
        return sharedPreferences.getString("token", "");
    }

    public boolean isDeveloper() {
        return sharedPreferences.getBoolean("developer", false);
    }


    public View.OnClickListener browse(String url) {
        return (View v) -> v.getContext().startActivity(new Intent(activity, BrowserActivity.class).setData(Uri.parse(url)));
    }

    public void copy(String a, String b) {
        ClipboardManager clip = (ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
        clip.setPrimaryClip(ClipData.newPlainText(a, b));
    }

    public void toast(int resource) {
        toast(activity.getString(resource));
    }

    public void toast(String toast) {
        Toast.makeText(activity, toast, Toast.LENGTH_LONG).show();
    }

    public String getLoginMode() {
        return PreferenceManager.getDefaultSharedPreferences(activity).getString("loginMode", "2");
    }

    public void gotoLogin(View view, @Nullable String url) {
        Intent intent = new Intent(activity, LoginActivity.class);
        if (url != null) {
            intent.putExtra("url", url);
        }
        switch (getLoginMode()) {
            case "0":
                Snackbar.make(view, R.string.login_warning, Snackbar.LENGTH_LONG).setAction(R.string.login, v -> launch.launch(intent, ActivityOptionsCompat.makeSceneTransitionAnimation(activity, view, "miniapp"))).show();
                break;
            case "1":
                if (activity instanceof MainActivity) {
                    Snackbar.make(view, R.string.login_warning, Snackbar.LENGTH_LONG).setAction(R.string.login, v -> launch.launch(intent, ActivityOptionsCompat.makeSceneTransitionAnimation(activity, view, "miniapp"))).show();
                } else {
                    launch.launch(intent, ActivityOptionsCompat.makeSceneTransitionAnimation(activity, view, "miniapp"));
                }
                break;
            case "3":
                String account = getAccount();
                String password = getPassword();
                if (account.isEmpty() || password.isEmpty()) {
                    toast(R.string.require_netid_password);
                    launch.launch(intent, ActivityOptionsCompat.makeSceneTransitionAnimation(activity, view, "miniapp"));
                    break;
                }
                toast(R.string.logging_in);
                LoginViewModel model = new ViewModelProvider(activity).get(LoginViewModel.class);
                WebView web = LoginWebFragment.getWebView(activity, model, () -> model.setUrl(String.format("""
                        javascript:(function(){\
                        function waitElement(selector, callback) {\
                        const element = document.querySelector(selector);\
                        if (element) {callback();}else{setTimeout(() => {waitElement(selector,callback);}, 100);}}\
                        waitElement('.para-widget-account-psw', () => {\
                        var component=document.querySelector('.para-widget-account-psw');var data=component[Object.keys(component).filter(k => k.startsWith('jQuery') && k.endsWith('2'))[0]].widget_accountPsw;data.loginModel.dataField.username='%s';data.loginModel.dataField.password='%s';data.passwordInputVal='password';data.$loginBtn.click();});})()""", account, password)));
                LoginActivity.initModel(activity, model, url, () -> {
                    afterLogin.run();
                    web.destroy();
                    toast(R.string.login_successfully);
                });
//                ((FrameLayout)activity.findViewById(android.R.id.content)).addView(web);
                break;
            case "2":
            default:
                launch.launch(intent, ActivityOptionsCompat.makeSceneTransitionAnimation(activity, view, "miniapp"));
                break;
        }
    }

}
