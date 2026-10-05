package com.miyuyan.sysuer.preference

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey

/**
 * Authorization 与 Token 偏好，迁移自 "authorization" 和 "token" SharedPreferences，按 host 存储
 */
class AuthorizationPreference(context: Context) {

	private val authStore = PreferenceStore.getInstance(context, AUTHORIZATION_FILE_NAME) {
		listOf(sharedPreferencesMigration(context, AUTHORIZATION_FILE_NAME))
	}
	private val tokenStore = PreferenceStore.getInstance(context, TOKEN_FILE_NAME) {
		listOf(sharedPreferencesMigration(context, TOKEN_FILE_NAME))
	}

	/** 获取 host 的 Authorization */
	fun getAuthorization(host: String?): String = authStore.get(hostKey(host), "")

	/** 设置 host 的 Authorization，传入 null 时移除 */
	fun setAuthorization(host: String?, authorization: String?) {
		val key = hostKey(host)
		if (authorization == null) authStore.remove(key)
		else authStore.set(key, authorization)
	}

	/** 获取 host 的 Token */
	fun getToken(host: String?): String = tokenStore.get(hostKey(host), "")

	/** 设置 host 的 Token，传入 null 时移除 */
	fun setToken(host: String?, token: String?) {
		val key = hostKey(host)
		if (token == null) tokenStore.remove(key)
		else tokenStore.set(key, token)
	}

	companion object {
		private const val AUTHORIZATION_FILE_NAME = "authorization"
		private const val TOKEN_FILE_NAME = "token"

		private fun hostKey(host: String?): Preferences.Key<String> =
			stringPreferencesKey(host ?: "")
	}
}
