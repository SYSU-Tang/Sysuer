package com.sysu.edu.home

import android.app.Application
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.application
import androidx.lifecycle.viewModelScope
import com.alibaba.fastjson2.JSONArray
import com.alibaba.fastjson2.JSONObject
import com.alibaba.fastjson2.JSONReader
import com.sysu.edu.R
import com.sysu.edu.home.data.CollectionDatabase
import com.sysu.edu.home.data.DashboardShortcutEntity
import com.sysu.edu.home.data.ServiceCollectionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.nio.charset.StandardCharsets

class ServiceViewModel(application: Application) : AndroidViewModel(application) {
	private val db by lazy { CollectionDatabase.getDatabase(application) }
	private val _collection = mutableStateListOf<JSONObject>()
	val collection: SnapshotStateList<JSONObject> = _collection
	val allItems: SnapshotStateList<JSONObject> = mutableStateListOf()
	val serviceData: SnapshotStateList<Pair<String, List<JSONObject>>> = mutableStateListOf()
	fun loadServiceData() {
		if (allItems.isNotEmpty()) return
		val reader = JSONReader.of(application.resources.openRawResource(R.raw.service), StandardCharsets.UTF_8)
		val tempGroups = mutableListOf<Pair<String, List<JSONObject>>>()
		val tempItems = mutableListOf<JSONObject>()
		reader.readJSONArray().forEach {
			val name = (it as JSONObject).getString("name", "")
			val items = it.getJSONArray("items") ?: JSONArray()
			val itemList = items.map { item -> item as JSONObject }
			tempGroups.add(Pair(name, itemList))
			tempItems.addAll(itemList)
		}
		reader.close()
		serviceData.addAll(tempGroups)
		allItems.addAll(tempItems)
	}
	
	private val _orderCollection = mutableStateListOf<JSONObject>()
	val orderCollection: SnapshotStateList<JSONObject> = _orderCollection
	fun loadCollection() {
		viewModelScope.launch(Dispatchers.IO) {
			val services = db.collectionDao().getCollectedServices()
			_collection.clear()
			services.forEach { entity ->
				entity.serviceJson?.let { json ->
					_collection.add(JSONObject.parse(json))
				}
			}
		}
	}
	
	fun loadOrderCollection() {
		viewModelScope.launch(Dispatchers.IO) {
			val services = db.collectionDao().getCollectedServices()
			_orderCollection.clear()
			services.forEach { entity ->
				entity.serviceJson?.let { json ->
					_orderCollection.add(JSONObject.parse(json))
				}
			}
		}
	}
	
	fun moveOrderCollection(from: Int, to: Int) {
		if (from == to) return
		val item = _orderCollection.removeAt(from)
		_orderCollection.add(to, item)
	}
	
	fun saveOrderCollection() {
		viewModelScope.launch(Dispatchers.IO) {
			_orderCollection.forEachIndexed { index, item ->
				db.collectionDao().updateServicePosition(item.getIntValue("id"), index)
			}
			loadCollection()
		}
	}
	
	suspend fun isServiceCollected(id: Int): Boolean = db.collectionDao().isServiceCollected(id)
	suspend fun isDashboardShortcutCollected(id: Int): Boolean = db.collectionDao().isDashboardShortcutCollected(id)
	fun addService(serviceId: Int, serviceJson: String, position: Int?) {
		viewModelScope.launch(Dispatchers.IO) {
			db.collectionDao().addService(ServiceCollectionEntity(serviceId = serviceId, serviceJson = serviceJson, position = position))
		}
	}
	
	fun deleteService(serviceId: Int) {
		viewModelScope.launch(Dispatchers.IO) { db.collectionDao().deleteService(serviceId) }
	}
	
	fun addDashboardShortcut(shortcutId: Int, shortcutJson: String, position: Int?) {
		viewModelScope.launch(Dispatchers.IO) {
			db.collectionDao().addDashboardShortcut(DashboardShortcutEntity(shortcutId = shortcutId, shortcutJson = shortcutJson, position = position))
		}
	}
	
	fun deleteDashboardShortcut(shortcutId: Int) {
		viewModelScope.launch(Dispatchers.IO) { db.collectionDao().deleteDashboardShortcut(shortcutId) }
	}
}