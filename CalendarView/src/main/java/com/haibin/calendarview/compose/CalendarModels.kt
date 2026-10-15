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

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.haibin.calendarview.SysuerCalendar
import java.util.Calendar

/**
 * 日历显示模式：月视图 / 周视图
 */
enum class CalendarViewMode { MONTH, WEEK }

/**
 * 周起始，取值与 [com.haibin.calendarview.CalendarViewDelegate] 保持一致
 */
enum class CalendarWeekStart(val value: Int) { Sunday(1), Monday(2), Saturday(7) }

/**
 * 月视图显示模式，取值与 [com.haibin.calendarview.CalendarViewDelegate] 保持一致
 */
enum class CalendarMonthShowMode(val value: Int) { AllMonth(0), OnlyCurrentMonth(1), FitMonth(2) }

/**
 * 选择模式
 */
enum class CalendarSelectMode { Default, Single, Range, Multi }

/**
 * 一个日期，Compose 日历的最小数据单元。
 */
@Immutable
data class CalendarDay(val year: Int, val month: Int, val day: Int) {

	/**
	 * 对应的时间戳（当天 0 点）
	 */
	val timeInMillis: Long
		get() = Calendar.getInstance().apply {
			set(year, month - 1, day, 0, 0, 0)
			set(Calendar.MILLISECOND, 0)
		}.timeInMillis

	/**
	 * 转换为旧版日历对象，便于复用 [com.haibin.calendarview.CalendarUtil] 的日期计算。
	 */
	fun toSysuerCalendar(): SysuerCalendar {
		val calendar = SysuerCalendar()
		calendar.year = this.year
		calendar.month = this.month
		calendar.day = this.day
		return calendar
	}

	/**
	 * 该日期在一周中的位置，0 表示周日
	 */
	internal val dayOfWeek: Int
		get() = Calendar.getInstance().apply { set(year, month - 1, day) }
			.get(Calendar.DAY_OF_WEEK) - 1

	/**
	 * 是否周末
	 */
	internal val isWeekendDay: Boolean
		get() = dayOfWeek == 0 || dayOfWeek == 6

	/**
	 * 与另一个日期相差的天数
	 */
	internal fun daysUntil(other: CalendarDay): Int =
		((other.timeInMillis - timeInMillis) / 86_400_000L).toInt()

	/**
	 * 加上若干天
	 */
	internal fun plusDays(days: Int): CalendarDay =
		of(timeInMillis + days * 86_400_000L)

	override fun toString(): String =
		"$year-${month.toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}"

	companion object {
		/**
		 * 由时间戳构造
		 */
		fun of(timeInMillis: Long): CalendarDay {
			val calendar = Calendar.getInstance().apply { this.timeInMillis = timeInMillis }
			return CalendarDay(
				calendar.get(Calendar.YEAR),
				calendar.get(Calendar.MONTH) + 1,
				calendar.get(Calendar.DAY_OF_MONTH)
			)
		}

		/**
		 * 今天
		 */
		fun today(): CalendarDay = of(System.currentTimeMillis())
	}
}

/**
 * 日期标记（小圆点 + 文字）
 */
@Immutable
data class CalendarDayScheme(val color: Color, val text: String? = null)

/**
 * 某个日期在当前状态下的绘制信息，供自定义内容与装饰使用。
 */
@Immutable
data class CalendarDayState(
	val isSelected: Boolean = false,
	val isCurrentDay: Boolean = false,
	val isCurrentMonth: Boolean = true,
	val isWeekend: Boolean = false,
	val isInRange: Boolean = false,
	val isRangeStart: Boolean = false,
	val isRangeEnd: Boolean = false,
	val isOutOfRange: Boolean = false,
	val isIntercepted: Boolean = false,
	val isDisabled: Boolean = false,
	val hasScheme: Boolean = false,
	val scheme: CalendarDayScheme? = null,
	val isExpandedWeek: Boolean = false,
)

