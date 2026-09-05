package com.miyuyan.sysuer

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.miyuyan.sysuer.api.Config
import com.miyuyan.sysuer.api.SettingManager

open class BaseActivity : AppCompatActivity() {
	lateinit var settingManager: SettingManager
	lateinit var config: Config
	override fun attachBaseContext(context: Context) {
		settingManager = SettingManager(context).apply {
			setLanguage()
			setTheme()
			super.attachBaseContext(setFontSize(fontSize))
		}
	}
	
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		config = Config(this)
	}
	
	override fun onDestroy() {
		super.onDestroy()
		config.contextUtil.disposable.dispose()
	}
}