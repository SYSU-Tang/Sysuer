package com.miyuyan.sysuer.academic

import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import com.alibaba.fastjson2.JSONObject
import com.haibin.calendarview.compose.CalendarColors
import com.haibin.calendarview.compose.CalendarDay
import com.haibin.calendarview.compose.CalendarMonthShowMode
import com.haibin.calendarview.compose.CalendarSelectMode
import com.haibin.calendarview.compose.CalendarWeekStart
import com.haibin.calendarview.compose.ComposeCalendar
import com.haibin.calendarview.compose.ComposeCalendarConfig
import com.haibin.calendarview.compose.rememberComposeCalendarState
import com.miyuyan.preference.ItemPreference
import com.miyuyan.preference.PreferenceCategory
import com.miyuyan.preference.PreferenceScreen
import com.miyuyan.sysuer.R
import com.miyuyan.sysuer.nav.navigateBack
import com.miyuyan.sysuer.view.ActivityPager
import com.miyuyan.sysuer.view.StatePage
import com.miyuyan.sysuer.view.UiState
import java.time.LocalDate

/**
 * 日程：单个 ActivityPager 承载 ComposeCalendar 与当日日程列表。
 *
 * 顶部为月视图 / 周视图日历，向上拖动列表收起为周视图，向下拖动展开为月视图；
 * 左右滑动日历可切换月份（月视图）或周（周视图）。日历下方按时间段分组展示日程，
 * 每项沿用偏好设置样式的分段列表卡片。
 */
@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun AgendaRoute(
	backStack: MutableList<NavKey>,
	sharedTransitionScope: SharedTransitionScope? = null,
	animatedVisibilityScope: AnimatedVisibilityScope? = null,
) {
	val viewModel: AgendaViewModel = viewModel()
	val activity = LocalActivity.current
	val uiState by viewModel.uiState.collectAsStateWithLifecycle()
	var selectedEntry by remember { mutableStateOf<JSONObject?>(null) }

	val config = rememberAgendaCalendarConfig()
	val state = rememberComposeCalendarState(
			config = config,
			initialDate = CalendarDay.today(),
	)

	// 顶栏副标题跟随当前可见月份
	var visibleYear by remember { mutableIntStateOf(state.anchorDay.year) }
	var visibleMonth by remember { mutableIntStateOf(state.anchorDay.month) }
	SideEffect {
		state.onMonthChange = { year, month ->
			visibleYear = year
			visibleMonth = month
		}
		state.onYearChange = { visibleYear = it }
	}

	// 选中日期变化（点击日期、周翻页、月翻页、回到今天）后加载当天日程；
	// selectedDays 由 Compose 状态驱动，结构相等时不会重复请求。
	val selectedDays = state.selectedDays
	LaunchedEffect(selectedDays) {
		selectedDays.firstOrNull()?.let { day ->
			viewModel.loadSchedule(LocalDate.of(day.year, day.month, day.day))
		}
	}

	ActivityPager(
			title = stringResource(R.string.agenda),
			subtitle = stringResource(R.string.year_month, visibleYear, visibleMonth),
			isNestedScrollEnabled = false,
			onNavigationClick = { backStack.navigateBack(activity) },
			sharedTransitionScope = sharedTransitionScope,
			animatedVisibilityScope = animatedVisibilityScope,
			sharedKey = "Agenda",
	) {
		ComposeCalendar(
				state = state,
				config = config,
				modifier = Modifier.fillMaxSize(),
		) {
			Surface(
					modifier = Modifier.fillMaxSize(),
					color = MaterialTheme.colorScheme.surface,
					shadowElevation = 2.dp,
			) {
				val entries = (uiState as? AgendaUiState.Content)?.entries
				StatePage(
						// 列表与状态合并为一个 sealed 状态，这里只做视图层映射
						state = when (uiState) {
							AgendaUiState.Loading -> UiState.Loading
							AgendaUiState.Empty -> UiState.Empty
							AgendaUiState.Error -> UiState.Error
							is AgendaUiState.Content -> UiState.Content
						},
						onRetry = { viewModel.retry() },
				) {
					AgendaList(entries.orEmpty()) { selectedEntry = it }
				}
			}
		}
	}

	selectedEntry?.let { entry ->
		ModalBottomSheet(onDismissRequest = { selectedEntry = null }) {
			AgendaDetailContent(entry)
		}
	}
}

/**
 * 日程列表：每个元素自带时间段标题，因此各自成为一个分段分组。
 *
 * @param onClick 点击日程条目时回调，用于打开详情弹窗。
 */
