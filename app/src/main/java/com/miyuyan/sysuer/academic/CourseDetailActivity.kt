package com.miyuyan.sysuer.academic

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.SharedTransitionLayout
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import com.miyuyan.sysuer.BaseActivity
import com.miyuyan.sysuer.browser.RichTextRoute
import com.miyuyan.sysuer.nav.CourseDetail
import com.miyuyan.sysuer.nav.RichText
import com.miyuyan.sysuer.nav.SysuerNavDisplay
import com.miyuyan.sysuer.theme.SysuerTheme

class CourseDetailActivity : BaseActivity() {
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		enableEdgeToEdge()
		val code = intent.getStringExtra("code") ?: ""
		val id = intent.getStringExtra("id") ?: ""
		setContent {
			val backStack = rememberNavBackStack(CourseDetail(courseId = id, courseNum = code))
			SysuerTheme {
				SharedTransitionLayout {
					SysuerNavDisplay(backStack = backStack, entryProvider = entryProvider {
						entry<CourseDetail> {
							CourseDetailRoute(backStack, it, sharedTransitionScope = this@SharedTransitionLayout, animatedVisibilityScope = LocalNavAnimatedContentScope.current)
						}
						entry<RichText> {
							RichTextRoute(backStack, it, sharedTransitionScope = this@SharedTransitionLayout, animatedVisibilityScope = LocalNavAnimatedContentScope.current)
						}
					})
				}
			}
		}
	}
}