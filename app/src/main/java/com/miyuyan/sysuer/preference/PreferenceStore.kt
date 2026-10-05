package com.miyuyan.sysuer.preference

import android.content.Context
import androidx.datastore.core.DataMigration
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.preference.PreferenceDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okio.Path.Companion.toPath
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import kotlin.concurrent.Volatile

/**
 * 构造从 SharedPreferences 的一次性迁移：仅迁移 DataStore 中尚不存在的键，
 * 迁移完成后由迁移器清理 SharedPreferences
 */
internal fun sharedPreferencesMigration(
	context: Context,
	sharedPreferencesName: String,
): DataMigration<Preferences> =
	SharedPreferencesMigration(context.applicationContext, sharedPreferencesName)

/**
 * 基于 KMP 版 Preferences DataStore 的偏好存储，按迁移前的 SharedPreferences 文件分类。
 * 首次创建时阻塞读取一次快照，此后同步读取走内存缓存，写入先更新缓存再异步落盘。
 */
class PreferenceStore private constructor(
	context: Context,
	name: String,
	migrations: List<DataMigration<Preferences>>,
) {
	private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
	private val dataStore: DataStore<Preferences> = PreferenceDataStoreFactory.createWithPath(
		migrations = migrations,
		scope = scope,
		produceFile = { File(context.filesDir, "datastore/$name.preferences_pb").absolutePath.toPath() }
	)

	@Volatile
	private var snapshot: Preferences = runBlocking { dataStore.data.first() }

	private val _data = MutableStateFlow(snapshot)

	/** 当前存储内容的响应式流，可用于订阅变化 */
	val data: StateFlow<Preferences> = _data.asStateFlow()

	init {
		scope.launch {
			dataStore.data.collect {
				snapshot = it
				_data.value = it
			}
		}
	}

	operator fun <T> get(key: Preferences.Key<T>): T? = snapshot[key]

	fun <T> get(key: Preferences.Key<T>, default: T): T = snapshot[key] ?: default

	fun <T> set(key: Preferences.Key<T>, value: T) {
		updateSnapshot { it[key] = value }
		scope.launch { dataStore.edit { prefs -> prefs[key] = value } }
	}

	@Suppress("UNCHECKED_CAST")
	fun remove(vararg keys: Preferences.Key<out Any>) {
		updateSnapshot { prefs ->
			keys.forEach { prefs.remove(it as Preferences.Key<Any>) }
		}
		scope.launch {
			dataStore.edit { prefs ->
				keys.forEach { prefs.remove(it as Preferences.Key<Any>) }
			}
		}
	}

	@Synchronized
	private fun updateSnapshot(transform: (MutablePreferences) -> Unit) {
		snapshot = snapshot.toMutablePreferences().apply(transform).toPreferences()
		_data.value = snapshot
	}

	/**
	 * 桥接 androidx.preference 的 PreferenceFragmentCompat 界面，
	 * 使设置界面的读写直接作用于 DataStore
	 */
	fun asAndroidPreferenceDataStore(): PreferenceDataStore =
		object : PreferenceDataStore() {
			override fun getString(key: String?, defValue: String?): String? =
				get(stringPreferencesKey(key ?: return defValue), defValue ?: "")

			override fun putString(key: String?, value: String?) {
				if (key == null) return
				if (value == null) remove(stringPreferencesKey(key))
				else set(stringPreferencesKey(key), value)
			}

			override fun getBoolean(key: String?, defValue: Boolean): Boolean =
				get(booleanPreferencesKey(key ?: return defValue), defValue)

			override fun putBoolean(key: String?, value: Boolean) {
				if (key != null) set(booleanPreferencesKey(key), value)
			}

			override fun getInt(key: String?, defValue: Int): Int =
				get(intPreferencesKey(key ?: return defValue), defValue)

			override fun putInt(key: String?, value: Int) {
				if (key != null) set(intPreferencesKey(key), value)
			}

			override fun getLong(key: String?, defValue: Long): Long =
				get(longPreferencesKey(key ?: return defValue), defValue)

			override fun putLong(key: String?, value: Long) {
				if (key != null) set(longPreferencesKey(key), value)
			}

			override fun getFloat(key: String?, defValue: Float): Float =
				get(floatPreferencesKey(key ?: return defValue), defValue)

			override fun putFloat(key: String?, value: Float) {
				if (key != null) set(floatPreferencesKey(key), value)
			}

			override fun getStringSet(key: String?, defValues: Set<String>?): MutableSet<String> =
				get(
					stringSetPreferencesKey(key ?: return defValues?.toMutableSet() ?: mutableSetOf()),
					defValues ?: emptySet()
				).toMutableSet()

			override fun putStringSet(key: String?, values: MutableSet<String>?) {
				if (key == null) return
				if (values == null) remove(stringSetPreferencesKey(key))
				else set(stringSetPreferencesKey(key), values)
			}
		}

	companion object {
		private val stores = ConcurrentHashMap<String, PreferenceStore>()

		/**
		 * 按文件名单例获取，避免对同一文件创建多个 DataStore 实例
		 * @param migrations 首次访问时执行的一次性迁移，仅在实例创建时求值
		 * */
		fun getInstance(
			context: Context,
			name: String,
			migrations: () -> List<DataMigration<Preferences>> = { emptyList() },
		): PreferenceStore = stores.computeIfAbsent(name) {
			PreferenceStore(context.applicationContext, name, migrations())
		}
	}
}
