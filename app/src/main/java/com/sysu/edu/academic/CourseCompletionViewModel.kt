package com.sysu.edu.academic

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.alibaba.fastjson2.JSONObject
import com.sysu.edu.model.JwxtModel

class CourseCompletionViewModel(application: Application) : AndroidViewModel(application) {
	private val model = JwxtModel(application)
	private val _creditHours = MutableLiveData<List<JSONObject>>(emptyList())
	val creditHours: LiveData<List<JSONObject>> = _creditHours
	private val _courseList = MutableLiveData<List<JSONObject>>(emptyList())
	val courseList: LiveData<List<JSONObject>> = _courseList
	private var page = 0
	private var total = -1
	
	init {
		model.message.observeForever { (code, response) ->
			if (response.getInteger("code") == 200 && response.get("data") != null) {
				when (code) {
					0 -> {
						_creditHours.value = response.getJSONArray("data").filterIsInstance<JSONObject>()
					}
					1 -> {
						val data = response.getJSONObject("data")
						if (total == -1) total = data.getInteger("total")
						val newRows = data.getJSONArray("rows").filterIsInstance<JSONObject>()
						_courseList.value = _courseList.value!! + newRows
					}
				}
			}
		}
	}
	
	fun fetchCreditHours() {
		model.addAndNext("jwxt/gradua-degree/graduatemsg/studentsGraduationExamination/creditHoursStu?cultureTypeCode=01", "", 0)
	}
	
	fun fetchCourseList() {
		model.addAndNext("jwxt/gradua-degree/graduatemsg/studentsGraduationExamination/studentCourse", """{"pageNo":${++page},"pageSize":10,"total":true,"param":{"cultureTypeCode":"01"}}""", 1)
	}
	
	fun reFetchCourseList() {
		_courseList.value = emptyList()
		page = 0
		total = -1
		fetchCourseList()
	}
	
	fun hasMore(): Boolean = page * 10 < total
	override fun onCleared() {
		model.dispose()
	}
}