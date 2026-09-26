package com.miyuyan.sysuer.academic

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.alibaba.fastjson2.JSONArray
import com.alibaba.fastjson2.JSONObject
import com.miyuyan.sysuer.model.PortalModel
import com.miyuyan.sysuer.view.RecyclerStateViewModel
import com.miyuyan.sysuer.view.UiState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class AgendaViewModel(application: Application) : AndroidViewModel(application),
	RecyclerStateViewModel {
	private val portalModel = PortalModel(application)
	override val uiState = portalModel.getUiState(0)
	private val _scheduleList = MutableStateFlow<JSONArray?>(null)
	val scheduleList = _scheduleList.asFlow()

	init {
		viewModelScope.launch {
			portalModel.message.collect { (code, response) ->
				if (code == 0) {
					if (response.getJSONObject("meta")
							.getInteger("statusCode") == 200 && response.get("data") != null
					) response.getJSONArray("data").takeIf { it.isNotEmpty() }?.let {
						val list = it.getJSONObject(0).getJSONArray("newUserScheduleDetailList")
						_scheduleList.value = list
						uiState.value = if (list.isNotEmpty()) UiState.Content else UiState.Empty
					} ?: run {
						_scheduleList.value = null
						uiState.value = UiState.Empty
					} else uiState.value = UiState.Error
				}
			}
		}
	}

	fun loadSchedule(day: LocalDate) {
		uiState.value = UiState.Loading
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

	override fun retry() {
		uiState.value = UiState.Loading
		portalModel.retryAll()
	}

	override fun onCleared() {
		portalModel.dispose()
	}
}