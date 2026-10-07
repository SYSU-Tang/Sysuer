package com.miyuyan.sysuer.academic

import android.app.Application
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import com.alibaba.fastjson2.JSONArray
import com.alibaba.fastjson2.JSONObject
import com.miyuyan.sysuer.R
import com.miyuyan.sysuer.api.ContextUtil
import com.miyuyan.sysuer.model.JwxtModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.jsonPrimitive
import java.util.Locale

/** 选课阶段信息：阶段名（工具栏标题）、起止时间（副标题）、是否不在选课期间 */
data class CourseSelectionStage(
	val name: String? = null,
	val time: String? = null,
	val notInSelection: Boolean = false,
)

/** 高级筛选下拉项：展示名 + 提交代码 */
data class FilterOption(val name: String?, val code: String?)

/**
 * 选课统一 ViewModel：主选课（轮次/类别/筛选）、选课预览、已选课程三个页面
 * 共用一个 JwxtModel，按请求码段区分；另含高级筛选选项数据
 */
class CourseSelectionViewModel(application: Application) : AndroidViewModel(application) {
	private val model = JwxtModel(application)
	private val contextUtil = ContextUtil.getInstance(application)

	/** 请求体构建：默认值与 null 不参与序列化 */
	private val bodyJson = Json {
		encodeDefaults = false
		explicitNulls = false
	}

	// ==================== 主选课 ====================
	private val _mainCourses = mutableStateListOf<JSONObject>()
	val mainCourses: List<JSONObject> = _mainCourses
	var mainPage: Int = 0
		private set
	var mainTotal: Int = -1
		private set
	private val _mainIsLoading = MutableStateFlow(false)
	val mainIsLoading: StateFlow<Boolean> = _mainIsLoading.asStateFlow()

	/** 选课轮次：1 = 本专业，4 = 全校公选，2 = 跨专业 */
	private val _selectionType = MutableStateFlow(1)
	val selectionType: StateFlow<Int> = _selectionType.asStateFlow()

	/** 课程类别代码：11 专必 / 21 专选 / 30 院内公选 / 10 体育 / 1 英语 / 31 荣誉 */
	private val _selectionCategory = MutableStateFlow(11)
	val selectionCategory: StateFlow<Int> = _selectionCategory.asStateFlow()

	private val _hideSelected = MutableStateFlow(false)
	val hideSelected: StateFlow<Boolean> = _hideSelected.asStateFlow()
	private val _vacancySort = MutableStateFlow(false)
	val vacancySort: StateFlow<Boolean> = _vacancySort.asStateFlow()
	private val _hideVacancy = MutableStateFlow(false)
	val hideVacancy: StateFlow<Boolean> = _hideVacancy.asStateFlow()
	private val _onlyCollection = MutableStateFlow(false)
	val onlyCollection: StateFlow<Boolean> = _onlyCollection.asStateFlow()

	/** 体育课程排序入口是否可见 */
	private val _showPeSort = MutableStateFlow(false)
	val showPeSort: StateFlow<Boolean> = _showPeSort.asStateFlow()
	private val _peCourses = mutableStateListOf<JSONObject>()
	val peCourses: List<JSONObject> = _peCourses

	private val _stage = MutableStateFlow(CourseSelectionStage())
	val stage: StateFlow<CourseSelectionStage> = _stage.asStateFlow()

	private val _mainFilterName = MutableStateFlow(CourseFilterNameData())
	val mainFilterName: StateFlow<CourseFilterNameData> = _mainFilterName.asStateFlow()
	private val _mainFilterValue = MutableStateFlow(CourseFilterValueData())
	val mainFilterValue: StateFlow<CourseFilterValueData> = _mainFilterValue.asStateFlow()

	// ==================== 选课预览 ====================
	private val _previewCourses = mutableStateListOf<JSONObject>()
	val previewCourses: List<JSONObject> = _previewCourses
	var previewPage: Int = 1
		private set
	var previewTotal: Int = -1
		private set
	private val _previewIsLoading = MutableStateFlow(false)
	val previewIsLoading: StateFlow<Boolean> = _previewIsLoading.asStateFlow()

