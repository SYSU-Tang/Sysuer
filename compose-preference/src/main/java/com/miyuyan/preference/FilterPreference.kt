package com.miyuyan.preference

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * 筛选偏好项：summary 区域为一组 [FilterChip]（ChipGroup），点击 Chip 即时切换选中态并回调，
 * 无需弹出二级菜单。适合把一组互斥或可多选的筛选项直接铺在偏好项上。
 *
 * 条目外观与 [Preference] 一致（同一分组的圆角衔接），标题位于 Chips 上方。
 * 选中态以索引集合表示，调用方自行维护集合。
 *
 * @param entries Chip 文案列表，索引即 Chip 的标识。
 * @param entryValues 与 [entries] 按索引对应的业务值。
 * @param selections 当前选中的 Chip 索引集合。
 * @param onChange Chip 点击回调，参数为 (索引, 选项文案, 业务值)。
 * @param singleSelection true 时为单选：点击某项选中它，再次点击已选中项取消（清空筛选）；
 *                        false 时为多选，任意增删。
 * @param enabled false 时所有 Chip 不可点。
 */
@Composable
fun <T> FilterPreference(
	modifier: Modifier = Modifier,
	title: String,
	entries: List<String>,
	entryValues: List<T>? = null,
	selections: Set<Int>,
	onChange: ((Int?, String?, T?) -> Unit)? = null,
	enabled: Boolean = true,
	icon: @Composable (() -> Unit)? = null,
	singleSelection: Boolean = false,
) {
	Preference(
			onClick = null,
			title = title,
			enabled = enabled,
			modifier = modifier,
			icon = icon,
			content = {
				FlowRow(
						horizontalArrangement = Arrangement.spacedBy(8.dp)
				) {
					entries.forEachIndexed { i, entry ->
						FilterChip(
								selected = i in selections,
								onClick = {
									onChange?.invoke(i, entry, entryValues?.getOrNull(i))
								},
								label = {
									Text(entry, style = MaterialTheme.typography.labelMedium)
								},
								leadingIcon = if (i in selections) {
									{
										Icon(
												imageVector = Icons.Rounded.Check,
												contentDescription = null,
												modifier = Modifier.size(18.dp),
										)
									}
								} else null,
								enabled = enabled,
						)
					}
				}
			})
}

/**
 * 自动管理选中状态的 [FilterPreference] 重载：[initialSelections] 作为初始选中集合，
 * Chip 点击时更新内部状态并经 [onChange] 通知父级，参数为 (索引, 选项文案, 业务值)。
 */
@Composable
fun <T> FilterPreference(
	modifier: Modifier = Modifier,
	title: String,
	entries: List<String>,
	entryValues: List<T>? = null,
	enabled: Boolean = true,
	icon: @Composable (() -> Unit)? = null,
	singleSelection: Boolean = false,
	initialSelections: Set<Int> = emptySet(),
	requireSelection: Boolean = false,
	onChange: ((Set<Int>, List<String>?, List<T>) -> Unit)? = null,
) {
	var selections by rememberSaveable { mutableStateOf(initialSelections) }
	FilterPreference(
			title = title,
			entries = entries,
			entryValues = entryValues,
			selections = selections,
			onChange = { index, name, value ->
				if (index != null) {
					if (index in selections) {
						if (selections.size > 1 || !requireSelection) {
							selections -= index
						}
					} else if (singleSelection) {
						selections = setOf(index)
					} else {
						selections += index
					}
				}
				onChange?.invoke(
						selections,
						selections.map { entries[it] },
						entryValues?.let { v -> selections.map { v[it] } } ?: emptyList())
			},
			modifier = modifier,
			enabled = enabled,
			icon = icon,
			singleSelection = singleSelection,
	)
}
