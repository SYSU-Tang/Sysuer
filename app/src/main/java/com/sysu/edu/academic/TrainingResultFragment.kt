package com.sysu.edu.academic

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import com.alibaba.fastjson2.JSONObject
import com.sysu.edu.R
import com.sysu.edu.api.CommonUtil
import com.sysu.edu.api.CommonUtil.extractValue
import com.sysu.edu.databinding.FragmentTrainingResultBinding
import com.sysu.edu.model.JwxtModel
import com.sysu.edu.view.StaggeredFragment

class TrainingResultFragment : Fragment() {
	var page: Int = 0
	var total: Int = -1
	lateinit var model: JwxtModel
	override fun onDestroyView() {
		super.onDestroyView()
		model.dispose()
	}
	
	override fun onCreateView(inflater: LayoutInflater,
	                          container: ViewGroup?,
	                          savedInstanceState: Bundle?): View {
		val binding = FragmentTrainingResultBinding.inflate(inflater, container, false)
		model = JwxtModel(requireActivity())
		val staggeredFragment = StaggeredFragment()
		getParentFragmentManager().beginTransaction().add(R.id.result, staggeredFragment).commit()
		staggeredFragment.setScrollBottom {
			if (total > page * 10) selectedCourses
		}
		binding.export.setOnClickListener { staggeredFragment.export(binding.export, getString(R.string.result)) }
		model.message.observe(requireActivity(), Observer { message: CommonUtil.Tuple2<Int, JSONObject> ->
			val response = message.second
			if (response.getIntValue("code") == 200) {
				if (message.first == 1) {
					val data = response.getJSONObject("data")
					total = data.getInteger("total")
					data.getJSONArray("rows").forEach { o: Any? ->
						staggeredFragment.add((o as JSONObject).getString("name"), R.drawable.book, mutableListOf("专业", "年级", "学院", "培养类别", "修业年限", "学科门类", "学位", "专业代码", "专业ID"), extractValue(o, arrayOf("professionName", "grade", "manageUnitName", "trainTypeName", "educationalSystem", "disciplineCateName", "degreeGrantName", "professionCode", "professionId")))
					}
				}
			}
		})
		selectedCourses
		return binding.getRoot()
	}
	
	fun getSelectedCourses(unit: String?, grade: String?, profession: String?, trainType: String?) {
		model.addAndNext("jwxt/training-programe/training-programe/undergradute/profession-info", "{\"pageNo\":${++page},\"pageSize\":10,\"total\":true,\"param\":{\"manageUnitNum\":\"$unit\",\"grade\":\"$grade\",\"professionCode\":\"$profession\",\"trainTypeCode\":\"$trainType\"}}", 1)
	}
	
	val selectedCourses: Unit
		get() {
			getSelectedCourses(requireArguments().getString("unit"), requireArguments().getString("grade"), requireArguments().getString("profession"), requireArguments().getString("type"))
		}
}