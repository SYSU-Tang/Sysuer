package com.miyuyan.sysuer.todo

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ElevatedAssistChip
import androidx.compose.material3.ElevatedFilterChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ToggleButtonShapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import com.haibin.calendarview.compose.CalendarColors
import com.haibin.calendarview.compose.CalendarDay
import com.haibin.calendarview.compose.CalendarDayScheme
import com.haibin.calendarview.compose.CalendarMonthShowMode
import com.haibin.calendarview.compose.CalendarSelectMode
import com.haibin.calendarview.compose.CalendarWeekStart
import com.haibin.calendarview.compose.ComposeCalendar
import com.haibin.calendarview.compose.ComposeCalendarConfig
import com.haibin.calendarview.compose.ComposeCalendarState
import com.haibin.calendarview.compose.rememberComposeCalendarState
import com.miyuyan.preference.FilterPreference
import com.miyuyan.preference.PreferenceCategory
import com.miyuyan.preference.PreferenceScreen
import com.miyuyan.sysuer.R
import com.miyuyan.sysuer.api.DateTimeManager
import com.miyuyan.sysuer.api.TodoManager
import com.miyuyan.sysuer.nav.navigateBack
import com.miyuyan.sysuer.view.ActivityPager
import com.miyuyan.sysuer.view.InputDialogChip
import com.miyuyan.sysuer.view.MultiSelectChipDropdown
import java.time.LocalDate

/**
 * 待办页：Compose 版 [ComposeCalendar] + 可组合多维筛选的待办列表。
 *
 * 日历只是「指定日期」范围的辅助选择器：点日历会把范围切到指定日期，选其它时间范围
 * 则取消日历高亮，列表不再被锁死在单天上。筛选条件由 [TodoFilter] 承载，经
 * [buildTodoWhere] 下推成一条 SQL。
 *
 * UI 分两层：**基本筛选**（时间范围 / 日期字段 / 状态 / 关键词）直接铺在页面上，
 * 日期字段、状态、关键词复用 [MultiSelectChipDropdown] / [InputDialogChip]；
 * **高级筛选**（科目 / 类型 / 标签 / 优先级）收在底部弹层，用 compose-preference 的
 * [PreferenceScreen] + [FilterPreference] 呈现。
 */
@Composable
fun TodoRoute(
	backStack: MutableList<NavKey>,
) {
	val todoModel: TodoModel = viewModel()
	val activity = LocalActivity.current ?: return
	// TodoManager 需要生命周期协程域来调度提醒等异步操作
	val lifecycleScope: LifecycleCoroutineScope = LocalLifecycleOwner.current.lifecycleScope
	val todoManager = remember(todoModel) {
		TodoManager(activity, lifecycleScope, todoModel)
	}

	// 完整筛选条件，默认「全部」
	var filter by remember { mutableStateOf(TodoFilter()) }
	// 增删改后自增，驱动下面的查询重跑
	var reloadTick by remember { mutableIntStateOf(0) }
	var showAdvancedSheet by remember { mutableStateOf(false) }
	var isAddRequested by remember { mutableStateOf(false) }

	// 高级筛选的候选项来自这三张字典表
	LaunchedEffect(Unit) { todoManager.init() }

	// 筛选条件变化 -> 重新查询列表
	LaunchedEffect(filter, reloadTick) {
		val (where, args) = buildTodoWhere(filter, DateTimeManager.today)
		todoModel.loadTodos(where, args)
	}

	// 日历圆点走全量查询，只在数据变更后刷新，与筛选无关
	LaunchedEffect(reloadTick) { todoModel.loadCalendarMarks() }

	// 待办增删改后要同时刷新列表与日历标记
	LaunchedEffect(Unit) { todoManager.refreshListener = { reloadTick++ } }

	val todoList by todoModel.todoList.observeAsState(emptyList())
	val calendarMarks by todoModel.calendarMarks.observeAsState(emptySet())
	val subjects by todoModel.subjects.observeAsState(emptyList())
	val types by todoModel.types.observeAsState(emptyList())
	val tags by todoModel.tags.observeAsState(emptyList())

	// 日历标记：全量待办的 dueDate / ddl 汇总为圆点日期集合
	val markedDays = remember(calendarMarks) {
		calendarMarks.mapNotNull { parseCalendarDay(it) }.toSet()
	}

	ActivityPager(
			title = stringResource(R.string.todo),
			isNestedScrollEnabled = false,
			onNavigationClick = { backStack.navigateBack(activity) },
			floatingActionButton = { _ ->
				ExtendedFloatingActionButton(
						onClick = { isAddRequested = true },
						text = { Text(stringResource(R.string.add_todo)) },
						icon = {
							Icon(
									painterResource(R.drawable.add),
									contentDescription = stringResource(R.string.add_todo)
							)
						},
				)
			},
	) { _ ->
		TodoContent(
				todoManager = todoManager,
				todoList = todoList,
				markedDays = markedDays,
				filter = filter,
				onFilterChange = { filter = it },
				onAdvancedClick = { showAdvancedSheet = true },
				isAddRequested = isAddRequested,
				onAddHandled = { isAddRequested = false },
		)
	}

	if (showAdvancedSheet) {
		TodoAdvancedSheet(
				filter = filter,
				subjects = subjects.mapNotNull { it.name },
				types = types.mapNotNull { it.name },
				tags = tags.mapNotNull { it.name },
				onFilterChange = { filter = it },
				onDismiss = { showAdvancedSheet = false },
		)
	}
}

