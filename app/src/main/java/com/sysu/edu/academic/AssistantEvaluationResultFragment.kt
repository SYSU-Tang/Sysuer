package com.sysu.edu.academic

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import com.alibaba.fastjson2.JSONObject
import com.sysu.edu.api.CommonUtil
import com.sysu.edu.api.CommonUtil.extractValue
import com.sysu.edu.databinding.FragmentResultBinding
import com.sysu.edu.model.JwxtModel
import com.sysu.edu.view.StaggeredFragment

class AssistantEvaluationResultFragment : StaggeredFragment() {
	var page: Int = 1
	var total: Int = -1
	lateinit var model: JwxtModel
	override fun onDestroyView() {
		super.onDestroyView()
		model.dispose()
	}
	
	override fun onCreateView(inflater: LayoutInflater,
	                          container: ViewGroup?,
	                          savedInstanceState: Bundle?): View {
		val resultBinding = FragmentResultBinding.inflate(inflater, container, false)
		var order = 1
		resultBinding.root
			.addView(super.onCreateView(inflater, resultBinding.root, savedInstanceState))
		model = JwxtModel(requireContext())
		setScrollBottom {
			if ((page - 1) * 10 < total) result
		}
		model.message.observe(requireActivity(), Observer { message: CommonUtil.Tuple2<Int, JSONObject> ->
			val response = message.second
			if (response.getInteger("code") == 200) if (message.first == 0) {
				val data = response.getJSONObject("data")
				if (total == -1) total = data.getInteger("total")
				data.getJSONArray("rows").forEach { item: Any? ->
					add((order++).toString(), mutableListOf<String?>("学年学期", "助教学期", "助教姓名", "助教培养单位", "教学班号", "课程名称", "课程编码", "课程类别", "课程教学类型", "开课单位", "是否开班", "是否合班", "总教学班号", "任课教师", "课程学时", "助教承担的课程教学学时", "上课时间地点", "助教考核结论"), extractValue(item as JSONObject, arrayOf("yearTerm", "assistantNum", "assistantName", "assistantCollege", "classNum", "courseName", "courseNum", "courseType", "courseTeachingType", "courseCollege", "openClassFlag", "mergeClassFlag", "sumClassNum", "teacherName", "courseHours", "assistantHours", "teachingTimePlace", "conclusion")))
				}
			}
		})
		result
		return resultBinding.root
	}
	
	val result: Unit
		get() {
			model.addAndNext("jwxt/assistant-manage/assistantEvaluation/evaluationResultPageList?code=jwxsd_zjpjck", "{\"pageNo\":${page++},\"pageSize\":10,\"total\":true,\"param\":${requireArguments().getString("params")}}", 0)
		}
}