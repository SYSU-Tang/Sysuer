package com.sysu.edu.academic

import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import com.sysu.edu.R
import com.sysu.edu.nav.navigateBack
import com.sysu.edu.view.ActivityPager
import com.sysu.edu.view.StaggerScreen
import com.sysu.edu.view.exportMarkdownMenuItem

@Composable fun CourseSelectedRoute(
	backStack: MutableList<NavKey>, sharedTransitionScope: SharedTransitionScope? = null,
	animatedVisibilityScope: AnimatedVisibilityScope? = null,
                                   ) {
	val viewModel: CourseSelectedViewModel = viewModel()
	LaunchedEffect(Unit) {
		viewModel.reFetchCourseList()
	}
	LaunchedEffect(Unit) {
		viewModel.navigationEvents.collect { nav ->
			backStack.add(nav)
		}
	}
	var searchQuery by remember { mutableStateOf("") }
	LaunchedEffect(searchQuery) {
		viewModel.reFetchCourseList(searchQuery)
	}
	val activity = LocalActivity.current
	ActivityPager(title = stringResource(R.string.course_selected), onNavigationClick = { backStack.navigateBack(activity) }, isNestedScrollEnabled = false, topBarMenus = {
		listOf(exportMarkdownMenuItem(backStack, viewModel.sections, stringResource(R.string.course_selected), stringResource(R.string.course_selected)))
	}, topBarContent = {
		OutlinedTextField(value = searchQuery, onValueChange = { searchQuery = it }, trailingIcon = {
			if (searchQuery.isNotEmpty()) IconButton(onClick = { searchQuery = "" }) {
				Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.clear))
			}
		}, label = { Text(stringResource(R.string.search_course)) }, singleLine = true, modifier = Modifier
			.fillMaxWidth()
			.padding(dimensionResource(R.dimen.horizontal_padding), dimensionResource(R.dimen.vertical_padding)))
	}, sharedTransitionScope = sharedTransitionScope, animatedVisibilityScope = animatedVisibilityScope,sharedKey = "CourseSelected") {
		StaggerScreen(sections = viewModel.sections, onScrollBottom = {
			if (viewModel.hasMore()) viewModel.fetchCourseList()
		}, sharedTransitionScope = sharedTransitionScope, animatedVisibilityScope = animatedVisibilityScope)
	}
}