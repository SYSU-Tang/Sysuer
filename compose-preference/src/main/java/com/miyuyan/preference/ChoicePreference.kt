package com.miyuyan.preference

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MultiChoiceSegmentedButtonRow
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow

/**
 * 分段选择偏好项：trailingContent 区域为一组 Material3 [SegmentedButton]，点击即
 * 切换选中态并回调，适合把一组互斥或多选的选项直接铺在偏好项尾部。
 *
 * 条目外观与 [Preference] 一致（同一分组的圆角衔接），标题为主文本，内容区默认显示
 * 当前选中的选项文案（多选时用"、"连接），也可用 [summary] 覆盖。
 * 选中态以索引集合表示，调用方自行维护集合。
 *
 * @param entries 选项文案列表，索引即选项的标识。
 * @param entryValues 与 [entries] 按索引对应的业务值。
 * @param selections 当前选中的选项索引集合。
 * @param onChange 选项点击回调，参数为 (索引, 选项文案, 业务值)。
 * @param singleSelection true 时为单选：点击某项选中它，再次点击已选中项取消（清空选择）；
 *                        false 时为多选，任意增删。
 * @param enabled false 时所有按钮不可点。
 */
@Composable
fun <T> ChoicePreference(
	modifier: Modifier = Modifier,
	title: String,
	entries: List<String>,
	entryValues: List<T>? = null,
	selections: Set<Int>,
	onChange: ((Int?, String?, T?) -> Unit)? = null,
	enabled: Boolean = true,
	icon: @Composable (() -> Unit)? = null,
	summary: String? = null,
	singleSelection: Boolean = false,
) {
	val selectedText = selections.sorted().mapNotNull(entries::getOrNull).joinToString("、")

	Preference(
			onClick = null,
			title = title,
			modifier = modifier,
			enabled = enabled,
			icon = icon,
			summary = summary ?: selectedText.ifEmpty { null },
			trailing = {
				if (singleSelection) {
					SingleChoiceSegmentedButtonRow {
						entries.forEachIndexed { i, entry ->
							SegmentedButton(
									selected = i in selections,
									onClick = {
										onChange?.invoke(
												i,
												entry,
												entryValues?.getOrNull(i)
										)
									},
									shape = SegmentedButtonDefaults.itemShape(
											index = i, count = entries.size
									),
									label = {
										Text(
												entry,
												style = MaterialTheme.typography.labelMedium,
												maxLines = 1,
												overflow = TextOverflow.Ellipsis,
										)
									},
									enabled = enabled,
							)
						}
					}
				} else {
					MultiChoiceSegmentedButtonRow {
						entries.forEachIndexed { i, entry ->
							SegmentedButton(
									checked = i in selections,
									onCheckedChange = {
										onChange?.invoke(i, entry, entryValues?.getOrNull(i))
									},
									shape = SegmentedButtonDefaults.itemShape(
											index = i, count = entries.size
									),
									label = {
										Text(
												entry,
												style = MaterialTheme.typography.labelMedium,
												maxLines = 1,
												overflow = TextOverflow.Ellipsis,
										)
									},
									enabled = enabled,
							)
						}
					}
				}
			},
	)
}

/**
 * 自动管理选中状态的 [ChoicePreference] 重载：[initialSelections] 作为初始选中集合，
 * 选项点击时更新内部状态并经 [onChange] 通知父级，参数为 (索引, 选项文案, 业务值)。
 */
@Composable
fun <T> ChoicePreference(
	modifier: Modifier = Modifier,
	title: String,
	entries: List<String>,
	entryValues: List<T>? = null,
	enabled: Boolean = true,
	icon: @Composable (() -> Unit)? = null,
	summary: String? = null,
	singleSelection: Boolean = false,
	requireSelection: Boolean = false,
	initialSelections: Set<Int> = emptySet(),
	onChange: ((Set<Int>, List<String>, List<T>) -> Unit)? = null,
) {
	var selections by rememberSaveable { mutableStateOf(initialSelections) }
	ChoicePreference(
			title = title,
			entries = entries,
			entryValues = entryValues,
			selections = selections,
			onChange = { index, _, _ ->
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
			summary = summary,
			singleSelection = singleSelection,
	)
}
