package com.miyuyan.sysuer.academic

import android.app.Application
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.AndroidViewModel
import com.alibaba.fastjson2.JSONObject
import com.alibaba.fastjson2.JSONWriter
import com.miyuyan.sysuer.R
import com.miyuyan.sysuer.api.ContextUtil
import com.miyuyan.sysuer.model.JwxtModel
import kotlinx.coroutines.flow.MutableStateFlow

class CourseSelectionPreviewViewModel(application: Application) : AndroidViewModel(application) {
	private val model: JwxtModel = JwxtModel(application)
	private val _courses = mutableStateListOf<JSONObject>()
	val courses: List<JSONObject> = _courses
	var page: Int = 1
		private set
	var total: Int = -1
		private set
	var isLoading: MutableStateFlow<Boolean> = MutableStateFlow(false)
		private set
	var hiddenSelectedStatus: MutableStateFlow<Boolean> = MutableStateFlow(false)
		private set
	var type: MutableIntState = mutableIntStateOf(1)
		private set
	var filterName: CourseFilterNameData = CourseFilterNameData()
		private set
	var filterValue: CourseFilterValueData = CourseFilterValueData()
		private set
	
	init {
		model.message.observeForever { (code, response) ->
			if (response.getInteger("code") == 200) {
				when (code) {
					0 -> {
						val data = response.getJSONObject("data")
						total = data.getInteger("total")
						data.getJSONArray("rows").forEach { e ->
							_courses.add(e as JSONObject)
						}
						isLoading.value = false
					}
					1 -> {
						ContextUtil.getInstance(application).toast(response.getString("data", application.getString(R.string.action_success)))
						reload()
					}
				}
			}
		}
	}
	
	fun setHiddenSelectedStatus(newStatus: Boolean) {
		hiddenSelectedStatus.value = newStatus
		reload()
	}
	
	fun setType(newType: Int) {
		if (type.intValue == newType) return
		type.intValue = newType
		reload()
	}
	
	fun setFilterName(name: CourseFilterNameData) {
		filterName = name
	}
	
	fun setFilterValue(value: CourseFilterValueData) {
		filterValue = value
		reload()
	}
	
	fun reload() {
		page = 1
		total = -1
		_courses.clear()
		loadMore()
	}
	
	fun loadMore() {
		if (total != -1 && _courses.size >= total) return
		isLoading.value = true
		val data = JSONObject.of("pageNo", page++, "pageSize", 10, "param", JSONObject.of("hiddenSelectedStatus", if (hiddenSelectedStatus.value) "1" else "", "type", type.intValue))
		data.getJSONObject("param").putAll(JSONObject.from(filterValue, JSONWriter.Feature.FieldBased))
		model.addAndNext("jwxt/choose-course-front-server/schoolCourse/pageList", "$data", 0)
	}
	
	fun like(classesID: String) {
		model.addAndNext("jwxt/choose-course-front-server/stuCollectedCourse/create", "{\"classesID\":\"$classesID\",\"selectedType\":\"1\"}", 1)
	}
	
	override fun onCleared() {
		model.dispose()
	}
}