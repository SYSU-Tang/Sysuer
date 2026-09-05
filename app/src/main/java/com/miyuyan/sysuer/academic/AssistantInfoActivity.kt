package com.miyuyan.sysuer.academic

import android.os.Bundle
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI.setupWithNavController
import com.miyuyan.sysuer.BaseActivity
import com.miyuyan.sysuer.R
import com.miyuyan.sysuer.databinding.ActivityAssistantInfoBinding

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