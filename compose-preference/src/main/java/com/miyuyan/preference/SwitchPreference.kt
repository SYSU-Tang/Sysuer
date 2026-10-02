package com.miyuyan.preference

import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

@Composable
fun SwitchPreference(
	title: String,
	checked: Boolean,
	onCheckedChange: (Boolean) -> Unit,
	modifier: Modifier = Modifier,
	enabled: Boolean = true,
	icon: @Composable (() -> Unit)? = null,
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

/**
 * 自动管理开关状态的 [SwitchPreference] 重载:[initialValue] 作为初始状态,
 * 点击时更新内部状态并经 [onCheckedChange] 通知父级。
 */
@Composable
fun SwitchPreference(
	title: String,
	modifier: Modifier = Modifier,
	enabled: Boolean = true,
	icon: @Composable (() -> Unit)? = null,
	summary: String? = null,
	initialValue: Boolean = false,
	onCheckedChange: ((Boolean) -> Unit)? = null,
) {
	var checked by rememberSaveable { mutableStateOf(initialValue) }
	SwitchPreference(
		title = title,
		checked = checked,
		onCheckedChange = {
			checked = it
			onCheckedChange?.invoke(it)
		},
		modifier = modifier,
		enabled = enabled,
		icon = icon,
		summary = summary,
	)
}
