package com.miyuyan.sysuer.academic

import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import com.miyuyan.preference.EditPreference
import com.miyuyan.preference.MenuPreference
import com.miyuyan.preference.PreferenceCategory
import com.miyuyan.preference.PreferenceScreen
import com.miyuyan.sysuer.R
import com.miyuyan.sysuer.nav.CourseDetail
import com.miyuyan.sysuer.nav.navigateBack
import com.miyuyan.sysuer.view.ActivityPager
import com.miyuyan.sysuer.view.MenuItem

/**
 * 选课：主选课 / 预览 / 已选 三个子页共用一个统一 ViewModel，
 * 高级筛选以 ModalBottomSheet 在页内编辑
 */
@Composable
fun CourseSelectionRoute(
	backStack: MutableList<NavKey>,
	sharedTransitionScope: SharedTransitionScope? = null,
	animatedVisibilityScope: AnimatedVisibilityScope? = null,
) {
	val viewModel: CourseSelectionViewModel = viewModel()
	val activity = LocalActivity.current
	var filterTarget by remember { mutableStateOf<Int?>(null) }
	var headVisible by remember { mutableStateOf(true) }
	var showPeDialog by remember { mutableStateOf(false) }

	ActivityPager(
			title = stringResource(R.string.course_selection),
			tabs = listOf(
					MenuItem(stringResource(R.string.course_selection)),
					MenuItem(stringResource(R.string.preview)),
					MenuItem(stringResource(R.string.course_selected)),
			),
			isNestedScrollEnabled = false,
			onNavigationClick = { backStack.navigateBack(activity) },
			sharedTransitionScope = sharedTransitionScope,
			animatedVisibilityScope = animatedVisibilityScope,
			sharedKey = "CourseSelection",
			topBarMenus = {
				listOf(
						MenuItem(
								title = stringResource(
										if (headVisible) R.string.hide_filter else R.string.show_filter
								),
								iconResource = R.drawable.filter,
								onClick = { headVisible = !headVisible },
						),
				)
			},
			topBarContent = { page ->
				AnimatedVisibility(
						visible = headVisible,
						enter = expandVertically() + fadeIn(),
						exit = shrinkVertically() + fadeOut(),
				) {
					when (page) {
						0 -> CourseSelectionFilterPanel(
								viewModel = viewModel,
								onNavigateToFilter = { _, _ -> filterTarget = 0 },
								onShowPeDialog = { showPeDialog = true },
						)
						1 -> CourseSelectionPreviewFilterPanel(
								viewModel = viewModel,
								onNavigateToFilter = { _, _ -> filterTarget = 1 },
						)
						else -> CourseSelectionSelectedFilterPanel(viewModel)
					}
				}
			},
	) { page ->
		when (page) {
			0 -> CourseSelectionScreen(
					viewModel = viewModel,
					onNavigateToDetail = { id, code, _ ->
						backStack.add(CourseDetail(courseId = id, courseNum = code))
					},
			)
			1 -> CourseSelectionPreviewScreen(
					viewModel = viewModel,
					onNavigateToDetail = { id, code, _ ->
						backStack.add(CourseDetail(courseId = id, courseNum = code))
					},
			)
			else -> CourseSelectionSelectedScreen(
					viewModel = viewModel,
					onNavigateToDetail = { id, code, _ ->
						backStack.add(CourseDetail(courseId = id, courseNum = code))
					},
			)
		}
	}

	if (showPeDialog) {
		PeSortDialog(
				viewModel = viewModel,
				onDismiss = { showPeDialog = false },
		)
	}

	filterTarget?.let { target ->
		CourseSelectionFilterSheet(
				viewModel = viewModel,
				target = target,
				onDismiss = { filterTarget = null },
		)
	}
}

