package com.sysu.edu.academic

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.res.stringResource
import com.sysu.edu.BaseActivity
import com.sysu.edu.R
import com.sysu.edu.view.ActivityPager
import com.sysu.edu.view.MenuItem
import com.sysu.edu.view.StaggerScreen
import com.sysu.edu.view.exportMarkdownMenuItem

class MajorInfoActivity : BaseActivity() {
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		enableEdgeToEdge()
		val viewModel: MajorInfoViewModel by viewModels()
		setContent {
			val categories = viewModel.categories
			val majorList = viewModel.majorList
			var page by remember { mutableIntStateOf(0) }
			LaunchedEffect(Unit) {
				viewModel.fetchCategories()
			}
			LaunchedEffect(Unit) {
				snapshotFlow { categories.toList() }.collect { list ->
					if (list.isNotEmpty()) {
						viewModel.fetchMajorList(page)
					}
				}
			}
			LaunchedEffect(page) {
				if (categories.isNotEmpty() && majorList[page]?.isNotEmpty() != true) viewModel.fetchMajorList(page)
			}
			val tabs = categories.map { MenuItem(it.getString("dataName")) }
			ActivityPager(title = stringResource(R.string.major_info), tabs = tabs, isNestedScrollEnabled = false, onPageChange = { page = it }, onNavigationClick = { supportFinishAfterTransition() }, topBarMenus = {
				listOf(exportMarkdownMenuItem(tabs.indices.map { majorList.getOrElse(it) { remember { mutableStateListOf() } } }.toList(), tabs, stringResource(R.string.major_info)))
			}) { page ->
				majorList[page]?.let {
					StaggerScreen(sections = it, onScrollBottom = {
						viewModel.fetchMajorList(page)
					})
				}
			}
		}
	}
}