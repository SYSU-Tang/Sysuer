package com.sysu.edu.academic

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModelProvider
import com.sysu.edu.BaseActivity
import com.sysu.edu.R
import com.sysu.edu.api.CommonUtil.extractValue
import com.sysu.edu.view.ActivityPager
import com.sysu.edu.view.MenuItem
import com.sysu.edu.view.SectionData
import com.sysu.edu.view.StaggerScreen
import com.sysu.edu.view.exportMarkdownMenuItem

class CourseCompletionActivity : BaseActivity() {
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		enableEdgeToEdge()
		val viewModel = ViewModelProvider(this)[CourseCompletionViewModel::class.java]

		setContent {
			val creditHours by viewModel.creditHours.observeAsState(emptyList())
			val courseList by viewModel.courseList.observeAsState(emptyList())

			LaunchedEffect(Unit) {
				viewModel.fetchCreditHours()
				viewModel.fetchCourseList()
			}

			val creditHoursSections = remember(creditHours) {
				mutableStateListOf<SectionData>().also { list ->
					creditHours.forEach { item ->
						val actualCredit = item.getFloatValue("actualCredit")
						val earnedCredit = item.getFloatValue("earnedCredit")
						list.add(SectionData(
							title = item.getString("courseCategoryName"),
							rows = extractValue(this@CourseCompletionActivity,
								item,
								intArrayOf(R.string.course_category, R.string.training_credit, R.string.exempt_credit, R.string.actual_credit, R.string.earned_credit, R.string.earned_point),
								arrayOf("courseCategoryName", "trainingCredit", "exemptCredit", "actualCredit", "earnedAllCredit","earnedAllPoint")),
							footer =
								if (actualCredit > 0) {
									{
										LinearProgressIndicator(
										progress = { earnedCredit / actualCredit },
										modifier = Modifier
											.fillMaxWidth())
									}
								} else {
									null
								}
							)
						)
					}
				}
			}
			
			val view = stringResource(R.string.course_outline)
			val courseCompletionSections = remember(courseList) {
				mutableStateListOf<SectionData>().also { list ->
					courseList.forEach { item ->
						val rows = extractValue(this@CourseCompletionActivity,
							item,
							intArrayOf(R.string.academic_year_semester, R.string.course_number, R.string.course_name, R.string.course_category, R.string.credit,
							           R.string.academic_year_semester, R.string.achievement_course_number, R.string.achievement_course_name, R.string.achievement_course_category, R.string.achievement_credit, R.string.is_passed, R.string.achievement_point),
							arrayOf("acadYearSemester", "courseNumber", "courseName", "courseCategoryName", "credit",
							        "achievementAcadYearSemester", "achievementCourseNumber", "achievementCourseName", "achievementCourseCategoryName", "achievementCredit", "ispassed", "achievementPoint"))
//						val acadYearSemester = item.getString("acadYearSemester")
//						if (!acadYearSemester.isNullOrEmpty()) {
//							rows[0] = rows[0].copy(value = acadYearSemester.replace(",", "|"))
//						}
//						rows.addAll(extractValue(this@CourseCompletionActivity,
//							item,
//							intArrayOf(R.string.academic_year_semester, R.string.achievement_course_number, R.string.achievement_course_name, R.string.achievement_course_category, R.string.achievement_credit, R.string.is_passed, R.string.achievement_point),
//							arrayOf()))
//						val achievementSemester = item.getString("achievementAcadYearSemester")
//						if (!achievementSemester.isNullOrEmpty()) {
//							rows[5] = rows[5].copy(value = achievementSemester.replace(",", "|"))
//						}
						list.add(SectionData(
							title = item.getString("courseName"),
							footerMenus = mutableStateListOf(
								MenuItem(view){
									startActivity(Intent(this@CourseCompletionActivity, CourseDetailActivity::class.java)
										.putExtra("code", item.getString("courseNumber")))
									true
								},
							),
							rows = rows))
					}
				}
			}

			val tabs = listOf(
					MenuItem(stringResource(R.string.credit_hours_status)),
					MenuItem(stringResource(R.string.course_completion_status)),
				)
			ActivityPager(
				title = stringResource(R.string.course_completion),
				tabs = tabs,
				onNavigationClick = { supportFinishAfterTransition() },
				topBarMenus = {
					listOf(
						exportMarkdownMenuItem(
							listOf(creditHoursSections, courseCompletionSections),
							tabs,stringResource(R.string.course_completion))
					)
				}
			) { page ->
				StaggerScreen(
					sections = if (page == 0) creditHoursSections else courseCompletionSections,
					isHideNull = page == 1,
					onScrollBottom = {
						if (page == 1 && viewModel.hasMore()) viewModel.fetchCourseList()
					}
				)
			}
		}
	}
}