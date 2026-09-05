package com.sysu.edu.academic

import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import com.sysu.edu.R
import com.sysu.edu.nav.navigateBack
import com.sysu.edu.view.ActivityPager
import com.sysu.edu.view.InputDialogChip
import com.sysu.edu.view.SingleSelectChipDropdown
import com.sysu.edu.view.StaggerScreen
import com.sysu.edu.view.exportMarkdownMenuItem

@OptIn(ExperimentalLayoutApi::class, ExperimentalSharedTransitionApi::class)
@Composable
fun GradeForLevelRoute(
	backStack: MutableList<NavKey>,
	sharedTransitionScope: SharedTransitionScope? = null,
	animatedVisibilityScope: AnimatedVisibilityScope? = null,
) {
	val viewModel: GradeForLevelViewModel = viewModel()
	val activity = LocalActivity.current

	var trainTypeValue by rememberSaveable { mutableStateOf<String?>(null) }
	var yearValue by rememberSaveable { mutableStateOf<String?>(null) }
	var courseTypeValue by rememberSaveable { mutableStateOf<String?>(null) }
	var courseNameValue by rememberSaveable { mutableStateOf("") }
	var courseNumberValue by rememberSaveable { mutableStateOf("") }
	var minGradeValue by rememberSaveable { mutableStateOf("") }

	fun onFilterChange() {
		viewModel.trainType = trainTypeValue
		viewModel.year = yearValue
		viewModel.courseType = courseTypeValue
		viewModel.courseName = courseNameValue.ifEmpty { null }
		viewModel.courseNumber = courseNumberValue.ifEmpty { null }
		viewModel.minGrade = minGradeValue.ifEmpty { null }
		viewModel.reFetchGrade()
	}

	LaunchedEffect(Unit) {
		viewModel.fetchOptions()
		viewModel.fetchGrade()
	}

	ActivityPager(
		title = stringResource(R.string.grade_for_level),
		onNavigationClick = { backStack.navigateBack(activity) },
		isNestedScrollEnabled = false,
		sharedTransitionScope = sharedTransitionScope,
		animatedVisibilityScope = animatedVisibilityScope,
		sharedKey = "GradeForLevel",
		topBarMenus = {
			listOf(exportMarkdownMenuItem(backStack, viewModel.sections, stringResource(R.string.grade_for_level), stringResource(R.string.grade_for_level)))
		},
		topBarContent = {
			FlowRow(
				modifier = Modifier
					.fillMaxWidth()
					.padding(
						horizontal = dimensionResource(R.dimen.horizontal_padding),
						vertical = dimensionResource(R.dimen.vertical_padding)
					),
				horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.horizontal_margin))
			) {
				SingleSelectChipDropdown(
					category = stringResource(R.string.train_type),
					options = listOf(stringResource(R.string.reset)) + viewModel.trainTypeOptions.map { it.getString("dataName") },
					optionValues = listOf(null) + viewModel.trainTypeOptions.map { it.getString("dataNumber") },
					selectedValue = trainTypeValue,
					onValueChange = { trainTypeValue = it; onFilterChange() }
				)
				SingleSelectChipDropdown(
					category = stringResource(R.string.year),
					options = listOf(stringResource(R.string.reset)) + viewModel.yearOptions.map { it.getString("acadYearSemester") },
					optionValues = listOf(null) + viewModel.yearOptions.map { it.getString("acadYearSemester") },
					selectedValue = yearValue,
					onValueChange = { yearValue = it; onFilterChange() }
				)
				SingleSelectChipDropdown(
					category = stringResource(R.string.course_type),
					options = listOf(stringResource(R.string.reset)) + viewModel.courseTypeOptions.map { it.getString("catName") },
					optionValues = listOf(null) + viewModel.courseTypeOptions.map { it.getString("catCode") },
					selectedValue = courseTypeValue,
					onValueChange = { courseTypeValue = it; onFilterChange() }
				)
				InputDialogChip(stringResource(R.string.course_name), courseNameValue) { courseNameValue = it; onFilterChange() }
				InputDialogChip(stringResource(R.string.course_number), courseNumberValue) { courseNumberValue = it; onFilterChange() }
				InputDialogChip(stringResource(R.string.min_grade), minGradeValue, KeyboardType.Number) { minGradeValue = it; onFilterChange() }
			}
		}
	) {
		StaggerScreen(
			sections = viewModel.sections,
			onScrollBottom = {
				if (viewModel.hasMore()) viewModel.fetchGrade()
			}
		)
	}
}
