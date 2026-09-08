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
import androidx.compose.runtime.CompositionLocalProvider
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
 * [PreferenceCategory] 的作用域,提供 [item] 函数来收集偏好项。
 *
 * 设计说明:[Preference] 需要知道自己在分组中的 index / count 才能正确拼接圆角。
 * 但 Compose 的 composable lambda 是声明式的,调用方体内可以放任意多的子项,运行前无法预知数量。
 * 因此这里借鉴 LazyListScope 的模式:先通过 [item] 把所有子项收集到列表中,
 * 再由 [PreferenceCategory] 统一用 CompositionLocalProvider 注入 index/count 进行渲染。
 */
class PreferenceCategoryScope internal constructor() {
	internal val collectedItems = mutableListOf<@Composable () -> Unit>()

	/**
	 * 向分组中添加一个偏好项。
	 *
	 * 被包裹的 content 不会在这里立即渲染,而是先收集,等 [PreferenceCategory] 知道总数后,
	 * 再带着正确的 index/count 通过 CompositionLocalProvider 统一渲染。
	 */
	fun item(content: @Composable () -> Unit) {
		collectedItems.add(content)
	}
}

/**
 * 偏好设置分组。
 *
 * 渲染一个分组标题,并在内部为每个通过 [PreferenceCategoryScope.item] 注册的子项
 * 自动设置 [LocalPreferenceIndex] / [LocalPreferenceCount],让圆角无缝衔接。
 *
 * 用法:
 * ```
 * PreferenceCategory(title = "通用") {
 *     item { SwitchPreference(title = "通知", checked = true, onCheckedChange = { }) }
 *     item { JumpPreference(key = R.string.about, icon = R.drawable.info) }
 *     item { IconTextPreference(title = "版本", icon = R.drawable.info, summary = "1.0.0") }
 * }
 * ```
 *
 * @param title 分组标题(显示在卡片上方)。
 * @param items 该分组下的偏好项,通过 [PreferenceCategoryScope.item] 逐个注册。
 */
@Composable
fun PreferenceCategory(
	modifier: Modifier = Modifier,
	title: String? = null,
	items: @Composable PreferenceCategoryScope.() -> Unit,
) {
	val scope = PreferenceCategoryScope()
	scope.items()
	Column(modifier = modifier.fillMaxWidth()) {
		if (title != null) {
			Text(
				text = title,
				style = MaterialTheme.typography.titleSmall,
				color = MaterialTheme.colorScheme.secondary,
				modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
			)
		}
		Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
			scope.collectedItems.forEachIndexed { index, content ->
				CompositionLocalProvider(
					LocalPreferenceIndex provides index,
					LocalPreferenceCount provides scope.collectedItems.size,
				) {
					content()
				}
			}
		}
	}
}