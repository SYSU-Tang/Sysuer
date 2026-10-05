package com.miyuyan.sysuer.preference

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.flow.StateFlow

/**
 * 应用设置类偏好，迁移自默认 SharedPreferences（androidx.preference 界面同源）
 */
class SettingPreference(context: Context) {

	private val store = PreferenceStore.getInstance(context, FILE_NAME) {
		listOf(sharedPreferencesMigration(context, context.packageName + "_preferences"))
	}

	/**
	 * 主题
	 * 0: 浅色主题 1: 深色主题 2: 系统主题
	 * */
	var theme: String
		get() = store.get(THEME, "2")
		set(value) {
			store.set(THEME, value)
		}

	/**
	 * 语言
	 * 0: 中文 1: 英文 2: 系统语言
	 * */
	var language: String
		get() = store.get(LANGUAGE, "2")
		set(value) {
			store.set(LANGUAGE, value)
		}

	/**
	 * 字体大小
	 * "0": 跟随系统 1-5 对应 0.5f - 1.5f
	 * */
	var fontSize: String
		get() = store.get(FONT_SIZE, "0")
		set(value) {
			store.set(FONT_SIZE, value)
		}

	/** 是否开启动态取色 @default true */
	var dynamicColor: Boolean
		get() = store.get(DYNAMIC_COLOR, true)
		set(value) {
			store.set(DYNAMIC_COLOR, value)
		}

	/** 是否开启导航栏模糊效果 @default true */
	var blurNavigationBar: Boolean
		get() = store.get(NAVIGATION_BAR, true)
		set(value) {
			store.set(NAVIGATION_BAR, value)
		}

	/** 图标主题 0: 默认 1: 浅色 2: 深色 */
	var iconTheme: String
		get() = store.get(ICON_THEME, "0")
		set(value) {
			store.set(ICON_THEME, value)
		}

	/** 开发者模式 @default false */
	var developerMode: Boolean
		get() = store.get(DEVELOPER_MODE, false)
		set(value) {
			store.set(DEVELOPER_MODE, value)
		}

	/** 检测测试版本更新 @default false */
	var betaCheck: Boolean
		get() = store.get(BETA_CHECK, false)
		set(value) {
			store.set(BETA_CHECK, value)
		}

	/** 逸仙码小程序的捷径链接 */
	var qrCode: String
		get() = store.get(QRCODE, "")
		set(value) {
			store.set(QRCODE, value)
		}

	/** 课程日期 0: 今天 1: 最近 */
	var courseDate: Int
		get() = store.get(COURSE_DATE, "0").toIntOrNull() ?: 0
		set(value) {
			store.set(COURSE_DATE, "$value")
		}

	/** 仪表盘展示的分区序号 */
	var dashboard: Set<String>
		get() = store.get(DASHBOARD, (0..5).map { "$it" }.toSet())
		set(value) {
			store.set(DASHBOARD, value)
		}

	/** 首页 0-5 分区序号 */
	var home: String
		get() = store.get(HOME, "2")
		set(value) {
			store.set(HOME, value)
		}

	/** 是否检测版本更新 @default true */
	var update: Boolean
		get() = store.get(UPDATE, true)
		set(value) {
			store.set(UPDATE, value)
		}

	/** 是否首次启动 @default false */
	var isFirstLaunch: Boolean
		get() = store.get(IS_FIRST_LAUNCH, false)
		set(value) {
			store.set(IS_FIRST_LAUNCH, value)
		}

	/** 是否已同意隐私政策 @default false */
	var isAgree: Boolean
		get() = store.get(IS_AGREE, false)
		set(value) {
			store.set(IS_AGREE, value)
		}

	/** 桥接 androidx.preference 的设置界面，使其读写直接作用于本偏好 */
	fun asAndroidPreferenceDataStore() = store.asAndroidPreferenceDataStore()

	/** DataStore 内容流：Compose 界面收集它即可在任意偏好变化时重组 */
	val data: StateFlow<Preferences> get() = store.data

	companion object {
		private const val FILE_NAME = "settings"

		internal val THEME = stringPreferencesKey("theme")
		internal val LANGUAGE = stringPreferencesKey("language")
		internal val FONT_SIZE = stringPreferencesKey("fontSize")
		internal val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
		internal val NAVIGATION_BAR = booleanPreferencesKey("navigation_bar")
		internal val ICON_THEME = stringPreferencesKey("icon_theme")
		internal val DEVELOPER_MODE = booleanPreferencesKey("developer_mode")
		internal val BETA_CHECK = booleanPreferencesKey("beta_check")
		internal val QRCODE = stringPreferencesKey("qrcode")
		internal val COURSE_DATE = stringPreferencesKey("course_date")
		internal val DASHBOARD = stringSetPreferencesKey("dashboard")
		internal val HOME = stringPreferencesKey("home")
		internal val UPDATE = booleanPreferencesKey("update")
		internal val IS_FIRST_LAUNCH = booleanPreferencesKey("launch")
		internal val IS_AGREE = booleanPreferencesKey("agree")
	}
}
