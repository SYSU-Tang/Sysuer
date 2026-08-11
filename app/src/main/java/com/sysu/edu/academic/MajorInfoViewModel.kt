package com.sysu.edu.academic

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.alibaba.fastjson2.JSONObject
import com.sysu.edu.model.JwxtModel

class MajorInfoViewModel(application: Application) : AndroidViewModel(application) {
	private val model: JwxtModel = JwxtModel(application)
	private val _categories = MutableLiveData<List<JSONObject>>(emptyList())
	val categories: LiveData<List<JSONObject>> = _categories
	private val _majorList = MutableLiveData<Map<Int, List<JSONObject>>>(emptyMap())
	val majorList: LiveData<Map<Int, List<JSONObject>>> = _majorList
	private val pages = mutableMapOf<Int, Int>()
	private val totals = mutableMapOf<Int, Int>()

	init {
		model.message.observeForever { (code, response) ->
			if (response.getInteger("code") == 200 && response.get("data") != null) {
				when (code) {
					0 -> {
						_categories.value = response.getJSONArray("data").filterIsInstance<JSONObject>()
					}
					else -> {
						val tabIndex = code - 1
						val data = response.getJSONObject("data")
						totals[tabIndex] = data.getInteger("total")
						val newRows = data.getJSONArray("rows").filterIsInstance<JSONObject>()
						val current = _majorList.value?.get(tabIndex) ?: emptyList()
						_majorList.value = (_majorList.value ?: emptyMap()) + (tabIndex to (current + newRows))
					}
				}
			}
		}
	}

	fun fetchCategories() {
		model.addAndNext("jwxt/base-info/codedata/findcodedataNames?datableNumber=135", 0)
	}

	fun fetchMajorList(tabIndex: Int) {
		val categoryCode = _categories.value?.getOrNull(tabIndex)?.getString("dataNumber") ?: return
		val page = pages.getOrPut(tabIndex) { 0 } + 1
		pages[tabIndex] = page
		model.addAndNext("jwxt/base-info/profession-direction/list",
			"""{"pageNo":$page,"pageSize":10,"total":true,"param":{"majorProfessionDircetion":"0","disciplineCateCode":"$categoryCode"}}""",
			tabIndex + 1)
	}

	fun reFetchMajorList(tabIndex: Int) {
		pages[tabIndex] = 0
		totals[tabIndex] = -1
		val currentMap = _majorList.value?.toMutableMap() ?: mutableMapOf()
		currentMap.remove(tabIndex)
		_majorList.value = currentMap
		fetchMajorList(tabIndex)
	}

	fun hasMore(tabIndex: Int): Boolean {
		val page = pages[tabIndex] ?: 0
		val total = totals[tabIndex] ?: -1
		return page * 10 < total
	}

	override fun onCleared() {
		model.dispose()
	}
}