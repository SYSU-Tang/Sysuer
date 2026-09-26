package com.miyuyan.sysuer.academic

import android.app.Application
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.miyuyan.sysuer.R
import com.miyuyan.sysuer.api.TargetUrl
import com.miyuyan.sysuer.model.TiceModel
import com.miyuyan.sysuer.view.RowData
import com.miyuyan.sysuer.view.SectionData
import com.miyuyan.sysuer.view.UiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup


class PhysicalFitnessTestResultViewModel(application: Application) : AndroidViewModel(application) {
	private val model: TiceModel = TiceModel(application)

	private val _sections = mutableStateMapOf<Int, SnapshotStateList<SectionData>>()
	val sections: SnapshotStateMap<Int, SnapshotStateList<SectionData>> = _sections

	private val _uiStates = mutableStateMapOf<Int, MutableStateFlow<UiState>>()
	fun getUiState(tabIndex: Int): StateFlow<UiState> {
		return _uiStates.getOrPut(tabIndex) { MutableStateFlow(UiState.Loading) }.asStateFlow()
	}

	private val urls = listOf("m/tice", "m/kwjfList", "m/tice/studentSwim")
	private val nameResIds =
		listOf(R.string.total_score, R.string.total_gym_credit, R.string.swimming_status)

	init {
		fetchData()
	}

	fun fetchData() {
		val cookieString = model.cookie

		(0..2).forEach { tabIndex ->
			val uiStateFlow = _uiStates.getOrPut(tabIndex) { MutableStateFlow(UiState.Loading) }
			uiStateFlow.value = UiState.Loading

			viewModelScope.launch(Dispatchers.IO) {
				try {
					val doc = Jsoup.connect("https://tice.sysu.edu.cn/${urls[tabIndex]}")
						.header("Cookie", cookieString)
						.userAgent("Mozilla/5.0 (Linux; Android 15.0; Nexus 5 Build/MRA58N) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/144.0.0.0 Mobile Safari/537.36")
						.timeout(5000).get()

					if (doc.getElementById("netid-login") != null) {
						withContext(Dispatchers.Main) {
							model.contextUtil.login(TargetUrl.TICE) {
								fetchData()
							}
						}
						return@launch
					}

					val sectionList = mutableStateListOf<SectionData>()

					doc.select("a.weui-cell.weui-cell_access").forEach { element ->
						val title = element.selectFirst(".weui-cell__bd p")?.text() ?: ""
						val score = element.selectFirst("span")?.text() ?: ""
						val link = element.attr("href")

						val detailRows = mutableStateListOf<RowData>()
						if (link.isNotBlank()) {
							try {
								val detailDoc = Jsoup.connect("https://tice.sysu.edu.cn$link")
									.header("Cookie", cookieString)
									.userAgent("Mozilla/5.0 (Linux; Android 15.0; Nexus 5 Build/MRA58N) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/144.0.0.0 Mobile Safari/537.36")
									.timeout(5000).get()

								detailDoc.select(".weui-cell").forEach { cellElement ->
									when (tabIndex) {
										0 -> {
											val cellTitle =
												cellElement.selectFirst(".weui-cell__bd p")?.text()
											val cellScore =
												cellElement.selectFirst(".weui-cell__ft")?.text()
											if (!cellTitle.isNullOrBlank()) {
												detailRows.add(RowData(cellTitle, cellScore))
											}
										}

										1 -> {
											val key = cellElement.selectFirst("p")?.text()
											val value =
												cellElement.selectFirst(".weui-cell__hd")?.text()
											if (!key.isNullOrBlank() || !value.isNullOrBlank()) {
												detailRows.add(RowData(key, value))
											}
											cellElement.select(".container").forEach { item ->
												val itemKey = item.selectFirst(".left_side")?.text()
												val itemVal = item.selectFirst("p")?.text()
												if (!itemKey.isNullOrBlank() || !itemVal.isNullOrBlank()) {
													detailRows.add(RowData(itemKey, itemVal))
												}
											}
										}

										2 -> {
											val cellTitle =
												cellElement.selectFirst(".weui-cell__bd p")?.text()
											val cellScore =
												cellElement.selectFirst(".weui-cell__ft")?.text()
											if (!cellTitle.isNullOrBlank()) {
												detailRows.add(RowData(cellTitle, cellScore))
											}
										}
									}
								}
							} catch (_: Exception) {
							}
						}

						if (detailRows.isEmpty()) {
							detailRows.add(
									RowData(
											getApplication<Application>().getString(
													nameResIds[tabIndex]
											), score
									)
							)
						}
						sectionList.add(SectionData(title = title, rows = detailRows))
					}

					_sections[tabIndex] = sectionList
					uiStateFlow.value =
						if (sectionList.isEmpty()) UiState.Empty else UiState.Content

				} catch (_: Exception) {
					withContext(Dispatchers.Main) {
						uiStateFlow.value = UiState.Error
					}
				}
			}
		}
	}

	override fun onCleared() {
		model.dispose()
	}
}