/**
 * 日历配色。默认值参考 App 的浅色主题，实际使用建议由 `MaterialTheme.colorScheme` 构造。
 */
@Immutable
data class CalendarColors(
	val background: Color,
	val weekBarBackground: Color,
	val weekBarTextColor: Color,
	val dayTextColor: Color,
	val otherMonthDayTextColor: Color,
	val lunarTextColor: Color,
	val otherMonthLunarTextColor: Color,
	val currentDayTextColor: Color,
	val currentDayLunarTextColor: Color,
	val selectedBackgroundColor: Color,
	val selectedTextColor: Color,
	val selectedLunarTextColor: Color,
	val schemeTextColor: Color,
	val schemeLunarTextColor: Color,
	val schemeDefaultColor: Color,
	val rangeBackgroundColor: Color,
	val rangeEdgeColor: Color,
	val disabledTextColor: Color,
	val outOfRangeTextColor: Color,
	val weekendTextColor: Color = Color.Unspecified,
	val weekendLunarTextColor: Color = Color.Unspecified,
) {
	/**
	 * 周末日期颜色，未单独指定时回退到“今天”的颜色。
	 */
	internal val effectiveWeekendTextColor: Color
		get() = if (weekendTextColor == Color.Unspecified) currentDayTextColor else weekendTextColor

	internal val effectiveWeekendLunarTextColor: Color
		get() =
			if (weekendLunarTextColor == Color.Unspecified) currentDayLunarTextColor
			else weekendLunarTextColor

	companion object {
		/**
		 * 默认配色
		 */
		fun default(): CalendarColors = CalendarColors(
			background = Color(0xFFF6FBF5),
			weekBarBackground = Color.Transparent,
			weekBarTextColor = Color(0xFF414942),
			dayTextColor = Color(0xFF171D18),
			otherMonthDayTextColor = Color(0xFF9AA39B),
			lunarTextColor = Color(0xFF6B746C),
			otherMonthLunarTextColor = Color(0xFFB6BEB7),
			currentDayTextColor = Color(0xFFD32F2F),
			currentDayLunarTextColor = Color(0xFFD32F2F),
			selectedBackgroundColor = Color(0xFF006B3A),
			selectedTextColor = Color(0xFFFFFFFF),
			selectedLunarTextColor = Color(0xFFFFFFFF),
			schemeTextColor = Color(0xFFFFFFFF),
			schemeLunarTextColor = Color(0xFF6B746C),
			schemeDefaultColor = Color(0xFF128C4B),
			rangeBackgroundColor = Color(0x1A006B3A),
			rangeEdgeColor = Color(0xFF006B3A),
			disabledTextColor = Color(0xFFBDBDBD),
			outOfRangeTextColor = Color(0xFFCFD8CF),
			weekendTextColor = Color(0xFFC24A20),
			weekendLunarTextColor = Color(0xFFC24A20),
		)
	}
}

/**
 * 月视图作用域，供 [ComposeCalendarConfig.monthViewContent] 使用。
 */
class MonthViewScope internal constructor(
	val year: Int,
	val month: Int,
	val days: List<CalendarDay>,
	val state: ComposeCalendarState,
	val config: ComposeCalendarConfig,
	val rowCount: Int,
)

/**
 * 周视图作用域，供 [ComposeCalendarConfig.weekViewContent] 使用。
 */
class WeekViewScope internal constructor(
	val days: List<CalendarDay>,
	val state: ComposeCalendarState,
	val config: ComposeCalendarConfig,
)

/**
 * 日期内容插槽：完全替换默认的日期文本绘制。
 */
typealias CalendarDayContent = @Composable BoxScope.(CalendarDay, CalendarDayState) -> Unit

/**
 * 日期装饰插槽：在默认绘制之上追加绘制内容。
 */
typealias CalendarDayDecoration = DrawScope.(CalendarDay, CalendarDayState) -> Unit

/**
 * 星期栏内容插槽。
 */
typealias CalendarWeekBarContent = @Composable RowScope.(List<String>) -> Unit
