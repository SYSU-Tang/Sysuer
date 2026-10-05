package com.miyuyan.preference

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntRect
import com.miyuyan.preference.simplemenu.SimpleMenuPopup
import kotlin.math.roundToInt

/**
 * 单选下拉偏好项，弹出 Simple Menu（Material Design 1）样式的菜单，
 * 完整复刻 View 版 [SimpleMenuPreference] 的 PopupMenu 样式、背景与动画。
 *
 * @param selectedIndex 选中项索引。
 *
 * @param onChange 选中项回调。
 */
@Composable
fun <T> MenuPreference(
	title: String,
	modifier: Modifier = Modifier,
	enabled: Boolean = true,
	icon: @Composable (() -> Unit)? = null,
	summary: String? = null,
	entries: List<String>,
	entryValues: List<T>? = null,
	selectedIndex: Int? = null,
	required: Boolean = false,
	onChange: ((Int?, String?, T?) -> Unit)? = null,
) {
	var popupVisible by remember { mutableStateOf(false) }
	var itemBounds by remember { mutableStateOf(IntRect.Zero) }
	var titleBounds by remember { mutableStateOf(IntRect.Zero) }

	val clickAction = if (enabled && entries.isNotEmpty()) {
		{ popupVisible = true }
	} else null

	Box(
			modifier.onGloballyPositioned { coordinates ->
				val bounds = coordinates.boundsInWindow()
				itemBounds = IntRect(
						bounds.left.roundToInt(),
						bounds.top.roundToInt(),
						bounds.right.roundToInt(),
						bounds.bottom.roundToInt()
				)
			}) {
		Preference(
				onClick = clickAction,
				title = title,
				modifier = Modifier.fillMaxWidth(),
				enabled = enabled,
				icon = icon,
				summary = summary,
				titleModifier = Modifier.onGloballyPositioned { coordinates ->
					val bounds = coordinates.boundsInWindow()
					titleBounds = IntRect(
							bounds.left.roundToInt(),
							bounds.top.roundToInt(),
							bounds.right.roundToInt(),
							bounds.bottom.roundToInt()
					)
				},
				trailing = if (selectedIndex != null && !required) {
					{
						IconButton(onClick = { onChange?.invoke(null, null, null) }) {
							Icon(
									imageVector = Icons.Rounded.Close,
									tint = MaterialTheme.colorScheme.primary,
									contentDescription = stringResource(R.string.clear),
							)
						}
					}
				} else {
					{
						Icon(
								imageVector = Icons.Rounded.ArrowDropDown,
								tint = MaterialTheme.colorScheme.primary,
								contentDescription = null,
						)
					}
				},

				)
		if (popupVisible) {
			SimpleMenuPopup(
					entries = entries,
					selectedIndex = selectedIndex,
					anchorBounds = if (titleBounds != IntRect.Zero) titleBounds else itemBounds,
					onDismiss = { popupVisible = false },
			) { index ->
				onChange?.invoke(index, entries.getOrNull(index), entryValues?.getOrNull(index))
			}
		}
	}
}


@Composable
fun <T> MenuPreference(
	modifier: Modifier = Modifier,
	title: String,
	icon: @Composable (() -> Unit)? = null,
	enabled: Boolean = true,
	required: Boolean = false,
	entries: List<String>,
	entryValues: List<T>? = null,
	initialIndex: Int? = null,
	onChange: ((Int?, String?, T?) -> Unit)? = null,
) {
	var selectedIndex by remember { mutableStateOf(initialIndex) }
	val none = stringResource(R.string.none)
	val summary = remember(selectedIndex) {
		entries.getOrElse(selectedIndex ?: -1) { none }
	}
	MenuPreference(
			title = title,
			modifier = modifier,
			enabled = enabled,
			required = required,
			icon = icon,
			summary = summary,
			entries = entries,
			entryValues = entryValues,
			selectedIndex = selectedIndex,
			onChange = { index, name, value ->
				selectedIndex = index
				onChange?.invoke(index, name, value)
			},
	)
}
