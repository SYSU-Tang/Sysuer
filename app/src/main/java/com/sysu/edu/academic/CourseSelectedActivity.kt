package com.sysu.edu.academic

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.SharedTransitionLayout
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import com.sysu.edu.BaseActivity
import com.sysu.edu.browser.RichTextRoute
import com.sysu.edu.nav.CourseDetail
import com.sysu.edu.nav.CourseSelected
import com.sysu.edu.nav.RichText
import com.sysu.edu.nav.SysuerNavDisplay

class CourseSelectedActivity : BaseActivity() {
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		enableEdgeToEdge()
		setContent {
			val backStack = rememberNavBackStack(CourseSelected)
			SharedTransitionLayout {
				SysuerNavDisplay(backStack = backStack,entryProvider = entryProvider {
					entry<CourseSelected> {
						CourseSelectedRoute(backStack, sharedTransitionScope = this@SharedTransitionLayout, animatedVisibilityScope = LocalNavAnimatedContentScope.current)
					}
					entry<CourseDetail> {
						CourseDetailRoute(backStack, navKey = it, sharedTransitionScope = this@SharedTransitionLayout, animatedVisibilityScope = LocalNavAnimatedContentScope.current)
					}
					entry<RichText> {
						RichTextRoute(backStack, navKey = it, sharedTransitionScope = this@SharedTransitionLayout, animatedVisibilityScope = LocalNavAnimatedContentScope.current)
					}
				})
			}
		}
	}
}