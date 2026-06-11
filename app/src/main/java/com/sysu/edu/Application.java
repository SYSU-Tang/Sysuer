package com.sysu.edu;

import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

import java.util.Objects;
import java.util.Set;

public class Application extends android.app.Application {
    
    @Override
    public void onCreate() {
        super.onCreate();
        SharedPreferences pm = PreferenceManager.getDefaultSharedPreferences(this);
        if ((!pm.contains("dashboard")) || Objects.requireNonNull(pm.getStringSet("dashboard", null)).isEmpty())
            pm.edit().putStringSet("dashboard", Set.of(getResources().getStringArray(R.array.values_6))).apply();
    }
}
