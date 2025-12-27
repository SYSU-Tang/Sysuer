package com.sysu.edu;

import androidx.appcompat.app.AppCompatDelegate;

import com.sysu.edu.preference.Language;
import com.sysu.edu.preference.Theme;

public class Application extends android.app.Application {

    @Override
    public void onCreate() {
        super.onCreate();
        AppCompatDelegate.setDefaultNightMode(new Theme(this).getThemeMode());
        Language.setLanguage(this);
        //getResources().getConfiguration().fontScale=2.0f;//new float[]{1.0f,0.5f,0.75f,1.5f,2.0f}[Integer.parseInt(Objects.requireNonNull(SysuerPreferenceManager.getDefaultSharedPreferences(this).getString("fontSize", "2")))];
    }
/*
    @Override
    protected void attachBaseContext(Context base) {
        Configuration configuration = base.getResources().getConfiguration();
        configuration.fontScale = 2.0f;
        super.attachBaseContext(base.createConfigurationContext(configuration));

    }*/
}
