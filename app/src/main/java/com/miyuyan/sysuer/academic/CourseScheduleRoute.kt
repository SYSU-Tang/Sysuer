package com.miyuyan.sysuer.academic

import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import com.miyuyan.preference.EditPreference
import com.miyuyan.preference.ItemPreference
import com.miyuyan.preference.MenuPreference
import com.miyuyan.preference.PreferenceCategory
import com.miyuyan.preference.PreferenceScreen
import com.miyuyan.preference.RangeSliderPreference
import com.miyuyan.preference.simplemenu.SimpleMenuPopup
import com.miyuyan.sysuer.R
import com.miyuyan.sysuer.nav.CourseDetail
import com.miyuyan.sysuer.nav.navigateBack
import com.miyuyan.sysuer.view.ActivityPager
import com.miyuyan.sysuer.view.MenuItem
import com.miyuyan.sysuer.view.StatePage
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.TextStyle
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * 课程表：按学期与周次展示课表网格，支持周次切换、今日定位、课程详情、
 * 导出 PDF 与自定义课程添加
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseScheduleRoute(
	backStack: MutableList<NavKey>,
	sharedTransitionScope: SharedTransitionScope? = null,
	animatedVisibilityScope: AnimatedVisibilityScope? = null,
) {
	val viewModel: CourseScheduleViewModel = viewModel()
	val activity = LocalActivity.current
	val snackbarHostState = remember { SnackbarHostState() }
	val currentTerm by viewModel.currentTerm.collectAsStateWithLifecycle()
	val terms by viewModel.terms.collectAsStateWithLifecycle()
	val weeks by viewModel.weeks.collectAsStateWithLifecycle()
	val weekIndex by viewModel.weekIndex.collectAsStateWithLifecycle()
	val weekStartDate by viewModel.weekStartDate.collectAsStateWithLifecycle()
	val courses by viewModel.courses.collectAsStateWithLifecycle()
	val uiState by viewModel.uiState.collectAsStateWithLifecycle()
	val realTimeTerm by viewModel.realTimeTerm.collectAsStateWithLifecycle()
	val realTimeWeekIndex by viewModel.realTimeWeekIndex.collectAsStateWithLifecycle()
	val isToday = currentTerm == realTimeTerm && weekIndex == realTimeWeekIndex

	var showAddDialog by remember { mutableStateOf(false) }
	var addDay by remember { mutableIntStateOf(0) }
	var addSection by remember { mutableIntStateOf(1) }
	var selectedCourse by remember { mutableStateOf<CourseCard?>(null) }

	LaunchedEffect(Unit) {
		viewModel.snackbarMessage.collect { snackbarHostState.showSnackbar(it) }
	}
	LaunchedEffect(Unit) {
		viewModel.courseDetailNav.collect { nav ->
			backStack.add(CourseDetail(courseId = nav.courseId, courseNum = nav.courseNum))
		}
	}

	ActivityPager(
			title = stringResource(R.string.course_schedule),
			snackbar = snackbarHostState,
			isNestedScrollEnabled = false,
			onNavigationClick = { backStack.navigateBack(activity) },
			sharedTransitionScope = sharedTransitionScope,
			animatedVisibilityScope = animatedVisibilityScope,
			sharedKey = "CourseSchedule",
			topBarMenus = {
				listOf(
						MenuItem(
								title = stringResource(R.string.add),
								iconResource = R.drawable.add,
								onClick = {
									addDay = 0
									addSection = 1
									showAddDialog = true
								},
						),
						MenuItem(
								title = stringResource(R.string.export),
								iconResource = R.drawable.export,
								onClick = { viewModel.printTable() },
						),
				)
			},
			topBarContent = {
				WeekBar(
						term = currentTerm,
						terms = terms,
						weekIndex = weekIndex,
						weeks = weeks,
						isToday = isToday,
						onTermChange = { viewModel.changeTerm(it) },
						onWeekIndexChange = { viewModel.changeWeek(it) },
						onToday = {
							viewModel.changeTermWeek(
									viewModel.realTimeTerm.value, viewModel.realTimeWeekIndex.value
							)
						},
				)
			},
	) {
		StatePage(state = uiState, onRetry = { viewModel.retry() }, overlay = true) {
			ScheduleBoard(
					courses = courses,
					weekStartDate = weekStartDate,
					onCourseClick = { selectedCourse = it },
					onWeekSwipe = { viewModel.changeWeek(weekIndex + it) },
					onEmptyCellDoubleTap = { day, section ->
						addDay = day
						addSection = section + 1
						showAddDialog = true
					},
			)
		}
	}

	selectedCourse?.let { course ->
		ModalBottomSheet(onDismissRequest = { selectedCourse = null }) {
			CourseDetailContent(
					course = course,
					onOpen = {
						selectedCourse = null
						viewModel.openCourseDetail(course.name)
					},
			)
		}
	}

	if (showAddDialog) {
		ModalBottomSheet(onDismissRequest = { showAddDialog = false }) {
			AddCourseContent(
					terms = terms,
					currentTerm = currentTerm,
					maxWeek = weeks.lastOrNull() ?: 17,
					initialDay = addDay,
					initialSection = addSection,
					initialWeek = weeks.getOrNull(weekIndex) ?: 1,
					onDismiss = { showAddDialog = false },
					onSave = {
						if (viewModel.saveCourse(it)) showAddDialog = false
					},
			)
		}
	}
}

