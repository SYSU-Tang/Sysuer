package com.sysu.edu.academic

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.res.stringResource
import com.sysu.edu.BaseActivity
import com.sysu.edu.R
import com.sysu.edu.view.ActivityPager
import com.sysu.edu.view.MenuItem
import com.sysu.edu.view.StaggerScreen
import com.sysu.edu.view.exportMarkdownMenuItem

class CourseDetailActivity : BaseActivity() {
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		enableEdgeToEdge()
		val viewModel: CourseDetailViewModel by viewModels()
		val code = intent.getStringExtra("code")
		val id = intent.getStringExtra("id")
		viewModel.initFromIntent(code, id)
		setContent {
			LaunchedEffect(Unit) {
				viewModel.toastEvent.collect { message ->
					config.toast(message)
				}
			}
			ActivityPager(title = stringResource(R.string.course_detail),
			              onNavigationClick = { supportFinishAfterTransition() },
			              tabs = listOf(
				              MenuItem(title = stringResource(R.string.course_detail)),
				              MenuItem(title = stringResource(R.string.course_draft)),
			                           ),
			              topBarMenus = {
				              listOf(MenuItem(title = stringResource(R.string.download), icon = Icons.Rounded.Download, onClick = {
					              viewModel.outlineId.value?.let {
						              viewModel.downloadOutline(it)
					              } ?: viewModel.getOutlineId()
					              true
				              }), exportMarkdownMenuItem(viewModel.outlineSections, stringResource(R.string.course_outline), stringResource(R.string.course_outline)))
			              }) { page ->
				when (page) {
					0 -> StaggerScreen(viewModel.detailSections)
					1 -> StaggerScreen(viewModel.outlineSections)
				}
			}
		}
	}
}