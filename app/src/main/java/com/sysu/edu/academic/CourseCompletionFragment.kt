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
import com.sysu.edu.view.StaggerFragment

class CourseCompletionFragment : StaggerFragment() {
	var page: Int = 0
	lateinit var model: JwxtModel
	override fun onDestroyView() {
		super.onDestroyView()
		model.dispose()
	}
	
	override fun onCreateView(inflater: LayoutInflater,
	                          container: ViewGroup?,
	                          savedInstanceState: Bundle?): View? {
		val view = super.onCreateView(inflater, container, savedInstanceState)
		model = JwxtModel(requireActivity())
		studentCourse
		model.message.observe(requireActivity(), Observer { message: CommonUtil.Tuple2<Int, JSONObject> ->
			val response = message.second
			if (response.getInteger("code") == 200 && response.get("data") != null) {
				if (message.first == 0) response.getJSONObject("data").getJSONArray("rows").forEach { a: Any? ->
					val values: ArrayList<String?> = extractValue(a as JSONObject, arrayOf("acadYearSemester", "courseNumber", "courseName", "courseCategoryName", "credit",  /**/"acadYearSemester", "achievementCourseNumber", "achievementCourseName", "achievementCourseCategoryName", "achievementCredit", "ispassed", "achievementPoint"))
					if (values[0] != null) values[0] = values[0]!!.replace(",", "|")
					if (values[5] != null) values[5] = values[5]!!.replace(",", "|")
					addSection(a.getString("courseName"), mutableListOf("学年学期", "课程号", "课程名称", "课程类别", "学分", "成绩获取学年学期", "课程号", "课程名称", "课程类别", "学分", "是否及格", "成绩"), values)
				}
			}
		})
		model.next()
		return view
	}
	
	val studentCourse: Unit
		get() {
			model.add("jwxt/gradua-degree/graduatemsg/studentsGraduationExamination/studentCourse", "{\"pageNo\":${++page},\"pageSize\":10,\"total\":true,\"param\":{\"cultureTypeCode\":\"01\"}}", 0)
		}
}
