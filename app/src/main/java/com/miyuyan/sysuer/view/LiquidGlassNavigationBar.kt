package com.miyuyan.sysuer.view

import android.os.Build
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollDispatcher
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.highlight.Highlight
import top.yukonga.miuix.kmp.blur.textureBlur
import top.yukonga.miuix.kmp.squircle.squircleClip
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin


private fun buildSquirclePath(
    center: Offset,
    halfWidth: Float,
    halfHeight: Float,
    cornerRadius: Float,
    n: Float = 2.4f,
    cornerSegments: Int = 10,
): Path {
    val r = cornerRadius.coerceAtMost(minOf(halfWidth, halfHeight))
    val left = center.x - halfWidth
    val right = center.x + halfWidth
    val top = center.y - halfHeight
    val bottom = center.y + halfHeight

    fun cornerPoint(cx: Float, cy: Float, sx: Float, sy: Float, t: Float): Offset {
        val theta = t * (PI.toFloat() / 2f)
        val c = cos(theta).coerceAtLeast(0f)
        val s = sin(theta).coerceAtLeast(0f)
        val x = cx + sx * r * c.pow(2f / n)
        val y = cy + sy * r * s.pow(2f / n)
        return Offset(x, y)
    }

    val path = Path()
    path.moveTo(left + r, top)
    path.lineTo(right - r, top)
    // 右上角: 从"顶边"过渡到"右边"
    for (i in 0..cornerSegments) {
        val t = i / cornerSegments.toFloat()
        val p = cornerPoint(right - r, top + r, 1f, -1f, 1f - t)
        path.lineTo(p.x, p.y)
    }
    path.lineTo(right, bottom - r)
    // 右下角
    for (i in 0..cornerSegments) {
        val t = i / cornerSegments.toFloat()
        val p = cornerPoint(right - r, bottom - r, 1f, 1f, t)
        path.lineTo(p.x, p.y)
    }
    path.lineTo(left + r, bottom)
    // 左下角
    for (i in 0..cornerSegments) {
        val t = i / cornerSegments.toFloat()
        val p = cornerPoint(left + r, bottom - r, -1f, 1f, 1f - t)
        path.lineTo(p.x, p.y)
    }
    path.lineTo(left, top + r)
    // 左上角
    for (i in 0..cornerSegments) {
        val t = i / cornerSegments.toFloat()
        val p = cornerPoint(left + r, top + r, -1f, -1f, t)
        path.lineTo(p.x, p.y)
    }
    path.close()
    return path
}
private data class BlobGeometry(
    val centerX: Float,
    val centerY: Float,
    val halfWidth: Float,
    val halfHeight: Float,
)

private fun computeBlobGeometry(
    pageOffset: Float,
    itemWidthPx: Float,
    itemCount: Int,
    radiusPx: Float,
    centerY: Float,
): BlobGeometry {
    val currentPage = pageOffset.toInt().coerceIn(0, itemCount - 1)
    val rawFraction = (pageOffset - currentPage).coerceIn(0f, 1f)
    val hasNext = currentPage < itemCount - 1

    val startCx = itemWidthPx * currentPage + itemWidthPx / 2f
    val endCx = if (hasNext) itemWidthPx * (currentPage + 1) + itemWidthPx / 2f else startCx
    val t = if (hasNext) rawFraction else 0f

    val growth = sin(t * PI.toFloat()).coerceIn(0f, 1f)
    val centerX = startCx + (endCx - startCx) * t
    val distance = abs(endCx - startCx)
    val halfWidth = radiusPx + growth * distance / 2f

    return BlobGeometry(
        centerX = centerX,
        centerY = centerY,
        halfWidth = halfWidth,
        halfHeight = radiusPx,
    )
}

