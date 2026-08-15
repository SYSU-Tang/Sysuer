package com.sysu.edu.academic

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
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
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import com.sysu.edu.BaseActivity
import com.sysu.edu.R
import com.sysu.edu.api.CalendarManager
import com.sysu.edu.api.DataStoreManager
import com.sysu.edu.api.FileManager
import com.sysu.edu.browser.RichTextActivity
import com.sysu.edu.view.ActivityPager
import com.sysu.edu.view.MenuItem
import com.sysu.edu.view.RowOrientation
import com.sysu.edu.view.SectionCard
import com.sysu.edu.view.SectionData
import com.sysu.edu.view.StaggerScreen
import com.sysu.edu.view.WarningCard
import com.sysu.edu.view.toMarkdown

class LeaveSlipActivity : BaseActivity() {
	private val viewModel: LeaveSlipViewModel by viewModels()
	private val fileLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult? ->
		if (result?.resultCode == RESULT_OK) {
			result.data?.data?.let { uri ->
				viewModel.uploadAttachment(FileManager.getAttachmentRequestBody(this, uri))
			}
		}
	}
	
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		enableEdgeToEdge()
		
		setContent {
			var apply by rememberSaveable { mutableStateOf(false) }
			LaunchedEffect(Unit) { viewModel.fetchLeaveSlips() }
			BackHandler {
				if (apply) apply = false
				else supportFinishAfterTransition()
			}
			ActivityPager(title = stringResource(R.string.leave_slip), floatingActionButton = {
				ExtendedFloatingActionButton(onClick = { apply = true },
				                             modifier = Modifier.nestedScroll(rememberNestedScrollInteropConnection()),
				                             icon = { Icon(imageVector = Icons.Default.Edit, contentDescription = stringResource(R.string.ask_for_leave)) },
				                             text = { Text(stringResource(R.string.ask_for_leave)) })
			}, onNavigationClick = { if (apply) apply = false else supportFinishAfterTransition() }, isNestedScrollEnabled = false, actions = {
				IconButton(onClick = {
					val markdown = viewModel.sections.toMarkdown()
					DataStoreManager.saveContent(this@LeaveSlipActivity, getString(R.string.result), markdown) {
						startActivity(Intent(this@LeaveSlipActivity, RichTextActivity::class.java).putExtra("type", DataStoreManager.ContentType.MARKDOWN.name).putExtra("title", getString(R.string.result)))
					}
				}) {
					Icon(painter = painterResource(R.drawable.export), contentDescription = stringResource(R.string.export))
				}
			}) {
				if (apply) ApplyPage(viewModel, onUpload = {
					val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
						type = "*/*"
						addCategory(Intent.CATEGORY_OPENABLE)
						putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("image/jpeg", "image/png", "image/gif", "application/pdf"))
					}
					fileLauncher.launch(intent)
				})
				else StaggerScreen(
					sections = viewModel.sections,
					onScrollBottom = {
						if (viewModel.hasMore) viewModel.fetchLeaveSlips()
					},
				                  )
			}
		}
	}
}