/** 待办页主体：日历 + 时间范围 + 基本筛选 + 高级筛选生效项 + 列表 */
@Composable
private fun TodoContent(
	todoManager: TodoManager,
	todoList: List<TodoEntity>,
	markedDays: Set<CalendarDay>,
	filter: TodoFilter,
	onFilterChange: (TodoFilter) -> Unit,
	onAdvancedClick: () -> Unit,
	isAddRequested: Boolean,
	onAddHandled: () -> Unit,
) {
	val colorScheme = MaterialTheme.colorScheme
	val config = remember(colorScheme) {
		// 属性对齐旧版 fragment_todo.xml 的 CalendarView
		ComposeCalendarConfig(
				weekStart = CalendarWeekStart.Monday,
				monthShowMode = CalendarMonthShowMode.OnlyCurrentMonth,
				// Range 模式：单日是它的退化形式（只点一下），连点两天即框选区间。
				// 选它而非 Single 还有个关键原因：onMonthPageChanged / scrollTo 只在
				// Default / Single 下会把 selectedDay 兜底成本月 1 号或落地日，
				// 那就是「初始全部状态却选中今天」的病根；Range 模式下高亮只来自
				// rangeStart / rangeEnd，没选就是干净的空状态。
				selectMode = CalendarSelectMode.Range,
				minYear = 2008,
				maxYear = 2050,
				itemHeight = 56.dp,
				weekBarHeight = 46.dp,
				calendarPaddingLeft = 4.dp,
				calendarPaddingRight = 4.dp,
				dayTextSize = 16.sp,
				lunarTextSize = 10.sp,
				weekBarTextSize = 12.sp,
				colors = todoCalendarColors(colorScheme),
		)
	}
	val state = rememberComposeCalendarState(
			config = config,
			initialDate = CalendarDay.today(),
	)

	// 有待办的日期画圆点标记
	val schemeDates = remember(markedDays, colorScheme) {
		markedDays.associateWith { CalendarDayScheme(colorScheme.primary) }
	}
	// 点日历即切到「指定区间」。Range 模式下 selectedDays 只含已定的端点：
	// 第一个端点 selectedEndDate 置空表示「区间待定」，此时查询按单日过滤，
	// 但日历仍保留待定的起点，再点一天就自动补全成区间。
	val currentOnDaySelected by rememberUpdatedState { _: CalendarDay ->
		val days = state.selectedDays
		if (days.isNotEmpty()) {
			onFilterChange(
						filter.copy(
								range = TodoDateRange.SelectedRange,
								selectedDate = days.first().toIsoString(),
								selectedEndDate =
										days.lastOrNull()
												?.takeIf { it != days.first() }
												?.toIsoString().orEmpty()
						)
				)
		}
	}
	SideEffect {
		state.schemeDates = schemeDates
		state.onDaySelected = { day, _ -> currentOnDaySelected(day) }
	}

	// 日历高亮是筛选条件的投影：由 resolveDateBounds 统一算出区间，与 SQL 谓词同源，
	// 两者不可能不一致。「全部」清空高亮，「今天 / 本周 / 本月」亮出对应的日或时间段。
	//
	// SelectedRange 跳过：那时日历是端点的权威持有者（含「点了起点、终点待定」这个
	// 中间态），写回 selectRange 会把待定区间提前闭合，第二次点击就变成开新段了。
	LaunchedEffect(filter.range, filter.selectedDate, filter.selectedEndDate) {
		if (filter.range == TodoDateRange.SelectedRange) return@LaunchedEffect
		val bounds = resolveDateBounds(filter, DateTimeManager.today)
		if (bounds == null) state.clearSelect()
		else state.selectRange(bounds.first.toCalendarDay(), bounds.second.toCalendarDay())
	}

	ComposeCalendar(
			state = state,
			config = config,
	) {
		Column(
				modifier = Modifier
					.fillMaxSize()
					.verticalScroll(rememberScrollState()),
		) {
			TodoRangeRow(
					range = filter.range,
					onRangeChange = { range -> applyRange(range, state, filter, onFilterChange) },
			)
			TodoBasicFilterRow(
					filter = filter,
					onFilterChange = onFilterChange,
					onAdvancedClick = onAdvancedClick,
			)
			TodoActiveFilters(
					filter = filter,
					onFilterChange = onFilterChange,
			)
			todoManager.TodoListScreen(
					todoList = todoList,
					isAddRequested = isAddRequested,
					onAddHandled = onAddHandled,
			) {
				Text(
						text = stringResource(
								if (filter.isDefault) R.string.no_todo
								else R.string.no_matching_todo
						),
						style = MaterialTheme.typography.bodyLarge,
						color = MaterialTheme.colorScheme.onSurfaceVariant,
						modifier = Modifier.padding(
								dimensionResource(R.dimen.horizontal_margin),
								dimensionResource(R.dimen.vertical_margin),
						),
				)
			}
		}
	}
}

