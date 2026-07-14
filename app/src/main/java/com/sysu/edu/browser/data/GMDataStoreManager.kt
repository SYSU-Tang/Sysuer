package com.sysu.edu.browser.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.rxjava3.RxPreferenceDataStoreBuilder
import androidx.datastore.rxjava3.RxDataStore

/**
 * Singleton manager for Greasemonkey scripts DataStore to avoid "multiple DataStores active" error.
 */
object GMDataStoreManager {
    private var dataStore: RxDataStore<Preferences>? = null

    @Synchronized
    fun getInstance(context: Context): RxDataStore<Preferences> {
        if (dataStore == null) {
            dataStore = RxPreferenceDataStoreBuilder(
                context.applicationContext,
                "gm_scripts_data"
            ).build()
        }
        return dataStore!!
    }
}
