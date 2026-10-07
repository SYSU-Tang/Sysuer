package com.miyuyan.sysuer.academic

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedFilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
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
 * 已选课程页：类别/状态筛选 chips + 瀑布流课程卡片，
 * 支持选课/退课（确认 Snackbar）、二级专业（PNP）切换与课程详情跳转
 */
@Composable
fun CourseSelectionSelectedScreen(
	viewModel: CourseSelectionViewModel = viewModel(),
	onNavigateToDetail: (id: String, code: String, className: String) -> Unit = { _, _, _ -> },
) {
	val snackbarHostState = remember { SnackbarHostState() }
//	val clipboard = LocalClipboard.current
	val scope = rememberCoroutineScope()
	val courses = viewModel.selectedCourses
	val isLoading by viewModel.selectedIsLoading.collectAsStateWithLifecycle()
	val gridState = rememberLazyStaggeredGridState()

//	fun showSnackbar(text: String) = scope.launch { snackbarHostState.showSnackbar(text) }
//	val copySuccess = stringResource(R.string.copy_successfully)
//	fun copyText(text: String) = scope.launch {
//		clipboard.setClipEntry(
//				ClipData.newPlainText("text", text).toClipEntry()
//		)
//		snackbarHostState.showSnackbar(copySuccess)
//	}

	val confirm = stringResource(R.string.confirm)
	fun withConfirm(message: String, action: () -> Unit) = scope.launch {
		if (snackbarHostState.showSnackbar(
					message,
					actionLabel = confirm,
					duration = SnackbarDuration.Long,
			) == SnackbarResult.ActionPerformed
		) action()
	}

	LaunchedEffect(Unit) {
		viewModel.loadMoreSelected()
	}
	LaunchedEffect(gridState) {
		snapshotFlow { gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
			.distinctUntilChanged().filterNotNull().collect { lastVisibleIndex ->
				if (lastVisibleIndex >= courses.size - 3 && !isLoading) {
					viewModel.loadMoreSelected()
				}
			}
	}

//	Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
		Column(
				modifier = Modifier.fillMaxSize()
//					.padding(
//							horizontal = dimensionResource(R.dimen.horizontal_margin),
//							vertical = dimensionResource(R.dimen.vertical_margin)
//					)
		) {
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
					val status = item.getInteger("status") ?: 0
					val isSelected = status == 3 || status == 4
					val message = stringResource(
							if (isSelected) R.string.drop_course else R.string.select_course
					)
					CourseSelectedCard(
							item = item,
							onOpen = {
								println(item)                                /*{"cultureCategoryCode":"01","pubCourseTypeCode":"TY","volunteerNum":"3","crossMajor":"0","selectCount":"30","courseEnglishName":"Physical education","status":"2","teachingClassId":"2072223770943778817","teachingClassNum":"202611769","teachingClassName":"游泳+侯博文","courseNum":"PE305","courseName":"体育","courseNameEng":"Physical education","courseCateCode":"10","courseCategoryName":"公必","credit":0.5,"examFormName":"考查","courseUnitName":"体育部","baseReceiveNum":30,"addReceiveNum":0,"teachingTimePlace":"侯博文;1-8每周星期二第3节-第4节;东校园 ","sportVolunteer":"3","sumCourse":11,"sumCredit":24.5,"gbCount":4,"gxCount":1,"zbCount":4,"zxCount":2,"rykcCount":0,"kzyCount":0,"gbCredit":5.5,"gxCredit":2.0,"zbCredit":12.0,"zxCredit":5.0,"kzyCredit":0.0,"rykcCredit":0.0,"isPGCourse":"0","isThrough":"0","isInTwoTierSet":"1","courseCateList":"21,30,31","scoreRecordWayCode":"01"}*/
								onNavigateToDetail(
										item.getString("teachingClassId"),
										item.getString("courseNum"),
										item.getString("teachingClassNum"),
								)
							},
							onToggleSelect = {
								withConfirm(message) {
									if (isSelected) {
										viewModel.unselectSelected(
												item.getString("courseId"),
												item.getString("teachingClassId"),
												item.getString("selectedType"),
										)
									} else {
										viewModel.selectSelected(
												item.getString("teachingClassId"),
												item.getString("selectedType"),
												item.getString("courseCateCode"),
										)
									}
								}
							},
							onTogglePNP = {
								val isPNP = item.getString("isTwoTier")
									.isNullOrEmpty() || item.getString("isTwoTier") == "0"
								viewModel.setPNP(
										if (isPNP) "1" else "0", item.getString("teachingClassId")
								)
							},
//							onChipClick = { showSnackbar(it) },
					)
				}
				if (isLoading) {
					item(span = StaggeredGridItemSpan.FullLine) {
						Box(
								modifier = Modifier
									.fillMaxWidth()
									.padding(16.dp),
								contentAlignment = androidx.compose.ui.Alignment.Center,
						) {
							LinearWavyProgressIndicator()
						}
					}
				}
			}
		}
	}
