package com.miyuyan.sysuer.home

import android.app.Application
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.application
import androidx.lifecycle.viewModelScope
import com.alibaba.fastjson2.JSONArray
import com.alibaba.fastjson2.JSONObject
import com.alibaba.fastjson2.JSONReader
import com.alibaba.fastjson2.to
import com.miyuyan.sysuer.R
import com.miyuyan.sysuer.home.data.CollectionDatabase
import com.miyuyan.sysuer.home.data.DashboardShortcutEntity
import com.miyuyan.sysuer.home.data.ServiceCollectionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.nio.charset.StandardCharsets

class ServiceViewModel(application: Application) : AndroidViewModel(application) {
	private val db by lazy { CollectionDatabase.getDatabase(application) }
	private val _collection = mutableStateListOf<ServiceConfig>()
	val collection: SnapshotStateList<ServiceConfig> = _collection
	val allItems: SnapshotStateList<ServiceConfig> = mutableStateListOf()
	val serviceData: SnapshotStateList<Pair<String, List<ServiceConfig>>> = mutableStateListOf()
	fun loadServiceData() {
		if (allItems.isNotEmpty()) return
		val reader = JSONReader.of(application.resources.openRawResource(R.raw.service), StandardCharsets.UTF_8)
		reader.readJSONArray().forEach {
			val name = (it as JSONObject).getString("name", "")
			val items = it.getJSONArray("items") ?: JSONArray()
			val itemList = items.map { item ->
				(item as JSONObject).to<ServiceConfig>( JSONReader.Feature.SupportSmartMatch, JSONReader.Feature.IgnoreSetNullValue)
			}
			serviceData.add(Pair(name, itemList))
			allItems.addAll(itemList)
		}
		reader.close()
	}
	
	private val _orderCollection = mutableStateListOf<ServiceConfig>()
	val orderCollection: SnapshotStateList<ServiceConfig> = _orderCollection
	fun loadCollection() {
		viewModelScope.launch(Dispatchers.IO) {
			val services = db.collectionDao().getCollectedServices()
			_collection.clear()
			services.forEach { entity ->
				entity.serviceJson?.let { json ->
					_collection.add(JSONObject.parseObject(json, ServiceConfig::class.java, JSONReader.Feature.SupportSmartMatch, JSONReader.Feature.IgnoreSetNullValue))
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
					_orderCollection.add(JSONObject.parseObject(json, ServiceConfig::class.java, JSONReader.Feature.SupportSmartMatch, JSONReader.Feature.IgnoreSetNullValue))
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
				db.collectionDao().updateServicePosition(item.id, index)
			}
			loadCollection()
		}
	}
	
	suspend fun isServiceCollected(id: Int): Boolean = db.collectionDao().isServiceCollected(id)
	suspend fun isDashboardShortcutCollected(id: Int): Boolean = db.collectionDao().isDashboardShortcutCollected(id)
	fun addService(serviceId: Int, serviceJson: String, position: Int? = collection.size) {
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