package com.sysu.edu

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.sysu.edu.api.Config
import com.sysu.edu.api.SettingManager

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