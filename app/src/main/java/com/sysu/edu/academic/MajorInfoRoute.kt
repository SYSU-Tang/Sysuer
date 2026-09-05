package com.sysu.edu.academic

import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import com.sysu.edu.R
import com.sysu.edu.nav.navigateBack
import com.sysu.edu.view.ActivityPager
import com.sysu.edu.view.MenuItem
import com.sysu.edu.view.StaggerScreen
import com.sysu.edu.view.exportMarkdownMenuItem

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun MajorInfoRoute(
    backStack: MutableList<NavKey>,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
) {
    val viewModel: MajorInfoViewModel = viewModel()
    val activity = LocalActivity.current

    val categories = viewModel.categories
    val majorList = viewModel.majorList
    var page by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        if (categories.isEmpty()) {
            viewModel.fetchCategories()
        }
    }

    LaunchedEffect(Unit) {
        snapshotFlow { categories.toList() }.collect { list ->
            if (list.isNotEmpty()) {
                viewModel.fetchMajorList(page)
            }
        }
    }

    LaunchedEffect(page) {
        if (categories.isNotEmpty() && majorList[page]?.isNotEmpty() != true) {
            viewModel.fetchMajorList(page)
        }
    }

    val tabs = categories.map { MenuItem(it.getString("dataName")) }

    ActivityPager(
        title = stringResource(R.string.major_info),
        tabs = tabs,
        isNestedScrollEnabled = false,
        onPageChange = { page = it },
        onNavigationClick = { backStack.navigateBack(activity) },
        sharedTransitionScope = sharedTransitionScope,
        animatedVisibilityScope = animatedVisibilityScope,
        sharedKey = "MajorInfo",
        topBarMenus = {
            val sectionsList = tabs.indices.map { 
                majorList[it] ?: remember { mutableStateListOf() }
            }
            listOf(
                exportMarkdownMenuItem(
                    backStack,
                    sectionsList,
                    tabs,
                    stringResource(R.string.major_info)
                )
            )
        }
    ) { page ->
        majorList[page]?.let {
            StaggerScreen(
                sections = it,
                onScrollBottom = { viewModel.fetchMajorList(page) }
            )
        }
    }
}
