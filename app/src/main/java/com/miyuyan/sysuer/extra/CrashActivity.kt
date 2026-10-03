package com.miyuyan.sysuer.extra

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.animation.SharedTransitionLayout
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import com.miyuyan.sysuer.BaseActivity
import com.miyuyan.sysuer.nav.Crash
import com.miyuyan.sysuer.nav.SysuerNavDisplay
import com.miyuyan.sysuer.theme.SysuerTheme

class CrashActivity : BaseActivity() {
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		val crashInfo = intent.getStringExtra("crash") ?: ""
		setContent {
			SysuerTheme(settingManager) {
				val backStack = rememberNavBackStack(Crash(crashInfo))
				SharedTransitionLayout {
					SysuerNavDisplay(backStack = backStack, entryProvider = entryProvider {
						entry<Crash> {
							CrashRoute(
									backStack = backStack,
									navKey = it,
									sharedTransitionScope = this@SharedTransitionLayout,
									animatedVisibilityScope = LocalNavAnimatedContentScope.current
							)
						}
					})
				}
			}
		}
	}
}