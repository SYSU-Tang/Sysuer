package com.sysu.edu

import android.content.Context
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import com.sysu.edu.api.Params

open class BaseActivity : AppCompatActivity() {
	private var defaultFontSize: Float = 0f
	var sysuerParams: Params? = null
	override fun attachBaseContext(context: Context) {
		val configuration: Configuration = context.resources.configuration
		if (defaultFontSize == 0f) defaultFontSize = configuration.fontScale
		PreferenceManager.getDefaultSharedPreferences(context).getString("fontSize", "0")?.let {
			configuration.fontScale = (if ("0" != it) floatArrayOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f)[it.toInt() - 1]
			else defaultFontSize)
		}
		super.attachBaseContext(context.createConfigurationContext(configuration))
	}
	override fun onDestroy() {
		super.onDestroy()
		sysuerParams?.contextUtil?.dispose()
	}
}