/** 顶栏周次切换栏：上一周 / 学期 / 今日 / 周次 / 下一周 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WeekBar(
	term: String,
	terms: List<String>,
	weekIndex: Int,
	weeks: List<Int>,
	isToday: Boolean,
	onTermChange: (String) -> Unit,
	onWeekIndexChange: (Int) -> Unit,
	onToday: () -> Unit,
) {
	var termPopupVisible by remember { mutableStateOf(false) }
	var weekPopupVisible by remember { mutableStateOf(false) }
	var termAnchor by remember { mutableStateOf(IntRect.Zero) }
	var weekAnchor by remember { mutableStateOf(IntRect.Zero) }
	Row(
			modifier = Modifier.fillMaxWidth(),
			horizontalArrangement = Arrangement.SpaceEvenly,
			verticalAlignment = Alignment.CenterVertically,
	) {
		TooltipBox(
				positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
						TooltipAnchorPosition.Below
				),
				tooltip = {
					PlainTooltip {
						Text(stringResource(R.string.last_week))
					}
				},
				state = rememberTooltipState(),
		) {
			OutlinedIconButton(
					onClick = { onWeekIndexChange(weekIndex - 1) },
					shapes = IconButtonDefaults.shapes(),
					border = BorderStroke(
							1.dp, MaterialTheme.colorScheme.outlineVariant,
					)
			) {
				Icon(
						imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
						tint = MaterialTheme.colorScheme.onSurfaceVariant,
						contentDescription = stringResource(R.string.last_week),
				)
			}
		}
		Box(
				modifier = Modifier.onGloballyPositioned { coordinates ->
					val bounds = coordinates.boundsInWindow()
					termAnchor = IntRect(
							bounds.left.roundToInt(),
							bounds.top.roundToInt(),
							bounds.right.roundToInt(),
							bounds.bottom.roundToInt()
					)
				}) {
			OutlinedButton(
					onClick = { termPopupVisible = true }, shapes = ButtonDefaults.shapes()
			) {
				Text(
						term.ifEmpty { stringResource(R.string.term) },
						maxLines = 1,
						overflow = TextOverflow.Ellipsis,
				)
			}
			if (termPopupVisible) {
				SimpleMenuPopup(
						entries = terms.map { stringResource(R.string.term_x, it) },
						selectedIndex = terms.indexOf(term).takeIf { it >= 0 },
						anchorBounds = termAnchor,
						onDismiss = { termPopupVisible = false },
				) { index -> onTermChange(terms[index]) }
			}
		}
		// 当前正处于今天的学期与周次时用带背景色的按钮标记，否则用描边按钮
		if (isToday) {
			FilledTonalButton(
					onClick = onToday,
					shapes = ButtonDefaults.shapes(),
					contentPadding = PaddingValues(0.dp)
			) {
				Text(stringResource(R.string.today))
			}
		} else {
			OutlinedButton(
					onClick = onToday,
					shapes = ButtonDefaults.shapes(),
					contentPadding = PaddingValues(0.dp)
			) {
				Text(stringResource(R.string.today))
			}
		}
		Box(
				modifier = Modifier.onGloballyPositioned { coordinates ->
					val bounds = coordinates.boundsInWindow()
					weekAnchor = IntRect(
							bounds.left.roundToInt(),
							bounds.top.roundToInt(),
							bounds.right.roundToInt(),
							bounds.bottom.roundToInt()
					)
				}) {
			OutlinedButton(onClick = { weekPopupVisible = true }) {
				Text(
						weeks.getOrNull(weekIndex)?.let { stringResource(R.string.week_d, it) }
							?: stringResource(R.string.week_range),
						maxLines = 1,
						overflow = TextOverflow.Ellipsis,
				)
			}
			if (weekPopupVisible) {
				SimpleMenuPopup(
						entries = weeks.map { stringResource(R.string.week_d, it) },
						selectedIndex = weekIndex.takeIf { it >= 0 },
						anchorBounds = weekAnchor,
						onDismiss = { weekPopupVisible = false },
				) { index -> onWeekIndexChange(index) }
			}
		}
		TooltipBox(
				positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
						TooltipAnchorPosition.Below
				),
				tooltip = {
					PlainTooltip {
						Text(stringResource(R.string.next_week))
					}
				},
				state = rememberTooltipState(),
		) {
			OutlinedIconButton(
					onClick = { onWeekIndexChange(weekIndex + 1) },
					shapes = IconButtonDefaults.shapes(),
					border = BorderStroke(
							1.dp, MaterialTheme.colorScheme.outlineVariant,
					)
			) {
				Icon(
						imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
						tint = MaterialTheme.colorScheme.onSurfaceVariant,
						contentDescription = stringResource(R.string.next_week),
						modifier = Modifier.rotate(180f),
				)
			}
		}
	}
}

/** 节次起始纵坐标（第 4、8 节后有大课间空隙） */
private fun sectionOffset(index: Int, cellHeight: Dp, gap: Dp): Dp {
	var offset = cellHeight * index
	if (index >= 4) offset += gap
	if (index >= 8) offset += gap
	return offset
}

