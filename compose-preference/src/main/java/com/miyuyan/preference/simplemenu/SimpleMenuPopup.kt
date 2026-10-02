package com.miyuyan.preference.simplemenu

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds

/**
 * android.view.animation.DecelerateInterpolator (factor = 1)。
 */
private val DecelerateEasing = Easing { 1f - (1f - it) * (1f - it) }

/**
 * android.view.animation.AccelerateInterpolator (factor = 1)。
 */
private val AccelerateEasing = Easing { it * it }

/** @android:interpolator/decelerate_cubic */
private val DecelerateCubicEasing = CubicBezierEasing(0.215f, 0.61f, 0.355f, 1f)

/** @android:interpolator/decelerate_quint */
private val DecelerateQuintEasing = CubicBezierEasing(0.22f, 0.61f, 0.36f, 1f)

/** 提前量，对应 shrink_fade_out_center 的 toYDelta = -8px。 */
private val ExitTranslationY = 8.dp

private data class SimpleMenuMeasure(val dialogMode: Boolean, val width: Float)

private class SimpleMenuPositionProvider(private val offset: IntOffset) : PopupPositionProvider {
	override fun calculatePosition(
		anchorBounds: IntRect,
		windowSize: IntSize,
		layoutDirection: LayoutDirection,
		popupContentSize: IntSize
	): IntOffset = offset
}

/**
 *
 * - 弹出模式：宽度按文本测量后向上取整为 56dp 的整数倍（竖屏最多 5 个单位，
 *   sw600dp 为 7 个），条目超高或含换行时回退为居中 Dialog 模式；
 * - 背景：4dp 圆角、锚点处小矩形展开（DecelerateInterpolator，时长按
 *   尺寸/4096 计算并夹在 150~300ms），阴影 elevation 同步过渡；
 * - 条目：48dp 最小高度、16sp 文本，以 30ms * |selectedIndex - i| 的延迟
 *   交错淡入并位移；选中项带 colorControlHighlight 底色；
 * - 退出：350ms 淡出（220ms 的 -8px 上移）。
 */