@Composable
private fun AgendaList(entries: List<JSONObject>, onClick: (JSONObject) -> Unit) {
	LazyColumn(
			modifier = Modifier.fillMaxSize(),
			contentPadding = PaddingValues(
					horizontal = dimensionResource(R.dimen.horizontal_margin),
					vertical = dimensionResource(R.dimen.vertical_margin),
			),
			verticalArrangement = Arrangement.spacedBy(
					dimensionResource(R.dimen.vertical_margin)
			),
	) {
		items(entries) { entry -> AgendaGroup(entry, onClick) }
	}
}

/**
 * 单个时间段分组：标题为 timeZone，内容为该时段的日程条目。
 *
 * @param onClick 点击条目时回调，用于打开详情弹窗。
 */
@Composable
private fun AgendaGroup(entry: JSONObject, onClick: (JSONObject) -> Unit) {
	val place = entry.getString("place")
	PreferenceCategory(title = entry.getString("timeZone")) {
		item {
			ItemPreference(
					title = entry.getString("title").orEmpty(),
					icon = R.drawable.text,
					summary = place?.takeIf { it.isNotEmpty() },
					onClick = { onClick(entry) },
			)
		}
	}
}

/**
 * 日程详情弹窗：标题、时间段与位置三行只读信息。
 */
@Composable
private fun AgendaDetailContent(entry: JSONObject) {
	val none = stringResource(R.string.none)
	val rows = listOf(
			DetailRow(
					R.drawable.text, stringResource(R.string.title),
					entry.getString("title")?.takeIf { it.isNotEmpty() } ?: none,
			),
			DetailRow(
					R.drawable.time, stringResource(R.string.time_segment),
					entry.getString("timeZone")?.takeIf { it.isNotEmpty() } ?: none,
			),
			DetailRow(
					R.drawable.location, stringResource(R.string.location),
					entry.getString("place")?.takeIf { it.isNotEmpty() } ?: none,
			),
	)
	PreferenceScreen {
		PreferenceCategory {
			items(rows) { row ->
				ItemPreference(title = row.label, icon = row.icon, summary = row.value)
			}
		}
	}
}

/** 日程详情弹窗的一行数据 */
private data class DetailRow(val icon: Int, val label: String, val value: String)

/**
 * 日程日历配置：与旧版 activity_agenda.xml 中 CalendarView 的属性保持一致。
 */
@Composable
private fun rememberAgendaCalendarConfig(): ComposeCalendarConfig {
	val colorScheme = MaterialTheme.colorScheme
	return remember(colorScheme) {
		ComposeCalendarConfig(
				weekStart = CalendarWeekStart.Monday,
				monthShowMode = CalendarMonthShowMode.OnlyCurrentMonth,
				selectMode = CalendarSelectMode.Single,
				minYear = 2008,
				maxYear = 2050,
				itemHeight = 56.dp,
				weekBarHeight = 46.dp,
				calendarPaddingLeft = 4.dp,
				calendarPaddingRight = 4.dp,
				dayTextSize = 16.sp,
				lunarTextSize = 10.sp,
				weekBarTextSize = 12.sp,
				colors = agendaCalendarColors(colorScheme),
		)
	}
}

/**
 * 由 Material3 配色派生日历配色，对齐旧版 CustomMonthView / CustomWeekView 的取色。
 */
private fun agendaCalendarColors(colorScheme: ColorScheme): CalendarColors =
	CalendarColors.default().copy(
			background = colorScheme.surfaceBright,
			weekBarBackground = colorScheme.surface,
			weekBarTextColor = colorScheme.onSurface,
			dayTextColor = colorScheme.onSurface,
			otherMonthDayTextColor = colorScheme.onSurface,
			lunarTextColor = colorScheme.onSurfaceVariant,
			otherMonthLunarTextColor = colorScheme.outline,
			currentDayTextColor = colorScheme.error,
			currentDayLunarTextColor = colorScheme.error,
			selectedBackgroundColor = colorScheme.primary,
			selectedTextColor = colorScheme.onPrimary,
			selectedLunarTextColor = colorScheme.onPrimary,
			schemeTextColor = colorScheme.onSurface,
			schemeLunarTextColor = colorScheme.onSurfaceVariant,
			schemeDefaultColor = Color(0xFF128C4B),
			rangeBackgroundColor = colorScheme.primaryContainer,
			rangeEdgeColor = colorScheme.primary,
			disabledTextColor = colorScheme.outlineVariant,
			outOfRangeTextColor = colorScheme.outlineVariant,
			// 旧版 CustomMonthView / CustomWeekView 中周末与节气使用同一种棕色
			weekendTextColor = Color(0xFFB76201),
			weekendLunarTextColor = Color(0xFFB76201),
	)
