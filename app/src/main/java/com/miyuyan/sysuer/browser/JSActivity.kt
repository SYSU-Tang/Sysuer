package com.miyuyan.sysuer.browser

import android.os.Bundle
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI.setupWithNavController
import com.miyuyan.sysuer.BaseActivity
import com.miyuyan.sysuer.R
import com.miyuyan.sysuer.databinding.ActivityJsActivityBinding

class JSActivity : BaseActivity() {
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		val binding = ActivityJsActivityBinding.inflate(layoutInflater)
		setContentView(binding.root)
		val fragment = supportFragmentManager.findFragmentById(R.id.fragment) as NavHostFragment
		setupWithNavController(binding.toolbar, fragment.navController, AppBarConfiguration.Builder()
			.setFallbackOnNavigateUpListener {
				supportFinishAfterTransition()
				false
			}
			.build())
		menuInflater.inflate(R.menu.editor, binding.toolbar.menu)
		intent.getLongExtra("id", -1L).takeIf { it > 0 }?.let{
			fragment.navController.navigate(R.id.list_to_info, Bundle().apply {
				putLong("id", it)
			})
		}
	}
}