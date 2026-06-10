package com.sysu.edu.extra

import android.content.pm.PackageManager
import android.os.Bundle
import androidx.core.content.pm.PackageInfoCompat
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.sysu.edu.R
import java.util.Objects

class AboutFragment : PreferenceFragmentCompat() {
	override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
		setPreferencesFromResource(R.xml.about, rootKey)
		try {
			val version = requireContext().packageManager.getPackageInfo(requireContext().packageName, 0)
			val versionPreference = Objects.requireNonNull<Preference>(findPreference("version"))
			versionPreference.setSummary("${version.versionName}(${PackageInfoCompat.getLongVersionCode(version)})")
			//            versionPreference.setOnPreferenceClickListener(_ -> {
//                ((AboutActivity) requireActivity()).checkUpdate();
//                return false;
//            });
		} catch (e: PackageManager.NameNotFoundException) {
			throw RuntimeException(e)
		}
	}
}