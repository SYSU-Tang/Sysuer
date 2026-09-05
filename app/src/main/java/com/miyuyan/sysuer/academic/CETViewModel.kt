package com.miyuyan.sysuer.academic

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.alibaba.fastjson2.JSONObject
import com.miyuyan.sysuer.model.JwxtModel

class CETViewModel(application: Application): AndroidViewModel(application) {
	private val model: JwxtModel = JwxtModel(application)
	
	private val _scores = MutableLiveData<List<JSONObject>>(emptyList())
	val scores: LiveData<List<JSONObject>> = _scores
	
	private val _isLoading = MutableLiveData(false)
	private var page = 0
	private var total = 0
	
	init {
		model.message.observeForever { (code, response) ->
			if (response.getInteger("code") == 200 && code == 0) {
				val data = response.getJSONObject("data")
				if (data != null) {
					total = data.getInteger("total")
					val newRows = data.getJSONArray("rows").filterIsInstance<JSONObject>()
					_scores.value = _scores.value?.plus(newRows)
				}
				_isLoading.value = false
			}
		}
	}
	
	fun fetchNextPage() {
		if (_isLoading.value == true) return
		if (page > 0 && (_scores.value?.size ?: 0) >= total) return
		
		_isLoading.value = true
		model.add("jwxt/achievement-manage/englishGradeAchievement/stuPageList",
		          "{\"pageNo\":${++page},\"pageSize\":10,\"total\":true,\"param\":{}}", 0)
		model.next()
	}
	
	override fun onCleared() {
		model.dispose()
	}
}
