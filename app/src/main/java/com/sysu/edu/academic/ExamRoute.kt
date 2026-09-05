package com.sysu.edu.academic

import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenu
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import com.alibaba.fastjson2.JSONObject
import com.sysu.edu.R
import com.sysu.edu.nav.navigateBack
import com.sysu.edu.view.ActivityPager
import com.sysu.edu.view.StaggerScreen
import com.sysu.edu.view.exportMarkdownMenuItem

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun ExamRoute(
    backStack: MutableList<NavKey>,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
) {
    val examViewModel: ExamViewModel = viewModel()
    val terms by examViewModel.termList.collectAsStateWithLifecycle()
    val term by examViewModel.term.collectAsStateWithLifecycle()
    val examWeeks by examViewModel.examWeekList.collectAsStateWithLifecycle()
    val examWeek by examViewModel.examWeek.collectAsStateWithLifecycle()
    var termExpanded by remember { mutableStateOf(false) }
    var examExpanded by remember { mutableStateOf(false) }
    val activity = LocalActivity.current

    LaunchedEffect(Unit) {
        examViewModel.getTerms()
    }
    LaunchedEffect(terms) {
        examViewModel.getTerm()
    }
    LaunchedEffect(term) {
        examViewModel.getExamWeek(term)
    }
    LaunchedEffect(examWeek, term) {
        if (examWeek != null && term != null) examViewModel.getResult()
    }
    ActivityPager(
        title = stringResource(R.string.exam),
        onNavigationClick = { backStack.navigateBack(activity) },
        isNestedScrollEnabled = false,
        sharedTransitionScope = sharedTransitionScope,
        animatedVisibilityScope = animatedVisibilityScope,
        sharedKey = "Exam",
        topBarMenus = {
            listOf(
                exportMarkdownMenuItem(
                    backStack,
                    examViewModel.sections,
                    stringResource(R.string.exam),
                    stringResource(R.string.exam)
                ),
            )
        },
        topBarContent = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        dimensionResource(R.dimen.horizontal_padding),
                        dimensionResource(R.dimen.vertical_padding)
                    ),
                verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.vertical_margin)),
            ) {
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
                    ExposedDropdownMenu(expanded = termExpanded, onDismissRequest = { termExpanded = false }) {
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
                                modifier = Modifier.background(if (isSelected) Color.LightGray else Color.Transparent),
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
                    ExposedDropdownMenu(expanded = examExpanded, onDismissRequest = { examExpanded = false }) {
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
                                modifier = Modifier.background(if (isSelected) Color.LightGray else Color.Transparent),
                            )
                        }
                    }
                }
            }
        }) {
        StaggerScreen(sections = examViewModel.sections)
    }
}
