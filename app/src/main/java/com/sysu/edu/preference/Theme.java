package com.sysu.edu.preference;

import android.content.Context;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.PreferenceManager;

public class Theme {
    final Context context;

    public Theme(Context c) {
        this.context = c;
    }

    public int getThemeCode() {
        return Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(context).getString("theme", "2"));
    }

    public int getThemeMode() {
        return (new int[]{AppCompatDelegate.MODE_NIGHT_NO, AppCompatDelegate.MODE_NIGHT_YES, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM})[getThemeCode()];
    }

    public void setTheme() {
        AppCompatDelegate.setDefaultNightMode(getThemeMode());
    }
}
