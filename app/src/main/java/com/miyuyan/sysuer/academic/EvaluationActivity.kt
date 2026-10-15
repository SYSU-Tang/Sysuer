package com.miyuyan.sysuer.academic

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.SharedTransitionLayout
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import com.miyuyan.sysuer.BaseActivity
import com.miyuyan.sysuer.nav.Evaluation
import com.miyuyan.sysuer.nav.SysuerNavDisplay
import com.miyuyan.sysuer.theme.SysuerTheme

/**
 * 教学评价入口：仅负责承载 [EvaluationRoute]，三级下钻由页面内部的层级状态接管。
 */
class EvaluationActivity : BaseActivity() {
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		enableEdgeToEdge()
		setContent {
			val backStack = rememberNavBackStack(Evaluation)
			SysuerTheme(settingManager) {
				SharedTransitionLayout {
					SysuerNavDisplay(backStack = backStack, entryProvider = entryProvider {
						entry<Evaluation> {
							EvaluationRoute(
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
