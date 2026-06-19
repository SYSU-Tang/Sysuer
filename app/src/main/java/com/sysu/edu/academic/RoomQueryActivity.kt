package com.sysu.edu.academic

import android.os.Bundle
import android.view.WindowManager
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI.setupWithNavController
import com.sysu.edu.BaseActivity
import com.sysu.edu.R
import com.sysu.edu.databinding.ActivityRoomQueryBinding

class RoomQueryActivity : BaseActivity() {
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		val binding = ActivityRoomQueryBinding.inflate(layoutInflater)
		setContentView(binding.getRoot())
		window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)
		setupWithNavController(binding.toolbar, (supportFragmentManager.findFragmentById(R.id.fragment) as NavHostFragment).navController, AppBarConfiguration.Builder()
			.setFallbackOnNavigateUpListener {
				supportFinishAfterTransition()
				false
			}.build())
	}
}