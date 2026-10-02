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

class CourseQueryViewModel(application: Application) : AndroidViewModel(application) {
	private val model: JwxtModel = JwxtModel(application)

	/** 学年学期/结束学年选项 */
	private val _yearTerms = MutableStateFlow<List<String>>(emptyList())
	val yearTerms = _yearTerms.asStateFlow()

	/** 校区选项 */
	private val _campusNames = MutableStateFlow<List<String>>(emptyList())
	val campusNames = _campusNames.asStateFlow()
	private val _campusValues = MutableStateFlow<List<String>>(emptyList())
	val campusValues = _campusValues.asStateFlow()

	/** 教学班层次选项 */
	private val _classLevelNames = MutableStateFlow<List<String>>(emptyList())
	val classLevelNames = _classLevelNames.asStateFlow()
	private val _classLevelValues = MutableStateFlow<List<String>>(emptyList())
	val classLevelValues = _classLevelValues.asStateFlow()

	/** 教学类型选项 */
	private val _teachingTypeNames = MutableStateFlow<List<String>>(emptyList())
	val teachingTypeNames = _teachingTypeNames.asStateFlow()
	private val _teachingTypeValues = MutableStateFlow<List<String>>(emptyList())
	val teachingTypeValues = _teachingTypeValues.asStateFlow()

	/** 教学楼选项 */
	private val _buildingNames = MutableStateFlow<List<String>>(emptyList())
	val buildingNames = _buildingNames.asStateFlow()
	private val _buildingValues = MutableStateFlow<List<String>>(emptyList())
	val buildingValues = _buildingValues.asStateFlow()

	/** 开课单位选项 */
	private val _departmentNames = MutableStateFlow<List<String>>(emptyList())
	val departmentNames = _departmentNames.asStateFlow()
	private val _departmentValues = MutableStateFlow<List<String>>(emptyList())
	val departmentValues = _departmentValues.asStateFlow()

	/** 教室选项 */
	private val _classroomNames = MutableStateFlow<List<String>>(emptyList())
	val classroomNames = _classroomNames.asStateFlow()
	private val _classroomValues = MutableStateFlow<List<String>>(emptyList())
	val classroomValues = _classroomValues.asStateFlow()

	// 查询页筛选条件
	var yearTerm: String? = null
	var endYear: String? = null
	var weekDay: Float = 0f
	var beginWeek: Float = 0f
	var endWeek: Float = 0f
	var beginLesson: Float = 0f
	var endLesson: Float = 0f
	var campusValue: String? = null
	var buildingValue: String? = null
	var classroomValue: String? = null
	var classroomName: String = ""
	var courseName: String? = null
	var courseNumber: String? = null
	var courseType: String? = null
	var teachingType: String? = null
	var className: String? = null
	var classLevelValue: String? = null
	var classNumber: String? = null
	var teacher: String? = null
	var departmentValue: String? = null
	var departmentName: String = ""

	/** 查询结果 */
	val sections = mutableStateListOf<SectionData>()

	private val _uiState = model.getUiState(RESULT_REQUEST)
	val uiState: StateFlow<UiState> = _uiState.asStateFlow()

	private var page = 0
	private var total = 0

	private val resultNames = arrayOf(
			"学年学期",
			"课程名称",
			"课程编号",
			"开课单位",
			"课程类别",
			"学分",
			"主讲教师",
			"限选人数",
			"已选人数",
			"考试方式",
			"上课信息",
			"上课校区",
			"修读对象",
			"教学班号"
	)
	private val resultKeys = arrayOf(
			"yearTerm",
			"courseName",
			"courseNum",
			"openingUnitName",
			"courseCategoryName",
			"score",
			"teachingName",
			"limitNumber",
			"selectedNumber",
			"examMode",
			"teachingTimePlaceStr",
			"openingSchoolName",
			"readObj",
			"classNumber"
	)

