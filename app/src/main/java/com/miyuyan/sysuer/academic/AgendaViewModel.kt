package com.miyuyan.sysuer.academic

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.alibaba.fastjson2.JSONArray
import com.alibaba.fastjson2.JSONObject
import com.miyuyan.sysuer.model.PortalModel
import com.miyuyan.sysuer.view.RecyclerStateViewModel
import com.miyuyan.sysuer.view.UiState
import kotlinx.coroutines.launch
import java.time.LocalDate

class AgendaViewModel(application: Application) : AndroidViewModel(application),
	RecyclerStateViewModel {
	private val portalModel = PortalModel(application)
	override val uiState = portalModel.getUiState(0)
	val scheduleList = MutableLiveData<JSONArray?>()

	init {
		viewModelScope.launch {
			portalModel.messageChannel.collect { (code, response) ->
				if (code == 0) {
					if (response.getJSONObject("meta")
							.getInteger("statusCode") == 200 && response.get("data") != null
					) response.getJSONArray("data").takeIf { it.isNotEmpty() }?.let {
						val list = it.getJSONObject(0).getJSONArray("newUserScheduleDetailList")
						scheduleList.value = list
						uiState.value = if (list.isNotEmpty()) UiState.Content else UiState.Empty
					} ?: run {
						scheduleList.value = null
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
		portalModel.addAndNext(
				"newClient/api/schedule/newSchedule/getScheduleByTimeZone", "$args", 0
		)
	}

	override fun retry() {
		uiState.value = UiState.Loading
//		portalModel.retry()
	}

	override fun onCleared() {
		portalModel.dispose()
	}
}