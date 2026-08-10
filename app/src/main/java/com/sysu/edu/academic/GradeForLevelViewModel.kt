package com.sysu.edu.academic

import android.app.Application
import androidx.core.text.isDigitsOnly
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.alibaba.fastjson2.JSONObject
import com.sysu.edu.api.CommonUtil.isEmpty
import com.sysu.edu.model.JwxtModel

class GradeForLevelViewModel(application: Application) : AndroidViewModel(application) {
	private val model: JwxtModel = JwxtModel(application)
	private val _gradeList = MutableLiveData<List<JSONObject>>(emptyList())
	val gradeList: LiveData<List<JSONObject>> = _gradeList
	private val _trainTypeOptions = MutableLiveData<List<JSONObject>>(emptyList())
	val trainTypeOptions: LiveData<List<JSONObject>> = _trainTypeOptions
	private val _yearOptions = MutableLiveData<List<JSONObject>>(emptyList())
	val yearOptions: LiveData<List<JSONObject>> = _yearOptions
	private val _courseTypeOptions = MutableLiveData<List<JSONObject>>(emptyList())
	val courseTypeOptions: LiveData<List<JSONObject>> = _courseTypeOptions
	var trainType: String? = null
	var year: String? = null
	var courseType: String? = null
	var courseName: String? = null
	var courseNumber: String? = null
	var minGrade: String? = null
	private var page = 1
	private var total = -1
	
	init {
		model.message.observeForever { (code, response) ->
			if (response.getInteger("code") == 200) {
				when (code) {
					3 -> {
						if (total == -1) total = response.getJSONObject("data").getInteger("total")
						val rows = response.getJSONObject("data").getJSONArray("rows").filterIsInstance<JSONObject>()
						_gradeList.value = _gradeList.value!! + rows
					}
					0 -> _trainTypeOptions.value = response.getJSONArray("data").filterIsInstance<JSONObject>()
					1 -> _yearOptions.value = response.getJSONArray("data").filterIsInstance<JSONObject>()
					2 -> _courseTypeOptions.value = response.getJSONArray("data").filterIsInstance<JSONObject>()
				}
				model.nextAll()
			}
		}
	}
	
	fun fetchOptions() {
		model.add("jwxt/base-info/codedata/findcodedataNames?datableNumber=97", 0)
		model.add("jwxt/base-info/acadyearterm/findAcadyeartermNamesBox", 1)
		model.add("jwxt/base-info/base-category/SfqyBox", 2)
		model.nextAll()
	}
	
	fun fetchGrade() {
		model.addAndNext("jwxt/achievement-manage/achievement/selfPageList", "{\"pageNo\":${page++},\"pageSize\":10,\"total\":true,\"param\":$args}", 3)
	}
	
	fun reFetchGrade() {
		_gradeList.value = emptyList()
		page = 1
		total = -1
		fetchGrade()
	}
	
	fun hasMore(): Boolean = (page - 1) * 10 < total
	private val args: JSONObject
		get() {
			val args = JSONObject()
			if (!isEmpty(trainType)) args["categoryCode"] = trainType
			if (!isEmpty(year)) args["schoolSemester"] = year
			if (!isEmpty(courseType)) args["courseTypeCode"] = courseType
			if (!isEmpty(courseName)) args["courseName"] = courseName
			if (!isEmpty(courseNumber)) args["courseNum"] = courseNumber
			if (!isEmpty(minGrade) && minGrade?.isDigitsOnly() == true) args["finalAchievement"] = minGrade?.toInt()
			println(args)
			return args
		}
	
	override fun onCleared() {
		model.dispose()
	}
}