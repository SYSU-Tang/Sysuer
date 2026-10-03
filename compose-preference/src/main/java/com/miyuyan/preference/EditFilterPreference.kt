package com.miyuyan.preference

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.lerp
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * 可输入筛选的 ChipGroup 偏好项：点击条目进入编辑态，输入文本实时筛选下方
 * [FilterChip] 组（ChipGroup），点选 Chip 立即写入对应的 entryValue；也可以直接
 * 提交输入的文本。选项较多时先输入缩小范围再点选。
 *
 * 提交规则（确认按钮、IME"完成"或焦点移出时）：输入能精确匹配到某选项时写入该选项的
 * entryValue；未匹配到时，[requireSelection] 为 false 则把输入文本作为自定义值提交，
 * 为 true 则放弃提交并回显旧值。
 *
 * @param entries 选项文案列表。
 * @param entryValues 与 [entries] 按索引对应的业务值。
 * @param name 当前值：选中项的 entryValue，或（[requireSelection] 为 false 时）自定义文本。
 * @param onChange 值变化回调：点选 Chip 或提交输入精确匹配到选项时，参数为
 *                 (选项索引, 选项文案, entryValue)；提交自定义文本时为 (null, null, 文本)。
 * @param requireSelection true 时只能从选项中选择，未匹配的输入无法提交。
 * @param placeholder [name] 为空时显示的提示文本。
 */
