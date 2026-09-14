package com.miyuyan.sysuer.academic

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.alibaba.fastjson2.JSONObject
import com.miyuyan.sysuer.api.CommonUtil.extractValue
import com.miyuyan.sysuer.model.JwxtModel
import com.miyuyan.sysuer.view.StaggerFragment
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

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
		viewLifecycleOwner.lifecycleScope.launch {
			viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
				model.messageChannel.receiveAsFlow().collect { (code, response) ->
					if (response.getInteger("code") == 200 && response.get("data") != null) {
						if (code == 0) response.getJSONObject("data").getJSONArray("rows").forEach { a: Any? ->
							val values: ArrayList<String?> = extractValue(a as JSONObject, arrayOf("acadYearSemester", "courseNumber", "courseName", "courseCategoryName", "credit",  /**/"acadYearSemester", "achievementCourseNumber", "achievementCourseName", "achievementCourseCategoryName", "achievementCredit", "ispassed", "achievementPoint"))
							if (values[0] != null) values[0] = values[0]!!.replace(",", "|")
							if (values[5] != null) values[5] = values[5]!!.replace(",", "|")
							addSection(a.getString("courseName"), mutableListOf("学年学期", "课程号", "课程名称", "课程类别", "学分", "成绩获取学年学期", "课程号", "课程名称", "课程类别", "学分", "是否及格", "成绩"), values)
						}
					}
				}
			}
		}
		model.next()
		return view
	}
	
	val studentCourse: Unit
		get() {
			model.add("jwxt/gradua-degree/graduatemsg/studentsGraduationExamination/studentCourse", "{\"pageNo\":${++page},\"pageSize\":10,\"total\":true,\"param\":{\"cultureTypeCode\":\"01\"}}", 0)
		}
}
