package com.sysu.edu.academic

import android.os.Bundle
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI.setupWithNavController
import com.sysu.edu.BaseActivity
import com.sysu.edu.R
import com.sysu.edu.databinding.ActivityTrainingScheduleBinding

class TrainingProgramActivity : BaseActivity() {
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		val binding = ActivityTrainingScheduleBinding.inflate(layoutInflater)
		setContentView(binding.getRoot())
		binding.toolbar.setNavigationOnClickListener { supportFinishAfterTransition() }
		setupWithNavController(binding.toolbar, (this.supportFragmentManager.findFragmentById(R.id.fragment) as NavHostFragment).navController, AppBarConfiguration.Builder()
			.setFallbackOnNavigateUpListener {
				supportFinishAfterTransition()
				true
			}.build())
	}
}