package com.sysu.edu.life

import android.os.Bundle
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.sysu.edu.BaseActivity
import com.sysu.edu.R
import com.sysu.edu.databinding.ActivityPagerBinding
import com.sysu.edu.view.Pager2Adapter

class PayActivity : BaseActivity() {
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		val adp = Pager2Adapter(this)
		(0..<5).forEach { adp.add(PayFragment.newInstance(it)) }
		ActivityPagerBinding.inflate(layoutInflater).apply {
			toolbar.setTitle(R.string.pay)
			pager.adapter = adp
			TabLayoutMediator(tabLayout, pager) { tab: TabLayout.Tab?, position: Int -> tab?.text = arrayOf("待交费用", "选交费用", "交费情况", "付款记录", "退费记录")[position] }.attach()
			toolbar.setNavigationOnClickListener { supportFinishAfterTransition() }
		}.also{
			setContentView(it.root)
		}
	}
}