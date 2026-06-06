package com.sysu.edu.api;

import static android.text.TextUtils.isEmpty;
import static com.sysu.edu.api.CommonUtil.toStringOrDefault;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputLayout;
import com.sysu.edu.R;
import com.sysu.edu.databinding.DialogAccountBinding;

import java.util.concurrent.ExecutionException;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.disposables.CompositeDisposable;

public class ContextUtil {
    private final Context context;
    private final SharedPreferences sharedPreferences;
    private final LoginManager loginManager;
    private final AccountManager accountManager;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final CompositeDisposable disposable = new CompositeDisposable();
    private DialogAccountBinding binding;
    private AlertDialog dialog;
    
    public ContextUtil(Context context) {
        this.context = context;
        sharedPreferences = context.getSharedPreferences("privacy", Context.MODE_PRIVATE);
        loginManager = new LoginManager(context);
        accountManager = AccountManager.getInstance(context.getApplicationContext());
        if (!isEmpty(getUserName()) && !isEmpty(getPassword()))
            disposable.add(accountManager.setAccountAsync("sysu.edu.cn", getUserName(), getPassword(), true).subscribe(() -> sharedPreferences.edit().remove("username").remove("password").apply()));
    }
    
    public Context getContext() {
        return context;
    }
    
    public int getColorFromAttr(int attr) {
        TypedValue typedValue = new TypedValue();
        context.getTheme().resolveAttribute(attr, typedValue, true);
        return typedValue.data;
    }
    
    /**
     * 将 dp 值转换为 px 值
     *
     * @param dps dp 值
     * @return 对应的 px 值
     */
    public int dpToPx(int dps) {
        return Math.round(context.getResources().getDisplayMetrics().density * dps);
    }
    
    /**
     * 获取用户名
     *
     * @return 用户名
     */
    public String getUserName() {
        return sharedPreferences.getString("username", "");
    }
//
//    /**
//     * 设置用户名
//     *
//     * @param userName 用户名
//     */
//    public void setUserName(String userName) {
//        sharedPreferences.edit().putString("username", userName).apply();
//    }
//
    
    /**
     * 获取密码
     *
     * @return 密码
     */
    public String getPassword() {
        return sharedPreferences.getString("password", "");
    }
//
//    /**
//     * 设置密码
//     *
//     * @param password 密码
//     */
//    public void setPassword(String password) {
//        sharedPreferences.edit().putString("password", password).apply();
//    }

//    /**
//     * 获取 SharedPreferences 对象
//     *
//     * @return SharedPreferences 对象
//     */
//    public SharedPreferences getSharedPreferences() {
//        return sharedPreferences;
//    }
    
    /**
     * 获取是否为开发者
     *
     * @return 是否为开发者
     */
    public boolean isDeveloper() {
        return sharedPreferences.getBoolean("developer", false);
    }
    
    /**
     * 设置是否为开发者
     *
     * @param developer 是否为开发者
     */
    public void setDeveloper(boolean developer) {
        sharedPreferences.edit().putBoolean("developer", developer).apply();
    }
    
    /**
     * 复制文本到剪贴板
     *
     * @param tag  剪贴板标签
     * @param text 要复制的文本
     */
    public void copy(String tag, String text) {
        ClipboardManager clip = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        clip.setPrimaryClip(ClipData.newPlainText(tag, text));
    }
    
    /**
     * 显示 Toast 消息
     *
     * @param resource 字符串资源 ID
     */
    public void toast(int resource) {
        Toast.makeText(context, resource, Toast.LENGTH_LONG).show();
    }
    
    /**
     * 显示 Toast 消息
     *
     * @param toast 要显示的文本
     */
    public void toast(String toast) {
        Toast.makeText(context, toast, Toast.LENGTH_LONG).show();
    }
    
    /**
     * 登录
     *
     * @param service    登录 URL,建议使用 TargeterURL 中的默认登录 URL
     * @param afterLogin 登录成功后的回调 Runnable 对象
     *
     */
    
    public void loginForUrl(String service, String host, Runnable afterLogin) {
        disposable.add(accountManager.getActiveAccountAsync(host).subscribe(
                activeAccount -> {
                    System.out.println("activeAccount " + activeAccount);
                    System.out.println(service);
                    if (!isEmpty(activeAccount.first) && !isEmpty(activeAccount.second) && !isEmpty(service)) {
                        try {
                            loginManager.setOnLoginListener(new LoginManager.LoginListener() {
                                @Override
                                public void onSuccess() {
                                    if (afterLogin != null) afterLogin.run();
                                }
                                
                                @Override
                                public void onError(String code, String message) {
                                    System.out.println("LoginManager onError " + code + " " + message);
                                    if ("SSO10002".equals(code) || "30506".equals(code))
                                        changeAccount(service, host, afterLogin);
                                    else
                                        handler.post(() -> toast(toStringOrDefault(message)));
                                }
                            });
                            loginManager.login(activeAccount.first, activeAccount.second, service);
                        } catch (ExecutionException | InterruptedException e) {
                            Log.e("ContextUtil", "loginForUrl: ", e);
                        }
                    } else changeAccount(service, host, afterLogin);
                }));
    }
    
    public void login(String url, Runnable afterLogin) {
        loginForUrl(url, "sysu.edu.cn", afterLogin);
    }
    
    public void changeAccount(String url, String host, Runnable afterLogin) {
        if (context instanceof Activity activity && !activity.isFinishing() && !activity.isDestroyed()) {
            if (binding == null) {
                binding = DialogAccountBinding.inflate(LayoutInflater.from(context));
                binding.password.editLayout.setEndIconMode(TextInputLayout.END_ICON_PASSWORD_TOGGLE);
            }
            if (dialog == null)
                dialog = new MaterialAlertDialogBuilder(context)
                        .setView(binding.getRoot())
                        .setTitle(R.string.privacy)
                        .setPositiveButton(android.R.string.ok, (_, _) -> {
                            Editable username = binding.username.edit.getText();
                            Editable password = binding.password.edit.getText();
                            if (isEmpty(username) || isEmpty(password))
                                toast(R.string.username_password_warning);
                            else
                                disposable.add(accountManager.setAccountAsync(host, username.toString(), password.toString(), true)
                                        .subscribe( () -> loginForUrl(url, host, afterLogin)));
                        })
                        .setNegativeButton(android.R.string.cancel, null)
                        .create();
            disposable.add(accountManager.getActiveAccountAsync(host).subscribe(account -> activity.runOnUiThread(() -> {
                if (!isEmpty(account.first) && !isEmpty(account.second)) {
                    binding.password.edit.setText(account.second);
                    binding.username.edit.setText(account.first);
                }
                dialog.show();
            })));
        }
    }
    
    public AccountManager getAccountManager() {
        return accountManager;
    }
    
    public void dispose() {
        disposable.dispose();
    }
}
