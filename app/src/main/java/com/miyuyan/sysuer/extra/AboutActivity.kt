package com.miyuyan.sysuer.extra

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.animation.SharedTransitionLayout
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import com.miyuyan.sysuer.BaseActivity
import com.miyuyan.sysuer.nav.About
import com.miyuyan.sysuer.nav.SysuerNavDisplay
import com.miyuyan.sysuer.theme.SysuerTheme

class AboutActivity : BaseActivity() {
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContent {
			SysuerTheme(settingManager) {
				val backStack = rememberNavBackStack(About)
				SharedTransitionLayout {
					SysuerNavDisplay(backStack = backStack, entryProvider = entryProvider {
						entry<About> {
							AboutRoute(
								backStack,
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
