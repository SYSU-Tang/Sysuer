package com.miyuyan.sysuer.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.haibin.calendarview.compose.CalendarColors
import com.haibin.calendarview.compose.CalendarDay
import com.haibin.calendarview.compose.CalendarDayScheme
import com.haibin.calendarview.compose.CalendarSelectMode
import com.haibin.calendarview.compose.CalendarViewMode
import com.haibin.calendarview.compose.ComposeCalendar
import com.haibin.calendarview.compose.ComposeCalendarConfig
import com.haibin.calendarview.compose.rememberComposeCalendarState

/** 预览用：以今天为基准生成若干标记日期 */
private fun previewSchemeDates(color: Color): Map<CalendarDay, CalendarDayScheme> {
	val now = System.currentTimeMillis()
	return mapOf(
			CalendarDay.of(now + 3 * 86_400_000L) to CalendarDayScheme(color, "假"),
			CalendarDay.of(now + 10 * 86_400_000L) to CalendarDayScheme(color, "考"),
	)
}

/** 预览一：默认展开的月视图外观 */
@Preview(name = "月视图展开", showBackground = true, widthDp = 380, heightDp = 420)
@Composable
private fun ComposeCalendarMonthExpandedPreview() {
	val state = rememberComposeCalendarState(initialDate = CalendarDay.today())
	val schemeColor = MaterialTheme.colorScheme.error
	// 标记日期只在首次组合时写入一次
	remember(schemeColor) { state.schemeDates = previewSchemeDates(schemeColor) }
	ComposeCalendar(state = state)
}

/** 预览二：收起后的周视图 */
@Preview(name = "周视图收起", showBackground = true, widthDp = 380, heightDp = 260)
@Composable
private fun ComposeCalendarWeekCollapsedPreview() {
	val state = rememberComposeCalendarState(
			config = ComposeCalendarConfig(defaultExpanded = false),
			initialDate = CalendarDay.today(),
			initialMode = CalendarViewMode.WEEK,
	)
	ComposeCalendar(state = state)
}

/** 预览三：范围选择模式，最少 2 天、最多 14 天 */
@Preview(name = "范围选择", showBackground = true, widthDp = 380, heightDp = 420)
@Composable
private fun ComposeCalendarRangeSelectPreview() {
	val today = CalendarDay.today()
	val state = rememberComposeCalendarState(
			config = ComposeCalendarConfig(
					selectMode = CalendarSelectMode.Range,
					minSelectRange = 2,
					maxSelectRange = 14,
			),
			initialDate = today,
	)
	// 预置一段范围，仅在首次组合时执行
	remember {
		state.select(CalendarDay.of(today.timeInMillis + 2 * 86_400_000L))
		state.select(CalendarDay.of(today.timeInMillis + 8 * 86_400_000L))
	}
	ComposeCalendar(state = state)
}

/** 预览四：高度自定义（自定义 dayContent / weekBarContent / 配色） */
@Preview(name = "深度自定义", showBackground = true, widthDp = 380, heightDp = 420)
@Composable
private fun ComposeCalendarCustomPreview() {
	val colors = CalendarColors.default().copy(
			background = Color(0xFF101418),
			dayTextColor = Color(0xFFE6E1E5),
			otherMonthDayTextColor = Color(0xFF49454F),
			selectedTextColor = Color(0xFF101418),
			selectedBackgroundColor = Color(0xFFF2C14E),
			currentDayTextColor = Color(0xFFF2C14E),
			weekBarTextColor = Color(0xFFF2C14E),
			weekBarBackground = Color(0xFF1B2026),
	)
	val config = ComposeCalendarConfig(
			colors = colors,
			dayTextSize = 16.sp,
			weekBarTextSize = 13.sp,
			weekBarLabels = listOf("一", "二", "三", "四", "五", "六", "日"),
			dayContent = { _, dayState ->
				Box(
						modifier = Modifier
							.size(34.dp)
							.clip(CircleShape)
							.background(if (dayState.isSelected) colors.selectedBackgroundColor else Color.Transparent),
						contentAlignment = Alignment.Center,
				) {
					Text(
							text = "•",
							color = if (dayState.isWeekend) Color(0xFFFF8A80) else colors.dayTextColor,
							fontWeight = FontWeight.Medium,
					)
				}
			},
			weekBarContent = { labels ->
				labels.forEach { label ->
					Box(
							modifier = Modifier.weight(1f),
							contentAlignment = Alignment.Center,
					) {
						Text(
								text = label,
								color = colors.weekBarTextColor,
								textAlign = TextAlign.Center,
								fontWeight = FontWeight.SemiBold,
						)
					}
				}
			},
	)
	val state = rememberComposeCalendarState(config = config, initialDate = CalendarDay.today())
	Column(
			modifier = Modifier
				.fillMaxWidth()
				.background(colors.background)
	) {
		Text(
				text = "我的日程",
				color = colors.dayTextColor,
				modifier = Modifier.padding(16.dp),
				fontWeight = FontWeight.Bold,
		)
		ComposeCalendar(state = state, config = config, modifier = Modifier.weight(1f))
	}
}
