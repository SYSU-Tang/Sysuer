package com.sysu.edu.home

import android.app.Application
import android.content.ComponentName
import android.content.Intent
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.core.net.toUri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.preference.PreferenceManager
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.alibaba.fastjson2.JSONArray
import com.alibaba.fastjson2.JSONObject
import com.sysu.edu.ClassNotificationWorker
import com.sysu.edu.R
import com.sysu.edu.api.DateTimeManager
import com.sysu.edu.home.data.CollectionDatabase
import com.sysu.edu.home.data.DashboardShortcutEntity
import com.sysu.edu.model.JwxtModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.concurrent.TimeUnit

class DashboardViewModel(application: Application) : AndroidViewModel(application) {
	private val model = JwxtModel(application)
	private val db by lazy { CollectionDatabase.getDatabase(application) }
	private val _term = MutableStateFlow("")
	val term: StateFlow<String> = _term.asStateFlow()
	private val _week = MutableStateFlow("")
	val week: StateFlow<String> = _week.asStateFlow()
	private val _finalExamWeek = MutableStateFlow("")
	val finalExamWeek: StateFlow<String> = _finalExamWeek.asStateFlow()
	private val _todayExamIndex = MutableStateFlow(-1)
	val todayExamIndex: StateFlow<Int> = _todayExamIndex.asStateFlow()
	private val _selectedCourses = mutableStateListOf<JSONObject>()
	val selectedCourses: SnapshotStateList<JSONObject> = _selectedCourses
	private val _todayCourses = mutableStateListOf<JSONObject>()
	val todayCourses: SnapshotStateList<JSONObject> = _todayCourses
	private val _tomorrowCourses = mutableStateListOf<JSONObject>()
	val tomorrowCourses: SnapshotStateList<JSONObject> = _tomorrowCourses
	private val _week18Exams = mutableStateListOf<JSONObject>()
	val week18Exams: SnapshotStateList<JSONObject> = _week18Exams
	private val _week19Exams = mutableStateListOf<JSONObject>()
	val week19Exams: SnapshotStateList<JSONObject> = _week19Exams
	private val _progressMax = MutableStateFlow(0)
	val progressMax: StateFlow<Int> = _progressMax.asStateFlow()
	private val _progressCurrent = MutableStateFlow(0)
	val progressCurrent: StateFlow<Int> = _progressCurrent.asStateFlow()
	private val _nextClassMarkdown = MutableStateFlow("")
	val nextClassMarkdown: StateFlow<String> = _nextClassMarkdown.asStateFlow()
	private val _isShowToday = MutableStateFlow(true)
	val isShowToday: StateFlow<Boolean> = _isShowToday.asStateFlow()
	private val _isShowWeek18 = MutableStateFlow(true)
	val isShowWeek18: StateFlow<Boolean> = _isShowWeek18.asStateFlow()
	private var examSubject = ""
	private val _navigateToCourseDetail = MutableStateFlow<JSONObject?>(null)
	val navigateToCourseDetail: StateFlow<JSONObject?> = _navigateToCourseDetail.asStateFlow()
	fun onNavigatedToCourseDetail() {
		_navigateToCourseDetail.value = null
	}

	private val _dashboardShortcuts = mutableStateListOf<DashboardShortcutEntity>()
	val dashboardShortcuts: SnapshotStateList<DashboardShortcutEntity> = _dashboardShortcuts

	fun loadDashboardShortcuts() {
		viewModelScope.launch(Dispatchers.IO) {
			val shortcuts = db.collectionDao().getCollectedDashboardShortcuts()
			_dashboardShortcuts.clear()
			_dashboardShortcuts.addAll(shortcuts)
		}
	}

	suspend fun isServiceCollected(id: Int): Boolean = db.collectionDao().isServiceCollected(id)

	suspend fun isDashboardShortcutCollected(id: Int): Boolean = db.collectionDao().isDashboardShortcutCollected(id)

	fun addService(serviceId: Int, serviceJson: String, position: Int?) {
		viewModelScope.launch(Dispatchers.IO) {
			db.collectionDao().addService(com.sysu.edu.home.data.ServiceCollectionEntity(serviceId = serviceId, serviceJson = serviceJson, position = position))
		}
	}

	fun deleteService(serviceId: Int) {
		viewModelScope.launch(Dispatchers.IO) { db.collectionDao().deleteService(serviceId) }
	}

	fun addDashboardShortcut(shortcutId: Int, shortcutJson: String, position: Int?) {
		viewModelScope.launch(Dispatchers.IO) {
			db.collectionDao().addDashboardShortcut(DashboardShortcutEntity(shortcutId = shortcutId, shortcutJson = shortcutJson, position = position))
		}
	}

	fun deleteDashboardShortcut(shortcutId: Int) {
		viewModelScope.launch(Dispatchers.IO) { db.collectionDao().deleteDashboardShortcut(shortcutId) }
	}

