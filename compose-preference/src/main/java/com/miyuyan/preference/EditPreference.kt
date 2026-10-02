package com.miyuyan.preference

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Edit
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.lerp
import androidx.compose.ui.unit.dp

/**
 * 文本编辑偏好项：点击后直接在条目上原地编辑文本，无需弹窗。
 *
 * 展示时文本以摘要样式显示在标题下方；点击进入编辑态后，标题缩小为 label 样式、
 * 正文放大为输入框（字号与颜色带缩放过渡动画），尾部变为确认按钮。通过 IME 的"完成"
 * 动作、确认按钮或焦点移出条目来提交，提交时才回调 [onChange]，输入过程不产生回调。
 * 键盘弹出时条目会自动滚动到输入法上方。
 *
 * @param value 当前文本。
 * @param onChange 提交回调，参数为 (index, name, value)，编辑项没有候选选项，
 *                 index/name 恒为 null、value 为编辑后的文本。父级应在回调中更新 [value]，
 *                 否则条目回显旧值。
 * @param placeholder [value] 为空时显示的提示文本，同时作为输入框的占位提示。
 * @param singleLine 是否单行。多行时 IME 不再提供"完成"动作，需用确认按钮或焦点移出提交。
 * @param maxLength 输入长度上限。
 * @param keyboardOptions 键盘配置（键盘类型、IME 动作等），单行时默认使用"完成"动作。
 * @param trailing 展示状态下自定义尾部 Composable（默认为编辑图标，禁用时隐藏）。
 */
@Composable
fun EditPreference(
	modifier: Modifier = Modifier,
	title: String,
	value: String,
	onChange: ((Int?, String?, String?) -> Unit)? = null,
	enabled: Boolean = true,
	icon: @Composable (() -> Unit)? = null,
	placeholder: String? = null,
	singleLine: Boolean = true,
	maxLength: Int = Int.MAX_VALUE,
	keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
	trailing: (@Composable () -> Unit)? = null,
) {
	var editing by remember { mutableStateOf(false) }
	var draft by remember { mutableStateOf(TextFieldValue(value)) }
	// 点击进入编辑时的光标定位依据：展示文本的点击坐标与其排版结果
	var tapPosition by remember { mutableStateOf<Offset?>(null) }
	var displayLayout by remember { mutableStateOf<TextLayoutResult?>(null) }
	// onFocusChanged 在节点初次挂载时也会以"未获得焦点"触发一次，
	// 必须等输入框真正获得过焦点后再把失焦当作提交，否则编辑态刚组合就会被提交退出
	var fieldHadFocus by remember { mutableStateOf(false) }
	val focusRequester = remember { FocusRequester() }
	val bringIntoViewRequester = remember { BringIntoViewRequester() }
	val keyboard = LocalSoftwareKeyboardController.current
	val index = LocalPreferenceIndex.current
	val count = LocalPreferenceCount.current
	// ime insets 在组合中读取为状态，键盘弹出/收起动画的每帧都会触发重组
	val density = LocalDensity.current
	val imeBottom = WindowInsets.ime.getBottom(density)

	val commit: () -> Unit = {
		if (editing) {
			editing = false
			fieldHadFocus = false
			keyboard?.hide()
			if (draft.text != value) {
				onChange?.invoke(null, null, draft.text)
			}
		}
	}
	LaunchedEffect(editing) {
		if (editing) {
			// 光标落在进入编辑前点击的文本位置；未点到文本上或文本为空时放到末尾/开头
			val cursor = if (value.isNotEmpty()) {
				displayLayout?.let { layout ->
					tapPosition?.let { pos -> layout.getOffsetForPosition(pos) }
				} ?: value.length
			} else 0
			tapPosition = null
			draft = TextFieldValue(value, TextRange(cursor.coerceIn(0, value.length)))
			focusRequester.requestFocus()
			keyboard?.show()
		}
	}
	// 键盘弹出过程中持续把条目滚到可视区域内（视口已由 PreferenceScreen 的 imePadding 收缩），
	// 避免输入法遮挡正在编辑的条目
	LaunchedEffect(editing, imeBottom) {
		if (editing && imeBottom > 0) {
			bringIntoViewRequester.bringIntoView()
		}
	}

	// 展示态与编辑态共用一个条目布局，标题与正文的字号、颜色用 TextStyle 插值连续缩放：
	// 标题 bodyLarge(主文本色) ↔ bodyMedium(辅助色)，正文 bodyMedium(辅助色) ↔ bodyLarge(主文本色)
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
	// 值为空且没有占位提示时，标题独占内容区（与 Preference 无摘要时的布局一致）
	val titleOnly = !editing && value.isEmpty() && placeholder.isNullOrEmpty()

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
			verticalAlignment = Alignment.CenterVertically,
			modifier = modifier
				.fillMaxWidth()
				.bringIntoViewRequester(bringIntoViewRequester),
			shapes = ListItemDefaults.segmentedShapes(index, count),
			colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
			leadingContent = { icon?.invoke() },
			overlineContent = if (!titleOnly) {
				{ Text(title, style = titleStyle) }
			} else null,
			trailingContent = {
				if (editing) {
					IconButton(onClick = commit) {
						Icon(
								imageVector = Icons.Rounded.Check,
								contentDescription = stringResource(R.string.confirm),
								tint = MaterialTheme.colorScheme.primary,
						)
					}
				} else if (trailing != null) {
					trailing()
				} else if (enabled) {
					if (value.isNotEmpty()) {
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
		if (titleOnly) {
			Text(title, style = titleStyle)
		} else if (editing) {
			BasicTextField(
					value = draft,
					onValueChange = { draft = it.copy(text = it.text.take(maxLength)) },
					textStyle = valueStyle,
					cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
					singleLine = singleLine,
					keyboardOptions = if (singleLine && keyboardOptions.imeAction == ImeAction.Default) {
						keyboardOptions.copy(imeAction = ImeAction.Done)
					} else keyboardOptions,
					keyboardActions = KeyboardActions(onDone = { commit() }),
					modifier = Modifier
						.fillMaxWidth()
						.focusRequester(focusRequester)
						.padding(vertical =  8.dp)
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
		} else {
			Text(
					value.ifEmpty { placeholder.orEmpty() },
					style = valueStyle,
					onTextLayout = { displayLayout = it },
					// 记录点击位置（Initial pass，不消费事件，不影响整行点击进入编辑），
					// 进入编辑后光标定位到点击处
					modifier = Modifier.pointerInput(value) {
						awaitEachGesture {
							awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
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

/**
 * 自动管理文本状态的 [EditPreference] 重载：[initialValue] 作为初始文本，
 * 提交时更新内部状态并经 [onChange] 通知父级（index/name 恒为 null，value 为文本）。
 */
@Composable
fun EditPreference(
	title: String,
	modifier: Modifier = Modifier,
	enabled: Boolean = true,
	icon: @Composable (() -> Unit)? = null,
	placeholder: String? = null,
	singleLine: Boolean = true,
	maxLength: Int = Int.MAX_VALUE,
	keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
	initialValue: String = "",
	onChange: ((Int?, String?, String?) -> Unit)? = null,
) {
	var value by rememberSaveable { mutableStateOf(initialValue) }
	EditPreference(
			title = title,
			value = value,
			onChange = { _, _, newValue ->
				value = newValue.orEmpty()
				onChange?.invoke(null, null, newValue)
			},
			modifier = modifier,
			enabled = enabled,
			icon = icon,
			placeholder = placeholder,
			singleLine = singleLine,
			maxLength = maxLength,
			keyboardOptions = keyboardOptions,
	)
}
