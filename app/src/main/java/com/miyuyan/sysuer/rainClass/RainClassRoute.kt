package com.miyuyan.sysuer.rainClass

import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Assignment
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AppBarWithSearch
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberContainedSearchBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import com.miyuyan.sysuer.R
import com.miyuyan.sysuer.nav.navigateBack
import com.miyuyan.sysuer.view.ActivityPager
import com.miyuyan.sysuer.view.MenuItem
import com.miyuyan.sysuer.view.UiState
import com.miyuyan.sysuer.view.WarningCard
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.mapLatest

@OptIn(
		ExperimentalMaterial3Api::class,
		ExperimentalSharedTransitionApi::class,
		ExperimentalCoroutinesApi::class
)
@Composable
fun RainClassRoute(
	backStack: MutableList<NavKey>,
	sharedTransitionScope: SharedTransitionScope? = null,
	animatedVisibilityScope: AnimatedVisibilityScope? = null,
) {
	val viewModel: RainClassViewModel = viewModel()
	val activity = LocalActivity.current
	val searchBarState = rememberContainedSearchBarState()
	val textFieldState = rememberTextFieldState()
	var searchQuery by remember { mutableStateOf("") }
	val pagerState = rememberPagerState(0, pageCount = { 3 })

	LaunchedEffect(textFieldState) {
		snapshotFlow { textFieldState.text.toString() }.mapLatest { it }.distinctUntilChanged()
			.collect { searchQuery = it }
	}

	LaunchedEffect(Unit) {
		if (viewModel.courseUiState.value == UiState.Unstarted || viewModel.courseUiState.value == UiState.Error) {
			viewModel.getCourseList()
		}
		if (viewModel.examsUiState.value == UiState.Unstarted || viewModel.examsUiState.value == UiState.Error) {
			viewModel.getExams()
		}
		if (viewModel.userUiState.value == UiState.Unstarted || viewModel.userUiState.value == UiState.Error) {
			viewModel.getUserInfo()
		}
	}

	LaunchedEffect(viewModel.loginRequired) {
		viewModel.loginRequired.collect {
			pagerState.animateScrollToPage(2)
		}
	}
	LaunchedEffect(Unit) {
		viewModel.isLoginRequired.collect {
			if (it) {
				viewModel.getCourseList()
				viewModel.getExams()
			}
		}
	}

	ActivityPager(
			pagerState = pagerState,
			title = stringResource(R.string.rain_class),
			navs = listOf(
					MenuItem(stringResource(R.string.course), Icons.AutoMirrored.Rounded.MenuBook),
					MenuItem(stringResource(R.string.exam), Icons.AutoMirrored.Rounded.Assignment),
					MenuItem(stringResource(R.string.account), Icons.Rounded.AccountCircle),
			),
			isTopBarContentFixed = true,
			sharedKey = "RainClass",
			sharedTransitionScope = sharedTransitionScope,
			animatedVisibilityScope = animatedVisibilityScope,
			onNavigationClick = { backStack.navigateBack(activity) },
			topBarContent = { page ->
				Column(
						modifier = Modifier
							.fillMaxWidth()
							.padding(
									dimensionResource(R.dimen.horizontal_padding),
									dimensionResource(R.dimen.vertical_padding)
							),
						verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.vertical_margin))
				) {
					WarningCard()
					if (page == 0) {
						val scrollBehavior = SearchBarDefaults.enterAlwaysSearchBarScrollBehavior()
						val appBarWithSearchColors = SearchBarDefaults.appBarWithSearchColors(
								searchBarColors = SearchBarDefaults.containedColors(state = searchBarState)
						)
						val inputField = @Composable {
							SearchBarDefaults.InputField(
									textFieldState = textFieldState,
									searchBarState = searchBarState,
									colors = appBarWithSearchColors.searchBarColors.inputFieldColors,
									onSearch = { },
									placeholder = {
										Text(
												modifier = Modifier.clearAndSetSemantics {},
												text = stringResource(R.string.search_course)
										)
									},
									leadingIcon = {
										Icon(
												Icons.Rounded.Search,
												contentDescription = stringResource(R.string.search)
										)
									},
							)
						}
						AppBarWithSearch(
								scrollBehavior = scrollBehavior,
								windowInsets = SearchBarDefaults.windowInsets.only(WindowInsetsSides.Horizontal),
								state = searchBarState,
								colors = SearchBarDefaults.appBarWithSearchColors(
										appBarContainerColor = Color.Transparent,
								),
								inputField = inputField,
						)
					}
				}
			},
			pageContent = { page ->
				when (page) {
					0 -> CourseScreen(
							backStack = backStack,
							searchQuery = searchQuery,
							sharedTransitionScope = sharedTransitionScope,
							animatedVisibilityScope = animatedVisibilityScope
					)

					1 -> ExamScreen()
					2 -> AccountScreen()
				}
			},
	)
}