package com.miyuyan.sysuer.preference

import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.alibaba.fastjson2.JSONObject
import rikka.material.preference.MaterialSwitchPreference
import rikka.preference.SimpleMenuPreference

class PreferenceParamsBuilder(private val fragment: PreferenceFragmentCompat) {
	private val params: JSONObject = JSONObject()
	fun insertMenuValue(preferenceKey: String, paramsKey: String?): PreferenceParamsBuilder {
		fragment.findPreference<SimpleMenuPreference?>(preferenceKey)?.let {
			params[paramsKey] = it.value
		}
		return this
	}

	fun insertEditValue(preferenceKey: String, paramsKey: String?): PreferenceParamsBuilder {
		fragment.findPreference<EditPreference?>(preferenceKey)?.let {
			params[paramsKey] = it.value
		}
		return this
	}

	fun insertSliderValue(preferenceKey: String, paramsKey: String?): PreferenceParamsBuilder {
		fragment.findPreference<SliderPreference?>(preferenceKey)?.takeIf { it.value != 0 }?.let {
			params[paramsKey] = it.value
		}
		return this
	}

	fun insertFilterValue(preferenceKey: String, paramsKey: String?): PreferenceParamsBuilder {
		fragment.findPreference<FilterPreference?>(preferenceKey)?.let {
			params[paramsKey] = it.value
		}
		return this
	}

	fun <T> insertSwitchValue(
		preferenceKey: String, paramsKey: String?, ifChecked: T?, ifNotChecked: T?
	): PreferenceParamsBuilder {
		fragment.findPreference<MaterialSwitchPreference?>(preferenceKey)?.let {
			params[paramsKey] = if (it.isChecked) ifChecked else ifNotChecked
		}
		return this
	}

	fun insertSwitchValue(preferenceKey: String, paramsKey: String?): PreferenceParamsBuilder {
		fragment.findPreference<MaterialSwitchPreference?>(preferenceKey)?.let {
			params[paramsKey] = it.isChecked
		}
		return this
	}

	fun insertValue(
		preferenceKey: String, paramsKey: String?, defaultValue: Any? = null
	): PreferenceParamsBuilder {
		fragment.findPreference<Preference?>(preferenceKey)?.let {
			when (it) {
				is EditPreference -> params[paramsKey] = it.value
				is SliderPreference -> params[paramsKey] = it.value
				is FilterPreference -> params[paramsKey] = it.value
				is MaterialSwitchPreference -> params[paramsKey] = it.isChecked
				is SimpleMenuPreference -> params[paramsKey] = it.value
				else -> {
					params[paramsKey] = defaultValue
				}
			}
		}
		return this
	}

	fun insert(key: String?, value: Any?): PreferenceParamsBuilder {
		params[key] = value
		return this
	}

	fun build() = params
}
