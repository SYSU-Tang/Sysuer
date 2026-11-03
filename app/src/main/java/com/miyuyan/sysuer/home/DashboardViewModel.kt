package com.miyuyan.sysuer.home

import android.app.Application
import android.content.ComponentName
import android.content.Intent
import android.content.pm.ShortcutInfo
import androidx.core.net.toUri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.application
import androidx.lifecycle.viewModelScope
import com.alibaba.fastjson2.JSONArray
import com.alibaba.fastjson2.JSONObject
import com.miyuyan.sysuer.ClassIsland
import com.miyuyan.sysuer.R
import com.miyuyan.sysuer.api.DateTimeManager
import com.miyuyan.sysuer.api.ShortcutReader
import com.miyuyan.sysuer.api.SysuerJson
import com.miyuyan.sysuer.home.data.CollectionDatabase
import com.miyuyan.sysuer.home.data.DashboardShortcutEntity
import com.miyuyan.sysuer.home.data.ServiceCollectionEntity
import com.miyuyan.sysuer.model.JwxtModel
import com.miyuyan.sysuer.preference.SettingPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.time.Duration.Companion.milliseconds

class DashboardViewModel(application: Application) : AndroidViewModel(application) {
	private val model = JwxtModel(application)
	private val db by lazy { CollectionDatabase.getDatabase(application) }
	private val _term = MutableStateFlow("")
	val term: StateFlow<String> = _term.asStateFlow()
	private val _week = MutableStateFlow("")
	val week: StateFlow<String> = _week.asStateFlow()
	private val _totalWeek = MutableStateFlow(18)
	val totalWeek: StateFlow<Int> = _totalWeek.asStateFlow()
	private val _finalExamWeek = MutableStateFlow("")
	val finalExamWeek: StateFlow<String> = _finalExamWeek.asStateFlow()
	private val _todayExamIndex = MutableStateFlow(-1)
	val todayExamIndex: StateFlow<Int> = _todayExamIndex.asStateFlow()
	private val _selectedCourses = MutableStateFlow<List<JSONObject>>(emptyList())
	private val _todayCourses = MutableStateFlow<List<CourseInfo>>(emptyList())
	val todayCourses: StateFlow<List<CourseInfo>> = _todayCourses.asStateFlow()
	private val _recentCourses = MutableStateFlow<List<CourseInfo>>(emptyList())
	val tomorrowCourses: StateFlow<List<CourseInfo>> = _recentCourses.asStateFlow()
	private val _week18Exams = MutableStateFlow<List<JSONObject>>(emptyList())
	val week18Exams: StateFlow<List<JSONObject>> = _week18Exams.asStateFlow()
	private val _week19Exams = MutableStateFlow<List<JSONObject>>(emptyList())
	val week19Exams: StateFlow<List<JSONObject>> = _week19Exams.asStateFlow()
	private val _progressMax = MutableStateFlow(0)
	val progressMax: StateFlow<Int> = _progressMax.asStateFlow()
	private val _progressCurrent = MutableStateFlow(0)
	val progressCurrent: StateFlow<Int> = _progressCurrent.asStateFlow()
	private val _nextClassInfo = MutableStateFlow<Map<String, String>>(emptyMap())
	val nextClassInfo: StateFlow<Map<String, String>> = _nextClassInfo.asStateFlow()
	private val _isShowWeek18 = MutableStateFlow(true)
	val isShowWeek18: StateFlow<Boolean> = _isShowWeek18.asStateFlow()
	private var examSubject = ""
	private var statusJob: Job? = null
	private val statusTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
	private val _navigateToCourseDetail = MutableStateFlow<JSONObject?>(null)
	val navigateToCourseDetail: StateFlow<JSONObject?> = _navigateToCourseDetail.asStateFlow()
	fun onNavigatedToCourseDetail() {
		_navigateToCourseDetail.value = null
	}

	private val _dashboardShortcuts = MutableStateFlow<List<DashboardShortcutEntity>>(emptyList())
	val dashboardShortcuts: StateFlow<List<DashboardShortcutEntity>> =
		_dashboardShortcuts.asStateFlow()

	fun loadDashboardShortcuts() {
		viewModelScope.launch(Dispatchers.IO) {
			_dashboardShortcuts.value = db.collectionDao().getCollectedDashboardShortcuts()
		}
	}

	suspend fun isServiceCollected(id: Int): Boolean = db.collectionDao().isServiceCollected(id)

