package com.miyuyan.sysuer.home

import android.app.Application
import android.content.ComponentName
import android.content.Intent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import androidx.core.net.toUri
import androidx.core.text.isDigitsOnly
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.application
import androidx.lifecycle.viewModelScope
import androidx.preference.PreferenceManager
import com.alibaba.fastjson2.JSONArray
import com.alibaba.fastjson2.JSONObject
import com.miyuyan.sysuer.ClassIsland
import com.miyuyan.sysuer.R
import com.miyuyan.sysuer.api.DateTimeManager
import com.miyuyan.sysuer.home.data.CollectionDatabase
import com.miyuyan.sysuer.home.data.DashboardShortcutEntity
import com.miyuyan.sysuer.home.data.ServiceCollectionEntity
import com.miyuyan.sysuer.model.JwxtModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

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
	private val _selectedCourses = MutableStateFlow<List<JSONObject>>(emptyList())
	private val _todayCourses = MutableStateFlow<List<JSONObject>>(emptyList())
	val todayCourses: StateFlow<List<JSONObject>> = _todayCourses.asStateFlow()
	private val _recentCourses = MutableStateFlow<List<JSONObject>>(emptyList())
	val tomorrowCourses: StateFlow<List<JSONObject>> = _recentCourses.asStateFlow()
	private val _week18Exams = MutableStateFlow<List<JSONObject>>(emptyList())
	val week18Exams: StateFlow<List<JSONObject>> = _week18Exams.asStateFlow()
	private val _week19Exams = MutableStateFlow<List<JSONObject>>(emptyList())
	val week19Exams: StateFlow<List<JSONObject>> = _week19Exams.asStateFlow()
	private val _progressMax = MutableStateFlow(0)
	val progressMax: StateFlow<Int> = _progressMax.asStateFlow()
	private val _progressCurrent = MutableStateFlow(0)
	val progressCurrent: StateFlow<Int> = _progressCurrent.asStateFlow()
	private val _nextClassMarkdown = MutableStateFlow("")
	val nextClassMarkdown: StateFlow<String> = _nextClassMarkdown.asStateFlow()
	private val _isShowWeek18 = MutableStateFlow(true)
	val isShowWeek18: StateFlow<Boolean> = _isShowWeek18.asStateFlow()
	private var examSubject = ""
	private val _navigateToCourseDetail = MutableStateFlow<JSONObject?>(null)
	val navigateToCourseDetail: StateFlow<JSONObject?> = _navigateToCourseDetail.asStateFlow()
	fun onNavigatedToCourseDetail() {
		_navigateToCourseDetail.value = null
	}

	private val _dashboardShortcuts = MutableStateFlow<List<DashboardShortcutEntity>>(emptyList())
	val dashboardShortcuts: StateFlow<List<DashboardShortcutEntity>> = _dashboardShortcuts.asStateFlow()

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
					shortcutId = shortcutId, shortcutJson = shortcutJson, position = position
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

	val date: String? =
		LocalDate.now().format(DateTimeFormatter.ofPattern("M月dd日", Locale.getDefault()))
	val weekDay: String? =
		getApplication<Application>().resources.getStringArray(R.array.weeks)[LocalDate.now().dayOfWeek.value - 1]

	val dateText: String
		get() {
			val md = StringBuilder()
			if (_week.value.isNotEmpty()) {
				if (_week.value.isDigitsOnly()) md.append("###### 第${_week.value}周\n")
				else md.append("###### ${_week.value}\n")
			}
			if (_term.value.isNotEmpty()) md.append("###### 第${_term.value}学期\n")
			if (date != null) md.append(date).append("\n\n")
			if (weekDay != null) md.append(weekDay)
			return "$md"
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
				model.contextUtil.toast(R.string.activity_not_found)
			}
		} catch (e: Exception) {
			e.printStackTrace()
			model.contextUtil.toast(R.string.activity_not_found)
		}
	}

	fun openQrCode() {
		PreferenceManager.getDefaultSharedPreferences(application).getString("qrcode", "")
			?.takeIf { it.isNotEmpty() }?.run {
				Intent(Intent.ACTION_VIEW, toUri()).takeIf {
					it.resolveActivity(application.packageManager) != null
				}?.let {
					it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
					application.startActivity(it)
				} ?: model.contextUtil.toast(R.string.fix_sysu_code_warning)
			} ?: model.contextUtil.toast(R.string.set_sysu_code_warning)
	}

	private fun updateNextClassMarkdown(beforeSize: Int, isAfterEmpty: Boolean) {
		val markdown = if (isAfterEmpty) {
		val next = _recentCourses.value.getOrNull(0)
			"###### ${application.getString(R.string.no_class_today)}\n\n${application.getString(R.string.next_class)}：**${
				next?.getString("courseName") ?: application.getString(R.string.none)
			}**\n\n${application.getString(R.string.location)}：**${
				next?.getString("teachingPlace") ?: application.getString(R.string.none)
			}**\n\n${application.getString(R.string.time)}：**${
				next?.getString("time") ?: application.getString(
					R.string.none
				)
			}**"
		} else {
		val current = _todayCourses.value.getOrNull(beforeSize)
			"###### ${current?.getString("courseName") ?: application.getString(R.string.none)}\n\n${
				application.getString(
					R.string.location
				)
			}：**${
				current?.getString("teachingPlace") ?: application.getString(R.string.none)
			}**\n\n${application.getString(R.string.time)}：**${
				current?.getString("time") ?: application.getString(
					R.string.none
				)
			}**\n\n${
				application.getString(R.string.date)
			}：**${current?.getString("teachingDate") ?: application.getString(R.string.none)}**"
		}
		_nextClassMarkdown.value = markdown
	}

	private fun scheduleIslandTick() {
		ClassIsland.updateCourseData(_todayCourses.value, _recentCourses.value)
		ClassIsland.triggerAndScheduleTick(application)
	}

	init {
		viewModelScope.launch {
			model.messageChannel.filter { it.second.getInteger("code") == 200 }
				.collect { (code, response) ->
					when (code) {
					1 -> {
						val todayList = mutableListOf<JSONObject>()
						val recentList = mutableListOf<JSONObject>()
						val (beforeArray, afterArray) = response.getJSONArray("data")
							.map { it as JSONObject }.filter { item ->
								item["status"] = getTimePosition(
									"${item.getString("teachingDate")} ${
										item.getString("startTime")
									}",
									"${item.getString("teachingDate")} ${item.getString("endTime")}"
								)
								item["time"] =
									"${item.getString("startTime")}~${item.getString("endTime")}"
								item["course"] =
									"第${item.getString("startClassTimes")}~${item.getString("endClassTimes")}节课"
								val isToday = "TD" == item.getString("useflag")
								if (isToday) todayList.add(item) else recentList.add(
									item
								)
								isToday
							}.partition { it.getString("status") == "before" }
						_todayCourses.value = todayList
						_recentCourses.value = recentList
						_progressMax.value = todayList.size
						_progressCurrent.value = beforeArray.size
						updateNextClassMarkdown(beforeArray.size, afterArray.isEmpty())
						scheduleIslandTick()
					}

					2 -> {
						val week18List = mutableListOf<JSONObject>()
						val week19List = mutableListOf<JSONObject>()
						response.getJSONArray("data")?.forEachIndexed { i, v ->
							val exams = if (i == 0) week18List else week19List
							val timetable = (v as JSONObject).getJSONObject("timetable")
							timetable.keys.sortedBy { it.toIntOrNull() ?: Int.MAX_VALUE }
								.forEach {
									(timetable[it] as JSONArray?)?.apply {
										forEach { exam ->
											(exam as JSONObject)["status"] =
												getDatePosition(exam.getString("examDate"))
											exams.add(exam)
										}
									}
								}
						}
						_week18Exams.value = week18List
						_week19Exams.value = week19List
						_todayExamIndex.value =
							(week18List + week19List).indexOfFirst { it.getString("status") == "in" }.let {
								if (it < 0) (week18List + week19List).indexOfFirst { e -> e.getString("status") == "after" } else it
							}
						_isShowWeek18.value = _week.value != "19"
					}

						3 -> {
							_term.value =
								response.getJSONObject("data").getString("acadYearSemester")
						}

						4 -> {
							_week.value = response.getJSONArray("data").getJSONObject(0)
								.getString("weekTimes")
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
						_selectedCourses.value = _selectedCourses.value + newCourses
						_selectedCourses.value.firstOrNull { it.getString("courseName") == examSubject }
							?.let {
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

	fun getTodayCourses(term: String = "") {
		model.addAndNext(
			"jwxt/timetable-search/classTableInfo/queryTodayStudentClassTable?academicYear=$term", 1
		)
	}

	fun getExams(term: String, weekId: String?) {
		model.addAndNext(
			"jwxt/examination-manage/classroomResource/queryStuEaxmInfo?code=jwxsd_ksxxck",
			"{\"acadYear\":\"$term\",\"examWeekId\":\"$weekId\",\"examWeekName\":\"18-19周期末考\",\"examDate\":\"\"}",
			2
		)
	}

	fun getExamWeekName(term: String) {
		model.addAndNext(
			"jwxt/schedule/agg/commonScheduleExamTime/queryExamWeekName?yearTerm=$term", 5
		)
	}

	fun getSelectedCourses(courseName: String) {
		examSubject = courseName
		if (_selectedCourses.value.isEmpty()) model.addAndNext(
			"jwxt/choose-course-front-server/electiveCourseResult/queryHistory",
			"{\"pageNo\":1,\"pageSize\":100,\"total\":true,\"param\":{\"yearTerm\":\"${_term.value}\",\"successStatus\":\"1\",\"failureStatus\":\"0\",\"retiredClass\":\"0\",\"waitingScreen\":\"0\"}}",
			6
		)
		else _selectedCourses.value.firstOrNull { it.getString("courseName") == examSubject }?.let {
			_navigateToCourseDetail.value = it
		}
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