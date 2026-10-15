package com.miyuyan.sysuer.academic

import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.miyuyan.sysuer.view.SingleSelectChipDropdown
import com.miyuyan.sysuer.view.StaggerScreen

@Composable
fun GradeRoute(
	backStack: MutableList<NavKey>,
	sharedTransitionScope: SharedTransitionScope? = null,
	animatedVisibilityScope: AnimatedVisibilityScope? = null,
) {
	val viewModel: GradeViewModel = viewModel()
	val activity = LocalActivity.current
	val allLabel = stringResource(R.string.all)

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
				FlowRow(
						modifier = Modifier
							.fillMaxWidth()
							.padding(
									horizontal = dimensionResource(R.dimen.horizontal_padding),
									vertical = dimensionResource(R.dimen.vertical_padding)
							),
						horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.horizontal_gap)),
						verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.vertical_gap))
				) {
					SingleSelectChipDropdown(
							title = stringResource(R.string.year),
							entries = viewModel.years.map { it.getString("dataName") },
							entryValues = viewModel.years.map { it.getString("dataNumber") },
							selectedIndex = viewModel.year?.let { year ->
								viewModel.years.indexOfFirst {
									it.getString("dataNumber") == year
								}.takeIf { it >= 0 }
							},
							onChange = { _, name, value ->
								if (value == null) {
									viewModel.year = null
									viewModel.yearName = allLabel
									viewModel.fetchAllYear()
								} else {
									viewModel.year = value
									viewModel.yearName = name.orEmpty()
									viewModel.fetchScore()
								}
							})
					SingleSelectChipDropdown(
							title = stringResource(R.string.term),
							entries = viewModel.terms.toList(),
							entryValues = viewModel.terms.toList(),
							selectedIndex = viewModel.termIndex,
							required = true,
							onChange = { index, _, _ ->
								if (index != null) {
									viewModel.termIndex = index
									viewModel.fetchScore()
								}
							})
					SingleSelectChipDropdown(
							title = stringResource(R.string.train_type),
							entries = viewModel.trainTypes.map { it.getString("dataName") },
							entryValues = viewModel.trainTypes.map { it.getString("dataNumber") },
							selectedIndex = viewModel.trainType?.let { type ->
								viewModel.trainTypes.indexOfFirst {
									it.getString("dataNumber") == type
								}.takeIf { it >= 0 }
							},
							required = true,
							onChange = { _, name, value ->
								if (value != null) {
									viewModel.trainType = value
									viewModel.trainTypeName = name.orEmpty()
									viewModel.fetchScore()
								}
							})
				}
			}) {

		LazyColumn(
				modifier = Modifier.fillMaxSize(),
		) {
			item {
				StaggerScreen(
						sections = viewModel.sections, isNestedEnabled = false
				)
			}
			if (viewModel.scores.isNotEmpty()) {
				item {
					HorizontalDivider()
				}
				itemsIndexed(viewModel.scores) { index, score ->
					ScoreItem(
							score = score,
							modifier = Modifier.padding(
									dimensionResource(R.dimen.horizontal_padding),
									dimensionResource(R.dimen.vertical_padding)
							),
							onClick = { viewModel.requestGrade(index) })
				}
			}
		}
	}
}

@Composable
fun ScoreItem(
	score: JSONObject, modifier: Modifier = Modifier, onClick: () -> Unit
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
					}", style = MaterialTheme.typography.titleLarge
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
