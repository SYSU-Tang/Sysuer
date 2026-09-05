package com.miyuyan.sysuer.academic

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.alibaba.fastjson2.JSONObject
import com.miyuyan.sysuer.model.XgxtModel

class DormViewModel(application: Application) : AndroidViewModel(application) {
	private val model: XgxtModel = XgxtModel(application)
	
	private val _dormInfo = MutableLiveData<JSONObject>()
	val dormInfo: MutableLiveData<JSONObject> = _dormInfo
	
	init {
		model.message.observeForever { (code, data) ->
			if (data.containsKey("code") && data.getInteger("code") == 200) {
				_dormInfo.value = data.getJSONObject("data")
			}
		}
	}
	
	fun fetchDormInfo() {
		model.addAndNext("ssgl/api/sm-ssgl/stu-info", 0)
	}
	
	override fun onCleared() {
		model.dispose()
	}
}