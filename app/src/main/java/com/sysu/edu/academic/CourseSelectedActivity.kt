package com.sysu.edu.academic

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModelProvider
import com.sysu.edu.BaseActivity
import com.sysu.edu.R
import com.sysu.edu.api.CommonUtil.extractValue
import com.sysu.edu.view.ActivityPager
import com.sysu.edu.view.RowData
import com.sysu.edu.view.SectionData
import com.sysu.edu.view.StaggerScreen
import com.sysu.edu.view.exportMarkdownMenuItem
import java.util.regex.Pattern

class CourseSelectedActivity : BaseActivity() {
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		enableEdgeToEdge()
		val viewModel = ViewModelProvider(this)[CourseSelectedViewModel::class.java]
		
		setContent {
			val courseList by viewModel.courseList.observeAsState(emptyList())
			
			LaunchedEffect(Unit) {
				viewModel.reFetchCourseList()
			}
			var searchQuery by remember { mutableStateOf("") }
			val sections = remember(courseList) {
				mutableStateListOf<SectionData>().also { list ->
					courseList.forEach { item ->
						val rows = mutableStateListOf<RowData>()
						val teachingTimePlace = item.getString("teachingTimePlace")
						if (teachingTimePlace.isNullOrEmpty()) {
							rows.add(RowData(getString(R.string.course_arrangement), getString(R.string.none)))
						}
						else {
							Pattern.compile(",").splitAsStream(teachingTimePlace).forEach { s ->
								rows.add(RowData(getString(R.string.course_arrangement), s.replace(";", "/")))
							}
						}
						rows.addAll(extractValue(this@CourseSelectedActivity,
						                         item,
						                         intArrayOf(R.string.course_name,
						                                    R.string.course_category,
						                                    R.string.open_unit,
						                                    R.string.exam_time,
						                                    R.string.exam_mode,
						                                    R.string.credit,
						                                    R.string.teaching_class_id,
						                                    R.string.class_number,
						                                    R.string.class_name,
						                                    R.string.course_number),
						                         arrayOf("courseName", "courseCategoryName", "courseUnitName", "scheduleExamTime", "examFormName", "credit", "teachingClassId", "teachingClassNum", "teachingClassName", "courseNum")))
						list.add(SectionData(title = item.getString("courseName"), rows = rows, footerMenus = mutableStateListOf(com.sysu.edu.view.MenuItem(title = getString(R.string.course_detail), onClick = {
							startActivity(Intent(this@CourseSelectedActivity, CourseDetailActivity::class.java).putExtra("id", item.getString("teachingClassId"))
								              .putExtra("code", item.getString("courseNum"))
								              .putExtra("class", item.getString("teachingClassNum")))
							true
						}))))
					}
				}
			}
			
			ActivityPager(title = stringResource(R.string.course_selected), onNavigationClick = { supportFinishAfterTransition() }, isNestedScrollEnabled = false, topBarMenus = {
				listOf(exportMarkdownMenuItem(sections, stringResource(R.string.course_selected), stringResource(R.string.course_selected)))
			}) {
				Column(modifier = Modifier.fillMaxSize()) {
					OutlinedTextField(value = searchQuery,
					                  onValueChange = { newQuery ->
						                  searchQuery = newQuery
						                  viewModel.courseName = newQuery
						                  viewModel.reFetchCourseList()
					                  },
					                  label = { androidx.compose.material3.Text(stringResource(R.string.search_course)) },
					                  singleLine = true,
					                  modifier = Modifier
						                  .fillMaxWidth()
						                  .padding(dimensionResource(R.dimen.horizontal_padding), dimensionResource(R.dimen.vertical_padding)))
					
					StaggerScreen(sections = sections, onScrollBottom = {
						if (viewModel.hasMore()) viewModel.fetchCourseList()
					})
				}
			}
		}
	}
}