/**
 * 切换时间范围时同步日历的选中态。
 *
 * 只有「指定日期」保留高亮；其余范围取消高亮，避免日历显示一个并不生效的选中日。
 */
private fun applyRange(
	range: TodoDateRange,
	state: ComposeCalendarState,
	filter: TodoFilter,
	onFilterChange: (TodoFilter) -> Unit,
) {
	if (range == TodoDateRange.SelectedRange) {
		// 沿用日历上已有的选择（可能上次框过一段），没选过则以今天兜底并滚过去
		val days = state.selectedDays
		val start = days.firstOrNull() ?: CalendarDay.today()
		val end = days.lastOrNull() ?: start
		state.scrollTo(start)
		state.selectRange(start, end)
		onFilterChange(
				filter.copy(
						range = range,
						selectedDate = start.toIsoString(),
						selectedEndDate = end.takeIf { it != start }?.toIsoString().orEmpty()
				)
		)
	} else {
		// 清掉日期字段，避免下次回到「指定区间」时残留旧端点；
		// 日历高亮由上面的 LaunchedEffect 按新 range 同步，不在此处动手
		onFilterChange(filter.copy(range = range, selectedDate = "", selectedEndDate = ""))
	}
}

/** 时间范围：全部 / 今天 / 本周 / 本月 / 指定日期 */
@Composable
private fun TodoRangeRow(
	range: TodoDateRange,
	onRangeChange: (TodoDateRange) -> Unit,
) {
	val ranges = listOf(
			TodoDateRange.All to stringResource(R.string.all),
			TodoDateRange.Today to stringResource(R.string.today),
			TodoDateRange.ThisWeek to stringResource(R.string.this_week),
			TodoDateRange.ThisMonth to stringResource(R.string.this_month),
			TodoDateRange.SelectedRange to stringResource(R.string.selected_range),
	)
//	Row(
//			modifier = Modifier
//				.fillMaxWidth()
//				.horizontalScroll(rememberScrollState())
//				.padding(
//						horizontal = dimensionResource(R.dimen.horizontal_margin),
//						vertical = dimensionResource(R.dimen.vertical_margin),
//				),
//	) {
		ButtonGroup(
				modifier = Modifier
			.fillMaxWidth()
			.horizontalScroll(rememberScrollState())
			.padding(
					horizontal = dimensionResource(R.dimen.horizontal_margin),
					vertical = dimensionResource(R.dimen.vertical_margin),
			),
				overflowIndicator = { ButtonGroupDefaults.OverflowIndicator(it) },
				horizontalArrangement = Arrangement.spacedBy(
						dimensionResource(R.dimen.horizontal_gap)
				),
		) {
			ranges.forEach { (value, label) ->
				toggleableItem(
						checked = range == value,
						onCheckedChange = { onRangeChange(value) },
						label = label,
				)
//			customItem(
//					buttonGroupContent = {
//						// interactionSource 必须同时给 animateWidth 和 ToggleButton，
//						// ButtonGroup 才能观察按压并展开该项、压缩相邻项
//						val interactionSource = remember(value) { MutableInteractionSource() }
//						ToggleButton(
//								checked = checked,
//								// 单选语义：忽略回调里的 Boolean，点哪一项就选哪一项
//								onCheckedChange = { onRangeChange(value) },
//								modifier = Modifier.animateWidth(interactionSource),
//								interactionSource = interactionSource,
//								shapes = connectedShapesFor(index, ranges.size),
//						) { Text(stringResource(label), maxLines = 1) }
//					},
//					menuContent = { state ->
//						DropdownMenuItem(
//								text = { Text(stringResource(label)) },
//								leadingIcon = if (checked) {
//									{ Icon(Icons.Filled.Check, contentDescription = null) }
//								} else {
//									null
//								},
//								onClick = {
//									onRangeChange(value)
//									state.dismiss()
//								},
//						)
//					},
//			)
//			}
		}
	}
}

