package com.miyuyan.sysuer.academic

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.alibaba.fastjson2.JSONObject
import com.miyuyan.sysuer.R
import com.miyuyan.sysuer.api.DownloadManager
import com.miyuyan.sysuer.model.JwxtModel
import com.miyuyan.sysuer.view.RecyclerStateViewModel
import com.miyuyan.sysuer.view.UiState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** 一门自定义课程：名称、地点与若干时间段 */
data class CourseData(
	val name: String,
	val location: String,
	val segments: List<CourseTimeSegmentData>,
)

data class CourseTimeSegmentData(
	val term: String?,
	val dayIndex: Int,
	val weekStart: Int,
	val weekEnd: Int,
	val sectionStart: Int,
	val sectionEnd: Int,
)

/** 课程表上的一张课程卡片 */
data class CourseCard(
	val name: String,
	val teacher: String,
	val campus: String,
	val building: String,
	val classroom: String,
	val assistant: String?,
	val day: Int,
	val startSection: Int,
	val endSection: Int,
	val isStop: Boolean,
	val classesId: String,
) {
	val location: String
		get() = "$campus-$building-$classroom"
}

/** 从课程详情弹窗跳转课程详情页所需参数 */
data class CourseDetailNav(val courseId: String, val courseNum: String)

/**
 * 课程表：按学期与周次查询学生课表，支持周次切换、今日定位、导出与自定义课程
 */