	/** 预览轮次：1 = 本专业，4 = 全校公选，2 = 通识 */
	private val _previewType = MutableStateFlow(1)
	val previewType: StateFlow<Int> = _previewType.asStateFlow()
	private val _hiddenSelectedStatus = MutableStateFlow(false)
	val hiddenSelectedStatus: StateFlow<Boolean> = _hiddenSelectedStatus.asStateFlow()
	private val _previewFilterName = MutableStateFlow(CourseFilterNameData())
	val previewFilterName: StateFlow<CourseFilterNameData> = _previewFilterName.asStateFlow()
	private val _previewFilterValue = MutableStateFlow(CourseFilterValueData())
	val previewFilterValue: StateFlow<CourseFilterValueData> = _previewFilterValue.asStateFlow()

	// ==================== 已选课程 ====================
	private val _selectedCourses = mutableStateListOf<JSONObject>()
	val selectedCourses: List<JSONObject> = _selectedCourses
	var selectedPage: Int = 1
		private set
	var selectedTotal: Int = -1
		private set
	var selectedIsLoading: MutableStateFlow<Boolean> = MutableStateFlow(false)
		private set

	/** 状态筛选：1 = 包含该状态，0 = 排除 */
	private val _selectedSuccess = MutableStateFlow(1)
	val selectedSuccess: StateFlow<Int> = _selectedSuccess.asStateFlow()
	private val _selectedFailure = MutableStateFlow(1)
	val selectedFailure: StateFlow<Int> = _selectedFailure.asStateFlow()
	private val _selectedRetired = MutableStateFlow(1)
	val selectedRetired: StateFlow<Int> = _selectedRetired.asStateFlow()
	private val _selectedWaiting = MutableStateFlow(1)
	val selectedWaiting: StateFlow<Int> = _selectedWaiting.asStateFlow()

	/** 课程类别代码：空 = 全部 */
	private val _selectedCategory = MutableStateFlow("")
	val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

	// ==================== 高级筛选选项 ====================
	private val _filterOptions = MutableStateFlow(List(5) { emptyList<FilterOption>() })
	val filterOptions: StateFlow<List<List<FilterOption>>> = _filterOptions.asStateFlow()
	private var filterOptionsLoaded = false

	private var term: String? = null

	init {
		viewModelScope.launch {
			model.message.collect { (code, response) ->
				if (response.getInteger("code") != 200) return@collect
				when (code) {
					MAIN_INFO -> {
						val data = response.getJSONObject("data")
						if (data.containsKey("code") && data.getInteger("code") == 100) {
							_stage.value = CourseSelectionStage(notInSelection = true)
						} else if (data.containsKey("code") && data.getInteger("code") == 200) {
							term = data.getString("semesterYear")
							val start = data.getString("startTime", "")
							val end = data.getString("endTime", "")
							_stage.value = CourseSelectionStage(
									name = data.getString("electiveCourseStageName"),
									time = if (start.isNotEmpty() && end.isNotEmpty()) "$start~$end" else null,
							)
							courseList()
						}
					}
					MAIN_COURSES -> response.getJSONObject("data")?.run {
						mainTotal = getInteger("total")
						getJSONArray("rows").forEach { e -> _mainCourses.add(e as JSONObject) }
						_mainIsLoading.value = false
					}
					MAIN_ACTION -> {
						val message = response.getString("data")
						if (!message.isNullOrEmpty()) contextUtil.toast(message)
						reloadMain()
					}
					MAIN_PE -> {
						_peCourses.clear()
						if (response.getJSONArray("data").isEmpty()) {
							_showPeSort.value = false
						} else {
							response.getJSONArray("data")
								.sortedBy { (it as JSONObject).getInteger("volunteerNum") }
								.forEach { e -> _peCourses.add(e as JSONObject) }
							_showPeSort.value = true
						}
					}
					PREVIEW_COURSES -> {
						val data = response.getJSONObject("data")
						previewTotal = data.getInteger("total")
						data.getJSONArray("rows").forEach { e -> _previewCourses.add(e as JSONObject) }
						_previewIsLoading.value = false
					}
					PREVIEW_ACTION -> {
						contextUtil.toast(
								response.getString(
										"data",
										getApplication<Application>().getString(R.string.action_success)
								)
						)
						reloadPreview()
					}
					SELECTED_COURSES -> {
						val data = response.getJSONObject("data")
						selectedTotal = data.getInteger("total")
						data.getJSONArray("rows").forEach { e -> _selectedCourses.add(e as JSONObject) }
						selectedIsLoading.value = false
					}
					SELECTED_ACTION -> {
						val message = response.getString("data")
						if (!message.isNullOrEmpty()) contextUtil.toast(message)
						reloadSelected()
					}
					in FILTER_BASE..FILTER_BASE + 4 -> handleFilterOptions(code - FILTER_BASE, response)
				}
			}
		}
		info()
	}

