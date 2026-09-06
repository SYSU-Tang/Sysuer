package com.miyuyan.preference

import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * 开关型偏好项。
 *
 * @param title 主标题文本。
 * @param checked 当前开关状态。
 * @param onCheckedChange 开关状态变化回调。
 * @param icon 左侧图标 drawable 资源 id(可选)。
 * @param summary 副标题(可选)。
 * @param enabled 是否启用。
 */
@Composable
fun SwitchPreference(
	title: String,
	checked: Boolean,
	onCheckedChange: (Boolean) -> Unit,
	modifier: Modifier = Modifier,
	enabled: Boolean = true,
	icon: Int? = null,
	summary: String? = null,
) {
	Preference(
		onClick = { onCheckedChange(!checked) },
		title = title,
		modifier = modifier,
		enabled = enabled,
		icon = icon,
		summary = summary,
		trailing = {
			Switch(
				checked = checked,
				onCheckedChange = onCheckedChange,
				enabled = enabled,
			)
		},
	)
}
