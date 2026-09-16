package com.miyuyan.sysuer.academic

import kotlinx.coroutines.launch
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.Lifecycle
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import androidx.lifecycle.ViewModelProvider
import com.alibaba.fastjson2.JSONObject
import com.miyuyan.sysuer.BaseActivity
import com.miyuyan.sysuer.databinding.ActivityLeaveReturnRegistrationBinding
import com.miyuyan.sysuer.model.XgxtModel

class LeaveReturnRegistrationActivity : BaseActivity() {
	lateinit var model: XgxtModel
	override fun onDestroy() {
		super.onDestroy()
		model.dispose()
	}
	
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		model = XgxtModel(this)
		val viewModel = ViewModelProvider(this).get<LeaveReturnRegistrationViewModel>(LeaveReturnRegistrationViewModel::class.java)
		val binding = ActivityLeaveReturnRegistrationBinding.inflate(layoutInflater).apply {
			toolbar.setNavigationOnClickListener { supportFinishAfterTransition() }
		}
		setContentView(binding.root)
		lifecycleScope.launch {
			repeatOnLifecycle(Lifecycle.State.STARTED) {
				model.messageChannel.collect {
				(first, response) ->
						if (response.getInteger("code") == 200) {
				if (first == 0) {
					response.getJSONArray("data")?.let {
						val years = ArrayList<String?>()
						it.forEach { o: Any? -> years.add((o as JSONObject).getString("label", "")) }
						binding.years.apply {
							setSimpleItems(years.toTypedArray<String?>())
							setOnItemClickListener { _: AdapterView<*>?, _: View?, position: Int, _: Long ->
								viewModel.year.value = it.getJSONObject(position).getString("value")
							}
							setText(years[0], false)
						}
						viewModel.year.value = it.getJSONObject(0).getString("value")
					}
				}
			}
		}
		}
	}
		years()
	}
	
	private fun years() {
		model.addAndNext("jjrlfx/api/sm-jjrlfx/student/school-year", 0)
	}
}