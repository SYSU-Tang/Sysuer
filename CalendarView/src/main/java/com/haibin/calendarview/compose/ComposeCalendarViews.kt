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

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.haibin.calendarview.CalendarUtil
import com.haibin.calendarview.LunarCalendar
import com.haibin.calendarview.R
import kotlin.math.min

/**
 * 计算某个月在月视图中的高度（像素）。
 */
internal fun monthHeightPx(
	year: Int,
	month: Int,
	config: ComposeCalendarConfig,
	density: Density,
): Float {
	val item = with(density) { config.itemHeight.toPx().toInt() }
	return CalendarUtil.getMonthViewHeight(
			year, month, item, config.weekStartValue, config.monthShowModeValue,
	).toFloat()
}

/**
 * 在指定基线上居中绘制一行文本。
 */
private fun DrawScope.drawCenteredText(
	measurer: TextMeasurer,
	text: String,
	centerX: Float,
	baselineY: Float,
	style: TextStyle,
) {
	if (text.isEmpty()) return
	val layout = measurer.measure(text = text, style = style)
	drawText(
			textLayoutResult = layout,
			topLeft = Offset(centerX - layout.size.width / 2f, baselineY - layout.firstBaseline),
	)
}

/**
 * 以某个点为中心绘制一行文本。
 */
private fun DrawScope.drawCenteredTextAt(
	measurer: TextMeasurer,
	text: String,
	center: Offset,
	style: TextStyle,
) {
	if (text.isEmpty()) return
	val layout = measurer.measure(text = text, style = style)
	drawText(
			textLayoutResult = layout,
			topLeft = Offset(
					center.x - layout.size.width / 2f,
					center.y - layout.size.height / 2f,
			),
	)
}

/**
 * 日期单元格：默认绘制选中圆、日期文字、农历/节日文字与标记圆点。
 *
 * 提供 [ComposeCalendarConfig.dayContent] 时完全替换默认内容，
 * 提供 [ComposeCalendarConfig.dayDecoration] 时在默认内容之上追加绘制。
 */
@Composable
internal fun CalendarDayCell(
	day: CalendarDay,
	state: CalendarDayState,
	config: ComposeCalendarConfig,
	modifier: Modifier = Modifier,
	onClick: () -> Unit,
	onLongClick: () -> Unit,
) {
	val measurer = rememberTextMeasurer()
	val colors = config.colors
	val density = LocalDensity.current
	val lunarText = remember(day) { resolveLunarText(day) }
	val scheme = state.scheme

	// 日期文字颜色，优先级参考 CustomMonthView
	val dayColor = when {
		state.isSelected -> colors.selectedTextColor
		state.isCurrentDay -> colors.currentDayTextColor
		state.isDisabled -> colors.disabledTextColor
		state.isWeekend && state.isCurrentMonth -> colors.effectiveWeekendTextColor
		state.isCurrentMonth -> colors.dayTextColor
		else -> colors.otherMonthDayTextColor
	}
	val lunarColor = when {
		state.isSelected -> colors.selectedLunarTextColor
		state.isCurrentDay -> colors.currentDayLunarTextColor
		state.isDisabled -> colors.disabledTextColor
		state.isWeekend && state.isCurrentMonth -> colors.effectiveWeekendLunarTextColor
		state.isCurrentMonth -> colors.lunarTextColor
		else -> colors.otherMonthLunarTextColor
	}
	val dayStyle = TextStyle(
			color = dayColor,
			fontSize = config.dayTextSize,
			fontWeight = FontWeight.SemiBold,
	)
	val lunarStyle = TextStyle(
			color = lunarColor,
			fontSize = config.lunarTextSize,
	)

	Box(
			modifier = modifier
				.fillMaxSize()
				.clip(RoundedCornerShape(8.dp))
				.combinedClickable(onClick = onClick, onLongClick = onLongClick),
			contentAlignment = Alignment.Center,
	) {
		val dayContent = config.dayContent
		if (dayContent != null) {
			dayContent(day, state)
			return@Box
		}
		Canvas(modifier = Modifier.fillMaxSize()) {
			val w = size.width
			val h = size.height
			val cx = w / 2f
			val radius = min(w, h) / 11f * 5f
			val padding = with(density) { 3.dp.toPx() }
			val centerY = h / 2f

			// 范围选择背景
			if (state.isInRange && !state.isRangeStart && !state.isRangeEnd) {
				drawRect(
						color = colors.rangeBackgroundColor,
						topLeft = Offset(0f, 0f),
						size = Size(w, h),
				)
			}
			// 选中/范围端点圆
			if (state.isSelected || state.isRangeStart || state.isRangeEnd) {
				drawCircle(
						color = if (state.isSelected) colors.selectedBackgroundColor
						else colors.rangeEdgeColor,
						radius = radius,
						center = Offset(cx, centerY),
				)
			}
			// 标记：右上角圆点 + 文字，与旧版 DefaultMonthView.onDrawScheme 一致
			if (state.hasScheme && config.showSchemeText) {
				val badgePadding = with(density) { 4.dp.toPx() }
				val badgeRadius = min(with(density) { 7.dp.toPx() }, h / 3f)
				val badgeCenter = Offset(
						x = w - badgePadding - badgeRadius / 2f,
						y = badgePadding + badgeRadius,
				)
				drawCircle(
						color = scheme?.color ?: colors.schemeDefaultColor,
						radius = badgeRadius,
						center = badgeCenter,
				)
				val schemeText = scheme?.text
				if (!schemeText.isNullOrEmpty()) {
					drawCenteredTextAt(
							measurer = measurer,
							text = schemeText,
							center = badgeCenter,
							style = TextStyle(
									color = colors.schemeTextColor,
									fontSize = 8.sp,
									fontWeight = FontWeight.Bold,
							),
					)
				}
			}
			val baseline = centerY + with(density) { config.dayTextSize.toPx() } * 0.35f
			drawCenteredText(measurer, day.day.toString(), cx, baseline - h / 6f, dayStyle)
			if (config.showLunar) {
				drawCenteredText(measurer, lunarText, cx, baseline + h / 10f, lunarStyle)
			}
			config.dayDecoration?.invoke(this, day, state)
		}
	}
}