//}

/** 一条已选课程卡片 */
@Composable
private fun CourseSelectedCard(
	item: JSONObject,
	onOpen: () -> Unit,
	onToggleSelect: () -> Unit,
	onTogglePNP: () -> Unit,
) {
	val status = item.getInteger("status") ?: 0
	val isSelected = status == 3 || status == 4
	val canPNP =
		status == 4 && item.getString("isInTwoTierSet") == "1" && item.getString("courseCateList")
			.split(",").contains(item.getString("courseCateCode"))
	val isPNP = item.getString("isTwoTier").isNullOrEmpty() || item.getString("isTwoTier") == "0"
	val infoLabels = stringArrayResource(R.array.course_info_labels)
	val seatLabels = stringArrayResource(R.array.seat_info_labels).drop(1)
	val statusText = stringResource(
			when (status) {
				4 -> R.string.status_selected
				3 -> R.string.filtering
				1 -> R.string.retired
				else -> R.string.unselected
			}
	)

	fun conv(key: String): String = trim(item.getString(key)).replace("\n\n", "\n")
	SelectionContainer {
		Card(
				onClick = onOpen,
				colors = CardDefaults.cardColors(
						containerColor = MaterialTheme.colorScheme.surfaceContainer,
				),
		) {
			Column(
					modifier = Modifier.padding(dimensionResource(R.dimen.content_padding)),
					verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.vertical_gap))
			) {
				Text(
						"${conv("courseNum")}-${conv("courseName")}",
						style = MaterialTheme.typography.titleLarge,
						color = MaterialTheme.colorScheme.primary,
				)
				HorizontalDivider()
				Text(
						conv("teachingTimePlace").replace(";", " | ").replace(",", "\n"),
						style = MaterialTheme.typography.bodyMedium,
				)
				HorizontalDivider()
				FlowRow(
						horizontalArrangement = Arrangement.spacedBy(
								dimensionResource(R.dimen.horizontal_gap)
						), verticalArrangement = Arrangement.spacedBy(
						dimensionResource(R.dimen.vertical_gap)
				)
				) {
					arrayOf(
							"credit", "teachingClassNum", "scheduleExamTime", "examFormName"
					).forEachIndexed { index, key ->
						InfoChip(
								text = "${infoLabels[index]}：${conv(key)}",
						)
					}
				}
				HorizontalDivider()
				Row(
						modifier = Modifier.fillMaxWidth(),
						horizontalArrangement = Arrangement.SpaceEvenly
				) {
					Text(
							"${seatLabels[0]}\n${conv("baseReceiveNum")}",
							style = MaterialTheme.typography.bodyMedium,
							textAlign = TextAlign.Center,
							modifier = Modifier.weight(1f),
					)
					Text(
							"${stringResource(R.string.status)}\n$statusText",
							style = MaterialTheme.typography.bodyMedium,
							textAlign = TextAlign.Center,
							modifier = Modifier.weight(1f),
					)
					Text(
							"${seatLabels[1]}\n${conv("selectCount")}",
							style = MaterialTheme.typography.bodyMedium,
							textAlign = TextAlign.Center,
							modifier = Modifier.weight(1f),
					)
				}
				HorizontalDivider()
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
									containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
									else MaterialTheme.colorScheme.surfaceContainerHigh,
									contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
									else MaterialTheme.colorScheme.onSurface,
							),
							onClick = onToggleSelect,
					) {
						Text(stringResource(if (isSelected) R.string.drop_course else R.string.select_course))
					}
					if (canPNP) {
						FilledTonalButton(
								modifier = Modifier.weight(1f),
								shapes = ButtonDefaults.shapes(),
								colors = ButtonDefaults.filledTonalButtonColors(
										containerColor = if (isPNP) MaterialTheme.colorScheme.surfaceContainerHigh
										else MaterialTheme.colorScheme.primaryContainer,
										contentColor = if (isPNP) MaterialTheme.colorScheme.onSurface
										else MaterialTheme.colorScheme.onPrimaryContainer,
								),
								onClick = onTogglePNP,
						) {
							Text(stringResource(if (isPNP) R.string.set_pnp else R.string.cancel_pnp))
						}
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
}

