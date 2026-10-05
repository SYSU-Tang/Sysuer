package com.miyuyan.sysuer.api

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.miyuyan.sysuer.preference.SettingPreference

class PreferenceViewModel(application: Application) : AndroidViewModel(application) {
	private val settingPreference = SettingPreference(application)
	val isAgreeLiveData: MutableLiveData<Boolean> = MutableLiveData()
	val dashboardLiveData: MutableLiveData<MutableSet<String?>> =
		MutableLiveData()

	val theme: String?
		get() = settingPreference.theme

	init {
		isAgreeLiveData.value = isAgree
		dashboardLiveData.value = dashboard
	}

	val dashboard: MutableSet<String?>
		get() = settingPreference.dashboard.map { it as String? }.toMutableSet()
	val home: String?
		get() = settingPreference.home
	val language: String?
		get() = settingPreference.language
	val qrcode: String?
		get() = settingPreference.qrCode
	var isAgree: Boolean
		get() = settingPreference.isAgree
		set(isAgree) {
			isAgreeLiveData.value = isAgree
			settingPreference.isAgree = isAgree
		}
	var isFirstLaunch: Boolean
		get() = settingPreference.isFirstLaunch
		set(isFirstLaunch) {
			settingPreference.isFirstLaunch = isFirstLaunch
		}
	val update: Boolean
		get() = settingPreference.update
}
