package com.sysu.edu.academic

import android.content.Intent
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import com.sysu.edu.R
import com.sysu.edu.api.CommonUtil.extractValue
import com.sysu.edu.nav.navigateBack
import com.sysu.edu.view.ActivityPager
import com.sysu.edu.view.MenuItem
import com.sysu.edu.view.SectionData
import com.sysu.edu.view.StaggerScreen
import com.sysu.edu.view.exportMarkdownMenuItem

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
                list.add(
                    SectionData(
                        title = item.getString("courseCategoryName"),
                        rows = extractValue(
                            context,
                            item,
                            intArrayOf(
                                R.string.course_category,
                                R.string.training_credit,
                                R.string.exempt_credit,
                                R.string.actual_credit,
                                R.string.earned_credit,
                                R.string.earned_point
                            ),
                            arrayOf(
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
                val rows = extractValue(
                    context,
                    item,
                    intArrayOf(
                        R.string.academic_year_semester,
                        R.string.course_number,
                        R.string.course_name,
                        R.string.course_category,
                        R.string.credit,
                        R.string.academic_year_semester,
                        R.string.achievement_course_number,
                        R.string.achievement_course_name,
                        R.string.achievement_course_category,
                        R.string.achievement_credit,
                        R.string.is_passed,
                        R.string.achievement_point
                    ),
                    arrayOf(
                        "acadYearSemester",
                        "courseNumber",
                        "courseName",
                        "courseCategoryName",
                        "credit",
                        "achievementAcadYearSemester",
                        "achievementCourseNumber",
                        "achievementCourseName",
                        "achievementCourseCategoryName",
                        "achievementCredit",
                        "ispassed",
                        "achievementPoint"
                    )
                )
                list.add(
                    SectionData(
                        title = item.getString("courseName"),
                        footerMenus = mutableStateListOf(
                            MenuItem(view) {
                                activity?.startActivity(
                                    Intent(activity, CourseDetailActivity::class.java)
                                        .putExtra("code", item.getString("courseNumber"))
                                )
                                true
                            },
                        ),
                        rows = rows
                    )
                )
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
        onNavigationClick = { backStack.navigateBack(activity) },
        sharedTransitionScope = sharedTransitionScope,
        animatedVisibilityScope = animatedVisibilityScope,
        sharedKey = "CourseCompletion",
        topBarMenus = {
            listOf(
                exportMarkdownMenuItem(
                    backStack,
                    listOf(creditHoursSections, courseCompletionSections),
                    tabs, stringResource(R.string.course_completion)
                )
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
