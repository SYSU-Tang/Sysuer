package com.sysu.edu.preference

import android.text.TextUtils
import androidx.preference.PreferenceFragmentCompat
import com.alibaba.fastjson2.JSONObject
import rikka.material.preference.MaterialSwitchPreference
import rikka.preference.SimpleMenuPreference

class PreferenceUtil(private val fragment: PreferenceFragmentCompat) {
	val params: JSONObject = JSONObject()
	fun insertMenuValue(preferenceKey: String, paramsKey: String?) {
		val preference = fragment.findPreference<SimpleMenuPreference?>(preferenceKey)
		if (preference != null && !TextUtils.isEmpty(preference.value)) params[paramsKey] = preference.value
	}
	
	fun insertEditValue(preferenceKey: String, paramsKey: String?) {
		val preference = fragment.findPreference<EditPreference?>(preferenceKey)
		if (preference != null) params[paramsKey] = preference.value
	}
	
	fun insertSliderValue(preferenceKey: String, paramsKey: String?) {
		val preference = fragment.findPreference<SliderPreference?>(preferenceKey)
		if (preference != null && preference.value != 0) params[paramsKey] = preference.value
	}
	
	fun insertFilterValue(preferenceKey: String, paramsKey: String?) {
		val preference = fragment.findPreference<FilterPreference?>(preferenceKey)
		if (preference != null) params[paramsKey] = preference.value
	}
	
	fun <T> insertSwitchValue(preferenceKey: String,
	                          paramsKey: String?,
	                          ifChecked: T?,
	                          ifNotChecked: T?) {
		val preference = fragment.findPreference<MaterialSwitchPreference?>(preferenceKey)
		if (preference != null) params[paramsKey] = if (preference.isChecked) ifChecked else ifNotChecked
	}
	
	fun insertSwitchValue(preferenceKey: String, paramsKey: String?) {
		val preference = fragment.findPreference<MaterialSwitchPreference?>(preferenceKey)
		if (preference != null) params[paramsKey] = preference.isChecked
	}
	
	fun insert(key: String?, value: Any?) {
		params[key] = value
	}
}
