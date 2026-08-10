package com.sysu.edu.view

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import com.sysu.edu.R

@OptIn(ExperimentalMaterial3Api::class) @Composable fun EditDialog(
	title: String,
	value: String,
	inputType: KeyboardType = KeyboardType.Unspecified,
	onCancel: () -> Unit,
	onConfirm: (String) -> Unit,
	showClear: Boolean = false,
	onClear: (() -> Unit)? = null,
                                                                  ) {
	var tempValue by remember { mutableStateOf(value) }
	AlertDialog(onDismissRequest = onCancel, title = { Text(title) }, text = {
		OutlinedTextField(value = tempValue, onValueChange = { tempValue = it }, label = { Text(title) }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = inputType))
	}, confirmButton = {
		TextButton(onClick = { onConfirm(tempValue) }, shapes = ButtonDefaults.shapes()) {
			Text(stringResource(R.string.confirm))
		}
	}, dismissButton = {
		Row{
			if (showClear && onClear != null) {
				TextButton(onClick = { onClear() }, shapes = ButtonDefaults.shapes()) {
					Text(stringResource(R.string.clear), color = MaterialTheme.colorScheme.error)
				}
			}
			TextButton(onClick = { onCancel() }, shapes = ButtonDefaults.shapes()) {
				Text(stringResource(R.string.cancel))
			}
		}
	})
}