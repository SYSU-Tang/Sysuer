package com.miyuyan.sysuer.academic

import android.app.Application
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.alibaba.fastjson2.JSONObject
import com.miyuyan.sysuer.R
import com.miyuyan.sysuer.api.TargetUrl
import com.miyuyan.sysuer.model.LmsModel
import com.miyuyan.sysuer.view.RecyclerStateViewModel
import com.miyuyan.sysuer.view.UiState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** 一条作业日程：所属课程弹窗名 + 事件详情 */
data class HomeworkItem(val course: String, val event: JSONObject)

/**
 * 学习平台作业：拉取 Moodle 近期日程并按事件逐条展示，登录失效时重新登录后自动重试
 */
class HomeworkViewModel(application: Application) : AndroidViewModel(application),
	RecyclerStateViewModel {
	private val model = LmsModel(application)

	val items = mutableStateListOf<HomeworkItem>()

	private val _uiState = model.getUiState(LmsModel.EVENTS_REQUEST)
	override val uiState: StateFlow<UiState> = _uiState.asStateFlow()

	private val _snackbarMessage = MutableSharedFlow<String>(extraBufferCapacity = 16)
	val snackbarMessage: SharedFlow<String> = _snackbarMessage.asSharedFlow()

	init {
		viewModelScope.launch {
			model.message.collect { (code, response) ->
				if (code != LmsModel.EVENTS_REQUEST) return@collect
				var hasError = false
				response.getJSONArray("data")?.filterIsInstance<JSONObject>()?.forEach { item ->
					if (item.getBoolean("error") == true) {
						hasError = true
						_snackbarMessage.tryEmit(
								item.getJSONObject("exception")?.getString("message") ?: ""
						)
					} else {
						item.getJSONObject("data")?.getJSONArray("events")
							?.filterIsInstance<JSONObject>()?.forEach { event ->
								items.add(HomeworkItem(event.getString("popupname") ?: "", event))
							}
					}
				}
				_uiState.value = if (items.isEmpty()) {
					if (hasError) UiState.Error else UiState.Empty
				} else UiState.Content
				if (hasError) {
					model.contextUtil.login(TargetUrl.LMS) {
						model.getUpcomingEvents()
					}
				}
			}
		}
	}

	fun fetch() {
		if (_uiState.value == UiState.Loading) return
		if (items.isEmpty()) _uiState.value = UiState.Loading
		model.getUpcomingEvents()
	}

	override fun retry() {
		fetch()
	}
}
