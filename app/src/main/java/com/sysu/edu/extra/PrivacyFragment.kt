package com.sysu.edu.extra

import android.os.Bundle
import android.util.Pair
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.sysu.edu.R
import com.sysu.edu.api.Config
import com.sysu.edu.model.PayModel

class PrivacyFragment : PreferenceFragmentCompat() {
	val model: PayModel by lazy { PayModel(requireContext()) }
	override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
		setPreferencesFromResource(R.xml.privacy, rootKey)
		val config = Config(this)
		model.contextUtil.disposable.add(config.contextUtil.accountManager.getActiveAccountAsync("sysu.edu.cn")
			                                 .subscribe { activeAccount: Pair<String?, String?>? ->
				                                 (findPreference("netId") as Preference?)?.setSummary(
					                                 activeAccount!!.first)
				                                 (findPreference("password") as Preference?)?.setOnPreferenceClickListener { _: Preference? ->
					                                 config.toast(activeAccount?.second)
					                                 false
				                                 }
			                                 })
		model.message.observe(viewLifecycleOwner) { (code, response) ->
			if (response.getInteger("code") == 200) {
				if (response.get("data") != null) {
					if (code == 0) {
						val data = response.getJSONObject("data")
						arrayOf("姓名",
						        "学号",
						        "证件类别",
						        "证件号码",
						        "电话",
						        "邮箱").forEachIndexed { i, name ->
							val p = Preference(requireContext()).apply {
								title = name
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
								setOnPreferenceClickListener { preference: Preference? ->
									config.copy(preference!!.title as String?,
									            preference.getSummary() as String?)
									config.toast(R.string.copy_successfully)
									false
								}
							}
							preferenceScreen.addPreference(p)
						}
					}
				}
			}
			else config.toast(response.getString("message"))
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