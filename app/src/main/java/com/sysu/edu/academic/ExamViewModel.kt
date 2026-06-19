package com.sysu.edu.academic

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.alibaba.fastjson2.JSONObject

class ExamViewModel : ViewModel() {
	val termList: MutableLiveData<ArrayList<String?>?> = MutableLiveData<ArrayList<String?>?>()
	val term: MutableLiveData<String?> = MutableLiveData<String?>()
	val examWeekList: MutableLiveData<ArrayList<String?>?> = MutableLiveData<ArrayList<String?>?>()
	val examWeekInfo: MutableLiveData<ArrayList<JSONObject?>?> = MutableLiveData<ArrayList<JSONObject?>?>()
	val examWeek: MutableLiveData<String?> = MutableLiveData<String?>()
	val examResult: MutableLiveData<String?> = MutableLiveData<String?>()
	val examWeekId: MutableLiveData<String?> = MutableLiveData<String?>()
	fun setTermList(terms: ArrayList<String?>?) {
		termList.value = terms
	}
	
	fun setTerm(term: String?) {
		this.term.value = term
	}
	
	fun setExamWeekList(examWeekList: ArrayList<String?>?) {
		this.examWeekList.value = examWeekList
	}
	
	fun setExamWeek(examWeek: String?) {
		this.examWeek.value = examWeek
	}
	
	fun setExamResult(examResult: String?) {
		this.examResult.value = examResult
	}
	
	fun setExamWeekId(examWeekId: String?) {
		this.examWeekId.value = examWeekId
	}
	
	fun setExamWeekInfo(examWeekInfo: ArrayList<JSONObject?>?) {
		this.examWeekInfo.value = examWeekInfo
	}
}
