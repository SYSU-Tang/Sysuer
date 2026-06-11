package com.sysu.edu

import android.content.Context
import android.os.Bundle
import androidx.activity.BackEventCompat
import androidx.activity.OnBackPressedCallback
import androidx.activity.OnBackPressedDispatcher
import androidx.appcompat.app.AppCompatActivity
import com.sysu.edu.api.SettingManager

open class BaseActivity : AppCompatActivity() {
	lateinit var settingManager: SettingManager
	override fun attachBaseContext(context: Context) {
		settingManager = SettingManager(context).apply {
			setLanguage()
			setTheme()
			super.attachBaseContext(setFontSize(getFontSize()))
		}
	}
	
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		
	}
}