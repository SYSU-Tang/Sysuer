package com.miyuyan.sysuer.academic

import android.app.Application
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.alibaba.fastjson2.JSONArray
import com.alibaba.fastjson2.JSONObject
import com.miyuyan.sysuer.R
import com.miyuyan.sysuer.api.CommonUtil.extractValue
import com.miyuyan.sysuer.api.DateTimeManager
import com.miyuyan.sysuer.model.JwxtModel
import com.miyuyan.sysuer.view.UiState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 自习室/研讨室查询：校区-教学楼两级联动筛选，按日期与节次范围分页查询空闲教室
 */
class ClassroomQueryViewModel(application: Application) : AndroidViewModel(application) {
	private val model: JwxtModel = JwxtModel(application)

	/** 校区选项 */
	private val _campusNames = MutableStateFlow<List<String>>(emptyList())
	val campusNames = _campusNames.asStateFlow()
	private val _campusValues = MutableStateFlow<List<String>>(emptyList())
	val campusValues = _campusValues.asStateFlow()

	/** 所选校区的教学楼选项 */
	private val _buildingNames = MutableStateFlow<List<String>>(emptyList())
	val buildingNames = _buildingNames.asStateFlow()
	private val _buildingValues = MutableStateFlow<List<String>>(emptyList())
	val buildingValues = _buildingValues.asStateFlow()

	private val _dateMillis = MutableStateFlow(System.currentTimeMillis())
	val dateMillis = _dateMillis.asStateFlow()

	/** 查询结果 */
	val rooms = mutableStateListOf<JSONObject>()

	private val _uiState = model.getUiState(RESULT_REQUEST)
	val uiState: StateFlow<UiState> = _uiState.asStateFlow()

	private val _snackbarMessage = MutableSharedFlow<String>(extraBufferCapacity = 16)
	val snackbarMessage: SharedFlow<String> = _snackbarMessage.asSharedFlow()

// 查询页筛选条件
var campusSelection: List<String> = emptyList()
var buildingSelection: List<String> = emptyList()
var typeSelection: List<String> = listOf(SELF_STUDY_ROOM, SEMINAR_ROOM)
var sections: ClosedFloatingPointRange<Float> = 1f..11f

	private var page = 0
	private var total = -1

	/** 教室图片经 WebVPN 鉴权加载所需的凭证 */
	val host: String get() = model.host
	val cookie: String get() = model.cookie

	init {
		viewModelScope.launch {
			model.message.collect { (code, response) ->
				if (response.getInteger("code") != 200) {
					if (code == RESULT_REQUEST) _uiState.value = UiState.Error
					return@collect
				}
				when (code) {
					CAMPUS_REQUEST -> {
						val data = response.getJSONArray("data") ?: return@collect
						val (names, values) = extractValue(data, "campusName", "id")
						_campusNames.value = names
						_campusValues.value = values
					}

					BUILDING_REQUEST -> {
						val data = response.getJSONArray("data") ?: return@collect
						val (names, values) = extractValue(data, "dataName", "id")
						_buildingNames.value = names
						_buildingValues.value = values
					}

					RESULT_REQUEST -> {
						val body = response.getJSONObject("data") ?: return@collect
						if (page == 1) {
							total = body.getIntValue("total", -1)
							rooms.clear()
						}
						body.getJSONArray("rows").filterIsInstance<JSONObject>().forEach {
							rooms.add(it)
						}
						_uiState.value =
							if (page == 1 && rooms.isEmpty()) UiState.Empty else UiState.Content
					}
				}
			}
		}
	}

	fun fetchCampuses() {
		model.enqueue("jwxt/base-info/campus/findCampusNamesBox", CAMPUS_REQUEST)
	}

	/** 按所选校区联动教学楼，并清空已选教学楼 */
	fun fetchBuildings() {
		buildingSelection = emptyList()
		if (campusSelection.isEmpty()) {
			_buildingNames.value = emptyList()
			_buildingValues.value = emptyList()
			return
		}
		model.enqueue(
				"jwxt/schedule/agg/selfStudyClassRoom/buildingConditionPull",
				JSONObject.of("campusIdList", JSONArray(campusSelection)).toJSONString(),
				BUILDING_REQUEST,
		)
	}

	/** 更新查询日期（毫秒时间戳） */
	fun setDate(millis: Long) {
		_dateMillis.value = millis
	}

	/** 按筛选条件从第一页开始查询 */
	fun query() {
		if (buildingSelection.isEmpty()) {
			_snackbarMessage.tryEmit(
					getApplication<Application>().getString(R.string.select_teaching_building)
			)
			return
		}
		page = 0
		total = -1
		rooms.clear()
		fetchResult()
	}

	/** 加载下一页，滚动到底部时触发 */
	fun fetchResult() {
		if (buildingSelection.isEmpty()) return
		if (_uiState.value == UiState.Loading || _uiState.value == UiState.LoadMore) return
		if (total >= 0 && rooms.size >= total) return
		_uiState.value = if (page == 0) UiState.Loading else UiState.LoadMore
		val param = JSONObject.of(
				"dateStr", DateTimeManager.toDateString(_dateMillis.value),
				"teachingBuildIDs", JSONArray(buildingSelection),
				"startClassTimes", sections.start.toInt(),
				"endClassTimes", sections.endInclusive.toInt(),
				"classRoomTagList", JSONArray(typeSelection),
		)
		model.enqueue(
				"jwxt/schedule/agg/selfStudyClassRoom/pageListStudyClassroom",
				JSONObject.of("pageNo", ++page, "pageSize", PAGE_SIZE, "param", param).toJSONString(),
				RESULT_REQUEST,
		)
	}

	override fun onCleared() {
		model.dispose()
	}

	companion object {
		private const val CAMPUS_REQUEST = 0
		private const val BUILDING_REQUEST = 1
		private const val RESULT_REQUEST = 2
		private const val PAGE_SIZE = 20

		/** 教室类型代号：自习室 */
		const val SELF_STUDY_ROOM = "003"

		/** 教室类型代号：研讨室 */
		const val SEMINAR_ROOM = "002"
	}
}
