package com.miyuyan.sysuer.extra

import android.os.Bundle
import android.view.MenuItem
import com.miyuyan.sysuer.BaseActivity
import com.miyuyan.sysuer.R
import com.miyuyan.sysuer.api.ContextUtil
import com.miyuyan.sysuer.databinding.ActivityPrivacyBinding

class PrivacyActivity : BaseActivity() {
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		val contextUtil = ContextUtil(this)
		ActivityPrivacyBinding.inflate(layoutInflater).apply {
			setContentView(root)
			toolbar.setNavigationOnClickListener {  supportFinishAfterTransition() }
			toolbar.menu.add(R.string.edit).setIcon(R.drawable.edit).setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_IF_ROOM).setOnMenuItemClickListener { _: MenuItem? ->
				contextUtil.changeAccount(null, "sysu.edu.cn", null,null)
				false
			}
		}
	}
}