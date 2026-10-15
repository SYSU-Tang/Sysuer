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
import android.util.AttributeSet
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.withStyledAttributes
import androidx.compose.ui.graphics.toArgb
import com.haibin.calendarview.R

/**
 * Compose 日历的全部配置项，可通过 Kotlin 直接构造，也可由 XML 属性解析。
 *
 * 通过 [dayContent] / [dayDecoration] / [weekBarContent] / [monthViewContent] / [weekViewContent]
 * 五个插槽实现高度自定义。
 */
@Immutable
data class ComposeCalendarConfig(
	/** 周起始 */
	val weekStart: CalendarWeekStart = CalendarWeekStart.Monday,
	/** 月视图显示模式 */
	val monthShowMode: CalendarMonthShowMode = CalendarMonthShowMode.OnlyCurrentMonth,
	/** 选择模式 */
	val selectMode: CalendarSelectMode = CalendarSelectMode.Default,
	/** 最小年份 */
	val minYear: Int = 1900,
	/** 最大年份 */
	val maxYear: Int = 2099,
	/** 可选最小日期，优先于 [minYear] */
	val minDate: CalendarDay? = null,
	/** 可选最大日期，优先于 [maxYear] */
	val maxDate: CalendarDay? = null,
	/** 日历项高度 */
	val itemHeight: Dp = 52.dp,
	/** 星期栏高度 */
	val weekBarHeight: Dp = 40.dp,
	/** 日历内部左 padding */
	val calendarPaddingLeft: Dp = 0.dp,
	/** 日历内部右 padding */
	val calendarPaddingRight: Dp = 0.dp,
	/** 日期文字大小 */
	val dayTextSize: TextUnit = 15.sp,
	/** 农历文字大小 */
	val lunarTextSize: TextUnit = 10.sp,
	/** 星期栏文字大小 */
	val weekBarTextSize: TextUnit = 12.sp,
	/** 默认是否展开月视图 */
	val defaultExpanded: Boolean = true,
	/** 是否显示星期栏 */
	val showWeekBar: Boolean = true,
	/** 是否显示农历 */
	val showLunar: Boolean = true,
	/** 是否显示标记文字 */
	val showSchemeText: Boolean = true,
	/** 是否显示超出范围的日期 */
	val showOutOfRangeDays: Boolean = true,
	/** 月视图是否可横向滑动 */
	val isMonthPagerScrollable: Boolean = true,
	/** 周视图是否可横向滑动 */
	val isWeekPagerScrollable: Boolean = true,
	/** 是否启用展开/收起手势 */
	val isGestureEnabled: Boolean = true,
	/** 多选上限，0 表示不限制 */
	val maxMultiSelectSize: Int = 0,
	/** 范围选择最小天数 */
	val minSelectRange: Int = 0,
	/** 范围选择最大天数 */
	val maxSelectRange: Int = 0,
	/** 星期栏文字，为空时使用本地化默认值 */
	val weekBarLabels: List<String>? = null,
	/** 配色 */
	val colors: CalendarColors = CalendarColors.default(),
	/** 日期内容插槽，非空时完全替换默认内容 */
	val dayContent: (@Composable BoxScope.(CalendarDay, CalendarDayState) -> Unit)? = null,
	/** 日期装饰插槽，在默认内容之上追加绘制 */
	val dayDecoration: (DrawScope.(CalendarDay, CalendarDayState) -> Unit)? = null,
	/** 星期栏内容插槽 */
	val weekBarContent: (@Composable RowScope.(List<String>) -> Unit)? = null,
	/** 月视图整体内容插槽，非空时替换默认月视图网格 */
	val monthViewContent: (@Composable MonthViewScope.() -> Unit)? = null,
	/** 周视图整体内容插槽，非空时替换默认周视图网格 */
	val weekViewContent: (@Composable WeekViewScope.() -> Unit)? = null,
) {
	/** 周起始对应的 int 值，用于调用旧版日期工具 */
	val weekStartValue: Int get() = weekStart.value

	/** 月视图显示模式对应的 int 值 */
	val monthShowModeValue: Int get() = monthShowMode.value
}

