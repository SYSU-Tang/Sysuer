package com.miyuyan.sysuer.academic

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.haibin.calendarview.compose.CalendarColors
import com.haibin.calendarview.compose.CalendarDay
import com.haibin.calendarview.compose.CalendarDayScheme
import com.haibin.calendarview.compose.CalendarMonthShowMode
import com.haibin.calendarview.compose.CalendarSelectMode
import com.haibin.calendarview.compose.CalendarViewMode
import com.haibin.calendarview.compose.CalendarWeekStart
import com.haibin.calendarview.compose.ComposeCalendar
import com.haibin.calendarview.compose.ComposeCalendarConfig
import com.haibin.calendarview.compose.rememberComposeCalendarState
import com.miyuyan.sysuer.BaseActivity
import com.miyuyan.sysuer.theme.SysuerTheme
import kotlinx.coroutines.launch

/** Compose 版日历组件演示页 */
class ComposeCalendarDemoActivity : BaseActivity() {
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContent {
			SysuerTheme(settingManager) {
				ComposeCalendarDemoScreen()
			}
		}
	}
}

/** 演示用的单条安排（自造的确定性假数据） */
private data class DemoEvent(val title: String, val time: String, val location: String)

/** 选择模式下拉项：模式 -> 中文标签 */
private val selectModeOptions = listOf(
	CalendarSelectMode.Default to "默认",
	CalendarSelectMode.Single to "单选",
	CalendarSelectMode.Range to "范围",
	CalendarSelectMode.Multi to "多选",
)

/** 依据日期生成确定性假课表，保证同一天每次展示完全一致 */
private fun demoEvents(day: CalendarDay): List<DemoEvent> {
	val seed = day.year * 372 + day.month * 31 + day.day
	return List(6) { index ->
		val building = 'A' + (seed + index) % 4
		val room = "${(seed + index) % 5 + 1}0${(seed + index) % 9 + 1}"
		DemoEvent(
			title = "第 ${index + 1} 节课",
			time = "%02d:00 - %02d:40".format(8 + index, 9 + index),
			location = "教学楼 $building$room",
		)
	}
}

/**
 * 演示页主体：顶部年月 / 视图模式 + 操作按钮，下方为日历与当天安排列表。
 *
 * 安排列表是 [LazyColumn]，因此上下拖动内容会通过嵌套滚动收起 / 展开月视图。
 */
@Composable
fun ComposeCalendarDemoScreen() {
	val colorScheme = MaterialTheme.colorScheme
	// 配色全部由 Material3 主题色派生，演示如何完全接管组件配色
	val calendarColors = remember(colorScheme) { demoCalendarColors(colorScheme) }
	// 标点日期：相对今天生成，保证演示数据自洽
	val schemeDates = remember(colorScheme) {
		val now = System.currentTimeMillis()
		mapOf(
			CalendarDay.of(now - 2 * 86_400_000L) to CalendarDayScheme(colorScheme.tertiary, "休"),
			CalendarDay.of(now + 3 * 86_400_000L) to CalendarDayScheme(colorScheme.error, "假"),
			CalendarDay.of(now + 10 * 86_400_000L) to CalendarDayScheme(colorScheme.primary, "考"),
			CalendarDay.of(now + 20 * 86_400_000L) to CalendarDayScheme(colorScheme.secondary, "游"),
		)
	}

	// 日历配置，演示周起始、显示模式、选中模式与自定义绘制
	val config = remember(calendarColors) {
		ComposeCalendarConfig(
			weekStart = CalendarWeekStart.Monday,
			monthShowMode = CalendarMonthShowMode.OnlyCurrentMonth,
			selectMode = CalendarSelectMode.Single,
			defaultExpanded = true,
			showWeekBar = true,
			showLunar = true,
			showSchemeText = true,
			colors = calendarColors,
			// 有标记的日期画一圈细环，标记来源见 schemeDates
			dayDecoration = { _, dayState ->
				if (dayState.hasScheme) {
					drawCircle(
						color = colorScheme.primary,
						radius = size.minDimension * 0.36f,
						style = Stroke(width = 1.5.dp.toPx()),
					)
				}
			},
		)
	}

	val today = remember { CalendarDay.today() }
	val state = rememberComposeCalendarState(
		config = config,
		initialDate = today,
		initialMode = CalendarViewMode.MONTH,
	)

	var visibleYear by remember { mutableIntStateOf(today.year) }
	var visibleMonth by remember { mutableIntStateOf(today.month) }
	var viewMode by remember { mutableStateOf(CalendarViewMode.MONTH) }
	var selectMode by remember { mutableStateOf(CalendarSelectMode.Single) }
	var selectedDay by remember { mutableStateOf(state.selectedDay) }
	var showSelectMenu by remember { mutableStateOf(false) }
	val snackbarHostState = remember { SnackbarHostState() }
	val scope = rememberCoroutineScope()

	// 回调挂在状态对象上，用 SideEffect 保证每次重组后依然有效
	SideEffect {
		state.schemeDates = schemeDates
		state.onDaySelected = { day, _ -> selectedDay = day }
		state.onMonthChange = { year, month ->
			visibleYear = year
			visibleMonth = month
		}
		state.onViewModeChange = { mode ->
			viewMode = mode
			scope.launch {
				snackbarHostState.showSnackbar(
					if (mode == CalendarViewMode.MONTH) "已展开月视图" else "已收起为周视图"
				)
			}
		}
	}

	Scaffold(
		topBar = {
			TopAppBar(
				title = {
					Column {
						Text(
							text = "%d年%d月".format(visibleYear, visibleMonth),
							style = MaterialTheme.typography.titleLarge,
							fontWeight = FontWeight.SemiBold,
						)
						Text(
							text = if (viewMode == CalendarViewMode.MONTH) "月视图 · 上滑收起" else "周视图",
							style = MaterialTheme.typography.labelSmall,
							color = MaterialTheme.colorScheme.onSurfaceVariant,
						)
					}
				},
				actions = {
					IconButton(onClick = { state.toggle() }) {
						Icon(
							imageVector = if (state.isExpanded) Icons.Rounded.KeyboardArrowUp
							else Icons.Rounded.KeyboardArrowDown,
							contentDescription = "展开或收起",
						)
					}
					IconButton(onClick = { state.scrollToCurrent() }) {
						Icon(Icons.Rounded.DateRange, contentDescription = "回到今天")
					}
					Box {
						IconButton(onClick = { showSelectMenu = true }) {
							Icon(Icons.Rounded.MoreVert, contentDescription = "选择模式")
						}
						DropdownMenu(
							expanded = showSelectMenu,
							onDismissRequest = { showSelectMenu = false },
						) {
							selectModeOptions.forEach { (mode, label) ->
								DropdownMenuItem(
									text = { Text(label) },
									leadingIcon = {
										if (selectMode == mode) {
											Icon(Icons.Rounded.Check, contentDescription = null)
										}
									},
									onClick = {
										selectMode = mode
									// 同时同步状态与配置，避免两者不一致
									state.config = state.config.copy(selectMode = mode)
										state.clearSelect()
										selectedDay = null
										showSelectMenu = false
									},
								)
							}
						}
					}
				},
			)
		},
		snackbarHost = { SnackbarHost(snackbarHostState) },
	) { padding ->
		Column(
			modifier = Modifier
				.fillMaxSize()
				.padding(padding)
		) {
			ComposeCalendar(
				state = state,
				config = state.config,
				modifier = Modifier.weight(1f),
			) {
				DayEventsList(selectedDay)
			}
		}
	}
}

