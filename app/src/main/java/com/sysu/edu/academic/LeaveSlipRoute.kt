package com.sysu.edu.academic

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ElevatedFilterChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import com.sysu.edu.R
import com.sysu.edu.api.DateTimeManager
import com.sysu.edu.api.FileManager
import com.sysu.edu.browser.BrowserActivity
import com.sysu.edu.nav.navigateBack
import com.sysu.edu.view.ActivityPager
import com.sysu.edu.view.MenuItem
import com.sysu.edu.view.RowOrientation
import com.sysu.edu.view.SectionCard
import com.sysu.edu.view.SectionData
import com.sysu.edu.view.StaggerScreen
import com.sysu.edu.view.WarningCard
import com.sysu.edu.view.exportMarkdownMenuItem

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun LeaveSlipRoute(
	backStack: MutableList<NavKey>,
	sharedTransitionScope: SharedTransitionScope? = null,
	animatedVisibilityScope: AnimatedVisibilityScope? = null,
) {
	val viewModel: LeaveSlipViewModel = viewModel()
	val context = LocalContext.current
	val activity = LocalActivity.current
	val fileLauncher =
		rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
			if (result.resultCode == Activity.RESULT_OK) {
				result.data?.data?.let { uri ->
					viewModel.uploadAttachment(FileManager.getAttachmentRequestBody(context, uri))
				}
			}
		}

	var apply by rememberSaveable { mutableStateOf(false) }
	var fabExpanded by rememberSaveable { mutableStateOf(true) }
	val submitSuccess by viewModel.submitSuccess.observeAsState(initial = false)

	LaunchedEffect(Unit) {
		viewModel.fetchLeaveSlips()
		viewModel.fetchTerms()
	}
	LaunchedEffect(submitSuccess) {
		if (submitSuccess) {
			apply = false
		}
	}
	BackHandler(apply) {
		apply = false
	}
	ActivityPager(
		title = stringResource(R.string.leave_slip),
		floatingActionButton = {
			if (apply) {
				Column(
					modifier = Modifier.nestedScroll(rememberNestedScrollInteropConnection()),
					horizontalAlignment = Alignment.End,
					verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.vertical_margin))
				) {
					ExtendedFloatingActionButton(
						expanded = fabExpanded,
						onClick = { viewModel.reset() },
						text = { Text(stringResource(R.string.reset)) },
						icon = {
							Icon(
								imageVector = Icons.Rounded.Refresh,
								contentDescription = stringResource(R.string.reset)
							)
						})
					ExtendedFloatingActionButton(expanded = fabExpanded, onClick = {
						viewModel.submitLeaveSlip()
					}, icon = {
						Icon(
							imageVector = Icons.Default.Edit,
							contentDescription = stringResource(R.string.submit)
						)
					}, text = { Text(stringResource(R.string.submit)) })
				}
			} else {
				ExtendedFloatingActionButton(
					expanded = fabExpanded,
					onClick = {
						viewModel.resetSubmitSuccess()
						apply = true
					},
					modifier = Modifier.nestedScroll(rememberNestedScrollInteropConnection()),
					icon = {
						Icon(
							imageVector = Icons.Default.Edit,
							contentDescription = stringResource(R.string.ask_for_leave)
						)
					},
					text = { Text(stringResource(R.string.ask_for_leave)) })
			}
		},
		onNavigationClick = { if (apply) apply = false else backStack.navigateBack() },
		isNestedScrollEnabled = false,
		sharedTransitionScope = sharedTransitionScope,
		animatedVisibilityScope = animatedVisibilityScope,
		sharedKey = "LeaveSlip",
		topBarMenus = {
			listOf(
				exportMarkdownMenuItem(
					backStack,
					viewModel.sections,
					stringResource(R.string.leave_slip),
					stringResource(R.string.leave_slip)
				)
			)
		}) {
		if (apply) ApplyPage(viewModel, onUpload = {
			val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
				type = "*/*"
				addCategory(Intent.CATEGORY_OPENABLE)
				putExtra(
					Intent.EXTRA_MIME_TYPES,
					arrayOf("image/jpeg", "image/png", "image/gif", "application/pdf")
				)
			}
			fileLauncher.launch(intent)
		})
		else StaggerScreen(
			sections = viewModel.sections,
			onScrollBottom = {
				if (viewModel.hasMore) viewModel.fetchLeaveSlips()
			},
			onScrollTopChanged = { fabExpanded = it },
		)
	}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApplyPage(viewModel: LeaveSlipViewModel, onUpload: () -> Unit) {
	var leaveDays by rememberSaveable { mutableStateOf(viewModel.leaveDays) }
	var leaveReasonDescription by rememberSaveable { mutableStateOf(viewModel.leaveReasonDescription) }
	var leaveReason by rememberSaveable { mutableStateOf(viewModel.leaveReason) }
	var leaveReasonName by rememberSaveable { mutableStateOf(viewModel.leaveReasonName) }
	var startPeriod by rememberSaveable { mutableIntStateOf(viewModel.startPeriod) }
	var endPeriod by rememberSaveable { mutableIntStateOf(viewModel.endPeriod) }
	var showStartDatePicker by rememberSaveable { mutableStateOf(false) }
	var showEndDatePicker by rememberSaveable { mutableStateOf(false) }
	val leaveReasons = viewModel.leaveReasons
	var startMillis by rememberSaveable { mutableLongStateOf(viewModel.startMillis) }
	var endMillis by rememberSaveable { mutableLongStateOf(viewModel.endMillis) }
	val attachment by viewModel.attachment.observeAsState(null)
	val hasAttachment = attachment != null
	val context = LocalContext.current
	val days = leaveDays.toDoubleOrNull()
	val leaveTypeName = when {
		days == null -> ""
		days > 40 -> stringResource(R.string.retire_warning)
		days >= 8 -> stringResource(R.string.long_leave)
		else -> stringResource(R.string.short_leave)
	}
	val leaveType = remember(days) {
		when {
			days == null -> "1"
			days > 40 -> "3"
			days >= 8 -> "2"
			else -> "1"
		}
	}
	LaunchedEffect(Unit) {
		if (leaveReasons.isEmpty()) viewModel.fetchLeaveTypes()
	}
	LaunchedEffect(viewModel.resetTrigger) {
		leaveDays = viewModel.leaveDays
		leaveReasonDescription = viewModel.leaveReasonDescription
		leaveReason = viewModel.leaveReason
		leaveReasonName = viewModel.leaveReasonName
		startPeriod = viewModel.startPeriod
		endPeriod = viewModel.endPeriod
		startMillis = viewModel.startMillis
		endMillis = viewModel.endMillis
	}
	LaunchedEffect(
		leaveDays,
		leaveReasonDescription,
		leaveReason,
		leaveReasonName,
		startPeriod,
		endPeriod,
		startMillis,
		endMillis,
		leaveType,
		leaveTypeName
	) {
		viewModel.leaveDays = leaveDays
		viewModel.leaveReasonDescription = leaveReasonDescription
		viewModel.leaveReason = leaveReason
		viewModel.leaveReasonName = leaveReasonName
		viewModel.startPeriod = startPeriod
		viewModel.endPeriod = endPeriod
		viewModel.startMillis = startMillis
		viewModel.endMillis = endMillis
		viewModel.leaveType = leaveType
		viewModel.leaveTypeName = leaveTypeName
	}

	if (showStartDatePicker) {
		val startDatePickerState = rememberDatePickerState(initialSelectedDateMillis = startMillis)
		DatePickerDialog(onDismissRequest = { showStartDatePicker = false }, confirmButton = {
			TextButton(onClick = {
				startDatePickerState.selectedDateMillis?.let {
					startMillis = it
				}
				showStartDatePicker = false
			}) { Text(stringResource(R.string.confirm)) }
		}, dismissButton = {
			TextButton(onClick = {
				showStartDatePicker = false
			}) { Text(stringResource(R.string.cancel)) }
		}) {
			DatePicker(state = startDatePickerState)
		}
	}
	if (showEndDatePicker) {
		val endDatePickerState = rememberDatePickerState(initialSelectedDateMillis = endMillis)
		DatePickerDialog(onDismissRequest = { showEndDatePicker = false }, confirmButton = {
			TextButton(onClick = {
				endDatePickerState.selectedDateMillis?.let {
					endMillis = it
				}
				showEndDatePicker = false
			}) { Text(stringResource(R.string.confirm)) }
		}, dismissButton = {
			TextButton(onClick = {
				showEndDatePicker = false
			}) { Text(stringResource(R.string.cancel)) }
		}) {
			DatePicker(state = endDatePickerState)
		}
	}
	Column(
		modifier = Modifier
			.fillMaxSize()
			.verticalScroll(rememberScrollState())
			.nestedScroll(rememberNestedScrollInteropConnection())
			.padding(
				dimensionResource(R.dimen.horizontal_padding),
				dimensionResource(R.dimen.vertical_padding)
			),
		verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.vertical_margin))
	) {
		WarningCard()
		OutlinedTextField(
			modifier = Modifier.fillMaxWidth(),
			value = leaveDays,
			singleLine = true,
			suffix = { Text(stringResource(R.string.day)) },
			leadingIcon = {
				Icon(
					imageVector = Icons.Default.CalendarMonth,
					contentDescription = stringResource(R.string.day)
				)
			},
			onValueChange = { input ->
				val filtered = input.filter { c -> c.isDigit() || c == '.' }
				leaveDays = if (filtered.indexOf('.') != filtered.lastIndexOf('.')) {
					val idx = filtered.lastIndexOf('.')
					filtered.substring(0, idx) + filtered.substring(idx + 1)
				} else filtered
			},
			label = { Text(stringResource(R.string.leave_day)) },
			supportingText = { Text(leaveTypeName) },
			keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
		)

		OutlinedTextField(
			modifier = Modifier.fillMaxWidth(),
			value = leaveReasonDescription,
			onValueChange = { leaveReasonDescription = it },
			label = { Text(stringResource(R.string.leave_reason)) },
			leadingIcon = {
				Icon(
					imageVector = Icons.Default.Edit,
					contentDescription = stringResource(R.string.leave_reason)
				)
			})
		FlowRow(horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.horizontal_margin))) {
			AssistChip(label = { Text(stringResource(R.string.leave_reason)) }, onClick = { })
			leaveReasons.forEach { item ->
				val dataNumber = item.getString("dataNumber")
				val dataName = item.getString("dataName")
				val isSelected = dataNumber == leaveReason
				ElevatedFilterChip(
					onClick = {
						leaveReason = if (isSelected) null else dataNumber
						leaveReasonName = if (isSelected) null else dataName
					},
					label = {
						Text(dataName)
					},
					selected = isSelected,
					leadingIcon = if (isSelected) {
						{
							Icon(
								imageVector = Icons.Filled.Done,
								contentDescription = "Checked",
								modifier = Modifier.size(FilterChipDefaults.IconSize)
							)
						}
					} else null,
				)
			}
		}
		Row(
			modifier = Modifier.fillMaxWidth(),
			horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.horizontal_margin)),
			verticalAlignment = Alignment.CenterVertically
		) {
			OutlinedTextField(
				modifier = Modifier.weight(1f),
				value = DateTimeManager.toDateString(startMillis) ?: "",
				onValueChange = {},
				readOnly = true,
				singleLine = true,
				label = { Text(stringResource(R.string.start_time)) },
				leadingIcon = {
					IconButton(onClick = { showStartDatePicker = true }) {
						Icon(
							imageVector = Icons.Default.CalendarMonth,
							contentDescription = stringResource(R.string.start_time)
						)
					}
				},
			)
			SingleChoiceSegmentedButtonRow {
				listOf(R.string.morning, R.string.afternoon).forEachIndexed { index, label ->
					SegmentedButton(
						shape = SegmentedButtonDefaults.itemShape(index = index, count = 2),
						onClick = { startPeriod = index },
						selected = index == startPeriod,
					) {
						Text(stringResource(label))
					}
				}
			}
		}
		Row(
			modifier = Modifier.fillMaxWidth(),
			horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.horizontal_margin)),
			verticalAlignment = Alignment.CenterVertically
		) {
			OutlinedTextField(
				modifier = Modifier.weight(1f),
				value = DateTimeManager.toDateString(endMillis) ?: "",
				onValueChange = {},
				readOnly = true,
				singleLine = true,
				label = { Text(stringResource(R.string.end_time)) },
				leadingIcon = {
					IconButton(onClick = { showEndDatePicker = true }) {
						Icon(
							imageVector = Icons.Default.CalendarMonth,
							contentDescription = stringResource(R.string.end_time)
						)
					}
				},
			)
			SingleChoiceSegmentedButtonRow {
				listOf(R.string.morning, R.string.afternoon).forEachIndexed { index, label ->
					SegmentedButton(
						shape = SegmentedButtonDefaults.itemShape(index = index, count = 2),
						onClick = { endPeriod = index },
						selected = index == endPeriod,
					) {
						Text(stringResource(label))
					}
				}
			}
		}
		val upload = stringResource(R.string.upload)
		val delete = stringResource(R.string.delete)
		val preview = stringResource(R.string.preview)
		SectionCard(
			section = SectionData(
				title = stringResource(R.string.attachment),
				rows = viewModel.attachmentRows,
				rowOrientation = RowOrientation.Vertical,
				footerMenus = remember(attachment) {
					mutableStateListOf(
						MenuItem(
							upload,
							enabled = !hasAttachment
						) { onUpload(); true },
						MenuItem(
							delete,
							enabled = hasAttachment
						) { viewModel.deleteAttachment(); true },
						MenuItem(preview, enabled = hasAttachment) {
							context.startActivity(
								Intent(context, BrowserActivity::class.java).setData(
									"https://jwxt.sysu.edu.cn/jwxt/reports-register/askLeaveAgg/downloadFile?filePath=${
										attachment?.getString(
											"filePath"
										)
									}&fileName=${
										attachment?.getString("fileName")
									}".toUri()
								)
							)
							true
						})
				})
		)
		Text("文件类型：jpg,jpeg,png,gif,pdf", style = MaterialTheme.typography.labelMedium)
	}
}
