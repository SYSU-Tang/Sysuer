package com.miyuyan.sysuer.extra

import android.os.Bundle
import android.view.View
import com.miyuyan.sysuer.BaseActivity
import com.miyuyan.sysuer.databinding.ActivitySettingBinding

class SettingActivity : BaseActivity() {
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		if (savedInstanceState != null) setResult(RESULT_OK)
		ActivitySettingBinding.inflate(layoutInflater).apply {
			setContentView(getRoot())
			toolbar.setNavigationOnClickListener { _: View? -> supportFinishAfterTransition() }
			/*OnBackPressedDispatcher().addCallback(this@SettingActivity, object : OnBackPressedCallback(true) {
				override fun handleOnBackPressed() {
					supportFinishAfterTransition()
					isEnabled = false
				}
				override fun handleOnBackCancelled() {
					root.apply {
						alpha = 1f
						scaleX = 1f
						scaleY = 1f
					}
					super.handleOnBackCancelled()
				}
				override fun handleOnBackStarted(backEvent: BackEventCompat) {
					println(backEvent)
					val progress = backEvent.progress
					val scale = 1f - (progress * 0.7f)
					val alpha = 1f - (progress * 0.8f)
					root.apply {
						this.alpha = alpha
						scaleX = scale
						scaleY = scale
					}
					super.handleOnBackStarted(backEvent)
				}
			})*/
		}
	}
}