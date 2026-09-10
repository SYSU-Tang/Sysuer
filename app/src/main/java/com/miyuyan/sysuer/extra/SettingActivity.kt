package com.miyuyan.sysuer.extra

import android.os.Bundle
import com.miyuyan.sysuer.BaseActivity
import com.miyuyan.sysuer.databinding.ActivitySettingBinding

class SettingActivity : BaseActivity() {
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		if (savedInstanceState != null) setResult(RESULT_OK)
		setContentView(ActivitySettingBinding.inflate(layoutInflater).apply {
			toolbar.setNavigationOnClickListener { supportFinishAfterTransition() }
		}.root)
	}
}