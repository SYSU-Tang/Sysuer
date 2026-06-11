package com.sysu.edu;

import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

import com.sysu.edu.preference.Language;
import com.sysu.edu.preference.Theme;

import java.util.Objects;
import java.util.Set;

public class Application extends android.app.Application {
    
    @Override
    public void onCreate() {
        super.onCreate();
        Theme.setTheme(this);
        Language.setLanguage(this);
        SharedPreferences pm = PreferenceManager.getDefaultSharedPreferences(this);
        if ((!pm.contains("dashboard")) || Objects.requireNonNull(pm.getStringSet("dashboard", null)).isEmpty())
            pm.edit().putStringSet("dashboard", Set.of(getResources().getStringArray(R.array.values_6))).apply();
    }
}
