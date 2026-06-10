package com.sysu.edu.extra

import android.os.Bundle
import android.view.View
import com.sysu.edu.BaseActivity
import com.sysu.edu.R
import com.sysu.edu.api.Params
import com.sysu.edu.databinding.ActivityInfoBinding

class AboutActivity : BaseActivity() {
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		val binding = ActivityInfoBinding.inflate(layoutInflater)
		setContentView(binding.getRoot())
		val click = ArrayList<Long?>()
		val params = Params(this)
		binding.toolbar.setNavigationOnClickListener { _: View? -> finishAfterTransition() }
		binding.icon.setOnClickListener { _: View? ->
			if (click.isEmpty() || System.currentTimeMillis() - click[click.size - 1]!! < 500) {
				if (click.size == 4) {
					params.toast(if (params.isDeveloper) R.string.developer_disabled else R.string.developer_enabled)
					params.isDeveloper = !params.isDeveloper
					click.clear()
				} else click.add(System.currentTimeMillis())
			} else click.clear()
		}
	}
}


