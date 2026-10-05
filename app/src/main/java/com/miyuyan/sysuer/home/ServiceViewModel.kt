package com.miyuyan.sysuer.home

import android.app.Application
import android.content.res.Configuration
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.core.os.LocaleListCompat
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
	private var loadedLocales: String? = null

	/**
	 * 加载服务数据。数据源为带语言限定符的 raw 资源（raw / raw-en），
	 * 语言切换后 Activity 虽会重建但 ViewModel 保留，
	 * 因此仅在传入的 locales 变化时清空并按新语言重新加载
	 * @param locales 调用方（Activity）当前的Locale标签，如 "zh-CN"、"en-US"
	 * */
	fun loadServiceData(locales: String) {
		if (allItems.isNotEmpty() && locales == loadedLocales) return
		serviceData.clear()
		allItems.clear()
		val baseConfig = Configuration(application.resources.configuration)
		(LocaleListCompat.forLanguageTags(locales).unwrap() as? android.os.LocaleList)
			?.takeIf { !it.isEmpty }?.let(baseConfig::setLocales)
		val resources = application.createConfigurationContext(baseConfig).resources
		val reader = JSONReader.of(resources.openRawResource(R.raw.service), StandardCharsets.UTF_8)
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
		loadedLocales = locales
	}
	
	private val _orderCollection = mutableStateListOf<ServiceConfig>()
	val orderCollection: SnapshotStateList<ServiceConfig> = _orderCollection

	/**
	 * 收藏项展示名跟随当前语言：优先用 [allItems] 中当前语言的配置，
	 * 找不到（如数据尚未加载或服务已下架）时回落到收藏时存储的 JSON 快照
	 * */
	private fun List<ServiceCollectionEntity>.resolveConfigs(): List<ServiceConfig> =
		mapNotNull { entity ->
			allItems.firstOrNull { it.id == entity.serviceId }
				?: entity.serviceJson?.let {
					JSONObject.parseObject(
							it, ServiceConfig::class.java,
							JSONReader.Feature.SupportSmartMatch, JSONReader.Feature.IgnoreSetNullValue
					)
				}
		}

	fun loadCollection() {
		viewModelScope.launch(Dispatchers.IO) {
			val refreshed = db.collectionDao().getCollectedServices().resolveConfigs()
			_collection.clear()
			_collection.addAll(refreshed)
		}
	}

	fun loadOrderCollection() {
		viewModelScope.launch(Dispatchers.IO) {
			val refreshed = db.collectionDao().getCollectedServices().resolveConfigs()
			_orderCollection.clear()
			_orderCollection.addAll(refreshed)
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