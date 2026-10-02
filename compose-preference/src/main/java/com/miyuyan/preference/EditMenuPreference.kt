package com.miyuyan.preference

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenu
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
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
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.lerp
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * 按输入文本筛选选项，返回保留下来的原始索引：空查询返回全部，
 * 否则保留文案包含该文本（忽略大小写）的选项。
 */
internal fun filterEntryIndices(entries: List<String>, query: String): List<Int> =
	if (query.isBlank()) entries.indices.toList()
	else entries.indices.filter { entries[it].contains(query.trim(), ignoreCase = true) }

/**
 * 可输入筛选的下拉偏好项：与 [EditMenuPreference] 一致，但输入文本会实时筛选候选列表
 * （包含匹配、忽略大小写），选项较多时先输入缩小范围再点选。
 *
 * @param entries 选项文案列表。
 * @param entryValues 与 [entries] 按索引对应的业务值。
 * @param name 当前值：选中项的 entryValue，或（[requireSelection] 为 false 时）自定义文本。
 * @param onChange 值变化回调：点选候选项或提交输入精确匹配到选项时，参数为
 *                 (选项索引, 选项文案, entryValue)；提交自定义文本时为 (null, null, 文本)。
 * @param requireSelection true 时只能从选项中选择，未匹配的输入无法提交。
 * @param placeholder [name] 为空时显示的提示文本。
 */