/** 高级筛选编辑弹层：compose-preference 输入行 + 下拉筛选，重置与确认 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CourseSelectionFilterSheet(
	viewModel: CourseSelectionViewModel,
	target: Int,
	onDismiss: () -> Unit,
) {
	val initialName = (if (target == 0) viewModel.mainFilterName else viewModel.previewFilterName).value
	val initialValue = (if (target == 0) viewModel.mainFilterValue else viewModel.previewFilterValue).value
	var name by remember {
		mutableStateOf(
				CourseFilterNameData(
						courseName = initialName?.courseName,
						studyCampusId = initialName?.studyCampusId,
						week = initialName?.week,
						classTimes = initialName?.classTimes,
						courseUnitNum = initialName?.courseUnitNum,
						teachingTeacherNum = initialName?.teachingTeacherNum,
						teachingLanguageCode = initialName?.teachingLanguageCode,
						specialClassCode = initialName?.specialClassCode,
				)
		)
	}
	var value by remember {
		mutableStateOf(
				CourseFilterValueData(
						courseName = initialValue?.courseName,
						studyCampusId = initialValue?.studyCampusId,
						week = initialValue?.week,
						classTimes = initialValue?.classTimes,
						courseUnitNum = initialValue?.courseUnitNum,
						teachingTeacherNum = initialValue?.teachingTeacherNum,
						teachingLanguageCode = initialValue?.teachingLanguageCode,
						specialClassCode = initialValue?.specialClassCode,
				)
		)
	}
	val options by viewModel.filterOptions.collectAsStateWithLifecycle()
	LaunchedEffect(Unit) {
		viewModel.fetchFilterOptions()
	}

	fun submit() {
		val courseName = name.courseName?.trim().orEmpty()
		val teacher = name.teachingTeacherNum?.trim().orEmpty()
		val school = name.courseUnitNum?.trim().orEmpty()
		name = name.copy(courseName = courseName, teachingTeacherNum = teacher, courseUnitNum = school)
		value = value.copy(courseName = courseName, teachingTeacherNum = teacher, courseUnitNum = school)
		if (target == 0) {
			viewModel.setMainFilterName(name)
			viewModel.setMainFilterValue(value)
		} else {
			viewModel.setPreviewFilterName(name)
			viewModel.setPreviewFilterValue(value)
		}
		onDismiss()
	}

	ModalBottomSheet(onDismissRequest = onDismiss) {
		PreferenceScreen {
		PreferenceCategory {
			item {
				EditPreference(
						title = stringResource(R.string.course),
						icon = { Icon(painterResource(R.drawable.course), contentDescription = null) },
						value = name.courseName.orEmpty(),
						placeholder = stringResource(R.string.course),
						onChange = { _, _, v ->
							val text = v.orEmpty()
							name = name.copy(courseName = text)
							value = value.copy(courseName = text)
						},
				)
			}
			item {
				MenuPreference(
						title = stringResource(R.string.campus),
						icon = { Icon(painterResource(R.drawable.location), contentDescription = null) },
						required = false,
						summary = name.studyCampusId?.takeIf { it.isNotEmpty() },
						entries = options[0].map { it.name.orEmpty() },
						entryValues = options[0].map { it.code },
						selectedIndex = options[0].indexOfFirst { it.name == name.studyCampusId }
							.takeIf { it >= 0 },
						onChange = { index, _, _ ->
							if (index != null) {
								val option = options[0][index]
								name = name.copy(studyCampusId = option.name)
								value = value.copy(studyCampusId = option.code)
							} else {
								name = name.copy(studyCampusId = "")
								value = value.copy(studyCampusId = "")
							}
						},
				)
			}
			item {
				MenuPreference(
						title = stringResource(R.string.week),
						icon = { Icon(painterResource(R.drawable.calendar), contentDescription = null) },
						required = false,
						summary = name.week?.takeIf { it.isNotEmpty() },
						entries = options[1].map { it.name.orEmpty() },
						entryValues = options[1].map { it.code },
						selectedIndex = options[1].indexOfFirst { it.name == name.week }.takeIf { it >= 0 },
						onChange = { index, _, _ ->
							if (index != null) {
								val option = options[1][index]
								name = name.copy(week = option.name)
								value = value.copy(week = option.code)
							} else {
								name = name.copy(week = "")
								value = value.copy(week = "")
							}
						},
				)
			}
			item {
				MenuPreference(
						title = stringResource(R.string.section),
						icon = { Icon(painterResource(R.drawable.time), contentDescription = null) },
						required = false,
						summary = name.classTimes?.takeIf { it.isNotEmpty() },
						entries = options[2].map { it.name.orEmpty() },
						entryValues = options[2].map { it.code },
						selectedIndex = options[2].indexOfFirst { it.name == name.classTimes }
							.takeIf { it >= 0 },
						onChange = { index, _, _ ->
							if (index != null) {
								val option = options[2][index]
								name = name.copy(classTimes = option.name)
								value = value.copy(classTimes = option.code)
							} else {
								name = name.copy(classTimes = "")
								value = value.copy(classTimes = "")
							}
						},
				)
			}
			item {
				MenuPreference(
						title = stringResource(R.string.language),
						icon = { Icon(painterResource(R.drawable.language), contentDescription = null) },
						required = false,
						summary = name.teachingLanguageCode?.takeIf { it.isNotEmpty() },
						entries = options[3].map { it.name.orEmpty() },
						entryValues = options[3].map { it.code },
						selectedIndex = options[3].indexOfFirst { it.name == name.teachingLanguageCode }
							.takeIf { it >= 0 },
						onChange = { index, _, _ ->
							if (index != null) {
								val option = options[3][index]
								name = name.copy(teachingLanguageCode = option.name)
								value = value.copy(teachingLanguageCode = option.code)
							} else {
								name = name.copy(teachingLanguageCode = "")
								value = value.copy(teachingLanguageCode = "")
							}
						},
				)
			}
			item {
				MenuPreference(
						title = stringResource(R.string.special),
						icon = { Icon(painterResource(R.drawable.priority), contentDescription = null) },
						required = false,
						summary = name.specialClassCode?.takeIf { it.isNotEmpty() },
						entries = options[4].map { it.name.orEmpty() },
						entryValues = options[4].map { it.code },
						selectedIndex = options[4].indexOfFirst { it.name == name.specialClassCode }
							.takeIf { it >= 0 },
						onChange = { index, _, _ ->
							if (index != null) {
								val option = options[4][index]
								name = name.copy(specialClassCode = option.name)
								value = value.copy(specialClassCode = option.code)
							} else {
								name = name.copy(specialClassCode = "")
								value = value.copy(specialClassCode = "")
							}
						},
				)
			}
			item {
				EditPreference(
						title = stringResource(R.string.school),
						icon = { Icon(painterResource(R.drawable.school), contentDescription = null) },
						value = name.courseUnitNum.orEmpty(),
						placeholder = stringResource(R.string.school),
						onChange = { _, _, v ->
							val text = v.orEmpty()
							name = name.copy(courseUnitNum = text)
							value = value.copy(courseUnitNum = text)
						},
				)
			}
			item {
				EditPreference(
						title = stringResource(R.string.teacher),
						icon = { Icon(painterResource(R.drawable.account), contentDescription = null) },
						value = name.teachingTeacherNum.orEmpty(),
						placeholder = stringResource(R.string.teacher),
						onChange = { _, _, v ->
							val text = v.orEmpty()
							name = name.copy(teachingTeacherNum = text)
							value = value.copy(teachingTeacherNum = text)
						},
				)
			}
		}
		PreferenceCategory {
			item {
				TextButton(
						onClick = {
							name = CourseFilterNameData()
							value = CourseFilterValueData()
						},
						modifier = Modifier.fillMaxWidth(),
				) {
					Text(stringResource(R.string.reset))
				}
			}
			item {
				FilledTonalButton(onClick = { submit() }, modifier = Modifier.fillMaxWidth()) {
					Text(stringResource(R.string.confirm))
				}
			}
			}
		}
	}
}
