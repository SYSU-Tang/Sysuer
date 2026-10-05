package com.miyuyan.sysuer.api

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import okio.Path.Companion.toPath
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.concurrent.Volatile

/**
 * DataStore 管理类，支持通过 Hilt 依赖注入，并保留单例与静态方法以实现向下兼容。
 */
@Singleton
class DataStoreManager @Inject constructor(
	@ApplicationContext private val context: Context
) {
	enum class ContentType {
		MARKDOWN, HTML
	}

	val dataStore: DataStore<Preferences> by lazy {
		PreferenceDataStoreFactory.createWithPath(
			produceFile = { File(context.filesDir, "datastore/today_class.preferences_pb").absolutePath.toPath() }
		)
	}

	/** 保存标题对应的内容 */
	suspend fun saveContent(title: String, content: String) {
		dataStore.edit { it[stringPreferencesKey(title)] = content }
	}

	/** 读取标题对应的内容，缺省为空字符串 */
	suspend fun loadContent(title: String): String =
		dataStore.data.first()[stringPreferencesKey(title)] ?: ""

	/** 订阅标题对应的内容变化 */
	fun loadContentFlow(title: String): Flow<String> =
		dataStore.data.map { it[stringPreferencesKey(title)] ?: "" }

	companion object {
		val TODAY_CLASS: Preferences.Key<String> = stringPreferencesKey("today_class")

		@Volatile
		private var instance: DataStoreManager? = null

		private fun getManagerInstance(context: Context): DataStoreManager {
			return instance ?: synchronized(this) {
				instance ?: DataStoreManager(context.applicationContext).also { instance = it }
			}
		}

		@Synchronized
		fun getInstance(context: Context): DataStore<Preferences> {
			return getManagerInstance(context).dataStore
		}

		suspend fun saveContent(context: Context, title: String, content: String) {
			getManagerInstance(context).saveContent(title, content)
		}

		suspend fun loadContent(context: Context, title: String): String {
			return getManagerInstance(context).loadContent(title)
		}
	}
}

/**
 * Hilt 依赖注入模块，向 Hilt 提供 [DataStoreManager] 与 [DataStore<Preferences>] 依赖
 */
@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {

	@Provides
	@Singleton
	fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> {
		return DataStoreManager.getInstance(context)
	}
}