@Composable
fun <T> FilteredEditMenuPreference(
	modifier: Modifier = Modifier,
	title: String,
	entries: List<String>,
	entryValues: List<T>? = null,
	name: String,
	onChange: ((Int?, String, T?) -> Unit)? = null,
	enabled: Boolean = true,
	icon: @Composable (() -> Unit)? = null,
	requireSelection: Boolean = true,
	placeholder: String? = null,
) {
	val originalEntries = remember { entries }
	val originalEntryValues = remember { entryValues }
	var name by remember { mutableStateOf(name) }
	var query by remember { mutableStateOf(name) }
	var entries by remember { mutableStateOf(originalEntries) }
	var entryValues by remember { mutableStateOf(originalEntryValues) }
	LaunchedEffect(query) {
		val (newKeys, newValues) = originalEntries.zip(originalEntryValues ?: emptyList())
			.filter { (k, _) ->
				k.contains(query, ignoreCase = true)
			}.unzip()
		entries = newKeys
		entryValues = newValues
	}
	EditMenuPreference(
			modifier = modifier,
			title = title,
			entries = entries,
			entryValues = entryValues,
			initialName = name,
			onChange = { a, b, c ->
				name = b
				onChange?.invoke(a, b, c)
			},
			onQueryChange = { q ->
				query = q
			},
			enabled = enabled,
			icon = icon,
			requireSelection = requireSelection,
			placeholder = placeholder,
	)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> EditMenuPreference(
	modifier: Modifier = Modifier,
	title: String,
	entries: List<String>,
	entryValues: List<T>? = null,
	initialName: String = "",
	onChange: ((Int?, String, T?) -> Unit)? = null,
	onQueryChange: ((String) -> Unit)? = null,
	enabled: Boolean = true,
	required: Boolean = false, // 1. 新增必填标志，默认 false
	icon: @Composable (() -> Unit)? = null,
	requireSelection: Boolean = true,
	emptyHint: String = stringResource(R.string.click_to_edit),
	placeholder: String? = null,
) {
	val index = LocalPreferenceIndex.current
	val count = LocalPreferenceCount.current
	var editing by remember { mutableStateOf(false) }
	var draft by remember { mutableStateOf(TextFieldValue(initialName)) }

	var tapPosition by remember { mutableStateOf<Offset?>(null) }
	var displayLayout by remember { mutableStateOf<TextLayoutResult?>(null) }

	var fieldHadFocus by remember { mutableStateOf(false) }
	val focusRequester = remember { FocusRequester() }
	val keyboard = LocalSoftwareKeyboardController.current

	val density = LocalDensity.current
	val windowSize = LocalWindowInfo.current.containerSize
	val imeBottomInset = maxOf(
			WindowInsets.systemBars.getBottom(density),
			WindowInsets.displayCutout.getBottom(density),
			WindowInsets.ime.getBottom(density)
	)
	var editorBounds by remember { mutableStateOf(IntRect.Zero) }
	var menuExpanded by remember { mutableStateOf(false) }

	var summary by remember(initialName) { mutableStateOf(initialName) }

	// 2. 提交逻辑中增加必填校验
	val commitText: (String, Int?) -> Unit = { textToCommit, explicitIndex ->
		val trimmed = textToCommit.trim()

		// 如果设置为必填且输入内容为空，则取消提交（可在此处扩展错误提示）
		if (required && trimmed.isEmpty()) {
			// 恢复为之前的 summary
			draft = draft.copy(text = summary)
		} else {
			if (trimmed != summary) {
				summary = trimmed
				if (requireSelection) {
					val targetIndex = explicitIndex ?: entries.indexOfFirst {
						it.equals(trimmed, ignoreCase = true)
					}
					if (targetIndex >= 0) {
						onChange?.invoke(
								targetIndex, trimmed, entryValues?.getOrNull(targetIndex)
						)
					}
				} else {
					onChange?.invoke(null, trimmed, null)
				}
			}
		}
	}

	val commit = {
		if (editing) {
			val trimmed = draft.text.trim()
			// 必填项为空时点击确认/失焦不退出编辑态
			if (required && trimmed.isEmpty()) {
				// 可在此处触发 Toast 或展示错误提示
			} else {
				editing = false
				fieldHadFocus = false
				keyboard?.hide()
				commitText(draft.text, null)
			}
		}
	}

	LaunchedEffect(editing) {
		if (editing) {
			val cursor = if (summary.isNotEmpty()) {
				displayLayout?.let { layout ->
					tapPosition?.let { pos -> layout.getOffsetForPosition(pos) }
				} ?: summary.length
			} else 0
			tapPosition = null
			draft = TextFieldValue(summary, TextRange(cursor.coerceIn(0, summary.length)))
			focusRequester.requestFocus()
			keyboard?.show()
			menuExpanded = true
		} else {
			menuExpanded = false
		}
	}

	BackHandler(enabled = editing && menuExpanded) {
		menuExpanded = false
	}
	BackHandler(enabled = editing && !menuExpanded) {
		commit()
	}

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
	val error = colorScheme.error
	// 3. 构建带红星号的 Title 样式
	val annotatedTitle = remember(title, required, titleStyle.color) {
		buildAnnotatedString {
			append(title)
			if (required) {
				withStyle(SpanStyle(error)) {
					append(" *")
				}
			}
		}
	}

	SegmentedListItem(
			onClick = {
		if (enabled && !editing) {
			editing = true
			fieldHadFocus = false
		}
		menuExpanded = !menuExpanded
	},
			enabled = enabled,
			verticalAlignment = Alignment.CenterVertically,
			modifier = modifier.fillMaxWidth(),
			shapes = ListItemDefaults.segmentedShapes(index, count),
			colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
			leadingContent = { icon?.invoke() },
			overlineContent = {
				Text(text = annotatedTitle, style = titleStyle)
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
					// 如果必填，不展示一键清空按钮（防止用户误删）
					if (!required && !requireSelection && summary.isNotEmpty()) {
						IconButton(onClick = {
							summary = ""
							onChange?.invoke(null, "", null)
							onQueryChange?.invoke("")
						}) {
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
			val editorVisible =
				editorBounds.bottom > 0 && editorBounds.top < windowSize.height - imeBottomInset

			ExposedDropdownMenuBox(
					expanded = menuExpanded && entries.isNotEmpty() && editorVisible,
					onExpandedChange = { },
					modifier = Modifier.fillMaxWidth(),
			) {
				BasicTextField(
						value = draft,
						onValueChange = {
							draft = it
							menuExpanded = true
							onQueryChange?.invoke(it.text)
						},
						textStyle = valueStyle,
						cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
						singleLine = true,
						keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
						keyboardActions = KeyboardActions(onDone = { commit() }),
						modifier = Modifier
							.fillMaxWidth()
							.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable)
							.padding(vertical = 8.dp)
							.focusRequester(focusRequester)
							.onGloballyPositioned { coordinates ->
								val bounds = coordinates.boundsInWindow()
								editorBounds = IntRect(
										bounds.left.roundToInt(),
										bounds.top.roundToInt(),
										bounds.right.roundToInt(),
										bounds.bottom.roundToInt()
								)
							}
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
				ExposedDropdownMenu(
						expanded = menuExpanded && entries.isNotEmpty() && editorVisible,
						onDismissRequest = {
							menuExpanded = false
						},
				) {
					entries.forEachIndexed { i, entry ->
						DropdownMenuItem(
								text = {
							Text(
									entry,
									style = MaterialTheme.typography.bodyLarge,
									maxLines = 1,
									overflow = TextOverflow.Ellipsis,
							)
						}, onClick = {
							editing = false
							fieldHadFocus = false
							keyboard?.hide()
							draft = draft.copy(text = entry)
							commitText(entry, i)
						}, modifier = Modifier.exposedDropdownSize(true)
						)
					}
				}
			}
		} else {
			Text(
					summary.takeIf { it.isNotEmpty() } ?: emptyHint,
					style = valueStyle,
					maxLines = 1,
					overflow = TextOverflow.Ellipsis,
					onTextLayout = { displayLayout = it },
					modifier = Modifier.pointerInput(summary) {
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