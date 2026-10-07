package com.miyuyan.sysuer.academic

import android.content.ClipData
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedAssistChip
import androidx.compose.material3.ElevatedFilterChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.compose.ui.platform.toClipEntry
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.alibaba.fastjson2.JSONObject
import com.miyuyan.sysuer.R
import com.miyuyan.sysuer.api.CommonUtil.trim
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

/**
 * 主选课页：选课轮次/课程类别/筛选 chips + 瀑布流课程卡片，
 * 支持收藏、选课/退课（确认 Snackbar）、高级筛选跳转与体育课程志愿排序
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CourseSelectionScreen(
	viewModel: CourseSelectionViewModel = viewModel(),
	onNavigateToDetail: (id: String, code: String, className: String) -> Unit = { _, _, _ -> },
) {
	val context = LocalContext.current
	val snackbarHostState = remember { SnackbarHostState() }
	val clipboard = LocalClipboard.current
	val scope = rememberCoroutineScope()
	val isLoading by viewModel.mainIsLoading.collectAsStateWithLifecycle()
	val courses = viewModel.mainCourses
	val gridState = rememberLazyStaggeredGridState()

	fun showSnackbar(text: String) = scope.launch { snackbarHostState.showSnackbar(text) }
	fun copyText(text: String) = scope.launch {
		clipboard.setClipEntry(ClipData.newPlainText("text", text).toClipEntry())
		snackbarHostState.showSnackbar(context.getString(R.string.copy_successfully))
	}
	fun withConfirm(message: String, action: () -> Unit) = scope.launch {
		if (snackbarHostState.showSnackbar(
					message,
					actionLabel = context.getString(R.string.confirm),
					duration = SnackbarDuration.Long,
			) == SnackbarResult.ActionPerformed
		) action()
	}

	LaunchedEffect(gridState) {
		snapshotFlow { gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
			.distinctUntilChanged().filterNotNull().collect { lastVisibleIndex ->
				if (lastVisibleIndex >= courses.size - 3 && !isLoading) {
					viewModel.courseList()
				}
			}
	}

	Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
		LazyVerticalStaggeredGrid(
				columns = StaggeredGridCells.Adaptive(240.dp),
				state = gridState,
				contentPadding = PaddingValues(
						dimensionResource(R.dimen.horizontal_margin),
						dimensionResource(R.dimen.vertical_margin)
				),
				horizontalArrangement = Arrangement.spacedBy(
						dimensionResource(R.dimen.horizontal_margin)
				),
				verticalItemSpacing = dimensionResource(R.dimen.vertical_margin),
				modifier = Modifier
					.fillMaxSize()
					.nestedScroll(rememberNestedScrollInteropConnection()),
		) {
			items(courses, key = { it.getString("teachingClassId") }) { item ->
				val selectedStatus = item.getInteger("selectedStatus") ?: 0
				val isSelected = selectedStatus == 3 || selectedStatus == 4
				val isLike = item.getInteger("collectionStatus") == 1
				CourseCard(
						item = item,
						isSelected = isSelected,
						isLike = isLike,
						onOpen = {
							onNavigateToDetail(
									item.getString("courseId"),
									item.getString("courseNum"),
									item.getString("clazzNum"),
							)
						},
						onToggleSelect = {
							val message = "${context.getString(if (isSelected) R.string.drop_course else R.string.select_course)}? ${
								trim(item.getString("courseName"))
							}"
							withConfirm(message) {
								if (isSelected) {
									viewModel.unselectCourse(
											item.getString("courseId"),
											item.getString("teachingClassId"),
									)
								} else {
									viewModel.selectCourse(item.getString("teachingClassId"))
								}
							}
						},
						onLike = {
							showSnackbar(
									context.getString(R.string.already) + context.getString(
											if (isLike) R.string.unlike else R.string.like
									)
							)
							viewModel.likeMain(item.getString("teachingClassId"))
						},
						onChipClick = { showSnackbar(it) },
						onChipLongClick = { copyText(it) },
				)
			}
			if (isLoading) {
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

/** 筛选面板：选课轮次/类别/筛选 chips + 高级筛选生效项 + 体育课程排序入口（置于 topBarContent） */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun CourseSelectionFilterPanel(
	viewModel: CourseSelectionViewModel,
	onNavigateToFilter: (CourseFilterNameData, CourseFilterValueData) -> Unit,
	onShowPeDialog: () -> Unit,
) {
	val type by viewModel.selectionType.collectAsStateWithLifecycle()
	val category by viewModel.selectionCategory.collectAsStateWithLifecycle()
	val hideSelected by viewModel.hideSelected.collectAsStateWithLifecycle()
	val vacancySort by viewModel.vacancySort.collectAsStateWithLifecycle()
	val hideVacancy by viewModel.hideVacancy.collectAsStateWithLifecycle()
	val onlyCollection by viewModel.onlyCollection.collectAsStateWithLifecycle()
	val showPeSort by viewModel.showPeSort.collectAsStateWithLifecycle()
	val filterName by viewModel.mainFilterName.collectAsStateWithLifecycle()
	val filterValue by viewModel.mainFilterValue.collectAsStateWithLifecycle()
	Column {
		// 选课轮次单选
		FlowRow(
				modifier = Modifier
					.fillMaxWidth()
					.padding(horizontal = dimensionResource(R.dimen.horizontal_margin)),
				horizontalArrangement = Arrangement.spacedBy(
						dimensionResource(R.dimen.horizontal_gap)
				),
		) {
			AssistChip(
					onClick = {},
					label = { Text(stringResource(R.string.type)) },
			)
			listOf(
					R.string.my_major to 1,
					R.string.public_selection to 4,
					R.string.transdisciplinary to 2,
			).forEach { (label, code) ->
				ElevatedFilterChip(
						selected = type == code,
						onClick = { viewModel.setType(code) },
						label = { Text(stringResource(label)) },
				)
			}
		}
		// 课程类别单选（仅本专业轮次）
		AnimatedVisibility(visible = type == 1) {
			FlowRow(
					modifier = Modifier
						.fillMaxWidth()
						.padding(horizontal = dimensionResource(R.dimen.horizontal_margin)),
					horizontalArrangement = Arrangement.spacedBy(
							dimensionResource(R.dimen.horizontal_gap)
					),
			) {
				AssistChip(
						onClick = {},
						label = { Text(stringResource(R.string.category)) },
				)
				listOf(
						R.string.major_compulsory to (1 to 11),
						R.string.major_selective to (1 to 21),
						R.string.school_public_selective to (1 to 30),
						R.string.pe to (3 to 10),
						R.string.en to (5 to 1),
						R.string.public_compulsory to (1 to 10),
						R.string.honor to (1 to 31),
				).forEach { (label, pair) ->
					ElevatedFilterChip(
							selected = category == pair.second && type == pair.first,
							onClick = { viewModel.setCategory(pair.first, pair.second) },
							label = { Text(stringResource(label)) },
					)
				}
			}
		}
		// 筛选 chips
		FlowRow(
				modifier = Modifier
					.fillMaxWidth()
					.padding(horizontal = dimensionResource(R.dimen.horizontal_margin)),
				horizontalArrangement = Arrangement.spacedBy(
						dimensionResource(R.dimen.horizontal_gap)
				),
		) {
			AssistChip(
					onClick = {},
					label = { Text(stringResource(R.string.filter)) },
			)
			listOf(
					R.string.hide_selected to hideSelected,
					R.string.filter_by_vacancy to vacancySort,
					R.string.hide_vacancy to hideVacancy,
					R.string.only_collection to onlyCollection,
			).forEachIndexed { index, (label, checked) ->
				ElevatedFilterChip(
						selected = checked,
						onClick = {
							val flags = listOf(hideSelected, vacancySort, hideVacancy, onlyCollection)
							val new = flags.toMutableList().also { it[index] = !it[index] }
							viewModel.setFlags(new[0], new[1], new[2], new[3])
						},
						label = { Text(stringResource(label)) },
				)
			}
			ElevatedAssistChip(
					onClick = { onNavigateToFilter(filterName, filterValue) },
					label = { Text(stringResource(R.string.add_filter)) },
			)
		}
		// 高级筛选生效项
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
		if (activeFilters.isNotEmpty()) {
			FlowRow(
					modifier = Modifier
						.fillMaxWidth()
						.padding(horizontal = dimensionResource(R.dimen.horizontal_margin)),
					horizontalArrangement = Arrangement.spacedBy(
							dimensionResource(R.dimen.horizontal_gap)
					),
			) {
				activeFilters.forEach { filter ->
					ElevatedAssistChip(
							onClick = {},
							label = { Text("$filter") },
					)
				}
			}
		}
		// 体育课程志愿排序入口
		if (showPeSort) {
			OutlinedButton(
					onClick = onShowPeDialog,
					modifier = Modifier
						.padding(horizontal = dimensionResource(R.dimen.horizontal_margin)),
			) {
				Text(stringResource(R.string.rank))
			}
		}
	}
}

