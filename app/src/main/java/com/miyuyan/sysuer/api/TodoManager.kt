package com.miyuyan.sysuer.api

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Edit
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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DisplayMode
import androidx.compose.material3.ElevatedAssistChip
import androidx.compose.material3.ElevatedFilterChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDialog
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberSliderState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import androidx.lifecycle.LifecycleCoroutineScope
import com.alibaba.fastjson2.JSONArray
import com.alibaba.fastjson2.JSONObject
import com.miyuyan.preference.EditPreference
import com.miyuyan.preference.LocalPreferenceCount
import com.miyuyan.preference.LocalPreferenceIndex
import com.miyuyan.preference.MenuPreference
import com.miyuyan.preference.Preference
import com.miyuyan.preference.PreferenceCategory
import com.miyuyan.preference.WheelPicker
import com.miyuyan.sysuer.R
import com.miyuyan.sysuer.todo.TodoEntity
import com.miyuyan.sysuer.todo.TodoInfo
import com.miyuyan.sysuer.todo.TodoModel
import com.miyuyan.sysuer.todo.TodoReminderReceiver
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.squircle.squircleBackground
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class TodoManager(
	private val context: Context,
	private val lifecycleScope: LifecycleCoroutineScope,
	val todoModel: TodoModel,
) {
	private companion object {
		/** 自定义提醒的最大提前量:24 小时 59 分。 */
		const val MAX_CUSTOM_REMIND_MINUTES = 24 * 60 + 59
	}

	private val colors = listOf(
			"#757575",
			"#F44336",
			"#E91E63",
			"#9C27B0",
			"#673AB7",
			"#3F51B5",
			"#2196F3",
			"#03A9F4",
			"#00BCD4",
			"#009688",
			"#4CAF50",
			"#8BC34A",
			"#CDDC39",
			"#FFEB3B",
			"#FFC107",
			"#FF9800",
			"#FF5722",
			"#795548",
			"#607D8B"
	)
	private val priorityLabels =
		listOf("无", "不重要且不紧急", "不重要但紧急", "重要但不紧急", "重要且紧急")
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
			todo.doneDateTime =
				LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
			cancelReminder(todo.id)
		} else {
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
		scheduleSingleReminder(
				todo.id,
				todo.title,
				todo.description,
				todo.status,
				todo.dueDate,
				todo.dueTime,
				todo.remindTime
		)
		scheduleSingleReminder(
				todo.id + 1000000,
				"[DDL] ${todo.title}",
				todo.description,
				todo.status,
				todo.ddl,
				todo.ddlTime,
				todo.ddlRemindTime
		)
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
			val pendingIntent = PendingIntent.getBroadcast(
					context,
					notificationId,
					intent,
					PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
			)
			val triggerAtMillis =
				remindDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
			alarmManager.setExactAndAllowWhileIdle(
					AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent
			)
		} catch (_: Exception) {
		}
	}

	/**
	 * 目标时间距今可用的提前量(分钟)。
	 *
	 * 日期/时间缺失或无法解析时返回 null(视为不限制);已过时间返回负数。
	 */
	private fun availableLeadMinutes(date: String?, time: String?): Int? {
		if (date.isNullOrEmpty() || time.isNullOrEmpty()) return null
		val target = try {
			LocalDateTime.of(LocalDate.parse(date), LocalTime.parse(time))
		} catch (_: Exception) {
			return null
		}
		return Duration.between(LocalDateTime.now(), target).toMinutes().toInt()
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
			} else 0
		}
	}

	private fun cancelReminder(id: Int) {
		cancelSingleReminder(id)
		cancelSingleReminder(id + 1000000)
	}

	private fun cancelSingleReminder(notificationId: Int) {
		val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
		val intent = Intent(context, TodoReminderReceiver::class.java)
		val pendingIntent = PendingIntent.getBroadcast(
				context,
				notificationId,
				intent,
				PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_NO_CREATE
		)
		if (pendingIntent != null) {
			alarmManager.cancel(pendingIntent)
			pendingIntent.cancel()
		}
	}

	@Stable
	class TodoDetailState(
		private val manager: TodoManager,
		val initialTodo: TodoEntity = TodoEntity(),
		val isAdd: Boolean = true,
	) {
		var title by mutableStateOf(initialTodo.title ?: "")
		var description by mutableStateOf(initialTodo.description ?: "")
		var priority by mutableIntStateOf(initialTodo.priority.coerceIn(0, 4))
		var todoType by mutableStateOf(initialTodo.todoType)
		var subject by mutableStateOf(initialTodo.subject)
		var selectedTags by mutableStateOf(initialTodo.tag.map { it as String })
		var isDone by mutableStateOf(initialTodo.status == TodoInfo.DONE)
		var dueDate by mutableStateOf(initialTodo.dueDate)
		var dueTime by mutableStateOf(initialTodo.dueTime)
		var remindTime by mutableStateOf(initialTodo.remindTime)
		var ddl by mutableStateOf(initialTodo.ddl)
		var ddlTime by mutableStateOf(initialTodo.ddlTime)
		var ddlRemindTime by mutableStateOf(initialTodo.ddlRemindTime)
		var selectedColor by mutableStateOf(initialTodo.color)
		var location by mutableStateOf(initialTodo.location)
		val subtasks =
			mutableStateListOf<JSONObject>().apply { addAll(initialTodo.subtask.map { it as JSONObject }) }

		var showDatePicker by mutableStateOf(false)
		var showTimePicker by mutableStateOf(false)
		var dateTarget by mutableStateOf("")
		var showAddDialog by mutableStateOf(false)
		var addCode by mutableIntStateOf(0)
		var newItemName by mutableStateOf("")
		var showSubtaskDialog by mutableStateOf(false)
		var remindTarget by mutableStateOf("")
		var showCustomRemindDialog by mutableStateOf(false)

		fun addSubtask(title: String) {
			if (title.isNotEmpty()) {
				subtasks.add(JSONObject.of("title", title, "status", TodoInfo.TODO))
			}
		}

		fun removeSubtask(index: Int) {
			subtasks.removeAt(index)
		}

		fun toggleTag(tag: String) {
			selectedTags = if (tag in selectedTags) selectedTags - tag else selectedTags + tag
		}

		fun removeTag(tag: String) {
			selectedTags = selectedTags - tag
		}

		fun toTodoEntity(): TodoEntity = TodoEntity(
				id = initialTodo.id,
				title = title,
				description = description,
				dueDate = dueDate,
				dueTime = dueTime,
				doneDateTime = if (isDone) LocalDateTime.now()
					.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) else null,
				createDateTime = initialTodo.createDateTime,
				updateDateTime = LocalDateTime.now()
					.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
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

		fun addTodo(onSuccess: () -> Unit = {}) = manager.addTodo(toTodoEntity(), onSuccess)

		fun updateTodo(onSuccess: () -> Unit = {}) = manager.updateTodo(toTodoEntity(), onSuccess)

		fun save(onSuccess: () -> Unit = {}) {
			if (isAdd) addTodo(onSuccess) else updateTodo(onSuccess)
		}

		fun delete(onSuccess: () -> Unit = {}) = manager.deleteTodo(initialTodo.id, onSuccess)
	}

	@Composable
	fun rememberTodoDetailState(
		initialTodo: TodoEntity = TodoEntity(), isAdd: Boolean = true
	): TodoDetailState = remember(initialTodo, isAdd) { TodoDetailState(this, initialTodo, isAdd) }

	@OptIn(ExperimentalMaterial3Api::class)
	@Composable
	fun TodoDetailSheet(
		initialTodo: TodoEntity = TodoEntity(),
		isAdd: Boolean = true,
		onDismiss: () -> Unit,
		onDelete: (() -> Unit)? = null,
	) {
		val todoState = rememberTodoDetailState(initialTodo, isAdd)
		val types by todoModel.types.observeAsState(emptyList())
		val subjects by todoModel.subjects.observeAsState(emptyList())
		val tags by todoModel.tags.observeAsState(emptyList())
		if (todoState.showAddDialog) {
			AddItemDialog(
					title = stringResource(
							when (todoState.addCode) {
								0 -> R.string.type
								1 -> R.string.subject
								else -> R.string.tag
							}
					),
					value = todoState.newItemName,
					onValueChange = { todoState.newItemName = it },
					onDismiss = { todoState.showAddDialog = false; todoState.newItemName = "" },
					onConfirm = {
						if (it.isNotEmpty()) when (todoState.addCode) {
							0 -> {
								addType(it); todoState.todoType = it
							}

							1 -> {
								addSubject(it); todoState.subject = it
							}

							2 -> {
								addTag(it); todoState.toggleTag(it)
							}
						}
						todoState.showAddDialog = false
						todoState.newItemName = ""
					})
		}

		if (todoState.showSubtaskDialog) {
			var subtaskTitle by remember { mutableStateOf("") }
			AlertDialog(
					onDismissRequest = { todoState.showSubtaskDialog = false },
					title = { Text(stringResource(R.string.subtask)) },
					text = {
						OutlinedTextField(
								value = subtaskTitle,
								onValueChange = { subtaskTitle = it },
								label = { Text(stringResource(R.string.title)) })
					},
					confirmButton = {
						TextButton(
								onClick = {
									todoState.addSubtask(subtaskTitle)
									todoState.showSubtaskDialog = false
								}, shapes = ButtonDefaults.shapes()
						) { Text(stringResource(R.string.confirm)) }
					},
					dismissButton = {
						TextButton(
								onClick = { todoState.showSubtaskDialog = false },
								shapes = ButtonDefaults.shapes()
						) { Text(stringResource(R.string.cancel)) }
					})
		}

		if (todoState.showCustomRemindDialog) {
			// 打开时用当前提醒值回填滚轮;预设值(如“1天”)也会换算成小时+分钟
			val currentMinutes = parseRemindMinutes(
					when (todoState.remindTarget) {
						"due" -> todoState.remindTime
						"ddl" -> todoState.ddlRemindTime
						else -> null
					}.orEmpty()
			)
			// 目标时间距今的可用提前量;24 小时内时把滚轮上限收紧到可用范围
			val targetDate =
				if (todoState.remindTarget == "ddl") todoState.ddl else todoState.dueDate
			val targetTime =
				if (todoState.remindTarget == "ddl") todoState.ddlTime else todoState.dueTime
			val availableTotal = remember(targetDate, targetTime) {
				availableLeadMinutes(targetDate, targetTime)
			}
			val maxTotal = (availableTotal ?: MAX_CUSTOM_REMIND_MINUTES).coerceIn(
					0, MAX_CUSTOM_REMIND_MINUTES
			)
			val maxHour = maxTotal / 60
			var hourIndex by remember {
				mutableIntStateOf(
						(currentMinutes / 60).coerceIn(
								0,
								maxHour
						)
				)
			}
			var minuteIndex by remember { mutableIntStateOf((currentMinutes % 60).coerceIn(0, 59)) }
			val hours = (0..maxHour).map { it.toString() }
			// 小时到顶时，分钟只能选到剩余可提前量
			val maxMinuteForHour = if (hourIndex == maxHour) maxTotal % 60 else 59
			val effectiveMinute = minuteIndex.coerceAtMost(maxMinuteForHour)
			val minutes = (0..maxMinuteForHour).map { it.toString() }
			val wheelHeight = 44.dp * 3
			AlertDialog(
					onDismissRequest = { todoState.showCustomRemindDialog = false },
					title = { Text(stringResource(R.string.custom_remind_title)) },
					text = {
						Row(
								modifier = Modifier.fillMaxWidth(),
								horizontalArrangement = Arrangement.Center,
								verticalAlignment = Alignment.CenterVertically,
						) {
							WheelPicker(
									entries = hours,
									selectedIndex = hourIndex,
									onSelected = { hourIndex = it },
									modifier = Modifier.size(width = 72.dp, height = wheelHeight),
							)
							Text(
									text = stringResource(R.string.hour),
									style = MaterialTheme.typography.bodyMedium,
									modifier = Modifier.padding(horizontal = 4.dp),
							)
							WheelPicker(
									entries = minutes,
									selectedIndex = effectiveMinute,
									onSelected = { minuteIndex = it },
									modifier = Modifier.size(width = 72.dp, height = wheelHeight),
							)
							Text(
									text = stringResource(R.string.minute),
									style = MaterialTheme.typography.bodyMedium,
									modifier = Modifier.padding(horizontal = 4.dp),
							)
						}
					},
					confirmButton = {
						TextButton(
								onClick = {
									val total = hourIndex * 60 + effectiveMinute
									// 0 分钟等价于“准时”
									val result = if (total == 0) {
										context.getString(R.string.on_time)
									} else {
										String.format(
												Locale.getDefault(),
												"%02d%s",
												total,
												context.getString(R.string.minute)
										)
									}
									when (todoState.remindTarget) {
										"due" -> todoState.remindTime = result
										"ddl" -> todoState.ddlRemindTime = result
									}
									todoState.showCustomRemindDialog = false
								}, shapes = ButtonDefaults.shapes()
						) { Text(stringResource(R.string.confirm)) }
					},
					dismissButton = {
						TextButton(
								onClick = { todoState.showCustomRemindDialog = false },
								shapes = ButtonDefaults.shapes()
						) { Text(stringResource(R.string.cancel)) }
					})
		}

		if (todoState.showDatePicker) {
			val state =
				rememberDatePickerState(
						initialSelectedDateMillis = when (todoState.dateTarget) {
							"due" -> todoState.dueDate?.let {
								DateTimeManager.toMillis(it)
							}

							"ddl" -> todoState.ddl?.let { DateTimeManager.toMillis(it) }
							else -> null
						}, initialDisplayMode = DisplayMode.Picker)
			DatePickerDialog(
					onDismissRequest = { todoState.showDatePicker = false },
					confirmButton = {
						TextButton(
								onClick = {
									state.selectedDateMillis?.let { millis ->
										val dateStr = DateTimeManager.toDateString(millis)
										when (todoState.dateTarget) {
											"due" -> todoState.dueDate = dateStr
											"ddl" -> todoState.ddl = dateStr
										}
									}
									todoState.showDatePicker = false
								}, shapes = ButtonDefaults.shapes()
						) { Text(stringResource(if (isAdd) R.string.add else R.string.save)) }
					},
					dismissButton = {
						TextButton(
								onClick = { todoState.showDatePicker = false },
								shapes = ButtonDefaults.shapes()
						) { Text(stringResource(R.string.cancel)) }
					}) {
				DatePicker(state = state)
			}
		}

		if (todoState.showTimePicker) {
			val parsedTime = when (todoState.dateTarget) {
				"due" -> todoState.dueTime
				"ddl" -> todoState.ddlTime
				else -> null
			}
			val (h, m) = parsedTime?.split(":")?.let {
				(it.getOrNull(0)?.toIntOrNull() ?: 0) to (it.getOrNull(1)?.toIntOrNull() ?: 0)
			} ?: (0 to 0)
			val timePickerState =
				rememberTimePickerState(initialHour = h, initialMinute = m, is24Hour = true)
			TimePickerDialog(
					onDismissRequest = { todoState.showTimePicker = false },
					title = { Text(stringResource(R.string.time)) },
					confirmButton = {
						TextButton(
								onClick = {
									val timeStr = String.format(
											Locale.getDefault(),
											"%02d:%02d",
											timePickerState.hour,
											timePickerState.minute
									)
									when (todoState.dateTarget) {
										"due" -> todoState.dueTime = timeStr
										"ddl" -> todoState.ddlTime = timeStr
									}
									todoState.showTimePicker = false
								}, shapes = ButtonDefaults.shapes()
						) { Text(stringResource(R.string.confirm)) }
					},
					dismissButton = {
						TextButton(
								onClick = { todoState.showTimePicker = false },
								shapes = ButtonDefaults.shapes()
						) { Text(stringResource(R.string.cancel)) }
					}) {
				TimePicker(state = timePickerState)
			}
		}

		ModalBottomSheet(onDismissRequest = onDismiss) {
			Column {
				Text(
						text = stringResource(if (isAdd) R.string.add_todo else R.string.edit_todo),
						style = MaterialTheme.typography.titleLarge,
						modifier = Modifier.padding(horizontal = dimensionResource(R.dimen.horizontal_margin))
				)
				Column(
						modifier = Modifier
							.fillMaxSize()
							.weight(1f)
							.verticalScroll(rememberScrollState())
							.imePadding()
							.padding(horizontal = dimensionResource(R.dimen.horizontal_margin)),
						verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.vertical_margin))
				) {
					Row(
							verticalAlignment = Alignment.CenterVertically,
							modifier = Modifier.fillMaxWidth()
					) {
						Checkbox(
								checked = todoState.isDone,
								onCheckedChange = { todoState.isDone = it })
						TextField(
								value = todoState.title,
								onValueChange = { todoState.title = it },
								placeholder = { Text(stringResource(R.string.title)) },
								modifier = Modifier
									.weight(1f)
									.padding(0.dp),
								textStyle = MaterialTheme.typography.titleLarge,
								singleLine = true,
								colors = TextFieldDefaults.colors(
										focusedContainerColor = Color.Transparent,
										unfocusedContainerColor = Color.Transparent,
										disabledContainerColor = Color.Transparent,
										focusedIndicatorColor = Color.Transparent,
										unfocusedIndicatorColor = Color.Transparent,
										disabledIndicatorColor = Color.Transparent
								)
						)
					}
					PreferenceCategory(title = null) {
						item {
							EditPreference(
									title = stringResource(R.string.description),
									value = todoState.description,
									onChange = { _, _, text ->
										todoState.description = text.orEmpty()
									},
									placeholder = stringResource(R.string.description),
									singleLine = false,
									icon = {
										Icon(
												Icons.Outlined.Edit, contentDescription = null
										)
									})
						}
						item {
							SingleSection(
									label = stringResource(R.string.type),
									icon = {
										Icon(
												Icons.Outlined.Category, contentDescription = null
										)
									},
									items = types.mapNotNull { it.name },
									selected = todoState.todoType,
									onSelect = {
										todoState.todoType =
											if (todoState.todoType == it) null else it
									},
									onAdd = {
										todoState.addCode = 0; todoState.showAddDialog =
										true; todoState.newItemName = ""
									},
									onDelete = {
										deleteType(it); if (todoState.todoType == it) todoState.todoType =
										null
									})
						}
						item {
							SingleSection(
									label = stringResource(R.string.subject),
									icon = {
										Icon(
												Icons.Outlined.Category, contentDescription = null
										)
									},
									items = subjects.mapNotNull { it.name },
									selected = todoState.subject,
									onSelect = {
										todoState.subject =
											if (todoState.subject == it) null else it
									},
									onAdd = {
										todoState.addCode = 1; todoState.showAddDialog =
										true; todoState.newItemName = ""
									},
									onDelete = {
										deleteSubject(it); if (todoState.subject == it) todoState.subject =
										null
									})
						}
						item {
							TagSection(
									label = stringResource(R.string.tag),
									icon = {
										Icon(
												Icons.AutoMirrored.Outlined.Label,
												contentDescription = null
										)
									},
									items = tags.mapNotNull { it.name },
									selectedTags = todoState.selectedTags,
									onToggle = { tag -> todoState.toggleTag(tag) },
									onAdd = {
										todoState.addCode = 2; todoState.showAddDialog =
										true; todoState.newItemName = ""
									},
									onDelete = {
										deleteTag(it); todoState.removeTag(it)
									})
						}
						item {
							EditPreference(
									title = stringResource(R.string.location),
									value = todoState.location ?: "",
									onChange = { _, _, text ->
										todoState.location = text?.ifEmpty { null }
									},
									placeholder = stringResource(R.string.location),
									singleLine = true,
									icon = {
										Icon(
												Icons.Default.LocationOn, contentDescription = null
										)
									})
						}
						item {
							PrioritySection(
									priority = todoState.priority,
									onPriorityChange = { todoState.priority = it },
									priorityLabels = priorityLabels
							)
						}
						item {
							ColorPickerSection(
									colors = colors,
									selectedColor = todoState.selectedColor,
									onColorSelected = {
										todoState.selectedColor =
											if (todoState.selectedColor == it || it.isEmpty()) null else it
									})
						}
					}
					SubtaskSection(
							subtasks = todoState.subtasks,
							onAdd = { todoState.showSubtaskDialog = true },
							onDelete = { index -> todoState.removeSubtask(index) })
					DateSection(
							label = stringResource(R.string.due),
							dateValue = todoState.dueDate,
							timeValue = todoState.dueTime,
							remindValue = todoState.remindTime,
							onDateClick = {
								todoState.dateTarget = "due"; todoState.showDatePicker = true
							},
							onTimeClick = {
								todoState.dateTarget = "due"; todoState.showTimePicker = true
							},
							onClearDate = { todoState.dueDate = null },
							onClearTime = { todoState.dueTime = null; todoState.remindTime = null },
							onRemindSelected = { todoState.remindTime = it },
							onCustomRemind = {
								todoState.remindTarget = "due"; todoState.showCustomRemindDialog =
								true
							},
							quickDates = listOf(
									stringResource(R.string.today) to {
										todoState.dueDate = DateTimeManager.toDateStringPLus(0)
									},
									stringResource(R.string.tomorrow) to {
										todoState.dueDate = DateTimeManager.toDateStringPLus(1)
									},
									stringResource(R.string.next_week) to {
										todoState.dueDate = DateTimeManager.toDateStringPLus(7)
									},
							)
					)
					DateSection(
							label = stringResource(R.string.ddl),
							dateValue = todoState.ddl,
							timeValue = todoState.ddlTime,
							remindValue = todoState.ddlRemindTime,
							onDateClick = {
								todoState.dateTarget = "ddl"; todoState.showDatePicker = true
							},
							onTimeClick = {
								todoState.dateTarget = "ddl"; todoState.showTimePicker = true
							},
							onClearDate = { todoState.ddl = null },
							onClearTime = {
								todoState.ddlTime = null; todoState.ddlRemindTime = null
							},
							onRemindSelected = { todoState.ddlRemindTime = it },
							onCustomRemind = {
								todoState.remindTarget = "ddl"; todoState.showCustomRemindDialog =
								true
							},
							quickDates = listOf(
									stringResource(R.string.today) to {
										todoState.ddl = DateTimeManager.toDateStringPLus(0)
									},
									stringResource(R.string.tomorrow) to {
										todoState.ddl = DateTimeManager.toDateStringPLus(1)
									},
									stringResource(R.string.next_week) to {
										todoState.ddl = DateTimeManager.toDateStringPLus(7)
									},
							)
					)
				}
				Row(
						modifier = Modifier
							.fillMaxWidth()
							.padding(
									vertical = dimensionResource(R.dimen.vertical_padding),
									horizontal = dimensionResource(R.dimen.horizontal_padding)
							),
						horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.horizontal_gap)),
						verticalAlignment = Alignment.CenterVertically,
				) {
					if (onDelete != null) {
						TextButton(onClick = onDelete, shapes = ButtonDefaults.shapes()) {
							Text(
									stringResource(R.string.delete),
									color = MaterialTheme.colorScheme.error
							)
						}
					}
					Spacer(modifier = Modifier.weight(1f))
					TextButton(onClick = onDismiss, shapes = ButtonDefaults.shapes()) {
						Text(
								stringResource(R.string.cancel)
						)
					}
					FilledTonalButton(
							onClick = { todoState.save(); onDismiss() },
							shapes = ButtonDefaults.shapes()
					) { Text(stringResource(if(isAdd) R.string.add else R.string.save)) }
				}
			}
		}
	}

	@Composable
	fun SingleSection(
		label: String,
		icon: (@Composable () -> Unit)? = null,
		items: List<String>,
		selected: String?,
		onSelect: (String) -> Unit,
		onAdd: () -> Unit,
		onDelete: ((String) -> Unit)? = null,
	) {
		Preference(
				title = label, icon = icon, content = {
			FlowRow(
					horizontalArrangement = Arrangement.spacedBy(4.dp),
					verticalArrangement = Arrangement.spacedBy((-8).dp)
			) {
				items.forEach { name ->
					ElevatedFilterChip(
							modifier = Modifier.combinedClickable(onClick = {
						onSelect(
								name
						)
					}, onLongClick = { onDelete?.invoke(name) }),
							selected = name == selected,
							onClick = { onSelect(name) },
							contentPadding = PaddingValues(horizontal = 4.dp),
							label = { Text(name) },
							leadingIcon = if (name == selected) {
								{
									Icon(
											imageVector = Icons.Default.Check,
											contentDescription = null,
											modifier = Modifier.size(FilterChipDefaults.IconSize)
									)
								}
							} else {
								null
							})
				}
				if (selected != null && selected !in items) {
					ElevatedFilterChip(
							modifier = Modifier.combinedClickable(
									onClick = {
								onSelect(
										selected
								)
							},
									onLongClick = { onDelete?.invoke(selected) },
									interactionSource = remember { MutableInteractionSource() },
									indication = null
							),
							selected = true,
							onClick = { onSelect(selected) },
							contentPadding = PaddingValues(horizontal = 4.dp),
							label = { Text(selected) },
							leadingIcon = {
								Icon(
										imageVector = Icons.Default.Check,
										contentDescription = null,
										modifier = Modifier.size(FilterChipDefaults.IconSize)
								)
							})
				}
				ElevatedAssistChip(
						onClick = onAdd,
						contentPadding = PaddingValues(horizontal = 4.dp),
						leadingIcon = {
							Icon(
									Icons.Default.Add,
									contentDescription = stringResource(R.string.add),
									modifier = Modifier.size(FilterChipDefaults.IconSize)
							)
						},
						label = { Text(stringResource(R.string.add)) })
			}
		})
	}

	@Composable
	fun PrioritySection(
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
		val state = rememberSliderState(
				value = priority.toFloat(), steps = 3, trackRange = 0f..4f
		)
		Preference(
				title = stringResource(R.string.priority),
				icon = { Icon(Icons.Rounded.PriorityHigh, contentDescription = null) },
				trailing = {
					Text(priorityLabels[priority])
				},
				content = {
					Slider(
							state = state,
							modifier = Modifier.fillMaxWidth(),
							enabled = true,
							onValueChange = {
								onPriorityChange(it.toInt())
								state.value = it
							},
							thumb = {
								Box(
										modifier = Modifier
											.size(width = 4.dp, height = 24.dp)
											.squircleBackground(
													when (it.value.toInt()) {
														0 -> MaterialTheme.colorScheme.outline
														1 -> MaterialTheme.colorScheme.tertiary
														2 -> MaterialTheme.colorScheme.primary
														3 -> MaterialTheme.colorScheme.secondary
														4 -> MaterialTheme.colorScheme.error
														else -> MaterialTheme.colorScheme.primary
													}, 24.dp
											)

								)
							},
							colors = SliderDefaults.colors(
									activeTrackColor = when (state.value.toInt()) {
										0 -> MaterialTheme.colorScheme.outline
										1 -> MaterialTheme.colorScheme.tertiary
										2 -> MaterialTheme.colorScheme.primary
										3 -> MaterialTheme.colorScheme.secondary
										4 -> MaterialTheme.colorScheme.error
										else -> MaterialTheme.colorScheme.primary
									}
							),
							interactionSource = remember { MutableInteractionSource() })
				})
	}

	@Composable
	fun TagSection(
		label: String,
		icon: (@Composable () -> Unit)? = null,
		items: List<String>,
		selectedTags: List<String>,
		onToggle: (String) -> Unit,
		onAdd: () -> Unit,
		onDelete: ((String) -> Unit)? = null,
	) {
		Preference(
				title = label, icon = icon, content = {
			FlowRow(
					horizontalArrangement = Arrangement.spacedBy(6.dp),
					verticalArrangement = Arrangement.spacedBy(0.dp)
			) {
				items.forEach { name ->
					ElevatedFilterChip(
							modifier = Modifier.combinedClickable(
									onClick = {
						onToggle(
								name
						)
					},
							onLongClick = { onDelete?.invoke(name) },
							interactionSource = remember { MutableInteractionSource() }),
							selected = name in selectedTags,
							onClick = { },
							contentPadding = PaddingValues(horizontal = 4.dp),
							label = { Text(name) },
							leadingIcon = if (name in selectedTags) {
								{
									Icon(
											imageVector = Icons.Default.Check,
											contentDescription = null,
											modifier = Modifier.size(FilterChipDefaults.IconSize)
									)
								}
							} else {
								null
							})
				}
				selectedTags.filter { it !in items }.forEach { name ->
					ElevatedFilterChip(
							modifier = Modifier.combinedClickable(
									onClick = { onToggle(name) },
									onLongClick = { onDelete?.invoke(name) },
									interactionSource = remember { MutableInteractionSource() },
									indication = null
							),
							selected = true,
							onClick = { onToggle(name) },
							contentPadding = PaddingValues(horizontal = 4.dp),
							label = { Text(name) },
							leadingIcon = {
								Icon(
										imageVector = Icons.Default.Check,
										contentDescription = null,
										modifier = Modifier.size(FilterChipDefaults.IconSize)
								)
							})
				}
				ElevatedAssistChip(
						onClick = onAdd,
						contentPadding = PaddingValues(horizontal = 4.dp),
						leadingIcon = {
							Icon(
									Icons.Default.Add,
									contentDescription = stringResource(R.string.add),
									modifier = Modifier.size(FilterChipDefaults.IconSize)
							)
						},
						label = { Text(stringResource(R.string.add)) })
			}
		})
	}

	@Composable
	fun DateSection(
		label: String,
		dateValue: String?,
		timeValue: String?,
		remindValue: String?,
		onDateClick: () -> Unit,
		onTimeClick: () -> Unit,
		onClearDate: () -> Unit,
		onClearTime: () -> Unit,
		onRemindSelected: (String?) -> Unit,
		onCustomRemind: () -> Unit,
		quickDates: List<Pair<String, () -> Unit>> = emptyList(),
	) {
		val noneStr = stringResource(R.string.none)
		// 目标时间在 24 小时内时，提醒选项需按可用提前量裁剪
		val availableMinutes = remember(dateValue, timeValue) {
			availableLeadMinutes(dateValue, timeValue)
		}
		PreferenceCategory(title = label) {
			item {
				Preference(title = stringResource(R.string.date), onClick = onDateClick, icon = {
					Icon(
							Icons.Outlined.CalendarMonth,
							contentDescription = stringResource(R.string.date)
					)
				}, trailing = {
					if (dateValue != null) {
						IconButton(onClick = onClearDate) {
							Icon(
									Icons.Rounded.Close,
									contentDescription = stringResource(R.string.delete)
							)
						}
					}
				}, content = {
					Column {
						Text(
								text = dateValue ?: noneStr,
								color = MaterialTheme.colorScheme.onSurfaceVariant,
								style = MaterialTheme.typography.bodyMediumEmphasized
						)
						if (quickDates.isNotEmpty()) {
							FlowRow(
									horizontalArrangement = Arrangement.spacedBy(4.dp),
									verticalArrangement = Arrangement.spacedBy((-4).dp)
							) {
								quickDates.forEach { (qLabel, action) ->
									SuggestionChip(
											onClick = action,
											contentPadding = PaddingValues(0.dp),
											label = {
												Text(
														qLabel,
														style = MaterialTheme.typography.labelMedium
												)
											},
									)
								}
							}
						}
					}
				})
			}
			item {
				Preference(title = stringResource(R.string.time), onClick = onTimeClick, icon = {
					Icon(
							Icons.Outlined.Schedule,
							contentDescription = stringResource(R.string.time)
					)
				}, summary = timeValue ?: noneStr, trailing = {
					if (timeValue != null) {
						IconButton(onClick = onClearTime) {
							Icon(
									Icons.Rounded.Close,
									contentDescription = stringResource(R.string.delete)
							)
						}
					}
				})
			}
			if (timeValue != null && dateValue != null) {
				item {
					// 目标时间距今 24 小时内时，只保留真正可行的提前量选项
					val presetMinutes = listOf(
							R.string.on_time to 0,
							R.string.five_mins to 5,
							R.string.fifteen_mins to 15,
							R.string.half_hour to 30,
							R.string.one_hour to 60,
							R.string.one_day to 1440,
					)
					val limited = availableMinutes != null && availableMinutes <= 1440
					val limit = (availableMinutes ?: 1440).coerceIn(0, 1440)
					val preset = presetMinutes.filter { !limited || it.second <= limit }
						.map { stringResource(it.first) }
					val customLabel = stringResource(R.string.custom_remind_title)
					// 自定义提醒值（如“45分钟”）不在预设里，动态追加为末尾条目，保证有选中态和清除按钮
					val customValue = remindValue?.takeIf { it !in preset && it != customLabel }
					// 不提供“无”条目,清除统一靠 trailing 的打叉按钮(onChange 回调带 null)
					val entries = preset + customLabel + listOfNotNull(customValue)
					val values: List<String?> = preset + customLabel + listOfNotNull(customValue)
					val selectedIndex = when (remindValue) {
						null -> null
						in preset -> preset.indexOf(remindValue)
						else -> entries.lastIndex
					}
					MenuPreference(
							title = stringResource(R.string.remind),
							icon = {
								Icon(
										Icons.Outlined.Notifications,
										contentDescription = stringResource(R.string.remind)
								)
							},
							summary = remindValue ?: noneStr,
							entries = entries,
							entryValues = values,
							selectedIndex = selectedIndex,
							onChange = { _, _, value ->
								if (value == customLabel) onCustomRemind() else onRemindSelected(
										value
								)
							},
					)
				}
			}
		}
	}

	@Composable
	fun SubtaskSection(
		subtasks: SnapshotStateList<JSONObject>,
		onAdd: () -> Unit,
		onDelete: (Int) -> Unit,
	) {
		PreferenceCategory {
			item {
				Preference(title = stringResource(R.string.subtask), onClick = onAdd, icon = {
					Icon(
							Icons.Rounded.SubdirectoryArrowRight,
							contentDescription = stringResource(R.string.subtask)
					)
				}, trailing = {
					Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.add))
				})                /*SegmentedListItem(
						onClick = onAdd,
						modifier = Modifier.fillMaxWidth(),
						shapes = ListItemDefaults.segmentedShapes(index = LocalPreferenceIndex.current, count = LocalPreferenceCount.current),
						colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
						leadingContent = {
							Icon(
									Icons.Rounded.SubdirectoryArrowRight,
									contentDescription = stringResource(R.string.subtask)
							)
						},
						trailingContent = {
							Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.add))
						}) {
					Text(
							stringResource(R.string.subtask),
							style = MaterialTheme.typography.bodyLargeEmphasized
					)
//					Text(timeValue ?: noneStr, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMediumEmphasized)
				}*/
			}
			itemsWithIndexed(subtasks) { index, item ->
				var isDone by remember {
					mutableStateOf(
							item.getIntValue(
									"status", TodoInfo.TODO
							) == TodoInfo.DONE
					)
				}
				var itemTitle by remember {
					mutableStateOf(item.getString("title", ""))
				}
				SegmentedListItem(
						onClick = {},
						modifier = Modifier.fillMaxWidth(),
						shapes = ListItemDefaults.segmentedShapes(
								index = LocalPreferenceIndex.current,
								count = LocalPreferenceCount.current
						),
						colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
						leadingContent = {
							Checkbox(checked = isDone, onCheckedChange = {
								item["status"] = if (it) TodoInfo.TODO else TodoInfo.DONE
								isDone = it
							})
						},
						trailingContent = {
							IconButton(onClick = { onDelete(index) }) {
								Icon(
										Icons.Rounded.Delete,
										contentDescription = stringResource(R.string.delete),
										tint = MaterialTheme.colorScheme.error
								)
							}
						}) {
					TextField(
							value = itemTitle,
							onValueChange = {
								item["title"] = it
								itemTitle = it
							},
							colors = TextFieldDefaults.colors(
									focusedContainerColor = Color.Transparent,
									unfocusedContainerColor = Color.Transparent,
									disabledContainerColor = Color.Transparent,
									focusedIndicatorColor = Color.Transparent,
									unfocusedIndicatorColor = Color.Transparent,
									disabledIndicatorColor = Color.Transparent
							),
							textStyle = MaterialTheme.typography.bodyMedium.copy(textDecoration = if (isDone) TextDecoration.LineThrough else TextDecoration.None),
							singleLine = true
					)
				}

			}
		}        /*subtasks.forEachIndexed { index, item ->
			var isDone by remember {
				mutableStateOf(
						item.getIntValue(
								"status", TodoInfo.TODO
						) == TodoInfo.DONE
				)
			}
			var itemTitle by remember {
				mutableStateOf(item.getString("title", ""))
			}
			SegmentedListItem(
					onClick = {},
					modifier = Modifier.fillMaxWidth(),
					shapes = ListItemDefaults.segmentedShapes(
							index = index + 1, count = 1 + subtasks.size
					),
					colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
					leadingContent = {
						Checkbox(checked = isDone, onCheckedChange = {
							item["status"] = if (it) TodoInfo.TODO else TodoInfo.DONE
							isDone = it
						})
					},
					trailingContent = {
						IconButton(onClick = { onDelete(index) }) {
							Icon(
									Icons.Rounded.Delete,
									contentDescription = stringResource(R.string.delete),
									tint = MaterialTheme.colorScheme.error
							)
						}
					}) {
				TextField(
						value = itemTitle,
						onValueChange = {
							item["title"] = it
							itemTitle = it
						},
						colors = TextFieldDefaults.colors(
								focusedContainerColor = Color.Transparent,
								unfocusedContainerColor = Color.Transparent,
								disabledContainerColor = Color.Transparent,
								focusedIndicatorColor = Color.Transparent,
								unfocusedIndicatorColor = Color.Transparent,
								disabledIndicatorColor = Color.Transparent
						),
						textStyle = MaterialTheme.typography.bodyMedium.copy(textDecoration = if (isDone) TextDecoration.LineThrough else TextDecoration.None),
						singleLine = true
				)
			}
		}*/
	}

	@Composable
	fun ColorPickerSection(
		colors: List<String>,
		selectedColor: String?,
		onColorSelected: (String) -> Unit,
	) {
		Preference(title = stringResource(R.string.color), icon = {
			Icon(
					Icons.Rounded.ColorLens, contentDescription = stringResource(R.string.color)
			)
		}, trailing = {
			if (selectedColor != null) {
				IconButton(onClick = { onColorSelected("") }) {
					Icon(
							Icons.Rounded.Close,
							contentDescription = stringResource(R.string.delete)
					)
				}
			}
		}, content = {
			Row(
					modifier = Modifier
						.horizontalScroll(rememberScrollState())
						.fillMaxWidth()
						.padding(vertical = dimensionResource(R.dimen.vertical_padding)),
					horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.horizontal_gap))
			) {
				colors.forEach { colorStr ->
					val color = try {
						Color(colorStr.toColorInt())
					} catch (_: Exception) {
						Color.Gray
					}
					val isSelected = colorStr == selectedColor
					Box(
							modifier = Modifier
								.size(24.dp)
								.clip(CircleShape)
								.background(color)
								.then(
										if (isSelected) Modifier.border(
												3.dp, Color.White, CircleShape
										) else Modifier
								)
								.clickable { onColorSelected(colorStr) })
				}
			}
		})
	}

	@Composable
	fun AddItemDialog(
		title: String,
		value: String,
		onValueChange: (String) -> Unit,
		onDismiss: () -> Unit,
		onConfirm: (String) -> Unit,
	) {
		AlertDialog(onDismissRequest = onDismiss, title = { Text(title) }, text = {
			OutlinedTextField(value = value, onValueChange = onValueChange, label = { Text(title) })
		}, confirmButton = {
			TextButton(onClick = { onConfirm(value) }, shapes = ButtonDefaults.shapes()) {
				Text(
						stringResource(R.string.confirm)
				)
			}
		}, dismissButton = {
			TextButton(onClick = onDismiss, shapes = ButtonDefaults.shapes()) {
				Text(
						stringResource(
								R.string.cancel
						)
				)
			}
		})
	}

	@Composable
	fun TodoListScreen(
		modifier: Modifier = Modifier,
		todoList: List<TodoEntity>,
		isAddRequested: Boolean = false,
		onAddHandled: () -> Unit = {},
		content: @Composable () -> Unit,
	) {
		var showAddDialog by remember { mutableStateOf(false) }
		var showEditDialog by remember { mutableStateOf(false) }
		var editingTodo by remember { mutableStateOf<TodoEntity?>(null) }
		var copyTodo by remember { mutableStateOf(TodoEntity()) }

		if (isAddRequested) {
			copyTodo = TodoEntity()
			showAddDialog = true
			onAddHandled()
		}

		if (showAddDialog) {
			TodoDetailSheet(
					initialTodo = copyTodo,
					isAdd = true,
					onDismiss = { showAddDialog = false; copyTodo = TodoEntity() },
			)
		}

		if (showEditDialog && editingTodo != null) {
			TodoDetailSheet(
					initialTodo = editingTodo!!,
					isAdd = false,
					onDismiss = { showEditDialog = false; editingTodo = null },
					onDelete = {
						deleteTodo(editingTodo!!.id)
						showEditDialog = false
						editingTodo = null
					},
			)
		}

		if (todoList.isEmpty()) content()
		else {
			val grouped = todoList.groupBy { it.dueDate ?: "无预定日期" }
			Column(
					modifier = modifier
						.fillMaxWidth()
						.padding(
								vertical = dimensionResource(R.dimen.vertical_padding)
						)
			) {
				grouped.forEach { (dateHeader, todos) ->
					Text(
							dateHeader,
							style = MaterialTheme.typography.titleMedium,
							modifier = Modifier.padding(
									dimensionResource(R.dimen.horizontal_padding),
									dimensionResource(R.dimen.vertical_padding)
							)
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
	}

	@Composable
	fun TodoItem(
		todo: TodoEntity,
		index: Int = 0,
		count: Int = 1,
		color: Color = MaterialTheme.colorScheme.surfaceContainer,
		onClick: () -> Unit = {},
		onToggle: () -> Unit = {},
		onDelete: () -> Unit = {},
		onCopy: () -> Unit = {},
	) {
		var isDone by remember { mutableStateOf(todo.status == TodoInfo.DONE) }
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
//				colors = ListItemDefaults.colors(containerColor = color),
				leadingContent = {
					Checkbox(checked = isDone, onCheckedChange = {
						onToggle()
						isDone = it
					})
				},
				overlineContent = {
					Text(
							todo.title ?: "",
							style = MaterialTheme.typography.bodyLarge,
							color = itemColor ?: MaterialTheme.colorScheme.onSurface,
							textDecoration = if (isDone) TextDecoration.LineThrough else TextDecoration.None,
							modifier = Modifier.alpha(if (isDone) 0.5f else 1f)
					)
				},
				trailingContent = {
					SingleChoiceSegmentedButtonRow {
						SegmentedButton(
								selected = false,
								onClick = onCopy,
								contentPadding = PaddingValues(0.dp),
								shape = SegmentedButtonDefaults.itemShape(0, 2),
								label = {
									Icon(
											Icons.Rounded.ContentCopy,
											contentDescription = stringResource(R.string.copy),
											tint = MaterialTheme.colorScheme.primary
									)
								},
								icon = {})

						SegmentedButton(
								selected = false,
								contentPadding = PaddingValues(0.dp),
								onClick = onDelete,
								shape = SegmentedButtonDefaults.itemShape(1, 2),
								label = {
									Icon(
											Icons.Rounded.Delete,
											contentDescription = stringResource(R.string.delete),
											tint = MaterialTheme.colorScheme.error
									)
								},
								icon = {})
					}
				},
				supportingContent = {
					val detailParts = mutableListOf<String>()
					todo.todoType?.let { detailParts.add(it) }
					todo.subject?.let { detailParts.add("${stringResource(R.string.subject)}:${it}") }
					todo.location?.let { detailParts.add("${stringResource(R.string.location)}:${it}") }
					todo.ddl?.let { detailParts.add("${stringResource(R.string.ddl)}:${it}") }
					todo.remindTime?.let { detailParts.add("${stringResource(R.string.remind)}:${it}") }
					if (todo.priority > 0) detailParts.add(priorityLabels[todo.priority])
					if (detailParts.isNotEmpty()) {
						Text(
								detailParts.joinToString(" | "),
								style = MaterialTheme.typography.labelSmall,
								color = MaterialTheme.colorScheme.onSurfaceVariant
						)
					}
				}) {}
	}
}