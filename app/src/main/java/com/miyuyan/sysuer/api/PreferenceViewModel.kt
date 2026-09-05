package com.miyuyan.sysuer.api

import android.app.Application
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.preference.PreferenceManager

class PreferenceViewModel(application: Application) : AndroidViewModel(application) {
	val isAgreeLiveData: MutableLiveData<Boolean> = MutableLiveData()
	val dashboardLiveData: MutableLiveData<MutableSet<String?>?> = MutableLiveData<MutableSet<String?>?>()
	val sharedPreferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(application)
	fun getString(key: String?, defValue: String?): String? =
		sharedPreferences.getString(key, defValue)
	
	fun getBoolean(key: String?, defValue: Boolean): Boolean =
		sharedPreferences.getBoolean(key, defValue)
	
	val theme: String?
		get() = getString(THEME, "2")
	
	init {
		isAgreeLiveData.value = isAgree
		dashboardLiveData.value = dashboard
	}
	
	private fun getSet(key: String?, defValue: Set<String?>?): MutableSet<String?>? =
		sharedPreferences.getStringSet(key, defValue)
	
	val dashboard: MutableSet<String?>?
		get() = getSet("dashboard", (0..5).map { "$it" }.toSet())
	val home: String?
		get() = getString(HOME, "2")
	val language: String?
		get() = getString(LANGUAGE, "2")
	val qrcode: String?
		get() = getString(QRCODE, "")
	var isAgree: Boolean
		get() = getBoolean(IS_AGREE, false)
		set(isAgree) {
			sharedPreferences.edit { putBoolean(IS_AGREE, isAgree) }
		}
	var isFirstLaunch: Boolean
		get() = getBoolean(IS_FIRST_LAUNCH, false)
		set(isFirstLaunch) {
			sharedPreferences.edit { putBoolean(IS_FIRST_LAUNCH, isFirstLaunch) }
		}
	val update: Boolean
		get() = getBoolean(UPDATE, true)
	
	fun setIsAgreeLiveData(isAgree: Boolean) {
		isAgreeLiveData.value = isAgree
	}
	
	companion object {
		private const val THEME = "theme"
		private const val HOME = "home"
		private const val LANGUAGE = "language"
		private const val QRCODE = "qrcode"
		private const val UPDATE = "update"
		private const val IS_FIRST_LAUNCH = "launch"
		private const val IS_AGREE = "agree"
	}
}
