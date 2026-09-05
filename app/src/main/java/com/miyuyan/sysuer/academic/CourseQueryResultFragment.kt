package com.miyuyan.sysuer.academic

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import com.alibaba.fastjson2.JSONObject
import com.miyuyan.sysuer.R
import com.miyuyan.sysuer.api.CommonUtil
import com.miyuyan.sysuer.api.CommonUtil.extractValue
import com.miyuyan.sysuer.databinding.FragmentCourseQueryResultBinding
import com.miyuyan.sysuer.model.JwxtModel
import com.miyuyan.sysuer.view.StaggerFragment

class CourseQueryResultFragment : StaggerFragment() {
	var page: Int = 1
	var total: Int = -1
	lateinit var model: JwxtModel
	override fun onDestroyView() {
		super.onDestroyView()
		model.dispose()
	}
	
	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?,
	                         ): View {
		model = JwxtModel(requireContext())
		val courseQueryResultBinding = FragmentCourseQueryResultBinding.inflate(inflater, container, false).apply {
				root.addView(super.onCreateView(inflater, container, savedInstanceState), -1, -1)
				fab.setOnClickListener {
					export(fab, getString(R.string.course))
				}
			}
		setScrollBottom {
			if ((page - 1) * 10 < total) courses
		}
		model.message.observe(requireActivity(), Observer { message: CommonUtil.Tuple2<Int, JSONObject> ->
			val response = message.second
			if (response.getInteger("code") == 200) {
				if (total == -1) total = response.getJSONObject("data").getInteger("total")
				response.getJSONObject("data").getJSONArray("rows").forEach { e: Any? ->
					val values: ArrayList<String?> = extractValue(e as JSONObject,
					                                              arrayOf("yearTerm", "courseName", "courseNum", "openingUnitName", "courseCategoryName", "score", "teachingName", "limitNumber", "selectedNumber", "examMode", "teachingTimePlaceStr", "openingSchoolName", "readObj", "classNumber"))
					if (values[10] != null) values[10] = values[10]!!.replace(",", "\n").replace("/", " | ")
					addSection(e.getString("courseName"), mutableListOf("学年学期", "课程名称", "课程编号", "开课单位", "课程类别", "学分", "主讲教师", "限选人数", "已选人数", "考试方式", "上课信息", "上课校区", "修读对象", "教学班号"), values)
				}
			}
		})
		courses
		return courseQueryResultBinding.getRoot()
	}
	
	val courses: Unit
		get() {
			model.addAndNext("jwxt/schedule/agg/schoolOpeningCoursesSchedule/querySchoolOpeningCourses", "{\"pageNo\":${page++},\"pageSize\":10,\"total\":true,\"param\":${requireArguments().getString("params")}}", 0)
		}
	
	fun reset() {
		clear()
		page = 1
		total = -1
	}
}