package com.miyuyan.sysuer.academic

import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import com.miyuyan.sysuer.R
import com.miyuyan.sysuer.api.CommonUtil.extractValue
import com.miyuyan.sysuer.nav.navigateBack
import com.miyuyan.sysuer.view.ActivityPager
import com.miyuyan.sysuer.view.MenuItem
import com.miyuyan.sysuer.view.RowData
import com.miyuyan.sysuer.view.SectionData
import com.miyuyan.sysuer.view.StaggerScreen
import com.miyuyan.sysuer.view.StatePage
import com.miyuyan.sysuer.view.exportMarkdownMenuItem

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun CourseCompletionRoute(
	backStack: MutableList<NavKey>,
	sharedTransitionScope: SharedTransitionScope? = null,
	animatedVisibilityScope: AnimatedVisibilityScope? = null,
) {
	val viewModel: CourseCompletionViewModel = viewModel()
	val context = LocalContext.current
	val activity = LocalActivity.current

	val creditHours by viewModel.creditHours.collectAsStateWithLifecycle(emptyList())
	val courseList = viewModel.courseList

	val creditUiState by viewModel.creditUiState.collectAsStateWithLifecycle()
	val courseUiState by viewModel.courseUiState.collectAsStateWithLifecycle()

	LaunchedEffect(Unit) {
		viewModel.fetchCreditHours()
		viewModel.fetchCourseList()
	}
	LaunchedEffect(Unit) {
		viewModel.navigationEvents.collect { nav ->
			backStack.add(nav)
		}
	}

	val totalCredit = stringResource(R.string.total_credit)
	val creditHoursSections = remember(creditHours) {
		mutableStateListOf<SectionData>().also { list ->
			val totalRow = mutableListOf(0f, 0f, 0f, 0f)
			creditHours.forEach { item ->
				val actualCredit = item.getFloatValue("actualCredit")
				val earnedCredit = item.getFloatValue("earnedCredit")
				if (item.getString("courseCategoryNumber") !in listOf("05", "06", "31")) {
					totalRow[0] += item.getFloatValue("trainingCredit")
					totalRow[1] += item.getFloatValue("exemptCredit")
					totalRow[2] += actualCredit
					totalRow[3] += earnedCredit
				}
				list.add(
						SectionData(
						title = item.getString("courseCategoryName"),
						rows = extractValue(
								context, item, intArrayOf(
								R.string.course_category,
								R.string.training_credit,
								R.string.exempt_credit,
								R.string.actual_credit,
								R.string.earned_credit,
								R.string.earned_point
						), arrayOf(
								"courseCategoryName",
								"trainingCredit",
								"exemptCredit",
								"actualCredit",
								"earnedAllCredit",
								"earnedAllPoint"
						)
						),
						footer = if (actualCredit > 0) {
							{
								LinearProgressIndicator(
										progress = { earnedCredit / actualCredit },
										modifier = Modifier.fillMaxWidth()
								)
							}
						} else null))
			}
			val rows = mutableStateListOf<RowData>(
			)
			intArrayOf(
					R.string.training_credit,
					R.string.exempt_credit,
					R.string.actual_credit,
					R.string.earned_credit
			).zip(totalRow).forEach { (key, value) ->
				rows.add(RowData(context.getString(key), value.toString()))
			}
			list.add(
					SectionData(
							title = totalCredit, rows = rows, footer = {
						LinearProgressIndicator(
								progress = { totalRow[3] / totalRow[2] },
								modifier = Modifier.fillMaxWidth()
						)
					})
			)

		}
	}
	val tabs = listOf(
			MenuItem(stringResource(R.string.credit_hours_status)),
			MenuItem(stringResource(R.string.course_completion_status)),
	)
	var courseName by remember { mutableStateOf("") }
	LaunchedEffect(courseName) {
		viewModel.refetchCourseList(courseName)
	}
	ActivityPager(
			title = stringResource(R.string.course_completion),
			tabs = tabs,
			onNavigationClick = { backStack.navigateBack(activity) },
			sharedTransitionScope = sharedTransitionScope,
			animatedVisibilityScope = animatedVisibilityScope,
			sharedKey = "CourseCompletion",
			topBarContent = {
				if (it == 1) Box(
						Modifier
							.fillMaxWidth()
							.padding(
									dimensionResource(R.dimen.horizontal_margin),
									dimensionResource(R.dimen.vertical_margin),
							)
				) {
					OutlinedTextField(
							value = courseName,
							onValueChange = { s -> courseName = s },
							modifier = Modifier.fillMaxWidth(),
							trailingIcon = {
								if (courseName.isNotEmpty()) {
									IconButton(
											onClick = { courseName = "" },
									) {
										Icon(
												Icons.Default.Close,
												contentDescription = stringResource(R.string.clear)
										)
									}
								}
							})
				}

			},
			topBarMenus = {
				listOf(
						exportMarkdownMenuItem(
								backStack,
								listOf(creditHoursSections, courseList),
								tabs,
								stringResource(R.string.course_completion)
						)
				)
			}) { page ->
		StatePage(
				state = when (page) {
					0 -> creditUiState
					else -> courseUiState
				}, onRetry = {
			if (page == 0) viewModel.fetchCreditHours()
			else viewModel.refetchCourseList(courseName)
		}) {
			StaggerScreen(
					sections = if (page == 0) creditHoursSections else courseList,
					isHideNull = page == 1,
					sharedTransitionScope = sharedTransitionScope,
					animatedVisibilityScope = animatedVisibilityScope,
					onScrollBottom = {
						if (page == 1 && viewModel.hasMore()) viewModel.fetchCourseList()
					})
		}
	}
}