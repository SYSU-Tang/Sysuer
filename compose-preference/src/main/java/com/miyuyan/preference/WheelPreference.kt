package com.miyuyan.preference

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * 滚轮选择偏好项：trailingContent 为 iOS 风格滚轮（移植自培训方案查询页的 GradePicker），
 * 滚动或点选后中间项即选中项，即时回调。适合在互斥选项较多、希望滑动直接选择的场景。
 *
 * 条目外观与 [Preference] 一致（同一分组的圆角衔接），标题位于上方，可经 [summary]
 * 覆盖内容区文案；选中变化以统一的 (索引, 选项文案, 业务值) 形态经 [onChange] 上报。
 *
 * @param entries 选项文案列表。
 * @param entryValues 与 [entries] 按索引对应的业务值。
 * @param selectedIndex 当前选中的选项索引；越界或 null 时落到第一个有效项。
 * @param onChange 选中项变化回调，参数为 (索引, 选项文案, 业务值)。
 * @param visibleCount 滚轮可见条目数（奇数时中间项即选中项）。
 * @param itemHeight 单个条目高度。
 *
 * 注意：trailingContent 使用 [Modifier.fillMaxWidth] 铺满剩余宽度，而 M3 ListItem
 * 的测量顺序是 leading → trailing → 标题文本，标题只能拿到 trailing 之后的剩余宽度，
 * 因此铺满后标题（overline）会被挤成 0 宽。需要保留标题时改用 fillMaxWidth(fraction)
 * 或把滚轮放到内容区（content 槽位）。
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun <T> WheelPreference(
	modifier: Modifier = Modifier,
	title: String,
	entries: List<String>,
	entryValues: List<T>? = null,
	selectedIndex: Int? = null,
	onChange: ((Int?, String?, T?) -> Unit)? = null,
	enabled: Boolean = true,
	icon: @Composable (() -> Unit)? = null,
	summary: String? = null,
	visibleCount: Int = 3,
	itemHeight: Dp = 44.dp,
	pickerWidth: Dp = 160.dp,
) {
	Preference(
			onClick = null,
			title = title,
			enabled = enabled,
			modifier = modifier,
			icon = icon,
			summary = summary,
			trailing = {
				Box(Modifier.height(itemHeight * visibleCount).width(pickerWidth)) {
					WheelPicker(
							entries = entries,
							selectedIndex = selectedIndex,
							onSelected = { i ->
								onChange?.invoke(i, entries.getOrNull(i), entryValues?.getOrNull(i))
							},
							enabled = enabled,
							itemHeight = itemHeight,
							visibleCount = visibleCount,
					)
				}
			})
}

/**
 * iOS 风格滚轮选择器：惯性滚动经 snap 吸附后，最靠近视口中心的条目即选中项
 * （放大、高亮，其余按距离渐隐缩小），点击条目滚动到该项。
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun WheelPicker(
	entries: List<String>,
	selectedIndex: Int?,
	onSelected: (Int) -> Unit,
	modifier: Modifier = Modifier,
	enabled: Boolean = true,
	itemHeight: Dp = 44.dp,
	visibleCount: Int = 3,
) {
	if (entries.isEmpty()) return

	val scope = rememberCoroutineScope()
	val initialIndex = (selectedIndex ?: 0).coerceIn(entries.indices)
	val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)
	val snapBehavior = rememberSnapFlingBehavior(lazyListState = listState)

	// 滚轮滚到两端时不把剩余位移/惯性速度传给外层滚动容器（如 PreferenceScreen 的
	// 纵向滚动），避免在滚轮边缘继续拖动/甩动时带动整个页面滚动
	val isolateNestedScroll = remember {
		object : NestedScrollConnection {
			override fun onPostScroll(
				consumed: Offset,
				available: Offset,
				source: NestedScrollSource,
			): Offset = available

			override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity =
				available
		}
	}

	// 当前吸附到视口中心的条目
	val centerIndex by remember {
		derivedStateOf {
			val layoutInfo = listState.layoutInfo
			val viewportCenter = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2
			layoutInfo.visibleItemsInfo.minByOrNull {
				abs((it.offset + it.size / 2) - viewportCenter)
			}?.index
		}
	}

	// 滚动吸附 -> 上报选中
	var reportedIndex by remember { mutableIntStateOf(initialIndex) }
	LaunchedEffect(centerIndex) {
		centerIndex?.let { index ->
			if (index in entries.indices && index != reportedIndex) {
				reportedIndex = index
				onSelected(index)
			}
		}
	}
	// 外部选中 -> 滚动到该项
	LaunchedEffect(selectedIndex) {
		val target = (selectedIndex ?: return@LaunchedEffect).coerceIn(entries.indices)
		if (target != centerIndex) {
			scope.launch { listState.animateScrollToItem(index = target) }
		}
	}

	Box(
			modifier = modifier
				.fillMaxSize()
				.height(itemHeight * visibleCount)
				.nestedScroll(isolateNestedScroll),
			contentAlignment = Alignment.Center
	) {
		// 中间选中区域高亮条
		Box(
				Modifier
					.fillMaxWidth()
					.height(itemHeight)
					.clip(RoundedCornerShape(percent = 50))
					.background(
							MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.64f)
					)
		)
		LazyColumn(
				state = listState,
				flingBehavior = snapBehavior,
				userScrollEnabled = enabled,
				modifier = Modifier.fillMaxSize(),
				contentPadding = PaddingValues(vertical = itemHeight),
				horizontalAlignment = Alignment.CenterHorizontally
		) {
			itemsIndexed(entries) { index, name ->
				val distanceFromCenter = remember {
					derivedStateOf {
						val layoutInfo = listState.layoutInfo
						val viewportCenter =
							(layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2
						layoutInfo.visibleItemsInfo.firstOrNull { it.index == index }
							?.let { item ->
								abs(item.offset + item.size / 2 - viewportCenter).toFloat() / item.size
							} ?: 3f
					}
				}

				val distance = distanceFromCenter.value.coerceIn(0f, 2f)
				val scale = 1f - distance * 0.16f
				val alpha = (1f - distance * 0.30f) * (if (enabled) 1f else 0.5f)
				val selected = index == centerIndex

				Box(
						Modifier
							.fillMaxWidth()
							.height(itemHeight)
							.graphicsLayer {
								scaleX = scale
								scaleY = scale
								this.alpha = alpha
							}
							.clip(RoundedCornerShape(percent = 50))
							.clickable(enabled = enabled) {
								scope.launch { listState.animateScrollToItem(index = index) }
							},
						contentAlignment = Alignment.Center
				) {
					Text(
							text = name,
							style = if (selected) MaterialTheme.typography.titleMedium
							else MaterialTheme.typography.bodyLarge,
							fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
							color = if (selected) MaterialTheme.colorScheme.onSurface
							else MaterialTheme.colorScheme.onSurfaceVariant,
							textAlign = TextAlign.Center,
							maxLines = 1,
					)
				}
			}
		}
	}
}