	// ==================== 主选课 ====================

	/** 切换选课轮次 */
	fun setType(newType: Int) {
		if (selectionType.value == newType) return
		_selectionType.value = newType
		if (term == null) info() else reloadMain()
	}

	/**
	 * 切换课程类别。原始接口用 (selectedType, selectedCate) 配对定位：
	 * 专必 (1,11) / 专选 (1,21) / 校公选 (1,30) / 体育 (3,10) / 英语 (5,1) / 公必 (1,10) / 荣誉 (1,31)
	 */
	fun setCategory(newType: Int, newCategory: Int) {
		if (selectionType.value == newType && selectionCategory.value == newCategory) return
		_selectionType.value = newType
		_selectionCategory.value = newCategory
		if (newType == 3 && newCategory == 10) getPE() else _showPeSort.value = false
		if (term == null) info() else reloadMain()
	}

	/** 调整筛选 chips，任一变化才重新拉取 */
	fun setFlags(
		hideSelected: Boolean,
		vacancySort: Boolean,
		hideVacancy: Boolean,
		onlyCollection: Boolean,
	) {
		if (this.hideSelected.value == hideSelected && this.vacancySort.value == vacancySort &&
			this.hideVacancy.value == hideVacancy && this.onlyCollection.value == onlyCollection
		) return
		this._hideSelected.value = hideSelected
		this._vacancySort.value = vacancySort
		this._hideVacancy.value = hideVacancy
		this._onlyCollection.value = onlyCollection
		reloadMain()
	}

	fun setMainFilterName(name: CourseFilterNameData) {
		_mainFilterName.value = name
	}

	fun setMainFilterValue(value: CourseFilterValueData) {
		_mainFilterValue.value = value
		reloadMain()
	}

	fun reloadMain() {
		mainPage = 0
		mainTotal = -1
		_mainCourses.clear()
		courseList()
	}

	/** 分页拉取课程列表 */
	fun courseList() {
		if (term == null) return
		if (mainTotal != -1 && _mainCourses.size >= mainTotal) return
		_mainIsLoading.value = true
		val param = JsonObject(bodyJson.encodeToJsonElement(mainFilterValue.value).jsonObject +
				buildJsonObject {
					put("semesterYear", term)
					put("selectedType", selectionType.value)
					put("selectedCate", selectionCategory.value)
					put("hiddenConflictStatus", "0")
					put("hiddenSelectedStatus", if (hideSelected.value) "1" else "0")
					put("hiddenEmptyStatus", if (hideVacancy.value) "1" else "0")
					put("vacancySortStatus", if (vacancySort.value) "1" else "0")
					put("collectionStatus", if (onlyCollection.value) "1" else "0")
				}
		)
		val body = buildJsonObject {
			put("pageNo", ++mainPage)
			put("pageSize", 10)
			put("total", true)
			put("param", param)
		}.toString()
		model.enqueue("jwxt/choose-course-front-server/classCourseInfo/course/list", body, MAIN_COURSES)
	}

	/** 选课轮次信息：学期、阶段名与起止时间 */
	private fun info() {
		model.enqueue("jwxt/choose-course-front-server/classCourseInfo/selectCourseInfo", MAIN_INFO)
	}

	/** 体育课程已选列表（可排序） */
	fun getPE() {
		model.enqueue("jwxt/choose-course-front-server/selectedCourse/sportsSelectedlist", MAIN_PE)
	}

	/** 收藏课程（主选课） */
	fun likeMain(code: String?) {
		model.enqueue("jwxt/choose-course-front-server/stuCollectedCourse/create", "{\"classesID\":\"$code\",\"selectedType\":\"1\"}", MAIN_ACTION)
	}

