package com.sysu.edu.widget

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.rxjava3.RxPreferenceDataStoreBuilder
import androidx.datastore.rxjava3.RxDataStore

object DataStoreManager {
	val TODAY_CLASS: Preferences.Key<String> = stringPreferencesKey("today_class")
	private var dataStore: RxDataStore<Preferences> ?= null
	@Synchronized fun getInstance(context: Context): RxDataStore<Preferences> {
		if (dataStore == null) dataStore = RxPreferenceDataStoreBuilder(context.applicationContext, "today_class").build()
		return dataStore!!
	}
}