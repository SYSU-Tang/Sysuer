package com.sysu.edu.academic

import android.app.Application
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.alibaba.fastjson2.JSONObject
import com.sysu.edu.R
import com.sysu.edu.api.CommonUtil.extractValue
import com.sysu.edu.model.JwxtModel
import com.sysu.edu.view.RowData
import com.sysu.edu.view.SectionData
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.util.regex.Pattern

data class CourseDetailNav(
	val teachingClassId: String,
	val courseNum: String,
	val teachingClassNum: String,
                          )

class CourseSelectedViewModel(application: Application) : AndroidViewModel(application) {
	private val model: JwxtModel = JwxtModel(application)
	private val _sections: SnapshotStateList<SectionData> = mutableStateListOf()
	val sections: SnapshotStateList<SectionData> = _sections
	private val _navigationEvents = MutableSharedFlow<CourseDetailNav>()
	val navigationEvents: SharedFlow<CourseDetailNav> = _navigationEvents.asSharedFlow()
	private var page = 0
	private var total = -1
	
	init {
		model.message.observeForever { (_, response) ->
			if (response.getInteger("code") == 200) {
				val data = response.getJSONObject("data")
				if (total == -1) total = data.getInteger("total")
				data.getJSONArray("rows").forEach { item ->
					val rows = mutableStateListOf<RowData>()
					val teachingTimePlace = (item as JSONObject).getString("teachingTimePlace")
					if (teachingTimePlace.isNullOrEmpty()) {
						rows.add(RowData(application.getString(R.string.course_arrangement), application.getString(R.string.none)))
					}
					else {
						Pattern.compile(",").splitAsStream(teachingTimePlace).forEach { s ->
							rows.add(RowData(application.getString(R.string.course_arrangement), s.replace(";", "/")))
						}
					}
					rows.addAll(extractValue(application,
					                         item,
					                         intArrayOf(R.string.course_name,
					                                    R.string.course_category,
					                                    R.string.open_unit,
					                                    R.string.exam_time,
					                                    R.string.exam_mode,
					                                    R.string.credit,
					                                    R.string.teaching_class_id,
					                                    R.string.class_number,
					                                    R.string.class_name,
					                                    R.string.course_number),
					                         arrayOf("courseName", "courseCategoryName", "courseUnitName", "scheduleExamTime", "examFormName", "credit", "teachingClassId", "teachingClassNum", "teachingClassName", "courseNum")))
					sections.add(SectionData(title = item.getString("courseName"), rows = rows, footerMenus = mutableStateListOf(com.sysu.edu.view.MenuItem(title = application.getString(R.string.course_detail), onClick = {
						viewModelScope.launch {
							_navigationEvents.emit(CourseDetailNav(
								teachingClassId = item.getString("teachingClassId"),
								courseNum = item.getString("courseNum"),
								teachingClassNum = item.getString("teachingClassNum"),
							                                      ))
						}
						true
					}))))
				}
			}
		}
	}
	
	fun fetchCourseList(courseName: String = "") {
		model.addAndNext("jwxt/choose-course-front-server/selectedCourse/list",
		                 "{\"pageNo\":${++page},\"pageSize\":10,\"total\":true,\"param\":{\"courseName\":\"$courseName\",\"successStatus\":\"1\",\"failureStatus\":\"0\",\"retiredClass\":\"0\",\"waitingScreen\":\"0\"}}",
		                 1)
	}
	
	fun reFetchCourseList(courseName: String = "") {
		sections.clear()
		page = 0
		total = -1
		fetchCourseList(courseName)
	}
	
	fun hasMore(): Boolean = page * 10 < total
	override fun onCleared() {
		model.dispose()
	}
}