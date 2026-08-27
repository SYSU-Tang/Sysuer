package com.sysu.edu

import android.content.Intent
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AppBarWithSearch
import androidx.compose.material3.ExpandedFullScreenContainedSearchBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberContainedSearchBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.core.app.ActivityCompat.recreate
import androidx.core.app.ActivityOptionsCompat
import androidx.core.net.toUri
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import com.sysu.edu.api.ContextUtil
import com.sysu.edu.api.PreferenceViewModel
import com.sysu.edu.api.SettingManager
import com.sysu.edu.api.TodoManager
import com.sysu.edu.browser.BrowserActivity
import com.sysu.edu.home.AccountScreen
import com.sysu.edu.home.DashboardScreen
import com.sysu.edu.home.DashboardViewModel
import com.sysu.edu.home.HomeViewModel
import com.sysu.edu.home.ServiceConfig
import com.sysu.edu.home.ServiceScreen
import com.sysu.edu.home.ServiceViewModel
import com.sysu.edu.view.ActivityPager
import com.sysu.edu.view.MenuItem
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class) @Composable fun HomeRoute(backStack: MutableList<NavKey>, sharedTransitionScope: SharedTransitionScope? = null, animatedVisibilityScope: AnimatedVisibilityScope? = null) {
	val activity = LocalActivity.current
	val context = LocalContext.current
	val dashboardViewModel: DashboardViewModel = viewModel()
	val homeViewModel: HomeViewModel = viewModel()
	val serviceViewModel: ServiceViewModel = viewModel()
	val spm: PreferenceViewModel = viewModel()
	val settingManager = SettingManager.getInstance(context)
	val todoManager = TodoManager(context, LocalLifecycleOwner.current.lifecycleScope)
	val progressMax by dashboardViewModel.progressMax.collectAsStateWithLifecycle()
	val progressCurrent by dashboardViewModel.progressCurrent.collectAsStateWithLifecycle()
	val searchBarState = rememberContainedSearchBarState()
	val textFieldState = rememberTextFieldState()
	var searchQuery by rememberSaveable { mutableStateOf("") }
	LaunchedEffect(Unit) { serviceViewModel.loadServiceData() }
	LaunchedEffect(textFieldState) {
		snapshotFlow { textFieldState.text.toString() }.collect { searchQuery = it }
	}
	val allItems = serviceViewModel.allItems
	val searchResults = remember(searchQuery, allItems) {
		if (searchQuery.isBlank()) allItems
		else allItems.filter { item ->
			item.name?.contains(searchQuery, true) == true || item.description?.contains(searchQuery, ignoreCase = true) == true
		}.sortedWith(compareByDescending<ServiceConfig> { item ->
			when {
				item.name?.startsWith(searchQuery, true) == true -> 2
				item.name?.contains(searchQuery, true) == true -> 1
				else -> 0
			}
		}.thenBy { it.name })
	}
	val scope = rememberCoroutineScope()
	fun navigateToServiceItem(item: ServiceConfig) {
		val intent = if (!item.activity.isNullOrBlank()) {
			try {
				Intent(context, Class.forName(context.packageName + item.activity)).takeIf {
					it.resolveActivity(context.packageManager) != null
				}
			} catch (_: Exception) {
				null
			}
		}
		else if (!item.url.isNullOrBlank()) {
			Intent(context, BrowserActivity::class.java).setData(item.url.toUri())
		}
		else null
		intent?.let { context.startActivity(it, activity?.let { it1 -> ActivityOptionsCompat.makeSceneTransitionAnimation(it1) }?.toBundle()) } ?: ContextUtil.getInstance(context).toast(R.string.activity_not_found)
	}
	ActivityPager(
		title = stringResource(R.string.app_name),
		navs = listOf(
			MenuItem(stringResource(R.string.dashboard), Icons.Rounded.Dashboard),
			MenuItem(stringResource(R.string.service), Icons.Rounded.GridView),
			MenuItem(stringResource(R.string.account), Icons.Rounded.Person),
		             ),
		topBarContent = { page ->
			when (page) {
				0 -> {
					if (progressMax > 0) LinearProgressIndicator(
						progress = { progressCurrent.toFloat() / progressMax },
						modifier = Modifier.fillMaxWidth(),
					                                            )
					else LinearProgressIndicator(
						progress = { 1f },
						modifier = Modifier.fillMaxWidth(),
					                            )
				}
				1 -> {
					val scrollBehavior = SearchBarDefaults.enterAlwaysSearchBarScrollBehavior()
					val appBarWithSearchColors = SearchBarDefaults.appBarWithSearchColors(searchBarColors = SearchBarDefaults.containedColors(state = searchBarState))
					val inputField = @Composable {
						SearchBarDefaults.InputField(
							textFieldState = rememberTextFieldState(),
							searchBarState = searchBarState,
							colors = appBarWithSearchColors.searchBarColors.inputFieldColors,
							onSearch = { },
							placeholder = {
								Text(modifier = Modifier.clearAndSetSemantics {}, text = stringResource(R.string.search))
							},
							leadingIcon = {
								Icon(Icons.Rounded.Search, contentDescription = stringResource(R.string.search))
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
					ExpandedFullScreenContainedSearchBar(
						state = searchBarState,
						inputField = @Composable {
							SearchBarDefaults.InputField(
								textFieldState = textFieldState,
								searchBarState = searchBarState,
								colors = appBarWithSearchColors.searchBarColors.inputFieldColors,
								onSearch = { },
								placeholder = {
									Text(modifier = Modifier.clearAndSetSemantics {}, text = stringResource(R.string.search))
								},
								leadingIcon = {
									IconButton(onClick = {
										scope.launch { searchBarState.animateToCollapsed() }
									}) {
										Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.back))
									}
								},
							                            )
						},
					                                    ) {
						ServiceSearchResults(results = searchResults, onResultClick = { item ->
							scope.launch { searchBarState.animateToCollapsed() }
							navigateToServiceItem(item)
						})
					}
				}
			}
		},
		pageContent = { page ->
			when (page) {
				0 -> DashboardScreen(dashboardViewModel, homeViewModel, spm, todoManager, settingManager, sharedTransitionScope, animatedVisibilityScope, backStack)
				1 -> ServiceScreen(homeViewModel, serviceViewModel, backStack, sharedTransitionScope, animatedVisibilityScope)
				2 -> AccountScreen (backStack){ activity?.let { recreate(it) } }
			}
		},
	             )
}