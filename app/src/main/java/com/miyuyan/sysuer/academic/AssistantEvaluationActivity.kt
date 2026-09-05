package com.miyuyan.sysuer.academic

import android.os.Bundle
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI.setupWithNavController
import com.miyuyan.sysuer.BaseActivity
import com.miyuyan.sysuer.R
import com.miyuyan.sysuer.databinding.ActivityAssistantEvaluationResultBinding

class AssistantEvaluationActivity : BaseActivity() {
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		val binding = ActivityAssistantEvaluationResultBinding.inflate(layoutInflater)
		setContentView(binding.root)
		val fragment = supportFragmentManager.findFragmentById(R.id.fragment) as NavHostFragment?
		if (fragment != null) setupWithNavController(binding.toolbar, fragment.navController, AppBarConfiguration.Builder()
			.setFallbackOnNavigateUpListener {
				supportFinishAfterTransition()
				false
			}
			.build())
	}
}