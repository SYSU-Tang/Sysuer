package com.miyuyan.sysuer.academic

import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import com.miyuyan.preference.ChoicePreference
import com.miyuyan.preference.EditMenuPreference
import com.miyuyan.preference.EditPreference
import com.miyuyan.preference.FilterPreference
import com.miyuyan.preference.ItemPreference
import com.miyuyan.preference.MenuPreference
import com.miyuyan.preference.PreferenceCategory
import com.miyuyan.preference.PreferenceScreen
import com.miyuyan.preference.SliderPreference
import com.miyuyan.sysuer.R
import com.miyuyan.sysuer.api.DataStoreManager
import com.miyuyan.sysuer.api.DateTimeManager
import com.miyuyan.sysuer.nav.RichText
import com.miyuyan.sysuer.nav.navigateBack
import com.miyuyan.sysuer.view.ActivityPager
import com.miyuyan.sysuer.view.MenuItem
import com.miyuyan.sysuer.view.StaggerScreen
import com.miyuyan.sysuer.view.StatePage
import com.miyuyan.sysuer.view.toMarkdown
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * 教室占用查询：单个 ActivityPager 承载查询与结果两页。查询页用偏好设置样式的筛选条件，
 * 支持按周次或按日期两种时间维度，校区-教学楼-教室逐级联动；悬浮按钮"查询"触发检索并
 * 翻到结果页；结果页为状态页 + 瀑布流卡片，滚动到底部自动分页，悬浮按钮"导出"生成
 * Markdown 预览。
 */
