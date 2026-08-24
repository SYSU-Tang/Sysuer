package com.sysu.edu.academic

import android.app.Application
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.AndroidViewModel
import com.alibaba.fastjson2.JSONArray
import com.alibaba.fastjson2.JSONObject
import com.sysu.edu.R
import com.sysu.edu.api.CommonUtil.extractValue
import com.sysu.edu.model.JwxtModel
import com.sysu.edu.view.SectionData
import kotlinx.coroutines.flow.MutableStateFlow

class ExamViewModel(application: Application) : AndroidViewModel(application) {
	private val model = JwxtModel(application)
	val termList: MutableStateFlow<JSONArray?> = MutableStateFlow(null)
	val examWeekList: MutableStateFlow<JSONArray?> = MutableStateFlow(null)
	val term: MutableStateFlow<String?> = MutableStateFlow(null)
	val examWeek: MutableStateFlow<String?> = MutableStateFlow(null)
	val examWeekId: MutableStateFlow<String?> = MutableStateFlow(null)
	private val _sections = mutableStateListOf<SectionData>()
	val sections: SnapshotStateList<SectionData> = _sections
	
	init {
		model.message.observeForever { (code, response) ->
			if (response.getInteger("code") == 200) {
				when (code) {
					1 -> termList.value = response.getJSONArray("data")
					2 -> term.value = response.getJSONObject("data").getString("acadYearSemester")
					3 -> {
						examWeekList.value = response.getJSONArray("data")
						val finalExamWeek = response.getJSONArray("data").firstOrNull { (it as JSONObject).getString("examWeekName") == "18-19周期末考" } as? JSONObject
						examWeekId.value = finalExamWeek?.getString("examWeekId")
						examWeek.value = finalExamWeek?.getString("examWeekName")
					}
					4 -> {
						_sections.clear()
						response.getJSONArray("data")?.forEach { a: Any? ->
							val timeTable = (a as JSONObject).getJSONObject("timetable")
							timeTable.keys.sorted().forEach {
								timeTable[it]?.let { item ->
									(item as JSONArray).forEach { o: Any? ->
										_sections.add(SectionData((o as JSONObject).getString("examSubjectName"),
										                          rows = extractValue(application,
										                                              o,
										                                              intArrayOf(R.string.subject, R.string.exam_classroom, R.string.duration, R.string.date, R.string.year),
										                                              arrayOf("examSubjectName", "classroomNumber", "durationTime", "examDate", "acadYear"))))
									}
								}
							}
						}
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
