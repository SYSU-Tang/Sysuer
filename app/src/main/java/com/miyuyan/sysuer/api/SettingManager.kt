package com.miyuyan.sysuer.api

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.miyuyan.sysuer.preference.SettingPreference
import kotlin.concurrent.Volatile

class SettingManager(private val context: Context) {
	private val settingPreference = SettingPreference(context)

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
		arrayOf("zh-CN", "en", "")[settingPreference.language.toInt()]

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
	fun getTheme(): Int = settingPreference.theme.toIntOrNull() ?: 2

	/**
	 * 是否开启深色主题
	 * @return 是否开启深色主题
	 * @default false
	 * */
	val isDarkTheme: Boolean =
		getTheme() == 1 || (getTheme() == 2 && (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES)
	val isDynamicColor: Boolean = settingPreference.dynamicColor

	/**
	 * 是否开启导航栏模糊效果
	 * @return 是否开启导航栏模糊效果
	 * @default true
	 * */
	val isBlurNavigationBar: Boolean = settingPreference.blurNavigationBar

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
		get() = settingPreference.fontSize.takeIf { "0" != it }
			?.let { floatArrayOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f)[it.toInt() - 1] }
			?: defaultFontSize
		set(value) {
			settingPreference.fontSize = "$value"
		}

	/**
	 * 开发者模式
	 * @return 是否开启开发者模式
	 * */
	var developerMode: Boolean = false
		get() = settingPreference.developerMode
		set(value) {
			field = value
			settingPreference.developerMode = value
		}

	/**
	 * 检测测试版本更新
	 * @return 是否开启检测测试版本更新
	 * */
	var betaCheck: Boolean
		get() = settingPreference.betaCheck
		set(value) {
			settingPreference.betaCheck = value
		}

	/**
	 * 逸仙码小程序的捷径链接
	 * @return 逸仙码小程序的捷径连接
	 * */
	var qrCode: String
		get() = settingPreference.qrCode
		set(value) {
			settingPreference.qrCode = value
		}

	/**
	 * 课程日期
	 * 0: 今天
	 * 1: 最近
	 * */
	var courseDate: Int
		get() = settingPreference.courseDate
		set(value) {
			settingPreference.courseDate = value
		}
}
