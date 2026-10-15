/*
 * Copyright (C) 2016 huanghaibin_dev <huanghaibin_dev@163.com>
 * WebSite https://github.com/MiracleTimes-Dev
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.haibin.calendarview.compose

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.haibin.calendarview.CalendarUtil
import com.haibin.calendarview.LunarCalendar
import com.haibin.calendarview.SysuerCalendar

/**
 * 农历与节气资源只需要初始化一次。
 */
private var lunarInitialized = false

internal fun ensureLunarInitialized(context: Context) {
	if (!lunarInitialized) {
		LunarCalendar.init(context)
		lunarInitialized = true
	}
}

/**
 * Compose 日历的状态持有者，等价于旧版的 [com.haibin.calendarview.CalendarView] 与其委托，
 * 负责日期计算、选择状态、滚动目标与事件回调。
 *
 * 通过 [rememberComposeCalendarState] 创建，并交给 [ComposeCalendar] 渲染。
 */
@Stable
class ComposeCalendarState internal constructor(
	config: ComposeCalendarConfig = ComposeCalendarConfig(),
	initialDate: CalendarDay? = null,
	initialMode: CalendarViewMode = CalendarViewMode.MONTH,
	context: Context? = null,
) {
	private val appContext: Context? = context?.applicationContext

	/** 日历配置，修改后会自动刷新网格数据 */
	var config: ComposeCalendarConfig = config
		set(value) {
			field = value
			selectMode = value.selectMode
			refresh()
		}

	/** 当前视图模式：月视图 / 周视图 */
	var viewMode: CalendarViewMode = initialMode
		private set

	/** 选择模式 */
	var selectMode: CalendarSelectMode = config.selectMode

	/** 今天 */
	var today: CalendarDay = CalendarDay.today()
		private set

	private val anchorDayState = mutableStateOf(initialDate ?: CalendarDay.today())

	/**
	 * 当前可见/锚定日期，决定显示哪个月与哪一周。
	 * 用 snapshot 状态承载，选中日期变化时折叠锚点与周分页器才能跟着重组。
	 */
	var anchorDay: CalendarDay
		get() = anchorDayState.value
		private set(value) {
			anchorDayState.value = value
		}

	private val selectedDayState =
		mutableStateOf<CalendarDay?>(initialDate ?: CalendarDay.today())

	/** 单选模式下的选中日期 */
	var selectedDay: CalendarDay?
		get() = selectedDayState.value
		private set(value) {
			selectedDayState.value = value
		}

	/** 多选模式下的选中日期集合 */
	private var multiSelected: List<CalendarDay> = emptyList()

	/** 范围选择的起点 */
	private var rangeStart: CalendarDay? = null

	/** 范围选择的终点 */
	private var rangeEnd: CalendarDay? = null

	/** 是否展开月视图 */
	var isExpanded: Boolean = config.defaultExpanded
		internal set

	/** 最近一次展开/收起动画时长，供渲染层读取 */
	internal var animationDurationMs: Int = 240

	/** 日期标记 */
	var schemeDates: Map<CalendarDay, CalendarDayScheme> = emptyMap()
		set(value) {
			field = value
			refresh()
		}

	/** 需要拦截的日期 */
	var interceptDates: List<CalendarDay> = emptyList()
		set(value) {
			field = value
			refresh()
		}

	/** 是否启用日期拦截 */
	var isInterceptEnabled: Boolean = false

	/** 点击日期回调 */
	var onDayClick: ((CalendarDay) -> Unit)? = null

	/** 长按日期回调 */
	var onDayLongClick: ((CalendarDay) -> Unit)? = null

	/** 日期被选中回调，第二个参数表示是否由点击触发 */
	var onDaySelected: ((CalendarDay, Boolean) -> Unit)? = null

	/** 日期超出范围回调 */
	var onDayOutOfRange: ((CalendarDay) -> Unit)? = null

	/** 月份变化回调 */
	var onMonthChange: ((Int, Int) -> Unit)? = null

	/** 周变化回调 */
	var onWeekChange: ((List<CalendarDay>) -> Unit)? = null

	/** 年份变化回调 */
	var onYearChange: ((Int) -> Unit)? = null

	/** 视图模式变化回调 */
	var onViewModeChange: ((CalendarViewMode) -> Unit)? = null

	/** 最小可选日期 */
	val minDate: CalendarDay
		get() = config.minDate ?: CalendarDay(config.minYear, 1, 1)

	/** 最大可选日期 */
	val maxDate: CalendarDay
		get() = config.maxDate ?: CalendarDay(config.maxYear, 12, 31)

	/** 最小年份 */
	val minYear: Int get() = minDate.year

	/** 最大年份 */
	val maxYear: Int get() = maxDate.year

	/** 月视图总页数 */
	val monthCount: Int
		get() = (12 * (maxYear - minYear) - minDate.month + 1 + maxDate.month).coerceAtLeast(1)

	/** 周视图总页数 */
	val weekCount: Int
		get() = CalendarUtil.getWeekCountBetweenBothCalendar(
			minYear, minDate.month, minDate.day,
			maxYear, maxDate.month, maxDate.day,
			config.weekStartValue,
		).coerceAtLeast(1)

	private val _monthDays = mutableStateOf<List<CalendarDay>>(emptyList())

	/** 当前月份的 42 个日期格 */
	val monthDays: List<CalendarDay> get() = _monthDays.value

	private val _currentWeekDays = mutableStateOf<List<CalendarDay>>(emptyList())

	/** 当前锚定周的 7 个日期 */
	val currentWeekDays: List<CalendarDay> get() = _currentWeekDays.value

	private val _selectedDays = mutableStateOf<List<CalendarDay>>(emptyList())

	/** 全部选中日期，单选模式下最多一个 */
	val selectedDays: List<CalendarDay> get() = _selectedDays.value

	init {
		appContext?.let { ensureLunarInitialized(it) }
		refresh()
	}

	// ---------------------------------------------------------------- 索引换算

	/**
	 * 日期所在的月份页下标
	 */
	fun monthIndexOf(day: CalendarDay): Int =
		(12 * (day.year - minYear) + day.month - minDate.month).coerceIn(0, monthCount - 1)

	/**
	 * 月份页下标对应的日期（该月 1 号）
	 */
	fun monthAt(index: Int): CalendarDay {
		val i = index.coerceIn(0, monthCount - 1)
		val year = (i + minDate.month - 1) / 12 + minYear
		val month = (i + minDate.month - 1) % 12 + 1
		return CalendarDay(year, month, 1)
	}

	/**
	 * 日期所在的周页下标
	 */
	fun weekIndexOf(day: CalendarDay): Int =
		(CalendarUtil.getWeekFromCalendarStartWithMinCalendar(
			day.toSysuerCalendar(), minYear, minDate.month, minDate.day, config.weekStartValue,
		) - 1).coerceIn(0, weekCount - 1)

	/**
	 * 周页下标对应的该周第一天
	 */
	fun weekAt(index: Int): CalendarDay {
		val i = index.coerceIn(0, weekCount - 1)
		val sysuer = CalendarUtil.getFirstCalendarStartWithMinCalendar(
			minYear, minDate.month, minDate.day, i + 1, config.weekStartValue,
		)
		return CalendarDay(sysuer.year, sysuer.month, sysuer.day)
	}

	/**
	 * 日期在月视图中的第几行，1..6
	 */
	fun weekIndexInMonth(day: CalendarDay): Int =
		CalendarUtil.getWeekFromDayInMonth(day.toSysuerCalendar(), config.weekStartValue)
			.coerceIn(1, 6)

	// ---------------------------------------------------------------- 数据刷新

	private fun refresh() {
		_monthDays.value = buildMonthDays(anchorDay.year, anchorDay.month)
		_currentWeekDays.value = buildWeekDays(weekAt(weekIndexOf(anchorDay)))
		_selectedDays.value = when (selectMode) {
			CalendarSelectMode.Multi -> multiSelected
			CalendarSelectMode.Range -> listOfNotNull(rangeStart, rangeEnd)
			else -> listOfNotNull(selectedDay)
		}
	}

	private fun buildMonthDays(year: Int, month: Int): List<CalendarDay> {
		val todaySysuer = today.toSysuerCalendar().apply { isCurrentDay = true }
		return CalendarUtil.initCalendarForMonthView(
			year, month, todaySysuer, config.weekStartValue,
		).map { CalendarDay(it.year, it.month, it.day) }
	}

	private fun buildWeekDays(first: CalendarDay): List<CalendarDay> =
		(0..6).map { first.plusDays(it) }

	/**
	 * 刷新“今天”，跨天时调用。
	 */
	fun updateCurrentDate() {
		today = CalendarDay.today()
		refresh()
	}

	// ---------------------------------------------------------------- 范围判断

	/**
	 * 日期是否在可选范围内
	 */
	fun isInRange(day: CalendarDay): Boolean =
		compare(day, minDate) >= 0 && compare(day, maxDate) <= 0

	/**
	 * 日期是否被拦截
	 */
	fun isIntercepted(day: CalendarDay): Boolean =
		isInterceptEnabled && interceptDates.any { it == day }

	private fun compare(a: CalendarDay, b: CalendarDay): Int {
		if (a.year != b.year) return a.year - b.year
		if (a.month != b.month) return a.month - b.month
		return a.day - b.day
	}

	// ---------------------------------------------------------------- 选择

	/**
	 * 选中一个日期，会按当前选择模式处理范围与多选。
	 */
	fun select(day: CalendarDay, isClick: Boolean = false) {
		if (!isInRange(day)) {
			onDayOutOfRange?.invoke(day)
			return
		}
		if (isIntercepted(day)) return
		when (selectMode) {
			CalendarSelectMode.Default, CalendarSelectMode.Single -> selectedDay = day
			CalendarSelectMode.Range -> selectRange(day)
			CalendarSelectMode.Multi -> selectMulti(day)
		}
		// 点击后把锚定日对齐到选中日，收起为周视图时停在这一周
		anchorDay = day
		refresh()
		onDaySelected?.invoke(day, isClick)
	}

	private fun selectRange(day: CalendarDay) {
		val start = rangeStart
		if (start == null || rangeEnd != null) {
			rangeStart = day
			rangeEnd = null
			selectedDay = day
			return
		}
		var from = if (compare(start, day) <= 0) start else day
		var to = if (compare(start, day) <= 0) day else start
		if (config.minSelectRange > 0 && from.daysUntil(to) + 1 < config.minSelectRange) {
			to = from.plusDays(config.minSelectRange - 1)
		}
		if (config.maxSelectRange > 0 && from.daysUntil(to) + 1 > config.maxSelectRange) {
			to = from.plusDays(config.maxSelectRange - 1)
		}
		if (compare(to, maxDate) > 0) to = maxDate
		if (compare(from, minDate) < 0) from = minDate
		rangeStart = from
		rangeEnd = to
		selectedDay = to
	}

	private fun selectMulti(day: CalendarDay) {
		val list = multiSelected.toMutableList()
		if (list.any { it == day }) list.removeAll { it == day }
		else {
			if (config.maxMultiSelectSize > 0 && list.size >= config.maxMultiSelectSize) return
			list.add(day)
		}
		multiSelected = list.sortedWith { a, b -> compare(a, b) }
		selectedDay = day
	}

	/**
	 * 直接指定一个已选区间，用于由外部状态（如筛选条件）驱动日历高亮。
	 *
	 * 与点击式的 [select] 有两点不同：
	 * 1. 不触发 [onDaySelected]，避免「外部写回 -> 回调 -> 外部再写回」的循环；
	 * 2. 两个端点按先后自动排序，并夹到 [minDate] / [maxDate] 之内。
	 *
	 * 非 [CalendarSelectMode.Range] 模式下退化为选中 [start] 单日。
	 */
	fun selectRange(start: CalendarDay, end: CalendarDay) {
		if (selectMode != CalendarSelectMode.Range) {
			selectedDay = start
			refresh()
			return
		}
		var from = if (compare(start, end) <= 0) start else end
		var to = if (compare(start, end) <= 0) end else start
		if (compare(from, minDate) < 0) from = minDate
		if (compare(to, maxDate) > 0) to = maxDate
		rangeStart = from
		rangeEnd = to
		selectedDay = to
		refresh()
	}

	/**
	 * 清空选择
	 */
	fun clearSelect() {
		selectedDay = null
		multiSelected = emptyList()
		rangeStart = null
		rangeEnd = null
		refresh()
	}

	// ---------------------------------------------------------------- 滚动

	/**
	 * 滚动到指定日期
	 */
	fun scrollTo(day: CalendarDay, smoothScroll: Boolean = true) {
		val clamped = clampToRange(day)
		anchorDay = clamped
		if (selectMode == CalendarSelectMode.Default || selectMode == CalendarSelectMode.Single) {
			selectedDay = clamped
		}
		refresh()
	}

	/**
	 * 滚动到今天
	 */
	fun scrollToCurrent(smoothScroll: Boolean = true) {
		updateCurrentDate()
		scrollTo(today, smoothScroll)
	}

	private fun clampToRange(day: CalendarDay): CalendarDay = when {
		compare(day, minDate) < 0 -> minDate
		compare(day, maxDate) > 0 -> maxDate
		else -> day
	}

	/**
	 * 展开月视图
	 */
	fun expand(durationMs: Int = 240) {
		animationDurationMs = durationMs
		if (!isExpanded) setExpanded(true)
	}

	/**
	 * 收起为周视图
	 */
	fun shrink(durationMs: Int = 240) {
		animationDurationMs = durationMs
		if (isExpanded) setExpanded(false)
	}

	/**
	 * 在展开与收起之间切换
	 */
	fun toggle() {
		if (isExpanded) shrink() else expand()
	}

	internal fun setExpanded(expanded: Boolean, notify: Boolean = true) {
		isExpanded = expanded
		viewMode = if (expanded) CalendarViewMode.MONTH else CalendarViewMode.WEEK
		if (notify) onViewModeChange?.invoke(viewMode)
	}

	// ---------------------------------------------------------------- 页面切换

	/**
	 * 月视图翻页后同步锚定日期与选择，由分页器内部调用。
	 */
	internal fun onMonthPageChanged(page: Int) {
		val month = monthAt(page)
		val previousMonth = anchorDay.month
		val previousYear = anchorDay.year
		// 选中日若落在本页月份就沿用，并让锚定日跟随它；
		// 这样收起为周视图时展示的是选中日所在周，而不是该月 1 号所在周。
		val current = selectedDay
		val keep =
			if (current != null && current.year == month.year && current.month == month.month) current
			else null
		anchorDay = keep ?: clampToRange(month)
		if (selectMode == CalendarSelectMode.Default || selectMode == CalendarSelectMode.Single) {
			selectedDay = keep ?: clampToRange(month)
		}
		refresh()
		if (previousYear != anchorDay.year) onYearChange?.invoke(anchorDay.year)
		if (previousMonth != anchorDay.month || previousYear != anchorDay.year) {
			onMonthChange?.invoke(anchorDay.year, anchorDay.month)
		}
	}

	/**
	 * 周视图翻页后只同步锚定日，不改变已选中的日期，由分页器内部调用。
	 */
	internal fun onWeekPageChanged(page: Int) {
		val first = weekAt(page)
		val days = buildWeekDays(first)
		val previousMonth = anchorDay.month
		val previousYear = anchorDay.year
		// 横向翻周只改变当前展示的那一周，不改变已选中的日期；
		// 保持与原锚定日同一个星期几，翻页时列位置稳定
		val target = days.firstOrNull { it.dayOfWeek == anchorDay.dayOfWeek } ?: first
		anchorDay = target
		refresh()
		if (previousYear != target.year) onYearChange?.invoke(target.year)
		if (previousMonth != target.month) onMonthChange?.invoke(target.year, target.month)
		onWeekChange?.invoke(days)
	}

	// ---------------------------------------------------------------- 状态查询

	/**
	 * 计算某个日期当前的绘制状态。
	 */
	fun dayState(day: CalendarDay): CalendarDayState {
		val isSelected = selectedDays.any { it == day }
		val start = rangeStart
		val end = rangeEnd
		val inRange = start != null && end != null &&
			compare(day, start) >= 0 && compare(day, end) <= 0
		val inVisibleRange = isInRange(day)
		return CalendarDayState(
			isSelected = isSelected,
			isCurrentDay = day == today,
			isCurrentMonth = day.year == anchorDay.year && day.month == anchorDay.month,
			isWeekend = day.isWeekendDay,
			isInRange = inRange,
			isRangeStart = start != null && day == start && end != null,
			isRangeEnd = end != null && day == end,
			isOutOfRange = !inVisibleRange,
			isIntercepted = isIntercepted(day),
			isDisabled = !inVisibleRange || isIntercepted(day),
			hasScheme = schemeDates.containsKey(day),
			scheme = schemeDates[day],
			isExpandedWeek = viewMode == CalendarViewMode.MONTH,
		)
	}

	/**
	 * 当前月份的星期栏文字，已按周起始旋转。
	 */
	internal fun weekBarLabels(resolve: (Int) -> String): List<String> =
		config.weekBarLabels
			?: (0..6).map { resolve((it + config.weekStartValue - 1 + 7) % 7) }

	internal companion object {
		/**
		 * 由旧版日历对象构造日期
		 */
		fun of(sysuerCalendar: SysuerCalendar): CalendarDay =
			CalendarDay(sysuerCalendar.year, sysuerCalendar.month, sysuerCalendar.day)
	}
}

/**
 * 创建并记住一个 [ComposeCalendarState]。
 */
@Composable
fun rememberComposeCalendarState(
	config: ComposeCalendarConfig = ComposeCalendarConfig(),
	initialDate: CalendarDay? = null,
	initialMode: CalendarViewMode = CalendarViewMode.MONTH,
): ComposeCalendarState {
	val context = LocalContext.current
	return remember(config, initialDate, initialMode) {
		ComposeCalendarState(config, initialDate, initialMode, context)
	}
}
