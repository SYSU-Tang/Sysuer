package com.sysu.edu;

import androidx.appcompat.app.AppCompatDelegate;

import com.sysu.edu.preference.ThemeHelper;

public class Application extends android.app.Application {

    @Override
    public void onCreate() {
        super.onCreate();
        ThemeHelper th = new ThemeHelper(this);
        AppCompatDelegate.setDefaultNightMode(th.getThemeMode());
    }
}
