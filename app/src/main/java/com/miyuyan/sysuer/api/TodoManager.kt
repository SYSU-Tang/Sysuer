package com.miyuyan.sysuer.api

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ColorLens
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.PriorityHigh
import androidx.compose.material.icons.rounded.SubdirectoryArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DisplayMode
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedAssistChip
import androidx.compose.material3.ElevatedFilterChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDialog
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.ViewModelProvider
import com.alibaba.fastjson2.JSONArray
import com.alibaba.fastjson2.JSONObject
import com.miyuyan.sysuer.R
import com.miyuyan.sysuer.theme.ExpressiveShapes
import com.miyuyan.sysuer.todo.TodoDatabase
import com.miyuyan.sysuer.todo.TodoEntity
import com.miyuyan.sysuer.todo.TodoInfo
import com.miyuyan.sysuer.todo.TodoModel
import com.miyuyan.sysuer.todo.TodoModelFactory
import com.miyuyan.sysuer.todo.TodoReminderReceiver
import com.miyuyan.sysuer.todo.TodoRepository
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class TodoManager(
	private val context: Context,
	private val lifecycleScope: LifecycleCoroutineScope,
                 ) {
	val todoModel: TodoModel by lazy {
		val repository = TodoRepository(TodoDatabase.getDatabase(context, lifecycleScope).todoDao())
		ViewModelProvider(context as androidx.fragment.app.FragmentActivity, TodoModelFactory(repository))[TodoModel::class.java]
	}
	private val colors = listOf("#757575", "#F44336", "#E91E63", "#9C27B0", "#673AB7", "#3F51B5", "#2196F3", "#03A9F4", "#00BCD4", "#009688", "#4CAF50", "#8BC34A", "#CDDC39", "#FFEB3B", "#FFC107", "#FF9800", "#FF5722", "#795548", "#607D8B")
	private val priorityLabels = listOf("无", "不重要且不紧急", "不重要但紧急", "重要但不紧急", "重要且紧急")
	var refreshListener: (() -> Unit)? = null
	fun init() {
		todoModel.loadTypes()
		todoModel.loadSubjects()
		todoModel.loadTags()
	}
	
	fun refresh(where: String, args: Array<String>) {
		todoModel.loadTodos(where, args)
	}
	
	fun performRefresh() {
		refreshListener?.invoke()
	}
	
	fun addTodo(todo: TodoEntity, onSuccess: () -> Unit = {}) {
		lifecycleScope.launch {
			val id = todoModel.addTodo(todo).await().toInt()
			val saved = todo.copy(id = id)
			scheduleReminder(saved)
			onSuccess()
			performRefresh()
		}
	}
	
	fun updateTodo(todo: TodoEntity, onSuccess: () -> Unit = {}) {
		lifecycleScope.launch {
			todoModel.updateTodo(todo).join()
			scheduleReminder(todo)
			onSuccess()
			performRefresh()
		}
	}
	
	fun deleteTodo(id: Int, onSuccess: () -> Unit = {}) {
		lifecycleScope.launch {
			cancelReminder(id)
			todoModel.deleteTodo(id).join()
			onSuccess()
			performRefresh()
		}
	}
	
	fun toggleTodoStatus(todo: TodoEntity) {
		val isDone = todo.status == TodoInfo.DONE
		todo.status = if (isDone) TodoInfo.TODO else TodoInfo.DONE
		if (!isDone) {
			todo.doneDateTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
			cancelReminder(todo.id)
		}
		else {
			scheduleReminder(todo)
		}
		lifecycleScope.launch {
			todoModel.updateTodo(todo).join()
			performRefresh()
		}
	}
	
	fun addType(name: String) {
		lifecycleScope.launch { todoModel.addType(name) }
	}
	
	fun addSubject(name: String) {
		lifecycleScope.launch { todoModel.addSubject(name) }
	}
	
	fun addTag(name: String) {
		lifecycleScope.launch { todoModel.addTag(name) }
	}
	
	fun deleteType(name: String) {
		lifecycleScope.launch { todoModel.deleteType(name) }
	}
	
	fun deleteSubject(name: String) {
		lifecycleScope.launch { todoModel.deleteSubject(name) }
	}
	
	fun deleteTag(name: String) {
		lifecycleScope.launch { todoModel.deleteTag(name) }
	}
	
	private fun scheduleReminder(todo: TodoEntity) {
		scheduleSingleReminder(todo.id, todo.title, todo.description, todo.status, todo.dueDate, todo.dueTime, todo.remindTime)
		scheduleSingleReminder(todo.id + 1000000, "[DDL] ${todo.title}", todo.description, todo.status, todo.ddl, todo.ddlTime, todo.ddlRemindTime)
	}
	
	private fun scheduleSingleReminder(
		notificationId: Int,
		title: String?,
		description: String?,
		status: Int,
		date: String?,
		time: String?,
		remindStr: String?,
	                                  ) {
		if (status == TodoInfo.DONE || date.isNullOrEmpty() || time.isNullOrEmpty() || remindStr.isNullOrEmpty()) {
			cancelSingleReminder(notificationId)
			return
		}
		try {
			val dueDate = LocalDate.parse(date)
			val dueTime = LocalTime.parse(time)
			var remindDateTime = LocalDateTime.of(dueDate, dueTime)
			val remindMinutes = parseRemindMinutes(remindStr)
			remindDateTime = remindDateTime.minusMinutes(remindMinutes.toLong())
			if (remindDateTime.isBefore(LocalDateTime.now())) return
			val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
			val intent = Intent(context, TodoReminderReceiver::class.java).apply {
				putExtra("todo_id", notificationId)
				putExtra("notification_id", notificationId)
				putExtra("todo_title", title)
				putExtra("todo_description", description)
			}
			val pendingIntent = PendingIntent.getBroadcast(context, notificationId, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
			val triggerAtMillis = remindDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
			alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
		} catch (_: Exception) {
		}
	}
	
	private fun parseRemindMinutes(remindStr: String): Int = when (remindStr) {
		context.getString(R.string.on_time) -> 0
		context.getString(R.string.five_mins) -> 5
		context.getString(R.string.fifteen_mins) -> 15
		context.getString(R.string.half_hour) -> 30
		context.getString(R.string.one_hour) -> 60
		context.getString(R.string.one_day) -> 1440
		else -> {
			val minuteStr = context.getString(R.string.minute)
			if (remindStr.endsWith(minuteStr)) {
				remindStr.replace(minuteStr, "").toIntOrNull() ?: 0
			}
			else 0
		}
	}
	
	private fun cancelReminder(id: Int) {
		cancelSingleReminder(id)
		cancelSingleReminder(id + 1000000)
	}
	
	private fun cancelSingleReminder(notificationId: Int) {
		val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
		val intent = Intent(context, TodoReminderReceiver::class.java)
		val pendingIntent = PendingIntent.getBroadcast(context, notificationId, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_NO_CREATE)
		if (pendingIntent != null) {
			alarmManager.cancel(pendingIntent)
			pendingIntent.cancel()
		}
	}
	
	@OptIn(ExperimentalMaterial3Api::class) @Composable fun TodoDetailDialog(
		initialTodo: TodoEntity = TodoEntity(),
		isAdd: Boolean = true,
		onDismiss: () -> Unit,
		onConfirm: (TodoEntity) -> Unit,
		onDelete: (() -> Unit)? = null,
	                                                                        ) {
		val types by todoModel.types.observeAsState(emptyList())
		val subjects by todoModel.subjects.observeAsState(emptyList())
		val tags by todoModel.tags.observeAsState(emptyList())
		var title by remember { mutableStateOf(initialTodo.title ?: "") }
		var description by remember { mutableStateOf(initialTodo.description ?: "") }
		var priority by remember { mutableIntStateOf(initialTodo.priority.coerceIn(0, 4)) }
		var todoType by remember { mutableStateOf(initialTodo.todoType) }
		var subject by remember { mutableStateOf(initialTodo.subject) }
		var selectedTags = initialTodo.tag.map { it as String }.toMutableStateList()
		var isDone by remember { mutableStateOf(initialTodo.status == TodoInfo.DONE) }
		var dueDate by remember { mutableStateOf(initialTodo.dueDate) }
		var dueTime by remember { mutableStateOf(initialTodo.dueTime) }
		var remindTime by remember { mutableStateOf(initialTodo.remindTime) }
		var ddl by remember { mutableStateOf(initialTodo.ddl) }
		var ddlTime by remember { mutableStateOf(initialTodo.ddlTime) }
		var ddlRemindTime by remember { mutableStateOf(initialTodo.ddlRemindTime) }
		var selectedColor by remember { mutableStateOf(initialTodo.color) }
		var location by remember { mutableStateOf(initialTodo.location) }
		val subtasks = remember { mutableStateListOf<JSONObject>().apply { addAll(initialTodo.subtask.map { it as JSONObject }) } }
		var showDatePicker by remember { mutableStateOf(false) }
		var showTimePicker by remember { mutableStateOf(false) }
		var dateTarget by remember { mutableStateOf("") }
		var showAddDialog by remember { mutableStateOf(false) }
		var addCode by remember { mutableIntStateOf(0) }
		var newItemName by remember { mutableStateOf("") }
		var showSubtaskDialog by remember { mutableStateOf(false) }
		var showRemindMenu by remember { mutableStateOf(false) }
		var remindTarget by remember { mutableStateOf("") }
		var showCustomRemindDialog by remember { mutableStateOf(false) }
		val modifier = Modifier
			.fillMaxWidth()
			.clip(ExpressiveShapes.medium)
			.background(MaterialTheme.colorScheme.surfaceContainer)
			.padding(dimensionResource(R.dimen.horizontal_padding), dimensionResource(R.dimen.vertical_padding))
		if (showAddDialog) {
			AddItemDialog(title = stringResource(when (addCode) {
				                                     0 -> R.string.type
				                                     1 -> R.string.subject
				                                     else -> R.string.tag
			                                     }), value = newItemName, onValueChange = { newItemName = it }, onDismiss = { showAddDialog = false; newItemName = "" }, onConfirm = {
				if (it.isNotEmpty()) when (addCode) {
					0 -> {
						addType(it); todoType = it
					}
					1 -> {
						addSubject(it); subject = it
					}
					2 -> {
						addTag(it); if (it !in selectedTags) selectedTags.add(it)
					}
				}
				showAddDialog = false
				newItemName = ""
			})
		}
		
		if (showSubtaskDialog) {
			var subtaskTitle by remember { mutableStateOf("") }
			AlertDialog(onDismissRequest = { showSubtaskDialog = false }, title = { Text(stringResource(R.string.subtask)) }, text = {
				OutlinedTextField(value = subtaskTitle, onValueChange = { subtaskTitle = it }, label = { Text(stringResource(R.string.title)) })
			}, confirmButton = {
				TextButton(onClick = {
					if (subtaskTitle.isNotEmpty()) {
						subtasks.add(JSONObject.of("title", subtaskTitle, "status", TodoInfo.TODO))
					}
					showSubtaskDialog = false
				}, shapes = ButtonDefaults.shapes()) { Text(stringResource(R.string.confirm)) }
			}, dismissButton = {
				TextButton(onClick = { showSubtaskDialog = false }, shapes = ButtonDefaults.shapes()) { Text(stringResource(R.string.cancel)) }
			})
		}
		
		if (showCustomRemindDialog) {
			var customMinutes by remember { mutableIntStateOf(0) }
			AlertDialog(onDismissRequest = { showCustomRemindDialog = false }, title = { Text(stringResource(R.string.custom_remind_title)) }, text = {
				Column {
					Text("$customMinutes ${stringResource(R.string.minute)}")
					Slider(value = customMinutes.toFloat(), onValueChange = { customMinutes = it.toInt() }, valueRange = 0f..59f, steps = 58,
					   thumb ={
						   Box(modifier = Modifier
							   .size(width = 4.dp, height = 24.dp)
							   .clip(RoundedCornerShape(50))
							   .background(MaterialTheme.colorScheme.primary))
					   })
				}
			}, confirmButton = {
				TextButton(onClick = {
					val result = String.format(Locale.getDefault(), "%02d%s", customMinutes, context.getString(R.string.minute))
					when (remindTarget) {
						"due" -> remindTime = result
						"ddl" -> ddlRemindTime = result
					}
					showCustomRemindDialog = false
				}, shapes = ButtonDefaults.shapes()) { Text(stringResource(R.string.confirm)) }
			}, dismissButton = {
				TextButton(onClick = { showCustomRemindDialog = false }, shapes = ButtonDefaults.shapes()) { Text(stringResource(R.string.cancel)) }
			})
		}
		
		if (showDatePicker) {
			val state = rememberDatePickerState(initialSelectedDateMillis = when (dateTarget) {
				"due" -> dueDate?.let {
					DateTimeManager.toMillis(it) }
				"ddl" -> ddl?.let { DateTimeManager.toMillis(it) }
				else -> null
			}, initialDisplayMode = DisplayMode.Picker)
			DatePickerDialog(onDismissRequest = { showDatePicker = false }, confirmButton = {
				TextButton(onClick = {
					state.selectedDateMillis?.let { millis ->
						val dateStr = DateTimeManager.toDateString(millis)
						when (dateTarget) {
							"due" -> dueDate = dateStr
							"ddl" -> ddl = dateStr
						}
					}
					showDatePicker = false
				}, shapes = ButtonDefaults.shapes()) { Text(stringResource(R.string.confirm)) }
			}, dismissButton = {
				TextButton(onClick = { showDatePicker = false }, shapes = ButtonDefaults.shapes()) { Text(stringResource(R.string.cancel)) }
			}) {
				DatePicker(state = state)
			}
		}
		
		if (showTimePicker) {
			val parsedTime = when (dateTarget) {
				"due" -> dueTime
				"ddl" -> ddlTime
				else -> null
			}
			val (h, m) = parsedTime?.split(":")?.let {
				(it.getOrNull(0)?.toIntOrNull() ?: 0) to (it.getOrNull(1)?.toIntOrNull() ?: 0)
			} ?: (0 to 0)
			val timePickerState = rememberTimePickerState(initialHour = h, initialMinute = m, is24Hour = true)
			TimePickerDialog(
				onDismissRequest = { showTimePicker = false },
				title = { Text(stringResource(R.string.time)) },
				confirmButton = {
					TextButton(onClick = {
						val timeStr = String.format(Locale.getDefault(), "%02d:%02d", timePickerState.hour, timePickerState.minute)
						when (dateTarget) {
							"due" -> dueTime = timeStr
							"ddl" -> ddlTime = timeStr
						}
						showTimePicker = false
					}, shapes = ButtonDefaults.shapes()) { Text(stringResource(R.string.confirm)) }
				}, dismissButton = {
					TextButton(onClick = { showTimePicker = false }, shapes = ButtonDefaults.shapes()) { Text(stringResource(R.string.cancel)) }
				}){
					TimePicker(state = timePickerState)
			}
		}
		
		AlertDialog(onDismissRequest = onDismiss, title = { Text(stringResource(if (isAdd) R.string.add_todo else R.string.edit_todo)) }, text = {
			Column(modifier = Modifier
				.fillMaxWidth()
				.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.vertical_margin))) {
				Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
					Checkbox(checked = isDone, onCheckedChange = { isDone = it })
					TextField(value = title,
					          onValueChange = { title = it },
					          placeholder = { Text(stringResource(R.string.title)) },
					          modifier = Modifier
						          .weight(1f)
						          .padding(0.dp),
					          textStyle = MaterialTheme.typography.titleLarge,
					          singleLine = true,
					          colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent,
					                                            unfocusedContainerColor = Color.Transparent,
					                                            disabledContainerColor = Color.Transparent,
					                                            focusedIndicatorColor = Color.Transparent,
					                                            unfocusedIndicatorColor = Color.Transparent,
					                                            disabledIndicatorColor = Color.Transparent))
				}
				OutlinedTextField(value = description, onValueChange = { description = it }, placeholder = { Text(stringResource(R.string.description)) }, modifier = Modifier.fillMaxWidth())
				SingleSection(label = stringResource(R.string.type),
				                      items = types.mapNotNull { it.name },
				                      selected = todoType,
				                      onSelect = { todoType = if (todoType == it) null else it },
				                      onAdd = { addCode = 0; showAddDialog = true; newItemName = "" },
				                      onDelete = { deleteType(it); if (todoType == it) todoType = null })
				SingleSection(label = stringResource(R.string.subject),
				                      items = subjects.mapNotNull { it.name },
				                      selected = subject,
				                      onSelect = { subject = if (subject == it) null else it },
				                      onAdd = { addCode = 1; showAddDialog = true; newItemName = "" },
				                      onDelete = { deleteSubject(it); if (subject == it) subject = null })
				TagSection(label = stringResource(R.string.tag), items = tags.mapNotNull { it.name }, selectedTags = selectedTags, onToggle = { tag ->
					selectedTags = if (tag in selectedTags) {
						selectedTags.filter { it != tag }.toMutableStateList()
					}
					else {
						(selectedTags + tag).toMutableStateList()
					}
				}, onAdd = { addCode = 2; showAddDialog = true; newItemName = "" },
				onDelete = { deleteTag(it); selectedTags = selectedTags.filter { tag -> tag != it }.toMutableStateList() })
				OutlinedTextField(value = location ?: "",
				                  onValueChange = { location = it.ifEmpty { null } },
				                  label = { Text(stringResource(R.string.location)) },
				                  leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
				                  modifier = Modifier.fillMaxWidth(),
				                  singleLine = true)
				SubtaskSection( subtasks = subtasks, onAdd = { showSubtaskDialog = true }, onToggle = { index ->
					val item = subtasks[index]
					val current = item.getIntValue("status", TodoInfo.TODO)
					item["status"] = if (current == TodoInfo.DONE) TodoInfo.TODO else TodoInfo.DONE
				}, onDelete = { index -> subtasks.removeAt(index) }, onTitleChange = { index, newTitle ->
					subtasks[index]["title"] = newTitle
				})
				PrioritySection(modifier = modifier, priority = priority, onPriorityChange = { priority = it }, priorityLabels = priorityLabels)
				ColorPickerSection(colors = colors, selectedColor = selectedColor, onColorSelected = { selectedColor = if (selectedColor == it || it.isEmpty()) null else it })
				DateSection(
				            label = stringResource(R.string.due),
				            dateValue = dueDate,
				            timeValue = dueTime,
				            remindValue = remindTime,
				            onDateClick = { dateTarget = "due"; showDatePicker = true },
				            onTimeClick = { dateTarget = "due"; showTimePicker = true },
				            onRemindClick = { remindTarget = "due"; showRemindMenu = true },
				            onClearDate = { dueDate = null },
				            onClearTime = { dueTime = null; remindTime = null },
				            onClearRemind = { remindTime = null },
				            onRemindSelected = { remindTime = it },
				            onCustomRemind = { remindTarget = "due"; showCustomRemindDialog = true },
				            showRemindMenu = showRemindMenu && remindTarget == "due",
				            onDismissRemindMenu = { showRemindMenu = false },
				            quickDates = listOf(
					            stringResource(R.string.today) to { dueDate = DateTimeManager.toDateStringPLus(0) },
					            stringResource(R.string.tomorrow) to { dueDate = DateTimeManager.toDateStringPLus(1) },
					            stringResource(R.string.next_week) to { dueDate = DateTimeManager.toDateStringPLus(7) },
				                               ))
				DateSection( label = stringResource(R.string.ddl),
				            dateValue = ddl,
				            timeValue = ddlTime,
				            remindValue = ddlRemindTime,
				            onDateClick = { dateTarget = "ddl"; showDatePicker = true },
				            onTimeClick = { dateTarget = "ddl"; showTimePicker = true },
				            onRemindClick = { remindTarget = "ddl"; showRemindMenu = true },
				            onClearDate = { ddl = null },
				            onClearTime = { ddlTime = null; ddlRemindTime = null },
				            onClearRemind = { ddlRemindTime = null },
				            onRemindSelected = { ddlRemindTime = it },
				            onCustomRemind = { remindTarget = "ddl"; showCustomRemindDialog = true },
				            showRemindMenu = showRemindMenu && remindTarget == "ddl",
				            onDismissRemindMenu = { showRemindMenu = false },
				            quickDates = listOf(
					            stringResource(R.string.today) to { ddl = DateTimeManager.toDateStringPLus(0) },
					            stringResource(R.string.tomorrow) to { ddl = DateTimeManager.toDateStringPLus(1) },
					            stringResource(R.string.next_week) to { ddl = DateTimeManager.toDateStringPLus(7) },
				                               ))
			}
		}, confirmButton = {
			TextButton(onClick = {
				val todo = TodoEntity(
					id = initialTodo.id,
					title = title,
					description = description,
					dueDate = dueDate,
					dueTime = dueTime,
					doneDateTime = if (isDone) LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) else null,
					createDateTime = initialTodo.createDateTime,
					updateDateTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
					status = if (isDone) TodoInfo.DONE else TodoInfo.TODO,
					priority = priority,
					todoType = todoType,
					subtask = JSONArray(subtasks.toList()),
					attachment = initialTodo.attachment,
					tag = JSONArray(selectedTags),
					subject = subject,
					location = location,
					color = selectedColor,
					label = initialTodo.label,
					ddl = ddl,
					ddlTime = ddlTime,
					ddlRemindTime = ddlRemindTime,
					remindTime = remindTime,
				                     )
				onConfirm(todo)
			}, shapes = ButtonDefaults.shapes()) { Text(stringResource(R.string.confirm)) }
		}, dismissButton = {
			if (onDelete != null) {
				TextButton(onClick = onDelete, shapes = ButtonDefaults.shapes()) {
					Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
				}
			}
			TextButton(onClick = onDismiss, shapes = ButtonDefaults.shapes()) { Text(stringResource(R.string.cancel)) }
		})
	}
	
	@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class) @Composable fun SingleSection(
		label: String,
		items: List<String>,
		selected: String?,
		onSelect: (String) -> Unit,
		onAdd: () -> Unit,
		onDelete: ((String) -> Unit)? = null,
	                                                                          ) {
		FlowRow(horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.horizontal_gap))) {
			AssistChip(label = { Text(label) }, onClick = { })
			items.forEach { name ->
				ElevatedFilterChip(modifier = Modifier.combinedClickable(
					onClick = { onSelect(name) }, onLongClick = { onDelete?.invoke(name) }), selected = name == selected, onClick = {onSelect(name)}, label = { Text(name) }, leadingIcon = if (name == selected) {
					{
						Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(FilterChipDefaults.IconSize))
					}
				}
				else {
					null
				})
			}
			if (selected != null && selected !in items) {
				ElevatedFilterChip(modifier = Modifier.combinedClickable(onClick = { onSelect(selected) }, onLongClick = { onDelete?.invoke(selected) },interactionSource = remember{ MutableInteractionSource() },
				                                                         indication = null), selected = true, onClick = { onSelect(selected) }, label = { Text(selected) }, leadingIcon = {
						Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(FilterChipDefaults.IconSize))
				})
			}
			ElevatedAssistChip(onClick = onAdd, leadingIcon =  { Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add)) }, label = { Text(stringResource(R.string.add)) })
		}
	}
	
	@Composable fun PrioritySection(
		modifier: Modifier = Modifier,
		priority: Int,
		onPriorityChange: (Int) -> Unit,
		priorityLabels: List<String>,
	                               ) {
//		SegmentedListItem(
//			ListItemDefaults.segmentedShapes(index = 0, count = 1),
//			leadingContent = {
//				Icon(Icons.Default.PriorityHigh, contentDescription = null)
//			},
//			overlineContent = {
//				Text(stringResource(R.string.priority))
//			},
//			trailingContent = {
//				Text(priorityLabels[priority])
//			},
//			supportingContent = {
//
//			}){
//			Slider(value = priority.toFloat(),modifier = Modifier.fillMaxWidth(), onValueChange = { onPriorityChange(it.toInt()) }, valueRange = 0f..4f, steps = 3, colors = SliderDefaults.colors(activeTrackColor = when (priority) {
//				0 -> MaterialTheme.colorScheme.outline
//				1 -> MaterialTheme.colorScheme.tertiary
//				2 -> MaterialTheme.colorScheme.primary
//				3 -> MaterialTheme.colorScheme.secondary
//				4 -> MaterialTheme.colorScheme.error
//				else -> MaterialTheme.colorScheme.primary
//			}), thumb = {
//				Box(modifier = Modifier
//					.size(width = 4.dp, height = 24.dp)
//					.clip(RoundedCornerShape(50))
//					.background(MaterialTheme.colorScheme.primary))
//			})
//		}
			Column(modifier = modifier
				  ) {
				Row(verticalAlignment = Alignment.CenterVertically) {
					Icon(Icons.Rounded.PriorityHigh, contentDescription = null, modifier = Modifier.size(20.dp))
					Spacer(Modifier.size(ButtonDefaults.IconSpacing))
					Text(stringResource(R.string.priority), style = MaterialTheme.typography.bodyLargeEmphasized, modifier = Modifier.weight(1f))
					Text(priorityLabels[priority], style = MaterialTheme.typography.bodyMedium)
				}
				Slider(value = priority.toFloat(),modifier = Modifier.fillMaxWidth(), onValueChange = { onPriorityChange(it.toInt()) }, valueRange = 0f..4f, steps = 3, colors = SliderDefaults.colors(activeTrackColor = when (priority) {
				0 -> MaterialTheme.colorScheme.outline
				1 -> MaterialTheme.colorScheme.tertiary
				2 -> MaterialTheme.colorScheme.primary
				3 -> MaterialTheme.colorScheme.secondary
				4 -> MaterialTheme.colorScheme.error
				else -> MaterialTheme.colorScheme.primary
			}), thumb = {
				Box(modifier = Modifier
					.size(width = 4.dp, height = 24.dp)
					.clip(RoundedCornerShape(50))
					.background(MaterialTheme.colorScheme.primary))
			})
			}
		}
	
	@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class) @Composable fun TagSection(
		label: String,
		items: List<String>,
		selectedTags: List<String>,
		onToggle: (String) -> Unit,
		onAdd: () -> Unit,
		onDelete: ((String) -> Unit)? = null,
	                                                               ) {
		FlowRow(horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.horizontal_gap))) {
			AssistChip(label = { Text(label) }, onClick = { })
			items.forEach { name ->
				ElevatedFilterChip(modifier = Modifier.combinedClickable(onClick = { onToggle(name) }, onLongClick = { onDelete?.invoke(name) }, interactionSource = remember { MutableInteractionSource() }), selected = name in selectedTags, onClick = { }, label = { Text(name) }, leadingIcon = if (name in selectedTags) {
					{
						Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(FilterChipDefaults.IconSize))
					}
				}
				else {
					null
				})
			}
			selectedTags.filter { it !in items }.forEach { name ->
				ElevatedFilterChip(modifier = Modifier.combinedClickable(onClick = { onToggle(name) }, onLongClick = { onDelete?.invoke(name) }, interactionSource = remember { MutableInteractionSource() }, indication = null), selected = true, onClick = { onToggle(name) }, label = { Text(name) }, leadingIcon = {
					Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(FilterChipDefaults.IconSize))
				})
			}
			ElevatedAssistChip(
			           onClick = onAdd,
			           leadingIcon =  { Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add)) },
			           label = { Text(stringResource(R.string.add)) })
		}
	}
	
	@OptIn(ExperimentalMaterial3ExpressiveApi::class) @Composable fun DateSection(
		label: String,
		dateValue: String?,
		timeValue: String?,
		remindValue: String?,
		onDateClick: () -> Unit,
		onTimeClick: () -> Unit,
		onRemindClick: () -> Unit,
		onClearDate: () -> Unit,
		onClearTime: () -> Unit,
		onClearRemind: () -> Unit,
		onRemindSelected: (String?) -> Unit,
		onCustomRemind: () -> Unit,
		showRemindMenu: Boolean,
		onDismissRemindMenu: () -> Unit,
		quickDates: List<Pair<String, () -> Unit>> = emptyList(),
	                           ) {
		val noneStr = stringResource(R.string.none)
		val count = if (timeValue != null && dateValue != null) 3 else 2
		Text(label, style = MaterialTheme.typography.bodyLargeEmphasized)
			SegmentedListItem(
				onClick = onDateClick,
				verticalAlignment = Alignment.CenterVertically,
				modifier = Modifier.fillMaxWidth(),
				shapes = ListItemDefaults.segmentedShapes(index = 0, count = count),
				colors =
					ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
				leadingContent = {
					Icon(Icons.Outlined.CalendarMonth, contentDescription = stringResource(R.string.date))
				},
				overlineContent = {
					Text(stringResource(R.string.date), style = MaterialTheme.typography.bodyLargeEmphasized)
				},
				trailingContent = {
					dateValue?.let {
						IconButton(onClick = onClearDate) {
							Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.delete))
						}
					}
				},
				supportingContent = {
					FlowRow (horizontalArrangement = Arrangement.SpaceBetween){
					quickDates.forEach { (label, action) ->
						AssistChip(
							onClick = action,
							contentPadding = PaddingValues(0.dp),
							label = { Text(label, style = MaterialTheme.typography.labelMedium) },
						          )
					}
				}
					}) {
				Text(dateValue ?: noneStr, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMediumEmphasized)
			}
		
			SegmentedListItem(
				onClick = onTimeClick,
				modifier = Modifier.fillMaxWidth(),
				shapes = ListItemDefaults.segmentedShapes(index = 1, count = count),
				colors =
					ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
				leadingContent = {
					Icon(Icons.Outlined.Schedule, contentDescription = stringResource(R.string.time))
				},
				overlineContent = {
					Text(stringResource(R.string.time), style = MaterialTheme.typography.bodyLargeEmphasized)
				},
				trailingContent = {
					timeValue?.let {
						IconButton(onClick = onClearTime) {
							Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.delete))
						}
					}
				}){
				Text(timeValue ?: noneStr, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMediumEmphasized)
			}
				
				if (timeValue != null && dateValue != null) {
					Box {
						SegmentedListItem(
							onClick = onRemindClick,
							modifier = Modifier.fillMaxWidth(),
							shapes = ListItemDefaults.segmentedShapes(index = 2, count = 3),
							colors =
								ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
							leadingContent = {
								Icon(Icons.Outlined.Notifications, contentDescription = stringResource(R.string.remind))
							},
							overlineContent = {
								Text(stringResource(R.string.remind), style = MaterialTheme.typography.bodyLargeEmphasized)
							},
							trailingContent = {
								remindValue?.let {
									IconButton(onClick = onClearRemind) {
										Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.delete))
									}
								}
							}){
							Text(remindValue ?: noneStr, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMediumEmphasized)
						}
						RemindDropdownMenu(expanded = showRemindMenu, onDismiss = onDismissRemindMenu, onSelected = onRemindSelected, onCustom = onCustomRemind)
					}
		}
	}
	
	@Composable fun RemindDropdownMenu(
		expanded: Boolean,
		onDismiss: () -> Unit,
		onSelected: (String?) -> Unit,
		onCustom: () -> Unit,
	                                  ) {
		DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
			DropdownMenuItem(text = { Text(stringResource(R.string.none)) }, onClick = { onSelected(null); onDismiss() })
			listOf(
				R.string.on_time,
				R.string.five_mins,
				R.string.fifteen_mins,
				R.string.half_hour,
				R.string.one_hour,
				R.string.one_day,
			      ).forEach { label ->
				DropdownMenuItem(text = { Text(stringResource(label)) }, onClick = { onSelected(context.getString(label)); onDismiss() })
			}
			DropdownMenuItem(text = { Text(stringResource(R.string.custom_remind_title)) }, onClick = { onDismiss(); onCustom() })
		}
	}
	
	@Composable fun SubtaskSection(
		subtasks: SnapshotStateList<JSONObject>,
		onAdd: () -> Unit,
		onToggle: (Int) -> Unit,
		onDelete: (Int) -> Unit,
		onTitleChange: (Int, String) -> Unit,
	                              ) {
				SegmentedListItem(
					onClick = onAdd,
					modifier = Modifier.fillMaxWidth(),
					shapes = ListItemDefaults.segmentedShapes(index = 0, count = 1+subtasks.size),
					colors =
						ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
					leadingContent = {
						Icon(Icons.Rounded.SubdirectoryArrowRight, contentDescription = stringResource(R.string.subtask))
					}, trailingContent = {
						Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.add))
					},
					overlineContent = {
						Text(stringResource(R.string.subtask), style = MaterialTheme.typography.bodyLargeEmphasized)
					}){
//					Text(timeValue ?: noneStr, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMediumEmphasized)
				}
				subtasks.forEachIndexed { index, item ->
					val isDone = item.getIntValue("status", TodoInfo.TODO) == TodoInfo.DONE
					val itemTitle = item.getString("title") ?: ""
					SegmentedListItem(
						onClick = {},
						modifier = Modifier.fillMaxWidth(),
						shapes = ListItemDefaults.segmentedShapes(index = index+1, count = 1+subtasks.size),
						colors =
							ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
						leadingContent = {
							Checkbox(checked = isDone, onCheckedChange = { onToggle(index) })
						},
						overlineContent = {
							TextField(value = itemTitle,
							          onValueChange = { onTitleChange(index, it) },
							          colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent,
							                                            unfocusedContainerColor = Color.Transparent,
							                                            disabledContainerColor = Color.Transparent,
							                                            focusedIndicatorColor = Color.Transparent,
							                                            unfocusedIndicatorColor = Color.Transparent,
							                                            disabledIndicatorColor = Color.Transparent),
							          textStyle = MaterialTheme.typography.bodyMedium.copy(textDecoration = if (isDone) TextDecoration.LineThrough else TextDecoration.None),
							          singleLine = true)
						}, trailingContent = {
							IconButton(onClick = { onDelete(index) }) {
								Icon(Icons.Rounded.Delete, contentDescription = stringResource(R.string.delete),  tint = MaterialTheme.colorScheme.error)
							}
						}){}
			}
	}
	
	@OptIn(ExperimentalLayoutApi::class) @Composable fun ColorPickerSection(
		colors: List<String>,
		selectedColor: String?,
		onColorSelected: (String) -> Unit,
	                                                                       ) {
		SegmentedListItem(
			onClick = {},
			verticalAlignment = Alignment.CenterVertically,
			modifier = Modifier.fillMaxWidth(),
			shapes = ListItemDefaults.segmentedShapes(index = 0, count = 1),
			colors =
				ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
			leadingContent = {
				Icon(Icons.Rounded.ColorLens, contentDescription = stringResource(R.string.color))
			},
			overlineContent = {
				Text(stringResource(R.string.color), style = MaterialTheme.typography.bodyLargeEmphasized)
			},
			trailingContent = {
				selectedColor?.let {
					IconButton(onClick = { onColorSelected("") }) {
						Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.delete))
					}
				}
			}){
			Row(modifier = Modifier
				.horizontalScroll(rememberScrollState())
				.fillMaxWidth()
				.padding(vertical = dimensionResource(R.dimen.vertical_padding)), horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.horizontal_gap)))  {
				colors.forEach { colorStr ->
					val color = try {
						Color(colorStr.toColorInt())
					} catch (_: Exception) {
						Color.Gray
					}
					val isSelected = colorStr == selectedColor
					Box(modifier = Modifier
						.size(24.dp)
						.clip(CircleShape)
						.background(color)
						.then(if (isSelected) Modifier.border(3.dp, Color.White, CircleShape) else Modifier)
						.clickable { onColorSelected(colorStr) })
				}
			}
		}
	}
	
	@Composable fun AddItemDialog(
		title: String,
		value: String,
		onValueChange: (String) -> Unit,
		onDismiss: () -> Unit,
		onConfirm: (String) -> Unit,
	                             ) {
		AlertDialog(onDismissRequest = onDismiss, title = { Text(title) }, text = {
			OutlinedTextField(value = value, onValueChange = onValueChange, label = { Text(title) })
		}, confirmButton = {
			TextButton(onClick = { onConfirm(value) }, shapes = ButtonDefaults.shapes()) { Text(stringResource(R.string.confirm)) }
		}, dismissButton = {
			TextButton(onClick = onDismiss, shapes = ButtonDefaults.shapes()) { Text(stringResource(R.string.cancel)) }
		})
	}
	
	@Composable fun TodoListScreen(
		modifier: Modifier = Modifier,
		todoList: List<TodoEntity>,
		addTrigger: Int = 0,
	                              ) {
		var showAddDialog by remember { mutableStateOf(false) }
		var showEditDialog by remember { mutableStateOf(false) }
		var editingTodo by remember { mutableStateOf<TodoEntity?>(null) }
		var copyTodo by remember { mutableStateOf(TodoEntity()) }

		if (showAddDialog) {
			TodoDetailDialog(
				initialTodo = copyTodo,
				isAdd = true,
				onDismiss = { showAddDialog = false; copyTodo = TodoEntity() },
				onConfirm = { todo ->
					addTodo(todo)
					showAddDialog = false
					copyTodo = TodoEntity()
				},
			)
		}

		if (showEditDialog && editingTodo != null) {
			TodoDetailDialog(
				initialTodo = editingTodo!!,
				isAdd = false,
				onDismiss = { showEditDialog = false; editingTodo = null },
				onConfirm = { todo ->
					updateTodo(todo)
					showEditDialog = false
					editingTodo = null
				},
				onDelete = {
					deleteTodo(editingTodo!!.id)
					showEditDialog = false
					editingTodo = null
				},
			)
		}

		LaunchedEffect(addTrigger) {
			if (addTrigger > 0) {
				copyTodo = TodoEntity()
				showAddDialog = true
			}
		}

		val grouped = todoList.groupBy { it.dueDate ?: "无预定日期" }
		Column(modifier = modifier.fillMaxWidth()) {
			grouped.forEach { (dateHeader, todos) ->
				Text(
					dateHeader,
					style = MaterialTheme.typography.titleMedium,
					modifier = Modifier.padding(dimensionResource(R.dimen.horizontal_padding), dimensionResource(R.dimen.vertical_padding))
				)
				todos.forEachIndexed { index, todo ->
					TodoItem(
						todo = todo,
						index = index,
						count = todos.size,
						color = Color.Transparent,
						onClick = {
							editingTodo = todo
							showEditDialog = true
						},
						onToggle = { toggleTodoStatus(todo) },
						onDelete = { deleteTodo(todo.id) },
						onCopy = {
							copyTodo = todo.copy()
							showAddDialog = true
						},
					)
				}
			}
		}
	}
	
	@Composable fun TodoItem(
		todo: TodoEntity,
		index: Int = 0,
		count: Int = 1,
		color: Color = MaterialTheme.colorScheme.surfaceContainer,
		onClick: () -> Unit = {},
		onToggle: () -> Unit = {},
		onDelete: () -> Unit = {},
		onCopy: () -> Unit = {},
	                        ) {
		val isDone = todo.status == TodoInfo.DONE
		val itemColor = todo.color?.takeIf { it.isNotEmpty() }?.let {
			try {
				Color(it.toColorInt())
			} catch (_: Exception) {
				null
			}
		}
		SegmentedListItem(
			onClick = onClick,
			verticalAlignment = Alignment.CenterVertically,
			modifier = Modifier.fillMaxWidth(),
			shapes = ListItemDefaults.segmentedShapes(index, count),
			colors =
				ListItemDefaults.colors(containerColor = color),
			leadingContent = {
				Checkbox(checked = isDone, onCheckedChange = { onToggle() })
							 },
			overlineContent = {
				Text(todo.title ?: "",
				     style = MaterialTheme.typography.bodyLarge,
				     color = itemColor ?: MaterialTheme.colorScheme.onSurface,
				     textDecoration = if (isDone) TextDecoration.LineThrough else TextDecoration.None,
				     modifier = Modifier.alpha(if (isDone) 0.5f else 1f))
							  },
			trailingContent = {
				SingleChoiceSegmentedButtonRow {
					SegmentedButton(selected = false,
					                onClick = onCopy,
					                contentPadding = PaddingValues(0.dp),
					                shape = SegmentedButtonDefaults.itemShape(0,2),
					                label = {
										Icon(Icons.Rounded.ContentCopy, contentDescription = stringResource(R.string.copy), tint = MaterialTheme.colorScheme.primary)
									}, icon = {})
					
					SegmentedButton(selected = false,
					                contentPadding = PaddingValues(0.dp),
					                onClick = onDelete,
					                shape = SegmentedButtonDefaults.itemShape(1,2),
					                label = {
						                Icon(Icons.Rounded.Delete, contentDescription = stringResource(R.string.delete), tint = MaterialTheme.colorScheme.error)
					                }, icon = {})
				}
			}, supportingContent = {
				val detailParts = mutableListOf<String>()
				todo.todoType?.let { detailParts.add(it) }
				todo.subject?.let { detailParts.add("${stringResource(R.string.subject)}:${it}") }
				todo.location?.let { detailParts.add("${stringResource(R.string.location)}:${it}") }
				todo.ddl?.let { detailParts.add("${stringResource(R.string.ddl)}:${it}") }
				todo.remindTime?.let { detailParts.add("${stringResource(R.string.remind)}:${it}") }
				if (todo.priority > 0) detailParts.add(priorityLabels[todo.priority])
				if (detailParts.isNotEmpty()) {
					Text(detailParts.joinToString(" | "), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
				}
			}){
			todo.description?.takeIf { it.isNotEmpty() }?.let {
				Text(it,
				     style = MaterialTheme.typography.bodySmall,
				     maxLines = 2,
				     overflow = TextOverflow.Ellipsis,
				     textDecoration = if (isDone) TextDecoration.LineThrough else TextDecoration.None,
				     modifier = Modifier.alpha(if (isDone) 0.5f else 1f))
			}
		}
	}
}