@Composable
fun RoomQueryRoute(
	backStack: MutableList<NavKey>,
	sharedTransitionScope: SharedTransitionScope? = null,
	animatedVisibilityScope: AnimatedVisibilityScope? = null,
) {
	val viewModel: RoomQueryViewModel = viewModel()
	val activity = LocalActivity.current
	val uiState by viewModel.uiState.collectAsStateWithLifecycle()
	val pagerState = rememberPagerState(pageCount = { 2 })
	val coroutineScope = rememberCoroutineScope()
	val title = stringResource(R.string.classroom_occupation_query)
	val snackbarHostState = remember { SnackbarHostState() }

	LaunchedEffect(Unit) {
		viewModel.snackbarMessage.collect {
			snackbarHostState.showSnackbar(it)
		}
	}

	LaunchedEffect(Unit) {
		viewModel.fetchCampuses()
		viewModel.fetchBuildings()
		viewModel.fetchYearTerms()
		viewModel.fetchClassrooms()
	}
	ActivityPager(
			title = title,
			snackbar = snackbarHostState,
			tabs = listOf(
					MenuItem(stringResource(R.string.query)),
					MenuItem(stringResource(R.string.result)),
			),
			pagerState = pagerState,
			isNestedScrollEnabled = false,
			onNavigationClick = { backStack.navigateBack(activity) },
			sharedTransitionScope = sharedTransitionScope,
			animatedVisibilityScope = animatedVisibilityScope,
			sharedKey = "RoomQuery",
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
private fun QueryPage(viewModel: RoomQueryViewModel) {
	val campusNames by viewModel.campusNames.collectAsStateWithLifecycle()
	val campusValues by viewModel.campusValues.collectAsStateWithLifecycle()
	val buildingNames by viewModel.buildingNames.collectAsStateWithLifecycle()
	val buildingValues by viewModel.buildingValues.collectAsStateWithLifecycle()
	val yearTerms by viewModel.yearTerms.collectAsStateWithLifecycle()
	val classroomNames by viewModel.classroomNames.collectAsStateWithLifecycle()
	val classroomValues by viewModel.classroomValues.collectAsStateWithLifecycle()
	var isWeek by rememberSaveable { mutableStateOf(viewModel.isWeek) }

	// 数组首项为空白占位，菜单已有清除按钮，直接过滤掉
	val checkTypeOptions =
		stringArrayResource(R.array.check_types).zip(stringArrayResource(R.array.check_type_values))
			.filter { it.first.isNotBlank() }
	val occupySourceOptions = stringArrayResource(R.array.occupy_sources)
		.zip(stringArrayResource(R.array.occupy_source_values)).filter { it.first.isNotBlank() }
	val weekTimeOptions =
		stringArrayResource(R.array.week_times).zip(stringArrayResource(R.array.week_time_values))
			.filter { it.first.isNotBlank() }
	val weeks = stringArrayResource(R.array.weeks).toList()
	val weekValues = stringArrayResource(R.array.week_values).toList()
	val noneText = stringResource(R.string.none)
	var showDatePicker by rememberSaveable { mutableStateOf(false) }

	PreferenceScreen(modifier = Modifier.fillMaxSize()) {
		PreferenceCategory {
			item {
				ItemPreference(
						title = stringResource(R.string.warning),
						icon = R.drawable.warning,
						summary = "时间范围不能大于30天！周次范围不能大于4周！"
				)
			}
		}
		PreferenceCategory(title = stringResource(R.string.classroom)) {
			item {
				MenuPreference(
						title = stringResource(R.string.campus),
						icon = { PreferenceIcon(R.drawable.location) },
						entries = campusNames,
						entryValues = campusValues,
						initialIndex = campusValues.indexOf(viewModel.campusValue)
							.takeIf { it >= 0 },

						onChange = { _, _, value ->
							viewModel.campusValue = value
							viewModel.buildingValue = null
							if (value == null) viewModel.fetchBuildings()
							else viewModel.fetchBuildingsByCampus(value)
							viewModel.fetchClassrooms()
						},
				)
			}
			item {
				MenuPreference(
						title = stringResource(R.string.office),
						icon = { PreferenceIcon(R.drawable.location) },
						entries = buildingNames,
						entryValues = buildingValues,
						initialIndex = buildingValues.indexOf(viewModel.buildingValue)
							.takeIf { it >= 0 },
						onChange = { _, _, value ->
							viewModel.buildingValue = value
							viewModel.fetchClassrooms()
						},
				)
			}
			item {
				EditMenuPreference(
						title = stringResource(R.string.classroom),
						icon = { PreferenceIcon(R.drawable.location) },
						entries = classroomNames,
						entryValues = classroomValues,
						initialName = viewModel.classroomName,
						onChange = { _, name, value ->
							viewModel.classroomValue = value
							viewModel.classroomName = name
						},
						onQueryChange = {
							viewModel.fetchClassrooms(
									it
							)
						},
						requireSelection = true,
				)
			}
		}
		PreferenceCategory(title = stringResource(R.string.query)) {
			item {
				val entryValues1 = checkTypeOptions.map { it.second }

				MenuPreference(
						title = stringResource(R.string.query_type),
						icon = { PreferenceIcon(R.drawable.info) },

						entries = checkTypeOptions.map { it.first },
						entryValues = entryValues1,
						initialIndex = entryValues1.indexOf(viewModel.checkType).takeIf { it >= 0 },

						onChange = { _, _, value ->
							viewModel.checkType = value
						},
				)
			}
			item {
				val entryValues1 = occupySourceOptions.map { it.second }

				MenuPreference(
						title = stringResource(R.string.occupy_source),
						icon = { PreferenceIcon(R.drawable.info) },
						entries = occupySourceOptions.map { it.first },
						entryValues = entryValues1,
						initialIndex = entryValues1.indexOf(viewModel.occupySource)
							.takeIf { it >= 0 },
						onChange = { _, _, value ->
							viewModel.occupySource = value
						},
				)
			}
			item {
				EditPreference(
						title = stringResource(R.string.occupy_reason),
						initialValue = viewModel.occupyReason.orEmpty(),
						onChange = { _, _, value -> viewModel.occupyReason = value.orEmpty() },
						icon = { PreferenceIcon(R.drawable.edit) },
						placeholder = stringResource(R.string.click_to_edit),
				)
			}
		}
		PreferenceCategory(title = stringResource(R.string.time)) {
			item {
				SliderPreference(
						title = stringResource(R.string.class_begin),
						icon = { PreferenceIcon(R.drawable.calendar) },
						valueRange = 0f..16f,
						steps = 15,
						initialValue = viewModel.classBegin,
						valueText = { if (it == 0f) noneText else it.roundToInt().toString() },
						onValueChange = { viewModel.classBegin = it },
				)
			}
			item {
				SliderPreference(
						title = stringResource(R.string.class_end),
						icon = { PreferenceIcon(R.drawable.calendar) },
						valueRange = 0f..16f,
						steps = 15,
						initialValue = viewModel.classEnd,
						valueText = { if (it == 0f) noneText else it.roundToInt().toString() },
						onValueChange = { viewModel.classEnd = it },
				)
			}
			item {
				ChoicePreference(
						title = stringResource(R.string.select),
						icon = { PreferenceIcon(R.drawable.help) },
						entries = listOf(
								stringResource(R.string.week), stringResource(R.string.date)
						),
						entryValues = listOf(true, false),
						selections = setOf(if (isWeek) 0 else 1),
						onChange = { _, _, value ->
							value?.let {
								isWeek = it
								viewModel.isWeek = it
							}
						},
						singleSelection = true,
				)
			}
		}
		if (isWeek) {
			PreferenceCategory(title = stringResource(R.string.week)) {
				item {

					MenuPreference(
							title = stringResource(R.string.year_term),
							icon = { PreferenceIcon(R.drawable.course) },
							entries = yearTerms,
							entryValues = yearTerms,
							initialIndex = yearTerms.indexOf(viewModel.yearTerm).takeIf { it >= 0 },

							onChange = { _, _, value ->
								viewModel.yearTerm = value
							},
					)
				}
				item {
					SliderPreference(
							title = stringResource(R.string.week_begin),
							icon = { PreferenceIcon(R.drawable.course) },
							valueText = { if (it == 0f) noneText else it.roundToInt().toString() },
							valueRange = 0f..25f,
							steps = 24,
							initialValue = viewModel.weekBegin,
							onValueChange = { viewModel.weekBegin = it },
					)
				}
				item {
					SliderPreference(
							title = stringResource(R.string.week_end),
							icon = { PreferenceIcon(R.drawable.course) },
							valueText = { if (it == 0f) noneText else it.roundToInt().toString() },
							valueRange = 0f..25f,
							steps = 24,
							initialValue = viewModel.weekEnd,
							onValueChange = { viewModel.weekEnd = it },
					)
				}
				item {
					FilterPreference(
							title = stringResource(R.string.week_time),
							icon = { PreferenceIcon(R.drawable.card) },
							entries = weekTimeOptions.map { it.first },
							entryValues = weekTimeOptions.map { it.second },
							initialSelections = weekTimeOptions.map { it.second }
								.indexOf(viewModel.weekTime).takeIf { it >= 0 }?.let { setOf(it) }
								?: emptySet(),
							onChange = { _, _, value ->
								viewModel.weekTime = value.firstOrNull()
							},
							singleSelection = true,
					)
				}
				item {
					FilterPreference(
							title = stringResource(R.string.week),
							icon = { PreferenceIcon(R.drawable.calendar) },
							entries = weeks,
							entryValues = weekValues,
							initialSelections = emptySet(),
							onChange = { _, _, value ->
								viewModel.weekday = value
							},
					)
				}
			}
		} else {
			PreferenceCategory(title = stringResource(R.string.date)) {
				item {
					ItemPreference(
							onClick = { showDatePicker = true },
							title = stringResource(R.string.date),
							icon = R.drawable.info,
							summary = listOfNotNull(viewModel.dateA, viewModel.dateB)
								.joinToString(" ~ ")
								.ifEmpty { stringResource(R.string.click_to_edit) },
					)
				}
			}
		}
	}
	if (showDatePicker) {
		val state = rememberDateRangePickerState()
		DatePickerDialog(onDismissRequest = { showDatePicker = false }, confirmButton = {
			TextButton(
					onClick = {
						viewModel.dateA = state.selectedStartDateMillis?.let {
							DateTimeManager.toDateString(it)
						}
						viewModel.dateB = state.selectedEndDateMillis?.let {
							DateTimeManager.toDateString(it)
						}
						showDatePicker = false
					}, shapes = ButtonDefaults.shapes()
			) { Text(stringResource(R.string.confirm)) }
		}, dismissButton = {
			TextButton(
					onClick = { showDatePicker = false }, shapes = ButtonDefaults.shapes()
			) { Text(stringResource(R.string.cancel)) }
		}) {
			DateRangePicker(state = state)
		}
	}
}
