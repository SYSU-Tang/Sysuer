package com.sysu.edu.extra

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import com.sysu.edu.BaseActivity
import com.sysu.edu.R
import com.sysu.edu.api.ContextUtil
import com.sysu.edu.databinding.ActivityPrivacyBinding

class PrivacyActivity : BaseActivity() {
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		val contextUtil = ContextUtil(this)
		ActivityPrivacyBinding.inflate(layoutInflater).apply {
			setContentView(getRoot())
			toolbar.setNavigationOnClickListener { _: View? -> supportFinishAfterTransition() }
			toolbar.getMenu().add(R.string.edit).setIcon(R.drawable.edit).setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_IF_ROOM).setOnMenuItemClickListener { _: MenuItem? ->
				contextUtil.changeAccount(null, "sysu.edu.cn", null)
				false
			}
		}
	}
}