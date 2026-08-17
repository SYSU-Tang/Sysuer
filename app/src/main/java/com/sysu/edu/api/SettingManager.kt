package com.sysu.edu.api

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.edit
import androidx.core.os.LocaleListCompat
import androidx.preference.PreferenceManager

class SettingManager(val context: Context) {
	val preferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
	
	init {
		if (defaultFontSize == 0.0f) defaultFontSize = context.resources.configuration.fontScale
	}
	
	fun getLanguageCode(): String {
		return arrayOf("zh-CN", "en", "")[preferences.getString("language", "2")!!.toInt()]
	}
	
	/*
	* 设置语言
	* 0: 中文
	* 1: 英文
	* 2: 系统语言
	* */
	fun setLanguage() {
		AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(getLanguageCode()))
	}
	
	/*
	* 设置主题
	* 0: 浅色主题
	* 1: 深色主题
	* 2: 系统主题
	* */
	fun setTheme() {
		AppCompatDelegate.setDefaultNightMode((intArrayOf(AppCompatDelegate.MODE_NIGHT_NO, AppCompatDelegate.MODE_NIGHT_YES, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM))[getTheme()])
	}
	
	/*
	* 获取主题
	* 0: 浅色主题
	* 1: 深色主题
	* 2: 系统主题
	* */
	fun getTheme(): Int = preferences.getString("theme", "2")?.toInt() ?: 2
	val isDarkTheme: Boolean = getTheme() == 1 || (getTheme() == 2 && AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES)
	
	companion object {
		@JvmStatic var defaultFontSize: Float = 0.0f
	}
	
	fun setFontSize(fontSize: Float): Context {
		context.resources.configuration.fontScale = fontSize
		return context.createConfigurationContext(context.resources.configuration)
	}
	
	var fontSize: Float = defaultFontSize
		get() {
			return preferences.getString("fontSize", "0")?.takeIf { "0" != it }?.let { floatArrayOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f)[it.toInt() - 1] } ?: defaultFontSize
		}
		set(value) {
			field = value
			preferences.edit { putString("fontSize", "$value") }
		}
	var developerMode: Boolean = false
		get() {
			return preferences.getBoolean("developer_mode", false)
		}
		set(value) {
			field = value
			preferences.edit { putBoolean("developer_mode", value) }
		}
	var betaCheck: Boolean = false
		get() {
			return preferences.getBoolean("beta_check", false)
		}
		set(value) {
			field = value
			preferences.edit { putBoolean("beta_check", value) }
		}
	var qrCode: String = ""
		get() {
			return preferences.getString("qrcode", "") ?: ""
		}
		set(value) {
			field = value
			preferences.edit { putString("qrcode", value) }
		}
}