package com.miyuyan.sysuer.extra

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.animation.SharedTransitionLayout
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import com.miyuyan.sysuer.BaseActivity
import com.miyuyan.sysuer.nav.Privacy
import com.miyuyan.sysuer.nav.SysuerNavDisplay
import com.miyuyan.sysuer.theme.SysuerTheme

class PrivacyActivity : BaseActivity() {
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContent {
			SysuerTheme(settingManager) {
				val backStack = rememberNavBackStack(Privacy)
				SharedTransitionLayout {
					SysuerNavDisplay(backStack = backStack, entryProvider = entryProvider {
						entry<Privacy> {
							PrivacyRoute(
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