	suspend fun isDashboardShortcutCollected(id: Int): Boolean =
		db.collectionDao().isDashboardShortcutCollected(id)

	fun collectService(serviceId: Int, serviceJson: String, position: Int?) {
		viewModelScope.launch(Dispatchers.IO) {
			db.collectionDao().addService(
					ServiceCollectionEntity(
							serviceId = serviceId, serviceJson = serviceJson, position = position
					)
			)
		}
	}

	fun deleteService(serviceId: Int) {
		viewModelScope.launch(Dispatchers.IO) { db.collectionDao().deleteService(serviceId) }
	}

	fun addDashboardShortcut(shortcutId: Int, shortcutJson: String, position: Int?) {
		viewModelScope.launch(Dispatchers.IO) {
			db.collectionDao().addDashboardShortcut(
					DashboardShortcutEntity(
							shortcutId = shortcutId,
							shortcutJson = shortcutJson,
							position = position
					)
			)
		}
	}

	fun deleteDashboardShortcut(shortcutId: Int) {
		viewModelScope.launch(Dispatchers.IO) {
			db.collectionDao().deleteDashboardShortcut(shortcutId)
		}
	}

	private val _orderShortcuts = MutableStateFlow<List<DashboardShortcutEntity>>(emptyList())
	val orderShortcuts: StateFlow<List<DashboardShortcutEntity>> = _orderShortcuts.asStateFlow()

	fun loadOrderShortcuts() {
		viewModelScope.launch(Dispatchers.IO) {
			_orderShortcuts.value = db.collectionDao().getCollectedDashboardShortcuts()
		}
	}

	fun moveOrderShortcut(from: Int, to: Int) {
		if (from == to) return
		val list = _orderShortcuts.value.toMutableList()
		val item = list.removeAt(from)
		list.add(to, item)
		_orderShortcuts.value = list
	}

	fun saveOrderShortcuts() {
		viewModelScope.launch(Dispatchers.IO) {
			_orderShortcuts.value.forEachIndexed { index, entity ->
				db.collectionDao().updateDashboardShortcutPosition(entity.shortcutId ?: 0, index)
			}
			loadDashboardShortcuts()
		}
	}

	fun setShowWeek18(showWeek18: Boolean) {
		_isShowWeek18.value = showWeek18
	}

	fun openWechatScan() {
		val context = getApplication<Application>()
		try {
			val intent = Intent().apply {
				component = ComponentName("com.tencent.mm", "com.tencent.mm.ui.LauncherUI")
				putExtra("LauncherUI.From.Scaner.Shortcut", true)
				flags = Intent.FLAG_ACTIVITY_NEW_TASK
				action = Intent.ACTION_VIEW
			}
			if (intent.resolveActivity(context.packageManager) != null) {
				intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
				context.startActivity(intent)
			} else {
				model.toast(R.string.activity_not_found)
			}
		} catch (e: Exception) {
			e.printStackTrace()
			model.toast(R.string.activity_not_found)
		}
	}

	var sysuCardShortcutInfo: ShortcutInfo? = null
	fun openQrCode() {
		sysuCardShortcutInfo?.let {
			ShortcutReader.start(it)
			return
		}
		SettingPreference(application).qrCode.takeIf { it.isNotEmpty() }?.run {
			Intent(Intent.ACTION_VIEW, toUri()).takeIf {
				it.resolveActivity(application.packageManager) != null
			}?.let {
				it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
				application.startActivity(it)
			} ?: model.toast(R.string.fix_sysu_code_warning)
		} ?: model.toast(R.string.set_sysu_code_warning)
	}

	private fun updateNextClassMarkdown(beforeSize: Int, isAfterEmpty: Boolean) {
		val info = if (isAfterEmpty) {
			val next = _recentCourses.value.getOrNull(0)
			mapOf(
					"title" to application.getString(R.string.no_class_today),
					application.getString(R.string.next_class) to (next?.courseName
						?: application.getString(R.string.none)),
					application.getString(R.string.location) to (next?.location
						?: application.getString(R.string.none)),
					application.getString(R.string.time) to (next?.let { "${it.startTime}~${it.endTime}" }
						?: application.getString(R.string.none)))
		} else {
			val current = _todayCourses.value.getOrNull(beforeSize)
			mapOf(
					"title" to (current?.courseName ?: application.getString(R.string.none)),
					application.getString(R.string.location) to (current?.location
						?: application.getString(R.string.none)),
					application.getString(R.string.time) to (current?.let { "${it.startTime}~${it.endTime}" }
						?: application.getString(R.string.none)),
					application.getString(R.string.date) to (current?.date ?: application.getString(
							R.string.none
					)))
		}
		_nextClassInfo.value = info
	}

