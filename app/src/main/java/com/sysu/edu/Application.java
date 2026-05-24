package com.sysu.edu;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceManager;

import com.sysu.edu.preference.Language;
import com.sysu.edu.preference.Theme;

import java.util.Objects;
import java.util.Set;

public class Application extends android.app.Application{
    float defaultFontSize;

    @Override
    public void onCreate() {
        super.onCreate();
        Theme.setTheme(this);
        Language.setLanguage(this);
        SharedPreferences pm = PreferenceManager.getDefaultSharedPreferences(this);
        if ((!pm.contains("dashboard")) || Objects.requireNonNull(pm.getStringSet("dashboard", null)).isEmpty())
            pm.edit().putStringSet("dashboard", Set.of(getResources().getStringArray(R.array.values_6))).apply();
        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle bundle) {
                Configuration configuration = activity.getResources().getConfiguration();
                String fontValue = PreferenceManager.getDefaultSharedPreferences(activity).getString("fontSize", "0");
                if (defaultFontSize == 0) defaultFontSize = configuration.fontScale;
                if (!"0".equals(fontValue))
                    configuration.fontScale = new float[]{0.5f, 0.75f, 1.0f, 1.25f, 1.5f}[Integer.parseInt(fontValue) - 1];
                else configuration.fontScale = defaultFontSize;
                activity.getResources().updateConfiguration(configuration, activity.getResources().getDisplayMetrics());
                getResources().updateConfiguration(configuration, getResources().getDisplayMetrics());
            }

            @Override
            public void onActivityDestroyed(@NonNull Activity activity) {
            
            }

            @Override
            public void onActivityPaused(@NonNull Activity activity) {

            }

            @Override
            public void onActivityResumed(@NonNull Activity activity) {

            }

            @Override
            public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle bundle) {

            }

            @Override
            public void onActivityStarted(@NonNull Activity activity) {

            }

            @Override
            public void onActivityStopped(@NonNull Activity activity) {

            }
        });
    }

//    static class UncaughtExceptionHandler implements Thread.UncaughtExceptionHandler {
//
//        private final android.app.Application app;
//        private final Thread.UncaughtExceptionHandler defaultHandler = Thread.getDefaultUncaughtExceptionHandler();
//
//        public UncaughtExceptionHandler(android.app.Application application) {
//            app = application;
//        }
//
//        @Override
//        public void uncaughtException(@NonNull Thread t, @NonNull Throwable e) {
//            StringWriter sw = new StringWriter();
//            PrintWriter pw = new PrintWriter(sw);
//            FirebaseCrashlytics.getInstance().recordException(e);
//            e.printStackTrace(pw);
//            pw.close();
//            app.startActivity(new Intent(app, CrashActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK).putExtra("crash", sw.toString()));
//            if (defaultHandler != null) defaultHandler.uncaughtException(t, e);
//            android.os.Process.killProcess(android.os.Process.myPid());
//            System.exit(10);
//        }
//    }
}
