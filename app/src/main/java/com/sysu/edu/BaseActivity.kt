package com.sysu.edu

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import com.sysu.edu.api.Params
import com.sysu.edu.preference.SettingManager

open class BaseActivity : AppCompatActivity() {
	var sysuerParams: Params? = null
	override fun attachBaseContext(context: Context) {
		SettingManager(context).apply {
			setLanguage()
			setTheme()
			super.attachBaseContext(setFontSize(getFontSize()))
		}
	}
	
	override fun onDestroy() {
		super.onDestroy()
		sysuerParams?.contextUtil?.dispose()
	}
}