/** connected 按钮组的不对称圆角：首项左侧全圆、尾项右侧全圆、中间小圆角 */
@Composable
private fun connectedShapesFor(index: Int, count: Int): ToggleButtonShapes = when (index) {
	0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
	count - 1 -> ButtonGroupDefaults.connectedTrailingButtonShapes()
	else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
}

/**
 * 基本筛选行，全部复用 [InputDialogChip] 系列组件。
 *
 * - **日期字段**：[MultiSelectChipDropdown]（预定 / 截止可同时选中，与旧版两个独立开关等价）
 * - **状态**：[MultiSelectChipDropdown]（未完成 / 已完成多选）
 * - **关键词**：[InputDialogChip]，点击弹输入框，自带清除按钮
 *
 * 日期字段仅在时间范围不是「全部」时出现 —— 范围为「全部」时查询不产生任何日期谓词，
 * 此时展示日期字段只会让人以为它生效。
 */
@Composable
private fun TodoBasicFilterRow(
	filter: TodoFilter,
	onFilterChange: (TodoFilter) -> Unit,
	onAdvancedClick: () -> Unit,
) {
	val dueLabel = stringResource(R.string.due)
	val ddlLabel = stringResource(R.string.ddl)
	val unfinishedLabel = stringResource(R.string.unfinished)
	val doneLabel = stringResource(R.string.done)
	// 四个芯片放进一个 FlowRow 自然回流，而不是写死两行：
	// 「日期字段」在范围为「全部」时不渲染，固定两行的话第二行就只剩一个芯片；
	// 回流则会自动补位，窄屏 / 大字号下也不会挤爆。
	FlowRow(
			modifier = Modifier
				.fillMaxWidth()
				.padding(horizontal = dimensionResource(R.dimen.horizontal_margin)),
			horizontalArrangement = Arrangement.spacedBy(
					dimensionResource(R.dimen.horizontal_gap)
			),
			verticalArrangement = Arrangement.spacedBy(
					dimensionResource(R.dimen.vertical_gap)
			),
	) {
			MultiSelectChipDropdown(
				category = stringResource(R.string.status),
				options = listOf(unfinishedLabel, doneLabel),
				selectedValues = buildSet {
					if (filter.showTodo) add(unfinishedLabel)
					if (filter.showDone) add(doneLabel)
				},
				onValueChange = { values ->
					onFilterChange(
							filter.copy(
									showTodo = unfinishedLabel in values,
									showDone = doneLabel in values
							)
					)
				},
		)
		InputDialogChip(
				category = stringResource(R.string.keyword),
				value = filter.keyword,
				onValueChange = { text -> onFilterChange(filter.copy(keyword = text)) },
		)
		if (filter.range != TodoDateRange.All) {
				MultiSelectChipDropdown(
					category = stringResource(R.string.date_field),
					options = listOf(dueLabel, ddlLabel),
					selectedValues = buildSet {
						if (filter.useDue) add(dueLabel)
						if (filter.useDdl) add(ddlLabel)
					},
					onValueChange = { values ->
						// 不允许两个都取消，否则日期条件整体失效、语义混乱
						if (values.isEmpty()) return@MultiSelectChipDropdown
						onFilterChange(
								filter.copy(
										useDue = dueLabel in values, useDdl = ddlLabel in values
								)
						)
					},
			)
		}
		ElevatedAssistChip(
				onClick = onAdvancedClick,
				leadingIcon = {
					Icon(
							painterResource(R.drawable.filter),
							contentDescription = null,
							modifier = Modifier.size(FilterChipDefaults.IconSize),
					)
				},
				label = { Text(stringResource(R.string.add_filter)) },
		)
	}
}

