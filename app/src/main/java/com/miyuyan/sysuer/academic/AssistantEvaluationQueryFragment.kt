package com.miyuyan.sysuer.academic

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.navigation.NavOptions
import androidx.navigation.Navigation.findNavController
import androidx.navigation.fragment.FragmentNavigator
import androidx.preference.PreferenceFragmentCompat
import com.alibaba.fastjson2.JSONObject
import com.miyuyan.sysuer.R
import com.miyuyan.sysuer.api.CommonUtil
import com.miyuyan.sysuer.api.CommonUtil.extractValue
import com.miyuyan.sysuer.databinding.FragmentQueryBinding
import com.miyuyan.sysuer.model.JwxtModel
import com.miyuyan.sysuer.preference.FilterPreference
import com.miyuyan.sysuer.preference.PreferenceUtil
import rikka.preference.SimpleMenuPreference

class AssistantEvaluationQueryFragment : PreferenceFragmentCompat() {
	lateinit var binding: FragmentQueryBinding
	lateinit var model: JwxtModel
	override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
		setPreferencesFromResource(R.xml.assisant_evaluation, rootKey)
	}
	
	override fun onCreateView(inflater: LayoutInflater,
	                          container: ViewGroup?,
	                          savedInstanceState: Bundle?): View {
		if (savedInstanceState == null) {
			model = JwxtModel(requireContext())
			binding = FragmentQueryBinding.inflate(inflater, container, false).apply {
				root.addView(super.onCreateView(inflater, root, null))
				fab.setOnClickListener {
					val data = Bundle()
					data.putString("params", "$params")
					findNavController(root).navigate(R.id.assistant_evaluation_result, data, NavOptions.Builder()
						.setEnterAnim(android.R.anim.fade_in)
						.setExitAnim(android.R.anim.fade_out)
						.build(), FragmentNavigator.Extras.Builder()
														 .addSharedElement(fab, "query")
														 .build())
				}
			}
			val unit = findPreference<FilterPreference>("unit")
			unit?.valueLiveData
				?.observe(requireActivity(), Observer { params: String? -> this.getUnit(params) })
			model.message.observe(requireActivity(), Observer { message: CommonUtil.Tuple2<Int, JSONObject> ->
				val response = message.second
				if (response.getInteger("code") == 200) {
					when (message.first) {
						0 -> {
							val years = extractValue(response.getJSONArray("data"), "acadYearSemester").toTypedArray<String?>()
							findPreference<SimpleMenuPreference>("yearTerm")?.apply {
								entries = years
								entryValues = years
							}
							getUnit(unit?.value)
						}
						1 -> {
							val extractValue = extractValue(response.getJSONArray("data"), "departmentName", "departmentNumber")
							unit?.entryValues = extractValue.first!!.toTypedArray<String?>()
							unit?.entryValues = extractValue.second!!.toTypedArray<String?>()
						}
					}
					model.nextAll()
				}
			})
			yearTerm
			model.next()
		}
		return binding.root
	}
	
	val yearTerm: Unit
		get() {
			model.add("jwxt/base-info/acadyearterm/findAcadyeartermNamesBox", 0)
		}
	
	fun getUnit(params: String?) {
		model.add("jwxt/base-info/department/findCommonDepartmentPull?nameParm=$params", 1)
	}
	
	val params: JSONObject
		get() {
			return PreferenceUtil(this).apply {
				insertMenuValue("yearTerm", "yearTerm")
				insertEditValue("teacher", "teacherName")
				insertEditValue("courseName", "courseName")
				insertFilterValue("unit", "openUnitNum")
			}.params
		}
	
	override fun onDestroyView() {
		super.onDestroyView()
		model.dispose()
	}
}