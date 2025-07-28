package com.sysu.edu.preference;

import android.content.Context;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;
import androidx.preference.PreferenceManager;

public class Language {

    public static void changeLanguage(String languageCode) {
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(languageCode));
//        Locale locale = new Locale(languageCode);
//        Locale.setDefault(locale);
//        Resources resources = context.getResources();
//        Configuration configuration = resources.getConfiguration();
//        configuration.setLocale(locale);
//        resources.updateConfiguration(configuration, resources.getDisplayMetrics());
    }
    public static String getLanguageCode(Context context){
        return (new String[]{"zh-CN", "en", ""})[Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(context).getString("language","2"))];
    }
    public static void setLanguage(Context context){
        changeLanguage(getLanguageCode(context));
    }
}