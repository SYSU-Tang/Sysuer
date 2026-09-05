package com.miyuyan.sysuer.extra

import android.os.Bundle
import android.view.View
import com.miyuyan.sysuer.BaseActivity
import com.miyuyan.sysuer.R
import com.miyuyan.sysuer.databinding.ActivityInfoBinding

class AboutActivity : BaseActivity() {
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		val binding = ActivityInfoBinding.inflate(layoutInflater)
		setContentView(binding.root)
		val click = ArrayList<Long?>()
		binding.toolbar.setNavigationOnClickListener { _: View? -> finishAfterTransition() }
		binding.icon.setOnClickListener { _: View? ->
			if (click.isEmpty() || System.currentTimeMillis() - click[click.size - 1]!! < 500) {
				if (click.size == 4) {
					config.toast(if (settingManager.developerMode) R.string.developer_disabled else R.string.developer_enabled)
					settingManager.developerMode = !settingManager.developerMode
					click.clear()
				} else click.add(System.currentTimeMillis())
			} else click.clear()
		}
	}
}


