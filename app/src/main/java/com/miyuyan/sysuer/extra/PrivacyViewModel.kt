package com.miyuyan.sysuer.extra

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.alibaba.fastjson2.JSONObject
import com.miyuyan.sysuer.api.TargetHost
import com.miyuyan.sysuer.model.PayModel
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

	init {
		viewModelScope.launch {
			val (id, pwd) = model.contextUtil.accountManager.getActiveAccount(TargetHost.SYSU)
			_netId.value = id
			_password.value = pwd
		}

		viewModelScope.launch {
			model.message.collect { (code, response) ->
				if (response.getInteger("code") == 200) {
					if (response.get("data") != null) {
						if (code == PERSON_REQUEST) {
							_personData.value = response.getJSONObject("data")
						}
					}
				}
			}
		}

		fetchPersonData()
	}

	fun fetchPersonData() {
		model.enqueue("client/api/client/person/get", "{}", PERSON_REQUEST)
	}

	override fun onCleared() {
		model.dispose()
	}

	companion object {
		private const val PERSON_REQUEST = 0
	}
}