/** 生效中的筛选条件：点 × 移除单项，末尾提供一键清除 */
@Composable
private fun TodoActiveFilters(
	filter: TodoFilter,
	onFilterChange: (TodoFilter) -> Unit,
) {
	// 关键词与日期字段的状态由各自的 Chip 自己显示，这里只列「无处可点」的维度：
	// 时间范围的具体日期，以及高级筛选里的四项。
	val hasChips =
		(filter.range == TodoDateRange.SelectedRange && filter.selectedDate.isNotBlank()) || filter.subjects.isNotEmpty() || filter.types.isNotEmpty() || filter.tags.isNotEmpty() || filter.priorities.isNotEmpty()
	if (!hasChips) return
	val priorityLabels = stringArrayResource(R.array.priority)
	FlowRow(
			modifier = Modifier
				.fillMaxWidth()
				.padding(horizontal = dimensionResource(R.dimen.horizontal_margin)),
			horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.horizontal_gap)),
			verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.vertical_gap)),
	) {
		if (filter.range == TodoDateRange.SelectedRange && filter.selectedDate.isNotBlank()) {
			// 终点待定（只点了一天）时只显示单日，否则显示区间
			val end = filter.selectedEndDate.takeIf { it.isNotBlank() }
			ActiveFilterChip(
					label = "${stringResource(R.string.selected_range)}: " +
							filter.selectedDate + (end?.let { " ~ $it" } ?: ""),
					onRemove = {
							onFilterChange(
									filter.copy(
											range = TodoDateRange.All,
											selectedDate = "",
											selectedEndDate = ""
									)
							)
						},
			)
		}
		filter.subjects.sorted().forEach { subject ->
			ActiveFilterChip(
					label = filterValueLabel(subject),
					onRemove = { onFilterChange(filter.copy(subjects = filter.subjects - subject)) },
			)
		}
		filter.types.sorted().forEach { type ->
			ActiveFilterChip(
					label = filterValueLabel(type),
					onRemove = { onFilterChange(filter.copy(types = filter.types - type)) },
			)
		}
		filter.tags.sorted().forEach { tag ->
			ActiveFilterChip(
					label = filterValueLabel(tag),
					onRemove = { onFilterChange(filter.copy(tags = filter.tags - tag)) },
			)
		}
		filter.priorities.sorted().forEach { priority ->
			ActiveFilterChip(
					label = "${stringResource(R.string.priority)}: " + priorityLabels.getOrElse(
							priority
					) { priority.toString() },
					onRemove = {
						onFilterChange(filter.copy(priorities = filter.priorities - priority))
					},
			)
		}
		ElevatedAssistChip(
				onClick = { onFilterChange(TodoFilter()) },
				label = { Text(stringResource(R.string.clear_all_filter)) },
		)
	}
}

