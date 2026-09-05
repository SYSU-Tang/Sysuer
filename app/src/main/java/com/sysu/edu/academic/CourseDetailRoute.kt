package com.sysu.edu.academic

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import com.sysu.edu.R
import com.sysu.edu.nav.CourseDetail
import com.sysu.edu.nav.navigateBack
import com.sysu.edu.view.ActivityPager
import com.sysu.edu.view.MenuItem
import com.sysu.edu.view.StaggerScreen
import com.sysu.edu.view.exportMarkdownMenuItem

@Composable
fun CourseDetailRoute(
	backStack: MutableList<NavKey>,
	navKey: CourseDetail? = backStack.lastOrNull() as? CourseDetail,
	sharedTransitionScope: SharedTransitionScope? = null,
	animatedVisibilityScope: AnimatedVisibilityScope? = null,
) {
	if (navKey == null) return
	val viewModel: CourseDetailViewModel = viewModel()
	val context = LocalContext.current
	println("navKey: $navKey courseId: ${navKey.courseId} courseNum: ${navKey.courseNum}")
	LaunchedEffect(navKey.courseId, navKey.courseNum) {
		viewModel.initFromIntent(navKey.courseNum, navKey.courseId)
	}
	LaunchedEffect(Unit) {
		viewModel.toastEvent.collect { message ->
			Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
		}
	}
	ActivityPager(
		sharedTransitionScope = sharedTransitionScope,
		animatedVisibilityScope = animatedVisibilityScope,
		sharedKey = "course_${navKey.courseId}_${navKey.courseNum}",
		title = stringResource(R.string.course_detail),
		onNavigationClick = { backStack.navigateBack() },
		tabs = listOf(
			MenuItem(title = stringResource(R.string.course_detail)),
			MenuItem(title = stringResource(R.string.course_draft)),
		),
		topBarMenus = {
			listOf(
				MenuItem(
					title = stringResource(R.string.download),
					icon = Icons.Rounded.Download,
					onClick = {
						viewModel.outlineId.value?.let {
							viewModel.downloadOutline(it)
						} ?: viewModel.getOutlineId()
						true
					}),
				exportMarkdownMenuItem(
					backStack,
					viewModel.outlineSections,
					stringResource(R.string.course_outline),
					stringResource(R.string.course_outline)
				)
			)
		}) {
		StaggerScreen(
			sections = when (it) {
				0 -> viewModel.detailSections
				1 -> viewModel.outlineSections
				else -> viewModel.detailSections
			}
		)
	}
}