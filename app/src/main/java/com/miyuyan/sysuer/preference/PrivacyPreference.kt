package com.miyuyan.sysuer.preference

import android.content.Context
import androidx.datastore.preferences.core.stringPreferencesKey

/**
 * 隐私类偏好，迁移自 "privacy" SharedPreferences，存放遗留的账号凭据
 */
class PrivacyPreference(context: Context) {

	private val store = PreferenceStore.getInstance(context, FILE_NAME) {
		listOf(sharedPreferencesMigration(context, FILE_NAME))
	}

	val username: String
		get() = store.get(USERNAME, "")

	val password: String
		get() = store.get(PASSWORD, "")

	/** 清除遗留的账号凭据 */
	fun clearCredentials() = store.remove(USERNAME, PASSWORD)

	companion object {
		private const val FILE_NAME = "privacy"

		private val USERNAME = stringPreferencesKey("username")
		private val PASSWORD = stringPreferencesKey("password")
	}
}