/** 周历 + 课程网格主体 */
@Composable
private fun ScheduleBoard(
	courses: List<CourseCard>,
	weekStartDate: LocalDate?,
	onCourseClick: (CourseCard) -> Unit,
	onWeekSwipe: (Int) -> Unit,
	onEmptyCellDoubleTap: (day: Int, section: Int) -> Unit,
) {
	val dayNames = stringArrayResource(R.array.weeks_simple)
	val durations = stringArrayResource(R.array.duration)
	val weekday = LocalDate.now().dayOfWeek.value - 1
	val gap = 8.dp
	val leftWidth = 48.dp

	// 同名课程分配同一种配色，优先使用未被占用的调色板槽位
	val palettes = listOf(
			MaterialTheme.colorScheme.primary to MaterialTheme.colorScheme.onPrimary,
			MaterialTheme.colorScheme.secondary to MaterialTheme.colorScheme.onSecondary,
			MaterialTheme.colorScheme.tertiary to MaterialTheme.colorScheme.onTertiary,
			MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer,
			MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer,
			MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer,
			MaterialTheme.colorScheme.primaryFixed to MaterialTheme.colorScheme.onPrimaryFixed,
			MaterialTheme.colorScheme.secondaryFixed to MaterialTheme.colorScheme.onSecondaryFixed,
			MaterialTheme.colorScheme.tertiaryFixed to MaterialTheme.colorScheme.onTertiaryFixed,
			MaterialTheme.colorScheme.primaryFixedDim to MaterialTheme.colorScheme.onPrimaryFixed,
			MaterialTheme.colorScheme.secondaryFixedDim to MaterialTheme.colorScheme.onSecondaryFixed,
			MaterialTheme.colorScheme.tertiaryFixedDim to MaterialTheme.colorScheme.onTertiaryFixed,
			MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer,
//			MaterialTheme.colorScheme.inversePrimary to MaterialTheme.colorScheme.onSurface,
			MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant,
//			MaterialTheme.colorScheme.surfaceContainerLow to MaterialTheme.colorScheme.onSurface,
//			MaterialTheme.colorScheme.surfaceContainer to MaterialTheme.colorScheme.onSurface,
//			MaterialTheme.colorScheme.surfaceContainerHigh to MaterialTheme.colorScheme.onSurface,
//			MaterialTheme.colorScheme.surfaceContainerHighest to MaterialTheme.colorScheme.onSurface,
//			MaterialTheme.colorScheme.surfaceDim to MaterialTheme.colorScheme.onSurface,
	)
	val assignedColors = remember(courses) {
		val used = mutableSetOf<Int>()
		courses.associate { course ->
			var idx = abs(course.name.hashCode()) % palettes.size
			while (idx in used && used.size < palettes.size) idx = (idx + 1) % palettes.size
			used.add(idx)
			course.name to idx
		}
	}

	Column(
			modifier = Modifier
				.fillMaxSize()
				.background(MaterialTheme.colorScheme.surfaceContainer),
	) {
		val monthText = (weekStartDate ?: LocalDate.now()).month.getDisplayName(
				TextStyle.SHORT, LocalLocale.current.platformLocale
		)
		Row(
				modifier = Modifier
					.fillMaxWidth()
					.height(48.dp),
				verticalAlignment = Alignment.CenterVertically,
		) {
			Box(
					modifier = Modifier
						.width(leftWidth)
						.fillMaxHeight()
						.background(MaterialTheme.colorScheme.surfaceDim),
					contentAlignment = Alignment.Center,
			) {
				Text(monthText, style = MaterialTheme.typography.bodyMedium)
			}
			(0..6).forEach { i ->
				// startTime 为周日：周一列 = start + 1 … 周日列 = start + 7
				val date = weekStartDate?.plusDays(i.toLong()) ?: LocalDate.now()
					.plusDays((i - weekday).toLong())
				Column(
						modifier = Modifier
							.weight(1f)
							.fillMaxHeight()
							.background(
									color = if (i == weekday) MaterialTheme.colorScheme.surfaceDim
									else Color.Transparent,
									shape = CircleShape,
							),
						horizontalAlignment = Alignment.CenterHorizontally,
						verticalArrangement = Arrangement.Center,
				) {
					Text(
							dayNames[i],
							style = MaterialTheme.typography.bodySmall,
							color = if (i == weekday) MaterialTheme.colorScheme.primary
							else MaterialTheme.colorScheme.onSurfaceVariant,
					)
					Text(
							stringResource(R.string.day_d, date.dayOfMonth),
							style = MaterialTheme.typography.bodySmall,
							color = if (i == weekday) MaterialTheme.colorScheme.primary
							else MaterialTheme.colorScheme.onSurfaceVariant,
					)
				}
			}
		}
		BoxWithConstraints(
				modifier = Modifier
					.fillMaxWidth()
					.weight(1f),
		) {
			// 格子高度固定：整表始终竖向滑动，不随视口压缩行高
			val cellHeight = 56.dp
			val cellCornerRadius = 8.dp
			val colWidth = (maxWidth - leftWidth) / 7
			val gridHeight = cellHeight * 11 + gap * 2
			val gridLineColor = MaterialTheme.colorScheme.outlineVariant
			Box(
					modifier = Modifier
						.fillMaxSize()
						.verticalScroll(rememberScrollState())
						.pointerInput(Unit) {
							var totalX = 0f
							detectHorizontalDragGestures(
									onDragStart = { totalX = 0f },
									onDragEnd = {
										if (abs(totalX) > 120.dp.toPx()) onWeekSwipe(if (totalX > 0) -1 else 1)
									},
							) { change, _ ->
								totalX += change.position.x - change.previousPosition.x
							}
						},
			) {
				val nowTime = LocalTime.now()
				val currentSection = durations.indexOfFirst { period ->
					val (startStr, endStr) = period.split("~")
					nowTime.isAfter(LocalTime.parse(startStr)) && nowTime.isBefore(
							LocalTime.parse(
									endStr
							)
					)
				}

				Box(
						modifier = Modifier
							.fillMaxWidth()
							.height(gridHeight)
							.drawBehind {
								val strokeWidth = 1.dp.toPx()
								val leftPx = leftWidth.toPx()
								val colPx = colWidth.toPx()
								// 三个连续行区块，大课间空隙（第 4~5、8~9 节之间）不在其中
								val blocks = listOf(0..3, 4..7, 8..10)
								// 纵向分隔线只在区块内部画，空隙区域无边框（含节次列右边界）
								blocks.forEach { rows ->
									val top = sectionOffset(rows.first, cellHeight, gap).toPx()
									val bottom = sectionOffset(
											rows.last, cellHeight, gap
									).toPx() + cellHeight.toPx()
									(0..7).forEach { i ->
										val x = leftPx + colPx * i
										drawLine(
												gridLineColor,
												Offset(x, top),
												Offset(x, bottom),
												strokeWidth
										)
									}
								}
								// 横向节次分隔线
								(0..10).forEach { i ->
									val y = sectionOffset(i, cellHeight, gap).toPx()
									drawLine(
											gridLineColor,
											Offset(0f, y),
											Offset(size.width, y),
											strokeWidth
									)
								}
								// 大课间空隙上缘（第 4、8 行的下边框）
								listOf(3, 7).forEach { i ->
									val y =
										sectionOffset(i, cellHeight, gap).toPx() + cellHeight.toPx()
									drawLine(
											gridLineColor,
											Offset(0f, y),
											Offset(size.width, y),
											strokeWidth
									)
								}
								drawLine(
										gridLineColor,
										Offset(0f, size.height - strokeWidth / 2),
										Offset(size.width, size.height - strokeWidth / 2),
										strokeWidth,
								)
							}
							.pointerInput(cellHeight, colWidth) {
								detectTapGestures(onDoubleTap = { pos ->
									val day = ((pos.x - leftWidth.toPx()) / colWidth.toPx()).toInt()
										.coerceIn(0, 6)
									val section = (0..10).firstOrNull { i ->
										val top = sectionOffset(i, cellHeight, gap).toPx()
										pos.y >= top && pos.y < top + cellHeight.toPx()
									} ?: -1
									if (section >= 0) onEmptyCellDoubleTap(day, section)
								})
							},
				) {
					if (weekday in 0..6) {
						// 今日列高亮按行区块分段绘制，空隙区域不填充，圆角矩形收边
						listOf(0..3, 4..7, 8..10).forEach { rows ->
							Box(
									modifier = Modifier
										.offset(
												x = leftWidth + colWidth * weekday,
												y = sectionOffset(rows.first, cellHeight, gap),
										)
										.width(colWidth)
										.height(
												sectionOffset(
														rows.last, cellHeight, gap
												) + cellHeight - sectionOffset(
														rows.first, cellHeight, gap
												)
										)
										.background(
												MaterialTheme.colorScheme.surfaceDim,
												RoundedCornerShape(cellCornerRadius),
										),
							)
						}
					}
					if (currentSection >= 0) {
						Box(
								modifier = Modifier
									.padding(
											start = leftWidth,
											top = sectionOffset(currentSection, cellHeight, gap)
									)
									.fillMaxWidth()
									.height(cellHeight)
									.background(
											MaterialTheme.colorScheme.surfaceDim,
											RoundedCornerShape(cellCornerRadius),
									),
						)
					}
					durations.forEachIndexed { i, period ->
						val isCurrent = i == currentSection
						Column(
								modifier = Modifier
									.offset(y = sectionOffset(i, cellHeight, gap))
									.width(leftWidth)
									.heightIn(min = cellHeight)
//									.padding(horizontal = 2.dp, vertical = 4.dp)
									.background(
											color = if (isCurrent) MaterialTheme.colorScheme.surfaceContainerHighest
											else Color.Transparent,
											shape = CircleShape,
									),
								horizontalAlignment = Alignment.CenterHorizontally,
								verticalArrangement = Arrangement.Center,
						) {
							Text(
									"${i + 1}",
									style = MaterialTheme.typography.labelMedium,
									color = if (isCurrent) MaterialTheme.colorScheme.primary
									else MaterialTheme.colorScheme.onSurfaceVariant,
							)
							Text(
									period.replace("~", "\n"),
									style = MaterialTheme.typography.labelSmall.copy(
											fontSize = 9.sp,
											lineHeight = 11.sp,
									),
									textAlign = TextAlign.Center,
									color = if (isCurrent) MaterialTheme.colorScheme.primary
									else MaterialTheme.colorScheme.onSurfaceVariant,
							)
						}
					}
					courses.forEach { card ->
						val (bg, fg) = palettes[assignedColors[card.name] ?: 0]
						Box(
								modifier = Modifier
									.offset(
											// 接口的 week 为 1 基（1=周一），网格天列为 0 基
											x = leftWidth + colWidth * (card.day - 1),
											y = sectionOffset(
													card.startSection - 1, cellHeight, gap
											),
									)
									.width(colWidth)
									.height(
											cellHeight * (card.endSection - card.startSection + 1)
													// 仅当课程横跨大课间空隙时才补空隙高度
													+ gap * (if (card.startSection <= 4 && card.endSection >= 5) 1 else 0) + gap * (if (card.startSection <= 8 && card.endSection >= 9) 1 else 0)
									)
//									.padding(1.dp),
						) {
							Card(
									onClick = { onCourseClick(card) },
									shape = RoundedCornerShape(cellCornerRadius),
									colors = CardDefaults.cardColors(
											containerColor = bg,
											disabledContainerColor = bg.copy(alpha = 0.5f),
									),
									modifier = Modifier
										.fillMaxSize()
										.padding(0.64.dp)
										.alpha(if (card.isStop) 0.5f else 1f),
							) {
								Box(
										modifier = Modifier
											.fillMaxSize()
											.padding(4.dp),
										contentAlignment = Alignment.Center,
								) {
									Text(
											"${card.name}/${card.building}-${card.classroom}",
											color = fg,
											style = MaterialTheme.typography.labelSmall.copy(
													fontSize = 9.sp,
													lineHeight = 11.sp,
											),
									)
								}
							}
						}
					}
				}
			}
		}
	}
}

