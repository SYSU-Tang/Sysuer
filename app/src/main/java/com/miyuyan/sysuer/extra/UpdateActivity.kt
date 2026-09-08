package com.miyuyan.sysuer.extra

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.animation.SharedTransitionLayout
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import com.miyuyan.sysuer.BaseActivity
import com.miyuyan.sysuer.nav.SysuerNavDisplay
import com.miyuyan.sysuer.nav.Update
import com.miyuyan.sysuer.theme.SysuerTheme

class UpdateActivity : BaseActivity() {
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContent {
			SysuerTheme(settingManager) {
				val backStack = rememberNavBackStack(Update)
				SharedTransitionLayout {
					SysuerNavDisplay(backStack = backStack, entryProvider = entryProvider {
						entry<Update> {
							UpdateRoute(
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