/** 带关闭图标的生效项芯片 */
@Composable
private fun ActiveFilterChip(label: String, onRemove: () -> Unit) {
	ElevatedFilterChip(
			selected = true,
			onClick = onRemove,
			label = { Text(label) },
			trailingIcon = {
				Icon(
						Icons.Rounded.Close,
						contentDescription = stringResource(R.string.clear),
						modifier = Modifier.size(FilterChipDefaults.IconSize)
				)
			},
	)
}

/** 「未分类」哨兵换成文案，其余原样显示 */
@Composable
private fun filterValueLabel(value: String): String =
	if (value == TODO_UNCLASSIFIED) stringResource(R.string.unclassified) else value

/**
 * 高级筛选弹层，用 compose-preference 的 [PreferenceScreen] + [FilterPreference] 呈现。
 *
 * [FilterPreference] 以「索引集合」表示选中态，因此这里把真实值预先算成索引集合回传，
 * 而 `entryValues` 传真实值，让 `onChange` 能直接拿到业务值而不用反查索引。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TodoAdvancedSheet(
	filter: TodoFilter,
	subjects: List<String>,
	types: List<String>,
	tags: List<String>,
	onFilterChange: (TodoFilter) -> Unit,
	onDismiss: () -> Unit,
) {
	val priorityLabels = stringArrayResource(R.array.priority).toList()
	// 「未分类」是虚拟选项，追加到字典表末尾
	val subjectValues = subjects + TODO_UNCLASSIFIED
	val typeValues = types + TODO_UNCLASSIFIED
	val priorityValues = priorityLabels.indices.map { it.toString() }
	val unclassifiedLabel = stringResource(R.string.unclassified)
	// 哨兵值在选项里要显示成「未分类」，但参与筛选的仍是哨兵本身
	val subjectEntries =
		subjectValues.map { if (it == TODO_UNCLASSIFIED) unclassifiedLabel else it }
	val typeEntries = typeValues.map { if (it == TODO_UNCLASSIFIED) unclassifiedLabel else it }
	ModalBottomSheet(onDismissRequest = onDismiss) {
		PreferenceScreen {
			PreferenceCategory(title = stringResource(R.string.add_filter)) {
				item {
					FilterPreference(
							title = stringResource(R.string.subject),
							icon = {
								Icon(
										painterResource(R.drawable.book),
										contentDescription = null
								)
							},
							entries = subjectEntries,
							entryValues = subjectValues,
							selections = subjectValues.indices
								.filter { subjectValues[it] in filter.subjects }.toSet(),
							onChange = { _, _, value ->
								value?.let {
									onFilterChange(
											filter.copy(
													subjects = filter.subjects.toggle(
															it
													)
											)
									)
								}
							},
					)
				}
				item {
					FilterPreference(
							title = stringResource(R.string.type),
							icon = {
								Icon(
										painterResource(R.drawable.todo),
										contentDescription = null
								)
							},
							entries = typeEntries,
							entryValues = typeValues,
							selections = typeValues.indices
								.filter { typeValues[it] in filter.types }.toSet(),
							onChange = { _, _, value ->
								value?.let {
									onFilterChange(
											filter.copy(
													types = filter.types.toggle(
															it
													)
											)
									)
								}
							},
					)
				}
				if (tags.isNotEmpty()) {
					item {
						FilterPreference(
								title = stringResource(R.string.tag),
								icon = {
									Icon(
											painterResource(R.drawable.favorite),
											contentDescription = null
									)
								},
								entries = tags,
								entryValues = tags,
								selections = tags.indices.filter { tags[it] in filter.tags }
									.toSet(),
								onChange = { _, _, value ->
									value?.let {
										onFilterChange(
												filter.copy(
														tags = filter.tags.toggle(
																it
														)
												)
										)
									}
								},
						)
					}
				}
				item {
					FilterPreference(
							title = stringResource(R.string.priority),
							icon = {
								Icon(
										painterResource(R.drawable.priority),
										contentDescription = null
								)
							},
							entries = priorityLabels,
							entryValues = priorityValues,
							selections = priorityValues.indices.filter {
								priorityValues[it].toIntOrNull() in filter.priorities
							}.toSet(),
							onChange = { _, _, value ->
								value?.let {
									val next =
										filter.priorities.map(Int::toString).toSet().toggle(it)
									onFilterChange(
											filter.copy(
													priorities = next
														.mapNotNull(String::toIntOrNull).toSet()
											)
									)
								}
							},
					)
				}
			}
			Row(
					modifier = Modifier.fillMaxWidth(),
					horizontalArrangement = Arrangement.End,
			) {
				TextButton(onClick = { onFilterChange(TodoFilter()) }) {
					Text(stringResource(R.string.reset))
				}
				TextButton(onClick = onDismiss) {
					Text(stringResource(R.string.confirm))
				}
			}
		}
	}
}

/** 集合的对称差：在则移除，不在则加入 */
private fun Set<String>.toggle(value: String): Set<String> =
	if (value in this) this - value else this + value

