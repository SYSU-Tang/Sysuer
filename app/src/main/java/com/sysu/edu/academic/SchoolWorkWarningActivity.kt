package com.sysu.edu.academic

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.res.stringResource
import com.sysu.edu.BaseActivity
import com.sysu.edu.R
import com.sysu.edu.view.ActivityPager
import com.sysu.edu.view.StaggerScreen
import com.sysu.edu.view.exportMarkdownMenuItem

class SchoolWorkWarningActivity : BaseActivity() {
	private val viewModel: SchoolWorkWarningViewModel by viewModels()
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		enableEdgeToEdge()
		setContent {
			val sections = viewModel.sections
			LaunchedEffect(Unit) {
				viewModel.fetchWarning()
			}
			ActivityPager(title = stringResource(R.string.school_work_warning), onNavigationClick = { supportFinishAfterTransition() }, isNestedScrollEnabled = false, topBarMenus = {
				listOf(exportMarkdownMenuItem(sections, stringResource(R.string.school_work_warning), stringResource(R.string.school_work_warning)))
			}) {
				StaggerScreen(
					sections = sections,
					onScrollBottom = {
						if (viewModel.hasMore) viewModel.fetchWarning()
					},
				             )
			}
		}
	}
}