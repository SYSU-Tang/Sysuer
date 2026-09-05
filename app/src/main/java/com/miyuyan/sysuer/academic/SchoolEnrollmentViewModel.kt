package com.miyuyan.sysuer.academic

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.alibaba.fastjson2.JSONObject
import com.miyuyan.sysuer.model.JwxtModel

class SchoolEnrollmentViewModel(application: Application) : AndroidViewModel(application) {
	private val model: JwxtModel = JwxtModel(application)
	private val _basicInfo = MutableLiveData<JSONObject?>()
	val basicInfo: LiveData<JSONObject?> = _basicInfo
	private val _familyList = MutableLiveData<List<JSONObject>>(emptyList())
	val familyList: LiveData<List<JSONObject>> = _familyList
	private val _experienceList = MutableLiveData<List<JSONObject>>(emptyList())
	val experienceList: LiveData<List<JSONObject>> = _experienceList
	private val _exchangeList = MutableLiveData<List<JSONObject>>(emptyList())
	val exchangeList: LiveData<List<JSONObject>> = _exchangeList
	private val _changeList = MutableLiveData<List<JSONObject>>(emptyList())
	val changeList: LiveData<List<JSONObject>> = _changeList
	private val _minorList = MutableLiveData<List<JSONObject>>(emptyList())
	val minorList: LiveData<List<JSONObject>> = _minorList
	private val _registerList = MutableLiveData<List<JSONObject>>(emptyList())
	val registerList: LiveData<List<JSONObject>> = _registerList
	private val _punishList = MutableLiveData<List<JSONObject>>(emptyList())
	val punishList: LiveData<List<JSONObject>> = _punishList
	private val pages = IntArray(8)
	private val totals = IntArray(8)
	
	init {
		model.message.observeForever { (code, response) ->
			if (response.getInteger("code") == 200) {
				val data = response.getJSONObject("data") ?: return@observeForever
				when (code) {
					0 -> {
						_basicInfo.value = data
						fetchFamily()
					}
					1 -> appendRows(data, _familyList, 1)
					2 -> appendRows(data, _experienceList, 2)
					3 -> appendRows(data, _exchangeList, 3)
					4 -> appendRows(data, _changeList, 4)
					5 -> appendRows(data, _minorList, 5)
					6 -> appendRows(data, _registerList, 6)
					7 -> appendRows(data, _punishList, 7)
				}
			}
		}
	}
	
	private fun appendRows(
		data: JSONObject,
		liveData: MutableLiveData<List<JSONObject>>,
		tab: Int,
	                      ) {
		totals[tab] = data.getInteger("total")
		val newRows = data.getJSONArray("rows").filterIsInstance<JSONObject>()
		liveData.value = liveData.value?.plus(newRows)
		if ((liveData.value?.size ?: 0) < totals[tab]) fetchTab(tab)
		else if (tab < 7) fetchTab(tab + 1)
	}
	
	fun fetchBasicInfo() {
		model.addAndNext("jwxt/student-status/countrystu/studentRollView", 0)
	}
	
	fun fetchFamily() {
		fetchPaginated("jwxt/student-status/stuFamily/showStudentFamily", 1)
	}
	
	fun fetchExperience() {
		fetchPaginated("jwxt/student-status/stuExperience/showStudentExperience", 2)
	}
	
	fun fetchExchange() {
		fetchPaginated("jwxt/student-status/abroadInformation/myStulistInformation", 3)
	}
	
	fun fetchChange() {
		fetchPaginated("jwxt/student-status-move/moveStuAgg/showStuChangeRoll", 4)
	}
	
	fun fetchMinor() {
		fetchPaginated("jwxt/minor-status/minDouDegMajRoll/queryMinDouDegMajRoll", 5)
	}
	
	fun fetchRegister() {
		fetchPaginated("jwxt/reports-register/stuRegistration/getSelfRegisterList", 6)
	}
	
	fun fetchPunish() {
		fetchPaginated("jwxt/student-status/stuRewPunish/showMyStudentRewPunish", 7)
	}
	
	private fun fetchPaginated(url: String, code: Int) {
		model.addAndNext(url, "{\"pageNo\":${++pages[code]},\"pageSize\":10,\"total\":true,\"param\":{}}", code)
	}
	
	private fun fetchTab(tab: Int) {
		when (tab) {
			1 -> fetchFamily()
			2 -> fetchExperience()
			3 -> fetchExchange()
			4 -> fetchChange()
			5 -> fetchMinor()
			6 -> fetchRegister()
			7 -> fetchPunish()
		}
	}
	
	override fun onCleared() {
		model.dispose()
	}
}