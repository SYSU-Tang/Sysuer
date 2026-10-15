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

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Velocity
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.min

/**
 * 收起动画收尾交接的展开度阈值：低于该值时选中周已平移到位，
 * 开始与周视图分页器做透明度交接。
 */
private const val COLLAPSE_HANDOFF = 0.15f

/**
 * Pager 已吸附到位判定阈值：页偏移绝对值小于该值时视为已停稳。
 * 拖拽/fling 交接、滚动取消等瞬态会让偏移停在两页之间，此时不应写状态。
 */
private const val SNAP_EPSILON = 0.01f

/**
 * Compose 日历布局组件，兼容旧版 [com.haibin.calendarview.CalendarLayout]。
 *
 * 特性：
 * 1. 月视图与周视图都可以横向滑动切换；
 * 2. 在 [content] 上纵向滑动（或直接拖动日历本身）可以把月视图收起为单周，再次下拉展开；
 * 3. 通过 [ComposeCalendarConfig] 与五个插槽实现高度自定义。
 *
 * @param state 日历状态
 * @param modifier 修饰符
 * @param config 配置，默认取 [ComposeCalendarState.config]
 * @param contentScrollable 内容区是否可滚动，可滚动时由嵌套滑动驱动收起/展开
 * @param content 日历下方的内容
 */
