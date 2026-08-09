package com.sysu.edu.api

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.rxjava3.RxPreferenceDataStoreBuilder
import androidx.datastore.rxjava3.RxDataStore
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.Disposable
import kotlinx.coroutines.ExperimentalCoroutinesApi

object DataStoreManager {
	val TODAY_CLASS: Preferences.Key<String> = stringPreferencesKey("today_class")
	
	enum class ContentType {
		MARKDOWN, HTML
	}
	
	private var dataStore: RxDataStore<Preferences>? = null
	@Synchronized fun getInstance(context: Context): RxDataStore<Preferences> {
		if (dataStore == null) dataStore = RxPreferenceDataStoreBuilder(context.applicationContext, "today_class").build()
		return dataStore!!
	}
	
	@OptIn(ExperimentalCoroutinesApi::class) @Synchronized fun saveContent(context: Context, title: String, content: String, callback: () -> Unit = {}): Disposable = getInstance(context).updateDataAsync { prefs ->
		Single.just(prefs.toMutablePreferences().apply { this[stringPreferencesKey(title)] = content })
	}.subscribe { callback() }
	
	@OptIn(ExperimentalCoroutinesApi::class) @Synchronized fun loadContent(context: Context, title: String, callback: (String) -> Unit = {}): Disposable = getInstance(context).data().subscribe {
		callback(it[stringPreferencesKey(title)] ?: "")
	}
}