	/** 按当前时间刷新今天课程的 status,同时同步进度、下一节信息与 ClassIsland。 */
	private fun applyCourseStatus() {
		val todayList = _todayCourses.value.map { it.withStatus() }
		_todayCourses.value = todayList
		val (finished, upcoming) = todayList.partition { it.status == CourseState.BEFORE }
		_progressMax.value = todayList.size
		_progressCurrent.value = finished.size
		updateNextClassMarkdown(finished.size, upcoming.isEmpty())
		ClassIsland.updateCourseData(_todayCourses.value, _recentCourses.value)
		ClassIsland.triggerAndScheduleTick(application)
	}

	/** 只在每节课开始和下课的时刻唤醒一次来更新 status,其余时间不跑任何定时任务。 */
	private fun scheduleStatusUpdates() {
		statusJob?.cancel()
		statusJob = viewModelScope.launch {
			while (isActive) {
				val boundary = nextStatusBoundary() ?: break
				delay(
						Duration.between(LocalDateTime.now(), boundary).toMillis()
							.coerceAtLeast(1L).milliseconds
				)
				applyCourseStatus()
			}
		}
	}

	/** 今天所有课程里,晚于当前时间的最近一个开始/下课时刻。 */
	private fun nextStatusBoundary(): LocalDateTime? {
		val now = LocalDateTime.now()
		return _todayCourses.value.flatMap { course ->
			listOfNotNull(
					course.parseStatusBoundary(course.startTime),
					course.parseStatusBoundary(course.endTime)
			)
		}.filter { it.isAfter(now) }.minOrNull()
	}

	private fun CourseInfo.parseStatusBoundary(time: String): LocalDateTime? =
		runCatching { LocalDateTime.parse("$date $time", statusTimeFormatter) }.getOrNull()

	init {
		viewModelScope.launch {
			model.message.filter { it.second.getInteger("code") == 200 }
				.collect { (code, response) ->
					when (code) {
						1 -> {
							val courseList =
								response.getJSONArray("data").filterIsInstance<JSONObject>().map {
									SysuerJson.decodeFromString<CourseInfo>(it.toJSONString())
										.withStatus()
								}
							val todayList = courseList.filter { it.useflag == "TD" }
							val recentList = courseList.filter { it.useflag != "TD" }
							_todayCourses.value = todayList
							_recentCourses.value = recentList
							applyCourseStatus()
							scheduleStatusUpdates()
						}

						2 -> {
							val week18List = mutableListOf<JSONObject>()
							val week19List = mutableListOf<JSONObject>()
							response.getJSONArray("data")?.forEachIndexed { i, v ->
								val exams = if (i == 0) week18List else week19List
								val timetable = (v as JSONObject).getJSONObject("timetable")
								timetable.keys.sortedBy { it.toIntOrNull() ?: Int.MAX_VALUE }
									.forEach {
										(timetable[it] as JSONArray?)?.onEach { exam ->
											(exam as JSONObject)["status"] =
												getDatePosition(exam.getString("examDate"))
											exams.add(exam)
										}
									}
							}
							_week18Exams.value = week18List
							_week19Exams.value = week19List
							_todayExamIndex.value = (week18List + week19List)
								.indexOfFirst { it.getString("status") == "in" }.let {
									if (it < 0) (week18List + week19List).indexOfFirst { e ->
										e.getString(
												"status"
										) == "after"
									} else it
								}
							_isShowWeek18.value = _week.value != "19"
						}

						3 -> {
							_term.value =
								response.getJSONObject("data").getString("acadYearSemester")
						}

						4 -> {
							_week.value = response.getJSONObject("data").getString("nowWeekly")
							_totalWeek.value =
								response.getJSONObject("data").getJSONArray("weeklyList").size
						}

						5 -> {
							_finalExamWeek.value =
								response.getJSONArray("data").filterIsInstance<JSONObject>()
									.firstOrNull { it.getString("examWeekName") == "18-19周期末考" }
									?.getString("examWeekId") ?: ""
						}

						6 -> {
							val newCourses = response.getJSONObject("data").getJSONArray("rows")
								.filterIsInstance<JSONObject>()
							_selectedCourses.value += newCourses
							_selectedCourses.value
								.firstOrNull { it.getString("courseName") == examSubject }?.let {
									_navigateToCourseDetail.value = it
								}
						}
					}
				}
		}
	}

