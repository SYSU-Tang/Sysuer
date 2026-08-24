package com.sysu.edu.academic

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import com.sysu.edu.BaseActivity
import com.sysu.edu.R
import com.sysu.edu.view.ActivityPager
import com.sysu.edu.view.StaggerScreen
import com.sysu.edu.view.exportMarkdownMenuItem

class CourseSelectedActivity : BaseActivity() {
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		enableEdgeToEdge()
		val viewModel: CourseSelectedViewModel by viewModels()
		setContent {
			val context = LocalContext.current
			LaunchedEffect(Unit) {
				viewModel.reFetchCourseList()
			}
			LaunchedEffect(Unit) {
				viewModel.navigationEvents.collect { nav ->
					context.startActivity(Intent(context, CourseDetailActivity::class.java).putExtra("id", nav.teachingClassId).putExtra("code", nav.courseNum).putExtra("class", nav.teachingClassNum))
				}
			}
			var searchQuery by remember { mutableStateOf("") }
			LaunchedEffect(searchQuery) {
				viewModel.reFetchCourseList(searchQuery)
			}
			ActivityPager(title = stringResource(R.string.course_selected), onNavigationClick = { supportFinishAfterTransition() }, isNestedScrollEnabled = false, topBarMenus = {
				listOf(exportMarkdownMenuItem(viewModel.sections, stringResource(R.string.course_selected), stringResource(R.string.course_selected)))
			}, topBarContent = {
				OutlinedTextField(value = searchQuery,
				                  onValueChange = {
					                  searchQuery = it
				                  },
				                  trailingIcon = { if (searchQuery.isNotEmpty()) IconButton(onClick = { searchQuery = "" }) { Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.clear)) } },
				                  label = { Text(stringResource(R.string.search_course)) },
				                  singleLine = true,
				                  modifier = Modifier
					                  .fillMaxWidth()
					                  .padding(dimensionResource(R.dimen.horizontal_padding), dimensionResource(R.dimen.vertical_padding)))
			}) {
				StaggerScreen(sections = viewModel.sections, onScrollBottom = {
					if (viewModel.hasMore()) viewModel.fetchCourseList()
				})
			}
		}
	}
}