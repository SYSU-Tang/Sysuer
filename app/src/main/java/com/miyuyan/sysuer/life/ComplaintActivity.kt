package com.miyuyan.sysuer.life

import android.os.Bundle
import android.view.MenuItem
import androidx.viewpager2.widget.ViewPager2
import com.miyuyan.sysuer.BaseActivity
import com.miyuyan.sysuer.R
import com.miyuyan.sysuer.databinding.ActivityComplaintBinding
import com.miyuyan.sysuer.view.Pager2Adapter

class ComplaintActivity : BaseActivity() {
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		val adapter = Pager2Adapter(this).add(ComplaintMainFragment())
			.add(ComplaintResponseFragment())
			.add(ComplaintSquareFragment())
		val itemIds = listOf(R.id.complaint, R.id.response, R.id.square)
		ActivityComplaintBinding.inflate(layoutInflater).apply {
			toolbar.setNavigationOnClickListener { supportFinishAfterTransition() }
			pager.adapter = adapter
			pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
				override fun onPageSelected(position: Int) {
					if (position < itemIds.size) bottomNav.selectedItemId = itemIds[position]
				}
			})
			bottomNav.setOnItemSelectedListener { item: MenuItem ->
				val currentItem = itemIds.indexOf(item.itemId)
				if (currentItem in 0..<adapter.itemCount) {
					pager.currentItem = currentItem
					toolbar.setTitle(item.title)
				}
				true
			}
		}.also {
			setContentView(it.root)
		}
	}
}