package com.miyuyan.sysuer.academic

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.alibaba.fastjson2.JSONObject
import com.miyuyan.sysuer.R
import com.miyuyan.sysuer.api.CommonUtil
import com.miyuyan.sysuer.api.CommonUtil.extractValue
import com.miyuyan.sysuer.model.XgxtModel
import com.miyuyan.sysuer.view.StaggerFragment

class LeaveReturnListFragment : StaggerFragment() {
	lateinit var model: XgxtModel
	override fun onDestroyView() {
		super.onDestroyView()
		model.dispose()
	}
	
	override fun onCreateView(inflater: LayoutInflater,
	                          container: ViewGroup?,
	                          savedInstanceState: Bundle?): View? {
		val view = super.onCreateView(inflater, container, savedInstanceState)
		model = XgxtModel(requireContext())
		val viewModel = ViewModelProvider(requireActivity())[LeaveReturnRegistrationViewModel::class.java]
		viewModel.year.observe(getViewLifecycleOwner(), Observer { year: String? -> this.getList(year) })
		model.message.observe(requireActivity(), Observer { message: CommonUtil.Tuple2<Int, JSONObject> ->
			val response = message.second
			if (response.getInteger("code") == 200) {
				clear()
				response.getJSONArray("data").forEachIndexed { index, e ->
					val item = e as JSONObject
					addSection(item.getString("gzmc"), if (item.getInteger("gzztm") == 1) R.drawable.uncheck else R.drawable.check, resources.getStringArray(R.array.registration_keys).toMutableList(), extractValue(item, arrayOf("blxn", "lxdjsj", "gzsm", "jjrmc", "jjrrq", "gzzt", "zt")))

					val isRegistering = item.getInteger("gzztm") == 1
					val status = item.getString("zt")

					sectionAdapter.setSectionFooter(index) {
						Button(
							onClick = {
								if (isRegistering) requireActivity().supportFragmentManager.beginTransaction()
									.replace(R.id.leave_return_list_fragment, LeaveReturnRegistrationFragment::class.java, Bundle().apply {
										putString("Id", item.getString("cjlfxgzId"))
									})
									.addToBackStack(null)
									.commit()
							},
							modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
						) {
							Text(getString(if (isRegistering) if ("registering" == status) R.string.start_registration else R.string.modify_registration else R.string.view_detail))
						}
					}
				}
			}
		})
		return view
	}
	
	fun getList(year: String?) {
		model.addAndNext("jjrlfx/api/sm-jjrlfx/student/work-list?blxn=$year", 0)
	}
}