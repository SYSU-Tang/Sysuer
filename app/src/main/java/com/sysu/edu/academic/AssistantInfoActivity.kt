package com.sysu.edu.academic

import android.os.Bundle
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI.setupWithNavController
import com.sysu.edu.BaseActivity
import com.sysu.edu.R
import com.sysu.edu.databinding.ActivityAssistantInfoBinding

class AssistantInfoActivity : BaseActivity() {
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		val binding = ActivityAssistantInfoBinding.inflate(layoutInflater)
		setContentView(binding.getRoot())
		val navController = (supportFragmentManager.findFragmentById(R.id.fragment) as NavHostFragment).navController
		setupWithNavController(binding.toolbar, navController, AppBarConfiguration.Builder()
			.setFallbackOnNavigateUpListener {
				supportFinishAfterTransition()
				false
			}
			.build())
	}
}