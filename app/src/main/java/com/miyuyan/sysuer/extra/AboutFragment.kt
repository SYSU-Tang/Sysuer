package com.miyuyan.sysuer.extra

import android.os.Bundle
import androidx.core.content.pm.PackageInfoCompat
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.miyuyan.sysuer.R

class AboutFragment : PreferenceFragmentCompat() {
	override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
		setPreferencesFromResource(R.xml.about, rootKey)
		val version = requireContext().packageManager.getPackageInfo(requireContext().packageName, 0)
		(findPreference("version") as Preference?)?.setSummary(getString(R.string.version_info, version.versionName, PackageInfoCompat.getLongVersionCode(version)))
	}
}