@Composable
fun ComposeCalendar(
	state: ComposeCalendarState,
	modifier: Modifier = Modifier,
	config: ComposeCalendarConfig = state.config,
	contentScrollable: Boolean = true,
	content: @Composable () -> Unit = {},
) {
	val density = LocalDensity.current
	val scope = rememberCoroutineScope()

	val weekBarPx = with(density) { if (config.showWeekBar) config.weekBarHeight.toPx() else 0f }
	val weekRowPx = with(density) { config.itemHeight.toPx() }

	val monthPagerState = rememberPagerState(
			initialPage = state.monthIndexOf(state.anchorDay),
			pageCount = { state.monthCount },
	)
	val weekPagerState = rememberPagerState(
			initialPage = state.weekIndexOf(state.anchorDay),
			pageCount = { state.weekCount },
	)

	// 展开进度：1f 表示完全展开月视图，0f 表示收起为单周
	var expansion by remember { mutableFloatStateOf(if (config.defaultExpanded) 1f else 0f) }

	// 月视图高度：随分页滚动在相邻月份之间插值
	val monthHeight = monthPagerHeight(state, config, monthPagerState)
	val rangePx = (monthHeight - weekRowPx).coerceAtLeast(0f)
	val rangeState = rememberUpdatedState(rangePx)
	// 折叠进度：前 85% 的行程完成逐行折叠，最后 15% 只做与周视图的淡出交接。
	// 容器高度必须跟着 foldProgress 而不是 expansion 收缩，否则折叠已经结束、
	// 容器还在继续变矮的那一段里，选中行与下方 content 之间会露出空白
	val foldProgress = ((1f - expansion) / (1f - COLLAPSE_HANDOFF)).coerceIn(0f, 1f)
	val weekViewAlpha = ((COLLAPSE_HANDOFF - expansion) / COLLAPSE_HANDOFF).coerceIn(0f, 1f)
	val gestureEnabled = config.isGestureEnabled
	val childScrolled = remember { mutableStateOf(false) }

	fun settle(velocityY: Float) {
		val target = when {
			// 与旧版 CalendarLayout 一致：速度超过阈值才按方向吸附，否则就近吸附
			abs(velocityY) >= 800f -> if (velocityY > 0f) 1f else 0f
			else -> if (expansion > 0.5f) 1f else 0f
		}
		scope.launch {
			animate(
					initialValue = expansion,
					targetValue = target,
					animationSpec = tween(240, easing = FastOutSlowInEasing),
			) { value, _ -> expansion = value }
			state.setExpanded(target == 1f)
		}
	}

	// 状态驱动的展开/收起（expand / shrink / toggle）
	LaunchedEffect(state.isExpanded) {
		val target = if (state.isExpanded) 1f else 0f
		if (expansion != target) {
			animate(
					initialValue = expansion,
					targetValue = target,
					animationSpec = tween(state.animationDurationMs, easing = FastOutSlowInEasing),
			) { value, _ -> expansion = value }
		}
	}

	// 判断两个分页器是否已在渲染，未渲染的分页器不会真实响应用户翻页；
	// 否则 animateScrollToPage 会在未渲染的分页上挂起等待首次布局，造成延迟执行。
	val monthPagerComposed by rememberUpdatedState(weekViewAlpha < 1f)
	val weekPagerComposed by rememberUpdatedState(weekViewAlpha > 0f)

	// 月视图分页 -> 状态
	// 只接收「分页器已渲染」+「滚动已停止」+「已吸附到位」的页码。
	// 拖拽/fling 交接、滚动取消等瞬态会让 settledPage 停在半页，
	// 从而把用户点选的日期覆盖成别的日期。
	LaunchedEffect(monthPagerState) {
		snapshotFlow {
			Triple(
					monthPagerState.settledPage,
					monthPagerState.currentPageOffsetFraction,
					monthPagerState.isScrollInProgress,
			)
		}.distinctUntilChanged().collect { (page, offset, scrolling) ->
			if (!monthPagerComposed) return@collect
			if (scrolling || abs(offset) > SNAP_EPSILON) return@collect
			state.onMonthPageChanged(page)
		}
	}
	// 状态 -> 月视图分页：仅在分页器已渲染时同步，避免在未渲染的分页上挂起动画。
	val targetMonthPage = state.monthIndexOf(state.anchorDay)
	LaunchedEffect(targetMonthPage, monthPagerComposed) {
		if (!monthPagerComposed) return@LaunchedEffect
		if (monthPagerState.currentPage != targetMonthPage) {
			monthPagerState.animateScrollToPage(targetMonthPage)
		}
	}

	// 周视图分页 -> 状态
	// 首次渲染的初始页码不写状态，否则会改写选中日；只有真正滚动并停稳后才同步，
	// 折叠时也不会用陈旧页码覆盖用户的点击。
	LaunchedEffect(weekPagerState) {
		var sawScroll = false
		snapshotFlow {
			Triple(
					weekPagerState.settledPage,
					weekPagerState.currentPageOffsetFraction,
					weekPagerState.isScrollInProgress,
			)
		}.distinctUntilChanged().collect { (page, offset, scrolling) ->
			if (!weekPagerComposed) {
				sawScroll = false
				return@collect
			}
			if (scrolling) {
				sawScroll = true
				return@collect
			}
			if (abs(offset) > SNAP_EPSILON) return@collect
			if (!sawScroll) return@collect
			sawScroll = false
			state.onWeekPageChanged(page)
		}
	}
	// 状态 -> 周视图分页：仅在分页器已渲染时同步。
	// 目标取锚定日而非选中日：横向翻周只改锚定日，若跟着选中日会把页面又拉回选中周。
	val targetWeekPage = state.weekIndexOf(state.anchorDay)
	LaunchedEffect(targetWeekPage, weekPagerComposed) {
		if (!weekPagerComposed) return@LaunchedEffect
		if (weekPagerState.currentPage != targetWeekPage) {
			weekPagerState.animateScrollToPage(targetWeekPage)
		}
	}

	val nestedScrollConnection = remember(state, gestureEnabled) {
		object : NestedScrollConnection {
			override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
				if (!gestureEnabled) return Offset.Zero
				val dy = available.y
				// 向上拖动时优先收起日历
				if (dy < 0f && expansion > 0f) {
					val range = rangeState.value
					if (range <= 0f) return Offset.Zero
					val consumed = min(-dy, expansion * range)
					expansion = (expansion - consumed / range).coerceIn(0f, 1f)
					childScrolled.value = false
					return Offset(0f, -consumed)
				}
				if (dy != 0f) childScrolled.value = true
				return Offset.Zero
			}

			override fun onPostScroll(
				consumed: Offset,
				available: Offset,
				source: NestedScrollSource,
			): Offset {
				if (!gestureEnabled) return Offset.Zero
				val dy = available.y
				if (dy == 0f) return Offset.Zero
				childScrolled.value = true
				val range = rangeState.value
				if (range <= 0f) return Offset.Zero
				// 内容已经滚到边界，剩余的距离用来展开/收起
				if (dy > 0f && expansion < 1f) {
					val consumedY = min(dy, (1f - expansion) * range)
					expansion = (expansion + consumedY / range).coerceIn(0f, 1f)
					return Offset(0f, consumedY)
				}
				if (dy < 0f && expansion > 0f) {
					val consumedY = min(-dy, expansion * range)
					expansion = (expansion - consumedY / range).coerceIn(0f, 1f)
					return Offset(0f, -consumedY)
				}
				return Offset.Zero
			}

			override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
				if (!gestureEnabled) return Velocity.Zero
				val y = available.y
				if (y != 0f) {
					settle(y)
					return available
				}
				return Velocity.Zero
			}
		}
	}

	BoxWithConstraints(modifier = modifier.fillMaxSize()) {
		val totalPx = with(density) { maxHeight.toPx() }
		// 网格实际高度 = 各行行高之和 = weekRowPx + rangePx * (1 - foldProgress)，
		// 容器取同一数值即可严丝合缝，不留空白也不被裁掉
		val headerHeightPx = weekRowPx + rangePx * (1f - foldProgress)
		val contentHeightPx = (totalPx - weekBarPx - weekRowPx).coerceAtLeast(0f)

		Column(
				modifier = Modifier
					.fillMaxSize()
					.clipToBounds()
					.then(
							if (gestureEnabled && contentScrollable) {
								Modifier.nestedScroll(nestedScrollConnection)
							} else Modifier
					)
		) {
			if (config.showWeekBar) {
				ComposeCalendarWeekBar(
						state = state,
						config = config,
						modifier = Modifier
							.fillMaxWidth()
							.height(config.weekBarHeight),
				)
			}

			Box(
					modifier = Modifier
						.fillMaxWidth()
						.height(with(density) { headerHeightPx.toDp() })
						.clipToBounds()
						.then(
								if (gestureEnabled) {
							Modifier.pointerInput(true) {
								detectVerticalDragGestures(
										onDragStart = { childScrolled.value = false },
										onDragEnd = { settle(0f) },
										onDragCancel = { settle(0f) },
										onVerticalDrag = { change, dragAmount ->
											if (childScrolled.value) return@detectVerticalDragGestures
											val range = rangeState.value
											if (range <= 0f) return@detectVerticalDragGestures
											change.consume()
											expansion = (expansion + dragAmount / range).coerceIn(
													0f, 1f
											)
										},
								)
							}
						} else Modifier)) {
				// 收起动画：MonthGridView 内部逐行折叠选中行之外的行，选中行
				// 被逐行顶到周视图位置。折叠与淡出严格分两段：先折完再交接，
				// 否则选中行位于最后一行时，它要到展开度归零才就位，
				// 会在还在移动的过程中就被淡出。
				// 折叠目标取锚定日：点击时它已同步到选中日（即选中行），
				// 横向翻周时它跟随当前展示的周，保证展开后仍折叠到刚刚浏览的那一周
				val collapseAnchorDay = state.anchorDay
				if (weekViewAlpha < 1f) {
					ComposeCalendarMonthPagerInternal(
							state = state,
							config = config,
							pagerState = monthPagerState,
							foldProgress = foldProgress,
							anchorDay = collapseAnchorDay,
							modifier = Modifier
								.align(Alignment.TopStart)
								.fillMaxWidth()
								.height(with(density) { monthHeight.toDp() })
								.graphicsLayer { alpha = 1f - weekViewAlpha },
					)
				}
				if (weekViewAlpha > 0f) {
					ComposeCalendarWeekPagerInternal(
							state = state,
							config = config,
							pagerState = weekPagerState,
							modifier = Modifier
								.align(Alignment.TopStart)
								.fillMaxWidth()
								.height(with(density) { weekRowPx.toDp() })
								.graphicsLayer { alpha = weekViewAlpha },
					)
				}
			}

			Box(
					modifier = Modifier
						.fillMaxWidth()
						.height(with(density) { contentHeightPx.toDp() })
			) {
				content()
			}
		}
	}
}

