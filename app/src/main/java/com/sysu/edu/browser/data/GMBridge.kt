package com.sysu.edu.browser.data

import android.content.Context
import android.util.Log
import android.webkit.JavascriptInterface
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.rxjava3.RxDataStore
import com.alibaba.fastjson2.JSON
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.CompositeDisposable
import kotlinx.coroutines.ExperimentalCoroutinesApi

/**
 * Greasemonkey API 桥接类 - 使用 DataStore 存储
 */
class GMBridge(context: Context) {
	private val dataStore: RxDataStore<Preferences> = GMDataStoreManager.getInstance(context)
	private val disposables = CompositeDisposable()
	private val commands = mutableMapOf<String, MutableList<String>>()
	@JavascriptInterface
	fun registerMenuCommand(scriptId: String, name: String) {
		commands.getOrPut(scriptId) { mutableListOf() }.apply {
			if (!contains(name)) add(name)
		}
	}

	fun getCommands(scriptId: String): List<String> = commands[scriptId] ?: emptyList()
	fun clearCommands(): Unit = commands.clear()
	@OptIn(ExperimentalCoroutinesApi::class)
	@JavascriptInterface
	fun setValue(
		scriptName: String, key: String, value: String
	) {
		val disposable = dataStore.updateDataAsync { prefs ->
			Single.just(prefs.toMutablePreferences().apply {
				this[stringPreferencesKey("${scriptName}_$key")] = value
			})
		}.subscribe({}, { it.printStackTrace() })
		disposables.add(disposable)
	}

	@OptIn(ExperimentalCoroutinesApi::class)
	@JavascriptInterface
	fun getValue(
		scriptName: String, key: String, defaultValue: String?
	): String? = try {
		val storedValue =
			dataStore.data().blockingFirst()[stringPreferencesKey("${scriptName}_$key")]
		storedValue ?: defaultValue
	} catch (e: Exception) {
		Log.e("GM_Script", "Error getting value for $key", e)
		defaultValue
	}

	@OptIn(ExperimentalCoroutinesApi::class)
	@JavascriptInterface
	fun deleteValue(scriptName: String, key: String) {
		val disposable = dataStore.updateDataAsync { prefs ->
			Single.just(prefs.toMutablePreferences().apply {
				remove(stringPreferencesKey("${scriptName}_$key"))
			})
		}.subscribe({}, { it.printStackTrace() })
		disposables.add(disposable)
	}

	@OptIn(ExperimentalCoroutinesApi::class)
	@JavascriptInterface
	fun listValues(scriptName: String): String = try {
		val prefix = "${scriptName}_"
		val keys =
			dataStore.data().blockingFirst().asMap().keys.filter { it.name.startsWith(prefix) }
				.map { it.name.removePrefix(prefix) }
		JSON.toJSONString(keys)
	} catch (_: Exception) {
		"[]"
	}

	@JavascriptInterface
	fun log(message: String) {
		Log.d("GM_Script", message)
	}

	fun release() {
		disposables.clear()
	}
}