@Composable
internal fun SimpleMenuPopup(
	entries: List<String>,
	selectedIndex: Int? = null,
	anchorBounds: IntRect,
	containerColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
	shape: Shape = RoundedCornerShape(4.dp),
	alignToSelectedItem: Boolean = true,
	onDismiss: () -> Unit,
	onSelect: (Int) -> Unit,
) {
	val density = LocalDensity.current
	val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
	val isTablet = LocalConfiguration.current.screenWidthDp >= 600
	val windowSize = LocalWindowInfo.current.containerSize

	// 尺寸对应 Widget.Preference.SimpleMenuPreference.PopupMenu 与 dimens.xml
	val unitPx = with(density) { 56.dp.toPx() }
	val maxUnits = if (isTablet) 7 else 5
	val popupMarginH = with(density) { (if (isTablet) 23.dp else 15.dp).toPx() }
	val dialogMarginH = with(density) { 16.dp.toPx() }
	val dialogMarginV = with(density) { 24.dp.toPx() }
	val itemPaddingH = with(density) { 16.dp.toPx() }
	val itemHeight = with(density) { 48.dp.toPx() }
	val verticalPadding = with(density) { 8.dp.toPx() }
	val dialogMaxWidth = with(density) { 600.dp.toPx() }
	val popupElevation = with(density) { 8.dp.toPx() }
	val dialogElevation = with(density) { 24.dp.toPx() }

	// 位置、滚动与展开动画由打开时的选中项决定；点击选中后 selectedIndex 的变化只用于勾选高亮，
	// 否则菜单会在淡出时重新定位/滚动到新选中项。
	// null 或越界视为无选中项：不勾选任何条目，菜单显示在锚点正下方而不是把第 0 项当选中项对齐
	val openSelectedIndex = remember { selectedIndex?.takeIf { it in entries.indices } }
	val hasSelection = openSelectedIndex != null
	val index = openSelectedIndex ?: 0
	val checkedIndex = selectedIndex?.takeIf { it in entries.indices }
	val textStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp)
	val textMeasurer = rememberTextMeasurer()

	// 对应 SimpleMenuPopupWindow.measureWidth：文本测量 + 56dp 单位取整，超限回退 Dialog
	val measure = remember(
			entries,
			textMeasurer,
			unitPx,
			maxUnits,
			popupMarginH,
			itemPaddingH,
			windowSize.width,
			textStyle
	) {
		var maxTextWidth = 0f
		for (entry in entries) {
			val bounds = textMeasurer.measure(entry, textStyle, softWrap = false, maxLines = 1)
			if (bounds.size.width > maxTextWidth) {
				maxTextWidth = bounds.size.width.toFloat()
			}
		}
		val contentWidth = maxTextWidth + itemPaddingH * 2
		val limit = min(unitPx * maxUnits, windowSize.width - popupMarginH * 2)
		val dialogMode = contentWidth > limit || entries.any { it.contains('\n') }
		var width = 0f
		while (contentWidth > width) {
			width += unitPx
		}
		SimpleMenuMeasure(dialogMode, width)
	}

	val fullContentHeight = itemHeight * entries.size + verticalPadding * 2

	// 菜单距屏幕边缘的最小间距取 elevation 的一半；edge-to-edge 下窗口覆盖整块屏幕，
	// 可显示区域还要收缩到系统栏/刘海内侧，避免菜单延伸到状态栏、导航栏之下
	val edgeMargin = popupElevation / 2f
	val systemBars = WindowInsets.systemBars
	val displayCutout = WindowInsets.displayCutout
	val visibleTop =
		(maxOf(systemBars.getTop(density), displayCutout.getTop(density)) + edgeMargin).roundToInt()
	val visibleBottom = (windowSize.height - maxOf(
			systemBars.getBottom(density), displayCutout.getBottom(density)
	) - edgeMargin).roundToInt()
	val layoutDirection = LocalLayoutDirection.current
	val visibleLeft = maxOf(
			systemBars.getLeft(density, layoutDirection),
			displayCutout.getLeft(density, layoutDirection),
			edgeMargin.roundToInt()
	)
	val visibleRight = windowSize.width - maxOf(
			systemBars.getRight(density, layoutDirection),
			displayCutout.getRight(density, layoutDirection),
			edgeMargin.roundToInt()
	)

	var popupX = 0
	var popupY = 0
	var scrollOffset = 0f
	// 滚动模式下选中项顶边在视口中应处于的位置：先 scrollToItem 定位到选中项，再按该值
	// 回滚使条目中心与锚点中心对齐；无选中项时保持 0（列表停在顶部）
	var itemTargetY = 0f
	var scrollable = false
	val menuWidth: Int
	val menuHeight: Int
	if (measure.dialogMode) {
		menuWidth = min(dialogMaxWidth, windowSize.width - dialogMarginH * 2).roundToInt()
		menuHeight = min(fullContentHeight, windowSize.height - dialogMarginV * 2).roundToInt()
		if (menuHeight < fullContentHeight) {
			// 无选中项时从顶部显示，不把“第 0 项”当选中项居中
			scrollOffset = if (hasSelection) (menuHeight - itemHeight) / 2 else 0f
		}
	} else {
		menuWidth = measure.width.roundToInt()
		val visibleHeight = (visibleBottom - visibleTop).coerceAtLeast(0)
		scrollable = fullContentHeight > visibleHeight
		// 至少保留一个条目的高度，避免可见区域过小时出现非正高度
		menuHeight = (if (scrollable) visibleHeight else fullContentHeight.roundToInt())
			.coerceAtLeast(itemHeight.roundToInt())
		val anchorCenterY = anchorBounds.top + anchorBounds.height / 2f
		if (scrollable) {
			// 超高时窗口贴可显示区域顶部，并让选中条目对齐锚点（对应 showPopupMenu 的 scroll 逻辑）
			popupY = visibleTop
			if (hasSelection) itemTargetY = anchorCenterY - popupY - itemHeight / 2f
		} else {
			popupY = (if (alignToSelectedItem && hasSelection) {
				// 选中项顶边与锚点（preference 的 title）顶边对齐：
				// 选中项在窗口内的纵坐标 = popupY + verticalPadding + index * itemHeight
				anchorBounds.top - verticalPadding - index * itemHeight
			} else {
				// 标准下拉：菜单顶边贴锚点底边，条目上方只保留列表自身的 8dp padding
				anchorBounds.bottom.toFloat()
		}).coerceAtMost(visibleBottom - fullContentHeight)
			.coerceAtLeast(visibleTop.toFloat()).roundToInt()
		}
		// 菜单边缘对齐锚点，listItemPadding 只作用于条目内部文字留白，不参与窗口定位
		popupX = ((if (rtl) anchorBounds.right - menuWidth else anchorBounds.left).toFloat())
			.coerceAtMost((visibleRight - menuWidth).toFloat())
			.coerceAtLeast(visibleLeft.toFloat()).roundToInt()
	}

	// 对应 SimpleMenuAnimation.startEnterAnimation：展开起点矩形与时长
	val centerX = if (measure.dialogMode) menuWidth / 2f
	else if (rtl) menuWidth.toFloat() else 0f
	// 展开动画起点：有选中项时取选中项中心，无选中项时从菜单顶边（紧贴锚点下方）展开
	val centerY =
		if (measure.dialogMode) {
			if (hasSelection)
				(verticalPadding + index * itemHeight + itemHeight * 0.5f - scrollOffset).coerceIn(
						0f, menuHeight.toFloat()
				)
			else 0f
		} else if (hasSelection) {
			(itemTargetY + itemHeight / 2f).coerceIn(0f, menuHeight.toFloat())
		} else 0f
	val startRect = if (measure.dialogMode) {
		Rect(centerX, menuHeight / 2f, centerX, menuHeight / 2f)
	} else if (rtl) {
		Rect(
				menuWidth - unitPx,
				centerY - itemHeight * 0.2f,
				menuWidth.toFloat(),
				centerY + itemHeight * 0.2f
		)
	} else {
		Rect(0f, centerY - itemHeight * 0.2f, unitPx, centerY + itemHeight * 0.2f)
	}
	val endRect = Rect(0f, 0f, menuWidth.toFloat(), menuHeight.toFloat())
	// centerX 使用窗口内坐标（popup 模式为菜单左边缘 0），对应修复后的 startEnterAnimation
	val endWidth = if (measure.dialogMode) menuWidth / 2f else menuWidth.toFloat()
	val endHeight = max(centerY, menuHeight - centerY)
	val boundsDuration = (max(endWidth, endHeight) / 4096f * 1000).roundToInt().coerceIn(150, 300)

	val baseElevation = if (measure.dialogMode) dialogElevation else popupElevation

	val bounds = remember { Animatable(startRect, Rect.VectorConverter) }
	val elevation = remember { Animatable(baseElevation * 0.75f) }
	val itemAlphas = remember { List(entries.size) { Animatable(0f) } }
	val itemTranslations = remember {
		entries.indices.map { i ->
			val offset = index - i
			Animatable(if (offset == 0) 0f else itemHeight * 0.2f * (if (offset < 0) -1f else 1f))
		}
	}

	LaunchedEffect(Unit) {
		launch {
			bounds.animateTo(endRect, tween(boundsDuration, easing = DecelerateEasing))
		}
		launch {
			elevation.animateTo(baseElevation, tween(boundsDuration, easing = FastOutSlowInEasing))
		}
		// 条目交错动画：延迟 30ms * |selectedIndex - i|
		entries.indices.forEach { i ->
			launch {
				delay((30L * abs(index - i)).milliseconds)
				launch {
					itemAlphas[i].animateTo(1f, tween(200, easing = AccelerateEasing))
				}
				launch {
					itemTranslations[i].animateTo(0f, tween(275, easing = DecelerateEasing))
				}
			}
		}
	}

	val listState = rememberLazyListState()
	LaunchedEffect(menuHeight, index) {
		if (scrollable && hasSelection) {
			// scrollToItem 只能把条目定位到视口顶部，先定位到选中项，再按 itemTargetY 回滚，
			// 让选中项中心与锚点中心对齐；超出可滚动范围时由列表自行钳制到首尾
			listState.scrollToItem(index)
			listState.scrollBy(-itemTargetY)
		}
	}

	// 对应 Animation.Preference.SimpleMenuCenter 的 windowExitAnimation：shrink_fade_out_center
	var dismissing by remember { mutableStateOf(false) }
	val exitAlpha = remember { Animatable(1f) }
	val exitTranslationY = remember { Animatable(0f) }
	LaunchedEffect(dismissing) {
		if (!dismissing) {
			return@LaunchedEffect
		}
		coroutineScope {
			launch { exitAlpha.animateTo(0f, tween(350, easing = DecelerateCubicEasing)) }
			launch {
				exitTranslationY.animateTo(
						-with(density) { ExitTranslationY.toPx() },
						tween(220, easing = DecelerateQuintEasing)
				)
			}
		}
		onDismiss()
	}
	val requestDismiss = {
		if (!dismissing) {
			dismissing = true
		}
	}
	val onItemSelect: (Int) -> Unit = { i ->
		if (!dismissing) {
			onSelect(i)
			dismissing = true
		}
	}

	val itemHorizontalPadding = if (measure.dialogMode) 24.dp else 16.dp
	val itemMaxLines = if (measure.dialogMode) Int.MAX_VALUE else 1

	val content: @Composable () -> Unit = {
		Box(
				Modifier.graphicsLayer {
					alpha = exitAlpha.value
					translationY = exitTranslationY.value
				}) {
			SimpleMenuSurface(
					menuWidth = menuWidth,
					menuHeight = menuHeight,
					bounds = bounds,
					elevation = elevation,
					containerColor = containerColor,
					shape = shape,
					listState = listState,
					entries = entries,
					// -1 表示无选中项，条目的 checked 判断不会匹配任何位置；
					// 传实时值让点击后的高亮立即跟随新选中项
					selectedIndex = checkedIndex ?: -1,
					itemPaddingH = itemHorizontalPadding,
					maxLines = itemMaxLines,
					itemAlphas = itemAlphas,
					itemTranslations = itemTranslations,
					onSelect = onItemSelect,
			)
		}
	}

	if (measure.dialogMode) {
		Dialog(
				onDismissRequest = requestDismiss,
				properties = DialogProperties(usePlatformDefaultWidth = false),
		) {
			Box(
					Modifier
						.fillMaxSize()
						.wrapContentSize(Alignment.Center)
			) {
				content()
			}
		}
	} else {
		Popup(
				popupPositionProvider = remember(popupX, popupY) {
					SimpleMenuPositionProvider(IntOffset(popupX, popupY))
				},
				onDismissRequest = requestDismiss,
				// clippingEnabled = false：禁止框架把弹窗钳制到弹出时刻的可视 frame。
				// 否则在输入法收起动画期间打开时，弹窗会被钳进"状态栏 ↔ 输入法顶部"的
				// 旧 frame，被顶部状态栏和底部原输入法区域截断，阴影不可见且对齐错位。
				// 窗口本身是 edge-to-edge 全屏，几何已按系统栏内缩，无需框架再调整。
				properties = PopupProperties(focusable = true, clippingEnabled = false),
		) {
			content()
		}
	}
}