/**
 * [ComposeCalendar] 的别名，语义上更接近旧版的 `CalendarLayout`。
 */
@Composable
fun ComposeCalendarLayout(
	state: ComposeCalendarState,
	modifier: Modifier = Modifier,
	config: ComposeCalendarConfig = state.config,
	contentScrollable: Boolean = true,
	content: @Composable () -> Unit = {},
) = ComposeCalendar(state, modifier, config, contentScrollable, content)

/**
 * 只渲染当前月份的月视图网格。
 */
@Composable
fun ComposeCalendarMonth(
	state: ComposeCalendarState,
	modifier: Modifier = Modifier,
	config: ComposeCalendarConfig = state.config,
) {
	MonthGridView(
			state = state,
			year = state.anchorDay.year,
			month = state.anchorDay.month,
			config = config,
			modifier = modifier,
	)
}

/**
 * 只渲染当前周的周视图网格。
 */
@Composable
fun ComposeCalendarWeek(
	state: ComposeCalendarState,
	modifier: Modifier = Modifier,
	config: ComposeCalendarConfig = state.config,
) {
	WeekGridView(
			state = state,
			firstDay = state.currentWeekDays.firstOrNull() ?: state.anchorDay,
			config = config,
			modifier = modifier,
	)
}

/**
 * 横向可滑动的月视图分页器。
 */