/** 课程详情弹窗内容：compose-preference 信息行 + 打开课程详情入口 */
@Composable
private fun CourseDetailContent(course: CourseCard, onOpen: () -> Unit) {
	val none = stringResource(R.string.none)
	val rows = listOf(
			DetailRowData(R.drawable.course, stringResource(R.string.name), course.name),
			DetailRowData(R.drawable.location, stringResource(R.string.location), course.location),
			DetailRowData(R.drawable.account, stringResource(R.string.teacher), course.teacher),
			DetailRowData(
					R.drawable.account,
					stringResource(R.string.assistant),
					course.assistant ?: none,
			),
			DetailRowData(
					R.drawable.calendar,
					stringResource(R.string.classTime),
					stringResource(
							R.string.from_to_section, course.startSection, course.endSection
					),
			),
	)
	PreferenceScreen {
		PreferenceCategory {
			items(rows) { row ->
				ItemPreference(title = row.label, icon = row.icon, summary = row.value)
			}
		}
		FilledTonalButton(
				onClick = onOpen, modifier = Modifier.fillMaxWidth()
//					.padding(bottom = dimensionResource(R.dimen.vertical_padding)),
		) {
			Text(stringResource(R.string.open))
		}
	}
}

/** 课程详情弹窗的一行数据 */
private data class DetailRowData(val icon: Int, val label: String, val value: String)