@Composable
private fun SimpleMenuSurface(
	menuWidth: Int,
	menuHeight: Int,
	bounds: Animatable<Rect, *>,
	elevation: Animatable<Float, *>,
	containerColor: Color,
	shape: Shape,
	listState: LazyListState,
	entries: List<String>,
	selectedIndex: Int,
	itemPaddingH: Dp,
	maxLines: Int,
	itemAlphas: List<Animatable<Float, *>>,
	itemTranslations: List<Animatable<Float, *>>,
	onSelect: (Int) -> Unit,
) {
	val density = LocalDensity.current
	Box(
			Modifier.size(
					with(density) { menuWidth.toFloat().toDp() },
					with(density) { menuHeight.toFloat().toDp() })
	) {
		// 背景与阴影跟随展开矩形（对应 CustomBoundsDrawable + clipToOutline）
		Box(Modifier
			.offset {
				IntOffset(bounds.value.left.roundToInt(), bounds.value.top.roundToInt())
			}
			.revealSize(bounds)
			.graphicsLayer {
				shadowElevation = elevation.value
				this.shape = shape
			}
			.background(containerColor, shape))
		LazyColumn(
				state = listState,
				contentPadding = PaddingValues(vertical = 8.dp),
				modifier = Modifier
					.matchParentSize()
					.drawWithContent {
						val rect = bounds.value
						clipRect(rect.left, rect.top, rect.right, rect.bottom) {
							this@drawWithContent.drawContent()
						}
					}
					.clip(shape)) {
			itemsIndexed(entries) { i, entry ->
				SimpleMenuItem(
						entry = entry,
						checked = i == selectedIndex,
						alpha = itemAlphas[i],
						translationY = itemTranslations[i],
						paddingH = itemPaddingH,
						maxLines = maxLines,
						onClick = { onSelect(i) },
				)
			}
		}
	}
}

