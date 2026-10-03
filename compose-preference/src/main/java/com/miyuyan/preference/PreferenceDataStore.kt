package com.miyuyan.preference

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStoreFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.io.IOException

/**
 * 库内共享的写操作作用域:偏好写入是异步落盘的 fire-and-forget,
 * 不依赖组合期间的 rememberCoroutineScope,组合外(如回调后)也能写。
 */
internal val PreferenceWriteScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

/**
 * compose-preference 的 DataStore 存储入口。
 *
 * 首次访问时以懒加载方式创建名为 `compose_preferences` 的 [Preferences] DataStore,
 * 之后全局复用同一实例。如需与应用侧已有 DataStore 合并或自定义文件名,
 * 在任何偏好组件首次使用前调用 [set] 注入实例即可。
 *
 * 用法:
 * ```
 * // 直接读写
 * val checked = rememberPreference(booleanPreferencesKey("notification"), false)
 * SwitchPreference(title = "通知", checked = checked.value, onCheckedChange = { checked.value = it })
 *
 * // 或使用组件的 key 绑定重载
 * SwitchPreference(title = "通知", key = booleanPreferencesKey("notification"))
 * ```
 *
 * 注意:实例按进程缓存,不支持多进程访问(与 PreferenceDataStoreFactory 的默认行为一致)。
 */
object PreferenceDataStore {

	@Volatile
	private var override: DataStore<Preferences>? = null

	@Volatile
	private var singleton: DataStore<Preferences>? = null

	/** 注入外部 DataStore(如应用侧已有的实例);须在首次 [get] 之前调用。 */
	fun set(dataStore: DataStore<Preferences>) {
		override = dataStore
	}

	/** 获取库内共享的 DataStore 单例。 */
	fun get(context: Context): DataStore<Preferences> {
		override?.let { return it }
		return singleton ?: synchronized(this) {
			override ?: PreferenceDataStoreFactory.create(
				scope = PreferenceWriteScope,
			) {
				context.applicationContext.preferencesDataStoreFile("compose_preferences")
			}.also { singleton = it }
		}
	}
}

/**
 * 把 [key] 对应的偏好绑定为一个 [MutableState]:读经 `dataStore.data` Flow 自动响应,
 * 写经 `dataStore.edit` 异步落盘。App 重启后值保持。
 *
 * @param key 偏好键,类型由 DataStore 的 key 工厂决定
 *   (`booleanPreferencesKey` / `intPreferencesKey` / `floatPreferencesKey` /
 *   `longPreferencesKey` / `doublePreferencesKey` / `stringPreferencesKey` /
 *   `stringSetPreferencesKey`)。
 * @param defaultValue 键尚无值时的默认值,也是异步加载完成前的占位值。
 */
@Composable
fun <T> rememberPreference(
	key: Preferences.Key<T>,
	defaultValue: T,
): MutableState<T> {
	val dataStore = PreferenceDataStore.get(LocalContext.current)

	val flow: Flow<T> = remember(key, dataStore) {
		dataStore.data
			// DataStore 在读取损坏文件等 IO 异常时会抛出;按官方建议回落到空偏好,避免崩溃
			.catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
			.map { it[key] ?: defaultValue }
	}
	val state = flow.collectAsState(initial = defaultValue)

	return remember(key, dataStore) {
		object : MutableState<T> {
			override var value: T
				get() = state.value
				set(newValue) {
					PreferenceWriteScope.launch {
						dataStore.edit { it[key] = newValue }
					}
				}

			override fun component1(): T = value
			override fun component2(): (T) -> Unit = { value = it }
		}
	}
}
