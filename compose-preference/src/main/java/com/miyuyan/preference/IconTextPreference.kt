package com.miyuyan.preference

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * 不可点击的图标 + 文本展示项,适用于信息展示(无 onClick)。
 *
 * @param title 主标题。
 * @param icon 左侧图标 drawable 资源 id。
 * @param summary 副标题(可选)。
 */
@Composable
fun IconTextPreference(
	title: String,
	icon: Int,
	modifier: Modifier = Modifier,
	summary: String? = null,
) {
	Preference(
		onClick = null,
		title = title,
		modifier = modifier,
		icon = icon,
		summary = summary,
	)
}
