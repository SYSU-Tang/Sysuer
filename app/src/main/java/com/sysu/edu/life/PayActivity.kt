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
		val tabs = resources.getStringArray(R.array.payment_key)
		(0..<5).forEach { adp.add(PayFragment.newInstance(it)) }
		ActivityPagerBinding.inflate(layoutInflater).apply {
			toolbar.setTitle(R.string.pay_fee)
			pager.adapter = adp
			TabLayoutMediator(tabLayout, pager) { tab: TabLayout.Tab?, position: Int -> tab?.text = tabs[position] }.attach()
			toolbar.setNavigationOnClickListener { supportFinishAfterTransition() }
		}.also{
			setContentView(it.root)
		}
	}
}