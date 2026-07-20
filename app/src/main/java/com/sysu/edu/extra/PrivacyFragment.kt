package com.sysu.edu.extra

import android.os.Bundle
import androidx.core.util.component1
import androidx.core.util.component2
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.sysu.edu.R
import com.sysu.edu.model.PayModel

class PrivacyFragment : PreferenceFragmentCompat() {
	val model: PayModel by lazy { PayModel(requireContext()) }
	override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
		setPreferencesFromResource(R.xml.privacy, rootKey)
		model.contextUtil.disposable.add(model.contextUtil.accountManager.getActiveAccountAsync("sysu.edu.cn")
			                                 .subscribe { (netId, password) ->
				                                 (findPreference("netId") as Preference?)?.setSummary(
					                                 netId)
				                                 (findPreference("password") as Preference?)?.setOnPreferenceClickListener { _: Preference? ->
					                                 model.contextUtil.toast(password)
					                                 false
				                                 }
			                                 })
		model.message.observe(this) { (code, response) ->
			if (response.getInteger("code") == 200) {
				if (response.get("data") != null) {
					if (code == 0) {
						val data = response.getJSONObject("data")
						arrayOf(R.string.name,
						        R.string.student_id,
						        R.string.id_type,
						        R.string.id_num,
						        R.string.phone,
						        R.string.email).forEachIndexed { i, name ->
							val p = Preference(requireContext()).apply {
								setTitle(name)
								summary = data.getString(arrayOf("userName",
								                                 "userCode",
								                                 "idTypeStr",
								                                 "idNum",
								                                 "tele",
								                                 "email")[i])
								setIcon(intArrayOf(R.drawable.name,
								                   R.drawable.id,
								                   R.drawable.card,
								                   R.drawable.account,
								                   R.drawable.phone,
								                   R.drawable.email)[i])
								setOnPreferenceClickListener { preference: Preference ->
									model.contextUtil.copy(preference.title as String?,
									                       preference.summary as String?)
									model.contextUtil.toast(R.string.copy_successfully)
									false
								}
							}
							preferenceScreen.addPreference(p)
						}
					}
				}
			}
			else model.contextUtil.toast(response.getString("message"))
		}
		info
	}
	
	val info: Unit
		get() {
			model.addAndNext("client/api/client/person/get", "{}", 0)
		}
	
	override fun onDestroyView() {
		super.onDestroyView()
		model.dispose()
	}
}