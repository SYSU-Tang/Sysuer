package com.miyuyan.sysuer.api

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.rxjava3.RxPreferenceDataStoreBuilder
import androidx.datastore.rxjava3.RxDataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.Disposable
import kotlinx.coroutines.ExperimentalCoroutinesApi
import javax.inject.Inject
import javax.inject.Singleton

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

	val rxDataStore: RxDataStore<Preferences> by lazy {
		RxPreferenceDataStoreBuilder(context.applicationContext, "today_class").build()
	}

	@OptIn(ExperimentalCoroutinesApi::class)
	fun saveContent(title: String, content: String, callback: () -> Unit = {}): Disposable =
		rxDataStore.updateDataAsync { prefs ->
			Single.just(prefs.toMutablePreferences().apply { this[stringPreferencesKey(title)] = content })
		}.subscribe({
			callback()
		}, {
			println("Error saving content: ${it.message}")
		})

	@OptIn(ExperimentalCoroutinesApi::class)
	fun loadContent(title: String, callback: (String) -> Unit = {}): Disposable =
		rxDataStore.data().subscribe {
			callback(it[stringPreferencesKey(title)] ?: "")
		}

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
		fun getInstance(context: Context): RxDataStore<Preferences> {
			return getManagerInstance(context).rxDataStore
		}

		@OptIn(ExperimentalCoroutinesApi::class)
		@Synchronized
		fun saveContent(context: Context, title: String, content: String, callback: () -> Unit = {}): Disposable =
			getManagerInstance(context).saveContent(title, content, callback)

		@OptIn(ExperimentalCoroutinesApi::class)
		@Synchronized
		fun loadContent(context: Context, title: String, callback: (String) -> Unit = {}): Disposable =
			getManagerInstance(context).loadContent(title, callback)
	}
}

/**
 * Hilt 依赖注入模块，向 Hilt 提供 [DataStoreManager] 与 [RxDataStore<Preferences>] 依赖
 */
@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {

	@Provides
	@Singleton
	fun provideRxDataStore(@ApplicationContext context: Context): RxDataStore<Preferences> {
		return DataStoreManager.getInstance(context)
	}
}
