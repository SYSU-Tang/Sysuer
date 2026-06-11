package com.sysu.edu.extra

import android.content.ComponentName
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import com.sysu.edu.R
import com.sysu.edu.browser.BrowserPreference
import com.sysu.edu.preference.MenuPreference
import rikka.material.preference.MaterialSwitchPreference

class SettingFragment : PreferenceFragmentCompat() {
	override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
		setPreferencesFromResource(R.xml.settings, rootKey)
		((findPreference("theme")) as MenuPreference?)?.setOnPreferenceChangeListener { _: Preference?, _: Any? ->
			requireActivity().recreate()
			true
		}
		((findPreference("icon_theme")) as MenuPreference?)?.setOnPreferenceChangeListener { _: Preference?, newValue: Any? ->
			val pkg = requireContext().packageName
			with(requireActivity().packageManager) {
				setComponentEnabledSetting(ComponentName(requireActivity().baseContext, "$pkg.MainActivity"), PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP)
				setComponentEnabledSetting(ComponentName(requireActivity().baseContext, "$pkg.MainActivityLight"), (intArrayOf(1, 2, 2))[(newValue as String?)!!.toInt()], PackageManager.DONT_KILL_APP)
				setComponentEnabledSetting(ComponentName(requireActivity().baseContext, "$pkg.MainActivityDark"), (intArrayOf(2, 1, 2))[newValue.toInt()], PackageManager.DONT_KILL_APP)
				setComponentEnabledSetting(ComponentName(requireActivity().baseContext, "$pkg.MainActivityDefault"), (intArrayOf(2, 2, 1))[newValue.toInt()], PackageManager.DONT_KILL_APP)
			}
			true
		}
		((findPreference("language")) as MenuPreference?)?.setOnPreferenceChangeListener { _: Preference?, _: Any? ->
			requireActivity().recreate()
			true
		}
		((findPreference("fontSize")) as MenuPreference?)?.setOnPreferenceChangeListener { _: Preference?, _: Any? ->
			requireActivity().recreate()
			true
		}
		val browserPreference = BrowserPreference(requireContext())
		((findPreference("image_blocked")) as MaterialSwitchPreference?)?.setChecked(browserPreference.isImageBlocked)
		((findPreference("image_blocked")) as MaterialSwitchPreference?)?.setOnPreferenceChangeListener { _: Preference?, newValue: Any? ->
			browserPreference.isImageBlocked = newValue as Boolean
			true
		}
		((findPreference("js_enabled")) as MaterialSwitchPreference?)?.setChecked(browserPreference.isJSEnabled)
		((findPreference("js_enabled")) as MaterialSwitchPreference?)?.setOnPreferenceChangeListener { _: Preference?, newValue: Any? ->
			browserPreference.isJSEnabled = newValue as Boolean
			true
		}
		((findPreference("save_mobile_data_mode")) as MaterialSwitchPreference?)?.setChecked(browserPreference.isSaveMobileDataMode)
		((findPreference("save_mobile_data_mode")) as MaterialSwitchPreference?)?.setOnPreferenceChangeListener { _: Preference?, newValue: Any? ->
			browserPreference.isSaveMobileDataMode = newValue as Boolean
			true
		}
		((findPreference("privacy_mode")) as MaterialSwitchPreference?)?.setChecked(browserPreference.isPrivacyMode)
		((findPreference("privacy_mode")) as MaterialSwitchPreference?)?.setOnPreferenceChangeListener { _: Preference?, newValue: Any? ->
			browserPreference.isPrivacyMode = newValue as Boolean
			true
		}
		((findPreference("mobile_mode")) as MaterialSwitchPreference?)?.setChecked(browserPreference.isSaveMobileDataMode)
		((findPreference("mobile_mode")) as MaterialSwitchPreference?)?.setOnPreferenceChangeListener { _: Preference?, newValue: Any? ->
			browserPreference.isSaveMobileDataMode = newValue as Boolean
			true
		}
		(findPreference("developer_mode") as Preference?)?.isVisible = PreferenceManager.getDefaultSharedPreferences(requireContext()).getBoolean("developer_mode", false)
	}
}
