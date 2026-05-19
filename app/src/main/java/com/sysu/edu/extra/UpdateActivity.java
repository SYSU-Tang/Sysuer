package com.sysu.edu.extra;

import static com.sysu.edu.api.DownloadManager.downloadFile;
import static com.sysu.edu.api.DownloadManager.getOpenFileIntent;
import static com.sysu.edu.api.DownloadManager.openFile;

import android.app.PendingIntent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationChannelCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.app.PendingIntentCompat;

import com.alibaba.fastjson2.JSONObject;
import com.sysu.edu.R;
import com.sysu.edu.api.DownloadManager;
import com.sysu.edu.api.HttpManager;
import com.sysu.edu.api.Params;
import com.sysu.edu.databinding.ActivityUpdateBinding;

import java.io.File;
import java.util.ArrayList;
import java.util.Locale;

import io.noties.markwon.Markwon;

public class UpdateActivity extends AppCompatActivity {
    
    HttpManager http;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityUpdateBinding binding = ActivityUpdateBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.toolbar.setNavigationOnClickListener(_ -> supportFinishAfterTransition());
        int versionCode = 0;
        String versionName;
        try {
            PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), 0);
            versionCode = info.versionCode;
            versionName = info.versionName;
            binding.version.setText(String.format(Locale.getDefault(), "%s(%d)", versionName, versionCode));
        } catch (PackageManager.NameNotFoundException _) {
        }
        NotificationChannelCompat channel = new NotificationChannelCompat.Builder("update", NotificationManagerCompat.IMPORTANCE_DEFAULT)
                .setDescription("APP下载通知")
                .setName("下载进度通知").build();
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        notificationManager.createNotificationChannel(channel);
        
        Params params = new Params(this);
        int finalVersionCode = versionCode;
        http = new HttpManager(new Handler(getMainLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                switch (msg.what) {
                    case -1 -> {
                        params.toast(R.string.no_net_connected);
                        binding.updateButton.setText(R.string.no_net_connected);
                    }
                    case 0 -> {
                        JSONObject response = JSONObject.parse((String) msg.obj);
                        Integer responseVersion = response.getInteger("version");
                        String responseVersionName = response.getString("versionName");
                        Markwon.create(UpdateActivity.this).setMarkdown(binding.changeLog, "### " + responseVersionName + "(" + responseVersion + ")\n" + response.getString("description"));
                        binding.updateButton.setText(responseVersion > finalVersionCode ? R.string.update : R.string.app_latest_installed);
                        binding.updateButton.setOnClickListener(_ -> {
                            if (responseVersion > finalVersionCode) {
                                String path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString() + "/" + getString(R.string.app_name) + responseVersionName + ".apk";
                                File file = new File(path);
                                if (file.exists() && file.getTotalSpace() > 0) {
                                    openFile(UpdateActivity.this, path);
                                    return;
                                }
                                downloadFile(UpdateActivity.this, response.getString("link"), path, new DownloadManager.DownloadListener() {
                                    @Override
                                    public void onDownloadProgress(long progress, long total) {
                                        String progressString = String.format(Locale.getDefault(), "%.2fMB/%.2fMB", progress / 1024.0f / 1024.0f, total / 1024.0f / 1024.0f);
                                        binding.updateButton.setText(progressString);
                                        NotificationCompat.Builder builder = new NotificationCompat.Builder(UpdateActivity.this, "update")
                                                .setContentTitle(getString(R.string.download))
                                                .setContentText(progressString)
                                                .setSmallIcon(R.drawable.down)
                                                .setStyle(new NotificationCompat.BigTextStyle()
                                                        .bigText(progressString))
                                                .setProgress((int) (total), (int) progress, false)
                                                .setPriority(NotificationCompat.PRIORITY_DEFAULT);
                                        if (ActivityCompat.checkSelfPermission(UpdateActivity.this, android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED)
                                            notificationManager.notify(1002, builder.build());
                                    }
                                    
                                    @Override
                                    public void onDownloadComplete(String path) {
                                        binding.updateButton.setText(R.string.install);
                                        NotificationCompat.Builder builder = new NotificationCompat.Builder(UpdateActivity.this, "update")
                                                .setContentTitle(getString(R.string.download))
                                                .setContentText("下载完成，点击安装更新")
                                                .setSmallIcon(R.drawable.down)
                                                .setContentIntent(PendingIntentCompat.getActivity(UpdateActivity.this, 0, getOpenFileIntent(UpdateActivity.this, path), PendingIntent.FLAG_ONE_SHOT, true))
                                                .setProgress(1, 1, false)
                                                .setPriority(NotificationCompat.PRIORITY_DEFAULT);
                                        if (ActivityCompat.checkSelfPermission(UpdateActivity.this, android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED)
                                            notificationManager.notify(1002, builder.build());
                                        openFile(UpdateActivity.this, path);
                                    }
                                    
                                    @Override
                                    public void onDownloadError(int code, String message) {
                                        params.toast(message);
                                    }
                                });
                            }
                        });
                    }
                }
            }
        });
        http.setParams(params);
        final ArrayList<Long> click = new ArrayList<>();
        binding.icon.setOnClickListener(_ -> {
            if (click.isEmpty() || System.currentTimeMillis() - click.get(click.size() - 1) < 500)
                if (click.size() == 4) {
                    params.toast(params.isDeveloper() ? R.string.developer_disabled : R.string.developer_enabled);
                    params.setDeveloper(!params.isDeveloper());
                    click.clear();
                } else click.add(System.currentTimeMillis());
            else click.clear();
        });
        getUpdate();
    }
    
    public void getUpdate() {
        http.getRequest("https://sysu-tang.github.io/latest.json", 0);
    }
}