@Composable
fun EditFilterPreference(
	modifier: Modifier = Modifier,
	title: String,
	entries: List<String>,
	entryValues: List<String>,
	name: String,
	onChange: ((Int?, String?, String?) -> Unit)? = null,
	enabled: Boolean = true,
	icon: @Composable (() -> Unit)? = null,
	requireSelection: Boolean = true,
	placeholder: String? = null,
) {
	val index = LocalPreferenceIndex.current
	val count = LocalPreferenceCount.current
	var editing by remember { mutableStateOf(false) }
	var draft by remember { mutableStateOf(TextFieldValue(name)) }
	// 点击进入编辑时的光标定位依据：展示文本的点击坐标与其排版结果
	var tapPosition by remember { mutableStateOf<Offset?>(null) }
	var displayLayout by remember { mutableStateOf<TextLayoutResult?>(null) }
	// onFocusChanged 在节点初次挂载时也会以"未获得焦点"触发一次，
	// 必须等输入框真正获得过焦点后再把失焦当作提交，否则编辑态刚组合就会被提交退出
	var fieldHadFocus by remember { mutableStateOf(false) }
	val focusRequester = remember { FocusRequester() }
	val keyboard = LocalSoftwareKeyboardController.current

	val commit: () -> Unit = {
		if (editing) {
			editing = false
			fieldHadFocus = false
			keyboard?.hide()
			val text = draft.text.trim()
			val idx = entries.indexOfFirst {
				it.equals(text, ignoreCase = true)
			}
			when {
				idx >= 0 -> {
					entryValues.getOrNull(idx)?.takeIf { it != name }?.let {
						onChange?.invoke(idx, entries.getOrNull(idx), it)
					}
				}

				!requireSelection && text.isNotBlank() -> {
					if (text != name) onChange?.invoke(null, null, text)
				}
				// 未匹配到选项且必须在选项中选择：放弃提交，回显旧值
			}
		}
	}
	LaunchedEffect(editing) {
		if (editing) {
			val cursor = if (name.isNotEmpty()) {
				displayLayout?.let { layout ->
					tapPosition?.let { pos -> layout.getOffsetForPosition(pos) }
				} ?: name.length
			} else 0
			tapPosition = null
			draft = TextFieldValue(name, TextRange(cursor.coerceIn(0, name.length)))
			focusRequester.requestFocus()
			keyboard?.show()
		}
	}
	// 编辑中按返回：提交并退出编辑（与 EditMenuDropdown 的返回层级对齐）
	BackHandler(enabled = editing) {
		commit()
	}
	val filteredIndices = remember(draft.text, entries) { filterEntryIndices(entries, draft.text) }

	// 展示态与编辑态共用一个条目布局，标题与正文的字号、颜色用 TextStyle 插值连续缩放：
	// 标题 bodyLarge(主文本色) ↔ labelMedium(辅助色)，正文 labelMedium(辅助色) ↔ bodyLarge(主文本色)
	val progress by animateFloatAsState(
			targetValue = if (editing) 1f else 0f,
			animationSpec = tween(200),
			label = "editTextScale",
	)
	val titleStyle = lerp(
			MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
			MaterialTheme.typography.labelMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
			progress,
	)
	val valueStyle = lerp(
			MaterialTheme.typography.labelMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
			MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
			progress,
	)

	SegmentedListItem(
			onClick = if (enabled && !editing) {
		{
			editing = true
			fieldHadFocus = false
		}
	} else {
		{}
	},
			enabled = enabled,
			verticalAlignment = Alignment.Top,
			modifier = modifier.fillMaxWidth(),
			shapes = ListItemDefaults.segmentedShapes(index, count),
			colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
			leadingContent = { icon?.invoke() },
			overlineContent = {
				Text(title, style = titleStyle)
			},
			trailingContent = if (editing) {
				{
					IconButton(onClick = commit) {
						Icon(
								imageVector = Icons.Rounded.Check,
								contentDescription = stringResource(R.string.confirm),
								tint = MaterialTheme.colorScheme.primary,
						)
					}
				}
			} else {
				{
					if (name.isNotEmpty()) {
						IconButton(onClick = { onChange?.invoke(null, null, "") }) {
							Icon(
									imageVector = Icons.Rounded.Close,
									contentDescription = stringResource(R.string.clear),
									tint = MaterialTheme.colorScheme.primary,
							)
						}
					} else {
						Icon(
								imageVector = Icons.Rounded.Edit,
								contentDescription = null,
								tint = MaterialTheme.colorScheme.primary,
						)
					}
				}
			}) {
		if (editing) {
			Column {
				BasicTextField(
						value = draft,
						onValueChange = { draft = it },
						textStyle = valueStyle,
						cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
						singleLine = true,
						keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
						keyboardActions = KeyboardActions(onDone = { commit() }),
						modifier = Modifier
							.fillMaxWidth()
							.focusRequester(focusRequester)
							.onFocusChanged {
								if (editing) {
									if (it.hasFocus) {
										fieldHadFocus = true
									} else if (fieldHadFocus) {
										commit()
									}
								}
							},
						decorationBox = { innerTextField ->
							Box {
								if (draft.text.isEmpty() && placeholder != null) {
									Text(
											placeholder,
											style = valueStyle,
											color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
													alpha = 0.5f
											),
											maxLines = 1,
									)
								}
								innerTextField()
							}
						},
				)
				FlowRow(
						modifier = Modifier
							.fillMaxWidth()
							.padding(top = 8.dp),
						horizontalArrangement = Arrangement.spacedBy(8.dp),
						verticalArrangement = Arrangement.spacedBy(4.dp),
				) {
					filteredIndices.forEach { i ->
						FilterChip(
								selected = entryValues.getOrNull(i) == name,
								onClick = {
									// 与菜单版一致：点选即提交并退出编辑
									editing = false
									keyboard?.hide()
									entryValues.getOrNull(i)?.takeIf { it != name }?.let { selected ->
										onChange?.invoke(i, entries.getOrNull(i), selected)
									}
								},
								label = {
									Text(
											entries[i],
											style = MaterialTheme.typography.labelMedium,
											maxLines = 1,
											overflow = TextOverflow.Ellipsis,
									)
								},
								leadingIcon = if (entryValues.getOrNull(i) == name) {
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
			}
		} else {
			Text(
					name.takeIf { it.isNotEmpty() } ?: stringResource(R.string.click_to_edit),
					style = valueStyle,
					maxLines = 1,
					overflow = TextOverflow.Ellipsis,
					onTextLayout = { displayLayout = it },
					// 记录点击位置（Initial pass，不消费事件，不影响整行点击进入编辑），
					// 进入编辑后光标定位到点击处
					modifier = Modifier.pointerInput(name) {
						awaitEachGesture {
							awaitFirstDown(
									requireUnconsumed = false, pass = PointerEventPass.Initial
							)
							val up = waitForUpOrCancellation(pass = PointerEventPass.Initial)
							if (up != null) {
								tapPosition = up.position
							}
						}
					},
			)
		}
	}
}
