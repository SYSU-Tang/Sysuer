package com.miyuyan.sysuer.browser

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.SharedTransitionLayout
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import com.miyuyan.sysuer.BaseActivity
import com.miyuyan.sysuer.nav.JsDetail
import com.miyuyan.sysuer.nav.JsList
import com.miyuyan.sysuer.nav.SysuerNavDisplay
import com.miyuyan.sysuer.theme.SysuerTheme

class JsActivity : BaseActivity() {
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		enableEdgeToEdge()
		setContent {
			SysuerTheme(settingManager) {
				SharedTransitionLayout {
					val initialId = intent.getLongExtra("id", -1L)
					val backStack = rememberNavBackStack(
							if (initialId > 0) JsDetail(initialId)
							else JsList(autoAdd = intent.getStringExtra("operation") == "add")
					)
					SysuerNavDisplay(backStack = backStack, entryProvider = entryProvider {
						entry<JsList> { key ->
							JsListRoute(backStack, key, this@SharedTransitionLayout, LocalNavAnimatedContentScope.current)
						}
						entry<JsDetail> { key ->
							JsDetailRoute(backStack, key, this@SharedTransitionLayout, LocalNavAnimatedContentScope.current)
						}
					})
				}
			}
		}
	}
}
