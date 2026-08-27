package com.sysu.edu.browser

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.SharedTransitionLayout
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import com.sysu.edu.BaseActivity
import com.sysu.edu.nav.RichText
import com.sysu.edu.nav.SysuerNavDisplay
import com.sysu.edu.theme.SysuerTheme
import kotlinx.coroutines.ExperimentalCoroutinesApi

class RichTextActivity : BaseActivity() {
	@OptIn(ExperimentalCoroutinesApi::class) override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		enableEdgeToEdge()
		setContent {
			val backStack = rememberNavBackStack(RichText(intent.getStringExtra("title") ?: "", intent.getStringExtra("type")))
			SysuerTheme {
				SharedTransitionLayout {
					SysuerNavDisplay(backStack = backStack, entryProvider = entryProvider {
						entry<RichText> {
							RichTextRoute(backStack, it, sharedTransitionScope = this@SharedTransitionLayout, animatedVisibilityScope = LocalNavAnimatedContentScope.current)
						}
					})
				}
			}
		}
	}
}
