package com.miyuyan.preference

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.compose.ui.unit.dp

/**
 * 偏好设置屏幕容器。
 *
 * 提供统一的滚动布局 + 分组标题样式,内部使用 [PreferenceCategory] 包裹具体的偏好项。
 *
 * 用法:
 * ```
 * PreferenceScreen(title = "设置") {
 *     PreferenceCategory(title = "通用") {
 *         SwitchPreference(title = "通知", checked = true, onCheckedChange = { })
 *         JumpPreference(key = R.string.about, icon = R.drawable.info)
 *     }
 * }
 * ```
 *
 * @param title 屏幕顶部大标题(可选,传 null 则不显示)。
 * @param content 内容区域,由若干 [PreferenceCategory] 组成。
 */
@Composable
fun PreferenceScreen(
	modifier: Modifier = Modifier,
	title: String? = null,
	content: @Composable ColumnScope.() -> Unit,
) {
	Column(
		modifier = modifier
			.fillMaxSize()
			.nestedScroll(rememberNestedScrollInteropConnection())
			.verticalScroll(rememberScrollState())
			.padding(horizontal = 16.dp, vertical = 8.dp),
		verticalArrangement = Arrangement.spacedBy(16.dp),
	) {
		if (title != null) {
			Text(
				text = title,
				style = MaterialTheme.typography.titleLarge,
				color = MaterialTheme.colorScheme.onSurface,
				modifier = Modifier.padding(vertical = 8.dp),
			)
		}
		content()
	}
}

/**
 * 偏好设置分组。
 *
 * 渲染一个分组标题,并在内部为每个 [Preference] 子项自动设置 [LocalPreferenceIndex] / [LocalPreferenceCount],
 * 让圆角无缝衔接 —— 使用方式为 `items` 接收一个 vararg 风格的列表。
 *
 * 用法:
 * ```
 * PreferenceCategory(title = "通用") {
 *     SwitchPreference(...)
 *     JumpPreference(...)
 *     IconTextPreference(...)
 * }
 * ```
 *
 * @param title 分组标题(显示在卡片上方)。
 * @param items 该分组下的偏好项。建议传入 1 ~ N 个 [Preference] / [SwitchPreference] /
 *              [CheckBoxPreference] / [IconTextPreference] / [JumpPreference] 子项。
 */
@Composable
fun PreferenceCategory(
	title: String,
	modifier: Modifier = Modifier,
	items: @Composable ColumnScope.() -> Unit,
) {
	Column(modifier = modifier.fillMaxWidth()) {
		Text(
			text = title,
			style = MaterialTheme.typography.titleSmall,
			color = MaterialTheme.colorScheme.primary,
			modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
		)
		Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
			items()
		}
	}
}
