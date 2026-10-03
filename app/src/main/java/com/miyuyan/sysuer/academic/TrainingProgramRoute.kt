package com.miyuyan.sysuer.academic

import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import com.miyuyan.preference.EditMenuPreference
import com.miyuyan.preference.FilterPreference
import com.miyuyan.preference.Preference
import com.miyuyan.preference.PreferenceCategory
import com.miyuyan.preference.PreferenceScreen
import com.miyuyan.preference.WheelPreference
import com.miyuyan.sysuer.R
import com.miyuyan.sysuer.api.DataStoreManager
import com.miyuyan.sysuer.nav.PersonalTrainingProgram
import com.miyuyan.sysuer.nav.RichText
import com.miyuyan.sysuer.nav.navigateBack
import com.miyuyan.sysuer.view.ActivityPager
import com.miyuyan.sysuer.view.MenuItem
import com.miyuyan.sysuer.view.StaggerScreen
import com.miyuyan.sysuer.view.toMarkdown
import kotlinx.coroutines.launch

/**
 * 培养方案查询：单个 ActivityPager 承载查询与结果两页。查询页用偏好设置样式：学院与专业
 * 为可输入过滤的菜单，年级为滚轮选择，类型为 Chip 单选，底部提供重置；悬浮按钮"查询"
 * 触发检索并翻到结果页；结果页为瀑布流卡片，滚动到底部自动分页，悬浮按钮"导出"生成
 * Markdown 预览。
 */
@Composable
fun TrainingProgramRoute(
	backStack: MutableList<NavKey>,
	sharedTransitionScope: SharedTransitionScope? = null,
	animatedVisibilityScope: AnimatedVisibilityScope? = null,
) {
	val viewModel: TrainingProgramViewModel = viewModel()
	val activity = LocalActivity.current
	val resultSections by viewModel.resultSections.collectAsState()
	val viewDetailProgramId by viewModel.viewDetailProgramId.collectAsState()
	val pagerState = rememberPagerState(pageCount = { 2 })
	val coroutineScope = rememberCoroutineScope()
	val title = stringResource(R.string.training_program_query)

	LaunchedEffect(viewDetailProgramId) {
		viewDetailProgramId?.let {
			backStack.add(PersonalTrainingProgram(it))
			viewModel.clearViewDetailProgramId()
		}
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
			sharedKey = "TrainingProgram",
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
												content = resultSections.toMarkdown(),
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
			else -> StaggerScreen(
					sections = resultSections,
					onScrollBottom = { viewModel.loadMore() },
			)
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
private fun QueryPage(viewModel: TrainingProgramViewModel) {
	val collegeNames by viewModel.collegeNames.collectAsState()
	val collegeIds by viewModel.collegeIds.collectAsState()
	val gradeNames by viewModel.gradeNames.collectAsState()
	val gradeIds by viewModel.gradeIds.collectAsState()
	val professionNames by viewModel.professionNames.collectAsState()
	val professionIds by viewModel.professionIds.collectAsState()
	val typeNames by viewModel.typeNames.collectAsState()
	val typeIds by viewModel.typeIds.collectAsState()
	val selectedTypeId by viewModel.selectedTypeId.collectAsState()
	val selectedGradeIndex by viewModel.selectedGradeIndex.collectAsState()
	val selectedCollegeName by viewModel.selectedCollegeName.collectAsState()
	val selectedProfessionName by viewModel.selectedProfessionName.collectAsState()

	PreferenceScreen(modifier = Modifier.fillMaxSize()) {
		PreferenceCategory(title = stringResource(R.string.filter)) {
			item {
				EditMenuPreference(
						title = stringResource(R.string.college),
						icon = { PreferenceIcon(R.drawable.home) },
						entries = collegeNames,
						entryValues = collegeIds,
						initialName = selectedCollegeName,
						onChange = { index, _, _ ->
							index?.let { viewModel.onCollegeSelected(it) }
						},
						onQueryChange = { viewModel.fetchColleges(it) },
				)
			}
			item {
				WheelPreference(
						title = stringResource(R.string.grade),
						icon = { PreferenceIcon(R.drawable.calendar) },
						entries = gradeNames,
						entryValues = gradeIds,
						selectedIndex = selectedGradeIndex,
						onChange = { index, _, _ -> index?.let { viewModel.onGradeSelected(it) } },
				)
			}
			item {
				EditMenuPreference(
						title = stringResource(R.string.profession),
						icon = { PreferenceIcon(R.drawable.account) },
						entries = professionNames,
						entryValues = professionIds,
						initialName = selectedProfessionName,
						onChange = { index, _, _ ->
							index?.let { viewModel.onProfessionSelected(it) }
						},
						onQueryChange = { viewModel.fetchProfessions(it) },
				)
			}
			item {
				FilterPreference(
						title = stringResource(R.string.type),
						icon = { PreferenceIcon(R.drawable.menu) },
						entries = typeNames,
						entryValues = typeIds,
						selections = typeIds.indexOf(selectedTypeId).takeIf { it >= 0 }
							?.let { setOf(it) } ?: emptySet(),
						onChange = { index, _, _ -> index?.let { viewModel.onTypeSelected(it) } },
						singleSelection = true,
				)
			}
		}
		PreferenceCategory {
			item {
				Preference(
						onClick = { viewModel.reset() },
						title = stringResource(R.string.reset),
				)
			}
		}
	}
}
