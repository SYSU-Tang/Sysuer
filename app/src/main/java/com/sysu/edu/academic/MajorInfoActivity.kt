package com.sysu.edu.academic

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModelProvider
import com.sysu.edu.BaseActivity
import com.sysu.edu.R
import com.sysu.edu.api.CommonUtil.extractValue
import com.sysu.edu.api.DataStoreManager
import com.sysu.edu.browser.RichTextActivity
import com.sysu.edu.view.ActivityPager
import com.sysu.edu.view.MenuItem
import com.sysu.edu.view.SectionData
import com.sysu.edu.view.StaggerScreen
import com.sysu.edu.view.toMarkdown

class MajorInfoActivity : BaseActivity() {
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		enableEdgeToEdge()
		val viewModel = ViewModelProvider(this)[MajorInfoViewModel::class.java]

		setContent {
			val categories by viewModel.categories.observeAsState(emptyList())
			val majorList by viewModel.majorList.observeAsState(emptyMap())

			LaunchedEffect(Unit) {
				viewModel.fetchCategories()
			}

			LaunchedEffect(categories) {
				if (categories.isNotEmpty()) {
					viewModel.fetchMajorList(0)
				}
			}

			val tabs = categories.map { MenuItem(it.getString("dataName")) }

			ActivityPager(
				title = stringResource(R.string.major_info),
				tabs = tabs,
				isNestedScrollEnabled = false,
				onNavigationClick = { supportFinishAfterTransition() },
				actions = {
					IconButton(onClick = {
						val allSections = mutableStateListOf<SectionData>()
						categories.forEachIndexed { index, _ ->
							majorList[index]?.forEach { item ->
								allSections.add(SectionData(
									title = item.getString("name"),
									rows = extractValue(this@MajorInfoActivity,
										item,
										intArrayOf(R.string.major_code,
											R.string.major_name,
											R.string.schooling_length,
											R.string.study_period,
											R.string.discipline_category,
											R.string.degree_granting_category),
										arrayOf("code",
											"name",
											"educationalSystem",
											"maxStudyYear",
											"disciplineCateName",
											"degreeGrantName"))))
							}
						}
						val markdown = allSections.toMarkdown()
						DataStoreManager.saveContent(this@MajorInfoActivity, getString(R.string.major_info), markdown) {
							startActivity(Intent(this@MajorInfoActivity, RichTextActivity::class.java)
								.putExtra("type", DataStoreManager.ContentType.MARKDOWN.name)
								.putExtra("title", getString(R.string.major_info)))
						}
					}) {
						Icon(painter = painterResource(R.drawable.export), contentDescription = stringResource(R.string.export))
					}
				}
			) { page ->
				val items = majorList[page] ?: emptyList()
				val sections = remember(items) {
					mutableStateListOf<SectionData>().also { list ->
						items.forEach { item ->
							list.add(SectionData(
								title = item.getString("name"),
								rows = extractValue(this@MajorInfoActivity,
									item,
									intArrayOf(R.string.major_code,
										R.string.major_name,
										R.string.schooling_length,
										R.string.study_period,
										R.string.discipline_category,
										R.string.degree_granting_category),
									arrayOf("code",
										"name",
										"educationalSystem",
										"maxStudyYear",
										"disciplineCateName",
										"degreeGrantName"))))
						}
					}
				}

				LaunchedEffect(page) {
					if (categories.isNotEmpty() && items.isEmpty()) {
						viewModel.fetchMajorList(page)
					}
				}

				StaggerScreen(
					sections = sections,
					onScrollBottom = {
						if (viewModel.hasMore(page)) viewModel.fetchMajorList(page)
					}
				)
			}
		}
	}
}