package com.sysu.edu.academic

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import com.alibaba.fastjson2.JSONObject
import com.sysu.edu.R
import com.sysu.edu.api.CommonUtil.extractValue
import com.sysu.edu.nav.CourseDetail
import com.sysu.edu.nav.navigateBack
import com.sysu.edu.view.ActivityPager
import com.sysu.edu.view.MenuItem
import com.sysu.edu.view.RowData
import com.sysu.edu.view.SectionData
import com.sysu.edu.view.StaggerScreen
import com.sysu.edu.view.exportMarkdownMenuItem
import com.sysu.edu.nav.PersonalTrainingProgram as PersonalTrainingProgramKey

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun PersonalTrainingProgramRoute(
	backStack: MutableList<NavKey>,
	key: PersonalTrainingProgramKey,
	sharedTransitionScope: SharedTransitionScope? = null,
	animatedVisibilityScope: AnimatedVisibilityScope? = null,
) {
	val viewModel: PersonalTrainingProgramViewModel = viewModel()
	val context = LocalContext.current

	val basicInfo by viewModel.basicInfo.observeAsState(null)
	val courseTable by viewModel.courseTable.observeAsState(null)
	val creditList by viewModel.creditList.observeAsState(emptyList())

	LaunchedEffect(key.programId) {
		if (key.programId != null) {
			viewModel.programId = key.programId
		} else if (viewModel.programId == null) {
			viewModel.fetchMyProgram()
		}
	}

	LaunchedEffect(viewModel.programId) {
		if (!viewModel.programId.isNullOrEmpty()) {
			viewModel.fetchBasicInfo()
		}
	}

	val trainingTargetTitle = stringResource(R.string.training_target)
	val graduationRequirementTitle = stringResource(R.string.graduation_requirement)
	val basicInfoSections = remember(basicInfo) {
		mutableStateListOf<SectionData>().also { list ->
			basicInfo?.let { data ->
				list.add(
					SectionData(
						title = data.getString("programName"), rows = extractValue(
							context, data, intArrayOf(
								R.string.college,
								R.string.grade,
								R.string.profession,
								R.string.profession_code,
								R.string.degree_type,
								R.string.school_length,
								R.string.audit_status
							), arrayOf(
								"collegeName",
								"grade",
								"professionName",
								"professionCode",
								"degreeType",
								"schoolLength",
								"auditStatus"
							)
						)
					)
				)
				val target = data.getString("target")
				if (!target.isNullOrEmpty()) {
					list.add(
						SectionData(
							title = trainingTargetTitle,
							rows = mutableStateListOf(RowData(null, target))
						)
					)
				}
				val requirement = data.getString("requirement")
				if (!requirement.isNullOrEmpty()) {
					val reqRows = mutableStateListOf<RowData>()
					requirement.split("\n").forEach { line ->
						if (line.isBlank()) return@forEach
						val colonIndex = line.indexOfAny(charArrayOf('：', ':'))
						reqRows.add(
							if (colonIndex > 0) RowData(
								line.substring(0, colonIndex).trim(),
								line.substring(colonIndex + 1).trim()
							)
							else RowData(line.trim(), null)
						)
					}
					list.add(SectionData(title = graduationRequirementTitle, rows = reqRows))
				}
			}
		}
	}

	val courseCategoryKeys =
		listOf("publicRequired", "proRequired", "proSelectBigClass", "honorCourseBigClass")
	val courseTotalKeys =
		listOf("publicRequiredVo", "proRequiredVo", "proSelectVo", "honorCourseVo")
	val courseCategoryNames = listOf(
		R.string.public_compulsory,
		R.string.major_compulsory,
		R.string.major_selective,
		R.string.honor
	)
	val creditPeriodDistributionTitle = stringResource(R.string.credit_period_distribution)
	val courseDraftTitle = stringResource(R.string.course_draft)
	val courseCategorySections = remember(courseTable) {
		mutableStateListOf<SnapshotStateList<SectionData>>().also { list ->
			courseTable?.let { data ->
				courseCategoryKeys.zip(courseTotalKeys).forEach { (key, totalKey) ->
					val courses =
						data.getJSONArray(key)?.filterIsInstance<JSONObject>() ?: emptyList()
					val sections = mutableStateListOf<SectionData>()
					val summary =
						data.getJSONArray(totalKey)?.filterIsInstance<JSONObject>()?.firstOrNull()
					if (summary != null) {
						sections.add(
							SectionData(
								title = creditPeriodDistributionTitle, rows = extractValue(
									context, summary, intArrayOf(
										R.string.required_credit,
										R.string.curriculum_count,
										R.string.total_all_credit,
										R.string.total_all_period,
										R.string.total_theoretical_period,
										R.string.total_practice_period,
										R.string.total_practice_period_week
									), arrayOf(
										"requiredCredit",
										"curriculumCount",
										"totalAllCredit",
										"totalAllPeriod",
										"totalTheoreticalPeriod",
										"totalPracticeTestPeriod",
										"totalPracticeTestPeriodWeek"
									)
								)
							)
						)
					}
					courses.forEach { course ->
						sections.add(
							SectionData(
								title = course.getString("courseName"),
								rows = extractValue(
									context, course, intArrayOf(
										R.string.course_number,
										R.string.course_category,
										R.string.course_sub_class,
										R.string.course_english_name,
										R.string.initiation_semester,
										R.string.credit,
										R.string.week_period,
										R.string.total_hours,
										R.string.theoretical_period,
										R.string.practice_period,
										R.string.teaching_type,
										R.string.course_manager,
										R.string.practice_credit
									), arrayOf(
										"courseNumber",
										"courseCategoryName",
										"courseSubClassName",
										"courseEN",
										"initiationSemesterAnnotation",
										"credit",
										"weekPeriod",
										"totalPeriod",
										"theoreticalPeriod",
										"practiceTestPeriod",
										"teachingTypeName",
										"courseManager",
										"practiceTestCredit"
									)
								),
								transitionName = "course_${
									course.getString(
										"courseId"
									)
								}_${course.getString("courseNumber")}",
								footerMenus = mutableStateListOf(MenuItem(title = courseDraftTitle) {
									println("courseId: ${course.getString("courseId")} courseNum: ${course.getString("courseNumber")}")
									backStack.add(
										CourseDetail(
											course.getString("courseId"),
											course.getString("courseNumber")
										)
									)
									true
								})
							)
						)
					}
					list.add(sections)
				}
			}
		}
	}

	val practiceSections = remember(courseTable) {
		mutableStateListOf<SectionData>().also { list ->
			courseTable?.let { data ->
				data.getJSONArray("practiceList")?.filterIsInstance<JSONObject>()
					?.forEach { course ->
						list.add(
							SectionData(
								title = course.getString("courseName"),
								rows = extractValue(
									context,
									course,
									intArrayOf(
										R.string.course_number,
										R.string.course_category,
										R.string.course_sub_class,
										R.string.course_english_name,
										R.string.initiation_semester,
										R.string.credit,
										R.string.week_period,
										R.string.total_hours,
										R.string.theoretical_period,
										R.string.practice_period,
										R.string.teaching_type,
										R.string.course_manager,
										R.string.practice_credit
									),
									arrayOf(
										"courseNumber",
										"courseCategoryName",
										"courseSubClassName",
										"courseEN",
										"initiationSemesterAnnotation",
										"credit",
										"weekPeriod",
										"totalPeriod",
										"theoreticalPeriod",
										"practiceTestPeriod",
										"teachingTypeName",
										"courseManager",
										"practiceTestCredit"
									),
								),
								transitionName = "course_${
									course.getString(
										"courseId"
									)
								}_${course.getString("courseNumber")}",
								footerMenus = mutableStateListOf(MenuItem(title = courseDraftTitle) {
									backStack.add(
										CourseDetail(
											course.getString("courseId"),
											course.getString("courseNumber")
										)
									)
									true
								})
							)
						)
					}
			}
		}
	}

	val creditSections = remember(creditList) {
		mutableStateListOf<SectionData>().also { list ->
			creditList.forEach { item ->
				list.add(
					SectionData(
						title = item.getString("parentCurriculumCategoryName"), rows = extractValue(
							context, item, intArrayOf(
								R.string.course_category,
								R.string.required_credit,
								R.string.credit_ratio,
								R.string.remark
							), arrayOf(
								"parentCurriculumCategoryName",
								"parentRequiredCredit",
								"parentRatio",
								"remark"
							)
						)
					)
				)
			}
		}
	}

	val creditPeriodSections = remember(courseTable) {
		mutableStateListOf<SectionData>().also { list ->
			courseTable?.let { data ->
				val periods =
					data.getJSONArray("creditPeriod")?.filterIsInstance<JSONObject>() ?: emptyList()
				periods.filter { it.getString("id") != "999" }.forEach { period ->
					list.add(
						SectionData(
							title = "${period.getString("year")}-${period.getString("term")} (${
								period.getString(
									"schoolYear"
								)
							})", rows = extractValue(
								context, period, intArrayOf(
									R.string.public_compulsory_credit,
									R.string.public_compulsory_hour,
									R.string.major_compulsory_credit,
									R.string.major_compulsory_hour,
									R.string.major_select_sug_credit,
									R.string.major_select_hour,
									R.string.major_select_set_credit,
									R.string.public_select_credit,
									R.string.public_select_hour,
									R.string.total_credit,
									R.string.total_hour
								), arrayOf(
									"publicRequiredCredit",
									"publicRequiredAge",
									"proRequiredCredit",
									"proRequiredAge",
									"proSelectCredit",
									"proSelectAge",
									"proSelectSetCredit",
									"publicSelectCredit",
									"publicSelectAge",
									"totalCredit",
									"totalAge"
								)
							)
						)
					)
				}
			}
		}
	}

	val tabStrings = remember(courseTable) {
		mutableStateListOf<Int>().also { list ->
			list.add(R.string.basic_info)
			courseCategoryKeys.forEachIndexed { i, key ->
				val courses =
					courseTable?.getJSONArray(key)?.filterIsInstance<JSONObject>() ?: emptyList()
				if (courses.isNotEmpty()) {
					list.add(courseCategoryNames[i])
				}
			}
			if (courseTable?.getJSONArray("practiceList")?.isEmpty() == false) {
				list.add(R.string.practice_course)
			}
			list.add(R.string.credit_requirement)
			list.add(R.string.credit_period_distribution)
		}
	}

	val pageSections = remember(
		tabStrings,
		basicInfoSections,
		courseCategorySections,
		practiceSections,
		creditSections,
		creditPeriodSections
	) {
		mutableStateListOf<SnapshotStateList<SectionData>>().also { list ->
			list.add(basicInfoSections)
			list.addAll(courseCategorySections)
			if (practiceSections.isNotEmpty()) list.add(practiceSections)
			list.add(creditSections)
			list.add(creditPeriodSections)
		}
	}

	val tabs = tabStrings.map { MenuItem(stringResource(it)) }

	ActivityPager(
		title = stringResource(R.string.personal_training_program),
		tabs = tabs,
		onNavigationClick = { backStack.navigateBack() },
		topBarMenus = {
			listOf(
				exportMarkdownMenuItem(
					backStack, pageSections, tabs, stringResource(R.string.personal_training_program)
				)
			)
		},
		sharedTransitionScope = sharedTransitionScope,
		animatedVisibilityScope = animatedVisibilityScope,
		sharedKey = "PersonalTrainingProgram_${key.programId}",
		isNestedScrollEnabled = false
	) { page ->
		StaggerScreen(
			sections = pageSections.getOrElse(page) { basicInfoSections }, isHideNull = page >= 1,
			sharedTransitionScope = sharedTransitionScope,
			animatedVisibilityScope = animatedVisibilityScope,
		)
	}
}
