package com.miyuyan.sysuer.api

import android.content.Context
import android.util.Base64
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.google.crypto.tink.Aead
import com.google.crypto.tink.KeyTemplates
import com.google.crypto.tink.RegistryConfiguration
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.integration.android.AndroidKeysetManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okio.Path.Companion.toPath
import java.io.File
import kotlin.concurrent.Volatile

class AccountManager private constructor(context: Context) {
	private val dataStore: DataStore<Preferences> = PreferenceDataStoreFactory.createWithPath(
		produceFile = { File(context.filesDir, "datastore/accounts.preferences_pb").absolutePath.toPath() }
	)
	private val aead: Aead

	init {
		try {
			AeadConfig.register()
			aead = AndroidKeysetManager.Builder()
				.withSharedPref(context, "master_keyset", "secure_keys")
				.withKeyTemplate(KeyTemplates.get("AES256_GCM"))
				.withMasterKeyUri("android-keystore://master_key")
				.build().keysetHandle.getPrimitive(RegistryConfiguration.get(), Aead::class.java)
		} catch (e: Exception) {
			throw RuntimeException("Tink 初始化失败", e)
		}
	}

	/**
	 * 同步获取 domain 域名下的活跃账号（阻塞当前线程，勿在主线程调用）
	 * @param domain 域名
	 * @return 活跃账号，<用户名, 密码>
	 * */
	fun getActiveAccountSync(domain: String?): Pair<String?, String?>? = runBlocking {
		val username =
			dataStore.data.first()[stringPreferencesKey("active:$domain")] ?: return@runBlocking null
		val password = getPasswordSync(domain, username)
		Pair(username, password)
	}

	/**
	 * 同步获取 domain 域名下的账号 username 的密码（阻塞当前线程，勿在主线程调用）
	 * @param domain 域名
	 * @param username 用户名
	 * @return 密码
	 * */
	fun getPasswordSync(domain: String?, username: String?): String? = runBlocking {
		val encoded = dataStore.data.first()[stringPreferencesKey("$domain:$username")]
			?: return@runBlocking null
		try {
			String(aead.decrypt(Base64.decode(encoded, Base64.DEFAULT), null))
		} catch (_: Exception) {
			null
		}
	}

	/**
	 * 异步获取 domain 域名下的活跃账号
	 * @param domain 域名
	 * @return 活跃账号，<用户名, 密码>，未设置时为 <"", "">
	 * */
	suspend fun getActiveAccount(domain: String): Pair<String, String> {
		val prefs = dataStore.data.first()
		val username = prefs[stringPreferencesKey("active:$domain")]
			?: return Pair("", "")
		val encoded = prefs[stringPreferencesKey("$domain:$username")]
			?: return Pair("", "")
		val password = try {
			String(aead.decrypt(Base64.decode(encoded, Base64.DEFAULT), null))
		} catch (_: Exception) {
			""
		}
		return Pair(username, password)
	}

	/**
	 * 异步设置账号，不设置为活跃账号
	 * @param domain 域名
	 * @param username 用户名
	 * @param password 密码
	 * */
	suspend fun setAccount(domain: String, username: String, password: String) {
		setAccount(domain, username, password, false)
	}

	/**
	 * 异步设置账号
	 * @param domain 域名
	 * @param username 用户名
	 * @param password 密码
	 * @param active 是否设置为活跃账号
	 * */
	suspend fun setAccount(
		domain: String, username: String, password: String, active: Boolean
	) {
		dataStore.edit { prefs ->
			prefs[stringPreferencesKey("$domain:$username")] = Base64.encodeToString(
				aead.encrypt(password.toByteArray(), null), Base64.DEFAULT
			)
			if (active || !prefs.contains(stringPreferencesKey("active:$domain"))) {
				prefs[stringPreferencesKey("active:$domain")] = username
			}
		}
	}

	/**
	 * 异步设置活跃账号
	 * @param domain 域名
	 * @param username 用户名
	 * */
	suspend fun setActiveAccount(domain: String, username: String) {
		dataStore.edit { it[stringPreferencesKey("active:$domain")] = username }
	}

	/**
	 * 异步删除账号
	 * @param domain 域名
	 * @param username 用户名
	 * */
	suspend fun removeAccount(domain: String, username: String) {
		dataStore.edit { it.remove(stringPreferencesKey("$domain:$username")) }
	}

	/**
	 * 异步删除活跃账号
	 * @param domain 域名
	 * */
	suspend fun removeActiveAccount(domain: String) {
		dataStore.edit { it.remove(stringPreferencesKey("active:$domain")) }
	}

	companion object {
		@Volatile
		private var INSTANCE: AccountManager? = null

		/**
		 * 获取 AccountManager 实例
		 * @param context 上下文对象
		 * */
		fun getInstance(context: Context): AccountManager =
			INSTANCE ?: synchronized(AccountManager::class.java) {
				INSTANCE ?: AccountManager(context.applicationContext).also { INSTANCE = it }
			}
	}
}
