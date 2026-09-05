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
class GMBridge(private val context: Context) {
	private val dataStore: RxDataStore<Preferences> = GMDataStoreManager.getInstance(context)
	private val disposables = CompositeDisposable()
	private val commands = mutableMapOf<String, MutableList<String>>()
	@JavascriptInterface fun registerMenuCommand(scriptId: String, name: String) {
		commands.getOrPut(scriptId) { mutableListOf() }.apply {
			if (!contains(name)) add(name)
		}
	}
	
	fun getCommands(scriptId: String): List<String> = commands[scriptId] ?: emptyList()
	fun clearCommands(): Unit = commands.clear()
	@OptIn(ExperimentalCoroutinesApi::class) @JavascriptInterface fun setValue(scriptName: String,
	                                                                           key: String,
	                                                                           value: String) {
		val disposable = dataStore.updateDataAsync { prefs ->
			Single.just(prefs.toMutablePreferences().apply {
				this[stringPreferencesKey("${scriptName}_$key")] = value
			})
		}.subscribe({}, { it.printStackTrace() })
		disposables.add(disposable)
	}
	
	@OptIn(ExperimentalCoroutinesApi::class) @JavascriptInterface fun getValue(scriptName: String,
	                                                                           key: String,
	                                                                           defaultValue: String?): String? {
		// SECURITY: Previously this method auto-injected `username`/`password` from
		// the active account when requested via `GM_getValue`. Because
		// `AndroidGM` is a global JavascriptInterface on the WebView, ANY web
		// page script (not just user-installed userscripts) could call it and
		// exfiltrate the user's credentials. We therefore refuse to ever
		// surface those keys from this bridge.
		if (key.equals("password", ignoreCase = true) ||
			key.equals("username", ignoreCase = true) ||
			key.contains("passwd", ignoreCase = true) ||
			key.contains("credential", ignoreCase = true)) {
			Log.w("GM_Script", "Refused credential-like GM_getValue key: $key")
			return defaultValue
		}
		return try {
			val storedValue = dataStore.data()
				.blockingFirst()[stringPreferencesKey("${scriptName}_$key")]
			storedValue ?: defaultValue
		} catch (e: Exception) {
			Log.e("GM_Script", "Error getting value for $key", e)
			defaultValue
		}
	}
	
	@OptIn(ExperimentalCoroutinesApi::class) @JavascriptInterface
	fun deleteValue(scriptName: String, key: String) {
		val disposable = dataStore.updateDataAsync { prefs ->
			Single.just(prefs.toMutablePreferences().apply {
				remove(stringPreferencesKey("${scriptName}_$key"))
			})
		}.subscribe({}, { it.printStackTrace() })
		disposables.add(disposable)
	}
	
	@OptIn(ExperimentalCoroutinesApi::class) @JavascriptInterface
	fun listValues(scriptName: String): String {
		return try {
			val prefix = "${scriptName}_"
			val keys = dataStore.data()
				.blockingFirst()
				.asMap().keys.filter { it.name.startsWith(prefix) }
				.map { it.name.removePrefix(prefix) }
			JSON.toJSONString(keys)
		} catch (_: Exception) {
			"[]"
		}
	}
	
	@JavascriptInterface fun log(message: String) {
		Log.d("GM_Script", message)
	}
	
	fun release() {
		disposables.clear()
	}
}
