package com.miyuyan.sysuer.api

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.edit
import androidx.core.os.LocaleListCompat
import androidx.preference.PreferenceManager
import kotlin.concurrent.Volatile

class SettingManager(private val context: Context) {
	val preferences: SharedPreferences =
		PreferenceManager.getDefaultSharedPreferences(context)

	init {
		if (defaultFontSize == 0.0f) defaultFontSize =
			context.resources.configuration.fontScale
	}

	/**
	 * 获取语言编码
	 * @return 语言编码
	 * @default "zh-CN"
	 * @range "zh-CN" "en" ""
	 * @description "zh-CN": 中文 "en": 英文 ""
	 * */
	fun getLanguageCode(): String =
		arrayOf("zh-CN", "en", "")[preferences.getString("language", "2")!!.toInt()]

	/**
	 * 设置语言
	 * 0: 中文
	 * 1: 英文
	 * 2: 系统语言
	 * */
	fun setLanguage() {
		AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(getLanguageCode()))
	}

	/**
	 * 设置主题
	 * 0: 浅色主题
	 * 1: 深色主题
	 * 2: 系统主题
	 * */
	fun setTheme() {
		AppCompatDelegate.setDefaultNightMode(
			(intArrayOf(
				AppCompatDelegate.MODE_NIGHT_NO,
				AppCompatDelegate.MODE_NIGHT_YES,
				AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
			))[getTheme()]
		)
	}

	/**
	 * 获取主题
	 * 0: 浅色主题
	 * 1: 深色主题
	 * 2: 系统主题
	 * @return 主题
	 * @default 2
	 * @range 0 - 2
	 * @description 0: 浅色主题 1: 深色主题 2: 系统主题
	 * */
	fun getTheme(): Int = preferences.getString("theme", "2")?.toInt() ?: 2

	/**
	 * 是否开启深色主题
	 * @return 是否开启深色主题
	 * @default false
	 * */
	val isDarkTheme: Boolean =
		getTheme() == 1 || (getTheme() == 2 && (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES)
	val isDynamicColor: Boolean = preferences.getBoolean("dynamic_color", true)

	/**
	 * 是否开启导航栏模糊效果
	 * @return 是否开启导航栏模糊效果
	 * @default true
	 * */
	val isBlurNavigationBar: Boolean = preferences.getBoolean("navigation_bar", true)

	companion object {
		@JvmStatic
		var defaultFontSize: Float = 0.0f

		@SuppressLint("StaticFieldLeak")
		@Volatile
		private var INSTANCE: SettingManager? = null
		fun getInstance(context: Context): SettingManager =
			INSTANCE ?: synchronized(SettingManager::class.java) {
				INSTANCE ?: SettingManager(context.applicationContext).also { INSTANCE = it }
			}
	}

	/**
	 * 设置字体大小
	 * @param fontSize 字体大小
	 * */
fun setFontSize(fontSize: Float): Context {
	val config = Configuration(context.resources.configuration)
	config.fontScale = fontSize
	return context.createConfigurationContext(config)
}

	/**
	 * 字体大小
	 * @return 字体大小
	 * @default 1.0f
	 * @range 0.5f - 1.5f
	 * */
	var fontSize: Float
		get() = preferences.getString("fontSize", "0")?.takeIf { "0" != it }
			?.let { floatArrayOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f)[it.toInt() - 1] }
			?: defaultFontSize
		set(value) {
			preferences.edit { putString("fontSize", "$value") }
		}

	/**
	 * 开发者模式
	 * @return 是否开启开发者模式
	 * */
	var developerMode: Boolean = false
		get() = preferences.getBoolean("developer_mode", false)
		set(value) {
			field = value
			preferences.edit { putBoolean("developer_mode", value) }
		}

	/**
	 * 检测测试版本更新
	 * @return 是否开启检测测试版本更新
	 * */
	var betaCheck: Boolean
		get() = preferences.getBoolean("beta_check", false)
		set(value) {
			preferences.edit { putBoolean("beta_check", value) }
		}

	/**
	 * 逸仙码小程序的捷径链接
	 * @return 逸仙码小程序的捷径连接
	 * */
	var qrCode: String
		get() = preferences.getString("qrcode", "") ?: ""
		set(value) {
			preferences.edit { putString("qrcode", value) }
		}

	/**
	 * 课程日期
	 * 0: 今天
	 * 1: 最近
	 * */
	var courseDate: Int
		get() = preferences.getString("course_date", "0")?.toInt() ?: 0
		set(value) {
			preferences.edit { putString("course_date", "$value") }
		}
}