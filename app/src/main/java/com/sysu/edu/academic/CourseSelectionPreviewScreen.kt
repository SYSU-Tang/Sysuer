package com.sysu.edu.academic

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedAssistChip
import androidx.compose.material3.ElevatedFilterChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.alibaba.fastjson2.JSONObject
import com.mikepenz.markdown.compose.Markdown
import com.mikepenz.markdown.compose.components.markdownComponents
import com.mikepenz.markdown.compose.elements.MarkdownTable
import com.mikepenz.markdown.compose.elements.MarkdownTableHeader
import com.mikepenz.markdown.compose.elements.MarkdownTableRow
import com.mikepenz.markdown.m3.markdownColor
import com.mikepenz.markdown.m3.markdownTypography
import com.mikepenz.markdown.model.rememberMarkdownState
import com.sysu.edu.R
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class) @Composable fun CourseSelectionPreviewScreen(
	viewModel: CourseSelectionPreviewViewModel = viewModel(),
	onNavigateToFilter: (CourseFilterNameData, CourseFilterValueData) -> Unit = { _, _ -> },
	onNavigateToDetail: (id: String, code: String, className: String) -> Unit = { _, _, _ -> },
                                                                                                                  ) {
	val courses = viewModel.courses
	val filterName = viewModel.filterName
	val filterValue = viewModel.filterValue
	val gridState = rememberLazyStaggeredGridState()
	val hiddenSelectedStatus by viewModel.hiddenSelectedStatus.collectAsStateWithLifecycle()
	LaunchedEffect(Unit) {
		viewModel.loadMore()
	}
	
	LaunchedEffect(gridState) {
		snapshotFlow { gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }.distinctUntilChanged().filterNotNull().collect { lastVisibleIndex ->
			if (lastVisibleIndex >= courses.size - 3 && !viewModel.isLoading.value) {
				viewModel.loadMore()
			}
		}
	}
	Column(
		modifier = Modifier.fillMaxWidth(),
		horizontalAlignment = Alignment.CenterHorizontally,
	      ) {
		SingleChoiceSegmentedButtonRow(modifier = Modifier
			.fillMaxWidth()
			.padding(dimensionResource(R.dimen.horizontal_margin), dimensionResource(R.dimen.vertical_margin))) {
			intArrayOf(1, 4, 2).zip(intArrayOf(R.string.my_major, R.string.public_selection, R.string.transdisciplinary)).forEachIndexed { index, (type, text) ->
				SegmentedButton(
					selected = viewModel.type.intValue == type,
					onClick = { viewModel.setType(type) },
					icon = {},
					shape = SegmentedButtonDefaults.itemShape(index, 3),
				               ) {
					Text(stringResource(text))
				}
			}
		}
		val activeFilters = listOf(
			filterName.courseName,
			filterName.studyCampusId,
			filterName.week,
			filterName.classTimes,
			filterName.courseUnitNum,
			filterName.teachingTeacherNum,
			filterName.teachingLanguageCode,
			filterName.specialClassCode,
		                          ).filter { !it.isNullOrEmpty() }
		FlowRow(
			modifier = Modifier
				.fillMaxWidth()
				.padding(horizontal = dimensionResource(R.dimen.horizontal_margin)),
			horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.horizontal_gap)),
		       ) {
			ElevatedFilterChip(
				leadingIcon = if (hiddenSelectedStatus) {
					{
						Icon(imageVector = Icons.Filled.Done, contentDescription = stringResource(R.string.hidden_selected), modifier = Modifier.size(FilterChipDefaults.IconSize))
					}
				}
				else {
					null
				},
				selected = hiddenSelectedStatus,
				onClick = { viewModel.setHiddenSelectedStatus(!hiddenSelectedStatus) },
				label = { Text(stringResource(R.string.hidden_selected)) },
			                  )
			activeFilters.forEach { filter ->
				ElevatedFilterChip(
					selected = true,
					onClick = {},
					label = { Text("$filter") },
				                  )
			}
			ElevatedAssistChip(
				onClick = {
					onNavigateToFilter(filterName, filterValue)
				},
				leadingIcon = { Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.add_filter)) },
				label = { Text(stringResource(R.string.add_filter)) },
			                  )
		}
		LazyVerticalStaggeredGrid(
			columns = StaggeredGridCells.Adaptive(240.dp),
			state = gridState,
			contentPadding = PaddingValues(dimensionResource(R.dimen.horizontal_margin), dimensionResource(R.dimen.vertical_margin)),
			modifier = Modifier
				.fillMaxSize()
				.nestedScroll(rememberNestedScrollInteropConnection()),
			horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.horizontal_margin)),
			verticalItemSpacing = dimensionResource(R.dimen.vertical_margin),
		                         ) {
			items(courses, key = { it.getString("teachingClassId") }) { item ->
				CourseCard(
					item = item,
					onClick = {
						onNavigateToDetail(
							item.getString("teachingClassId"),
							item.getString("courseNum"),
							item.getString("teachingClassNum"),
						                  )
					},
					onCollect = { viewModel.like(item.getString("teachingClassId")) },
				          )
			}
			
			if (viewModel.isLoading.value) {
				item(span = StaggeredGridItemSpan.FullLine) {
					Box(
						modifier = Modifier
							.fillMaxWidth()
							.padding(16.dp),
						contentAlignment = Alignment.Center,
					   ) {
						LinearWavyProgressIndicator()
					}
				}
			}
		}
	}
}