	fun getTerm() {
		model.enqueue("jwxt/base-info/acadyearterm/showNewAcadlist", 3)
	}

	fun getWeek(term: String?) {
		model.enqueue("jwxt/base-info/school-calender/weekly?academicYear=$term", 4)
	}

	fun getTodayCourses(term: String = "") {
		model.enqueue(
				"jwxt/timetable-search/classTableInfo/queryTodayStudentClassTable?academicYear=$term",
				1
		)
	}

	fun getExams(term: String, weekId: String?) {
		model.enqueue(
				"jwxt/examination-manage/classroomResource/queryStuEaxmInfo?code=jwxsd_ksxxck",
				"{\"acadYear\":\"$term\",\"examWeekId\":\"$weekId\",\"examWeekName\":\"18-19周期末考\",\"examDate\":\"\"}",
				2
		)
	}

	fun getExamWeekName(term: String) {
		model.enqueue(
				"jwxt/schedule/agg/commonScheduleExamTime/queryExamWeekName?yearTerm=$term", 5
		)
	}

	fun getSelectedCourses(courseName: String) {
		examSubject = courseName
		if (_selectedCourses.value.isEmpty()) model.enqueue(
				"jwxt/choose-course-front-server/electiveCourseResult/queryHistory",
				"{\"pageNo\":1,\"pageSize\":100,\"total\":true,\"param\":{\"yearTerm\":\"${_term.value}\",\"successStatus\":\"1\",\"failureStatus\":\"0\",\"retiredClass\":\"0\",\"waitingScreen\":\"0\"}}",
				6
		)
		else _selectedCourses.value.firstOrNull { it.getString("courseName") == examSubject }?.let {
			_navigateToCourseDetail.value = it
		}
	}

	fun CourseInfo.getTimePosition(): CourseState {
		val now = LocalDateTime.now()
		val start = parseStatusBoundary(startTime)
		val end = parseStatusBoundary(endTime)
		return when {
			now.isBefore(start) -> CourseState.AFTER
			now.isAfter(end) -> CourseState.BEFORE
			else -> CourseState.IN
		}
	}

	private fun CourseInfo.withStatus(): CourseInfo = copy(
			status = if (useflag == "TD") {
				getTimePosition()
			} else CourseState.TOMORROW
	)

	fun getDatePosition(date: String): String {
		val now = LocalDate.now()
		val target = DateTimeManager.toDate(date)
		return when {
			now.isBefore(target) -> "after"
			now.isAfter(target) -> "before"
			else -> "in"
		}
	}

	override fun onCleared() {
		statusJob?.cancel()
		model.dispose()
	}
}

enum class CourseState {
	/*今天未上*/AFTER, /*今天已上*/ BEFORE, /*今天在上*/ IN, /*明天要上*/ TOMORROW
}

/*{"schoolSemester":"2026-1","studentNum":"24308152","courseNum":"EIT317","classesNum":"202613608","courseName":"电磁场与电磁波","weekTimes":9,"week":1,"teachingDate":"2026-11-02","startClassTimes":3,"endClassTimes":4,"startTime":"10:10","endTime":"11:50","teacherName":"刘菊华","teachingPlace":"公共教学楼C栋/东C301","useflag":"RT"}*/
@Serializable
data class CourseInfo(
	val courseName: String,
	@SerialName("teacherName") val teacher: String,
	@SerialName("schoolSemester") val semester: String,
	@SerialName("teachingDate") val date: String,
	@SerialName("startTime") val startTime: String,
	@SerialName("endTime") val endTime: String,
	@SerialName("teachingPlace") val location: String,
	@SerialName("weekTimes") val week: Int,
	@SerialName("week") val day: Int,
	@SerialName("startClassTimes") val startSection: Int,
	@SerialName("endClassTimes") val endSection: Int,
	@SerialName("studentNum") val studentNum: String,
	@SerialName("courseNum") val courseCode: String,
	@SerialName("classesNum") val courseNum: String,
	@SerialName("useflag") val useflag: String,
	val status: CourseState = CourseState.AFTER
)