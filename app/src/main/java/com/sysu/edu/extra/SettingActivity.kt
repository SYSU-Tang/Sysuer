package com.sysu.edu.extra

import android.os.Bundle
import android.view.View
import com.sysu.edu.BaseActivity
import com.sysu.edu.databinding.ActivitySettingBinding

class SettingActivity : BaseActivity() {
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		if (savedInstanceState != null) setResult(RESULT_OK)
		ActivitySettingBinding.inflate(layoutInflater).apply {
			setContentView(getRoot())
			toolbar.setNavigationOnClickListener { _: View? -> supportFinishAfterTransition() }
		}
	}
}