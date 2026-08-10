package com.sysu.edu.academic

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.alibaba.fastjson2.JSONObject
import com.sysu.edu.model.JwxtModel

class RegistrationViewModel(application: Application) : AndroidViewModel(application) {
	private val model: JwxtModel = JwxtModel(application)

	private val _registerInfo = MutableLiveData<JSONObject?>()
	val registerInfo: LiveData<JSONObject?> = _registerInfo

	private val _payList = MutableLiveData<List<JSONObject>>(emptyList())
	val payList: LiveData<List<JSONObject>> = _payList

	private val _historyList = MutableLiveData<List<JSONObject>>(emptyList())
	val historyList: LiveData<List<JSONObject>> = _historyList

	private var historyPage = 0
	private var historyTotal = 0

	private var currentYear = "2025"

	init {
		model.message.observeForever { (code, response) ->
			if (response.getInteger("code") == 200 && response.get("data") != null) {
				when (code) {
					0 -> _registerInfo.value = response.getJSONObject("data")
					1 -> _payList.value = response.getJSONArray("data").filterIsInstance<JSONObject>()
					2 -> {
						val data = response.getJSONObject("data")
						historyTotal = data.getInteger("total")
						val newRows = data.getJSONArray("rows").filterIsInstance<JSONObject>()
						_historyList.value = _historyList.value?.plus(newRows)
					}
				}
			}
		}
	}

	fun fetchRegisterInfo() {
		model.addAndNext("jwxt/reports-register/stuRegistration/getSelfRegisterInfo", 0)
	}

	fun fetchPayInfo(year: String = currentYear) {
		currentYear = year
		model.addAndNext("jwxt/reports-register/stuRegistration/getSelfPayInfoDetail?acadYear=$year", 1)
	}

	fun fetchHistoryNextPage() {
		model.addAndNext("jwxt/reports-register/stuRegistration/getSelfRegisterList",
			"{\"pageNo\":${++historyPage},\"pageSize\":10,\"total\":true,\"param\":{}}", 2)
	}

	fun hasMoreHistory(): Boolean = (_historyList.value?.size ?: 0) < historyTotal

	override fun onCleared() {
		model.dispose()
	}
}