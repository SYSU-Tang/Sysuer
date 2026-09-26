package com.miyuyan.sysuer.academic

import android.app.Application
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.alibaba.fastjson2.JSONObject
import com.miyuyan.sysuer.R
import com.miyuyan.sysuer.api.CommonUtil.extractValue
import com.miyuyan.sysuer.model.JwxtModel
import com.miyuyan.sysuer.view.MenuItem
import com.miyuyan.sysuer.view.SectionData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

class TrainingProgramViewModel(application: Application) : AndroidViewModel(application) {
	private val model = JwxtModel(application)

	private val _collegeNames = MutableStateFlow<List<String>>(emptyList())
	val collegeNames: StateFlow<List<String>> = _collegeNames.asStateFlow()

	private val _collegeIds = MutableStateFlow<List<String>>(emptyList())
//	val collegeIds: StateFlow<List<String>> = _collegeIds.asStateFlow()

	private val _gradeNames = MutableStateFlow<List<String>>(emptyList())
	val gradeNames: StateFlow<List<String>> = _gradeNames.asStateFlow()

	private val _gradeIds = MutableStateFlow<List<String>>(emptyList())
//	val gradeIds: StateFlow<List<String>> = _gradeIds.asStateFlow()

	private val _professionNames = MutableStateFlow<List<String>>(emptyList())
	val professionNames: StateFlow<List<String>> = _professionNames.asStateFlow()

	private val _professionIds = MutableStateFlow<List<String>>(emptyList())
//	val professionIds: StateFlow<List<String>> = _professionIds.asStateFlow()

	private val _typeNames = MutableStateFlow<List<String>>(emptyList())
	val typeNames: StateFlow<List<String>> = _typeNames.asStateFlow()

	private val _typeIds = MutableStateFlow<List<String>>(emptyList())
	val typeIds: StateFlow<List<String>> = _typeIds.asStateFlow()

	private val _selectedCollegeId = MutableStateFlow<String?>(null)
//	val selectedCollegeId: StateFlow<String?> = _selectedCollegeId.asStateFlow()

	private val _selectedGradeId = MutableStateFlow<String?>(null)
//	val selectedGradeId: StateFlow<String?> = _selectedGradeId.asStateFlow()

	private val _selectedProfessionId = MutableStateFlow<String?>(null)
//	val selectedProfessionId: StateFlow<String?> = _selectedProfessionId.asStateFlow()

	private val _selectedTypeId = MutableStateFlow<String?>(null)
	val selectedTypeId: StateFlow<String?> = _selectedTypeId.asStateFlow()

	private val _selectedCollegeName = MutableStateFlow("")
	val selectedCollegeName: StateFlow<String> = _selectedCollegeName.asStateFlow()

	private val _selectedProfessionName = MutableStateFlow("")
	val selectedProfessionName: StateFlow<String> = _selectedProfessionName.asStateFlow()

	private val _selectedGradeIndex = MutableStateFlow(0)
	val selectedGradeIndex: StateFlow<Int> = _selectedGradeIndex.asStateFlow()

	private val _showResults = MutableStateFlow(false)
	val showResults: StateFlow<Boolean> = _showResults.asStateFlow()

	private val _viewDetailProgramId = MutableStateFlow<String?>(null)
	val viewDetailProgramId: StateFlow<String?> = _viewDetailProgramId.asStateFlow()

	private val _resultSections =
		MutableStateFlow<SnapshotStateList<SectionData>>(mutableStateListOf())
	val resultSections: StateFlow<SnapshotStateList<SectionData>> = _resultSections.asStateFlow()

	private var resultPage = 0
	private var resultTotal = -1

