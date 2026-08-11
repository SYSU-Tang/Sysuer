package com.sysu.edu.academic

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.alibaba.fastjson2.JSONObject
import com.sysu.edu.model.JwxtModel

class PersonalTrainingProgramViewModel(application: Application) : AndroidViewModel(application) {
	private val model = JwxtModel(application)
	private val _basicInfo = MutableLiveData<JSONObject?>()
	val basicInfo: LiveData<JSONObject?> = _basicInfo
	private val _courseTable = MutableLiveData<JSONObject?>()
	val courseTable: LiveData<JSONObject?> = _courseTable
	private val _creditList = MutableLiveData<List<JSONObject>>(emptyList())
	val creditList: LiveData<List<JSONObject>> = _creditList
	private var programId: String = ""
	
	init {
		model.message.observeForever { (code, response) ->
			if (response.getInteger("code") == 200) {
				when (code) {
					0 -> {
						val data = response.getJSONArray("data").getJSONObject(0)
						programId = data.getString("TEACHPLANNUMBER") ?: ""
						println("programId: $programId")
						fetchBasicInfo()
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
	
	fun fetchMyProgram() {
		model.addAndNext("jwxt/training-programe/training-programe/undergradute/student/personalMainProgram", 0)
	}
	
	private fun fetchBasicInfo() {
		model.addAndNext("jwxt/training-programe/trainingBasicInfo/getBasicInformation?id=$programId", 2)
	}
	
	private fun fetchCourseTable() {
		model.addAndNext("jwxt/training-programe/schemeSubmitAgg/getTableByProgramId?programId=$programId", 1)
	}
	
	private fun fetchCredit() {
		model.addAndNext("jwxt/training-programe/trainingReqGraduate/showReqGraduateCreits?grade&cultivateId=$programId&cultivateCategoryId=01", 3)
	}
	
	override fun onCleared() {
		model.dispose()
	}
}