	/** 选课 */
	fun selectCourse(code: String?) {
		model.enqueue(
				"jwxt/choose-course-front-server/classCourseInfo/course/choose",
				String.format(
						Locale.getDefault(),
						"{\"clazzId\":\"%s\",\"selectedType\":\"%d\",\"selectedCate\":\"%d\",\"check\":true}",
						code, selectionType.value, selectionCategory.value
				), MAIN_ACTION
		)
	}

	/** 退课 */
	fun unselectCourse(classId: String?, code: String?) {
		model.enqueue(
				"jwxt/choose-course-front-server/classCourseInfo/course/back",
				String.format(
						Locale.getDefault(),
						"{\"courseId\":\"%s\",\"clazzId\":\"%s\",\"selectedType\":\"%d\"}",
						classId, code, selectionType.value
				), MAIN_ACTION
		)
	}

	/** 提交体育课程志愿排序 */
	fun sortPE(data: String) {
		model.call("jwxt/choose-course-front-server/selectedCourse/updateSportsSelectedlist", data, null, object : okhttp3.Callback {
			override fun onFailure(call: okhttp3.Call, e: java.io.IOException) {
				contextUtil.toast(R.string.save_fail)
			}

			override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
				if (response.isSuccessful && response.code == 200) contextUtil.toast(R.string.save_successful)
				else model.login {
					sortPE(data)
				}
			}
		})
	}

	/** 按新的顺序提交体育志愿 */
	fun submitPeOrder(courses: List<JSONObject>) {
		val data = JSONArray()
		courses.forEachIndexed { i, v ->
			data.add(
					JSONObject.of(
							"studentFilterID", v.getString("studentFilterID"),
							"volunteerNum", i + 1,
					)
			)
		}
		sortPE("$data")
	}

	// ==================== 选课预览 ====================

	fun setPreviewType(newType: Int) {
		if (previewType.value == newType) return
		_previewType.value = newType
		reloadPreview()
	}

	fun setHiddenSelectedStatus(newStatus: Boolean) {
		_hiddenSelectedStatus.value = newStatus
		reloadPreview()
	}

	fun setPreviewFilterName(name: CourseFilterNameData) {
		_previewFilterName.value = name
	}

	fun setPreviewFilterValue(value: CourseFilterValueData) {
		_previewFilterValue.value = value
		reloadPreview()
	}

	fun reloadPreview() {
		previewPage = 1
		previewTotal = -1
		_previewCourses.clear()
		loadMorePreview()
	}

	/** 分页拉取预览课程列表 */
	fun loadMorePreview() {
		if (previewTotal != -1 && _previewCourses.size >= previewTotal) return
		_previewIsLoading.value = true
		val body = buildJsonObject {
			put("pageNo", previewPage++)
			put("pageSize", 10)
			put("param", JsonObject(
					bodyJson.encodeToJsonElement<CourseFilterValueData>(previewFilterValue.value).jsonObject + buildJsonObject {
						put(
								"hiddenSelectedStatus", if (hiddenSelectedStatus.value) "1" else ""
						)
						put("type", previewType.value)
					})
			)
		}.toString()
		model.enqueue("jwxt/choose-course-front-server/schoolCourse/pageList", body, PREVIEW_COURSES)
	}

	/** 收藏课程（预览） */
	fun likePreview(classesID: String?) {
		model.enqueue("jwxt/choose-course-front-server/stuCollectedCourse/create", "{\"classesID\":\"$classesID\",\"selectedType\":\"1\"}", PREVIEW_ACTION)
	}

	// ==================== 已选课程 ====================

	/** 调整选课状态筛选，任一变化才重新拉取 */
	fun setSelectedStatusFilter(success: Int, failure: Int, retired: Int, waiting: Int) {
		if (this.selectedSuccess.value == success && this.selectedFailure.value == failure &&
			this.selectedRetired.value == retired && this.selectedWaiting.value == waiting
		) return
		this._selectedSuccess.value = success
		this._selectedFailure.value = failure
		this._selectedRetired.value = retired
		this._selectedWaiting.value = waiting
		reloadSelected()
	}

	fun setSelectedCategory(category: String) {
		if (this.selectedCategory.value == category) return
		this._selectedCategory.value = category
		reloadSelected()
	}

	fun reloadSelected() {
		selectedPage = 1
		selectedTotal = -1
		_selectedCourses.clear()
		loadMoreSelected()
	}

	/** 分页拉取已选课程列表 */
	fun loadMoreSelected() {
		if (selectedTotal != -1 && _selectedCourses.size >= selectedTotal) return
		selectedIsLoading.value = true
		val body = buildJsonObject {
			put("pageNo", selectedPage++)
			put("pageSize", 10)
			put("total", true)
			put("param", buildJsonObject {
				put("successStatus", "${selectedSuccess.value}")
				put("failureStatus", "${selectedFailure.value}")
				put("retiredClass", "${selectedRetired.value}")
				put("waitingScreen", "${selectedWaiting.value}")
				if (selectedCategory.value.isNotEmpty()) {
					put("courseCateCode", selectedCategory.value)
				}
			})
		}.toString()
		model.enqueue("jwxt/choose-course-front-server/selectedCourse/list", body, SELECTED_COURSES)
	}

	/** 退课（已选） */
	fun unselectSelected(courseId: String?, clazzId: String?, type: String?) {
		model.enqueue(
				"jwxt/choose-course-front-server/classCourseInfo/course/back",
				"{\"courseId\":\"$courseId\",\"clazzId\":\"$clazzId\",\"selectedType\":\"$type\"}",
				SELECTED_ACTION
		)
	}

	/** 选课（已选） */
	fun selectSelected(clazzId: String?, type: String?, category: String?) {
		model.enqueue(
				"jwxt/choose-course-front-server/classCourseInfo/course/choose",
				"{\"clazzId\":\"$clazzId\",\"selectedType\":\"$type\",\"selectedCate\":\"$category\",\"check\":true}",
				SELECTED_ACTION
		)
	}

	/** 设置/取消二级专业（PNP） */
	fun setPNP(type: String?, clazzId: String?) {
		model.enqueue(
				"jwxt/choose-course-front-server/selectedCourse/setTwoTier?type=$type",
				"{\"clazzId\":\"$clazzId\"}",
				SELECTED_ACTION
		)
	}

	// ==================== 高级筛选选项 ====================

	/** 拉取五个筛选下拉的数据源（校区/星期/节次/语言/特殊），已加载则跳过 */
	fun fetchFilterOptions() {
		if (filterOptionsLoaded) return
		filterOptionsLoaded = true
		(0..4).forEach { i ->
			model.enqueue(
					arrayOf(
							"jwxt/base-info/campus/findCampusNamesBox",
							"jwxt/base-info/codedata/findcodedataNames?datableNumber=233",
							"jwxt/base-info/AcadyeartermSet/minorName?schoolYear=2025-1",
							"jwxt/base-info/codedata/findcodedataNames?datableNumber=204",
							"jwxt/base-info/codedata/findcodedataNames?datableNumber=387"
					)[i], FILTER_BASE + i
			)
		}
	}

	private fun handleFilterOptions(index: Int, response: JSONObject) {
		val data = response.getJSONArray("data") ?: return
		val nameFields = arrayOf("campusName", "dataName", "minorName", "dataName", "dataName")
		val codeFields = arrayOf("id", "dataNumber", "sectionNumber", "dataNumber", "dataNumber")
		val options = mutableListOf<FilterOption>()
		data.forEach { a: Any? ->
			val item = a as JSONObject
			options.add(FilterOption(item.getString(nameFields[index]), item.getString(codeFields[index])))
		}
		_filterOptions.value = _filterOptions.value.toMutableList().also { it[index] = options }
	}

	override fun onCleared() {
		model.dispose()
	}

	companion object {
		// 主选课
		const val MAIN_INFO = 0
		const val MAIN_COURSES = 1
		const val MAIN_ACTION = 3
		const val MAIN_PE = 4

		// 选课预览
		const val PREVIEW_COURSES = 10
		const val PREVIEW_ACTION = 11

		// 已选课程
		const val SELECTED_COURSES = 20
		const val SELECTED_ACTION = 21

		// 高级筛选选项：30~34
		const val FILTER_BASE = 30
	}
}
