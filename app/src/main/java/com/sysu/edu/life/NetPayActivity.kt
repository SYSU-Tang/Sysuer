package com.sysu.edu.life

import android.os.Bundle
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI.setupWithNavController
import com.sysu.edu.BaseActivity
import com.sysu.edu.R
import com.sysu.edu.databinding.ActivityNetPayBinding

class NetPayActivity : BaseActivity() {
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		val binding = ActivityNetPayBinding.inflate(layoutInflater)
		setContentView(binding.root)
		val navHostFragment = supportFragmentManager.findFragmentById(R.id.net_pay_fragment) as NavHostFragment
		setupWithNavController(binding.bottomNav, navHostFragment.navController)
		binding.toolbar.setNavigationOnClickListener { supportFinishAfterTransition() }
	}
}