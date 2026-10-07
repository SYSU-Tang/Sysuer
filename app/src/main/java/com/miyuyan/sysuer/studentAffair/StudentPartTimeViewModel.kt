package com.miyuyan.sysuer.studentAffair

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.alibaba.fastjson2.JSONObject
import com.miyuyan.sysuer.model.XgxtModel
import com.miyuyan.sysuer.view.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class StudentPartTimeViewModel(application: Application) : AndroidViewModel(application) {
	private val model: XgxtModel = XgxtModel(application)

	private val _years = MutableStateFlow<List<JSONObject>>(emptyList())
	val years: StateFlow<List<JSONObject>> = _years.asStateFlow()

	private val _campuses = MutableStateFlow<List<JSONObject>>(emptyList())
	val campuses: StateFlow<List<JSONObject>> = _campuses.asStateFlow()

	private val _jobTypes = MutableStateFlow<List<JSONObject>>(emptyList())
	val jobTypes: StateFlow<List<JSONObject>> = _jobTypes.asStateFlow()

	// 筛选条件，空串代表全部
	private val _year = MutableStateFlow(DEFAULT_YEAR)
	val year = _year.asStateFlow()

	private val _campus = MutableStateFlow("")
	val campus = _campus.asStateFlow()

	private val _jobType = MutableStateFlow("")
	val jobType = _jobType.asStateFlow()

	private val _jobName = MutableStateFlow("")
	val jobName = _jobName.asStateFlow()

	private val _unitName = MutableStateFlow("")
	val unitName = _unitName.asStateFlow()

	private val _jobs = MutableStateFlow<List<JSONObject>>(emptyList())
	val jobs: StateFlow<List<JSONObject>> = _jobs.asStateFlow()

	private val _cv = MutableStateFlow<JSONObject?>(null)
	val cv: StateFlow<JSONObject?> = _cv.asStateFlow()

	val uiState = model.getUiState(REQUEST_JOBS)
	val cvState = model.getUiState(REQUEST_CV)

	private var page = 1
	private var total = -1

	init {
		viewModelScope.launch {
			model.message.collect { (code, response) ->
				if (response.containsKey("code") && response.getInteger("code") == 200) {
					when (code) {
						REQUEST_JOBS -> response.getJSONObject("data")?.let { data ->
							total = data.getInteger("total") ?: -1
							_jobs.value += data.getJSONArray("list").orEmpty().filterIsInstance<JSONObject>()
							uiState.value = if (_jobs.value.isEmpty()) UiState.Empty else UiState.Content
						}

						REQUEST_YEARS ->
							_years.value = response.getJSONArray("data").orEmpty().filterIsInstance<JSONObject>()

						REQUEST_CAMPUSES ->
							_campuses.value = response.getJSONArray("data").orEmpty().filterIsInstance<JSONObject>()

						REQUEST_JOB_TYPES ->
							_jobTypes.value = response.getJSONArray("data").orEmpty().filterIsInstance<JSONObject>()

						REQUEST_CV -> {
							_cv.value = response.getJSONObject("data")
							cvState.value = if (_cv.value == null) UiState.Empty else UiState.Content
						}					}
				}
			}
		}
		year()
		campus()
		jobType()
		recruitment()
	}

	fun selectYear(value: String) {
		if (_year.value != value) {
			_year.value = value
			reload()
		}
	}

	fun selectCampus(value: String) {
		if (_campus.value != value) {
			_campus.value = value
			reload()
		}
	}

	fun selectJobType(value: String) {
		if (_jobType.value != value) {
			_jobType.value = value
			reload()
		}
	}

	fun setJobName(value: String) {
		if (_jobName.value != value) {
			_jobName.value = value
			reload()
		}
	}

	fun setUnitName(value: String) {
		if (_unitName.value != value) {
			_unitName.value = value
			reload()
		}
	}

	fun loadMore() {
		if ((page - 1) * PAGE_SIZE < total) recruitment()
	}

	fun loadCv() {
		if (_cv.value == null && cvState.value != UiState.Loading) {
			cvState.value = UiState.Loading
			model.enqueue("qgzx/api/sm-qgzx/xsjl/get", REQUEST_CV)
		}
	}

	fun reload() {
		page = 1
		total = -1
		_jobs.value = emptyList()
		uiState.value = UiState.Loading
		recruitment()
	}

	private fun recruitment() {
		val url = StringBuilder("qgzx/api/sm-qgzx/gwsq?pageSize=$PAGE_SIZE&pageNum=${page++}")
		mapOf(
				"qgzxnd" to _year.value,
				"gwlxids" to _jobType.value,
				"xqids" to _campus.value,
				"qgzxgwmc" to _jobName.value,
				"sgdwmc" to _unitName.value,
		).forEach { (key, value) ->
			if (value.isNotEmpty()) url.append("&$key=$value")
		}
		model.enqueue("$url", REQUEST_JOBS)
	}

	private fun year() {
		model.enqueue("qgzx/api/sm-qgzx/gwsq/ndlist/get", REQUEST_YEARS)
	}

	private fun campus() {
		model.enqueue("qgzx/api/sm-qgzx/gwsq/xylist/get", REQUEST_CAMPUSES)
	}

	private fun jobType() {
		model.enqueue("qgzx/api/sm-qgzx/gwsq/gwlxlist/get", REQUEST_JOB_TYPES)
	}

	override fun onCleared() {
		model.dispose()
	}

	companion object {
		private const val PAGE_SIZE = 10
		private const val DEFAULT_YEAR = "2026"

		private const val REQUEST_JOBS = 0
		private const val REQUEST_YEARS = 1
		private const val REQUEST_CAMPUSES = 2
		private const val REQUEST_JOB_TYPES = 3
		private const val REQUEST_CV = 4
	}
}
