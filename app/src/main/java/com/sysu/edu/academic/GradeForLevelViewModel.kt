package com.sysu.edu.academic

import android.app.Application
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.core.text.isDigitsOnly
import androidx.lifecycle.AndroidViewModel
import com.alibaba.fastjson2.JSONObject
import com.sysu.edu.R
import com.sysu.edu.api.CommonUtil.extractValue
import com.sysu.edu.model.JwxtModel
import com.sysu.edu.view.SectionData

class GradeForLevelViewModel(application: Application) : AndroidViewModel(application) {
	private val model: JwxtModel = JwxtModel(application)
	private val _sections = mutableStateListOf<SectionData>()
	val sections: SnapshotStateList<SectionData> = _sections
	private val _trainTypeOptions = mutableStateListOf<JSONObject>()
	val trainTypeOptions: SnapshotStateList<JSONObject> = _trainTypeOptions
	private val _yearOptions = mutableStateListOf<JSONObject>()
	val yearOptions: SnapshotStateList<JSONObject> = _yearOptions
	private val _courseTypeOptions = mutableStateListOf<JSONObject>()
	val courseTypeOptions: SnapshotStateList<JSONObject> = _courseTypeOptions
	var trainType: String? = null
	var year: String? = null
	var courseType: String? = null
	var courseName: String? = null
	var courseNumber: String? = null
	var minGrade: String? = null
	private var page = 1
	private var total = -1
	
	init {
		model.message.observeForever { (code, response) ->
			if (response.getInteger("code") == 200) {
				when (code) {
					0 -> _trainTypeOptions.addAll(response.getJSONArray("data").filterIsInstance<JSONObject>())
					1 -> _yearOptions.addAll(response.getJSONArray("data").filterIsInstance<JSONObject>())
					2 -> _courseTypeOptions.addAll(response.getJSONArray("data").filterIsInstance<JSONObject>())
					3 -> {
						if (total == -1) total = response.getJSONObject("data").getInteger("total")
						response.getJSONObject("data").getJSONArray("rows").forEach { item ->
							sections.add(SectionData(title = (item as JSONObject).getString("courseName"),
							                     rows = extractValue(application,
							                                         item,
							                                         intArrayOf(R.string.gpa,
							                                                    R.string.class_number,
							                                                    R.string.course_category,
							                                                    R.string.course_id,
							                                                    R.string.course_name,
							                                                    R.string.course_number,
							                                                    R.string.credit,
							                                                    R.string.exam_nature,
							                                                    R.string.level,
							                                                    R.string.grade,
							                                                    R.string.department,
							                                                    R.string.semester,
							                                                    R.string.total_hours,
							                                                    R.string.training_category,
							                                                    R.string.total_achievement),
							                                         arrayOf("achievementPoint",
							                                                 "classesNum",
							                                                 "courseCategoryName",
							                                                 "courseId",
							                                                 "courseName",
							                                                 "courseNum",
							                                                 "credit",
							                                                 "examNatureName",
							                                                 "finalAchievementStr",
							                                                 "grade",
							                                                 "openClassUnitName",
							                                                 "schoolSemester",
							                                                 "sumHours",
							                                                 "trainingCategoryName",
							                                                 "totalAchievement"))))
						}
					}
				}
				model.nextAll()
			}
		}
	}
	
	fun fetchOptions() {
		model.add("jwxt/base-info/codedata/findcodedataNames?datableNumber=97", 0)
		model.add("jwxt/base-info/acadyearterm/findAcadyeartermNamesBox", 1)
		model.add("jwxt/base-info/base-category/SfqyBox", 2)
		model.nextAll()
	}
	
	fun fetchGrade() {
		model.addAndNext("jwxt/achievement-manage/achievement/selfPageList", "{\"pageNo\":${page++},\"pageSize\":10,\"total\":true,\"param\":$args}", 3)
	}
	
	fun reFetchGrade() {
		sections.clear()
		page = 1
		total = -1
		fetchGrade()
	}
	
	fun hasMore(): Boolean = (page - 1) * 10 < total
	private val args: JSONObject
		get() {
			val args = JSONObject()
			if (!trainType.isNullOrEmpty()) args["categoryCode"] = trainType
			if (!year.isNullOrEmpty()) args["schoolSemester"] = year
			if (!courseType.isNullOrEmpty()) args["courseTypeCode"] = courseType
			if (!courseName.isNullOrEmpty()) args["courseName"] = courseName
			if (!courseNumber.isNullOrEmpty()) args["courseNum"] = courseNumber
			if (!minGrade.isNullOrEmpty() && minGrade?.isDigitsOnly() == true) args["finalAchievement"] = minGrade?.toInt()
			return args
		}
	
	override fun onCleared() {
		model.dispose()
	}
}