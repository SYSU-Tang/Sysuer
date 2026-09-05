package com.miyuyan.sysuer.academic

import android.app.Application
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.lifecycle.AndroidViewModel
import com.alibaba.fastjson2.JSONObject
import com.miyuyan.sysuer.R
import com.miyuyan.sysuer.api.CommonUtil.extractValue
import com.miyuyan.sysuer.model.JwxtModel
import com.miyuyan.sysuer.view.SectionData

class MajorInfoViewModel(application: Application) : AndroidViewModel(application) {
	private val model: JwxtModel = JwxtModel(application)
	private val _categories = mutableStateListOf<JSONObject>()
	val categories: SnapshotStateList<JSONObject> = _categories
	private val _majorList = mutableStateMapOf<Int, SnapshotStateList<SectionData>>()
	val majorList: SnapshotStateMap<Int, SnapshotStateList<SectionData>> = _majorList
	private val pages = mutableMapOf<Int, Int>()
	private val totals = mutableMapOf<Int, Int>()
	
	init {
		model.message.observeForever { (code, response) ->
			if (response.getInteger("code") == 200 && response.get("data") != null) {
				when (code) {
					0 -> _categories.addAll(response.getJSONArray("data").filterIsInstance<JSONObject>())
					else -> {
						val tabIndex = code - 1
						val data = response.getJSONObject("data")
						totals[tabIndex] = data.getInteger("total")
						println("code $code")
						data.getJSONArray("rows").forEach { item ->
							_majorList.getOrPut(tabIndex) { mutableStateListOf() }
								.add(SectionData(title = (item as JSONObject).getString("name"),
								                 rows = extractValue(application,
								                                     item,
								                                     intArrayOf(R.string.major_code, R.string.major_name, R.string.schooling_length, R.string.study_period, R.string.discipline_category, R.string.degree_granting_category),
								                                     arrayOf("code", "name", "educationalSystem", "maxStudyYear", "disciplineCateName", "degreeGrantName"))))
						}
					}
				}
			}
		}
	}
	
	fun fetchCategories() {
		model.addAndNext("jwxt/base-info/codedata/findcodedataNames?datableNumber=135", 0)
	}
	
	fun fetchMajorList(tabIndex: Int) {
		if (hasMore(tabIndex)) {
			println("category: ${_categories[tabIndex]}")
			val categoryCode = _categories.getOrNull(tabIndex)?.getString("dataNumber") ?: return
			val page = pages.getOrPut(tabIndex) { 0 } + 1
			pages[tabIndex] = page
			println("fetchMajorList $tabIndex $page")
			model.addAndNext("jwxt/base-info/profession-direction/list", "{\"pageNo\":$page,\"pageSize\":10,\"total\":true,\"param\":{\"majorProfessionDircetion\":\"0\",\"disciplineCateCode\":\"$categoryCode\"}}", tabIndex + 1)
		}
	}
	
	fun hasMore(tabIndex: Int): Boolean {
		val page = pages[tabIndex] ?: return true
		val total = totals[tabIndex] ?: return true
		return page * 10 < total
	}
	
	override fun onCleared() {
		model.dispose()
	}
}