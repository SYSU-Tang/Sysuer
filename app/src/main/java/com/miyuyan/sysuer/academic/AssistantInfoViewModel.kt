package com.miyuyan.sysuer.academic

import android.app.Application
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.alibaba.fastjson2.JSONObject
import com.miyuyan.sysuer.api.CommonUtil.extractValue
import com.miyuyan.sysuer.model.JwxtModel
import com.miyuyan.sysuer.view.SectionData
import com.miyuyan.sysuer.view.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AssistantInfoViewModel(application: Application) : AndroidViewModel(application) {
	private val model: JwxtModel = JwxtModel(application)

	/** 学年学期选项 */
	private val _yearTerms = MutableStateFlow<List<String>>(emptyList())
	val yearTerms = _yearTerms.asStateFlow()

	/** 校区选项 */
	private val _campusNames = MutableStateFlow<List<String>>(emptyList())
	val campusNames = _campusNames.asStateFlow()
	private val _campusValues = MutableStateFlow<List<String>>(emptyList())
	val campusValues = _campusValues.asStateFlow()

	// 查询页筛选条件
	var yearTerm: String? = null
	var campusValue: String? = null
	var courseNumber: String? = null
	var courseName: String? = null
	var teacher: String? = null

	/** 查询结果 */
	val sections = mutableStateListOf<SectionData>()

	private val _uiState = model.getUiState(RESULT_REQUEST)
	val uiState: StateFlow<UiState> = _uiState.asStateFlow()

	private var page = 0
	private var total = 0

	private val resultNames = arrayOf(
			"学年学期",
			"校区",
			"开设单位",
			"课程名称",
			"课程编号",
			"课程学时",
			"班级编号",
			"实选人数",
			"任课教师",
			"上课时间地点",
			"修读对象",
			"上课学生名单",
			"助教信息",
			"助教职责"
	)
	private val resultKeys = arrayOf(
			"semester",
			"studyCampus",
			"openUnitName",
			"courseName",
			"courseNum",
			"courseHour",
			"classNumber",
			"apersonNum",
			"teacherName",
			"teachingTimePlace",
			"studyObj",
			"stuList",
			"assistantInfo",
			"jobDuty"
	)

	init {
		viewModelScope.launch {
			model.message.collect { (code, response) ->
				if (response.getInteger("code") != 200) {
					if (code == RESULT_REQUEST) _uiState.value = UiState.Error
					return@collect
				}
				when (code) {
					YEAR_TERM_REQUEST -> {
						_yearTerms.value = extractValue(
								response.getJSONArray("data"), "acadYearSemester"
						).filterNotNull()
					}

					CAMPUS_REQUEST -> {
						val (names, values) = extractValue(
								response.getJSONArray("data"), "campusName", "id"
						)
						_campusNames.value = names
						_campusValues.value = values
					}

					RESULT_REQUEST -> {
						val data = response.getJSONObject("data") ?: return@collect
						if (page == 1) {
							total = data.getIntValue("total", 0)
							sections.clear()
						}
						data.getJSONArray("rows").filterIsInstance<JSONObject>().forEach { item ->
							sections.add(
									SectionData(
											title = item.getString("courseName"),
											rows = extractValue(item, resultNames, resultKeys)
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

	fun fetchYearTerms() {
		model.enqueue("jwxt/base-info/acadyearterm/findAcadyeartermNamesBox", YEAR_TERM_REQUEST)
	}

	fun fetchCampuses() {
		model.enqueue("jwxt/base-info/campus/findCampusNamesBox", CAMPUS_REQUEST)
	}

	/** 按筛选条件从第一页开始查询 */
	fun query() {
		page = 0
		total = 0
		sections.clear()
		fetchResult()
	}

	/** 加载下一页，滚动到底部时触发 */
	fun fetchResult() {
		if (_uiState.value == UiState.Loading || _uiState.value == UiState.LoadMore) return
		if (page > 0 && sections.size >= total) return
		_uiState.value = if (page == 0) UiState.Loading else UiState.LoadMore
		val params = JSONObject()
		if (yearTerm?.isNotBlank() == true) params["semester"] = yearTerm
		if (campusValue?.isNotBlank() == true) params["studyCampusCode"] = campusValue
		if (courseNumber?.isNotBlank() == true) params["courseNum"] = courseNumber
		if (courseName?.isNotBlank() == true) params["courseName"] = courseName
		if (teacher?.isNotBlank() == true) params["teacherName"] = teacher
		model.enqueue(
				"jwxt/assistant-manage/assistantInfoQuery/pageList?code=jwxsd_zjxxck",
				"{\"pageNo\":${++page},\"pageSize\":10,\"total\":true,\"param\":$params}",
				RESULT_REQUEST
		)
	}

	override fun onCleared() {
		model.dispose()
	}

	companion object {
		private const val YEAR_TERM_REQUEST = 0
		private const val CAMPUS_REQUEST = 1
		private const val RESULT_REQUEST = 2
	}
}
