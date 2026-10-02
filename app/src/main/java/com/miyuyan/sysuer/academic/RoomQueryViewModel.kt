package com.miyuyan.sysuer.academic

import android.app.Application
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.alibaba.fastjson2.JSONArray
import com.alibaba.fastjson2.JSONObject
import com.miyuyan.sysuer.api.CommonUtil.extractValue
import com.miyuyan.sysuer.model.JwxtModel
import com.miyuyan.sysuer.view.RowData
import com.miyuyan.sysuer.view.SectionData
import com.miyuyan.sysuer.view.UiState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RoomQueryViewModel(application: Application) : AndroidViewModel(application) {
	private val model: JwxtModel = JwxtModel(application)

	/** 校区选项 */
	private val _campusNames = MutableStateFlow<List<String>>(emptyList())
	val campusNames = _campusNames.asStateFlow()
	private val _campusValues = MutableStateFlow<List<String>>(emptyList())
	val campusValues = _campusValues.asStateFlow()

	/** 教学楼选项 */
	private val _buildingNames = MutableStateFlow<List<String>>(emptyList())
	val buildingNames = _buildingNames.asStateFlow()
	private val _buildingValues = MutableStateFlow<List<String>>(emptyList())
	val buildingValues = _buildingValues.asStateFlow()

	/** 学年学期选项 */
	private val _yearTerms = MutableStateFlow<List<String>>(emptyList())
	val yearTerms = _yearTerms.asStateFlow()

	/** 教室选项 */
	private val _classroomNames = MutableStateFlow<List<String>>(emptyList())
	val classroomNames = _classroomNames.asStateFlow()
	private val _classroomValues = MutableStateFlow<List<String>>(emptyList())
	val classroomValues = _classroomValues.asStateFlow()

	// 查询页筛选条件
	var campusValue: String? = null
	var buildingValue: String? = null
	var classroomValue: String? = null
	var classroomName: String = ""
	var checkType: String? = null
	var occupySource: String? = null
	var occupyReason: String? = null
	var classBegin: Float = 0f
	var classEnd: Float = 0f
	var isWeek: Boolean = true
	var yearTerm: String? = null
	var weekBegin: Float = 0f
	var weekEnd: Float = 0f
	var weekTime: String? = null
	var weekday: List<String> = emptyList()
	var dateA: String? = null
	var dateB: String? = null

	/** 查询结果 */
	val sections = mutableStateListOf<SectionData>()

	private val _uiState = model.getUiState(RESULT_REQUEST)
	val uiState: StateFlow<UiState> = _uiState.asStateFlow()

	private var page = 0
	private var total = -1

	private val resultNames = arrayOf(
			"学年学期",
			"日期",
			"周次",
			"星期",
			"校区",
			"教学楼",
			"教学楼编号",
			"教室编号",
			"楼层",
			"教室ID",
			"座位数"
	)
	private val resultKeys = arrayOf(
			"yearTerm",
			"date",
			"week",
			"dayWeek",
			"campus",
			"teachingBuild",
			"teachingBuildNum",
			"classroomNum",
			"floor",
			"classroomID",
			"seatCount"
	)
	private val sectionNames = arrayOf(
			"第一节",
			"第二节",
			"第三节",
			"第四节",
			"第五节",
			"第六节",
			"第七节",
			"第八节",
			"第九节",
			"第十节",
			"第十一节"
	)
	private val sectionKeys = arrayOf(
			"oneSection",
			"twoSection",
			"threeSection",
			"fourSection",
			"fiveSection",
			"sixSection",
			"sevenSection",
			"eightSection",
			"nineSection",
			"tenSection",
			"elevenSection"
	)

	private val _snackbarMessage = MutableSharedFlow<String>(extraBufferCapacity = 16)
	val snackbarMessage: SharedFlow<String> = _snackbarMessage.asSharedFlow()

	init {
		viewModelScope.launch {
			model.message.collect { (code, response) ->
				if (response.getInteger("code") != 200) {
					if (code == RESULT_REQUEST) _uiState.value = UiState.Error
					return@collect
				}
				val data = response.getJSONArray("data") ?: return@collect
				when (code) {
					CAMPUS_REQUEST -> {
						val (names, values) = extractValue(data, "campusName", "id")
						_campusNames.value = names
						_campusValues.value = values
					}

					BUILDING_REQUEST, BUILDING_BY_CAMPUS_REQUEST -> {
						val (names, values) = extractValue(data, "name", "id")
						_buildingNames.value = names
						_buildingValues.value = values
					}

					YEAR_TERM_REQUEST -> {
						_yearTerms.value = extractValue(data, "acadYearSemester").filterNotNull()
					}

					CLASSROOM_REQUEST, CLASSROOM_BY_CONDITION_REQUEST -> {
						val (names, values) = extractValue(data, "number", "id")
						_classroomNames.value = names
						_classroomValues.value = values
					}

					RESULT_REQUEST -> {
						val body = response.getJSONObject("data") ?: return@collect
						if (page == 1) {
							total = body.getIntValue("total", -1)
							sections.clear()
						}
						body.getJSONArray("data").filterIsInstance<JSONObject>().forEach { item ->
							val rows = extractValue(item, resultNames, resultKeys)
							sectionKeys.forEachIndexed { index, key ->
								item.getJSONObject(key)?.let {
									rows.add(
											RowData(
													sectionNames[index], it.getString(
													"occupyReason", ""
											) + "-" + it.getString(
													"occupyUseDepartment", ""
											)
											)
									)
								}
							}
							sections.add(
									SectionData(
											title = "${item.getString("classroomNum")}/${
												item.getString(
														"date"
												)
											}", rows = rows
									)
							)
						}
						_uiState.value =
							if (page == 1 && sections.isEmpty()) UiState.Empty else UiState.Content
					}
				}
			}
		}
	}

	fun fetchCampuses() {
		model.enqueue("jwxt/base-info/campus/findCampusNamesBox", CAMPUS_REQUEST)
	}

	fun fetchBuildings() {
		model.enqueue("jwxt/base-info/teaching-building/pull", BUILDING_REQUEST)
	}

	/** 按校区联动教学楼 */
	fun fetchBuildingsByCampus(campus: String?) {
		model.enqueue(
				"jwxt/base-info/teaching-building/pull?campusId=${campus ?: ""}",
				BUILDING_BY_CAMPUS_REQUEST
		)
	}

	fun fetchYearTerms() {
		model.enqueue("jwxt/base-info/acadyearterm/findAcadyeartermNamesBox", YEAR_TERM_REQUEST)
	}

	/*fun fetchClassrooms() {
		model.enqueue("jwxt/base-info/classroom/queryclassroombymulticondition", CLASSROOM_REQUEST)
	}*/

	/** 按校区、教学楼、关键字联动教室 */
	fun fetchClassrooms(query :String? = null /*campus: String?, building: String?, classroomCode: String?*/) {
		model.enqueue(
				"jwxt/base-info/classroom/queryclassroombymulticondition?campusId=${campusValue ?: ""}&buildingId=${buildingValue ?: ""}&classroomCode=${query ?: ""}",
				CLASSROOM_BY_CONDITION_REQUEST
		)
	}

	/** 按筛选条件从第一页开始查询 */
	fun query() {
		page = 0
		total = -1
		sections.clear()
		fetchResult()
	}

	/** 加载下一页，滚动到底部时触发 */
	fun fetchResult() {
		if (_uiState.value == UiState.Loading || _uiState.value == UiState.LoadMore) return
		if (total >= 0 && sections.size >= total) return
		_uiState.value = if (page == 0) UiState.Loading else UiState.LoadMore
		model.enqueue(
				"jwxt/schedule/agg/classroomOccupy/pageCheckList",
				"{\"pageNo\":${++page},\"pageSize\":10,\"total\":true,\"param\":${buildParams()}}",
				RESULT_REQUEST
		)
	}

	private fun buildParams(): JSONObject {
		val params = JSONObject()
		if (campusValue?.isNotBlank() == true) params["campusId"] = campusValue
		if (buildingValue?.isNotBlank() == true) params["teachingBuildID"] = buildingValue
		if (classroomValue?.isNotBlank() == true) params["classroomID"] = classroomValue
		if (buildingValue?.isNotBlank() != true || classroomValue?.isNotBlank() != true) {
			_snackbarMessage.tryEmit(
					"请选择教学楼或教室"
			)
		}
		if (classBegin > 0f) params["sectionA"] = classBegin.toInt().toString()
		if (classEnd > 0f) params["sectionB"] = classEnd.toInt().toString()
		if (checkType?.isNotBlank() == true) params["checkType"] = checkType
		if (occupySource?.isNotBlank() == true) params["occupySource"] = occupySource
		if (occupyReason?.isNotBlank() == true) params["occupyReason"] = occupyReason
		if (isWeek) {
			params["weekOrTime"] = "week"
			if (yearTerm?.isNotBlank() == true) params["yearTerm"] = yearTerm
			if (weekBegin > 0f) params["weekA"] = weekBegin.toInt().toString()
			if (weekEnd > 0f) params["weekB"] = weekEnd.toInt().toString()
			if (weekTime?.isNotBlank() == true) params["singleOrDoubleWeek"] = weekTime
			params["dayWeeks"] = JSONArray(weekday)
			if (yearTerm?.isNotBlank() != true || weekBegin <= 0f || weekEnd <= 0f) _snackbarMessage.tryEmit(
					"请选择学期和周数范围"
			)
		} else {
			params["weekOrTime"] = "time"
			if (dateA?.isNotBlank() == true) params["dateA"] = dateA
			if (dateB?.isNotBlank() == true) params["dateB"] = dateB
			if (dateA?.isNotBlank() != true || dateB?.isNotBlank() != true) _snackbarMessage.tryEmit(
					"请选择日期范围"
			)
		}
		return params
	}

	override fun onCleared() {
		model.dispose()
	}

	companion object {
		private const val CAMPUS_REQUEST = 0
		private const val BUILDING_REQUEST = 1
		private const val YEAR_TERM_REQUEST = 2
		private const val CLASSROOM_REQUEST = 3
		private const val BUILDING_BY_CAMPUS_REQUEST = 4
		private const val CLASSROOM_BY_CONDITION_REQUEST = 5
		private const val RESULT_REQUEST = 6
	}
}