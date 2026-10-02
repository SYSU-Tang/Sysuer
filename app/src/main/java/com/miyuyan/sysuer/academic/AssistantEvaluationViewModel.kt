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

class AssistantEvaluationViewModel(application: Application) : AndroidViewModel(application) {
	private val model: JwxtModel = JwxtModel(application)

	/** 学年学期选项 */
	private val _yearTerms = MutableStateFlow<List<String>>(emptyList())
	val yearTerms = _yearTerms.asStateFlow()

	/** 开课单位选项 */
	private val _departmentNames = MutableStateFlow<List<String>>(emptyList())
	val departmentNames = _departmentNames.asStateFlow()
	private val _departmentValues = MutableStateFlow<List<String>>(emptyList())
	val departmentValues = _departmentValues.asStateFlow()


	// 查询页筛选条件
	var yearTerm: String? = null
	var departmentValue: String? = null
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
			"助教学期",
			"助教姓名",
			"助教培养单位",
			"教学班号",
			"课程名称",
			"课程编码",
			"课程类别",
			"课程教学类型",
			"开课单位",
			"是否开班",
			"是否合班",
			"总教学班号",
			"任课教师",
			"课程学时",
			"助教承担的课程教学学时",
			"上课时间地点",
			"助教考核结论"
	)
	private val resultKeys = arrayOf(
			"yearTerm",
			"assistantNum",
			"assistantName",
			"assistantCollege",
			"classNum",
			"courseName",
			"courseNum",
			"courseType",
			"courseTeachingType",
			"courseCollege",
			"openClassFlag",
			"mergeClassFlag",
			"sumClassNum",
			"teacherName",
			"courseHours",
			"assistantHours",
			"teachingTimePlace",
			"conclusion"
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

					DEPARTMENT_REQUEST -> {
						val (names, numbers) = extractValue(
								response.getJSONArray("data"), "departmentName", "departmentNumber"
						)
						_departmentNames.value = names.orEmpty().filterNotNull()
						_departmentValues.value = numbers.orEmpty().filterNotNull()
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
											title = (sections.size + 1).toString(),
											rows = extractValue(item, resultNames, resultKeys)
									)
							)
						}
						_uiState.value =
							if (page == 1 && total == 0) UiState.Empty else UiState.Content
					}
				}
			}
		}
	}

	fun fetchYearTerms() {
		model.enqueue("jwxt/base-info/acadyearterm/findAcadyeartermNamesBox", YEAR_TERM_REQUEST)
	}

	fun fetchDepartments(nameParam: String) {
		model.enqueue(
				"jwxt/base-info/department/findCommonDepartmentPull?nameParm=$nameParam",
				DEPARTMENT_REQUEST
		)
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
		if (yearTerm?.isNotBlank() == true) params["yearTerm"] = yearTerm
		if (departmentValue?.isNotBlank() == true) params["openUnitNum"] = departmentValue
		if (courseName?.isNotBlank() == true) params["courseName"] = courseName
		if (teacher?.isNotBlank() == true) params["teacherName"] = teacher
		model.enqueue(
				"jwxt/assistant-manage/assistantEvaluation/evaluationResultPageList?code=jwxsd_zjpjck",
				"{\"pageNo\":${++page},\"pageSize\":10,\"total\":true,\"param\":$params}",
				RESULT_REQUEST
		)
	}

	override fun onCleared() {
		model.dispose()
	}

	companion object {
		private const val YEAR_TERM_REQUEST = 0
		private const val DEPARTMENT_REQUEST = 1
		private const val RESULT_REQUEST = 2
	}
}
