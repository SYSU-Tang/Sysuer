package com.miyuyan.preference

import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource

/**
 * 复选框型偏好项。
 *
 * 设计选择:左侧为 [Checkbox](复选框列表的常见模式,如系统设置中的多选项列表),
 * 主标题为 [title],尾部可附加一个装饰性 [trailingIcon],底部可显示 [summary]。
 *
 * @param title 主标题文本。
 * @param checked 当前选中状态。
 * @param onCheckedChange 选中状态变化回调。
 * @param trailingIcon 尾部的图标资源 id(可选)。
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
	Preference(
		onClick = { onCheckedChange(!checked) },
		title = title,
		modifier = modifier,
		enabled = enabled,
		summary = summary,
		icon = {
			Checkbox(
				checked = checked,
				onCheckedChange = onCheckedChange,
				enabled = enabled,
			)
		},
		trailing = if (trailingIcon != null) {
			{ Icon(painter = painterResource(trailingIcon), contentDescription = null) }
		} else null,
	)
}

/**
 * 自动管理选中状态的 [CheckBoxPreference] 重载:[initialValue] 作为初始选中态,
 * 点击时更新内部状态并经 [onCheckedChange] 通知父级。
 */
@Composable
fun CheckBoxPreference(
	title: String,
	modifier: Modifier = Modifier,
	enabled: Boolean = true,
	trailingIcon: Int? = null,
	summary: String? = null,
	initialValue: Boolean = false,
	onCheckedChange: ((Boolean) -> Unit)? = null,
) {
	var checked by rememberSaveable { mutableStateOf(initialValue) }
	CheckBoxPreference(
		title = title,
		checked = checked,
		onCheckedChange = {
			checked = it
			onCheckedChange?.invoke(it)
		},
		modifier = modifier,
		enabled = enabled,
		trailingIcon = trailingIcon,
		summary = summary,
	)
}
