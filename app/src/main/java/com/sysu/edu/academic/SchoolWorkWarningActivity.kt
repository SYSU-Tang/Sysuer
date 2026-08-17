package com.sysu.edu.academic

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.sysu.edu.BaseActivity
import com.sysu.edu.R
import com.sysu.edu.api.DataStoreManager
import com.sysu.edu.browser.RichTextActivity
import com.sysu.edu.view.ActivityPager
import com.sysu.edu.view.StaggerScreen
import com.sysu.edu.view.toMarkdown

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
			ActivityPager(
				title = stringResource(R.string.school_work_warning),
				onNavigationClick = { supportFinishAfterTransition() },
				isNestedScrollEnabled = false,
				actions = {
					IconButton(onClick = {
						val markdown = viewModel.sections.toMarkdown()
						DataStoreManager.saveContent(this@SchoolWorkWarningActivity, getString(R.string.school_work_warning), markdown) {
							startActivity(Intent(this@SchoolWorkWarningActivity, RichTextActivity::class.java).putExtra("type", DataStoreManager.ContentType.MARKDOWN.name).putExtra("title", getString(R.string.school_work_warning)))
						}
					}) {
						Icon(painter = painterResource(R.drawable.export), contentDescription = stringResource(R.string.export))
					}
				},
			             ) {
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