package com.sysu.edu.preference;

import android.content.Context;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.PreferenceManager;

public class Theme {
    
    public static void setTheme(Context context) {
        AppCompatDelegate.setDefaultNightMode((new int[]{AppCompatDelegate.MODE_NIGHT_NO, AppCompatDelegate.MODE_NIGHT_YES, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM})[Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(context).getString("theme", "2"))]);
    }
}
