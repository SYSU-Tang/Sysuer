package com.sysu.edu;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.Html;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.navigation.NavController;
import androidx.navigation.NavGraph;
import androidx.navigation.NavInflater;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;
import androidx.preference.PreferenceManager;

import com.alibaba.fastjson2.JSONObject;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.navigation.NavigationBarView;
import com.sysu.edu.api.Params;
import com.sysu.edu.api.SysuerPreferenceManager;
import com.sysu.edu.databinding.ActivityMainBinding;
import com.sysu.edu.extra.SettingActivity;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {
    Handler handler;
    long downloadId;
    File file;
    ActivityResultLauncher<Intent> detailLauncher;
    BroadcastReceiver receiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        NavController navController = ((NavHostFragment) Objects.requireNonNull(getSupportFragmentManager().findFragmentById(R.id.dashboard_scroll))).getNavController();
        NavGraph g = new NavInflater(this, navController.getNavigatorProvider()).inflate(R.navigation.main_navigation);
        if (savedInstanceState == null) {
            g.setStartDestination(new int[]{R.id.navigation_activity, R.id.navigation_service, R.id.navigation_account}[Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(this).getString("home", "0"))]);
        }
//        if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
//            //Log.d(TAG, "Shizuku 权限已授予");
//        } else {
//            Shizuku.requestPermission(0);
//            Shizuku.addRequestPermissionResultListener((requestCode, grantResult) -> {
//                if (grantResult == PackageManager.PERMISSION_GRANTED) {
//                    // 去连接服务（前提是Shizuku服务是正常并且已授权）
//                    Shizuku.bindUserService(new Shizuku.UserServiceArgs(new ComponentName("com.sysu.edu", UserService.class.getName()))
//                            .daemon(false)
//                            .processNameSuffix("service")
//                            .debuggable(BuildConfig.DEBUG)
//                            .version(2024), new ServiceConnection() {
//                        @Override
//                        public void onServiceConnected(ComponentName componentName, IBinder binder) {
//                            if (binder != null && binder.pingBinder()) {
//                                IUserService mUserService = IUserService.Stub.asInterface(binder);
//
//                                // executeWifiCommand();
//                            } else {
//                                //Log.i(TAG, " Shizuku binder 为 null 或者 binder.pingBinder() 有问题");
//                            }
//                        }
//
//                        @Override
//                        public void onServiceDisconnected(ComponentName componentName) {
//
//                        }
//                    });
//                    //Toast.makeText(MainActivity.this, "授权成功", Toast.LENGTH_SHORT).show();
//                } else {
//                    //Toast.makeText(MainActivity.this, "授权失败", Toast.LENGTH_SHORT).show();
//                }
//            });
//        }
        navController.setGraph(g);
        NavigationUI.setupWithNavController((NavigationBarView) binding.navView, navController);
        SysuerPreferenceManager spm = new SysuerPreferenceManager(this);
        if (spm.getUpdate()) {
            checkUpdate();
        }
        if (spm.getIsFirstLaunch()||!spm.getIsAgree()){
            new MaterialAlertDialogBuilder(this).setTitle("用户协议和隐私政策")
                    .setMessage(Html.fromHtml("请阅读<a href=\"https://sysu-tang.github.io/#/zh-cn/agreement/%E7%94%A8%E6%88%B7%E5%8D%8F%E8%AE%AE\">用户协议</a>和<a href=\"https://sysu-tang.github.io/#/zh-cn/agreement/%E9%9A%90%E7%A7%81%E6%94%BF%E7%AD%96\">隐私政策</a>",Html.FROM_HTML_MODE_LEGACY))
                    .setPositiveButton("同意", (dialogInterface, i) -> {
                        spm.setIsFirstLaunch(false);
                        spm.setIsAgree(true);
                    })
                    .setNegativeButton("不同意", (dialogInterface, i) -> {
                        spm.setIsFirstLaunch(false);
                        spm.setIsAgree(false);
                        supportFinishAfterTransition();
                    })
                    .create()
                    .show();

        }
        Params params = new Params(this);
        handler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                super.handleMessage(msg);
                //System.out.println(msg.obj);
                if (msg.what == -1) {
                    params.toast(R.string.no_wifi_warning);
                } else {
                    JSONObject response = JSONObject.parseObject((String) msg.obj);
                    try {
                        int version = getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;
                        if (version < response.getInteger("version")) {
                            new MaterialAlertDialogBuilder(MainActivity.this).setMessage(response.getString("description")).setTitle("发现新版本").setPositiveButton("更新", (dialogInterface, i) -> {
                                file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "sysuer.apk");
                                downloadId = ((DownloadManager) getSystemService(DOWNLOAD_SERVICE)).enqueue(new DownloadManager.Request(Uri.parse(response.getString("link"))).setDestinationUri(Uri.fromFile(file)).setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED));
//                                DownloadManager.Query query = new DownloadManager.Query();
//                                query.setFilterById(downloadId);
//                                Cursor cursor = downloadManager.query(query);
//                                if (cursor.moveToFirst()) {
//                                    int status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS));
//                                    if (status == DownloadManager.STATUS_SUCCESSFUL) {
//                                        String filePath = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI));
//                                        System.out.println("文件路径: " + filePath);
//                                    }
//                                }
//                                cursor.close();
                            }).setNegativeButton("取消", (dialogInterface, i) -> {
                            }).setCancelable(response.getBoolean("enforce")).create().show();
                        } else if (version < response.getInteger("version")) {
                            params.toast("本APP已被篡改");
                        }
                    } catch (PackageManager.NameNotFoundException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        };
        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(intent.getAction())) {
                    if (intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1) == downloadId) {
                        params.toast("完成下载");
                        startActivity(new Intent(Intent.ACTION_VIEW).setDataAndType(FileProvider.getUriForFile(context, getPackageName() + ".fileProvider", file), "application/*"));
                    }
                }
            }
        };
        ContextCompat.registerReceiver(this,receiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), ContextCompat.RECEIVER_NOT_EXPORTED);
        detailLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), o -> {});
        PackageManager pm = getPackageManager();
        pm.setComponentEnabledSetting(new ComponentName(this, SettingActivity.class), PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
        //WorkManager.getInstance(this).enqueue(new OneTimeWorkRequest.Builder(ClassIsland.class).build());
        new ClassIsland(this).doWork();
    }

    public ActivityResultLauncher<Intent> launch() {
        return detailLauncher;
    }

    void checkUpdate() {
        new OkHttpClient.Builder().build().newCall(new Request.Builder().url("https://sysu-tang.github.io/latest.json").build()).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Message msg = new Message();
                msg.what = -1;
                handler.sendMessage(msg);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                Message msg = new Message();
                msg.what = 0;
                msg.obj = response.body().string();
                handler.sendMessage(msg);
            }
        });
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(receiver);
        receiver=null;
        super.onDestroy();
    }
}