/** yyyy-MM-dd -> CalendarDay，非法输入返回 null */
private fun parseCalendarDay(date: String): CalendarDay? = runCatching {
	val parts = date.split("-")
	CalendarDay(parts[0].toInt(), parts[1].toInt(), parts[2].toInt())
}.getOrNull()

/** CalendarDay -> yyyy-MM-dd */
private fun CalendarDay.toIsoString(): String = "%04d-%02d-%02d".format(year, month, day)

/** LocalDate -> CalendarDay，用于把筛选区间回写给日历 */
private fun LocalDate.toCalendarDay(): CalendarDay = CalendarDay(year, monthValue, dayOfMonth)

/** 由 Material3 配色派生日历配色，对齐旧版 fragment_todo.xml 的取色 */
private fun todoCalendarColors(colorScheme: ColorScheme): CalendarColors =
	CalendarColors.default().copy(
			background = colorScheme.surface,
			weekBarBackground = colorScheme.surface,
			weekBarTextColor = colorScheme.onSurface,
			dayTextColor = colorScheme.onSurface,
			otherMonthDayTextColor = colorScheme.outline,
			lunarTextColor = colorScheme.onSurfaceVariant,
			otherMonthLunarTextColor = colorScheme.outline,
			currentDayTextColor = Color(0xFFFF0000),
			currentDayLunarTextColor = Color(0xFFFF0000),
			selectedBackgroundColor = colorScheme.primary,
			selectedTextColor = colorScheme.onPrimary,
			selectedLunarTextColor = colorScheme.onPrimary,
			schemeDefaultColor = colorScheme.primary,
	)