/** 课程信息小标签：点击弹出完整内容，长按复制 */
@Composable
private fun InfoChip(
	text: String
) {
	val interactionSource = remember { MutableInteractionSource() }
	val isPressed by interactionSource.collectIsPressedAsState()
	val elevation by animateDpAsState(
			targetValue = if (isPressed) 8.dp else 0.dp, animationSpec = spring(
			dampingRatio = Spring.DampingRatioMediumBouncy,
			stiffness = Spring.StiffnessLow,
	), label = "elevation"
	)
	Surface(
			shape = CircleShape,
			shadowElevation = elevation,
			color = MaterialTheme.colorScheme.surfaceContainerHighest,
			modifier = Modifier
				.clip(CircleShape)
				.combinedClickable(
						indication = ripple(),
						interactionSource = interactionSource,
						onClick = {},
				),
	) {
		Text(
				text,
				style = MaterialTheme.typography.labelMedium,
				maxLines = 1,
				overflow = TextOverflow.Ellipsis,
				modifier = Modifier.padding(
						horizontal = dimensionResource(R.dimen.horizontal_padding),
						vertical = dimensionResource(R.dimen.vertical_padding)
				),
		)
	}
}

/** 已选课程筛选面板：类别/状态 chips（置于 topBarContent） */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
internal fun CourseSelectionSelectedFilterPanel(viewModel: CourseSelectionViewModel) {
	val success by viewModel.selectedSuccess.collectAsStateWithLifecycle()
	val failure by viewModel.selectedFailure.collectAsStateWithLifecycle()
	val retired by viewModel.selectedRetired.collectAsStateWithLifecycle()
	val waiting by viewModel.selectedWaiting.collectAsStateWithLifecycle()
	val category by viewModel.selectedCategory.collectAsStateWithLifecycle()
	Column {
			// 课程类别单选筛选
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
						R.string.all to "",
						R.string.public_compulsory to "10",
						R.string.public_selective to "30",
						R.string.major_compulsory to "11",
						R.string.major_selective to "21",
						R.string.honor to "31",
						R.string.cross_major to "kzy",
				).forEach { (label, code) ->
					ElevatedFilterChip(
							selected = category == code,
							onClick = { viewModel.setSelectedCategory(code) },
							label = { Text(stringResource(label)) },
					)
				}
			}
			// 选课状态多选筛选
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
						R.string.success to success,
						R.string.failure to failure,
						R.string.to_filter to waiting,
						R.string.retired to retired,
				).forEachIndexed { index, (label, checked) ->
					ElevatedFilterChip(
							selected = checked == 1,
							onClick = {
								val flags = listOf(success, failure, waiting, retired)
								val new = flags.toMutableList().also { it[index] = 1 - it[index] }
								viewModel.setSelectedStatusFilter(new[0], new[1], new[3], new[2])
							},
							label = { Text(stringResource(label)) },
					)
				}
			}
	}
}
