package com.sysu.edu.academic

import android.os.Bundle
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI.setupWithNavController
import com.sysu.edu.BaseActivity
import com.sysu.edu.R
import com.sysu.edu.databinding.ActivityEvaluationBinding

class EvaluationActivity : BaseActivity() {
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		val binding = ActivityEvaluationBinding.inflate(layoutInflater)
		setContentView(binding.getRoot())
		setupWithNavController(binding.toolbar, (supportFragmentManager.findFragmentById(R.id.fragment) as NavHostFragment).navController, AppBarConfiguration.Builder()
			.setFallbackOnNavigateUpListener {
				supportFinishAfterTransition()
				true
			}
			.build())
	}
}
