package com.sysu.edu;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Objects;

public class UncaughtExceptionHandlerContentProvider extends ContentProvider {
    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
        return 0;
    }
    
    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        return "";
    }
    
    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
        return null;
    }
    
    @Override
    public boolean onCreate() {
        MyCustomCrashHandler myHandler = new MyCustomCrashHandler(Objects.requireNonNull(getContext()), Thread.getDefaultUncaughtExceptionHandler());
        Thread.setDefaultUncaughtExceptionHandler(myHandler);
        return true;
    }
    
    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection, @Nullable String[] selectionArgs, @Nullable String sortOrder) {
        return null;
    }
    
    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues values, @Nullable String selection, @Nullable String[] selectionArgs) {
        return 0;
    }
    
    static class MyCustomCrashHandler implements Thread.UncaughtExceptionHandler {
        @Nullable
        private final Thread.UncaughtExceptionHandler defaultHandler;
        private final Context app;
        
        public MyCustomCrashHandler(@NonNull Context context, @Nullable Thread.UncaughtExceptionHandler defaultHandler) {
            app = context.getApplicationContext();
            this.defaultHandler = defaultHandler;
        }
        
        @Override
        public void uncaughtException(@NonNull Thread thread, @NonNull Throwable e) {
            // We are now safely being called after Crashlytics does its own thing.
            // Whoever is the last handler on Thread.getDefaultUncaughtExceptionHandler() will execute first on uncaught exceptions.
            // Firebase Crashlytics will handle its own behavior first before calling ours in its own 'finally' block.
            // You can choose to propagate upwards (it will kill the app by default) or do your own thing and propagate if needed.
            
            try {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                e.printStackTrace(pw);
                pw.close();
                app.startActivity(new Intent(app, CrashActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK).putExtra("crash", sw.toString()));
                if (defaultHandler != null) defaultHandler.uncaughtException(thread, e);
                android.os.Process.killProcess(android.os.Process.myPid());
                System.exit(10);
                //do your own thing.
            } catch (Exception _) {
            } finally {
                if (defaultHandler != null) {
                    defaultHandler.uncaughtException(thread, e);
                }
            }
        }
    }
}
