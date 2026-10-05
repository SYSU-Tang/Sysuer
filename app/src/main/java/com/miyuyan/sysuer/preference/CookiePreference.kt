package com.miyuyan.sysuer.preference

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringSetPreferencesKey

/**
 * Cookie 偏好，迁移自 "cookie" SharedPreferences，按 host 存储 Cookie 集合
 */
class CookiePreference(context: Context) {

	private val store = PreferenceStore.getInstance(context, FILE_NAME) {
		listOf(sharedPreferencesMigration(context, FILE_NAME))
	}

	/** 获取 host 下的全部 Cookie */
	fun get(host: String?): MutableSet<String> = store.get(hostKey(host), emptySet()).toMutableSet()

	/** 覆盖 host 下的全部 Cookie */
	fun set(host: String?, cookieSet: Set<String>) {
		store.set(hostKey(host), cookieSet)
	}

	/** 移除 host 下的全部 Cookie */
	fun remove(host: String?) {
		store.remove(hostKey(host))
	}

	companion object {
		private const val FILE_NAME = "cookie"

		private fun hostKey(host: String?): Preferences.Key<Set<String>> =
			stringSetPreferencesKey(host ?: "")
	}
}
