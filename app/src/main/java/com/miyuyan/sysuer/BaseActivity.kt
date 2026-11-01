package com.miyuyan.sysuer

import android.content.Context
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.miyuyan.sysuer.api.Config
import com.miyuyan.sysuer.api.SettingManager

open class BaseActivity : AppCompatActivity() {
	val settingManager: SettingManager by lazy { SettingManager.getInstance(this) }
	val config: Config by lazy { Config(this) }
	override fun attachBaseContext(context: Context) {
		super.attachBaseContext(settingManager.setFontSize(settingManager.fontSize))
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		enableEdgeToEdge()
	}

	override fun onDestroy() {
		super.onDestroy()
		config.contextUtil.dispose()
	}
}