/** 体育课程志愿排序弹层：上下移动调整顺序，确认提交 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PeSortDialog(
	viewModel: CourseSelectionViewModel,
	onDismiss: () -> Unit,
) {
	val courses = remember {
		mutableStateListOf<JSONObject>().apply { addAll(viewModel.peCourses) }
	}
	ModalBottomSheet(onDismissRequest = onDismiss) {
		Column(
				modifier = Modifier
					.fillMaxWidth()
					.padding(horizontal = dimensionResource(R.dimen.horizontal_padding))
					.padding(bottom = dimensionResource(R.dimen.vertical_padding)),
		) {
			Text(
					stringResource(R.string.rank),
					style = MaterialTheme.typography.titleMedium,
					modifier = Modifier.padding(bottom = dimensionResource(R.dimen.vertical_margin)),
			)
			Column(
					modifier = Modifier
						.weight(1f, fill = false)
						.padding(bottom = dimensionResource(R.dimen.vertical_margin)),
			) {
				courses.forEachIndexed { index, course ->
					Row(
							modifier = Modifier
								.fillMaxWidth()
								.padding(vertical = dimensionResource(R.dimen.vertical_margin)),
							verticalAlignment = Alignment.CenterVertically,
					) {
						Column(modifier = Modifier.weight(1f)) {
							Text(
									"${index + 1}. ${course.getString("courseNum")}-${
										course.getString("courseName")
									}",
									style = MaterialTheme.typography.titleSmall,
							)
							Text(
									course.getString("teachingTimePlace", ""),
									style = MaterialTheme.typography.bodySmall,
									color = MaterialTheme.colorScheme.onSurfaceVariant,
							)
						}
						IconButton(
								onClick = { courses.add(index - 1, courses.removeAt(index)) },
								enabled = index > 0,
						) {
							Icon(
									Icons.Rounded.KeyboardArrowUp,
									contentDescription = stringResource(R.string.up),
							)
						}
						IconButton(
								onClick = {
									courses.add(index + 1, courses.removeAt(index))
								},
								enabled = index < courses.size - 1,
						) {
							Icon(
									Icons.Rounded.KeyboardArrowDown,
									contentDescription = stringResource(R.string.down),
							)
						}
					}
				}
			}
			Row(
					modifier = Modifier.fillMaxWidth(),
					horizontalArrangement = Arrangement.End,
			) {
				FilledTonalButton(onClick = {
					viewModel.submitPeOrder(courses)
					onDismiss()
				}) {
					Text(stringResource(R.string.confirm))
				}
			}
		}
	}
}

/** 一条候选课程卡片 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CourseCard(
	item: JSONObject,
	isSelected: Boolean,
	isLike: Boolean,
	onOpen: () -> Unit,
	onToggleSelect: () -> Unit,
	onLike: () -> Unit,
	onChipClick: (String) -> Unit,
	onChipLongClick: (String) -> Unit,
) {
	val selectedStatus = item.getInteger("selectedStatus") ?: 0
	val infoLabels = stringArrayResource(R.array.course_info_labels)
	val seatLabels = stringArrayResource(R.array.seat_info_labels)
	val statusName = stringResource(
			when {
				selectedStatus == 4 -> R.string.status_selected
				selectedStatus == 3 -> R.string.filtering
				selectedStatus == 1 -> R.string.retired
				else -> R.string.unselected
			}
	)
	fun conv(key: String): String = trim(item.getString(key)).replace("\n\n", "\n")
	val divider = @Composable {
		HorizontalDivider(
				modifier = Modifier.padding(vertical = dimensionResource(R.dimen.vertical_margin))
		)
	}
	Card(
			onClick = onOpen,
			colors = CardDefaults.cardColors(
					containerColor = MaterialTheme.colorScheme.surfaceContainer,
			),
	) {
		Column(modifier = Modifier.padding(dimensionResource(R.dimen.content_padding))) {
			Text(
					"${conv("courseNum")}-${conv("courseName")}",
					style = MaterialTheme.typography.titleMedium,
					color = MaterialTheme.colorScheme.primary,
			)
			divider()
			Text(
					conv("teachingTimePlace").replace(";", " | ").replace(",", "\n"),
					style = MaterialTheme.typography.bodyMedium,
			)
			divider()
			FlowRow(
					horizontalArrangement = Arrangement.spacedBy(
							dimensionResource(R.dimen.horizontal_gap)
					),
			) {
				val keys = arrayOf(
						"credit", "clazzNum", "scheduleExamTime", "examFormName"
				)
				keys.forEachIndexed { index, key ->
					InfoChip(
							text = "${infoLabels[index]}：${conv(key)}",
							onClick = onChipClick,
							onLongClick = onChipLongClick,
					)
				}
				InfoChip(
						text = "${infoLabels[4]}：$statusName",
						onClick = onChipClick,
						onLongClick = onChipLongClick,
				)
			}
			divider()
			Row(modifier = Modifier.fillMaxWidth()) {
				Text(
						"${seatLabels[0]}\n${conv("baseReceiveNum")}",
						style = MaterialTheme.typography.bodyMedium,
						modifier = Modifier.weight(1f),
				)
				Text(
						"${seatLabels[1]}\n${conv("filterSelectedNum")}",
						style = MaterialTheme.typography.bodyMedium,
						textAlign = TextAlign.Center,
						modifier = Modifier.weight(1f),
				)
				Text(
						"${seatLabels[2]}\n${conv("courseSelectedNum")}",
						style = MaterialTheme.typography.bodyMedium,
						textAlign = TextAlign.End,
						modifier = Modifier.weight(1f),
				)
			}
			divider()
			Row(
					modifier = Modifier.fillMaxWidth(),
					horizontalArrangement = Arrangement.spacedBy(
							dimensionResource(R.dimen.horizontal_gap)
					),
			) {
				FilledTonalButton(
						modifier = Modifier.weight(1f),
						shapes = ButtonDefaults.shapes(),
						colors = ButtonDefaults.filledTonalButtonColors(
								containerColor = if (isLike) MaterialTheme.colorScheme.primaryContainer
								else MaterialTheme.colorScheme.surfaceContainerHigh,
								contentColor = if (isLike) MaterialTheme.colorScheme.onPrimaryContainer
								else MaterialTheme.colorScheme.onSurface,
						),
						onClick = onLike,
				) {
					Text(stringResource(if (isLike) R.string.unlike else R.string.like))
				}
				FilledTonalButton(
						modifier = Modifier.weight(1f),
						shapes = ButtonDefaults.shapes(),
						colors = ButtonDefaults.filledTonalButtonColors(
								containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
								else MaterialTheme.colorScheme.surfaceContainerHigh,
								contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
								else MaterialTheme.colorScheme.onSurface,
						),
						onClick = onToggleSelect,
				) {
					Text(stringResource(if (isSelected) R.string.drop_course else R.string.select_course))
				}
				FilledTonalButton(
						modifier = Modifier.weight(1f),
						shapes = ButtonDefaults.shapes(),
						onClick = onOpen,
				) {
					Text(stringResource(R.string.open))
				}
			}
		}
	}
}

/** 课程信息小标签：点击弹出完整内容，长按复制 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun InfoChip(
	text: String,
	onClick: (String) -> Unit,
	onLongClick: (String) -> Unit,
) {
	Surface(
			shape = RoundedCornerShape(8.dp),
			color = MaterialTheme.colorScheme.surfaceContainerHighest,
			modifier = Modifier.combinedClickable(
					onClick = { onClick(text) },
					onLongClick = { onLongClick(text) },
			),
	) {
		Text(
				text,
				style = MaterialTheme.typography.labelMedium,
				maxLines = 1,
				overflow = TextOverflow.Ellipsis,
				modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
		)
	}
}
