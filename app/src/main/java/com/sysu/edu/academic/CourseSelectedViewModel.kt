package com.sysu.edu.academic

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.alibaba.fastjson2.JSONObject
import com.sysu.edu.model.JwxtModel

class CourseSelectedViewModel(application: Application) : AndroidViewModel(application) {
	private val model: JwxtModel = JwxtModel(application)
	private val _courseList = MutableLiveData<List<JSONObject>>(emptyList())
	val courseList: LiveData<List<JSONObject>> = _courseList
	var courseName: String = ""
	private var page = 0
	private var total = -1

	init {
		model.message.observeForever { (_, response) ->
			if (response.getInteger("code") == 200) {
				val data = response.getJSONObject("data")
				if (total == -1) total = data.getInteger("total")
				val newRows = data.getJSONArray("rows").filterIsInstance<JSONObject>()
				_courseList.value = _courseList.value!! + newRows
			}
		}
	}

	fun fetchCourseList() {
		model.addAndNext("jwxt/choose-course-front-server/selectedCourse/list",
			"""{"pageNo":${++page},"pageSize":10,"total":true,"param":{"courseName":"$courseName","successStatus":"1","failureStatus":"0","retiredClass":"0","waitingScreen":"0"}}""",
			1)
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