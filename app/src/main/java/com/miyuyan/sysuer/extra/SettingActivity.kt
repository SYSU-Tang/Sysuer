package com.miyuyan.sysuer.extra

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.animation.SharedTransitionLayout
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import com.miyuyan.sysuer.BaseActivity
import com.miyuyan.sysuer.browser.BrowserRoute
import com.miyuyan.sysuer.browser.JsDetailRoute
import com.miyuyan.sysuer.browser.JsListRoute
import com.miyuyan.sysuer.nav.Browser
import com.miyuyan.sysuer.nav.Developer
import com.miyuyan.sysuer.nav.JsDetail
import com.miyuyan.sysuer.nav.JsList
import com.miyuyan.sysuer.nav.Setting
import com.miyuyan.sysuer.nav.SysuerNavDisplay
import com.miyuyan.sysuer.theme.SysuerTheme

class SettingActivity : BaseActivity() {
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContent {
			SysuerTheme(settingManager) {
				val backStack = rememberNavBackStack(Setting)
				SharedTransitionLayout {
					SysuerNavDisplay(backStack = backStack, entryProvider = entryProvider {
						entry<Setting> {
							SettingRoute(
									backStack,
									sharedTransitionScope = this@SharedTransitionLayout,
									animatedVisibilityScope = LocalNavAnimatedContentScope.current
							)
						}
						entry<Developer> {
							DevelopRoute(
									backStack,
									sharedTransitionScope = this@SharedTransitionLayout,
									animatedVisibilityScope = LocalNavAnimatedContentScope.current
							)
						}
						entry<JsList> { key ->
							JsListRoute(
									backStack,
									key,
									sharedTransitionScope = this@SharedTransitionLayout,
									animatedVisibilityScope = LocalNavAnimatedContentScope.current
							)
						}
						entry<JsDetail> { key ->
							JsDetailRoute(
									backStack,
									key,
									sharedTransitionScope = this@SharedTransitionLayout,
									animatedVisibilityScope = LocalNavAnimatedContentScope.current
							)
						}
						entry<Browser> {
							BrowserRoute(
									backStack,
									it,
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
