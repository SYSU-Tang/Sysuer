package com.sysu.edu.life

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.sysu.edu.R
import com.sysu.edu.view.KeyValueRow
import com.sysu.edu.view.RowData
import com.sysu.edu.view.StaggerScreen

@Composable fun NetOrderPage(viewModel: NetPayViewModel) {
	StaggerScreen(viewModel.orderSections)
}

@Composable fun NetStatusPage(viewModel: NetPayViewModel) {
	StaggerScreen(viewModel.statusSections)
}

@OptIn(ExperimentalMaterial3Api::class) @Composable fun NetPayDialog(viewModel: NetPayViewModel) {
	val sheetState = rememberBottomSheetState(SheetValue.Hidden)
	
	if (viewModel.showPayDialog) {
		ModalBottomSheet(
			onDismissRequest = { viewModel.closePayDialog() },
			sheetState = sheetState,
			dragHandle = { BottomSheetDefaults.DragHandle() },
		                ) {
			Column(
				modifier = Modifier.fillMaxWidth(),
				verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.vertical_margin)),
			      ) {
				KeyValueRow(RowData(stringResource(R.string.service), viewModel.serviceName))
				KeyValueRow(RowData(stringResource(R.string.old_out_date), viewModel.oldDateStr))
				if (viewModel.timeIndex >= 0) {
					KeyValueRow(RowData(stringResource(R.string.new_out_date), viewModel.newOutDateStr))
					KeyValueRow(RowData(stringResource(R.string.fee), "¥${viewModel.fee}"))
				}
				TimeDropdown(
					selectedIndex = viewModel.timeIndex,
					options = viewModel.timeOptions,
					onSelect = { viewModel.selectTime(it) },
				            )
				FilledTonalButton(
					onClick = { viewModel.submitOrder() },
					enabled = viewModel.timeIndex >= 0,
					shapes = ButtonDefaults.shapes(),
					modifier = Modifier
						.fillMaxWidth()
						.padding(dimensionResource(R.dimen.horizontal_padding), dimensionResource(R.dimen.vertical_padding)),
				                 ) {
					Text(stringResource(R.string.submit))
				}
			}
		}
	}
}

@Composable private fun TimeDropdown(
	selectedIndex: Int,
	options: List<String>,
	onSelect: (Int) -> Unit,
                                    ) {
	var expanded by remember { mutableStateOf(false) }
	val selectedText = if (selectedIndex >= 0 && selectedIndex < options.size) options[selectedIndex]
	else stringResource(R.string.click_to_select)
	
	Column {
		Row(
			modifier = Modifier
				.fillMaxWidth()
				.clickable { expanded = true }
				.padding(dimensionResource(R.dimen.horizontal_padding), dimensionResource(R.dimen.vertical_padding)),
			horizontalArrangement = Arrangement.SpaceBetween,
			verticalAlignment = Alignment.CenterVertically,
		   ) {
			Text(
				text = stringResource(R.string.time),
				style = MaterialTheme.typography.titleMedium,
				color = MaterialTheme.colorScheme.onSurfaceVariant,
			    )
			Row(verticalAlignment = Alignment.CenterVertically) {
				Text(
					text = selectedText,
					style = MaterialTheme.typography.bodyLarge,
					fontWeight = FontWeight.Medium,
					color = if (selectedIndex >= 0) MaterialTheme.colorScheme.primary
					else MaterialTheme.colorScheme.onSurfaceVariant,
				    )
				Spacer(Modifier.width(dimensionResource(R.dimen.icon_text_gap)))
				Icon(
					imageVector = if (expanded) Icons.Default.KeyboardArrowUp
					else Icons.Default.KeyboardArrowDown,
					contentDescription = null,
					tint = MaterialTheme.colorScheme.onSurfaceVariant,
				    )
			}
		}
		Box(modifier = Modifier.align(Alignment.End)) {
			DropdownMenu(
				expanded = expanded,
				onDismissRequest = { expanded = false },
			            ) {
				options.forEachIndexed { index, option ->
					DropdownMenuItem(
						text = {
							Text(
								text = option,
								color = if (index == selectedIndex) MaterialTheme.colorScheme.primary
								else MaterialTheme.colorScheme.onSurface,
							    )
						},
						onClick = {
							onSelect(index)
							expanded = false
						},
					                )
				}
			}
		}
	}
}

@Composable fun NetPaySnackbar(viewModel: NetPayViewModel) {
	val message = viewModel.snackbarMessage
	val actionLabel = viewModel.snackbarActionLabel
	val action = viewModel.snackbarAction
	
	if (message != null) {
		Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
			Snackbar(
				modifier = Modifier.padding(dimensionResource(R.dimen.horizontal_padding), dimensionResource(R.dimen.vertical_padding)),
				action = {
					if (actionLabel != null && action != null) {
						TextButton(
							onClick = {
								action()
								viewModel.clearSnackbar()
							},
						          ) {
							Text(actionLabel)
						}
					}
				},
				dismissAction = {
					TextButton(onClick = { viewModel.clearSnackbar() }) {
						Text(stringResource(R.string.close))
					}
				},
			        ) {
				Text(text = message)
			}
		}
	}
}