@Composable
fun ComposeCalendarMonthPager(
	state: ComposeCalendarState,
	modifier: Modifier = Modifier,
	config: ComposeCalendarConfig = state.config,
) {
	val pagerState = rememberPagerState(
			initialPage = state.monthIndexOf(state.anchorDay),
			pageCount = { state.monthCount },
	)
	LaunchedEffect(pagerState) {
		snapshotFlow { pagerState.settledPage }.distinctUntilChanged()
			.collect { page -> state.onMonthPageChanged(page) }
	}
	ComposeCalendarMonthPagerInternal(state, config, pagerState, modifier)
}

/**
 * 横向可滑动的周视图分页器。
 */
@Composable
fun ComposeCalendarWeekPager(
	state: ComposeCalendarState,
	modifier: Modifier = Modifier,
	config: ComposeCalendarConfig = state.config,
) {
	val pagerState = rememberPagerState(
			initialPage = state.weekIndexOf(state.anchorDay),
			pageCount = { state.weekCount },
	)
	LaunchedEffect(pagerState) {
		snapshotFlow { pagerState.settledPage }.distinctUntilChanged()
			.collect { page -> state.onWeekPageChanged(page) }
	}
	ComposeCalendarWeekPagerInternal(state, config, pagerState, modifier)
}

/**
 * 月视图分页器内部实现，复用外部的 [PagerState] 以便布局计算高度。
 */
@Composable
private fun ComposeCalendarMonthPagerInternal(
	state: ComposeCalendarState,
	config: ComposeCalendarConfig,
	pagerState: PagerState,
	modifier: Modifier = Modifier,
	foldProgress: Float = 0f,
	anchorDay: CalendarDay? = null,
) {
	HorizontalPager(
			state = pagerState,
			modifier = modifier,
			userScrollEnabled = config.isMonthPagerScrollable,
			key = { page ->
				val month = state.monthAt(page)
				"month-${month.year}-${month.month}"
			},
	) { page ->
		val month = state.monthAt(page)
		MonthGridView(
				state = state,
				year = month.year,
				month = month.month,
				config = config,
				modifier = Modifier.fillMaxSize(),
				foldProgress = foldProgress,
				anchorDay = anchorDay,
		)
	}
}

/**
 * 周视图分页器内部实现，复用外部的 [PagerState]。
 */
@Composable
private fun ComposeCalendarWeekPagerInternal(
	state: ComposeCalendarState,
	config: ComposeCalendarConfig,
	pagerState: PagerState,
	modifier: Modifier = Modifier,
) {
	HorizontalPager(
			state = pagerState,
			modifier = modifier,
			userScrollEnabled = config.isWeekPagerScrollable,
			key = { page -> "week-$page" },
	) { page ->
		WeekGridView(
				state = state,
				firstDay = state.weekAt(page),
				config = config,
				modifier = Modifier.fillMaxSize(),
		)
	}
}

/**
 * 当前月视图高度，分页滚动过程中在相邻月份高度之间线性插值。
 */
@Composable
private fun monthPagerHeight(
	state: ComposeCalendarState,
	config: ComposeCalendarConfig,
	pagerState: PagerState,
): Float {
	val density = LocalDensity.current
	val page = pagerState.currentPage
	val offset = pagerState.currentPageOffsetFraction
	val current = state.monthAt(page)
	val currentHeight = monthHeightPx(current.year, current.month, config, density)
	if (offset == 0f) return currentHeight
	val neighborIndex = (if (offset > 0f) page + 1 else page - 1).coerceIn(0, state.monthCount - 1)
	val neighbor = state.monthAt(neighborIndex)
	val neighborHeight = monthHeightPx(neighbor.year, neighbor.month, config, density)
	val fraction = abs(offset).coerceIn(0f, 1f)
	return currentHeight + (neighborHeight - currentHeight) * fraction
}
