package com.miyuyan.sysuer.academic

import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import com.miyuyan.sysuer.R
import com.miyuyan.sysuer.nav.navigateBack
import com.miyuyan.sysuer.view.ActivityPager
import com.miyuyan.sysuer.view.MenuItem
import com.miyuyan.sysuer.view.SectionData
import com.miyuyan.sysuer.view.StaggerScreen
import com.miyuyan.sysuer.view.StatePage
import com.miyuyan.sysuer.view.exportMarkdownMenuItem

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun PhysicalFitnessTestResultRoute(
	backStack: MutableList<NavKey>,
	sharedTransitionScope: SharedTransitionScope? = null,
	animatedVisibilityScope: AnimatedVisibilityScope? = null,
) {
	val viewModel: PhysicalFitnessTestResultViewModel = viewModel()
	val activity = LocalActivity.current

	val tabs = listOf(
			MenuItem(stringResource(R.string.total_score)),
			MenuItem(stringResource(R.string.total_gym_credit)),
			MenuItem(stringResource(R.string.swimming_status)),
	)

	val emptySections = remember { mutableStateListOf<SectionData>() }

	ActivityPager(
			title = stringResource(R.string.physical_fitness_test_result),
			tabs = tabs,
			topBarMenus = {
				listOf(
						exportMarkdownMenuItem(
								backStack,
								tabs.indices.map { viewModel.sections[it] ?: emptySections },
								tabs,
								stringResource(R.string.physical_fitness_test_result)
						)
				)
			},
			onNavigationClick = { backStack.navigateBack(activity) },
			sharedTransitionScope = sharedTransitionScope,
			animatedVisibilityScope = animatedVisibilityScope,
			sharedKey = "PhysicalFitnessTestResult",
			pageContent = { page ->
				val uiState by viewModel.getUiState(page).collectAsState()
				StatePage(state = uiState, onRetry = { viewModel.fetchData() }) {
					StaggerScreen(
							sections = viewModel.sections[page] ?: emptySections
					)
				}
			})
}
