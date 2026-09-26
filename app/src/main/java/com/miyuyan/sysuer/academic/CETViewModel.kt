package com.miyuyan.sysuer.academic

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.alibaba.fastjson2.JSONObject
import com.miyuyan.sysuer.model.JwxtModel
import com.miyuyan.sysuer.view.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CETViewModel(application: Application) : AndroidViewModel(application) {
	private val model: JwxtModel = JwxtModel(application)

	private val _scores = MutableStateFlow<List<JSONObject>>(emptyList())
	val scores: StateFlow<List<JSONObject>> = _scores.asStateFlow()
	private val _uiState = model.getUiState(0)
	val uiState: StateFlow<UiState> = _uiState.asStateFlow()

	private var page = 0
	private var total = 0

	init {
		viewModelScope.launch {
			model.message.collect { (code, response) ->
				if (response.getInteger("code") == 200 && code == 0) {
					val data = response.getJSONObject("data")
					if (data != null) {
						total = data.getInteger("total")
						val newRows = data.getJSONArray("rows").filterIsInstance<JSONObject>()
						_scores.value = _scores.value.plus(newRows)
						_uiState.value = if (total == 0) UiState.Empty else UiState.Content
					} else {
						_uiState.value = UiState.Empty
					}
				} else {
					_uiState.value = UiState.Error
				}
			}
		}
	}

	fun fetchNextPage() {
		if (_uiState.value == UiState.Loading || (page > 0 && (_scores.value.size >= total))) return
		_uiState.value = if (page == 0) UiState.Loading else UiState.LoadMore
		model.enqueue(
			"jwxt/achievement-manage/englishGradeAchievement/stuPageList",
			"{\"pageNo\":${++page},\"pageSize\":10,\"total\":true,\"param\":{}}",
			0
		)
	}

	override fun onCleared() {
		model.dispose()
	}
}