/**
 * 解析日期的农历/节日文字。
 */
private fun resolveLunarText(day: CalendarDay): String = runCatching {
	LunarCalendar.getLunarText(day.year, day.month, day.day)
}.getOrDefault("")

/**
 * 月视图网格：最多 6 行 7 列，按 [ComposeCalendarConfig.monthShowMode] 决定显示方式。
 *
 * @param foldProgress 折叠进度，1 表示选中行之外的行已全部折完、选中行已就位
 * @param anchorDay 折叠目标日期，由调用方传入以便选中日变化时能触发重组
 */
@Composable
internal fun MonthGridView(
	state: ComposeCalendarState,
	year: Int,
	month: Int,
	config: ComposeCalendarConfig,
	modifier: Modifier = Modifier,
	foldProgress: Float = 0f,
	anchorDay: CalendarDay? = null,
) {
	val days = remember(state, year, month, config) {
		val todaySysuer = state.today.toSysuerCalendar().apply { isCurrentDay = true }
		CalendarUtil.initCalendarForMonthView(year, month, todaySysuer, config.weekStartValue)
			.map { CalendarDay(it.year, it.month, it.day) }
	}
	val rowCount = remember(year, month, config) {
		CalendarUtil.getMonthViewLineCount(
				year, month, config.weekStartValue, config.monthShowModeValue,
		).coerceIn(1, 6)
	}
	val scope = remember(state, year, month, config, days, rowCount) {
		MonthViewScope(year, month, days, state, config, rowCount)
	}
	// 折叠锚点：直接在「当前渲染的这个月」的网格里定位选中日所在行，
	// 而不是另算一套日期行号，避免两者错位时把选中周当成普通行缩放掉；
	// 相邻月份页里找不到选中日，就不做折叠动画（保持常规渲染）
	val collapseAnchor = anchorDay ?: state.selectedDay ?: state.anchorDay
	val anchorRow = remember(days, collapseAnchor) {
		val index = days.indexOfFirst { it == collapseAnchor }
		if (index >= 0) index / 7 + 1 else -1
	}
	val custom = config.monthViewContent
	if (custom != null) {
		custom(scope)
		return
	}

	Column(
			modifier = modifier
				.fillMaxWidth()
				.padding(
						start = config.calendarPaddingLeft,
						end = config.calendarPaddingRight,
				),
	) {
		val rows = if (config.monthShowMode == CalendarMonthShowMode.AllMonth) 6 else rowCount
		// 折叠顺序：先选中行上方由近及远，再选中行下方由近及远。
		// 上方各行依次折完，选中行就被逐行顶上去，全部折完时正好落到周视图位置
		val collapseOrder = remember(anchorRow, rows) {
			if (anchorRow <= 0) emptyList()
			else buildList {
				for (r in anchorRow - 1 downTo 1) add(r)
				for (r in anchorRow + 1..rows) add(r)
			}
		}
		val stepCount = collapseOrder.size
		for (row in 0 until rows) {
			// 逐行折叠：每行占 1/stepCount 的行程并依次错开，行高真正收缩，
			// 于是选中行会被上方各行「顶」着逐行上移；选中行不参与折叠，
			// 行高与透明度始终不变
			val slot = collapseOrder.indexOf(row + 1)
			val rowProgress = if (slot < 0 || stepCount == 0) 0f
			else (foldProgress * stepCount - slot).coerceIn(0f, 1f)
			Row(
					modifier = Modifier
						.fillMaxWidth()
						.height(config.itemHeight * (1f - rowProgress))
						// 折叠中的行按自身高度裁剪，避免缩得很矮时文字溢出到相邻行
						.graphicsLayer {
							alpha = 1f - rowProgress
							clip = true
						},
			) {
				for (column in 0..6) {
					val index = row * 7 + column
					val day = days.getOrNull(index)
					val visible =
						day != null && (config.showOutOfRangeDays || state.isInRange(day)) && when (config.monthShowMode) {
							CalendarMonthShowMode.OnlyCurrentMonth -> day.year == year && day.month == month

							else -> true
						}
					if (day != null && visible) {
						CalendarDayCell(
								day = day,
								state = state.dayState(day),
								config = config,
								modifier = Modifier
									.weight(1f)
									.height(config.itemHeight),
								onClick = { handleDayClick(state, day) },
								onLongClick = { handleDayLongClick(state, day) },
						)
					} else {
						Spacer(
								modifier = Modifier
									.weight(1f)
									.height(config.itemHeight),
						)
					}
				}
			}
		}
	}
}

