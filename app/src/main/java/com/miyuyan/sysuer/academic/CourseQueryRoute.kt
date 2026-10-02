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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import com.miyuyan.preference.EditMenuPreference
import com.miyuyan.preference.EditPreference
import com.miyuyan.preference.MenuPreference
import com.miyuyan.preference.PreferenceCategory
import com.miyuyan.preference.PreferenceScreen
import com.miyuyan.preference.RangeSliderPreference
import com.miyuyan.preference.SliderPreference
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
import kotlin.math.roundToInt

/**
 * 全校开课课程：单个 ActivityPager 承载查询与结果两页。查询页用偏好设置样式的筛选条件，
 * 开课单位与教室支持输入过滤；悬浮按钮"查询"触发检索并翻到结果页；结果页为状态页 +
 * 瀑布流卡片，滚动到底部自动分页，悬浮按钮"导出"生成 Markdown 预览。
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun CourseQueryRoute(
	backStack: MutableList<NavKey>,
	sharedTransitionScope: SharedTransitionScope? = null,
	animatedVisibilityScope: AnimatedVisibilityScope? = null,
) {
	val viewModel: CourseQueryViewModel = viewModel()
	val activity = LocalActivity.current
	val uiState by viewModel.uiState.collectAsStateWithLifecycle()
	val pagerState = rememberPagerState(pageCount = { 2 })
	val coroutineScope = rememberCoroutineScope()
	val title = stringResource(R.string.course_query)
	val exportTitle = stringResource(R.string.course)

	LaunchedEffect(Unit) {
		viewModel.fetchYearTerms()
		viewModel.fetchCampuses()
		viewModel.fetchClassLevels()
		viewModel.fetchTeachingTypes()
		viewModel.fetchBuildings()
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
			sharedKey = "CourseQuery",
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
												title = exportTitle,
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
private fun QueryPage(viewModel: CourseQueryViewModel) {
	val yearTerms by viewModel.yearTerms.collectAsStateWithLifecycle()
	val campusNames by viewModel.campusNames.collectAsStateWithLifecycle()
	val campusValues by viewModel.campusValues.collectAsStateWithLifecycle()
	val classLevelNames by viewModel.classLevelNames.collectAsStateWithLifecycle()
	val classLevelValues by viewModel.classLevelValues.collectAsStateWithLifecycle()
	val teachingTypeNames by viewModel.teachingTypeNames.collectAsStateWithLifecycle()
	val teachingTypeValues by viewModel.teachingTypeValues.collectAsStateWithLifecycle()
	val buildingNames by viewModel.buildingNames.collectAsStateWithLifecycle()
	val buildingValues by viewModel.buildingValues.collectAsStateWithLifecycle()
	val departmentNames by viewModel.departmentNames.collectAsStateWithLifecycle()
	val departmentValues by viewModel.departmentValues.collectAsStateWithLifecycle()
	val classroomNames by viewModel.classroomNames.collectAsStateWithLifecycle()
	val classroomValues by viewModel.classroomValues.collectAsStateWithLifecycle()
	val weekdays = stringArrayResource(R.array.weeks)
	val none = stringResource(R.string.none)
	// 数组首项为空白占位，菜单已有清除按钮，直接过滤掉
	val courseTypeOptions = stringArrayResource(R.array.course_type_entries)
		.zip(stringArrayResource(R.array.course_type_values)).filter { it.first.isNotBlank() }
	PreferenceScreen(modifier = Modifier.fillMaxSize()) {
		PreferenceCategory(title = stringResource(R.string.time)) {
			item {
				MenuPreference(
						title = stringResource(R.string.year_term),
						icon = { PreferenceIcon(R.drawable.calendar) },
						entries = yearTerms,
						entryValues = yearTerms,
						initialIndex = yearTerms.indexOf(viewModel.yearTerm).takeIf { it >= 0 },
						onChange = { _, _, value ->
							viewModel.yearTerm = value
						},
				)
			}
			item {
				MenuPreference(
						title = stringResource(R.string.end_year),
						icon = { PreferenceIcon(R.drawable.calendar) },
						entries = yearTerms,
						entryValues = yearTerms,
						initialIndex = yearTerms.indexOf(viewModel.endYear).takeIf { it >= 0 },
						onChange = { _, _, value ->
							viewModel.endYear = value
						},
				)
			}
			item {
				SliderPreference(
						title = stringResource(R.string.week),
						icon = { PreferenceIcon(R.drawable.course) },
						valueRange = 0f..7f,
						steps = 6,
						valueText = {
							if (it.roundToInt() == 0) none
							else weekdays[it.roundToInt() - 1]
						},
						initialValue = viewModel.weekDay,
						onValueChange = { viewModel.weekDay = it },
				)
			}
			item {
				RangeSliderPreference(
						title = stringResource(R.string.week_range),
						valueRange = 0f..26f,
						steps = 25,
						valuesText = {
							val start = it.start.roundToInt()
							val end = it.endInclusive.roundToInt()
							if (start != end && start != 0) stringResource(
									R.string.from_to_week, start, end
							)
							else if (start == 0 && end == 0) stringResource(
									R.string.none
							)
							else stringResource(
									R.string.week_d, end
							)
						},
						initialValues = viewModel.beginWeek..viewModel.endWeek,
						onValueChange = {
							viewModel.beginWeek = it.start
							viewModel.endWeek = it.endInclusive
						},
				)
			}
			item {
				RangeSliderPreference(
						title = stringResource(R.string.class_range),
						valueRange = 0f..12f,
						steps = 11,
						valuesText = {
							val start = it.start.roundToInt()
							val end = it.endInclusive.roundToInt()
							if (start != end && start != 0) stringResource(
									R.string.from_to_section, start, end
							)
							else if (start == 0 && end == 0) stringResource(
									R.string.none
							)
							else stringResource(
									R.string.section_d, end
							)
						},
						initialValues = viewModel.beginLesson..viewModel.endLesson,
						onValueChange = {
							viewModel.beginLesson = it.start
							viewModel.endLesson = it.endInclusive
						},
				)
			}
		}
		PreferenceCategory(title = stringResource(R.string.location)) {
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
							viewModel.fetchBuildings()
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
						icon = { PreferenceIcon(R.drawable.home) },
						entries = classroomNames,
						entryValues = classroomValues,
						initialName = viewModel.classroomName,
						onChange = { _, name, value ->
							viewModel.classroomValue = value
							viewModel.classroomName = name
						},
						onQueryChange = { viewModel.fetchClassrooms(it) },
						requireSelection = false,
				)
			}
		}
		PreferenceCategory(title = stringResource(R.string.course)) {
			item {
				EditPreference(
						title = stringResource(R.string.course_name),
						value = viewModel.courseName ?: "",
						onChange = { _, _, value -> viewModel.courseName = value.orEmpty() },
						icon = { PreferenceIcon(R.drawable.calendar) },
						placeholder = stringResource(R.string.click_to_edit),
				)
			}
			item {
				EditPreference(
						title = stringResource(R.string.course_number),
						value = viewModel.courseNumber ?: "",
						onChange = { _, _, value -> viewModel.courseNumber = value.orEmpty() },
						icon = { PreferenceIcon(R.drawable.id) },
						placeholder = stringResource(R.string.click_to_edit),
				)
			}
			item {
				val entryValues1 = courseTypeOptions.map { it.second }
				MenuPreference(
						title = stringResource(R.string.course_type),
						icon = { PreferenceIcon(R.drawable.book) },
						entries = courseTypeOptions.map { it.first },
						entryValues = entryValues1,
						initialIndex = entryValues1.indexOf(viewModel.courseType)
							.takeIf { it >= 0 },
						onChange = { _, _, value ->
							viewModel.courseType = value
						},
				)
			}
			item {
				MenuPreference(
						title = stringResource(R.string.teaching_type),
						icon = { PreferenceIcon(R.drawable.card) },
						entries = teachingTypeNames,
						entryValues = teachingTypeValues,
						initialIndex = teachingTypeValues.indexOf(viewModel.teachingType)
							.takeIf { it >= 0 },
						onChange = { _, _, value ->
							viewModel.teachingType = value
						},
				)
			}
		}
		PreferenceCategory(title = stringResource(R.string.teaching_class)) {
			item {
				EditPreference(
						title = stringResource(R.string.class_name),
						value = viewModel.className ?: "",
						onChange = { _, _, value -> viewModel.className = value.orEmpty() },
						icon = { PreferenceIcon(R.drawable.font) },
						placeholder = stringResource(R.string.click_to_edit),
				)
			}
			item {
				MenuPreference(
						title = stringResource(R.string.class_level),
						icon = { PreferenceIcon(R.drawable.menu) },
						entries = classLevelNames,
						entryValues = classLevelValues,
						initialIndex = classLevelValues.indexOf(viewModel.classLevelValue)
							.takeIf { it >= 0 },
						onChange = { _, _, value ->
							viewModel.classLevelValue = value
						},
				)
			}
			item {
				EditPreference(
						title = stringResource(R.string.class_number),
						value = viewModel.classNumber ?: "",
						onChange = { _, _, value -> viewModel.classNumber = value.orEmpty() },
						icon = { PreferenceIcon(R.drawable.id) },
						placeholder = stringResource(R.string.click_to_edit),
				)
			}
			item {
				EditPreference(
						title = stringResource(R.string.main_teacher),
						value = viewModel.teacher ?: "",
						onChange = { _, _, value -> viewModel.teacher = value.orEmpty() },
						icon = { PreferenceIcon(R.drawable.account) },
						placeholder = stringResource(R.string.click_to_edit),
				)
			}
			item {
				EditMenuPreference(
						title = stringResource(R.string.department),
						icon = { PreferenceIcon(R.drawable.group) },
						entries = departmentNames,
						entryValues = departmentValues,
						initialName = viewModel.departmentName,
						onChange = { _, name, value ->
							viewModel.departmentValue = value
							viewModel.departmentName = name
						},
						onQueryChange = { viewModel.fetchDepartments(it) },
						requireSelection = false,
				)
			}
		}
	}
}
