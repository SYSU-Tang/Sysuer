package com.sysu.edu.academic

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.AndroidViewModel
import com.alibaba.fastjson2.JSONObject
import com.sysu.edu.R
import com.sysu.edu.api.CommonUtil.extractValue
import com.sysu.edu.model.JwxtModel
import com.sysu.edu.view.MenuItem
import com.sysu.edu.view.SectionData

class TrainingProgramViewModel(application: Application) : AndroidViewModel(application) {
	private val model = JwxtModel(application)
	var collegeNames: List<String> by mutableStateOf(listOf())
		private set
	var collegeIds: List<String> by mutableStateOf(listOf())
		private set
	var gradeNames: List<String> by mutableStateOf(listOf())
		private set
	var gradeIds: List<String> by mutableStateOf(listOf())
		private set
	var professionNames: List<String> by mutableStateOf(listOf())
		private set
	var professionIds: List<String> by mutableStateOf(listOf())
		private set
	var typeNames: List<String> by mutableStateOf(listOf())
		private set
	var typeIds: List<String> by mutableStateOf(listOf())
		private set
	var selectedCollegeId: String? by mutableStateOf(null)
	var selectedGradeId: String? by mutableStateOf(null)
	var selectedProfessionId: String? by mutableStateOf(null)
	var selectedTypeId: String? by mutableStateOf(null)
	var selectedCollegeName: String? by mutableStateOf("")
	var selectedProfessionName: String? by mutableStateOf("")
	var selectedGradeIndex: Int by mutableIntStateOf(0)
	var showResults: Boolean by mutableStateOf(false)
		private set
	var viewDetailProgramId: String? by mutableStateOf(null)
		internal set
	val resultSections: SnapshotStateList<SectionData> = mutableStateListOf()
	private var resultPage = 0
	private var resultTotal = -1
	
	init {
		model.message.observeForever { (code, response) ->
			if (response.getInteger("code") == 200) {
				when (code) {
					1 -> {
						val names = mutableListOf<String>()
						val ids = mutableListOf<String>()
						response.getJSONArray("data").forEach { e: Any? ->
							ids.add((e as JSONObject).getString("departmentNumber"))
							names.add(e.getString("departmentName"))
						}
						collegeNames = names
						collegeIds = ids
					}
					2 -> {
						val names = mutableListOf<String>()
						val ids = mutableListOf<String>()
						response.getJSONArray("data").forEach { e: Any? ->
							ids.add((e as JSONObject).getString("dataNumber"))
							names.add(e.getString("dataName"))
						}
						gradeNames = names
						gradeIds = ids
						if (ids.isNotEmpty()) {
							selectedGradeId = ids.last()
							selectedGradeIndex = names.size - 1
						}
					}
					3 -> {
						val names = mutableListOf<String>()
						val ids = mutableListOf<String>()
						response.getJSONArray("data").forEach { e: Any? ->
							ids.add((e as JSONObject).getString("dataNumber"))
							names.add(e.getString("dataName"))
						}
						typeNames = names
						typeIds = ids
						if (ids.isNotEmpty()) {
							selectedTypeId = ids[0]
						}
					}
					4 -> {
						val names = mutableListOf<String>()
						val ids = mutableListOf<String>()
						response.getJSONArray("data").forEach { e: Any? ->
							ids.add((e as JSONObject).getString("code"))
							names.add(e.getString("name"))
						}
						professionNames = names
						professionIds = ids
					}
					5 -> {
						val responseData = response.getJSONObject("data")
						resultTotal = responseData.getInteger("total")
						responseData.getJSONArray("rows").forEach { o: Any? ->
							val obj = o as JSONObject
							val keys = intArrayOf(R.string.profession, R.string.grade, R.string.college, R.string.training_category, R.string.study_period, R.string.discipline_category, R.string.degree, R.string.profession_code, R.string.profession_id)
							resultSections.add(SectionData(title = obj.getString("name"),  rows = extractValue(getApplication(),
							                                                                                                                        obj, keys, arrayOf("professionName", "grade", "manageUnitName", "trainTypeName", "educationalSystem", "disciplineCateName", "degreeGrantName", "professionCode", "professionId")
																																	 ), footerMenus = mutableStateListOf(MenuItem(getApplication<Application>().getString(R.string.view_detail)){
																																		 viewDetailProgramId = o.getString("teachPlanNumber")
																																		 true
																																	 })))
						}
					}
				}
				model.nextAll()
			}
		}
		loadInitialData()
	}
	
	fun next() {
		model.next()
	}
	
	private fun loadInitialData() {
		fetchColleges("")
		fetchGrades()
		fetchTypes()
		fetchProfessions("")
		next()
	}
	
	fun fetchColleges(keyword: String) {
		model.add("jwxt/base-info/department/recruitUnitPull", "{\"departmentName\":\"$keyword\",\"subordinateDepartmentNumber\":null,\"id\":null}", 1)
	}
	
	private fun fetchGrades() {
		model.add("jwxt/base-info/codedata/findcodedataNames?datableNumber=127", 2)
	}
	
	private fun fetchTypes() {
		model.add("jwxt/base-info/codedata/findcodedataNames?datableNumber=97", 3)
	}
	
	fun fetchProfessions(keyword: String) {
		model.add("jwxt/base-info/profession-direction/pull?majorProfessionDircetion=1&nameCode=$keyword", 4)
	}
	
	fun onCollegeSelected(index: Int) {
		if (index in collegeIds.indices) {
			selectedCollegeId = collegeIds[index]
			selectedCollegeName = collegeNames[index]
		}
	}
	
	fun onProfessionSelected(index: Int) {
		if (index in professionIds.indices) {
			selectedProfessionId = professionIds[index]
			selectedProfessionName = professionNames[index]
		}
	}
	
	fun onGradeSelected(index: Int) {
		if (index in gradeIds.indices) {
			selectedGradeId = gradeIds[index]
			selectedGradeIndex = index
		}
	}
	
	fun onTypeSelected(index: Int) {
		if (index in typeIds.indices) {
			selectedTypeId = typeIds[index]
		}
	}
	
	fun query() {
		resultSections.clear()
		resultPage = 0
		resultTotal = -1
		showResults = true
		fetchResults()
	}
	
	fun loadMore() {
		if (resultTotal > resultPage * 10) {
			fetchResults()
		}
	}
	
	private fun fetchResults() {
		val params = JSONObject.of("manageUnitNum", selectedCollegeId, "grade", selectedGradeId, "professionCode", selectedProfessionId, "trainTypeCode", selectedTypeId)
//		println("{\"pageNo\":${++resultPage},\"pageSize\":10,\"total\":true,\"param\":{\"manageUnitNum\":\"$selectedCollegeId\",\"grade\":\"$selectedGradeId\",\"professionCode\":\"$selectedProfessionId\",\"trainTypeCode\":\"$selectedTypeId\"}}")
		model.addAndNext("jwxt/training-programe/training-programe/undergradute/profession-info",
		                 "{\"pageNo\":${++resultPage},\"pageSize\":10,\"total\":true,\"param\":$params}",
		                 5)
	}
	
	fun reset() {
		selectedCollegeId = null
		selectedCollegeName = ""
		selectedGradeId = gradeIds.lastOrNull()
		selectedGradeIndex = gradeNames.size - 1
		selectedProfessionId = null
		selectedProfessionName = ""
		selectedTypeId = typeIds.firstOrNull()
		showResults = false
		resultSections.clear()
	}
	
	fun navigateBack() {
		if (showResults) {
			showResults = false
			resultSections.clear()
		}
	}
	
	override fun onCleared() {
		model.dispose()
	}
}