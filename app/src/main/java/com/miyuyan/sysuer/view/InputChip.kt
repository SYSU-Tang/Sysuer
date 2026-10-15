package com.miyuyan.sysuer.view

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.miyuyan.sysuer.R

/**
 * 单选下拉 Chip，参数与行为对齐 [com.miyuyan.preference.MenuPreference]：
 * 用索引选中，回调同时给出索引、文案和值；非必填且已选中时尾部可清空。
 *
 * @param selectedIndex 选中项索引，null 表示未选。
 * @param required 为 true 时不显示清空按钮。
 * @param onChange 选中或清空回调，清空时三个参数都为 null。
 */
@Composable
fun <T> SingleSelectChipDropdown(
	title: String,
	modifier: Modifier = Modifier,
	enabled: Boolean = true,
	entries: List<String>,
	entryValues: List<T>? = null,
	selectedIndex: Int? = null,
	required: Boolean = false,
	onChange: ((Int?, String?, T?) -> Unit)? = null,
) {
	var expanded by remember { mutableStateOf(false) }
	val isSelected = selectedIndex != null && entries.getOrNull(selectedIndex) != null

	Box(modifier = modifier) {
		FilterChip(
				selected = isSelected,
				enabled = enabled && entries.isNotEmpty(),
				onClick = { expanded = true },
				label = {
					val selectedEntry = selectedIndex?.let { entries.getOrNull(it) }
					Text(
							if (selectedEntry == null) title
							else "$title: $selectedEntry"
					)
				},
				trailingIcon = {
					if (isSelected && !required) {
						Box(
								modifier = Modifier
									.size(24.dp)
									.clip(CircleShape)
									.clickable { onChange?.invoke(null, null, null) },
								contentAlignment = Alignment.Center,
						) {
							Icon(
									imageVector = Icons.Rounded.Close,
									contentDescription = stringResource(R.string.clear),
									modifier = Modifier.size(FilterChipDefaults.IconSize),
							)
						}
					} else {
						Icon(
								imageVector = Icons.Rounded.ArrowDropDown,
								contentDescription = stringResource(R.string.expand)
						)
					}
				})
		DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
			entries.forEachIndexed { index, entry ->
				DropdownMenuItem(
						text = { Text(entry) },
						modifier = Modifier.background(
								if (index == selectedIndex) androidx.compose.ui.graphics.Color.LightGray
								else androidx.compose.ui.graphics.Color.Transparent
						),
						onClick = {
							onChange?.invoke(index, entry, entryValues?.getOrNull(index))
							expanded = false
						},
						leadingIcon = if (index == selectedIndex) {
							{ Icon(Icons.Default.Check, contentDescription = null) }
						} else null)
			}
		}
	}
}

@Composable
fun MultiSelectChipDropdown(
	category: String,
	options: List<String>,
	selectedValues: Set<String> = emptySet(),
	onValueChange: ((Set<String>) -> Unit)? = null,
) {
	var expanded by remember { mutableStateOf(false) }
	var internalValues by remember { mutableStateOf(setOf<String>()) }
	val currentValues = if (onValueChange != null) selectedValues else internalValues
	val isSelected = currentValues.isNotEmpty()
	val displayText = when {
		currentValues.isEmpty() -> category
		currentValues.size == 1 -> currentValues.first()
		else -> "$category(${currentValues.size})"
	}

	Box {
		FilterChip(
				selected = isSelected,
				onClick = { expanded = true },
				label = { Text(displayText) },
				trailingIcon = {
					Icon(
							imageVector = Icons.Rounded.ArrowDropDown,
							contentDescription = stringResource(R.string.expand)
					)
				})
		DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
			options.forEach { option ->
				val isChecked = currentValues.contains(option)
				DropdownMenuItem(text = { Text(option) }, onClick = {
					val newValues =
						if (isChecked) currentValues - option else currentValues + option
					if (onValueChange != null) onValueChange(newValues) else internalValues =
						newValues
				}, leadingIcon = {
					Checkbox(checked = isChecked, onCheckedChange = null)
				})
			}
		}
	}
}

@Composable
fun InputDialogChip(
	category: String,
	value: String? = null,
	inputType: KeyboardType = KeyboardType.Unspecified,
	onValueChange: ((String) -> Unit)? = null,
) {
	var showDialog by remember { mutableStateOf(false) }
	val isSelected = value?.isNotEmpty() == true

	FilterChip(selected = isSelected, onClick = {
		showDialog = true
	}, label = {
		Text(if (isSelected) "$category: $value" else category)
	}, trailingIcon = {
		Icon(
				imageVector = Icons.Default.Edit,
				contentDescription = stringResource(R.string.edit),
				modifier = Modifier.size(12.dp)
		)
	})

	if (showDialog) {
		EditDialog(title = category, value = value ?: "", inputType = inputType, onCancel = {
			showDialog = false
		}, onConfirm = { v ->
			if (onValueChange != null && value != v) onValueChange(v)
			showDialog = false
		}, showClear = isSelected, onClear = {
			if (onValueChange != null && !value.isNullOrEmpty()) onValueChange("")
			showDialog = false
		})
	}
}