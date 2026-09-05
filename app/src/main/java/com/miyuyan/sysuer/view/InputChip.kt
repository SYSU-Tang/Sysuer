package com.miyuyan.sysuer.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
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

@OptIn(ExperimentalMaterial3Api::class) @Composable fun SingleSelectChipDropdown(
	category: String,
	options: List<String>,
	selectedValue: String? = null,
	optionValues: List<String?> = options,
	onValueChange: ((String?) -> Unit)? = null,
                                                                                ) {
	var expanded by remember { mutableStateOf(false) }
	val isSelected = selectedValue != null && selectedValue != optionValues.firstOrNull()
	
	Box {
		FilterChip(selected = isSelected, onClick = { expanded = true }, label = {
			Text(if (!isSelected) category
			     else "$category: $selectedValue")
		}, trailingIcon = {
			Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "展开菜单")
		})
		DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
			options.forEachIndexed { index, option ->
				DropdownMenuItem(text = { Text(option) },
				                 modifier = Modifier.background(if (selectedValue == optionValues[index] || (index == 0 && !isSelected)) androidx.compose.ui.graphics.Color.LightGray else androidx.compose.ui.graphics.Color.Transparent),
				                 onClick = {
					                 if (onValueChange != null) onValueChange(optionValues[index])
					                 expanded = false
				                 },
				                 leadingIcon = if (selectedValue == optionValues[index]) {
					                 { Icon(Icons.Default.Check, contentDescription = null) }
				                 }
				                 else null)
			}
		}
	}
}

@OptIn(ExperimentalMaterial3Api::class) @Composable fun MultiSelectChipDropdown(
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
		currentValues.size == 1 -> "$category: ${currentValues.first()}"
		else -> "$category: 已选 ${currentValues.size} 项"
	}
	
	Box {
		FilterChip(selected = isSelected, onClick = { expanded = true }, label = { Text(displayText) }, trailingIcon = {
			Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = stringResource(R.string.expand))
		})
		DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
			options.forEach { option ->
				val isChecked = currentValues.contains(option)
				DropdownMenuItem(text = { Text(option) }, onClick = {
					val newValues = if (isChecked) currentValues - option else currentValues + option
					if (onValueChange != null) onValueChange(newValues) else internalValues = newValues
				}, leadingIcon = {
					Checkbox(checked = isChecked, onCheckedChange = null)
				})
			}
		}
	}
}

@OptIn(ExperimentalMaterial3Api::class) @Composable fun InputDialogChip(
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
		Icon(imageVector = Icons.Default.Edit, contentDescription = stringResource(R.string.edit), modifier = Modifier.size(12.dp))
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