/** 让背景 Box 尺寸直接跟随展开矩形，阴影 outline 同步变化。 */
private fun Modifier.revealSize(bounds: Animatable<Rect, *>) = layout { measurable, _ ->
	val rect = bounds.value
	val placeable = measurable.measure(
			Constraints.fixed(
					rect.width.roundToInt().coerceAtLeast(0),
					rect.height.roundToInt().coerceAtLeast(0)
			)
	)
	layout(placeable.width, placeable.height) { placeable.place(0, 0) }
}

@Composable
private fun SimpleMenuItem(
	entry: String,
	checked: Boolean,
	alpha: Animatable<Float, *>,
	translationY: Animatable<Float, *>,
	paddingH: Dp,
	maxLines: Int,
	onClick: () -> Unit,
) {
	// 对应 simple_menu_item_background：state_checked 时为 colorControlHighlight
	val checkedColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
	Box(Modifier
		.fillMaxWidth()
		.heightIn(min = 48.dp)
		.graphicsLayer {
			this.alpha = alpha.value
			this.translationY = translationY.value
		}
		.background(if (checked) checkedColor else Color.Transparent)
		.selectable(selected = checked, onClick = onClick)
		.padding(horizontal = paddingH, vertical = 8.dp)) {
		Text(
				entry,
				modifier = Modifier.align(Alignment.CenterStart),
				color = MaterialTheme.colorScheme.onSurface,
				style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
				maxLines = maxLines,
				overflow = TextOverflow.Clip,
		)
	}
}
