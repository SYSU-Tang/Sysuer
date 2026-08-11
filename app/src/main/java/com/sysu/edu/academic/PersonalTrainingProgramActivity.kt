package com.sysu.edu.academic

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModelProvider
import com.alibaba.fastjson2.JSONObject
import com.sysu.edu.BaseActivity
import com.sysu.edu.R
import com.sysu.edu.api.CommonUtil.extractValue
import com.sysu.edu.api.DataStoreManager
import com.sysu.edu.browser.RichTextActivity
import com.sysu.edu.view.ActivityPager
import com.sysu.edu.view.MenuItem
import com.sysu.edu.view.RowData
import com.sysu.edu.view.SectionData
import com.sysu.edu.view.StaggerScreen
import com.sysu.edu.view.toMarkdown

class PersonalTrainingProgramActivity : BaseActivity() {
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		enableEdgeToEdge()
		val viewModel = ViewModelProvider(this)[PersonalTrainingProgramViewModel::class.java]
		
		setContent {
			val basicInfo by viewModel.basicInfo.observeAsState(null)
			val courseTable by viewModel.courseTable.observeAsState(null)
			val creditList by viewModel.creditList.observeAsState(emptyList())
			
			LaunchedEffect(Unit) {
				viewModel.fetchMyProgram()
			}
			val basicInfoSections = remember(basicInfo) {
				mutableStateListOf<SectionData>().also { list ->
					basicInfo?.let { data ->
						list.add(SectionData(title = data.getString("programName"),
						                     rows = extractValue(this@PersonalTrainingProgramActivity,
						                                         data,
						                                         intArrayOf(R.string.college, R.string.grade, R.string.profession, R.string.profession_code, R.string.degree_type, R.string.school_length, R.string.audit_status),
						                                         arrayOf("collegeName", "grade", "professionName", "professionCode", "degreeType", "schoolLength", "auditStatus"))))
						val target = data.getString("target")
						if (!target.isNullOrEmpty()) {
							list.add(SectionData(title = getString(R.string.training_target), rows = mutableStateListOf(RowData(null, target))))
						}
						val requirement = data.getString("requirement")
						if (!requirement.isNullOrEmpty()) {
							val reqRows = mutableStateListOf<RowData>()
							requirement.split("\n").forEach { line ->
								if (line.isBlank()) return@forEach
								val colonIndex = line.indexOfAny(charArrayOf('：', ':'))
								reqRows.add(if (colonIndex > 0) RowData(line.substring(0, colonIndex).trim(), line.substring(colonIndex + 1).trim()) else RowData(line.trim(), null))
							}
							list.add(SectionData(title = getString(R.string.graduation_requirement), rows = reqRows))
						}
					}
				}
			}
			val courseCategoryKeys = listOf("publicRequired", "proRequired", "proSelectBigClass", "honorCourseBigClass")
			val courseTotalKeys = listOf("publicRequiredVo", "proRequiredVo", "proSelectVo", "honorCourseVo")
			val courseCategoryNames = listOf(R.string.public_compulsory, R.string.major_compulsory, R.string.major_selective, R.string.honor)
			val courseCategorySections = remember(courseTable) {
				mutableStateListOf<SnapshotStateList<SectionData>>().also { list ->
					courseTable?.let { data ->
						courseCategoryKeys.zip(courseTotalKeys).forEach { (key, totalKey) ->
							val courses = data.getJSONArray(key)?.filterIsInstance<JSONObject>() ?: emptyList()
							val sections = mutableStateListOf<SectionData>()
							val summary = data.getJSONArray(totalKey)?.filterIsInstance<JSONObject>()?.firstOrNull()
							if (summary != null) {
								sections.add(SectionData(title = getString(R.string.credit_period_distribution),
								                         rows = extractValue(this@PersonalTrainingProgramActivity,
								                                             summary,
								                                             intArrayOf(R.string.required_credit,
								                                                        R.string.curriculum_count,
								                                                        R.string.total_all_credit,
								                                                        R.string.total_all_period,
								                                                        R.string.total_theoretical_period,
								                                                        R.string.total_practice_period,
								                                                        R.string.total_practice_period_week),
								                                             arrayOf("requiredCredit", "curriculumCount", "totalAllCredit", "totalAllPeriod", "totalTheoreticalPeriod", "totalPracticeTestPeriod", "totalPracticeTestPeriodWeek"))))
							}
							courses.forEach { course ->
								sections.add(SectionData(title = course.getString("courseName"),
								                         rows = extractValue(this@PersonalTrainingProgramActivity,
								                                             course,
								                                             intArrayOf(R.string.course_number,
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
								                                                        R.string.practice_credit),
								                                             arrayOf("courseNumber",
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
								                                                     "practiceTestCredit")),
								                         footerMenus = mutableStateListOf(MenuItem(title = getString(R.string.course_draft), onClick = {
									                         startActivity(Intent(this@PersonalTrainingProgramActivity, CourseDetailActivity::class.java).putExtra("code", course.getString("courseNumber")).putExtra("id", course.getString("courseId")))
									                         true
								                         }))))
							}
							list.add(sections)
						}
					}
				}
			}
			val practiceSections = remember(courseTable) {
				mutableStateListOf<SectionData>().also { list ->
					courseTable?.let { data ->
						data.getJSONArray("practiceList")?.filterIsInstance<JSONObject>()?.forEach { course ->
							list.add(SectionData(title = course.getString("courseName"),
							                     rows = extractValue(this@PersonalTrainingProgramActivity,
							                                         course,
							                                         intArrayOf(R.string.course_number,
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
							                                                    R.string.practice_credit),
							                                         arrayOf("courseNumber",
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
							                                                 "practiceTestCredit")),
							                     footerMenus = mutableStateListOf(MenuItem(title = getString(R.string.course_draft), onClick = {
								                     startActivity(Intent(this@PersonalTrainingProgramActivity, CourseDetailActivity::class.java).putExtra("code", course.getString("courseNumber")).putExtra("id", course.getString("courseId")))
								                     true
							                     }))))
						}
					}
				}
			}
			val creditSections = remember(creditList) {
				mutableStateListOf<SectionData>().also { list ->
					creditList.forEach { item ->
						list.add(SectionData(title = item.getString("parentCurriculumCategoryName"),
						                     rows = extractValue(this@PersonalTrainingProgramActivity,
						                                         item,
						                                         intArrayOf(R.string.course_category, R.string.required_credit, R.string.credit_ratio, R.string.remark),
						                                         arrayOf("parentCurriculumCategoryName", "parentRequiredCredit", "parentRatio", "remark"))))
					}
				}
			}
			val creditPeriodSections = remember(courseTable) {
				mutableStateListOf<SectionData>().also { list ->
					courseTable?.let { data ->
						val periods = data.getJSONArray("creditPeriod")?.filterIsInstance<JSONObject>() ?: emptyList()
						periods.filter { it.getString("id") != "999" }.forEach { period ->
							list.add(SectionData(title = "${period.getString("year")}-${period.getString("term")} (${period.getString("schoolYear")})",
							                     rows = extractValue(this@PersonalTrainingProgramActivity,
							                                         period,
							                                         intArrayOf(R.string.public_compulsory_credit,
							                                                    R.string.public_compulsory_hour,
							                                                    R.string.major_compulsory_credit,
							                                                    R.string.major_compulsory_hour,
							                                                    R.string.major_select_sug_credit,
							                                                    R.string.major_select_hour,
							                                                    R.string.major_select_set_credit,
							                                                    R.string.public_select_credit,
							                                                    R.string.public_select_hour,
							                                                    R.string.total_credit,
							                                                    R.string.total_hour),
							                                         arrayOf("publicRequiredCredit",
							                                                 "publicRequiredAge",
							                                                 "proRequiredCredit",
							                                                 "proRequiredAge",
							                                                 "proSelectCredit",
							                                                 "proSelectAge",
							                                                 "proSelectSetCredit",
							                                                 "publicSelectCredit",
							                                                 "publicSelectAge",
							                                                 "totalCredit",
							                                                 "totalAge"))))
						}
					}
				}
			}
			val tabs = remember(courseTable) {
				mutableStateListOf<Int>().also { list ->
					list.add(R.string.basic_info)
					courseCategoryKeys.forEachIndexed { i, key ->
						val courses = courseTable?.getJSONArray(key)?.filterIsInstance<JSONObject>() ?: emptyList()
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
			val pageSections = remember(tabs, basicInfoSections, courseCategorySections, practiceSections, creditSections, creditPeriodSections) {
				mutableStateListOf<SnapshotStateList<SectionData>>().also { list ->
					list.add(basicInfoSections)
					list.addAll(courseCategorySections)
					if (practiceSections.isNotEmpty()) list.add(practiceSections)
					list.add(creditSections)
					list.add(creditPeriodSections)
				}
			}
			
			ActivityPager(title = stringResource(R.string.personal_training_program), tabs = tabs.map { MenuItem(stringResource(it)) }, onNavigationClick = { supportFinishAfterTransition() }, actions = {
				IconButton(onClick = {
					val markdown = pageSections.joinToString("\n\n") { it.toMarkdown() }
					DataStoreManager.saveContent(this@PersonalTrainingProgramActivity, getString(R.string.personal_training_program), markdown) {
						startActivity(Intent(this@PersonalTrainingProgramActivity, RichTextActivity::class.java).putExtra("type", DataStoreManager.ContentType.MARKDOWN.name).putExtra("title", getString(R.string.personal_training_program)))
					}
				}) {
					Icon(painter = painterResource(R.drawable.export), contentDescription = stringResource(R.string.export))
				}
			}) { page ->
				StaggerScreen(sections = pageSections.getOrElse(page) { basicInfoSections }, isHideNull = page >= 1)
			}
		}
	}
}