/** 选中日期的安排列表；未选中时展示空状态提示 */
@Composable
private fun DayEventsList(day: CalendarDay?) {
	if (day == null) {
		Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
			Column(horizontalAlignment = Alignment.CenterHorizontally) {
				Icon(
					imageVector = Icons.Rounded.DateRange,
					contentDescription = null,
					tint = MaterialTheme.colorScheme.outline,
					modifier = Modifier.size(48.dp),
				)
				Spacer(Modifier.height(12.dp))
				Text(
					text = "请选择日期查看当天安排",
					color = MaterialTheme.colorScheme.onSurfaceVariant,
				)
			}
		}
		return
	}
	LazyColumn(
		modifier = Modifier.fillMaxSize(),
		contentPadding = PaddingValues(16.dp),
		verticalArrangement = Arrangement.spacedBy(12.dp),
	) {
		item {
			Text(
				text = "%d年%d月%d日".format(day.year, day.month, day.day),
				style = MaterialTheme.typography.titleMedium,
				fontWeight = FontWeight.SemiBold,
			)
		}
		items(demoEvents(day), key = { it.title }) { event ->
			EventCard(event)
		}
	}
}

/** 单条安排卡片 */
@Composable
private fun EventCard(event: DemoEvent) {
	Card(
		modifier = Modifier.fillMaxWidth(),
		shape = RoundedCornerShape(16.dp),
		colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
	) {
		Row(
			modifier = Modifier
				.fillMaxWidth()
				.padding(16.dp),
			verticalAlignment = Alignment.CenterVertically,
		) {
			Column(modifier = Modifier.weight(1f)) {
				Text(text = event.title, style = MaterialTheme.typography.titleSmall)
				Text(
					text = event.time,
					style = MaterialTheme.typography.bodySmall,
					color = MaterialTheme.colorScheme.onSurfaceVariant,
				)
			}
			Spacer(Modifier.width(12.dp))
			Text(
				text = event.location,
				style = MaterialTheme.typography.labelMedium,
				color = MaterialTheme.colorScheme.primary,
			)
		}
	}
}

/** 由 Material3 主题色派生日历配色，演示不依赖默认配色的完整自定义 */
private fun demoCalendarColors(colorScheme: ColorScheme): CalendarColors =
	CalendarColors.default().copy(
		background = colorScheme.surface,
		weekBarBackground = colorScheme.surfaceVariant,
		weekBarTextColor = colorScheme.onSurfaceVariant,
		dayTextColor = colorScheme.onSurface,
		otherMonthDayTextColor = colorScheme.outline,
		lunarTextColor = colorScheme.onSurfaceVariant,
		otherMonthLunarTextColor = colorScheme.outlineVariant,
		currentDayTextColor = colorScheme.primary,
		currentDayLunarTextColor = colorScheme.primary,
		selectedBackgroundColor = colorScheme.primary,
		selectedTextColor = colorScheme.onPrimary,
		selectedLunarTextColor = colorScheme.onPrimary,
		schemeTextColor = colorScheme.onSurface,
		schemeLunarTextColor = colorScheme.onSurfaceVariant,
		schemeDefaultColor = colorScheme.secondary,
		rangeBackgroundColor = colorScheme.primaryContainer.copy(alpha = 0.35f),
		rangeEdgeColor = colorScheme.primary,
		disabledTextColor = colorScheme.outlineVariant,
		outOfRangeTextColor = colorScheme.outlineVariant,
		weekendTextColor = colorScheme.error,
		weekendLunarTextColor = colorScheme.error,
	)

/** 预览：完整演示页（含假数据） */
@Preview(showBackground = true, widthDp = 400, heightDp = 860)
@Composable
private fun ComposeCalendarDemoPreview() {
	SysuerTheme {
		ComposeCalendarDemoScreen()
	}
}
