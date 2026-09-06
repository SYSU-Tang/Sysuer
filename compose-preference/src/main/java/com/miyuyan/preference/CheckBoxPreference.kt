package com.miyuyan.preference

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource

/**
 * 复选框型偏好项。
 *
 * 设计选择:左侧为 [Checkbox](复选框列表的常见模式,如系统设置中的多选项列表),
 * 主标题为 [title],文本右侧可附加一个装饰性 [trailingIcon],底部可显示 [summary]。
 *
 * @param title 主标题文本。
 * @param checked 当前选中状态。
 * @param onCheckedChange 选中状态变化回调。
 * @param trailingIcon 标题右侧的图标资源 id(可选)。
 * @param summary 副标题(可选)。
 * @param enabled 是否启用。
 */
@Composable
fun CheckBoxPreference(
	title: String,
	checked: Boolean,
	onCheckedChange: (Boolean) -> Unit,
	modifier: Modifier = Modifier,
	enabled: Boolean = true,
	trailingIcon: Int? = null,
	summary: String? = null,
) {
	val index = LocalPreferenceIndex.current
	val count = LocalPreferenceCount.current
	SegmentedListItem(
		onClick = { onCheckedChange(!checked) },
		enabled = enabled,
		verticalAlignment = Alignment.CenterVertically,
		modifier = modifier.fillMaxWidth(),
		shapes = ListItemDefaults.segmentedShapes(index, count),
		colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
		leadingContent = {
			Checkbox(
				checked = checked,
				onCheckedChange = onCheckedChange,
				enabled = enabled,
			)
		},
		overlineContent = {
			Row(verticalAlignment = Alignment.CenterVertically) {
				Text(title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
				if (trailingIcon != null) {
					Icon(painter = painterResource(trailingIcon), contentDescription = null)
				}
			}
		},
		supportingContent = if (summary != null) {
			{ Text(summary, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
		} else null,
	) {}
}
