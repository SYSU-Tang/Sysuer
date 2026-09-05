package com.miyuyan.sysuer.academic

import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import com.alibaba.fastjson2.JSONObject
import com.mikepenz.markdown.compose.Markdown
import com.mikepenz.markdown.m3.markdownColor
import com.mikepenz.markdown.m3.markdownTypography
import com.miyuyan.sysuer.R
import com.miyuyan.sysuer.nav.navigateBack
import com.miyuyan.sysuer.view.ActivityPager
import com.miyuyan.sysuer.view.StaggerScreen

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun GradeRoute(
	backStack: MutableList<NavKey>,
	sharedTransitionScope: SharedTransitionScope? = null,
	animatedVisibilityScope: AnimatedVisibilityScope? = null,
) {
	val viewModel: GradeViewModel = viewModel()
	val activity = LocalActivity.current

	var yearExpanded by remember { mutableStateOf(false) }
	var termExpanded by remember { mutableStateOf(false) }
	var typeExpanded by remember { mutableStateOf(false) }

	LaunchedEffect(Unit) {
		viewModel.fetchPull()
	}

	ActivityPager(
		title = stringResource(R.string.score),
		onNavigationClick = { backStack.navigateBack(activity) },
		isNestedScrollEnabled = false,
		sharedTransitionScope = sharedTransitionScope,
		animatedVisibilityScope = animatedVisibilityScope,
		sharedKey = "Grade",
		topBarContent = {
			Row(
				modifier = Modifier
					.fillMaxWidth()
					.horizontalScroll(rememberScrollState())
					.padding(
						horizontal = dimensionResource(R.dimen.horizontal_padding),
						vertical = dimensionResource(R.dimen.vertical_padding)
					),
				horizontalArrangement = Arrangement.spacedBy(8.dp)
			) {
				Column {
					OutlinedButton(onClick = { yearExpanded = true }) {
						Text(viewModel.yearName.ifEmpty { stringResource(R.string.year) })
					}
					DropdownMenu(
						expanded = yearExpanded,
						onDismissRequest = { yearExpanded = false }) {
						val all = stringResource(R.string.all)
						DropdownMenuItem(
							text = { Text(all) },
							onClick = {
								viewModel.yearName = all
								viewModel.fetchAllYear()
								yearExpanded = false
							}
						)
						viewModel.years.forEach { y ->
							DropdownMenuItem(
								text = { Text(y.getString("dataName")) },
								onClick = {
									viewModel.year = y.getString("dataNumber")
									viewModel.yearName = y.getString("dataName")
									viewModel.fetchScore()
									yearExpanded = false
								}
							)
						}
					}
				}

				Column {
					OutlinedButton(onClick = { termExpanded = true }) {
						Text(
							viewModel.terms.getOrNull(viewModel.termIndex)
								?: stringResource(R.string.term)
						)
					}
					DropdownMenu(
						expanded = termExpanded,
						onDismissRequest = { termExpanded = false }) {
						viewModel.terms.forEachIndexed { i, t ->
							DropdownMenuItem(
								text = { Text(t) },
								onClick = {
									viewModel.termIndex = i
									viewModel.fetchScore()
									termExpanded = false
								}
							)
						}
					}
				}

				Column {
					OutlinedButton(onClick = { typeExpanded = true }) {
						Text(viewModel.trainTypeName.ifEmpty { stringResource(R.string.train_type) })
					}
					DropdownMenu(
						expanded = typeExpanded,
						onDismissRequest = { typeExpanded = false }) {
						viewModel.trainTypes.forEach { t ->
							DropdownMenuItem(
								text = { Text(t.getString("dataName")) },
								onClick = {
									viewModel.trainType = t.getString("dataNumber")
									viewModel.trainTypeName = t.getString("dataName")
									viewModel.fetchScore()
									typeExpanded = false
								}
							)
						}
					}
				}
			}
		}
	) {

		LazyColumn(
			modifier = Modifier.fillMaxSize(),
		) {
			item {
				StaggerScreen(
					sections = viewModel.sections,
					isNestedEnabled = false
				)
			}
			item {
				HorizontalDivider()
			}
			itemsIndexed(viewModel.scores) { index, score ->
				ScoreItem(
					score = score,
					modifier = Modifier.padding(dimensionResource(R.dimen.horizontal_padding), dimensionResource(R.dimen.vertical_padding)),
					onClick = { viewModel.requestGrade(index) }
				)
			}
		}
	}
}

@Composable
fun ScoreItem(
	score: JSONObject,
	modifier: Modifier = Modifier,
	onClick: () -> Unit
) {
	var gradeStr = ""
	if (score.containsKey("scoreList")) {
		score.getJSONArray("scoreList").forEach { a ->
			val item = a as JSONObject
			gradeStr = String.format(
				"%s（%s）%s×%s%%+",
				gradeStr,
				item.getString("FXMC"),
				item.getString("FXCJ"),
				item.getString("MRQZ")
			)
		}
	}

	val markdown = """
        - 学期：**${score.getString("scoSchoolYear")}第${score.getString("scoSemester")}学期**
        - 学分：**${score.getString("scoCredit")}**
        - 班级排名：**${score.getString("teachClassRank")}**
        - 年级排名：**${score.getString("gradeMajorRank")}**
        - 课程类别：**${score.getString("scoCourseCategoryName")}**
        - 老师：**${score.getString("scoTeacherName")}**
        - 是否通过：**${score.getString("accessFlag")}**
        - 考试性质：**${score.getString("examCharacter")}**
        - 班级号：**${score.getString("scoCourseNumber")}**
        - 教学班号：**${score.getString("teachClassNumber")}**
        - 成绩：**${
		if (score.getString("originalScore") == null) stringResource(R.string.click_for_grade)
		else gradeStr + "=" + score.getString("originalScore")
	}**
    """.trimIndent()

	ElevatedCard(
		onClick = { if (score.getString("originalScore") == null) onClick() },
		modifier = modifier.fillMaxWidth()
	) {
		Row(
			modifier = Modifier.padding(16.dp),
			horizontalArrangement = Arrangement.spacedBy(16.dp),
			verticalAlignment = Alignment.CenterVertically
		) {
			Text(
				text = "${score.getString("scoFinalScore")}${
					if (score.getString("scoPoint") == null) ""
					else "/" + score.getString("scoPoint")
				}",
				style = MaterialTheme.typography.titleLarge
			)
			Column {
				Text(
					text = score.getString("scoCourseName") ?: "",
					style = MaterialTheme.typography.titleLarge
				)
				HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
				Markdown(
					content = markdown,
					colors = markdownColor(),
					typography = markdownTypography(),
					modifier = Modifier.fillMaxWidth()
				)
			}
		}
	}
}
