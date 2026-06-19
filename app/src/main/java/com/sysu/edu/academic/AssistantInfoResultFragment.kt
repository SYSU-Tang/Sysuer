package com.sysu.edu.academic

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import com.alibaba.fastjson2.JSONObject
import com.sysu.edu.api.CommonUtil
import com.sysu.edu.api.CommonUtil.extractValue
import com.sysu.edu.model.JwxtModel
import com.sysu.edu.view.StaggeredFragment

class AssistantInfoResultFragment : StaggeredFragment() {
	var page: Int = 1
	var total: Int = -1
	lateinit var model: JwxtModel
	override fun onDestroyView() {
		super.onDestroyView()
		model.dispose()
	}
	
	override fun onCreateView(inflater: LayoutInflater,
	                          container: ViewGroup?,
	                          savedInstanceState: Bundle?): View? {
		val view = super.onCreateView(inflater, container, savedInstanceState)
		model = JwxtModel(requireContext())
		setScrollBottom {
			if (page * 10 < total) result
		}
		model.message.observe(requireActivity(), Observer { message: CommonUtil.Tuple2<Int, JSONObject> ->
			val response = message.second
			if (response.getInteger("code") == 200) {
				if (message.first == 0) {
					total = response.getJSONObject("data").getInteger("total")
					response.getJSONObject("data").getJSONArray("rows").forEach { o: Any? ->
							add((o as JSONObject).getString("courseName"), mutableListOf<String?>("序号", "学年学期", "校区", "开设单位", "课程名称", "课程编号", "课程学时", "班级编号", "实选人数", "任课教师", "上课时间地点", "修读对象", "上课学生名单", "助教信息", "助教职责"), extractValue(o, arrayOf("rowNum", "semester", "studyCampus", "openUnitName", "courseName", "courseNum", "courseHour", "classNumber", "apersonNum", "teacherName", "teachingTimePlace", "studyObj", "stuList", "assistantInfo", "jobDuty")))
						}
				}
			}
		})
		result
		return view
	}
	
	fun getResult(query: String?) {
		model.addAndNext("jwxt/assistant-manage/assistantInfoQuery/pageList?code=jwxsd_zjxxck", "{\"pageNo\":${page++},\"pageSize\":10,\"total\":true,\"param\":$query}", 0)
	}
	
	val result: Unit
		get() {
			val filter = JSONObject()
			val setFilter = { key: String?, value: String? ->
				if (requireArguments().containsKey(key) && !requireArguments().getString(key)
						.isNullOrEmpty()) filter[value] = requireArguments().getString(key)
			}
			setFilter("term", "semester")
			setFilter("campus", "studyCampusCode")
			setFilter("courseNumber", "courseNum")
			setFilter("courseName", "courseName")
			setFilter("teacherName", "teacherName")
			getResult("$filter")
		}
}