@OptIn(ExperimentalMaterial3Api::class) @Composable fun ApplyPage(viewModel: LeaveSlipViewModel, onUpload: () -> Unit) {
	var leaveDays by rememberSaveable { mutableStateOf("") }
	val leaveReasonDescription = rememberSaveable { mutableStateOf("") }
	var selectedType by rememberSaveable { mutableStateOf<String?>(null) }
	var selectedStartPeriod by rememberSaveable { mutableIntStateOf(0) }
	var selectedEndPeriod by rememberSaveable { mutableIntStateOf(0) }
	var showStartDatePicker by rememberSaveable { mutableStateOf(false) }
	var showEndDatePicker by rememberSaveable { mutableStateOf(false) }
	val leaveTypes = viewModel.types
	val calendarManager = CalendarManager()
	var startMillis by rememberSaveable { mutableLongStateOf(System.currentTimeMillis()) }
	var endMillis by rememberSaveable { mutableLongStateOf(System.currentTimeMillis()) }
	val hasSelectedAttachment = viewModel.selectedAttachmentIndex >= 0
	val days = leaveDays.toDoubleOrNull()
	val leaveType = when {
		days == null -> ""
		days > 40 -> stringResource(R.string.retire_warning)
		days >= 8 -> stringResource(R.string.long_leave)
		else -> stringResource(R.string.short_leave)
	}
	LaunchedEffect(Unit) {
		if (leaveTypes.isEmpty()) viewModel.fetchLeaveTypes()
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
			TextButton(onClick = { showStartDatePicker = false }) { Text(stringResource(R.string.cancel)) }
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
			TextButton(onClick = { showEndDatePicker = false }) { Text(stringResource(R.string.cancel)) }
		}) {
			DatePicker(state = endDatePickerState)
		}
	}
	Column(modifier = Modifier
		.fillMaxSize()
		.verticalScroll(rememberScrollState())
		.nestedScroll(rememberNestedScrollInteropConnection())
		.padding(dimensionResource(R.dimen.horizontal_padding), dimensionResource(R.dimen.vertical_padding)),
	       verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.vertical_margin))) {
		WarningCard()
		OutlinedTextField(modifier = Modifier.fillMaxWidth(),
		                  value = leaveDays,
		                  singleLine = true,
		                  suffix = { Text(stringResource(R.string.day)) },
		                  leadingIcon = { Icon(imageVector = Icons.Default.CalendarMonth, contentDescription = stringResource(R.string.day)) },
		                  onValueChange = { input ->
			                  val filtered = input.filter { c -> c.isDigit() || c == '.' }
			                  leaveDays = if (filtered.indexOf('.') != filtered.lastIndexOf('.')) {
				                  val idx = filtered.lastIndexOf('.')
				                  filtered.substring(0, idx) + filtered.substring(idx + 1)
			                  }
			                  else filtered
		                  },
		                  label = { Text(stringResource(R.string.leave_day)) },
		                  supportingText = { Text(leaveType) },
		                  keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
		
		OutlinedTextField(modifier = Modifier.fillMaxWidth(),
		                  value = leaveReasonDescription.value,
		                  onValueChange = { leaveReasonDescription.value = it },
		                  label = { Text(stringResource(R.string.leave_reason)) },
		                  leadingIcon = { Icon(imageVector = Icons.Default.Edit, contentDescription = stringResource(R.string.leave_reason)) })
		FlowRow(horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.horizontal_margin))) {
			AssistChip(label = { Text(stringResource(R.string.leave_type)) }, onClick = { })
			leaveTypes.forEach { item ->
				val dataNumber = item.getString("dataNumber")
				val isSelected = dataNumber == selectedType
				ElevatedFilterChip(
					onClick = { selectedType = if (isSelected) null else dataNumber },
					label = {
						Text(item.getString("dataName"))
					},
					selected = isSelected,
					leadingIcon = if (isSelected) {
						{
							Icon(imageVector = Icons.Filled.Done, contentDescription = "Checked", modifier = Modifier.size(FilterChipDefaults.IconSize))
						}
					}
					else null,
				                  )
			}
		}
		Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.horizontal_margin)), verticalAlignment = Alignment.CenterVertically) {
			OutlinedTextField(
				modifier = Modifier.weight(1f),
				value = calendarManager.toDateString(startMillis) ?: "",
				onValueChange = {},
				readOnly = true,
				singleLine = true,
				label = { Text(stringResource(R.string.start_time)) },
				leadingIcon = {
					IconButton(onClick = { showStartDatePicker = true }) {
						Icon(imageVector = Icons.Default.CalendarMonth, contentDescription = stringResource(R.string.start_time))
					}
				},
			                 )
			SingleChoiceSegmentedButtonRow {
				listOf(R.string.morning, R.string.afternoon).forEachIndexed { index, label ->
					SegmentedButton(
						shape = SegmentedButtonDefaults.itemShape(index = index, count = 2),
						onClick = { selectedStartPeriod = index },
						selected = index == selectedStartPeriod,
					               ) {
						Text(stringResource(label))
					}
				}
			}
		}
		Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.horizontal_margin)), verticalAlignment = Alignment.CenterVertically) {
			OutlinedTextField(
				modifier = Modifier.weight(1f),
				value = calendarManager.toDateString(endMillis) ?: "",
				onValueChange = {},
				readOnly = true,
				singleLine = true,
				label = { Text(stringResource(R.string.end_time)) },
				leadingIcon = {
					IconButton(onClick = { showEndDatePicker = true }) {
						Icon(imageVector = Icons.Default.CalendarMonth, contentDescription = stringResource(R.string.end_time))
					}
				},
			                 )
			SingleChoiceSegmentedButtonRow {
				listOf(R.string.morning, R.string.afternoon).forEachIndexed { index, label ->
					SegmentedButton(
						shape = SegmentedButtonDefaults.itemShape(index = index, count = 2),
						onClick = { selectedEndPeriod = index },
						selected = index == selectedEndPeriod,
					               ) {
						Text(stringResource(label))
					}
				}
			}
		}
		val upload = stringResource(R.string.upload)
		val delete = stringResource(R.string.delete)
		val preview = stringResource(R.string.preview)
		SectionCard(section = SectionData(title = stringResource(R.string.attachment), rows = viewModel.attachmentRows, rowOrientation = RowOrientation.Vertical, footerMenus = remember(hasSelectedAttachment) {
			mutableStateListOf(MenuItem(upload) { onUpload(); true }, MenuItem(delete, enabled = hasSelectedAttachment) { true }, MenuItem(preview, enabled = hasSelectedAttachment) {
				viewModel.deleteAttachment()
				true
			})
		}))
		Text("文件类型：jpg,jpeg,png,gif,pdf", style = MaterialTheme.typography.labelMedium)
	}
}