package com.sysu.edu;

import android.content.Context;
import android.content.res.Configuration;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.PreferenceManager;

import com.sysu.edu.preference.Language;
import com.sysu.edu.preference.Theme;

public class BaseActivity extends AppCompatActivity {
    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(newBase);
        AppCompatDelegate.setDefaultNightMode(new Theme(newBase).getThemeMode());
        Language.setLanguage(newBase);
        Configuration configuration = newBase.getResources().getConfiguration();
        String fontValue = PreferenceManager.getDefaultSharedPreferences(newBase).getString("fontSize", "0");
        if (!fontValue.equals("0")) {
            configuration.fontScale = new float[]{0.5f, 0.75f, 1.0f, 1.25f, 1.5f}[Integer.parseInt(fontValue) - 1];
        }
        applyOverrideConfiguration(configuration);
    }
}