	init {
		viewModelScope.launch {
			model.messageChannel.filter { it.second.getInteger("code") == 200 }
				.collect { (code, response) ->
					when (code) {
						1 -> {
							val names = mutableListOf<String>()
							val ids = mutableListOf<String>()
							response.getJSONArray("data").forEach { e: Any? ->
								ids.add((e as JSONObject).getString("departmentNumber"))
								names.add(e.getString("departmentName"))
							}
							_collegeNames.value = names
							_collegeIds.value = ids
						}

						2 -> {
							val names = mutableListOf<String>()
							val ids = mutableListOf<String>()
							response.getJSONArray("data").forEach { e: Any? ->
								ids.add((e as JSONObject).getString("dataNumber"))
								names.add(e.getString("dataName"))
							}
							_gradeNames.value = names
							_gradeIds.value = ids
							if (ids.isNotEmpty()) {
								_selectedGradeId.value = ids.last()
								_selectedGradeIndex.value = names.size - 1
							}
						}

						3 -> {
							val names = mutableListOf<String>()
							val ids = mutableListOf<String>()
							response.getJSONArray("data").forEach { e: Any? ->
								ids.add((e as JSONObject).getString("dataNumber"))
								names.add(e.getString("dataName"))
							}
							_typeNames.value = names
							_typeIds.value = ids
							if (ids.isNotEmpty()) {
								_selectedTypeId.value = ids[0]
							}
						}

						4 -> {
							val names = mutableListOf<String>()
							val ids = mutableListOf<String>()
							response.getJSONArray("data").forEach { e: Any? ->
								ids.add((e as JSONObject).getString("code"))
								names.add(e.getString("name"))
							}
							_professionNames.value = names
							_professionIds.value = ids
						}

						5 -> {
							val responseData = response.getJSONObject("data")
							resultTotal = responseData.getInteger("total")
							val newSections = _resultSections.value
							responseData.getJSONArray("rows").forEach { o: Any? ->
								val obj = o as JSONObject
								val keys = intArrayOf(
										R.string.profession,
										R.string.grade,
										R.string.college,
										R.string.training_category,
										R.string.study_period,
										R.string.discipline_category,
										R.string.degree,
										R.string.profession_code,
										R.string.profession_id
								)
								newSections.add(
										SectionData(
												title = obj.getString("name"),
												key = "PersonalTrainingProgram_${
													o.getString("teachPlanNumber")
												}",
												rows = extractValue(
														application, obj, keys, arrayOf(
														"professionName",
														"grade",
														"manageUnitName",
														"trainTypeName",
														"educationalSystem",
														"disciplineCateName",
														"degreeGrantName",
														"professionCode",
														"professionId"
												)
												),
												footerMenus = mutableStateListOf(
														MenuItem(
																application.getString(R.string.view_detail)
														) {
															_viewDetailProgramId.value =
																o.getString("teachPlanNumber")
															true
														})
										)
								)
							}
							_resultSections.value = newSections
						}
					}
				}
		}
		loadInitialData()
	}

	private fun loadInitialData() {
		fetchGrades()
		fetchTypes()
		fetchColleges("")
		fetchProfessions("")
	}

	fun fetchColleges(keyword: String) {
		model.enqueue(
				"jwxt/base-info/department/recruitUnitPull",
				"{\"departmentName\":\"$keyword\",\"subordinateDepartmentNumber\":null,\"id\":null}",
				1
		)
	}

	private fun fetchGrades() {
		model.enqueue("jwxt/base-info/codedata/findcodedataNames?datableNumber=127", 2)
	}

	private fun fetchTypes() {
		model.enqueue("jwxt/base-info/codedata/findcodedataNames?datableNumber=97", 3)
	}

	fun fetchProfessions(keyword: String) {
		model.enqueue(
				"jwxt/base-info/profession-direction/pull?majorProfessionDircetion=1&nameCode=$keyword",
				4
		)
	}

	fun updateSelectedCollegeName(name: String) {
		_selectedCollegeName.value = name
	}

	fun updateSelectedProfessionName(name: String) {
		_selectedProfessionName.value = name
	}

	fun clearViewDetailProgramId() {
		_viewDetailProgramId.value = null
	}

	fun onCollegeSelected(index: Int) {
		val ids = _collegeIds.value
		if (index in ids.indices) {
			_selectedCollegeId.value = ids[index]
			_selectedCollegeName.value = _collegeNames.value[index]
		}
	}

	fun onProfessionSelected(index: Int) {
		val ids = _professionIds.value
		if (index in ids.indices) {
			_selectedProfessionId.value = ids[index]
			_selectedProfessionName.value = _professionNames.value[index]
		}
	}

	fun onGradeSelected(index: Int) {
		val ids = _gradeIds.value
		if (index in ids.indices) {
			_selectedGradeId.value = ids[index]
			_selectedGradeIndex.value = index
		}
	}

	fun onTypeSelected(index: Int) {
		val ids = _typeIds.value
		if (index in ids.indices) {
			_selectedTypeId.value = ids[index]
		}
	}

	fun query() {
		_resultSections.value = mutableStateListOf()
		resultPage = 0
		resultTotal = -1
		_showResults.value = true
		fetchResults()
	}

	fun loadMore() {
		if (resultTotal > resultPage * 10) {
			fetchResults()
		}
	}

	private fun fetchResults() {
		val params = JSONObject.of(
				"manageUnitNum",
				_selectedCollegeId.value,
				"grade",
				_selectedGradeId.value,
				"professionCode",
				_selectedProfessionId.value,
				"trainTypeCode",
				_selectedTypeId.value
		)
		model.enqueue(
				"jwxt/training-programe/training-programe/undergradute/profession-info",
				"{\"pageNo\":${++resultPage},\"pageSize\":10,\"total\":true,\"param\":$params}",
				5
		)
	}

	fun reset() {
		_selectedCollegeId.value = null
		_selectedCollegeName.value = ""
		_selectedGradeId.value = _gradeIds.value.lastOrNull()
		_selectedGradeIndex.value = _gradeNames.value.size - 1
		_selectedProfessionId.value = null
		_selectedProfessionName.value = ""
		_selectedTypeId.value = _typeIds.value.firstOrNull()
		_showResults.value = false
		_resultSections.value = mutableStateListOf()
	}

	fun navigateBack() {
		if (_showResults.value) {
			_showResults.value = false
			_resultSections.value = mutableStateListOf()
		}
	}

	override fun onCleared() {
		model.dispose()
	}
}