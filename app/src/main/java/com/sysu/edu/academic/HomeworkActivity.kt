package com.sysu.edu.academic

import android.os.Bundle
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI.setupWithNavController
import com.sysu.edu.BaseActivity
import com.sysu.edu.R
import com.sysu.edu.databinding.ActivityHomeWorkBinding

class HomeworkActivity : BaseActivity() {
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		val binding = ActivityHomeWorkBinding.inflate(layoutInflater)
		setContentView(binding.getRoot())
		val navHostFragment = supportFragmentManager.findFragmentById(R.id.fragment) as NavHostFragment
		 setupWithNavController(binding.bottomNav, navHostFragment.navController)
		binding.toolbar.setNavigationOnClickListener { supportFinishAfterTransition() }
	}
}