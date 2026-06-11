package com.sysu.edu.preference

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.app.AppCompatDelegate.setApplicationLocales
import androidx.core.os.LocaleListCompat.forLanguageTags
import androidx.preference.PreferenceManager

class SettingManager(val context: Context) {
	val preferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
	
	init {
		if (defaultFontSize == 0.0f)
			defaultFontSize = context.resources.configuration.fontScale
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
		setApplicationLocales(forLanguageTags(getLanguageCode()))
	}
	
	/*
	* 设置主题
	* 0: 浅色主题
	* 1: 深色主题
	* 2: 系统主题
	* */
	fun setTheme() {
		preferences.getString("theme", "2")?.toInt()?.let { AppCompatDelegate.setDefaultNightMode((intArrayOf(AppCompatDelegate.MODE_NIGHT_NO, AppCompatDelegate.MODE_NIGHT_YES, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM))[it]) }
	}
	
	companion object {
		@JvmStatic var defaultFontSize: Float = 0.0f
	}
	
	fun getFontSize(): Float {
		return preferences.getString("fontSize", "0")?.takeIf { "0" != it }
			?.let { floatArrayOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f)[it.toInt() - 1] }
			?: defaultFontSize
	}
	
	fun setFontSize(fontSize: Float): Context {
		context.resources.configuration.fontScale = fontSize
		return context.createConfigurationContext(context.resources.configuration)
	}
}