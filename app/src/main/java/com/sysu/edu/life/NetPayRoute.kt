package com.sysu.edu.life

import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.rounded.AttachMoney
import androidx.compose.material.icons.rounded.Web
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
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult.ActionPerformed
import androidx.compose.material3.SnackbarResult.Dismissed
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import com.sysu.edu.R
import com.sysu.edu.nav.navigateBack
import com.sysu.edu.view.ActivityPager
import com.sysu.edu.view.KeyValueRow
import com.sysu.edu.view.MenuItem
import com.sysu.edu.view.RowData
import com.sysu.edu.view.StaggerScreen

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun NetPayRoute(
	backStack: MutableList<NavKey>,
	sharedTransitionScope: SharedTransitionScope? = null,
	animatedVisibilityScope: AnimatedVisibilityScope? = null,
) {
	val viewModel: NetPayViewModel = viewModel()
	val activity = LocalActivity.current
	val snackbar = remember { SnackbarHostState() }
	LaunchedEffect(
		viewModel.snackbarMessage
	) {
		if (!viewModel.snackbarMessage.isNullOrEmpty()) {
			when (snackbar.showSnackbar(
				viewModel.snackbarMessage ?: "", viewModel.snackbarActionLabel, true
			)) {
				ActionPerformed -> viewModel.snackbarAction?.let { it() }
				Dismissed -> {
					viewModel.clearSnackbar()
				}
			}
		}
	}

	ActivityPager(
		snackbar = snackbar,
		onNavigationClick = { backStack.navigateBack(activity) },
		title = stringResource(R.string.net_manager),
		navs = listOf(
			MenuItem(stringResource(R.string.order), Icons.Rounded.AttachMoney),
			MenuItem(stringResource(R.string.status), Icons.Rounded.Web),
		),
		isNestedScrollEnabled = false,
		sharedTransitionScope = sharedTransitionScope,
		animatedVisibilityScope = animatedVisibilityScope,
		sharedKey = "NetPay"
	) {
		StaggerScreen(
			sections = when (it) {
				0 -> viewModel.orderSections
				1 -> viewModel.statusSections
				else -> viewModel.orderSections
			}
		)
	}
	NetPayDialog(viewModel)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetPayDialog(viewModel: NetPayViewModel) {
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
					KeyValueRow(
						RowData(
							stringResource(R.string.new_out_date),
							viewModel.newOutDateStr
						)
					)
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
						.padding(
							dimensionResource(R.dimen.horizontal_padding),
							dimensionResource(R.dimen.vertical_padding)
						),
				) {
					Text(stringResource(R.string.submit))
				}
			}
		}
	}
}

@Composable
private fun TimeDropdown(
	selectedIndex: Int,
	options: List<String>,
	onSelect: (Int) -> Unit,
) {
	var expanded by remember { mutableStateOf(false) }
	val selectedText =
		if (selectedIndex >= 0 && selectedIndex < options.size) options[selectedIndex]
		else stringResource(R.string.click_to_select)

	Column {
		Row(
			modifier = Modifier
				.fillMaxWidth()
				.clickable { expanded = true }
				.padding(
					dimensionResource(R.dimen.horizontal_padding),
					dimensionResource(R.dimen.vertical_padding)
				),
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