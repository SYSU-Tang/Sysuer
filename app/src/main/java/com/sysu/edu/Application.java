package com.sysu.edu;

import androidx.appcompat.app.AppCompatDelegate;

import com.sysu.edu.preference.Language;
import com.sysu.edu.preference.Theme;

public class Application extends android.app.Application {

    @Override
    public void onCreate() {
        super.onCreate();
        Theme th = new Theme(this);
        AppCompatDelegate.setDefaultNightMode(th.getThemeMode());
        Language.setLanguage(this);
    }
}
