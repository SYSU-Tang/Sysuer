package com.miyuyan.sysuer.academic

import android.os.Bundle
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI.setupWithNavController
import com.miyuyan.sysuer.BaseActivity
import com.miyuyan.sysuer.R
import com.miyuyan.sysuer.databinding.ActivityHomeWorkBinding

class HomeworkActivity : BaseActivity() {
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		val binding = ActivityHomeWorkBinding.inflate(layoutInflater)
		setContentView(binding.root)
		val navHostFragment = supportFragmentManager.findFragmentById(R.id.fragment) as NavHostFragment
		setupWithNavController(binding.bottomNav, navHostFragment.navController)
		binding.toolbar.setNavigationOnClickListener { supportFinishAfterTransition() }
	}
}