@Composable private fun CourseCard(
	item: JSONObject,
	onClick: () -> Unit,
	onCollect: () -> Unit,
                                  ) {
	val key = arrayOf("courseCategoryName",
	                  "courseUnitName",
	                  "scheduleExamTime",
	                  "examFormName",
	                  "credit",
	                  "teachingClassId",
	                  "teachingClassNum",
	                  "teachingClassName",
	                  "courseNum",
	                  "baseReceiveNum",
	                  "addReceiveNum",
	                  "courseSelectedNum",
	                  "filterSelectedNum",
	                  "remainNum",
	                  "minorReceiveNum")
	val name = arrayOf("课程类别", "开设学院", "考试时间", "考核方式", "学分", "教学班ID", "教学班号", "教学班名", "课程号", "基本接收人数", "新增接收人数", "已选人数", "筛选中人数", "剩余人数", "辅修接收人数")
	val none = stringResource(R.string.none)
	val teachingTimePlace = item.getString("teachingTimePlace", "")
	val markdown = remember(item) {
		val md = StringBuilder("|老师|时间|地点|\n|:-----:|:----:|:----:|\n|").append(teachingTimePlace.replace(";", " | ").replace(",", " |\n| ")).append("|\n")
		key.forEachIndexed { i, v ->
			val value = item.getString(v, none)
			md.append("\n${name[i]}：**${value}**\n")
		}
		"$md"
	}
	val isCollect = item.getString("collectionStatus") == "1"
	val isSelect = item.getString("selectedStatus") == "1"
	Card(
		onClick = onClick,
		modifier = Modifier.fillMaxWidth(),
		colors = CardDefaults.cardColors(
			containerColor = MaterialTheme.colorScheme.surfaceContainer,
		                                ),
	    ) {
		Column(
			modifier = Modifier.padding(dimensionResource(R.dimen.content_padding)),
		      ) {
			println(item)
			Row(
				modifier = Modifier.fillMaxWidth(),
				verticalAlignment = Alignment.CenterVertically,
			   ) {
				if (isCollect) Icon(Icons.Rounded.Bookmark, contentDescription = stringResource(R.string.collect), tint = MaterialTheme.colorScheme.primary)
				if (isSelect) Icon(Icons.Rounded.Check, contentDescription = stringResource(R.string.select), tint = MaterialTheme.colorScheme.primary)
				if (isCollect || isSelect) Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
				Text(
					text = item.getString("courseName", ""),
					style = MaterialTheme.typography.headlineSmall,
					color = MaterialTheme.colorScheme.primary,
				    )
			}
			Spacer(modifier = Modifier.height(dimensionResource(R.dimen.vertical_margin)))
			Markdown(
				rememberMarkdownState(markdown),
				modifier = Modifier.fillMaxWidth(),
				colors = markdownColor(),
				typography = markdownTypography(),
				components = markdownComponents(table = { model ->
					MarkdownTable(
						content = model.content,
						node = model.node,
						style = model.typography.table,
						headerBlock = { content, header, tableWidth, style ->
							MarkdownTableHeader(
								content = content,
								header = header,
								tableWidth = tableWidth,
								style = style,
								maxLines = Int.MAX_VALUE,
								overflow = TextOverflow.Clip,
							                   )
						},
						rowBlock = { content, row, tableWidth, style ->
							MarkdownTableRow(
								content = content,
								header = row,
								tableWidth = tableWidth,
								style = style,
								maxLines = Int.MAX_VALUE,
								overflow = TextOverflow.Clip,
							                )
						},
					             )
				}),
			        )
			Spacer(modifier = Modifier.height(dimensionResource(R.dimen.vertical_margin)))
			Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.horizontal_gap)), verticalAlignment = Alignment.CenterVertically) {
				FilledTonalButton(modifier = Modifier.weight(1f), shapes = ButtonDefaults.shapes(), onClick = onCollect) { Text(stringResource(if (isCollect) R.string.cancel_collect else R.string.collect)) }
				FilledTonalButton(modifier = Modifier.weight(1f), shapes = ButtonDefaults.shapes(), onClick = onClick) { Text(stringResource(R.string.open)) }
			}
		}
	}
}