	private val _orderShortcuts = mutableStateListOf<DashboardShortcutEntity>()
	val orderShortcuts: SnapshotStateList<DashboardShortcutEntity> = _orderShortcuts

	fun loadOrderShortcuts() {
		viewModelScope.launch(Dispatchers.IO) {
			val shortcuts = db.collectionDao().getCollectedDashboardShortcuts()
			_orderShortcuts.clear()
			_orderShortcuts.addAll(shortcuts)
		}
	}

	fun moveOrderShortcut(from: Int, to: Int) {
		if (from == to) return
		val item = _orderShortcuts.removeAt(from)
		_orderShortcuts.add(to, item)
	}

	fun saveOrderShortcuts() {
		viewModelScope.launch(Dispatchers.IO) {
			_orderShortcuts.forEachIndexed { index, entity ->
				db.collectionDao().updateDashboardShortcutPosition(entity.shortcutId ?: 0, index)
			}
			loadDashboardShortcuts()
		}
	}
	
	val dateText: String
		get() {
			val date = LocalDate.now().format(DateTimeFormatter.ofPattern("M月dd日", Locale.getDefault()))
			val weeks = getApplication<Application>().resources.getStringArray(R.array.weeks)
			val weekDay = weeks[LocalDate.now().dayOfWeek.value - 1]
			return if (_term.value.isNotEmpty() && _week.value.isNotEmpty()) "第${_week.value}周\n第${_term.value}学期\n$date\n$weekDay" else if (_term.value.isNotEmpty()) "第${_term.value}学期\n$date\n$weekDay" else "$date/$weekDay"
		}
//	val currentCourses: SnapshotStateList<JSONObject>
//		get() = if (_isShowToday.value) _todayCourses else _tomorrowCourses
//	val currentExams: SnapshotStateList<JSONObject>
//		get() = if (_isShowWeek18.value) _week18Exams else _week19Exams
	
