package com.sysu.edu.academic

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.ViewModelProvider
import com.sysu.edu.BaseActivity
import com.sysu.edu.R
import com.sysu.edu.api.CommonUtil.extractValue
import com.sysu.edu.api.DataStoreManager
import com.sysu.edu.browser.RichTextActivity
import com.sysu.edu.view.ActivityPager
import com.sysu.edu.view.InputDialogChip
import com.sysu.edu.view.SectionData
import com.sysu.edu.view.SingleSelectChipDropdown
import com.sysu.edu.view.StaggerScreen
import com.sysu.edu.view.toMarkdown

class GradeForLevelActivity : BaseActivity() {
	@OptIn(ExperimentalLayoutApi::class) override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		enableEdgeToEdge()
		val viewModel = ViewModelProvider(this)[GradeForLevelViewModel::class.java]
		
		setContent {
			val gradeList by viewModel.gradeList.observeAsState(emptyList())
			val trainTypeOptions by viewModel.trainTypeOptions.observeAsState(emptyList())
			val yearOptions by viewModel.yearOptions.observeAsState(emptyList())
			val courseTypeOptions by viewModel.courseTypeOptions.observeAsState(emptyList())
			
			LaunchedEffect(Unit) {
				viewModel.fetchOptions()
				viewModel.fetchGrade()
			}
			var trainTypeValue by remember { mutableStateOf<String?>(null) }
			var yearValue by remember { mutableStateOf<String?>(null) }
			var courseTypeValue by remember { mutableStateOf<String?>(null) }
			var courseNameValue by remember { mutableStateOf("") }
			var courseNumberValue by remember { mutableStateOf("") }
			var minGradeValue by remember { mutableStateOf("") }
			fun onFilterChange() {
				viewModel.trainType = trainTypeValue
				viewModel.year = yearValue
				viewModel.courseType = courseTypeValue
				viewModel.courseName = courseNameValue.ifEmpty { null }
				viewModel.courseNumber = courseNumberValue.ifEmpty { null }
				viewModel.minGrade = minGradeValue.ifEmpty { null }
				viewModel.reFetchGrade()
			}
			
			val sections = remember(gradeList) {
				mutableStateListOf<SectionData>().also { list ->
					gradeList.forEach { item ->
						list.add(SectionData(title = item.getString("courseName"),
						                     rows = extractValue(this@GradeForLevelActivity,
						                                         item,
						                                         intArrayOf(R.string.gpa,
						                                                    R.string.class_number,
						                                                    R.string.course_category,
						                                                    R.string.course_id,
						                                                    R.string.course_name,
						                                                    R.string.course_number,
						                                                    R.string.credit,
						                                                    R.string.exam_nature,
						                                                    R.string.level,
						                                                    R.string.grade,
						                                                    R.string.department,
						                                                    R.string.semester,
						                                                    R.string.total_hours,
						                                                    R.string.training_category,
						                                                    R.string.total_achievement),
						                                         arrayOf("achievementPoint",
						                                                 "classesNum",
						                                                 "courseCategoryName",
						                                                 "courseId",
						                                                 "courseName",
						                                                 "courseNum",
						                                                 "credit",
						                                                 "examNatureName",
						                                                 "finalAchievementStr",
						                                                 "grade",
						                                                 "openClassUnitName",
						                                                 "schoolSemester",
						                                                 "sumHours",
						                                                 "trainingCategoryName",
						                                                 "totalAchievement"))))
					}
				}
			}
			
			ActivityPager(title = stringResource(R.string.grade_for_level), onNavigationClick = { supportFinishAfterTransition() }, isNestedScrollEnabled = false, actions = {
				IconButton(onClick = {
					val markdown = sections.toMarkdown()
					DataStoreManager.saveContent(this@GradeForLevelActivity, getString(R.string.grade_for_level), markdown) {
						startActivity(Intent(this@GradeForLevelActivity, RichTextActivity::class.java).putExtra("type", DataStoreManager.ContentType.MARKDOWN.name).putExtra("title", getString(R.string.grade_for_level)))
					}
				}) {
					Icon(painter = painterResource(R.drawable.export), contentDescription = stringResource(R.string.export))
				}
			}) {
				Column(modifier = Modifier.fillMaxSize()) {
					FlowRow(modifier = Modifier
						.fillMaxWidth()
						.padding(dimensionResource(R.dimen.horizontal_padding), dimensionResource(R.dimen.vertical_padding)),
					        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.horizontal_margin))) {
						SingleSelectChipDropdown(category = stringResource(R.string.train_type),
						                         options = listOf(getString(R.string.reset)) + trainTypeOptions.map { it.getString("dataName") },
						                         optionValues = listOf(null) + trainTypeOptions.map { it.getString("dataNumber") },
						                         selectedValue = trainTypeValue,
						                         onValueChange = { trainTypeValue = it; onFilterChange() })
						SingleSelectChipDropdown(category = stringResource(R.string.year),
						                         options = listOf(getString(R.string.reset)) + yearOptions.map { it.getString("acadYearSemester") },
						                         optionValues = listOf(null) + yearOptions.map { it.getString("acadYearSemester") },
						                         selectedValue = yearValue,
						                         onValueChange = { yearValue = it; onFilterChange() })
						SingleSelectChipDropdown(category = stringResource(R.string.course_type),
						                         options = listOf(getString(R.string.reset)) + courseTypeOptions.map { it.getString("catName") },
						                         optionValues = listOf(null) + courseTypeOptions.map { it.getString("catCode") },
						                         selectedValue = courseTypeValue,
						                         onValueChange = { courseTypeValue = it; onFilterChange() })
						InputDialogChip(stringResource(R.string.course_name), courseNameValue) { courseNameValue = it; onFilterChange() }
						InputDialogChip(stringResource(R.string.course_number), courseNumberValue) { courseNumberValue = it; onFilterChange() }
						InputDialogChip(stringResource(R.string.min_grade), minGradeValue, KeyboardType.Number) { minGradeValue = it; onFilterChange() }
					}
					
					StaggerScreen(sections = sections, onScrollBottom = {
						if (viewModel.hasMore()) viewModel.fetchGrade()
					})
				}
			}
		}
	}
}