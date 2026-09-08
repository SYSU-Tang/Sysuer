package com.miyuyan.preference

import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
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