	fun setShowToday(showToday: Boolean) {
		_isShowToday.value = showToday
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
			}
			else {
				model.contextUtil.toast(R.string.activity_not_found)
			}
		} catch (e: Exception) {
			e.printStackTrace()
			model.contextUtil.toast(R.string.activity_not_found)
		}
	}
	
	fun openQrCode() {
		val context = getApplication<Application>()
		PreferenceManager.getDefaultSharedPreferences(context).getString("qrcode", "")?.takeIf { it.isNotEmpty() }?.run {
				Intent(Intent.ACTION_VIEW, toUri()).takeIf {
					it.resolveActivity(context.packageManager) != null
				}?.let {
					it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
					context.startActivity(it)
				} ?: model.contextUtil.toast(R.string.fix_sysu_code_warning)
			} ?: model.contextUtil.toast(R.string.set_sysu_code_warning)
	}
	
	private fun updateNextClassMarkdown(beforeSize: Int, isAfterEmpty: Boolean) {
		val context = getApplication<Application>()
		val markdown = if (isAfterEmpty) {
			val next = _tomorrowCourses.getOrNull(0)
			"### ${context.getString(R.string.noClass)}\n\n${context.getString(R.string.next_class)}：**${
				next?.getString("courseName") ?: context.getString(R.string.none)
			}**\n\n${context.getString(R.string.location)}：**${
				next?.getString("teachingPlace") ?: context.getString(R.string.none)
			}**\n\n${context.getString(R.string.time)}：**${next?.getString("time") ?: context.getString(R.string.none)}**"
		}
		else {
			val current = _todayCourses.getOrNull(beforeSize)
			"### ${current?.getString("courseName") ?: context.getString(R.string.none)}\n\n${context.getString(R.string.location)}：**${
				current?.getString("teachingPlace") ?: context.getString(R.string.none)
			}**\n\n${context.getString(R.string.time)}：**${current?.getString("time") ?: context.getString(R.string.none)}**\n\n${
				context.getString(R.string.date)
			}：**${current?.getString("teachingDate") ?: context.getString(R.string.none)}**"
		}
		_nextClassMarkdown.value = markdown
	}
	
	private fun scheduleNotification(beforeSize: Int, isAfterEmpty: Boolean) {
		val course = if (isAfterEmpty) _tomorrowCourses.getOrNull(0) else _todayCourses.getOrNull(beforeSize)
		course?.run {
			val startTimeStr = "${getString("teachingDate")} ${getString("startTime")}"
			val delta = LocalDateTime.parse(startTimeStr, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() - System.currentTimeMillis()
			if (delta > 0) {
				val delay = if (delta < 15 * 60 * 1000) 0L else delta - 15 * 60 * 1000
				val workRequest = OneTimeWorkRequest.Builder(ClassNotificationWorker::class.java)
					.setInputData(workDataOf("courseName" to getString("courseName"), "teachingPlace" to getString("teachingPlace"), "time" to getString("time")))
					.setInitialDelay(delay, TimeUnit.MILLISECONDS)
					.build()
				WorkManager.getInstance(getApplication()).enqueueUniqueWork("next_class_notification_update", ExistingWorkPolicy.KEEP, workRequest)
			}
		}
	}
	
	init {
		model.message.observeForever { (code, response) ->
			if (response.getInteger("code") == 200) {
				when (code) {
					1 -> {
						_todayCourses.clear()
						_tomorrowCourses.clear()
						val (beforeArray, afterArray) = response.getJSONArray("data").map { it as JSONObject }.filter { item ->
							item["status"] = getTimePosition("${item.getString("teachingDate")} ${item.getString("startTime")}", "${item.getString("teachingDate")} ${item.getString("endTime")}")
							item["time"] = "${item.getString("startTime")}~${item.getString("endTime")}"
							item["course"] = "第${item.getString("startClassTimes")}~${item.getString("endClassTimes")}节课"
							val isToday = "TD" == item.getString("useflag")
							if (isToday) _todayCourses.add(item) else _tomorrowCourses.add(item)
							isToday
						}.partition { it.getString("status") == "before" }
						_progressMax.value = _todayCourses.size
						_progressCurrent.value = beforeArray.size
						updateNextClassMarkdown(beforeArray.size, afterArray.isEmpty())
						scheduleNotification(beforeArray.size, afterArray.isEmpty())
						_isShowToday.value = true
					}
					2 -> {
						_week18Exams.clear()
						_week19Exams.clear()
						response.getJSONArray("data")?.forEachIndexed { i, v ->
							val exams = if (i == 0) _week18Exams else _week19Exams
							val timetable = (v as JSONObject).getJSONObject("timetable")
							timetable.keys.sortedBy { it.toIntOrNull() ?: Int.MAX_VALUE }.forEach {
								(timetable[it] as JSONArray?)?.apply {
									forEach { exam ->
										(exam as JSONObject)["status"] = getDatePosition(exam.getString("examDate"))
										exams.add(exam)
									}
								}
							}
							_todayExamIndex.value = exams.indexOfFirst { it.getString("status") == "in" }.let {
								if (it < 0) exams.indexOfFirst { e -> e.getString("status") == "after" } else it
							}
						}
						_isShowWeek18.value = _week.value != "19"
					}
					3 -> {
						_term.value = response.getJSONObject("data").getString("acadYearSemester")
					}
					4 -> {
						_week.value = response.getJSONArray("data").getJSONObject(0).getString("weekTimes")
					}
					5 -> {
						_finalExamWeek.value = response.getJSONArray("data").filterIsInstance<JSONObject>().firstOrNull { it.getString("examWeekName") == "18-19周期末考" }?.getString("examWeekId") ?: ""
					}
					6 -> {
						_selectedCourses.addAll(response.getJSONObject("data").getJSONArray("rows").filterIsInstance<JSONObject>())
						_selectedCourses.firstOrNull { it.getString("courseName") == examSubject }?.let {
							_navigateToCourseDetail.value = it
						}
					}
				}
			}
		}
	}
	
	fun getTerm() {
		model.addAndNext("jwxt/base-info/acadyearterm/showNewAcadlist", 3)
	}
	
	fun getWeek(term: String?) {
		model.addAndNext("jwxt/timetable-search/classTableInfo/getDateWeekly?academicYear=$term", 4)
	}
	
	fun getTodayCourses() {
		model.addAndNext("jwxt/timetable-search/classTableInfo/queryTodayStudentClassTable?academicYear=", 1)
	}
	
	fun getExams(term: String, weekId: String?) {
		model.addAndNext("jwxt/examination-manage/classroomResource/queryStuEaxmInfo?code=jwxsd_ksxxck", "{\"acadYear\":\"$term\",\"examWeekId\":\"$weekId\",\"examWeekName\":\"18-19周期末考\",\"examDate\":\"\"}", 2)
	}
	
	fun getExamWeekName(term: String) {
		model.addAndNext("jwxt/schedule/agg/commonScheduleExamTime/queryExamWeekName?yearTerm=$term", 5)
	}
	
	fun getSelectedCourses(courseName: String?) {
		examSubject = courseName ?: ""
		_selectedCourses.clear()
		model.addAndNext("jwxt/choose-course-front-server/selectedCourse/list",
		                 "{\"pageNo\":1,\"pageSize\":100,\"total\":true,\"param\":{\"courseName\":\"$courseName\",\"successStatus\":\"1\",\"failureStatus\":\"0\",\"retiredClass\":\"0\",\"waitingScreen\":\"0\"}}",
		                 6)
	}
	
	fun getTimePosition(from: String?, to: String?): String {
		val now = LocalDateTime.now()
		val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
		val start = LocalDateTime.parse(from, formatter)
		val end = LocalDateTime.parse(to, formatter)
		return when {
			now.isBefore(start) -> "after"
			now.isAfter(end) -> "before"
			else -> "in"
		}
	}
	
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
		model.dispose()
	}
}