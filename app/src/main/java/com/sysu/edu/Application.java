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
        //getResources().getConfiguration().fontScale=2.0f;//new float[]{1.0f,0.5f,0.75f,1.5f,2.0f}[Integer.parseInt(Objects.requireNonNull(PreferenceManager.getDefaultSharedPreferences(this).getString("fontSize", "2")))];
    }



}
