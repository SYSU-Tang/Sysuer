package com.miyuyan.sysuer.academic

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.alibaba.fastjson2.JSONObject
import com.miyuyan.sysuer.model.JwxtModel
import com.miyuyan.sysuer.view.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AcademyNotificationViewModel(application: Application) : AndroidViewModel(application) {
	private val model: JwxtModel = JwxtModel(application)

	private val _academicNotices = MutableStateFlow<List<JSONObject>>(emptyList())
	val academicNotices: StateFlow<List<JSONObject>> = _academicNotices

	private val _schoolNotices = MutableStateFlow<List<JSONObject>>(emptyList())
	val schoolNotices: StateFlow<List<JSONObject>> = _schoolNotices

	private val _noticeContent = MutableStateFlow<String?>(null)
	val noticeContent: StateFlow<String?> = _noticeContent

	val academicNoticesUiState = model.getUiState(0)
	val schoolNoticesUiState = model.getUiState(1)

	init {
		viewModelScope.launch {
			model.messageChannel.collect { (code, response) ->
				if (response.getInteger("code") == 200) {
					when (code) {
						0 -> {
							val list = response.getJSONObject("data").getJSONArray("list")
							academicNoticesUiState.value =
								if (list.isEmpty()) UiState.Empty else UiState.Content
							_academicNotices.emit(list.filterIsInstance<JSONObject>())
						}

						1 -> {
							val list = response.getJSONObject("data").getJSONArray("list")
							schoolNoticesUiState.value =
								if (list.isEmpty()) UiState.Empty else UiState.Content
							_schoolNotices.emit(list.filterIsInstance<JSONObject>())
						}

						2 -> {
							val data = response.getString("data")
							_noticeContent.emit(data)
						}
					}
				}
			}
		}
	}

	fun fetchNotices() {
		fetchAcademicNotice()
		fetchSchoolNotice()
	}

	fun fetchAcademicNotice(keyword: String? = null) {
		model.enqueue(
				"jwxt/system-manage/info-delivery?column=01&deliveryObject=02&status=1&resourceCode=jwgld&title=$keyword",
				0
		)
	}

	fun fetchSchoolNotice(keyword: String? = null) {
		schoolNoticesUiState.value = UiState.Loading
		model.enqueue(
				"jwxt/system-manage/info-delivery?column=02&deliveryObject=02&status=1&resourceCode=jwgld&title=$keyword",
				1
		)
	}

	fun fetchContent(id: String) {
		academicNoticesUiState.value = UiState.Loading
		model.enqueue("jwxt/system-manage/info-delivery/noticeId?id=$id", 2)
	}

	fun clearNoticeContent() {
		_noticeContent.value = null
	}

	override fun onCleared() {
		model.dispose()
	}
}