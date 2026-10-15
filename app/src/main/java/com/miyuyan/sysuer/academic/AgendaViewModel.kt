package com.miyuyan.sysuer.academic

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.alibaba.fastjson2.JSONObject
import com.miyuyan.sysuer.model.PortalModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * 日程页状态：[Content] 携带条目，其余为无数据的中间态。
 * 合并 uiState 与 scheduleList 两个流，避免列表与状态不同步。
 */
sealed interface AgendaUiState {
	data object Loading : AgendaUiState

	data object Empty : AgendaUiState

	data object Error : AgendaUiState

	data class Content(val entries: List<JSONObject>) : AgendaUiState
}

class AgendaViewModel(application: Application) : AndroidViewModel(application) {
	private val portalModel = PortalModel(application)
	private val _uiState = MutableStateFlow<AgendaUiState>(AgendaUiState.Loading)
	val uiState = _uiState.asStateFlow()

	init {
		viewModelScope.launch {
			portalModel.message.collect { (code, response) ->
				if (code != 0) return@collect
				val data = response.getJSONArray("data")
				if (response.getJSONObject("meta").getInteger("statusCode") == 200 &&
					data != null
				) {
					val entries = data.takeIf { it.isNotEmpty() }
						?.getJSONObject(0)
						?.getJSONArray("newUserScheduleDetailList")
						?.mapNotNull { it as? JSONObject }
						.orEmpty()
					_uiState.value =
						if (entries.isEmpty()) AgendaUiState.Empty
						else AgendaUiState.Content(entries)
				} else _uiState.value = AgendaUiState.Error
			}
		}
	}

	fun loadSchedule(day: LocalDate) {
		_uiState.value = AgendaUiState.Loading
		val args = JSONObject.of(
				"startTime",
				day,
				"endTime",
				day,
				"types",
				null,
				"isMine",
				"1",
				"teamWorkDeptId",
				null
		)
		portalModel.enqueue(
				"newClient/api/schedule/newSchedule/getScheduleByTimeZone", "$args", 0
		)
	}

	fun retry() {
		_uiState.value = AgendaUiState.Loading
		portalModel.retryAll()
	}

	override fun onCleared() {
		portalModel.dispose()
	}
}
