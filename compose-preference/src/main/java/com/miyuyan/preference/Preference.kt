package com.miyuyan.preference

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

val LocalPreferenceIndex = compositionLocalOf { 0 }
val LocalPreferenceCount = compositionLocalOf { 1 }

/**
 * 单个偏好项的容器 Composable。
 *
 * 内部使用 Material3 的 [SegmentedListItem],并自动从 [LocalPreferenceIndex]/[LocalPreferenceCount]
 * 读取位置信息,使同一分组内的多个 [Preference] 圆角无缝衔接。
 *
 * @param onClick 点击回调。传 `null` 时表示只读展示。
 * @param title 主标题文本。
 * @param summary 副标题文本(显示在标题下方),可为 null。
 * @param icon 左侧图标 Composable。为 null 时不显示图标。
 *             推荐配合 `Icon(painterResource(...), contentDescription)` 使用。
 * @param trailing 标题右侧的尾部 Composable(例如 Switch、Checkbox、文字摘要)。
 *                 与 [summary] 同时存在时,优先显示 [summary] 在下方。
 * @param content 标题下方的自定义内容区 Composable(例如 Slider、ChipGroup、进度条)。
 *                传非 null 值时优先于 [summary] 显示,标题移至上方 overline 位置。
 */
@Composable
fun Preference(
	modifier: Modifier = Modifier,
	onClick: (() -> Unit)? = null,
	title: String,
	enabled: Boolean = true,
	icon: @Composable (() -> Unit)? = null,
	trailing: (@Composable () -> Unit)? = null,
	summary: String? = null,
	titleModifier: Modifier = Modifier,
	content: (@Composable () -> Unit)? = null,
) {
	val index = LocalPreferenceIndex.current
	val count = LocalPreferenceCount.current
	val colors =
		ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
	SegmentedListItem(
			onClick = onClick ?: {},
			enabled = enabled,
			verticalAlignment = Alignment.CenterVertically,
			modifier = modifier.fillMaxWidth(),
			shapes = ListItemDefaults.segmentedShapes(index, count),
			colors = colors,
			leadingContent = {
				if (icon != null) {
					icon()
				}
			},
			overlineContent = if (summary != null || content != null) {
				{
					Text(
							modifier = titleModifier,
							text = title,
//							color = colors.overlineContentColor,
							style = MaterialTheme.typography.bodyLarge,
					)
				}
			} else null,
			trailingContent = trailing) {
		when {
			content != null -> content()
			summary != null -> {
				Text(
						summary,
						style = MaterialTheme.typography.labelMedium,
//						color = colors.supportingContentColor
				)
			}

			else -> {
				Text(
						modifier = titleModifier,
						text = title,
						style = MaterialTheme.typography.bodyLarge,
//						color = colors.overlineContentColor
				)
			}
		}
	}
}