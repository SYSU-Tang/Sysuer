package com.miyuyan.sysuer.academic

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.alibaba.fastjson2.JSONObject
import com.miyuyan.sysuer.model.JwxtModel
import com.miyuyan.sysuer.view.UiState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AcademyNotificationViewModel(application: Application) : AndroidViewModel(application) {
	private val model: JwxtModel = JwxtModel(application)

	private val _academicNotices = MutableStateFlow<List<JSONObject>>(emptyList())
	val academicNotices: StateFlow<List<JSONObject>> = _academicNotices

	private val _schoolNotices = MutableStateFlow<List<JSONObject>>(emptyList())
	val schoolNotices: StateFlow<List<JSONObject>> = _schoolNotices

	private val _noticeContent = MutableSharedFlow<String?>(extraBufferCapacity = 1)
	val noticeContent: SharedFlow<String?> = _noticeContent.asSharedFlow()

	private val _academicNoticesUiState = model.getUiState(0)
	val academicNoticesUiState: StateFlow<UiState> = _academicNoticesUiState.asStateFlow()
	private val _schoolNoticesUiState = model.getUiState(1)
	val schoolNoticesUiState: StateFlow<UiState> = _schoolNoticesUiState.asStateFlow()

	init {
		viewModelScope.launch {
			model.message.collect { (code, response) ->
				if (response.getInteger("code") == 200) {
					when (code) {
						0 -> {
							val list = response.getJSONObject("data").getJSONArray("list")
							_academicNoticesUiState.value =
								if (list.isEmpty()) UiState.Empty else UiState.Content
							_academicNotices.tryEmit(list.filterIsInstance<JSONObject>())
						}

						1 -> {
							val list = response.getJSONObject("data").getJSONArray("list")
							_schoolNoticesUiState.value =
								if (list.isEmpty()) UiState.Empty else UiState.Content
							_schoolNotices.tryEmit(list.filterIsInstance<JSONObject>())
						}

						2 -> {
							val data = response.getString("data")
							_noticeContent.tryEmit(data)
						}
					}
				}
			}
		}
	}

	fun fetchAcademicNotice(keyword: String = "") {
		_academicNoticesUiState.value = UiState.Loading
		model.enqueue(
				"jwxt/system-manage/info-delivery?column=01&deliveryObject=02&status=1&resourceCode=jwgld&title=$keyword",
				0
		)
	}

	fun fetchSchoolNotice(keyword: String = "") {
		_schoolNoticesUiState.value = UiState.Loading
		model.enqueue(
				"jwxt/system-manage/info-delivery?column=02&deliveryObject=02&status=1&resourceCode=jwgld&title=$keyword",
				1
		)
	}

	fun fetchContent(id: String) {
		model.enqueue("jwxt/system-manage/info-delivery/noticeId?id=$id", 2)
	}

	override fun onCleared() {
		model.dispose()
	}
}