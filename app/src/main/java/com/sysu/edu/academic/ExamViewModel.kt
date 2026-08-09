package com.sysu.edu.academic

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.alibaba.fastjson2.JSONArray
import com.alibaba.fastjson2.JSONObject
import com.sysu.edu.model.JwxtModel

class ExamViewModel(application: Application) : AndroidViewModel(application) {
	private val model = JwxtModel(application)
	val termList: MutableLiveData<JSONArray?> = MutableLiveData<JSONArray?>()
	val examWeekList: MutableLiveData<JSONArray?> = MutableLiveData<JSONArray?>()
	val examResult: MutableLiveData<JSONArray?> = MutableLiveData<JSONArray?>()
	val term: MutableLiveData<String?> = MutableLiveData<String?>()
	val examWeek: MutableLiveData<String?> = MutableLiveData<String?>()
	val examWeekId: MutableLiveData<String?> = MutableLiveData<String?>()
	
	init {
		model.message.observeForever { (code, response) ->
			if (response.getInteger("code") == 200) {
				when (code) {
					1 -> {
						termList.value = response.getJSONArray("data")
						getTerm()
					}
					2 -> {
						term.value = response.getJSONObject("data").getString("acadYearSemester")
						getExamWeek(term.value)
					}
					3 -> { //						val examWeeks = mutableListOf<String?>()
						//						val examWeekInfo = mutableListOf<JSONObject?>()
						//						response.getJSONArray("data").forEach { item: Any? ->
						//							examWeeks.add((item as JSONObject).getString("examWeekName"))
						//							examWeekInfo.add(item)
						//						}
						examWeekList.value = response.getJSONArray("data") // examWeekList.value = examWeeks
						//binding.examWeek.setText(response.getJSONObject("data").getString("examWeekName"),false);
					}
					4 -> {
						examResult.value = response.getJSONArray("data")
					}
				}
			}
		}
	}
	
	fun getTerms() {
		model.addAndNext("jwxt/base-info/acadyearterm/findAcadyeartermNamesBox", 1)
	}
	
	fun getTerm() {
		model.addAndNext("jwxt/base-info/acadyearterm/showNewAcadlist", 2)
	}
	
	fun getExamWeek(term: String?) {
		model.addAndNext("jwxt/schedule/agg/commonScheduleExamTime/queryExamWeekName?yearTerm=$term", 3)
	}
	
	fun getResult() {
		val data = JSONObject()
		if (term.value != null) data["acadYear"] = term.value
		if (examWeekId.value != null) data["examWeekName"] = examWeekId.value
		model.addAndNext("jwxt/examination-manage/classroomResource/queryStuEaxmInfo", "$data", 4)
	}
}
