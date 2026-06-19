package com.sysu.edu.extra

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Pair
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.alibaba.fastjson2.JSONObject
import com.sysu.edu.R
import com.sysu.edu.api.AuthorizationJar
import com.sysu.edu.api.HttpManager
import com.sysu.edu.api.Config
import com.sysu.edu.api.TargetUrl
import io.reactivex.rxjava3.disposables.CompositeDisposable

class PrivacyFragment : PreferenceFragmentCompat() {
	var http: HttpManager? = null
	val disposable: CompositeDisposable = CompositeDisposable()
	override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
		if (savedInstanceState == null) {
			setPreferencesFromResource(R.xml.privacy, rootKey)
			val config = Config(this)
			disposable.add(config.contextUtil.accountManager.getActiveAccountAsync("sysu.edu.cn").subscribe { activeAccount: Pair<String?, String?>? ->
				(findPreference("netId") as Preference?)?.setSummary(activeAccount!!.first)
				(findPreference("password") as Preference?)?.setOnPreferenceClickListener { _: Preference? ->
					config.toast(activeAccount?.second)
					false
				}
			})
			config.setCallback { this.info }
			http = HttpManager(object : Handler(Looper.getMainLooper()) {
				override fun handleMessage(msg: Message) {
					if (msg.what == -1) config.toast(R.string.no_net_connected)
					else {
						val response = JSONObject.parseObject(msg.obj as String?)
						if (response != null && response.getInteger("code") == 200) {
							if (response.get("data") != null) {
								if (msg.what == 0) {
									val data = response.getJSONObject("data")
									val keyName: Array<String> = arrayOf("姓名", "学号", "证件类别", "证件号码", "电话", "邮箱")
									for (i in keyName.indices) {
										val p = Preference(requireContext()).apply {
											title = keyName[i]
											summary = data.getString(arrayOf("userName", "userCode", "idTypeStr", "idNum", "tele", "email")[i])
											setIcon(intArrayOf(R.drawable.name, R.drawable.id, R.drawable.card, R.drawable.account, R.drawable.phone, R.drawable.email)[i])
											setOnPreferenceClickListener { preference: Preference? ->
												config.copy(preference!!.title as String?, preference.getSummary() as String?)
												config.toast(R.string.copy_successfully)
												false
											}
										}
										preferenceScreen.addPreference(p)
									}
								}
							}
						} else if (response != null && response.getInteger("code") == 1003) config.gotoLogin(TargetUrl.PAY)
						else if (response != null) config.toast(response.getString("message"))
					}
				}
			}).apply {
				setParams(config)
				setAuthorizationJar(AuthorizationJar(requireContext()))
				setTokenRequired(true)
				setReferrer("https://pay.sysu.edu.cn/")
			}
			this.info
		}
	}
	
	val info: Unit
		get() {
			http!!.postRequest("https://pay.sysu.edu.cn/client/api/client/person/get", "{}", 0)
		}
	
	override fun onDestroyView() {
		super.onDestroyView()
		disposable.clear()
	}
}