package com.sysu.edu.academic

import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import com.sysu.edu.R
import com.sysu.edu.nav.navigateBack
import com.sysu.edu.view.ActivityPager
import com.sysu.edu.view.StaggerScreen
import com.sysu.edu.view.exportMarkdownMenuItem

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun SchoolWorkWarningRoute(
    backStack: MutableList<NavKey>,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
) {
    val viewModel: SchoolWorkWarningViewModel = viewModel()
    val activity = LocalActivity.current

    LaunchedEffect(Unit) {
        if (viewModel.sections.isEmpty()) {
            viewModel.fetchWarning()
        }
    }

    ActivityPager(
        title = stringResource(R.string.school_work_warning),
        onNavigationClick = { backStack.navigateBack(activity) },
        isNestedScrollEnabled = false,
        sharedTransitionScope = sharedTransitionScope,
        animatedVisibilityScope = animatedVisibilityScope,
        sharedKey = "SchoolWorkWarning",
        topBarMenus = {
            listOf(
                exportMarkdownMenuItem(
                    backStack,
                    viewModel.sections,
                    stringResource(R.string.school_work_warning),
                    stringResource(R.string.school_work_warning)
                )
            )
        }
    ) {
        StaggerScreen(
            sections = viewModel.sections,
            onScrollBottom = {
                if (viewModel.hasMore) viewModel.fetchWarning()
            },
        )
    }
}
