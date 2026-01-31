package com.sysu.edu;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.StatFs;
import android.util.DisplayMetrics;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.installations.BuildConfig;
import com.sysu.edu.api.Params;
import com.sysu.edu.databinding.ActivityCrashBinding;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import io.noties.markwon.Markwon;
import io.noties.markwon.ext.tables.TablePlugin;

public class CrashActivity extends AppCompatActivity {

    final MutableLiveData<String> crash = new MutableLiveData<>();
    ActivityCrashBinding binding;
    String crashInfo;
    Params params;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCrashBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        params = new Params(this);
        binding.toolbar.setNavigationOnClickListener(v -> supportFinishAfterTransition());
        binding.copy.setOnClickListener(v -> {
            params.copy("crash", crash.getValue());
            params.toast(R.string.copy_successfully);
        });
        binding.submit.setOnClickListener(v -> {
            openIssueInBrowser();
//            params.submit("crash.txt");
        });

        crash.observe(this, s -> Markwon.builder(this).usePlugin(TablePlugin.create(this)).build().setMarkdown(binding.crashContent, s));
        crashInfo = getIntent().getStringExtra("crash");
        if (crashInfo != null) {
            crash.setValue(createDetailedIssueBody(
                    new RuntimeException(crashInfo)
            ));
        }
        binding.restart.setOnClickListener(v -> {
            Intent intent = getPackageManager().getLaunchIntentForPackage(getPackageName());
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                supportFinishAfterTransition();
            }
        });
    }


    void openIssueInBrowser() {
//        new Thread(() -> {
        try {
            String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss",
                    Locale.getDefault()).format(new Date());
            String exceptionType = "Unknown Exception";

            if (crashInfo != null && !crashInfo.isEmpty()) {
                String[] lines = crashInfo.split("\n");
                if (lines.length > 0) {
                    String firstLine = lines[0];
                    if (firstLine.contains(":")) {
                        exceptionType = firstLine.split(":")[0];
                    }
                }
            }

            String title = String.format("[崩溃报告] %s - %s", exceptionType, timestamp);

            final String githubUrl = generateGitHubWebIssueUrl(title);

            params.copy("crash_issue", crash.getValue());
            params.toast(R.string.copy_successfully);

            startActivity(new Intent(Intent.ACTION_VIEW).setData(Uri.parse(githubUrl)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        } catch (Exception ignored) {
        }
//        }).start();
    }


    String generateGitHubWebIssueUrl(String title) throws UnsupportedEncodingException {
        return String.format("https://github.com/%s/%s/issues/new?title=%s&labels=bug,crash-report",
                "SYSU-Tang", "Sysuer", URLEncoder.encode(title, StandardCharsets.UTF_8));
    }

    String getAvailableMemory() {
        ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        if (activityManager != null) {
            ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
            activityManager.getMemoryInfo(memoryInfo);
            return String.format(Locale.getDefault(),
                    "%.2f MB / %.2f MB",
                    (memoryInfo.availMem / (1024.0 * 1024.0)),
                    (memoryInfo.totalMem / (1024.0 * 1024.0)));
        }
        return "Unknown";
    }

    String getStorageInfo() {
        try {
            StatFs statFs = new StatFs(Environment.getDataDirectory().getPath());
            long available = statFs.getAvailableBlocksLong() * statFs.getBlockSizeLong();
            long total = statFs.getBlockCountLong() * statFs.getBlockSizeLong();
            return String.format(Locale.getDefault(),
                    "%.2f GB / %.2f GB",
                    (available / (1024.0 * 1024.0 * 1024.0)),
                    (total / (1024.0 * 1024.0 * 1024.0)));
        } catch (Exception e) {
            return "Unknown";
        }
    }

    /**
     * 生成更详细的Markdown格式Issue内容
     */
    String createDetailedIssueBody(Throwable throwable) {
        StringBuilder markdown = new StringBuilder();

        // 崩溃报告模板
//        markdown.append("<!-- 请保留此模板结构，删除不需要的部分 -->\n\n");

        // 用户描述
        markdown.append("## 📝 用户描述\n");
        markdown.append("请简单描述崩溃发生时的场景和操作步骤。").append("\n\n");

        // 应用信息
        markdown.append("## 📱 应用信息\n");
        markdown.append("| 项目 | 值 |\n");
        markdown.append("|------|-----|\n");
        PackageInfo packageInfo;
        try {
            packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            markdown.append("| 应用版本 | ").append(packageInfo.versionName)
                    .append(" (").append(packageInfo.versionCode).append(") |\n");
            markdown.append("| 包名 | ").append(packageInfo.packageName).append(" |\n");
        } catch (PackageManager.NameNotFoundException e) {
            throw new RuntimeException(e);
        }
        markdown.append("| 构建类型 | ").append(BuildConfig.BUILD_TYPE).append(" |\n\n");

        // 设备信息表格
        markdown.append("## 📱 设备信息\n");
        markdown.append("| 项目 | 值 |\n");
        markdown.append("|------|-----|\n");
        markdown.append("| 设备型号 | ").append(Build.MANUFACTURER).append(" ")
                .append(Build.MODEL).append(" |\n");
        markdown.append("| Android版本 | ").append(Build.VERSION.RELEASE)
                .append(" (API ").append(Build.VERSION.SDK_INT).append(") |\n");

        // 屏幕信息
        DisplayMetrics displayMetrics = new DisplayMetrics();
        WindowManager windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        if (windowManager != null) {
            windowManager.getDefaultDisplay().getMetrics(displayMetrics);
            markdown.append("| 屏幕分辨率 | ").append(displayMetrics.widthPixels)
                    .append("×").append(displayMetrics.heightPixels).append(" |\n");
            markdown.append("| 屏幕密度 | ").append(displayMetrics.densityDpi).append("dpi |\n");
        }

        markdown.append("| 时区 | ").append(TimeZone.getDefault().getID()).append(" |\n");
        markdown.append("| 语言 | ").append(Locale.getDefault().getLanguage()).append(" |\n\n");

        // 崩溃详情
        markdown.append("## 💥 崩溃详情\n");
        markdown.append("**异常类型**: `").append(throwable.getClass().getSimpleName()).append("`\n\n");
        markdown.append("**异常消息**: \n```txt\n").append(throwable.getMessage() != null ?
                throwable.getMessage() : "无消息").append("\n```\n\n");

        // 复现步骤
        markdown.append("## 🔄 复现步骤\n");
        markdown.append("1. [请描述如何复现这个问题]\n");
        markdown.append("2. \n");
        markdown.append("3. \n\n");

        // 期望行为与实际行为
        markdown.append("## ✅ 期望行为\n");
        markdown.append("[描述期望发生的行为]\n\n");

        markdown.append("## ❌ 实际行为\n");
        markdown.append("[描述实际发生的行为]\n\n");

        // 堆栈跟踪（可折叠）
        /*markdown.append("<details>\n");
        markdown.append("<summary>点击查看完整堆栈跟踪</summary>\n\n");
        markdown.append("```txt\n");

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        throwable.printStackTrace(pw);
        markdown.append(sw);
        markdown.append("\n```\n\n");
        markdown.append("</details>\n\n");*/


        // 设备状态信息
        markdown.append("## 📊 设备状态\n");
        markdown.append("- **可用内存**: ").append(getAvailableMemory()).append("\n");
        markdown.append("- **存储空间**: ").append(getStorageInfo()).append("\n");
        markdown.append("- **网络状态**: ").append(getNetworkStatus()).append("\n");
        markdown.append("- **电池状态**: ").append(getBatteryStatus()).append("\n\n");

        // 崩溃时间
        markdown.append("## ⏰ 崩溃时间\n");
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z", Locale.getDefault());
        markdown.append(sdf.format(new Date())).append("\n\n");

        // 日志片段（如果有）
        /*String logSnippet = getRecentLogSnippet();
        if (!logSnippet.isEmpty()) {
            markdown.append("<details>\n");
            markdown.append("<summary>点击查看相关日志</summary>\n\n");
            markdown.append("```\n");
            markdown.append(logSnippet);
            markdown.append("\n```\n\n");
            markdown.append("</details>\n\n");
        }*/

        return markdown.toString();
    }

    private String getNetworkStatus() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null) {
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            if (activeNetwork != null && activeNetwork.isConnectedOrConnecting()) {
                return activeNetwork.getTypeName() + " (" +
                        activeNetwork.getSubtypeName() + ")";
            }
        }
        return "无网络连接";
    }

    private String getBatteryStatus() {
        IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = registerReceiver(null, filter);

        if (batteryStatus != null) {
            int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            float batteryPct = level * 100 / (float) scale;

            int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
            boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL;

            return String.format(Locale.getDefault(),
                    "%.1f%% %s", batteryPct,
                    isCharging ? "(充电中)" : "(未充电)");
        }
        return "未知";
    }

   /* String getRecentLogSnippet() {
        // 获取最近的日志片段
        try {
            Process process = Runtime.getRuntime().exec("logcat -d -t 100");
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            StringBuilder log = new StringBuilder();
            String line;
            int lineCount = 0;

            while ((line = reader.readLine()) != null && lineCount < 50) {
                if (line.contains(getPackageName()) ||
                        line.contains("Exception") ||
                        line.contains("Error")) {
                    log.append(line).append("\n");
                    lineCount++;
                }
            }

            return log.toString();
        } catch (Exception e) {
            return "";
        }
    }*/
}