/**
 * 从 XML 属性解析配置，缺失的属性回退到 [ComposeCalendarConfig] 默认值。
 *
 * 用法：
 * ```
 * val attrs = obtainStyledAttributes(null, R.styleable.ComposeCalendarView)
 * val config = rememberComposeCalendarConfig(attrs)
 * attrs.recycle()
 * ```
 */
@Composable
fun rememberComposeCalendarConfig(attrs: AttributeSet?): ComposeCalendarConfig {
	val context = LocalContext.current
	return remember(attrs) { parseComposeCalendarConfig(context, attrs) }
}

/**
 * 解析 XML 属性为配置对象。
 */
internal fun parseComposeCalendarConfig(
	context: Context,
	attrs: AttributeSet?,
): ComposeCalendarConfig {
	if (attrs == null) return ComposeCalendarConfig()
	val defaults = ComposeCalendarConfig()
	var result = defaults
	context.withStyledAttributes(attrs, R.styleable.ComposeCalendarView) {
		fun colorOf(attr: Int, default: Color): Color = Color(getColor(attr, default.toArgb()))
		val weekStart = CalendarWeekStart.entries
			.firstOrNull { it.value == getInt(R.styleable.ComposeCalendarView_compose_week_start_with, defaults.weekStart.value) }
			?: defaults.weekStart
		val monthShowMode = CalendarMonthShowMode.entries
			.firstOrNull { it.value == getInt(R.styleable.ComposeCalendarView_compose_month_show_mode, defaults.monthShowMode.value) }
			?: defaults.monthShowMode
		val selectMode = when (getInt(R.styleable.ComposeCalendarView_compose_select_mode, -1)) {
			0 -> CalendarSelectMode.Default
			1 -> CalendarSelectMode.Single
			2 -> CalendarSelectMode.Range
			3 -> CalendarSelectMode.Multi
			else -> defaults.selectMode
		}
		val minYear = getInt(R.styleable.ComposeCalendarView_compose_min_year, defaults.minYear)
		val maxYear = getInt(R.styleable.ComposeCalendarView_compose_max_year, defaults.maxYear)
		val minMonth = getInt(R.styleable.ComposeCalendarView_compose_min_year_month, 1)
		val maxMonth = getInt(R.styleable.ComposeCalendarView_compose_max_year_month, 12)
		val minDay = getInt(R.styleable.ComposeCalendarView_compose_min_year_day, 1)
		val maxDay = getInt(R.styleable.ComposeCalendarView_compose_max_year_day, 31)
		val colors = CalendarColors.default().copy(
			background = colorOf(R.styleable.ComposeCalendarView_compose_background, defaults.colors.background),
			weekBarBackground = colorOf(R.styleable.ComposeCalendarView_compose_week_bar_background, defaults.colors.weekBarBackground),
			weekBarTextColor = colorOf(R.styleable.ComposeCalendarView_compose_week_bar_text_color, defaults.colors.weekBarTextColor),
			dayTextColor = colorOf(R.styleable.ComposeCalendarView_compose_day_text_color, defaults.colors.dayTextColor),
			otherMonthDayTextColor = colorOf(R.styleable.ComposeCalendarView_compose_other_month_day_text_color, defaults.colors.otherMonthDayTextColor),
			lunarTextColor = colorOf(R.styleable.ComposeCalendarView_compose_lunar_text_color, defaults.colors.lunarTextColor),
			otherMonthLunarTextColor = colorOf(R.styleable.ComposeCalendarView_compose_other_month_lunar_text_color, defaults.colors.otherMonthLunarTextColor),
			currentDayTextColor = colorOf(R.styleable.ComposeCalendarView_compose_current_day_text_color, defaults.colors.currentDayTextColor),
			currentDayLunarTextColor = colorOf(R.styleable.ComposeCalendarView_compose_current_day_lunar_text_color, defaults.colors.currentDayLunarTextColor),
			selectedBackgroundColor = colorOf(R.styleable.ComposeCalendarView_compose_selected_background_color, defaults.colors.selectedBackgroundColor),
			selectedTextColor = colorOf(R.styleable.ComposeCalendarView_compose_selected_text_color, defaults.colors.selectedTextColor),
			selectedLunarTextColor = colorOf(R.styleable.ComposeCalendarView_compose_selected_lunar_text_color, defaults.colors.selectedLunarTextColor),
			schemeTextColor = colorOf(R.styleable.ComposeCalendarView_compose_scheme_text_color, defaults.colors.schemeTextColor),
			schemeLunarTextColor = colorOf(R.styleable.ComposeCalendarView_compose_scheme_lunar_text_color, defaults.colors.schemeLunarTextColor),
			schemeDefaultColor = colorOf(R.styleable.ComposeCalendarView_compose_scheme_default_color, defaults.colors.schemeDefaultColor),
			rangeBackgroundColor = colorOf(R.styleable.ComposeCalendarView_compose_range_background_color, defaults.colors.rangeBackgroundColor),
			rangeEdgeColor = colorOf(R.styleable.ComposeCalendarView_compose_range_edge_color, defaults.colors.rangeEdgeColor),
			disabledTextColor = colorOf(R.styleable.ComposeCalendarView_compose_disabled_text_color, defaults.colors.disabledTextColor),
			outOfRangeTextColor = colorOf(R.styleable.ComposeCalendarView_compose_out_of_range_text_color, defaults.colors.outOfRangeTextColor),
		)
		result = ComposeCalendarConfig(
			weekStart = weekStart,
			monthShowMode = monthShowMode,
			selectMode = selectMode,
			minYear = minYear,
			maxYear = maxYear,
			minDate = CalendarDay(minYear, minMonth.coerceIn(1, 12), minDay.coerceIn(1, 31)),
			maxDate = CalendarDay(maxYear, maxMonth.coerceIn(1, 12), maxDay.coerceIn(1, 31)),
			itemHeight = getDimension(R.styleable.ComposeCalendarView_compose_item_height, defaults.itemHeight.value).dp,
			weekBarHeight = getDimension(R.styleable.ComposeCalendarView_compose_week_bar_height, defaults.weekBarHeight.value).dp,
			calendarPaddingLeft = getDimension(R.styleable.ComposeCalendarView_compose_calendar_padding_left, defaults.calendarPaddingLeft.value).dp,
			calendarPaddingRight = getDimension(R.styleable.ComposeCalendarView_compose_calendar_padding_right, defaults.calendarPaddingRight.value).dp,
			dayTextSize = getDimension(R.styleable.ComposeCalendarView_compose_day_text_size, defaults.dayTextSize.value).sp,
			lunarTextSize = getDimension(R.styleable.ComposeCalendarView_compose_lunar_text_size, defaults.lunarTextSize.value).sp,
			weekBarTextSize = getDimension(R.styleable.ComposeCalendarView_compose_week_bar_text_size, defaults.weekBarTextSize.value).sp,
			defaultExpanded = getBoolean(R.styleable.ComposeCalendarView_compose_default_expanded, defaults.defaultExpanded),
			showWeekBar = getBoolean(R.styleable.ComposeCalendarView_compose_show_week_bar, defaults.showWeekBar),
			showLunar = getBoolean(R.styleable.ComposeCalendarView_compose_show_lunar, defaults.showLunar),
			showSchemeText = getBoolean(R.styleable.ComposeCalendarView_compose_show_scheme_text, defaults.showSchemeText),
			showOutOfRangeDays = getBoolean(R.styleable.ComposeCalendarView_compose_show_out_of_range_days, defaults.showOutOfRangeDays),
			isMonthPagerScrollable = getBoolean(R.styleable.ComposeCalendarView_compose_month_pager_scrollable, defaults.isMonthPagerScrollable),
			isWeekPagerScrollable = getBoolean(R.styleable.ComposeCalendarView_compose_week_pager_scrollable, defaults.isWeekPagerScrollable),
			isGestureEnabled = getBoolean(R.styleable.ComposeCalendarView_compose_gesture_enabled, defaults.isGestureEnabled),
			maxMultiSelectSize = getInt(R.styleable.ComposeCalendarView_compose_max_multi_select_size, defaults.maxMultiSelectSize),
			minSelectRange = getInt(R.styleable.ComposeCalendarView_compose_min_select_range, defaults.minSelectRange),
			maxSelectRange = getInt(R.styleable.ComposeCalendarView_compose_max_select_range, defaults.maxSelectRange),
			colors = colors,
		)
	}
	return result
}
