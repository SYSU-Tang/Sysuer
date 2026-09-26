package com.miyuyan.sysuer.academic

import android.app.Application
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.alibaba.fastjson2.JSONObject
import com.miyuyan.sysuer.R
import com.miyuyan.sysuer.api.CommonUtil
import com.miyuyan.sysuer.model.JwxtModel
import com.miyuyan.sysuer.nav.CourseDetail
import com.miyuyan.sysuer.view.MenuItem
import com.miyuyan.sysuer.view.SectionData
import com.miyuyan.sysuer.view.UiState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class CourseCompletionViewModel(application: Application) : AndroidViewModel(application) {
	private val model = JwxtModel(application)
	private val _creditHours = MutableSharedFlow<List<JSONObject>>()
	val creditHours: SharedFlow<List<JSONObject>> = _creditHours.asSharedFlow()
	private val _courseList = mutableStateListOf<SectionData>()
	val courseList: SnapshotStateList<SectionData> = _courseList
	private val _navigationEvents = MutableSharedFlow<CourseDetail>()
	val navigationEvents: SharedFlow<CourseDetail> = _navigationEvents.asSharedFlow()
	private var page = 0
	private var total = -1

	val creditUiState = model.getUiState(0)

	val courseUiState = model.getUiState(1)

	init {
		viewModelScope.launch {
			model.messageChannel.collect { (code, response) ->
				if (response.getInteger("code") == 200 && response.get("data") != null) {
					when (code) {
						0 -> {
							creditUiState.value =
								if (response.getJSONArray("data").isEmpty()) UiState.Empty else {
									_creditHours.emit(
											response.getJSONArray("data")
												.filterIsInstance<JSONObject>()
									)
									UiState.Content
								}
						}

						1 -> {
							val data = response.getJSONObject("data")
							if (total == -1) total = data.getInteger("total")
							if (total == 0) courseUiState.value = UiState.Empty
							else data.getJSONArray("rows").filterIsInstance<JSONObject>()
								.forEach { item ->
									_courseList.add(
											SectionData(
													title = item.getString("courseName"),
													key = "course__${item.getString("courseNumber")}",
													footerMenus = mutableStateListOf(
															MenuItem(application.getString(R.string.course_outline)) {
																viewModelScope.launch {
																	_navigationEvents.emit(
																			CourseDetail(
																					courseNum = item.getString(
																							"courseNumber"
																					)
																			)
																	)
																}
																true
															},
													),
													rows = CommonUtil.extractValue(
															application, item, intArrayOf(
															R.string.academic_year_semester,
															R.string.course_number,
															R.string.course_name,
															R.string.course_category,
															R.string.credit,
															R.string.academic_year_semester,
															R.string.achievement_course_number,
															R.string.achievement_course_name,
															R.string.achievement_course_category,
															R.string.achievement_credit,
															R.string.is_passed,
															R.string.achievement_point
													), arrayOf(
															"acadYearSemester",
															"courseNumber",
															"courseName",
															"courseCategoryName",
															"credit",
															"achievementAcadYearSemester",
															"achievementCourseNumber",
															"achievementCourseName",
															"achievementCourseCategoryName",
															"achievementCredit",
															"ispassed",
															"achievementPoint"
													)
													)
											)
									)
								}
						}
					}
				}
			}
		}
	}

	fun fetchCreditHours() {
		creditUiState.value = UiState.Loading
		model.enqueue(
				"jwxt/gradua-degree/graduatemsg/studentsGraduationExamination/creditHoursStu?cultureTypeCode=01",
				"",
				0
		)
	}

	fun fetchCourseList(courseName: String = "") {
		courseUiState.value = if (page == 0) UiState.Loading
		else UiState.LoadMore
		model.enqueue(
				"jwxt/gradua-degree/graduatemsg/studentsGraduationExamination/studentCourse",
				"""{"pageNo":${++page},"pageSize":10,"total":true,"param":{"cultureTypeCode":"01","courseName":"$courseName"}}""",
				1
		)
	}

	fun refetchCourseList(courseName: String = "") {
		_courseList.clear()
		page = 0
		total = -1
		fetchCourseList(courseName)
	}

	fun hasMore(): Boolean = page * 10 < total
	override fun onCleared() {
		model.dispose()
	}
}