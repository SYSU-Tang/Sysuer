package com.sysu.edu.home

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.sysu.edu.R
import com.sysu.edu.extra.SettingActivity

class AccountFragment : PreferenceFragmentCompat() {
	override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
		setPreferencesFromResource(R.xml.account, rootKey)
		val launch =
			registerForActivityResult<Intent?, ActivityResult?>(ActivityResultContracts.StartActivityForResult()) { o: ActivityResult? ->
				if (o!!.resultCode == Activity.RESULT_OK) requireActivity().recreate()
			}
		findPreference<Preference>("setting")?.setOnPreferenceClickListener { _: Preference? ->
			launch.launch(Intent(requireActivity(), SettingActivity::class.java), null)
			false
		}
	}
}