/**
 * 周视图网格：一行 7 个日期。
 */
@Composable
internal fun WeekGridView(
	state: ComposeCalendarState,
	firstDay: CalendarDay,
	config: ComposeCalendarConfig,
	modifier: Modifier = Modifier,
) {
	val days = remember(firstDay) { (0..6).map { firstDay.plusDays(it) } }
	val scope = remember(state, firstDay, config, days) {
		WeekViewScope(days, state, config)
	}
	val custom = config.weekViewContent
	if (custom != null) {
		custom(scope)
		return
	}
	Row(
			modifier = modifier
				.fillMaxWidth()
				.padding(
						start = config.calendarPaddingLeft,
						end = config.calendarPaddingRight,
				),
	) {
		days.forEach { day ->
			CalendarDayCell(
					day = day,
					state = state.dayState(day),
					config = config,
					modifier = Modifier
						.weight(1f)
						.height(config.itemHeight),
					onClick = { handleDayClick(state, day) },
					onLongClick = { handleDayLongClick(state, day) },
			)
		}
	}
}

/**
 * 星期栏，默认使用本地化的 `week_string_array` 并按周起始旋转。
 */
@Composable
fun ComposeCalendarWeekBar(
	state: ComposeCalendarState,
	modifier: Modifier = Modifier,
	config: ComposeCalendarConfig = state.config,
) {
	val weekStringArray = stringArrayResource(R.array.week_string_array)
	val labels = remember(config.weekStart, config.weekBarLabels) {
		val base = config.weekBarLabels ?: weekStringArray.toList()
		(0..6).map { base[(it + config.weekStartValue - 1 + 7) % base.size] }
	}
	val custom = config.weekBarContent
	if (custom != null) {
		Row(
				modifier = modifier.background(config.colors.weekBarBackground),
				verticalAlignment = Alignment.CenterVertically,
		) {
			custom(labels)
		}
		return
	}
	Row(
			modifier = modifier.background(config.colors.weekBarBackground),
			verticalAlignment = Alignment.CenterVertically,
	) {
		labels.forEach { label ->
			Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
				BasicText(
						text = label,
						style = TextStyle(
								color = config.colors.weekBarTextColor,
								fontSize = config.weekBarTextSize,
						),
				)
			}
		}
	}
}

/**
 * 点击日期：先派发点击回调，再执行选择。
 */
private fun handleDayClick(state: ComposeCalendarState, day: CalendarDay) {
	state.onDayClick?.invoke(day)
	state.select(day, true)
}

/**
 * 长按日期。
 */
private fun handleDayLongClick(state: ComposeCalendarState, day: CalendarDay) {
	state.onDayLongClick?.invoke(day)
	state.select(day, true)
}
