package com.sysu.edu;

import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.PreferenceManager;

import com.sysu.edu.preference.Language;
import com.sysu.edu.preference.Theme;

import java.util.Objects;
import java.util.Set;

public class Application extends android.app.Application {

    @Override
    public void onCreate() {
        super.onCreate();
        AppCompatDelegate.setDefaultNightMode(new Theme(this).getThemeMode());
        Language.setLanguage(this);
        SharedPreferences pm = PreferenceManager.getDefaultSharedPreferences(this);

        if ((!pm.contains("dashboard"))|| Objects.requireNonNull(pm.getStringSet("dashboard",null)).isEmpty()) {
            pm.edit().putStringSet("dashboard", Set.of(getResources().getStringArray(R.array.dashboard_values))).apply();
        }
    }
/*
    @Override
    protected void attachBaseContext(Context base) {
        Configuration configuration = base.getResources().getConfiguration();
        configuration.fontScale = 2.0f;
        super.attachBaseContext(base.createConfigurationContext(configuration));

    }*/
}
