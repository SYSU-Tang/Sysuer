package com.sysu.edu.life

import android.os.Bundle
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI.setupWithNavController
import com.sysu.edu.BaseActivity
import com.sysu.edu.R
import com.sysu.edu.databinding.ActivityGymPreservationBinding

class GymReservationActivity : BaseActivity() {
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		val binding = ActivityGymPreservationBinding.inflate(layoutInflater)
		setContentView(binding.root)
		val fragment = supportFragmentManager.findFragmentById(R.id.fragment) as NavHostFragment
		setupWithNavController(binding.toolbar, fragment.navController, AppBarConfiguration.Builder()
			.setFallbackOnNavigateUpListener {
				supportFinishAfterTransition()
				false
			}.build())
		setupWithNavController(binding.bottomNavigation, fragment.navController)
	}
}