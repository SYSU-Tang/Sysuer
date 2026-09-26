package com.miyuyan.sysuer.academic

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.alibaba.fastjson2.JSONObject
import com.miyuyan.sysuer.model.XgxtModel
import com.miyuyan.sysuer.view.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LeaveReturnRegistrationViewModel(application: Application) : AndroidViewModel(application) {
	private val model: XgxtModel = XgxtModel(application)

	private val _years = MutableStateFlow<List<JSONObject>>(emptyList())
	val years = _years.asStateFlow()

	private val _selectedYear = MutableStateFlow<String?>(null)
	val selectedYear = _selectedYear.asStateFlow()

	private val _workList = MutableStateFlow<List<JSONObject>>(emptyList())
	val workList = _workList.asStateFlow()

	private val _uiState = MutableStateFlow(UiState.Loading)
	val uiState = _uiState.asStateFlow()

	init {
		viewModelScope.launch {
			model.messageChannel.collect { (code, response) ->
				if (response.getInteger("code") == 200) {
					when (code) {
						0 -> {
							response.getJSONArray("data")?.let { array ->
								val list = array.filterIsInstance<JSONObject>()
								_years.value = list
								list.firstOrNull()?.getString("value")?.let { firstYear ->
									if (_selectedYear.value == null) {
										selectYear(firstYear)
									}
								}
							}
						}

						1 -> {
							response.getJSONArray("data")?.let { array ->
								_workList.value = array.filterIsInstance<JSONObject>()
								_uiState.value = UiState.Content
							}
						}
					}
				}
			}
		}
		fetchYears()
	}

	fun selectYear(yearStr: String) {
		_selectedYear.value = yearStr
		_uiState.value = UiState.Loading
		model.enqueue("jjrlfx/api/sm-jjrlfx/student/work-list?blxn=$yearStr", 1)
	}

	fun fetchYears() {
		model.enqueue("jjrlfx/api/sm-jjrlfx/student/school-year", 0)
	}
}
