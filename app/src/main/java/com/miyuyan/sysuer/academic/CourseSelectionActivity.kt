package com.miyuyan.sysuer.academic

import android.os.Bundle
import android.view.WindowManager
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.miyuyan.sysuer.BaseActivity
import com.miyuyan.sysuer.R
import com.miyuyan.sysuer.databinding.ActivityCourseSelectionBinding
import com.miyuyan.sysuer.view.Pager2Adapter

class CourseSelectionActivity : BaseActivity() {
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		val binding = ActivityCourseSelectionBinding.inflate(layoutInflater)
		binding.toolbar.setNavigationOnClickListener { supportFinishAfterTransition() }
		setContentView(binding.getRoot())
		val pager2Adapter = Pager2Adapter(this)
		binding.pager2.setAdapter(pager2Adapter)
		TabLayoutMediator(binding.tab, binding.pager2) { tab: TabLayout.Tab?, position: Int ->
			tab!!.setText(mutableListOf(R.string.course_selection, R.string.preview, R.string.course_selected)[position])
		}.attach()
		(0..<3).forEach { pager2Adapter.add(CourseSelectionContainerFragment.newInstance(it)) }
		window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)
	}
}