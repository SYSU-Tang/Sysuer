package com.miyuyan.sysuer.api

import android.content.Context
import android.util.Base64
import android.util.Pair
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.rxjava3.RxPreferenceDataStoreBuilder
import com.google.crypto.tink.Aead
import com.google.crypto.tink.KeyTemplates
import com.google.crypto.tink.RegistryConfiguration
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.integration.android.AndroidKeysetManager
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlin.concurrent.Volatile

@OptIn(ExperimentalCoroutinesApi::class)
class AccountManager private constructor(context: Context) {
	private val dataStore = RxPreferenceDataStoreBuilder(context, "accounts").build()
	private val aead: Aead
	
	init {
		try {
			AeadConfig.register()
			aead = AndroidKeysetManager.Builder()
				.withSharedPref(context, "master_keyset", "secure_keys")
				.withKeyTemplate(KeyTemplates.get("AES256_GCM"))
				.withMasterKeyUri("android-keystore://master_key")
				.build()
				.keysetHandle
				.getPrimitive(RegistryConfiguration.get(), Aead::class.java)
		} catch (e: Exception) {
			throw RuntimeException("Tink 初始化失败", e)
		}
	}
	
	fun getActiveAccountSync(domain: String?): Pair<String?, String?>? {
		val username = dataStore.data().blockingFirst()[stringPreferencesKey("active:$domain")] ?: return null
		val password = getPasswordSync(domain, username)
		return Pair<String?, String?>(username, password)
	}
	
	fun getPasswordSync(domain: String?, username: String?): String? {
		val encoded = dataStore.data().blockingFirst()[stringPreferencesKey("$domain:$username")] ?: return null
		return try {
			String(aead.decrypt(Base64.decode(encoded, Base64.DEFAULT), null))
		} catch (_: Exception) {
			null
		}
	}
	
	fun getActiveAccountAsync(domain: String): Single<Pair<String?, String?>> {
		return dataStore.data().firstOrError()
			.map<Pair<String?, String?>> { prefs: Preferences ->
				val username = prefs[stringPreferencesKey("active:$domain")] ?: return@map Pair<String?, String?>("", "")
				val encoded = prefs[stringPreferencesKey("$domain:$username")] ?: return@map Pair<String?, String?>("", "")
				val password = String(aead.decrypt(Base64.decode(encoded, Base64.DEFAULT), null))
				Pair<String?, String?>(username, password)
			}
			.subscribeOn(Schedulers.io())
			.observeOn(AndroidSchedulers.mainThread())
	}
	
	fun setAccountAsync(domain: String, username: String, password: String): Completable {
		return setAccountAsync(domain, username, password, false)
	}
	
	fun setAccountAsync(domain: String, username: String, password: String, active: Boolean): Completable {
		return dataStore.updateDataAsync { prefs: Preferences? ->
			Single.just(prefs!!.toMutablePreferences()
							.also {
								it[stringPreferencesKey("$domain:$username")] = Base64.encodeToString(aead.encrypt(password.toByteArray(), null), Base64.DEFAULT)
							}
							.also {
								if (active || !it.contains(stringPreferencesKey("active:$domain")))
									it[stringPreferencesKey("active:$domain")] = username
							})
		}.ignoreElement()
			.subscribeOn(Schedulers.io())
	}
	
	fun setActiveAccountAsync(domain: String?, username: String?): Completable {
		return dataStore.updateDataAsync { prefs: Preferences? ->
			Single.just(prefs!!.toMutablePreferences()
							.also { it[stringPreferencesKey("active:$domain")] = username as String })
		}.ignoreElement()
			.subscribeOn(Schedulers.io())
	}
	
	fun removeAccountAsync(domain: String, username: String): Completable {
		return dataStore.updateDataAsync { prefs: Preferences? ->
			Single.just(prefs!!.toMutablePreferences()
							.also { it.remove(stringPreferencesKey("$domain:$username")) })
		}.ignoreElement()
			.subscribeOn(Schedulers.io())
	}
	
	fun removeActiveAccountAsync(domain: String): Completable {
		return dataStore.updateDataAsync { prefs: Preferences? ->
			Single.just(prefs!!.toMutablePreferences()
							.also { it.remove(stringPreferencesKey("active:$domain")) })
		}.ignoreElement()
			.subscribeOn(Schedulers.io())
	}
	
	companion object {
		@Volatile
		private var INSTANCE: AccountManager? = null
		fun getInstance(context: Context): AccountManager {
			if (INSTANCE == null) synchronized(AccountManager::class.java) {
				if (INSTANCE == null)
					INSTANCE = AccountManager(context.applicationContext)
			}
			return INSTANCE!!
		}
	}
}
