package com.miyuyan.sysuer.academic

import android.os.Bundle
import android.view.WindowManager
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI.setupWithNavController
import com.miyuyan.sysuer.BaseActivity
import com.miyuyan.sysuer.R
import com.miyuyan.sysuer.databinding.ActivityCourseQueryBinding

class CourseQueryActivity : BaseActivity() {
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		val binding = ActivityCourseQueryBinding.inflate(layoutInflater)
		setContentView(binding.getRoot())
		binding.toolbar.setNavigationOnClickListener { supportFinishAfterTransition() }
		window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)
		setupWithNavController(binding.toolbar, (this.supportFragmentManager.findFragmentById(R.id.fragment) as NavHostFragment).navController, AppBarConfiguration.Builder()
			.setFallbackOnNavigateUpListener {
				supportFinishAfterTransition()
				false
			}
			.build())
	}
}