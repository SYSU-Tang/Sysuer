package com.miyuyan.sysuer.life

import android.app.Application
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.alibaba.fastjson2.JSONObject
import com.miyuyan.sysuer.R
import com.miyuyan.sysuer.api.CommonUtil
import com.miyuyan.sysuer.model.PortalModel
import com.miyuyan.sysuer.view.SectionData
import com.miyuyan.sysuer.view.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 校车班车信息:工作日 / 假日切换后重建各行驶方向的班次卡片
 */
class SchoolBusViewModel(application: Application) : AndroidViewModel(application) {
	private val portalModel = PortalModel(application)
	private val _uiState = portalModel.getUiState(0)
	val uiState = _uiState.asStateFlow()
	private var data: JSONObject? = null

	/** true 为工作日,false 为假日 */
	private val _day = MutableStateFlow(true)
	val day = _day.asStateFlow()

	/** 各行驶方向名称,作为顶栏 Tab 标题 */
	val routes = mutableStateListOf<String>()

	/** 与 [routes] 一一对应的各方向班次卡片 */
	val routeSections = mutableStateListOf<SnapshotStateList<SectionData>>()

	private val _notice = MutableStateFlow("")
	val notice = _notice.asStateFlow()

	init {
		viewModelScope.launch {
			portalModel.message.collect { (code, response) ->
				if (code == 0) {
					if (response.getJSONObject("meta").getInteger("statusCode") == 200) {
						data = response.getJSONObject("data")
						build()
					} else _uiState.value = UiState.Error
				}
			}
		}
		viewModelScope.launch {
			_day.collect { build() }
		}
	}

	fun load() {
		_uiState.value = UiState.Loading
		portalModel.enqueue("newClient/api/extraCard/schoolBusShuttleInfo/selectSchoolBusMap", 0)
	}

	private fun build() {
		val data = data ?: return
		val array = data.getJSONArray(if (_day.value) "workDay" else "holiday")
		routes.clear()
		routeSections.clear()
		if (array.isNullOrEmpty()) {
			_uiState.value = UiState.Empty
			return
		}
		array.forEach { item ->
			item as JSONObject
			_notice.value = item.getString("note") ?: ""
			routes.add(item.getString("drivingDirectionName"))
			val sections = mutableStateListOf<SectionData>()
			sections.add(
					SectionData(
							title = getApplication<Application>().getString(R.string.route_detail),
							icon = R.drawable.bus,
							rows = CommonUtil.extractValue(
									getApplication(), item, intArrayOf(
									R.string.route, R.string.start, R.string.end
							), arrayOf(
									"drivingDirectionName", "startStation", "endStation"
							)
							)
					)
			)
			item.getJSONArray("schoolBusShuttleMomentList").forEach {
				it as JSONObject
				sections.add(
						SectionData(
								title = it.getString("time"),
								icon = R.drawable.bus,
								rows = CommonUtil.extractValue(
										getApplication(), it, intArrayOf(
										R.string.passenger,
										R.string.vehicles,
										R.string.time,
										R.string.route
								), arrayOf(
										"passenger", "vehiclesType", "time", "drivingRoute"
								)
								)
						)
				)
			}
			routeSections.add(sections)
		}
		_uiState.value = if (routeSections.isEmpty()) UiState.Empty else UiState.Content
	}

	fun retry() {
		load()
	}

	fun setDay(day: Boolean) {
		_day.value = day
	}

	override fun onCleared() {
		portalModel.dispose()
	}
}
