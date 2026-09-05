package com.miyuyan.sysuer.academic

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.alibaba.fastjson2.JSONObject
import com.miyuyan.sysuer.model.XgxtModel

class PersonalInformationViewModel(application: Application) : AndroidViewModel(application) {
	private val model: XgxtModel = XgxtModel(application)

	private val _infoList = MutableLiveData<List<JSONObject>>(emptyList())
	val infoList: LiveData<List<JSONObject>> = _infoList

	init {
		model.message.observeForever { (_, response) ->
			if (response.containsKey("code") && response.getInteger("code") == 200) {
				_infoList.value = response.getJSONArray("data").filterIsInstance<JSONObject>()
			}
		}
	}

	fun fetchPersonalInfo() {
		model.addAndNext("xsxx/api/sm-xsxx/info/student/view", 0)
	}

	override fun onCleared() {
		model.dispose()
	}
}