class CourseScheduleViewModel(application: Application) : AndroidViewModel(application),
	RecyclerStateViewModel {
	private val model = JwxtModel(application)

	val currentTerm = MutableStateFlow("")
	val terms = MutableStateFlow<List<String>>(emptyList())
	val weeks = MutableStateFlow<List<Int>>(emptyList())
	val weekIndex = MutableStateFlow(-1)
	val week = MutableStateFlow(0)
	val weekStartDate = MutableStateFlow<LocalDate?>(null)
	val courses = MutableStateFlow<List<CourseCard>>(emptyList())
	val realTimeTerm = MutableStateFlow("")
	val realTimeWeekIndex = MutableStateFlow(-1)

	private var selectedCourses: List<JSONObject> = emptyList()
	private var pendingCourseName: String? = null

	private val _uiState = model.getUiState(SCHEDULE)
	override val uiState: StateFlow<UiState> = _uiState.asStateFlow()

	private val _snackbarMessage = MutableSharedFlow<String>(extraBufferCapacity = 16)
	val snackbarMessage: SharedFlow<String> = _snackbarMessage.asSharedFlow()

	private val _courseDetailNav = MutableSharedFlow<CourseDetailNav>(extraBufferCapacity = 16)
	val courseDetailNav: SharedFlow<CourseDetailNav> = _courseDetailNav.asSharedFlow()

	init {
		viewModelScope.launch {
			model.message.collect { (code, response) ->
				if (response.getInteger("code") != 200) return@collect
				when (code) {
					SCHEDULE -> handleSchedule(response)
					NEW_TERM -> handleNewTerm(response)
					WEEK_RANGE -> handleWeekRange(response)
					TERM_LIST -> handleTermList(response)
					WEEK_LIST -> handleWeekList(response)
					SELECTED_COURSES -> handleSelectedCourses(response)
				}
			}
		}
		term()
	}

	private fun handleSchedule(response: JSONObject) {
		val list = mutableListOf<CourseCard>()
		response.getJSONArray("data").forEach { e: Any? ->
			val data = e as JSONObject
			val day = data.getString("week")?.toIntOrNull() ?: return@forEach
			val startClassTimes = data.getInteger("startClassTimes") ?: return@forEach
			val endClassTimes = data.getInteger("endClassTimes") ?: return@forEach
			data.getJSONArray("teachingInfoList").forEach { detail: Any? ->
				val info = detail as JSONObject
				list.add(
						CourseCard(
								name = info.getString("courseName") ?: "",
								teacher = info.getString("teacherName") ?: "",
								campus = info.getString("teachingCampusName") ?: "",
								building = info.getString("teachingBuildingName") ?: "",
								classroom = info.getString("classroomNum") ?: "",
								assistant = info.getString("assistantInfo"),
								day = day,
								startSection = startClassTimes,
								endSection = endClassTimes,
								isStop = info.getString("whetherStopClass")?.let { it != "0" } == true,
								classesId = info.getString("classesId") ?: "",
						)
				)
			}
		}
		courses.value = list
		_uiState.value = if (list.isEmpty()) UiState.Empty else UiState.Content
	}

	private fun handleNewTerm(response: JSONObject) {
		val term = response.getJSONObject("data").getString("acadYearSemester")
		currentTerm.value = term
		realTimeTerm.value = term
		availableTerms()
		getAvailableWeeks(term)
		getCourseSchedule(term, week.value)
	}

	private fun handleWeekRange(response: JSONObject) {
		val start = response.getJSONObject("data")?.getString("startTime") ?: return
		weekStartDate.value = LocalDate.parse(start, DateTimeFormatter.ofPattern("yyyy-MM-dd"))
	}

	private fun handleTermList(response: JSONObject) {
		terms.value = response.getJSONArray("data")
			.map { (it as JSONObject).getString("acadYearSemester") }
	}

	private fun handleWeekList(response: JSONObject) {
		val data = response.getJSONObject("data")
		data.getString("nowWeekly")?.toIntOrNull()?.let { week.value = it }
		weeks.value = data.getJSONArray("weeklyList")
			.map { (it as JSONObject).getInteger("weekly") }
		weekIndex.value = weeks.value.indexOf(week.value)
		realTimeWeekIndex.value = weekIndex.value
		getCourseSchedule(currentTerm.value, week.value)
	}

	private fun handleSelectedCourses(response: JSONObject) {
		val rows = response.getJSONObject("data").getJSONArray("rows")
		if (rows != null) selectedCourses = rows.filterIsInstance<JSONObject>()
		val courseName = pendingCourseName
		val matched = selectedCourses.firstOrNull { it.getString("courseName") == courseName }
		if (matched != null) {
			_courseDetailNav.tryEmit(
					CourseDetailNav(
							courseId = matched.getString("teachingClassId") ?: "",
							courseNum = matched.getString("courseNum") ?: "",
					)
			)
		} else model.toast(getApplication<Application>().getString(R.string.course_not_found))
	}

	/** 当前学期：showNewAcadlist */
	private fun term() {
		model.enqueue("jwxt/base-info/acadyearterm/showNewAcadlist", NEW_TERM)
	}

	/** 学期内可选周次：school-calender/weekly */
	private fun getAvailableWeeks(academicYear: String?) {
		model.enqueue("jwxt/base-info/school-calender/weekly?academicYear=$academicYear", WEEK_LIST)
	}

	/** 全部可选学期 */
	private fun availableTerms() {
		model.enqueue("jwxt/base-info/acadyearterm/findAcadyeartermNamesBox", TERM_LIST)
	}

	/** 某周日期范围：school-calender */
	private fun getWeekRange(academicYear: String, week: Int) {
		model.enqueue(
				String.format(
						Locale.getDefault(),
						"jwxt/base-info/school-calender?academicYear=%s&weekly=%d",
						academicYear,
						week
				), WEEK_RANGE
		)
	}

	/** 某学期某周的课程表 */
	fun getCourseSchedule(academicYear: String, week: Int) {
		if (academicYear.isNotEmpty() && week > 0) {
			if (_uiState.value == UiState.Unstarted) _uiState.value = UiState.Loading
			model.enqueue(
					"jwxt/timetable-search/classTableInfo/queryStudentClassTable?academicYear=$academicYear&weekly=$week",
					SCHEDULE
			)
		}
	}

	/** 打开课程详情：有缓存的选课结果直接跳转，否则先查询 */
	fun openCourseDetail(courseName: String?) {
		selectedCourses.firstOrNull { it.getString("courseName") == courseName }?.let {
			_courseDetailNav.tryEmit(
					CourseDetailNav(
							courseId = it.getString("teachingClassId") ?: "",
							courseNum = it.getString("courseNum") ?: "",
					)
			)
		} ?: getSelectedCourses(courseName)
	}

	/** 查询选课结果以跳转课程详情 */
	fun getSelectedCourses(courseName: String?) {
		pendingCourseName = courseName
		model.enqueue(
				"jwxt/choose-course-front-server/electiveCourseResult/queryHistory",
				"{\"pageNo\":1,\"pageSize\":100,\"total\":true,\"param\":{\"yearTerm\":\"${currentTerm.value}\",\"successStatus\":\"1\",\"failureStatus\":\"0\",\"retiredClass\":\"0\",\"waitingScreen\":\"0\"}}",
				SELECTED_COURSES
		)
	}

	/** 切换学期并刷新周次、课表与日期范围 */
	fun changeTerm(newTerm: String) {
		if (newTerm == currentTerm.value) return
		currentTerm.value = newTerm
		getAvailableWeeks(newTerm)
		getCourseSchedule(newTerm, week.value)
		getWeekRange(newTerm, week.value)
	}

	/** 按索引切换周次 */
	fun changeWeek(newWeekIndex: Int) {
		val list = weeks.value
		if (newWeekIndex < 0) model.toast(R.string.first_week_warning)
		else if (newWeekIndex >= list.size) model.toast(R.string.last_week_warning)
		else {
			week.value = list[newWeekIndex]
			weekIndex.value = newWeekIndex
			getCourseSchedule(currentTerm.value, week.value)
			getWeekRange(currentTerm.value, week.value)
		}
	}

	/** 回到今天：切换到当前学期与当前周 */
	fun changeTermWeek(newTerm: String, newWeekIndex: Int) {
		if (newTerm != currentTerm.value) {
			currentTerm.value = newTerm
			getAvailableWeeks(newTerm)
		}
		val list = weeks.value
		if (newWeekIndex < 0) model.toast(R.string.first_week_warning)
		else if (newWeekIndex >= list.size) model.toast(R.string.last_week_warning)
		else if (newWeekIndex != weekIndex.value) {
			week.value = list[newWeekIndex]
			weekIndex.value = newWeekIndex
		}
		getCourseSchedule(currentTerm.value, week.value)
		getWeekRange(currentTerm.value, week.value)
	}

	/** 导出课表 PDF */
	fun printTable() {
		val request = model.http.generateRequest(
				"https://${model.host}/jwxt/timetable-search/stuTimeTabPrint/output",
				"acadYear=${currentTerm.value}&submitFlag=1&containKey=1%2C2%2C3%2C4%2C5",
				"application/x-www-form-urlencoded"
		).header("Cookie", model.cookie).header("Referer", "https://jwxt.sysu.edu.cn/")
			.header("Accept-Encoding", "identity").build()
		DownloadManager.downloadFile(getApplication(), request, "")
	}

	/** 保存自定义课程（当前仅提示成功），返回是否通过校验 */
	fun saveCourse(data: CourseData): Boolean {
		if (data.name.isBlank()) {
			model.toast(R.string.course_name_empty_warning)
			return false
		}
		if (data.segments.isEmpty()) {
			model.toast(R.string.no_time_segment_warning)
			return false
		}
		Log.d(TAG, "saveCourse: $data")
		model.toast(R.string.course_add_success)
		return true
	}

	override fun retry() {
		_uiState.value = UiState.Loading
		getCourseSchedule(currentTerm.value, week.value)
	}

	override fun onCleared() {
		model.dispose()
	}

	companion object {
		private const val TAG = "CourseScheduleViewModel"
		const val SCHEDULE = 1
		const val NEW_TERM = 2
		const val WEEK_RANGE = 3
		const val TERM_LIST = 4
		const val WEEK_LIST = 5
		const val SELECTED_COURSES = 6
	}
}
