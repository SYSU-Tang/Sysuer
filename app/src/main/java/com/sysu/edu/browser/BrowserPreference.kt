package com.sysu.edu.browser

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

class BrowserPreference(context: Context) {
	private val preference: SharedPreferences = context.getSharedPreferences("browser", Context.MODE_PRIVATE)
	var ua: Int
		get() = preference.getInt("ua", 0)
		set(ua) {
			preference.edit { putInt("ua", ua) }
		}
	var isPC: Boolean
		get() = preference.getBoolean("pc", false)
		set(pc) {
			preference.edit { putBoolean("pc", pc) }
		}
	var isImageBlocked: Boolean
		get() = preference.getBoolean("image_blocked", false)
		set(imageBlocked) {
			preference.edit { putBoolean("image_blocked", imageBlocked) }
		}
	var isJSEnabled: Boolean
		get() = preference.getBoolean("javascript_enabled", true)
		set(jsEnabled) {
			preference.edit { putBoolean("javascript_enabled", jsEnabled) }
		}
	var isSaveMobileDataMode: Boolean
		get() = preference.getBoolean("save_mobile_data_mode", false)
		set(saveMobileDataMode) {
			preference.edit { putBoolean("save_mobile_data_mode", saveMobileDataMode) }
		}
	var theme: Int
		get() = preference.getInt("theme", 0)
		/*
			 * 主题
			 * 0: 系统默认
			 * 1: 强制深色
			 * 2: 强制浅色
			 * */
		set(theme) {
			preference.edit { putInt("theme", theme) }
		}
	var isPrivacyMode: Boolean
		get() = preference.getBoolean("privacy_mode", false)
		set(privacyMode) {
			preference.edit { putBoolean("privacy_mode", privacyMode) }
		}
	var isCookieAccept: Boolean
		get() = preference.getBoolean("cookie_accept", true)
		set(accept) {
			preference.edit { putBoolean("cookie_accept", accept) }
		}
	var isThirdPartyCookieAccept: Boolean
		get() = preference.getBoolean("third_party_cookie_accept", true)
		set(accept) {
			preference.edit { putBoolean("third_party_cookie_accept", accept) }
		}
}
