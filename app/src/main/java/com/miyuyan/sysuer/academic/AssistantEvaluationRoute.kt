package com.miyuyan.sysuer.academic

import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import com.miyuyan.preference.EditMenuPreference
import com.miyuyan.preference.EditPreference
import com.miyuyan.preference.MenuPreference
import com.miyuyan.preference.PreferenceCategory
import com.miyuyan.preference.PreferenceScreen
import com.miyuyan.sysuer.R
import com.miyuyan.sysuer.api.DataStoreManager
import com.miyuyan.sysuer.nav.RichText
import com.miyuyan.sysuer.nav.navigateBack
import com.miyuyan.sysuer.view.ActivityPager
import com.miyuyan.sysuer.view.MenuItem
import com.miyuyan.sysuer.view.StaggerScreen
import com.miyuyan.sysuer.view.StatePage
import com.miyuyan.sysuer.view.toMarkdown
import kotlinx.coroutines.launch

/**
 * 助教考核：单个 ActivityPager 承载查询与结果两页。查询页用偏好设置样式的筛选条件，
 * 悬浮按钮"查询"触发检索并翻到结果页；结果页为状态页 + 瀑布流卡片，滚动到底部自动
 * 分页，悬浮按钮"导出"生成 Markdown 预览。
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun AssistantEvaluationRoute(
	backStack: MutableList<NavKey>,
	sharedTransitionScope: SharedTransitionScope? = null,
	animatedVisibilityScope: AnimatedVisibilityScope? = null,
) {
	val viewModel: AssistantEvaluationViewModel = viewModel()
	val activity = LocalActivity.current
	val uiState by viewModel.uiState.collectAsStateWithLifecycle()
	val pagerState = rememberPagerState(pageCount = { 2 })
	val coroutineScope = rememberCoroutineScope()
	val title = stringResource(R.string.assistant_evaluation)

	LaunchedEffect(Unit) {
		viewModel.fetchYearTerms()
		viewModel.fetchDepartments("")
	}
	ActivityPager(
			title = title,
			tabs = listOf(
					MenuItem(stringResource(R.string.query)),
					MenuItem(stringResource(R.string.result)),
			),
			pagerState = pagerState,
			isNestedScrollEnabled = false,
			onNavigationClick = { backStack.navigateBack(activity) },
			sharedTransitionScope = sharedTransitionScope,
			animatedVisibilityScope = animatedVisibilityScope,
			sharedKey = "AssistantEvaluation",
			floatingActionButton = { page ->
				when (page) {
					0 -> ExtendedFloatingActionButton(
							onClick = {
								viewModel.query()
								coroutineScope.launch { pagerState.animateScrollToPage(1) }
							},
							icon = {
								Icon(
										painter = painterResource(R.drawable.search),
										contentDescription = stringResource(R.string.query)
								)
							},
							text = { Text(stringResource(R.string.query)) },
					)

					else -> ExtendedFloatingActionButton(
							onClick = {
								backStack.add(
										RichText(
												title = title,
												content = viewModel.sections.toMarkdown(),
												contentType = DataStoreManager.ContentType.MARKDOWN.name
										)
								)
							},
							icon = {
								Icon(
										painter = painterResource(R.drawable.export),
										contentDescription = stringResource(R.string.export)
								)
							},
							text = { Text(stringResource(R.string.export)) },
					)
				}
			},
	) { page ->
		when (page) {
			0 -> QueryPage(viewModel)
			else -> StatePage(state = uiState) {
				StaggerScreen(
						sections = viewModel.sections,
						onScrollBottom = { viewModel.fetchResult() },
				)
			}
		}
	}
}

@Composable
private fun PreferenceIcon(drawableId: Int) {
	Icon(
			painter = painterResource(drawableId),
			contentDescription = null,
			tint = MaterialTheme.colorScheme.primary
	)
}

@Composable
private fun QueryPage(viewModel: AssistantEvaluationViewModel) {
	val yearTerms by viewModel.yearTerms.collectAsStateWithLifecycle()
	val departmentValues by viewModel.departmentValues.collectAsStateWithLifecycle()
	val departmentNames by viewModel.departmentNames.collectAsStateWithLifecycle()
	var departmentName by remember { mutableStateOf("") }

	PreferenceScreen(modifier = Modifier.fillMaxSize()) {
		PreferenceCategory(title = stringResource(R.string.filter)) {
			item {
				MenuPreference(
						title = stringResource(R.string.year_term),
						icon = { PreferenceIcon(R.drawable.calendar) },
						entries = yearTerms,
						entryValues = yearTerms,
						onChange = { _, entry, _ -> viewModel.yearTerm = entry },
				)
			}
			item {
				EditMenuPreference(
						title = stringResource(R.string.department),
						icon = { PreferenceIcon(R.drawable.home) },
						entries = departmentNames,
						entryValues = departmentValues,
						initialName = departmentName,
						onChange = { _, name, value ->
							viewModel.departmentValue = value
							departmentName = name
						},
						onQueryChange = { viewModel.fetchDepartments(it) },
						requireSelection = false,
				)
			}
			item {
				EditPreference(
						title = stringResource(R.string.course_name),
						value = viewModel.courseName ?: "",
						onChange = { _, _, value -> viewModel.courseName = value.orEmpty() },
						icon = { PreferenceIcon(R.drawable.text) },
						placeholder = stringResource(R.string.click_to_edit),
				)
			}
			item {
				EditPreference(
						title = stringResource(R.string.teacher),
						value = viewModel.teacher ?: "",
						onChange = { _, _, value -> viewModel.teacher = value.orEmpty() },
						icon = { PreferenceIcon(R.drawable.account) },
						placeholder = stringResource(R.string.click_to_edit),
				)
			}
		}
	}
}
