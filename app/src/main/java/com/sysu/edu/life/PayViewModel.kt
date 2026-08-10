package com.sysu.edu.life

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.alibaba.fastjson2.JSONObject
import com.sysu.edu.model.PayModel

class PayViewModel(application: Application) : AndroidViewModel(application) {
	private val model: PayModel = PayModel(application)

	private val _toPayList = MutableLiveData<List<JSONObject>>(emptyList())
	val toPayList: LiveData<List<JSONObject>> = _toPayList

	private val _selectivePayList = MutableLiveData<List<JSONObject>>(emptyList())
	val selectivePayList: LiveData<List<JSONObject>> = _selectivePayList

	private val _feeList = MutableLiveData<List<JSONObject>>(emptyList())
	val feeList: LiveData<List<JSONObject>> = _feeList

	private val _paymentList = MutableLiveData<List<JSONObject>>(emptyList())
	val paymentList: LiveData<List<JSONObject>> = _paymentList

	private val _refundList = MutableLiveData<List<JSONObject>>(emptyList())
	val refundList: LiveData<List<JSONObject>> = _refundList

	init {
		model.message.observeForever { (code, response) ->
			if (response.getInteger("code") == 200 && response.get("data") != null) {
				val data = response.getJSONArray("data").filterIsInstance<JSONObject>()
				when (code) {
					0 -> _toPayList.value = data
					1 -> _selectivePayList.value = data
					2 -> _feeList.value = data
					3 -> _paymentList.value = data
					4 -> _refundList.value = data
				}
			}
		}
	}

	fun fetchToPayList() {
		model.addAndNext("client/api/client/necessary/list", "{}", 0)
	}

	fun fetchSelectivePayList() {
		model.addAndNext("client/api/client/chooce/list", "{}", 1)
	}

	fun fetchFeeList(year: String) {
		model.addAndNext("client/api/client/record/feelist", "{\"year\":$year}", 2)
	}

	fun fetchPaymentList(from: String, to: String?) {
		model.addAndNext("client/api/client/record/paymentlist",
			"{\"startTime\":\"$from\",\"overTime\":\"$to\"}", 3)
	}

	fun fetchRefundList() {
		model.addAndNext("client/api/client/refund/list", "{}", 4)
	}

	override fun onCleared() {
		model.dispose()
	}
}