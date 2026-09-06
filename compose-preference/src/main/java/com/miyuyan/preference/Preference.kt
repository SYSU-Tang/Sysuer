package com.miyuyan.preference

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource

/**
 * 由 [PreferenceCategory] 设置,告知其中的 [Preference] 自己在分组中的索引。
 * 这样调用方无需手动传入 `index`/`count`。
 */
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
 * @param icon 左侧图标 drawable 资源 id。为 null 时不显示图标。
 * @param trailing 标题右侧的尾部 Composable(例如 Switch、Checkbox、文字摘要)。
 *                 与 [summary] 同时存在时,优先显示 [summary] 在下方。
 */
@Composable
fun Preference(
	onClick: (() -> Unit)?,
	title: String,
	modifier: Modifier = Modifier,
	enabled: Boolean = true,
	icon: Int? = null,
	trailing: (@Composable () -> Unit)? = null,
	summary: String? = null,
) {
	val index = LocalPreferenceIndex.current
	val count = LocalPreferenceCount.current
	SegmentedListItem(
		onClick = onClick ?: {},
		enabled = enabled,
		verticalAlignment = Alignment.CenterVertically,
		modifier = modifier.fillMaxWidth(),
		shapes = ListItemDefaults.segmentedShapes(index, count),
		colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
		leadingContent = {
			if (icon != null) {
				Icon(painter = painterResource(icon), contentDescription = title)
			}
		},
		overlineContent = {
			Row(verticalAlignment = Alignment.CenterVertically) {
				Text(title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
				trailing?.invoke()
			}
		},
		supportingContent = if (summary != null) {
			{ Text(summary, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
		} else null,
	) {}
}
