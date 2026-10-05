package com.miyuyan.sysuer.browser

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.SharedTransitionLayout
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import com.miyuyan.sysuer.BaseActivity
import com.miyuyan.sysuer.nav.Browser
import com.miyuyan.sysuer.nav.JsDetail
import com.miyuyan.sysuer.nav.JsList
import com.miyuyan.sysuer.nav.SysuerNavDisplay
import com.miyuyan.sysuer.theme.SysuerTheme

class BrowserActivity : BaseActivity() {
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		enableEdgeToEdge()
		setContent {
			SysuerTheme(settingManager) {
				SharedTransitionLayout {
					val backStack = rememberNavBackStack(
							Browser(
									url = intent.dataString ?: "https://www.sysu.edu.cn/",
									content = intent.getStringExtra("data"),
							)
					)
					SysuerNavDisplay(backStack = backStack, entryProvider = entryProvider {
						entry<Browser> { key ->
							BrowserRoute(
									backStack = backStack,
									navKey = key,
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
					})
				}
			}
		}
	}
}
