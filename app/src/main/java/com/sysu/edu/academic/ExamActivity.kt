package com.sysu.edu.academic

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenu
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import com.alibaba.fastjson2.JSONArray
import com.alibaba.fastjson2.JSONObject
import com.sysu.edu.BaseActivity
import com.sysu.edu.R
import com.sysu.edu.api.CommonUtil.extractValue
import com.sysu.edu.view.ActivityPager
import com.sysu.edu.view.SectionData
import com.sysu.edu.view.StaggerScreen

class ExamActivity : BaseActivity() {
	@OptIn(ExperimentalMaterial3Api::class) override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		val examViewModel = ViewModelProvider(this).get<ExamViewModel>(ExamViewModel::class.java)
		setContent {
			val terms by examViewModel.termList.observeAsState()
			val term by examViewModel.term.observeAsState()
			val examWeeks by examViewModel.examWeekList.observeAsState()
			val examWeek by examViewModel.examWeek.observeAsState()
			val result by examViewModel.examResult.observeAsState()
			LaunchedEffect(Unit) {
				examViewModel.getTerms()
			}
			val names = arrayOf("科目", "考场", "时长", "日期", "学年")
			val keys = arrayOf("examSubjectName", "classroomNumber", "durationTime", "examDate", "acadYear")
			val sections: SnapshotStateList<SectionData> = remember(result) {
				val sections: SnapshotStateList<SectionData> = mutableStateListOf()
				result?.forEach { a: Any? ->
					(a as JSONObject).getJSONObject("timetable").forEach { (_: String?, detail: Any?) ->
						detail?.let {
							(detail as JSONArray).forEach { o: Any? ->
								sections.add(SectionData((o as JSONObject).getString("examSubjectName"), rows = extractValue(o, names, keys)))
							}
						}
					}
				}
				sections
			}
			var termExpanded by remember { mutableStateOf(false) }
			var examExpanded by remember { mutableStateOf(false) }
			val maxMenuHeight = LocalConfiguration.current.screenHeightDp.dp * 0.5f
			ActivityPager(title = stringResource(R.string.exam), onNavigationClick = { supportFinishAfterTransition() }, isNestedScrollEnabled = false, floatingActionButton = {
				ExtendedFloatingActionButton(icon = { Icon(Icons.Filled.Search, null) }, text = { Text(stringResource(R.string.query)) }, onClick = {
					examViewModel.getResult()
				})
			}) {
				Column(modifier = Modifier.fillMaxSize()) {
					FlowRow(modifier = Modifier
						.fillMaxWidth()
						.padding(dimensionResource(R.dimen.horizontal_padding), dimensionResource(R.dimen.vertical_padding)), horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.horizontal_margin))) {
						ExposedDropdownMenuBox(expanded = termExpanded, onExpandedChange = { termExpanded = it }) {
							OutlinedTextField(
								modifier = Modifier
									.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
									.fillMaxWidth(),
								value = term ?: "",
								onValueChange = {},
								readOnly = true,
								singleLine = true,
								label = { Text(stringResource(R.string.term)) },
								trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = termExpanded) },
								colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
							                 )
							ExposedDropdownMenu(expanded = termExpanded, onDismissRequest = { termExpanded = false }, modifier = Modifier.fillMaxWidth().heightIn(max = maxMenuHeight)) {
								terms?.forEach { option ->
									val termOption = (option as JSONObject).getString("acadYearSemester")
									val isSelected = termOption == term
									DropdownMenuItem(
										text = { Text(termOption) },
										onClick = {
											examViewModel.term.value = termOption
											termExpanded = false
										},
										contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
										modifier = Modifier.background(
											if (isSelected) androidx.compose.ui.graphics.Color.LightGray else androidx.compose.ui.graphics.Color.Transparent
										),
									                )
								}
							}
						}
						ExposedDropdownMenuBox(expanded = examExpanded, onExpandedChange = { examExpanded = it }) {
							OutlinedTextField(
								modifier = Modifier
									.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
									.fillMaxWidth(),
								value = examWeek ?: "",
								onValueChange = {},
								readOnly = true,
								singleLine = true,
								label = { Text(stringResource(R.string.exam_week)) },
								trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = examExpanded) },
								colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
							                 )
							ExposedDropdownMenu(expanded = examExpanded, onDismissRequest = { examExpanded = false }, modifier = Modifier.fillMaxWidth().heightIn(max = maxMenuHeight)) {
								examWeeks?.forEach { option ->
									val examWeekName = (option as JSONObject).getString("examWeekName")
									val examWeekId = option.getString("examWeekId")
									val isSelected = examWeekName == examWeek
									DropdownMenuItem(
										text = { Text(examWeekName) },
										onClick = {
											examViewModel.examWeek.value = examWeekName
											examViewModel.examWeekId.value = examWeekId
											examExpanded = false
										},
										contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
										modifier = Modifier.background(
											if (isSelected) androidx.compose.ui.graphics.Color.LightGray else androidx.compose.ui.graphics.Color.Transparent
										),
									                )
								}
							}
						}
					}
					Box(modifier = Modifier.weight(1f)) {
						StaggerScreen(sections)
					}
				}
			}
		}
	}
}