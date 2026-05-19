package com.sysu.edu.widget;

import android.content.Context;

import androidx.datastore.preferences.core.Preferences;
import androidx.datastore.preferences.core.PreferencesKeys;
import androidx.datastore.preferences.rxjava3.RxPreferenceDataStoreBuilder;
import androidx.datastore.rxjava3.RxDataStore;


public class DataStoreManager {
    public static final Preferences.Key<String> TODAY_CLASS =
            PreferencesKeys.stringKey("today_class");
    private static RxDataStore<Preferences> dataStore = null;

    public static synchronized RxDataStore<Preferences> getInstance(Context context) {
        if (dataStore == null) dataStore = new RxPreferenceDataStoreBuilder(
                context.getApplicationContext(),
                "today_class"
        ).build();
        return dataStore;
    }
}