	init {
		viewModelScope.launch {
			model.message.collect { (code, response) ->
				if (response.getInteger("code") != 200) {
					if (code == RESULT_REQUEST) _uiState.value = UiState.Error
					return@collect
				}
				val data = response.getJSONArray("data") ?: return@collect
				when (code) {
					YEAR_TERM_REQUEST -> {
						_yearTerms.value = extractValue(data, "acadYearSemester").filterNotNull()
					}

					CAMPUS_REQUEST -> {
						val (names, values) = extractValue(data, "campusName", "id")
						_campusNames.value = names
						_campusValues.value = values
					}

					CLASS_LEVEL_REQUEST -> {
						val (names, values) = extractValue(data, "dataName", "dataNumber")
						_classLevelNames.value = names
						_classLevelValues.value = values
					}

					TEACHING_TYPE_REQUEST -> {
						val (names, values) = extractValue(data, "dataName", "dataNumber")
						_teachingTypeNames.value = names
						_teachingTypeValues.value = values
					}

					BUILDING_REQUEST -> {
						val (names, values) = extractValue(data, "name", "id")
						_buildingNames.value = names
						_buildingValues.value = values
					}

					DEPARTMENT_REQUEST -> {
						val (names, values) = extractValue(
								data, "departmentName", "departmentNumber"
						)
						_departmentNames.value = names
						_departmentValues.value = values
					}

					CLASSROOM_REQUEST -> {
						val (names, values) = extractValue(data, "number", "id")
						_classroomNames.value = names
						_classroomValues.value = values
					}

					RESULT_REQUEST -> {
						val body = response.getJSONObject("data") ?: return@collect
						if (page == 1) {
							total = body.getIntValue("total", 0)
							sections.clear()
						}
						body.getJSONArray("rows").filterIsInstance<JSONObject>().forEach { item ->
							val rows = extractValue(item, resultNames, resultKeys)
							// 上课信息中时间与地点以",/ "分隔，换成更易读的排版
							rows.getOrNull(10)?.value =
								rows.getOrNull(10)?.value?.replace(",", "\n")?.replace("/", " | ")
							sections.add(
									SectionData(
											title = item.getString("courseName"), rows = rows
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

	fun fetchClassLevels() {
		model.enqueue(
				"jwxt/base-info/codedata/findcodedataNames?datableNumber=216", CLASS_LEVEL_REQUEST
		)
	}

	fun fetchTeachingTypes() {
		model.enqueue(
				"jwxt/base-info/codedata/findcodedataNames?datableNumber=350", TEACHING_TYPE_REQUEST
		)
	}

	fun fetchBuildings() {
		model.enqueue(
				"jwxt/base-info/teaching-building/pull?campusId=${campusValue ?: ""}",
				BUILDING_REQUEST
		)
	}

	fun fetchDepartments(nameParam: String) {
		model.enqueue(
				"jwxt/base-info/department/findCommonDepartmentPull?nameParm=$nameParam",
				DEPARTMENT_REQUEST
		)
	}

	fun fetchClassrooms(query: String = "") {
		model.enqueue(
				"jwxt/base-info/classroom/queryclassroombymulticondition?campusId=${campusValue ?: ""}&buildingId=${buildingValue ?: ""}&classroomCode=${query}",
				CLASSROOM_REQUEST
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
		model.enqueue(
				"jwxt/schedule/agg/schoolOpeningCoursesSchedule/querySchoolOpeningCourses",
				"{\"pageNo\":${++page},\"pageSize\":10,\"total\":true,\"param\":${buildParams()}}",
				RESULT_REQUEST
		)
	}

	private fun buildParams(): JSONObject {
		val params = JSONObject()
		if (weekDay > 0f) params["weekDay"] = weekDay.toInt().toString()
		if (beginWeek > 0f) params["beginWeek"] = beginWeek.toInt().toString()
		if (endWeek > 0f) params["endWeek"] = endWeek.toInt().toString()
		if (beginLesson > 0f) params["beginLesson"] = beginLesson.toInt().toString()
		if (endLesson > 0f) params["endLesson"] = endLesson.toInt().toString()
		if (yearTerm?.isNotBlank() == true) params["yearTerm"] = yearTerm
		if (endYear?.isNotBlank() == true) params["endYearTerm"] = endYear
		if (classLevelValue?.isNotBlank() == true) params["classLevelNumber"] = classLevelValue
		if (campusValue?.isNotBlank() == true) params["openingSchoolNumber"] = campusValue
		if (courseType?.isNotBlank() == true) params["courseCategoryNumber"] = courseType
		if (buildingValue?.isNotBlank() == true) params["teachingBuildingID"] = buildingValue
		if (teachingType?.isNotBlank() == true) params["teachingTypeNumber"] = teachingType
		if (classroomValue?.isNotBlank() == true) params["classRoomID"] = classroomValue
		if (departmentValue?.isNotBlank() == true) params["openingUnitNumber"] = departmentValue
		if (courseName?.isNotBlank() == true) params["courseName"] = courseName
		if (teacher?.isNotBlank() == true) params["teachingNum"] = teacher
		if (classNumber?.isNotBlank() == true) params["classNumber"] = classNumber
		if (className?.isNotBlank() == true) params["className"] = className
		if (courseNumber?.isNotBlank() == true) params["courseNumber"] = courseNumber
		return params
	}

	override fun onCleared() {
		model.dispose()
	}

	companion object {
		private const val YEAR_TERM_REQUEST = 0
		private const val CAMPUS_REQUEST = 1
		private const val CLASS_LEVEL_REQUEST = 2
		private const val TEACHING_TYPE_REQUEST = 3
		private const val BUILDING_REQUEST = 4
		private const val DEPARTMENT_REQUEST = 5
		private const val CLASSROOM_REQUEST = 6
		private const val RESULT_REQUEST = 7
	}
}
