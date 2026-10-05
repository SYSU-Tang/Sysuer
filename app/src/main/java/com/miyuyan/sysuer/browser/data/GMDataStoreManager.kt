package com.miyuyan.sysuer.browser.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import okio.Path.Companion.toPath
import java.io.File

/**
 * Singleton manager for Greasemonkey scripts DataStore to avoid "multiple DataStores active" error.
 */
object GMDataStoreManager {
	private var dataStore: DataStore<Preferences>? = null

	@Synchronized
	fun getInstance(context: Context): DataStore<Preferences> {
		if (dataStore == null) {
			dataStore = PreferenceDataStoreFactory.createWithPath(
				produceFile = { File(context.filesDir, "datastore/gm_scripts_data.preferences_pb").absolutePath.toPath() }
			)
		}
		return dataStore!!
	}
}