@Composable
fun LiquidGlassNavBar(
    pagerState: PagerState,
    items: List<MenuItem>,
    backdrop: LayerBackdrop,
    onItemClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    isDark: Boolean = isSystemInDarkTheme(),
) {
    val itemCount = items.size
    val pageOffset = pagerState.currentPage + pagerState.currentPageOffsetFraction
	val width = itemCount * 75.dp.coerceIn(75.dp, 450.dp)
    val barHeight = 64.dp
    val cornerRadius = 36.dp

    val indicatorColor = MaterialTheme.colorScheme.surfaceContainerHigh

    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    var barWidthPx by remember { mutableFloatStateOf(0f) }
    val itemWidthPx = if (itemCount > 0 && barWidthPx > 0) barWidthPx / itemCount else 0f

    var isDragging by remember { mutableStateOf(false) }
    val dragBoost by animateFloatAsState(
        targetValue = if (isDragging) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 380f),
        label = "dragBoost"
    )
    val barScale by animateFloatAsState(
        targetValue = if (isDragging) 1.03f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 380f),
        label = "barScale"
    )

    val nestedScrollDispatcher = remember { NestedScrollDispatcher() }
    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                return if (isDragging) Offset(available.x, 0f) else Offset.Zero
            }
        }
    }

    Box(
        modifier = modifier
	        .padding(bottom = 24.dp)
	        .height(barHeight)
	        .width(width)
	        .nestedScroll(connection = nestedScrollConnection, dispatcher = nestedScrollDispatcher)
    ) {
        Box(
            modifier = Modifier
	            .matchParentSize()
	            .graphicsLayer { scaleX = barScale; scaleY = barScale }
	            .squircleClip(cornerRadius)
	            .textureBlur(
		            backdrop = backdrop,
		            shape = RoundedCornerShape(cornerRadius),
		            blurRadius = 36f,
		            highlight = if (isDark) Highlight.GlassStrokeMiddleDark else Highlight.GlassStrokeMiddleLight,
	                        )
	            .onGloballyPositioned { barWidthPx = it.size.width.toFloat() }
	            .pointerInput(itemCount, itemWidthPx) {
		            if (itemWidthPx <= 0f) return@pointerInput
		            val radiusPx = 27.dp.toPx()
		            val touchSlopPx = 18.dp.toPx()
		            var dragAccepted = false
		            
		            detectDragGesturesAfterLongPress(onDragStart = { offset ->
			            val curOffset = pagerState.currentPage + pagerState.currentPageOffsetFraction
			            val blobCx = itemWidthPx * curOffset + itemWidthPx / 2f
			            val blobCy = size.height / 2f
			            val dx = offset.x - blobCx
			            val dy = offset.y - blobCy
			            val hit = radiusPx + touchSlopPx
			            val withinBlob = (dx * dx + dy * dy) <= hit * hit
			            
			            if (withinBlob) {
				            dragAccepted = true
				            isDragging = true
				            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
			            }
		            }, onDrag = { change, dragAmount ->
			            if (!dragAccepted) return@detectDragGesturesAfterLongPress
			            change.consume()
			            val pageSizePx = pagerState.layoutInfo.pageSize.takeIf { it > 0 } ?: return@detectDragGesturesAfterLongPress
			            val scale = pageSizePx / itemWidthPx
			            val desired = Offset(dragAmount.x, 0f)
			            val preConsumed = nestedScrollDispatcher.dispatchPreScroll(desired, NestedScrollSource.UserInput)
			            val remaining = desired - preConsumed
			            
			            if (remaining.x != 0f) {
				            scope.launch {
					            pagerState.scroll(MutatePriority.UserInput) {
						            scrollBy(remaining.x * scale)
					            }
					            nestedScrollDispatcher.dispatchPostScroll(consumed = remaining, available = Offset.Zero, source = NestedScrollSource.UserInput)
				            }
			            }
		            }, onDragEnd = {
			            isDragging = false
			            if (dragAccepted) {
				            scope.launch {
					            val current = pagerState.currentPage + pagerState.currentPageOffsetFraction
					            val nearest = current.roundToInt().coerceIn(0, itemCount - 1)
					            pagerState.animateScrollToPage(page = nearest, animationSpec = spring(dampingRatio = 0.75f, stiffness = 300f))
				            }
			            }
			            dragAccepted = false
		            }, onDragCancel = {
			            isDragging = false
			            if (dragAccepted) {
				            scope.launch {
					            val current = pagerState.currentPage + pagerState.currentPageOffsetFraction
					            val nearest = current.roundToInt().coerceIn(0, itemCount - 1)
					            pagerState.animateScrollToPage(nearest)
				            }
			            }
			            dragAccepted = false
		            })
	            }
        ) {
            IndicatorGlow(
                pageOffset = pageOffset,
                itemWidthPx = itemWidthPx,
                itemCount = itemCount,
                dragBoost = dragBoost,
                barHeight = barHeight,
                density = density,
                glowColor = indicatorColor,
            )

            LiquidIndicator(
                pageOffset = pageOffset,
                itemWidthPx = itemWidthPx,
                itemCount = itemCount,
                dragBoost = dragBoost,
                indicatorColor = indicatorColor,
                modifier = Modifier.matchParentSize()
            )

            Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                items.forEachIndexed { index, item ->
                    val selected = 1f - abs(pageOffset - index).coerceIn(0f, 1f)
                    val tint = lerp(
                        MaterialTheme.colorScheme.onSurfaceVariant,
                        MaterialTheme.colorScheme.primary,
                        selected
                    )
                    Column(
                        modifier = Modifier
	                        .weight(1f)
	                        .fillMaxHeight()
	                        .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { onItemClick(index) },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        item.icon?.let {
                            Icon(
                                imageVector = it,
                                contentDescription = item.title,
                                tint = tint,
                                modifier = Modifier
	                                .size(20.87.dp)
	                                .graphicsLayer {
		                                val scale = 1f + 0.15f * selected
		                                scaleX = scale
		                                scaleY = scale
		                                translationY = -3.dp.toPx() * selected
	                                }
                            )
                        }
	                    if (  item.icon != null   &&  item.title != null)
                        Spacer(Modifier.height(2.dp))
                        item.title?.let {
                            Text(
                                text = it,
                                color = tint,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (selected > 0.5f) FontWeight.SemiBold else FontWeight.Normal,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}
@Composable
private fun LiquidIndicator(
    pageOffset: Float,
    itemWidthPx: Float,
    itemCount: Int,
    dragBoost: Float,
    indicatorColor: Color,
    modifier: Modifier = Modifier,
) {
    if (itemWidthPx <= 0f) return
    Canvas(modifier = modifier) {
        val baseRadius = 28.dp.toPx()
        val radius = baseRadius * (1f + 0.22f * dragBoost)
        val centerY = size.height / 2f
        val g = computeBlobGeometry(pageOffset, itemWidthPx, itemCount, radius, centerY)
        val center = Offset(g.centerX, g.centerY)
        val cornerRadius = radius * 1.9f // squircle 圆角相对半径的比例,可调

        val path = buildSquirclePath(
            center = center,
            halfWidth = g.halfWidth,
            halfHeight = g.halfHeight,
            cornerRadius = cornerRadius,
        )
        drawPath(
            path = path,
            color = indicatorColor.copy(alpha = 0.42f + 0.22f * dragBoost)
        )

        if (dragBoost > 0.01f) {
            clipPath(path) {
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.28f * dragBoost),
                            Color.Transparent
                        ),
                        center = Offset(
                            g.centerX - g.halfWidth * 0.35f,
                            g.centerY - g.halfHeight * 0.4f
                        ),
                        radius = (g.halfWidth.coerceAtLeast(g.halfHeight)) * 1.1f
                    ),
                    topLeft = Offset(g.centerX - g.halfWidth, g.centerY - g.halfHeight),
                    size = Size(g.halfWidth * 2f, g.halfHeight * 2f)
                )
            }
        }
    }
}

@Composable
private fun IndicatorGlow(
    pageOffset: Float,
    itemWidthPx: Float,
    itemCount: Int,
    dragBoost: Float,
    barHeight: Dp,
    density: Density,
    glowColor: Color,
) {
    if (itemWidthPx <= 0f || Build.VERSION.SDK_INT < 31 || dragBoost <= 0.01f) return

    val baseRadiusPx = with(density) { 27.dp.toPx() }
    val radiusPx = baseRadiusPx * (1f + 0.22f * dragBoost)
    val centerYPx = with(density) { (barHeight / 2).toPx() }
    val g = computeBlobGeometry(pageOffset, itemWidthPx, itemCount, radiusPx, centerYPx)

    val bleed = 12.dp
    val bleedPx = with(density) { bleed.toPx() }
    val leftDp = with(density) { (g.centerX - g.halfWidth).toDp() } - bleed
    val topDp = with(density) { (g.centerY - g.halfHeight).toDp() } - bleed
    val widthDp = with(density) { (g.halfWidth * 2f).toDp() } + bleed * 2
    val heightDp = with(density) { (g.halfHeight * 2f).toDp() } + bleed * 2

    Canvas(
        modifier = Modifier
	        .offset(x = leftDp, y = topDp)
	        .size(widthDp, heightDp)
	        .blur(radius = 10.dp + 8.dp * dragBoost, edgeTreatment = BlurredEdgeTreatment.Unbounded)
    ) {
        val center = Offset(bleedPx + g.halfWidth, bleedPx + g.halfHeight)
        val path = buildSquirclePath(
            center = center,
            halfWidth = g.halfWidth,
            halfHeight = g.halfHeight,
            cornerRadius = g.halfHeight * 0.9f,
        )
        drawPath(
            path = path,
            color = glowColor.copy(alpha = 0.18f + 0.22f * dragBoost),
            style = Stroke(width = with(density) { (4.dp + 2.dp * dragBoost).toPx() })
        )
    }
}