/** 一个时间段草稿：学期、星期、周次范围与节次范围 */
private data class SegmentDraft(
	val term: String,
	val dayIndex: Int = 0,
	val weekStart: Float = 1f,
	val weekEnd: Float = 17f,
	val sectionStart: Float = 1f,
	val sectionEnd: Float = 11f,
)

/** 添加自定义课程弹窗内容：compose-preference 输入行 + 时间段分组 */
@Composable
private fun AddCourseContent(
	terms: List<String>,
	currentTerm: String,
	maxWeek: Int,
	initialDay: Int,
	initialSection: Int,
	initialWeek: Int,
	onDismiss: () -> Unit,
	onSave: (CourseData) -> Unit,
) {
	val dayNames = stringArrayResource(R.array.weeks_simple)
	var courseName by remember { mutableStateOf("") }
	var location by remember { mutableStateOf("") }
	// 初始时间段沿用双击格子所在的时间点：当前学期、点击列、当前周、点击节次
	val segments = remember {
		mutableStateListOf(
				SegmentDraft(
						term = currentTerm,
						dayIndex = initialDay,
						weekStart = initialWeek.toFloat(),
						weekEnd = initialWeek.toFloat(),
						sectionStart = initialSection.toFloat(),
						sectionEnd = initialSection.toFloat(),
				)
		)
	}

	PreferenceScreen {
		PreferenceCategory {
			item {
				EditPreference(
						title = stringResource(R.string.course_name_label),
						icon = {
							Icon(
									painterResource(R.drawable.course), contentDescription = null
							)
						},
						placeholder = stringResource(R.string.course_name_label),
						initialValue = courseName,
						onChange = { _, _, value -> courseName = value.orEmpty() },
				)
			}
			item {
				EditPreference(
						title = stringResource(R.string.class_location),
						icon = {
							Icon(
									painterResource(R.drawable.location), contentDescription = null
							)
						},
						placeholder = stringResource(R.string.class_location),
						initialValue = location,
						onChange = { _, _, value -> location = value.orEmpty() },
				)
			}
		}
		segments.forEachIndexed { index, segment ->
			PreferenceCategory(title = stringResource(R.string.time_segment_x, index + 1)) {
				item {
					MenuPreference(
							title = stringResource(R.string.term),
							icon = {
								Icon(
										painterResource(R.drawable.calendar),
										contentDescription = null
								)
							},
							summary = terms.indexOf(segment.term).takeIf { it >= 0 }?.let { idx ->
								stringResource(R.string.term_x, terms[idx])
							},
							entries = terms.map { stringResource(R.string.term_x, it) },
							entryValues = terms,
							selectedIndex = terms.indexOf(segment.term).takeIf { it >= 0 },
							onChange = { _, _, value ->
								segments[index] = segment.copy(term = value ?: segment.term)
							},
					)
				}
				item {
					MenuPreference(
							title = stringResource(R.string.week),
							icon = {
								Icon(
										painterResource(R.drawable.course),
										contentDescription = null
								)
							},
							summary = stringResource(
									R.string.week_x, dayNames.getOrElse(segment.dayIndex) { "" }),
							entries = dayNames.map { stringResource(R.string.week_x, it) },
							entryValues = dayNames.toList(),
							selectedIndex = segment.dayIndex,
							onChange = { _, _, value ->
								segments[index] =
									segment.copy(dayIndex = value?.let { dayNames.indexOf(it) }
										?: segment.dayIndex)
							},
					)
				}
				item {
					RangeSliderPreference(
							title = stringResource(R.string.week_range),
							icon = {
								Icon(
										painterResource(R.drawable.priority),
										contentDescription = null
								)
							},
							valueRange = 1f..maxWeek.toFloat(),
							steps = (maxWeek - 2).coerceAtLeast(0),
							initialValues = segment.weekStart..segment.weekEnd,
							valuesText = { range ->
								if (range.start.toInt() == range.endInclusive.toInt()) {
									stringResource(R.string.week_d, range.start.toInt())
								} else {
									stringResource(
											R.string.from_to_week,
											range.start.toInt(),
											range.endInclusive.toInt(),
									)
								}
							},
							onValueChange = { range ->
								segments[index] = segment.copy(
										weekStart = range.start, weekEnd = range.endInclusive
								)
							},
					)
				}
				item {
					RangeSliderPreference(
							title = stringResource(R.string.section),
							icon = {
								Icon(
										painterResource(R.drawable.time), contentDescription = null
								)
							},
							valueRange = 1f..11f,
							steps = 9,
							initialValues = segment.sectionStart..segment.sectionEnd,
							valuesText = { range ->
								stringResource(
										R.string.from_to_section,
										range.start.toInt(),
										range.endInclusive.toInt(),
								)
							},
							onValueChange = { range ->
								segments[index] = segment.copy(
										sectionStart = range.start, sectionEnd = range.endInclusive
								)
							},
					)
				}
				if (segments.size > 1) {
					item {
						ItemPreference(
								title = stringResource(R.string.delete),
								icon = R.drawable.close,
								onClick = { segments.removeAt(index) },
						)
					}
				}
			}
		}
		PreferenceCategory {
			item {
				OutlinedButton(
						onClick = { segments.add(SegmentDraft(term = currentTerm)) },
						modifier = Modifier.fillMaxWidth(),
				) {
					Text(stringResource(R.string.add_time_segment))
				}
			}
			item {
				Row(
						modifier = Modifier
							.fillMaxWidth()
							.padding(bottom = dimensionResource(R.dimen.vertical_padding)),
						horizontalArrangement = Arrangement.End,
				) {
					TextButton(onClick = onDismiss) {
						Text(stringResource(R.string.cancel))
					}
					Spacer(modifier = Modifier.width(8.dp))
					FilledTonalButton(onClick = {
						onSave(
								CourseData(
										name = courseName.trim(),
										location = location.trim(),
										segments = segments.map {
											CourseTimeSegmentData(
													term = it.term,
													dayIndex = it.dayIndex,
													weekStart = it.weekStart.toInt(),
													weekEnd = it.weekEnd.toInt(),
													sectionStart = it.sectionStart.toInt(),
													sectionEnd = it.sectionEnd.toInt(),
											)
										},
								)
						)
					}) {
						Text(stringResource(R.string.add))
					}
				}
			}
		}
	}
}
