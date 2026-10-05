package com.miyuyan.sysuer.browser.data

import android.content.Context
import android.util.Log
import android.webkit.JavascriptInterface
import androidx.core.net.toUri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.alibaba.fastjson2.JSON
import com.miyuyan.sysuer.api.AccountManager
import com.miyuyan.sysuer.api.TargetHost
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * Grease monkey API 桥接类 - 使用 DataStore 存储
 */
class GMBridge(context: Context, private val urlProvider: (() -> String?)? = null) {
	private val appContext = context.applicationContext
	private val dataStore: DataStore<Preferences> = GMDataStoreManager.getInstance(context)
	private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
	private val commands = mutableMapOf<String, MutableList<String>>()
	@JavascriptInterface
	fun registerMenuCommand(scriptId: String, name: String) {
		commands.getOrPut(scriptId) { mutableListOf() }.apply {
			if (!contains(name)) add(name)
		}
	}

	fun getCommands(scriptId: String): List<String> = commands[scriptId] ?: emptyList()
	fun clearCommands(): Unit = commands.clear()
	@JavascriptInterface
	fun setValue(
		scriptName: String, key: String, value: String
	) {
		scope.launch {
			try {
				dataStore.edit { prefs ->
					prefs[stringPreferencesKey("${scriptName}_$key")] = value
				}
			} catch (e: Exception) {
				e.printStackTrace()
			}
		}
	}

	@JavascriptInterface
	fun getValue(
		scriptName: String, key: String, defaultValue: String?
	): String? = try {
		val host = urlProvider?.invoke()?.toUri()?.host
		val netId = if (host?.endsWith("sysu.edu.cn") == true) {
			AccountManager.getInstance(appContext).getActiveAccountSync(TargetHost.SYSU)
		} else null
		val netIdValue = when (key.lowercase()) {
			"username" -> netId?.first
			"password" -> netId?.second
			else -> null
		}
		val encoded = netIdValue?.let { JSON.toJSONString(it) }
		encoded ?: runBlocking {
			dataStore.data.first()[stringPreferencesKey("${scriptName}_$key")]
		} ?: defaultValue
	} catch (e: Exception) {
		Log.e("GM_Script", "Error getting value for $key", e)
		defaultValue
	}

	@JavascriptInterface
	fun deleteValue(scriptName: String, key: String) {
		scope.launch {
			try {
				dataStore.edit { prefs ->
					prefs.remove(stringPreferencesKey("${scriptName}_$key"))
				}
			} catch (e: Exception) {
				e.printStackTrace()
			}
		}
	}

	@JavascriptInterface
	fun listValues(scriptName: String): String = try {
		val prefix = "${scriptName}_"
		val keys = runBlocking {
			dataStore.data.first().asMap().keys.filter { it.name.startsWith(prefix) }
				.map { it.name.removePrefix(prefix) }
		}
		JSON.toJSONString(keys)
	} catch (_: Exception) {
		"[]"
	}

	@JavascriptInterface
	fun log(message: String) {
		Log.d("GM_Script", message)
	}


	/** 写入为 fire-and-forget 协程，无需显式释放；保留方法以兼容既有调用 */
	fun release() {
	}
}
