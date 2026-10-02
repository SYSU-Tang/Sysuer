package com.miyuyan.sysuer.extra

import android.app.Application
import androidx.core.util.component1
import androidx.core.util.component2
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.alibaba.fastjson2.JSONObject
import com.miyuyan.sysuer.model.PayModel
import com.miyuyan.sysuer.view.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PrivacyViewModel(application: Application) : AndroidViewModel(application) {
	val model: PayModel = PayModel(application)

	private val _netId = MutableStateFlow("")
	val netId: StateFlow<String> = _netId.asStateFlow()

	private val _password = MutableStateFlow("")
	val password: StateFlow<String> = _password.asStateFlow()

	private val _personData = MutableStateFlow<JSONObject?>(null)
	val personData: StateFlow<JSONObject?> = _personData.asStateFlow()

	private val _uiState = model.getUiState(PERSON_REQUEST)
	val uiState: StateFlow<UiState> = _uiState.asStateFlow()

	init {
		model.contextUtil.disposable.add(
				model.contextUtil.accountManager.getActiveAccountAsync("sysu.edu.cn")
					.subscribe { (id, pwd) ->
						_netId.value = id ?: ""
						_password.value = pwd ?: ""
					})

		viewModelScope.launch {
			model.message.collect { (code, response) ->
				if (response.getInteger("code") == 200) {
					if (response.get("data") != null) {
						if (code == PERSON_REQUEST) {
							_personData.value = response.getJSONObject("data")
							_uiState.value = UiState.Content
						}
					}
				}
			}
		}

		fetchPersonData()
	}

	fun fetchPersonData() {
		_uiState.value = UiState.Loading
		model.enqueue("client/api/client/person/get", "{}", PERSON_REQUEST)
	}

	override fun onCleared() {
		model.dispose()
	}

	companion object {
		private const val PERSON_REQUEST = 0
	}
}