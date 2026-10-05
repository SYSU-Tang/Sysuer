package com.miyuyan.sysuer.preference

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

/**
 * 浏览器偏好，迁移自 "browser" SharedPreferences
 */
class BrowserPreference(context: Context) {

	private val store = PreferenceStore.getInstance(context, FILE_NAME) {
		listOf(sharedPreferencesMigration(context, FILE_NAME))
	}

	var ua: Int
		get() = store.get(UA, 0)
		set(ua) {
			store.set(UA, ua)
		}

	/** 当前选中 UA 的字符串；null 表示跟随系统，初始化 WebView 时需同步读取 */
	var uaString: String?
		get() = store[UA_STRING]
		set(uaString) {
			if (uaString == null) store.remove(UA_STRING)
			else store.set(UA_STRING, uaString)
		}

	var isPC: Boolean
		get() = store.get(PC, false)
		set(pc) {
			store.set(PC, pc)
		}

	var isImageBlocked: Boolean
		get() = store.get(IMAGE_BLOCKED, false)
		set(imageBlocked) {
			store.set(IMAGE_BLOCKED, imageBlocked)
		}

	var isJSEnabled: Boolean
		get() = store.get(JAVASCRIPT_ENABLED, true)
		set(jsEnabled) {
			store.set(JAVASCRIPT_ENABLED, jsEnabled)
		}

	var isSaveMobileDataMode: Boolean
		get() = store.get(SAVE_MOBILE_DATA_MODE, false)
		set(saveMobileDataMode) {
			store.set(SAVE_MOBILE_DATA_MODE, saveMobileDataMode)
		}

	/**
	 * 主题
	 * 0: 系统默认
	 * 1: 强制深色
	 * 2: 强制浅色
	 * */
	var theme: Int
		get() = store.get(THEME, 0)
		set(theme) {
			store.set(THEME, theme)
		}

	var isPrivacyMode: Boolean
		get() = store.get(PRIVACY_MODE, false)
		set(privacyMode) {
			store.set(PRIVACY_MODE, privacyMode)
		}

	var isCookieAccept: Boolean
		get() = store.get(COOKIE_ACCEPT, true)
		set(accept) {
			store.set(COOKIE_ACCEPT, accept)
		}

	var isThirdPartyCookieAccept: Boolean
		get() = store.get(THIRD_PARTY_COOKIE_ACCEPT, true)
		set(accept) {
			store.set(THIRD_PARTY_COOKIE_ACCEPT, accept)
		}

	companion object {
		private const val FILE_NAME = "browser"

		private val UA = intPreferencesKey("ua")
		private val UA_STRING = stringPreferencesKey("ua_string")
		private val PC = booleanPreferencesKey("pc")
		private val IMAGE_BLOCKED = booleanPreferencesKey("image_blocked")
		private val JAVASCRIPT_ENABLED = booleanPreferencesKey("javascript_enabled")
		private val SAVE_MOBILE_DATA_MODE = booleanPreferencesKey("save_mobile_data_mode")
		private val THEME = intPreferencesKey("theme")
		private val PRIVACY_MODE = booleanPreferencesKey("privacy_mode")
		private val COOKIE_ACCEPT = booleanPreferencesKey("cookie_accept")
		private val THIRD_PARTY_COOKIE_ACCEPT = booleanPreferencesKey("third_party_cookie_accept")
	}
}
