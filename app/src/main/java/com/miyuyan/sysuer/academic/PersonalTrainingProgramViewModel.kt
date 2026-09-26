package com.miyuyan.sysuer.academic

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.alibaba.fastjson2.JSONObject
import com.miyuyan.sysuer.model.JwxtModel
import kotlinx.coroutines.launch

class PersonalTrainingProgramViewModel(application: Application) : AndroidViewModel(application) {
	private val model = JwxtModel(application)
	private val _basicInfo = MutableLiveData<JSONObject?>()
	val basicInfo: LiveData<JSONObject?> = _basicInfo
	private val _courseTable = MutableLiveData<JSONObject?>()
	val courseTable: LiveData<JSONObject?> = _courseTable
	private val _creditList = MutableLiveData<List<JSONObject>>(emptyList())
	val creditList: LiveData<List<JSONObject>> = _creditList
	var programId: String? by mutableStateOf(null)
	
	init {
		viewModelScope.launch {
			model.messageChannel.collect { (code, response) ->
				if (response.getInteger("code") == 200) {
					when (code) {
						0 -> {
							val data = response.getJSONArray("data").getJSONObject(0)
							programId = data.getString("TEACHPLANNUMBER") ?: ""
						}
						1 -> _courseTable.value = response.getJSONObject("data")
						2 -> {
							_basicInfo.value = response.getJSONObject("data")
							fetchCredit()
						}
						3 -> {
							_creditList.value = response.getJSONArray("data").filterIsInstance<JSONObject>()
							fetchCourseTable()
						}
					}
				}
			}
		}
	}
	
	fun fetchMyProgram() {
		model.enqueue("jwxt/training-programe/training-programe/undergradute/student/personalMainProgram", 0)
	}
	
	fun fetchBasicInfo() {
		model.enqueue("jwxt/training-programe/trainingBasicInfo/getBasicInformation?id=$programId", 2)
	}
	
	private fun fetchCourseTable() {
		model.enqueue("jwxt/training-programe/schemeSubmitAgg/getTableByProgramId?programId=$programId", 1)
	}
	
	private fun fetchCredit() {
		model.enqueue("jwxt/training-programe/trainingReqGraduate/showReqGraduateCreits?grade&cultivateId=$programId&cultivateCategoryId=01", 3)
	}
	
	override fun onCleared() {
		model.dispose()
	}
}