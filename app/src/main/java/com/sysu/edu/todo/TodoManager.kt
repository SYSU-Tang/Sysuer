package com.sysu.edu.todo

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.view.Gravity
import android.view.Menu
import android.view.View
import android.view.WindowManager
import android.widget.CompoundButton
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.NumberPicker
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.PopupMenu
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.toColorInt
import androidx.core.view.children
import androidx.core.view.get
import androidx.core.view.isEmpty
import androidx.core.view.setMargins
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.alibaba.fastjson2.JSONArray
import com.alibaba.fastjson2.JSONObject
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.listitem.ListItemLayout
import com.google.android.material.shape.ShapeAppearanceModel
import com.google.android.material.slider.Slider
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.timepicker.MaterialTimePicker
import com.sysu.edu.R
import com.sysu.edu.api.CalendarManager
import com.sysu.edu.databinding.DialogTodoBinding
import com.sysu.edu.databinding.ItemFilterChipBinding
import com.sysu.edu.databinding.ItemPreferenceBinding
import com.sysu.edu.databinding.ItemTodoBinding
import com.sysu.edu.view.AdapterListener
import com.sysu.edu.view.EditTextDialog
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class TodoManager(private val activity: FragmentActivity,
                  private val concatAdapter: ConcatAdapter) {
	private var function: Int = TodoInfo.ADD
	private var toAddCode: Int = 0
	var todoInfo: TodoEntity = TodoEntity()
	private var refreshListener: OnRefreshListener? = null
	private val colors = listOf("#757575", "#F44336", "#E91E63", "#9C27B0", "#673AB7", "#3F51B5", "#2196F3", "#03A9F4", "#00BCD4", "#009688", "#4CAF50", "#8BC34A", "#CDDC39", "#FFEB3B", "#FFC107", "#FF9800", "#FF5722", "#795548", "#607D8B")
	private val subtaskAdapter by lazy {
		SubtaskAdapter { adapter ->
			todoInfo.subtask.clear()
			dialogTodoBinding.subtaskHeader.updateAppearance(if (adapter.data.isEmpty()) ListItemLayout.POSITION_SINGLE else ListItemLayout.POSITION_FIRST)
			todoInfo.subtask = JSONArray(adapter.data)
		}
	}
	val todoModel: TodoModel by lazy {
		val repository = TodoRepository(TodoDatabase.getDatabase(activity, activity.lifecycleScope)
											.todoDao())
		ViewModelProvider(activity, TodoModelFactory(repository))[TodoModel::class.java]
	}
	private val dialogTodoBinding: DialogTodoBinding by lazy {
		DialogTodoBinding.inflate(activity.layoutInflater).apply {
			prioritySlider.addOnChangeListener { _: Slider?, value: Float, _: Boolean ->
				todoInfo.priority = value.toInt()
				priorityValue.text = activity.resources.getStringArray(R.array.priority)[value.toInt()]
			}
			todoType.setOnCheckedStateChangeListener { group, checkedIds ->
				todoInfo.todoType = checkedIds.firstOrNull()
					?.let { types.getOrNull(group.indexOfChild(group.findViewById(it)) - 1)?.name }
					?: ""
			}
			subject.setOnCheckedStateChangeListener { group, checkedIds ->
				todoInfo.subject = checkedIds.firstOrNull()
					?.let { subjects.getOrNull(group.indexOfChild(group.findViewById(it)) - 1)?.name }
					?: ""
			}
			tag.setOnCheckedStateChangeListener { group, checkedIds ->
				val array = JSONArray()
				checkedIds.forEach { id ->
					tags.getOrNull(group.indexOfChild(group.findViewById(id)) - 1)?.name?.let { array.add(it) }
				}
				todoInfo.tag = array
			}
			priorityContainer.updateAppearance(0, 2)
			colorContainer.updateAppearance(1, 2)
		}
	}
	private val todoDetailDialog: AlertDialog by lazy {
		MaterialAlertDialogBuilder(activity).setView(dialogTodoBinding.root)
			.setPositiveButton(R.string.confirm) { _: DialogInterface?, _: Int ->
				todoInfo.title = dialogTodoBinding.title.text.toString()
				todoInfo.description = dialogTodoBinding.description.text.toString()
				activity.lifecycleScope.launch {
					when (function) {
						TodoInfo.ADD -> {
							val id = todoModel.addTodo(todoInfo).await().toInt()
							todoInfo = todoInfo.copy(id = id)
							scheduleReminder(todoInfo)
						}
						TodoInfo.VIEW -> {
							todoModel.updateTodo(todoInfo).join()
							scheduleReminder(todoInfo)
						}
					}
					performRefresh()
				}
			}
			.setNegativeButton(R.string.cancel, null)
			.setNeutralButton(R.string.delete) { _: DialogInterface?, _: Int ->
				if (function == TodoInfo.VIEW) {
					activity.lifecycleScope.launch {
						cancelReminder(todoInfo.id)
						todoModel.deleteTodo(todoInfo.id).join()
						performRefresh()
					}
				}
			}
			.create()
			.also { setupDialogWindow(it) }
	}
	private val types: List<TypeEntity?> get() = todoModel.types.value ?: emptyList()
	private val subjects: List<SubjectEntity?> get() = todoModel.subjects.value ?: emptyList()
	private val tags: List<TagEntity?> get() = todoModel.tags.value ?: emptyList()
	
	init {
		setupViewModel()
		setupObservers()
		setupPickersAndInputs()
		setupTodoListObserver()
	}
	
	private fun setupViewModel() {
		todoModel.loadTypes()
		todoModel.loadSubjects()
		todoModel.loadTags()
	}
	
	private fun setupObservers() {
		todoModel.subjects.observe(activity) { list ->
			updateChips(dialogTodoBinding.subject, list, 1, todoInfo.subject)
		}
		todoModel.tags.observe(activity) { list ->
			updateChips(dialogTodoBinding.tag, list, 2, todoInfo.tag)
		}
		todoModel.types.observe(activity) { list ->
			updateChips(dialogTodoBinding.todoType, list, 0, todoInfo.todoType)
		}
	}
	
	private fun updateChips(group: ChipGroup, list: List<Any?>, code: Int, selection: Any?) {
		removeChips(group)
		list.forEach { entity ->
			val name = when (entity) {
				is TypeEntity -> entity.name
				is SubjectEntity -> entity.name
				is TagEntity -> entity.name
				else -> null
			}
			createFilterChip(name, group, code)
		}
		when (selection) {
			is String -> {
				if (selection.isNotEmpty()) {
					val names = list.map {
						(it as? TypeEntity)?.name ?: (it as? SubjectEntity)?.name
					}
					if (names.none { it == selection }) createFilterChip(selection, group, code)
					selectChipIfPresent(group, selection)
				}
				else group.clearCheck()
			}
			is JSONArray -> {
				if (selection.isNotEmpty()) {
					selection.forEach { v ->
						val tagName = v as String
						if (list.none { (it as? TagEntity)?.name == tagName }) createFilterChip(tagName, group, code)
					}
					selectChipIfPresent(group, selection)
				}
				else group.clearCheck()
			}
		}
	}
	
	private fun setupPickersAndInputs() {
		val calendarManager = CalendarManager()
		val todoItemAddDialog = EditTextDialog(activity).apply {
			getDialog().setButton(AlertDialog.BUTTON_POSITIVE, activity.getString(R.string.confirm)) { _: DialogInterface?, _: Int ->
				val value = this.value
				if (!value.isNullOrEmpty()) {
					when (toAddCode) {
						0 -> {
							todoModel.addType(value); todoInfo.todoType = value
						}
						1 -> {
							todoModel.addSubject(value); todoInfo.subject = value
						}
						2 -> {
							todoModel.addTag(value); todoInfo.tag.add(value)
						}
					}
				}
			}
		}
		val addButtons = listOf(dialogTodoBinding.todoTypeAdd, dialogTodoBinding.todoSubjectAdd, dialogTodoBinding.todoTagAdd)
		val hints = listOf(R.string.type, R.string.subject, R.string.tag)
		addButtons.forEachIndexed { index, button ->
			button.setOnClickListener {
				toAddCode = index
				todoItemAddDialog.setHint(hints[index])
				todoItemAddDialog.setTitle(hints[index])
				todoItemAddDialog.show()
			}
		}
		
		setupPreferenceItems(calendarManager)
		dialogTodoBinding.check.setOnCheckedChangeListener { _: CompoundButton?, isChecked: Boolean ->
			todoInfo.status = if (isChecked) TodoInfo.DONE else TodoInfo.TODO
		}
	}
	
	private fun setupPreferenceItems(calendarManager: CalendarManager) {
		val datePicker = MaterialDatePicker.Builder.datePicker().setTitleText(R.string.date).build()
		val ddlPicker = MaterialDatePicker.Builder.datePicker().setTitleText(R.string.ddl).build()
		val timePicker = MaterialTimePicker.Builder().setTitleText(R.string.time).build()
		val ddlTimePicker = MaterialTimePicker.Builder()
			.setTitleText(R.string.time)
			.build()        // --- Section 1: Due Info ---
		val dateBinding = ItemPreferenceBinding.inflate(activity.layoutInflater, dialogTodoBinding.times, false)
			.apply {
				itemTitle.text = activity.getString(R.string.date)
				itemIcon.setImageResource(R.drawable.calendar)
			}
		setupDateMenu(dateBinding, datePicker, calendarManager) { date -> todoInfo.dueDate = date }
		val timeBinding = ItemPreferenceBinding.inflate(activity.layoutInflater, dialogTodoBinding.times, false)
			.apply {
				itemTitle.text = activity.getString(R.string.time)
				itemIcon.setImageResource(R.drawable.time)
			}
		val timeMenu = PopupMenu(activity, timeBinding.root, Gravity.NO_GRAVITY, 0, com.google.android.material.R.style.Widget_Material3_PopupMenu_Overflow)
		timeMenu.menu.apply {
			add(0, Menu.NONE, Menu.NONE, R.string.none).setOnMenuItemClickListener {
				todoInfo.dueTime = null; todoInfo.remindTime = null; updatePreferenceUI(); true
			}
			add(1, Menu.NONE, Menu.NONE, R.string.select).setOnMenuItemClickListener {
				timePicker.show(activity.supportFragmentManager, "time_picker"); true
			}
		}
		timeBinding.root.setOnClickListener { timeMenu.show() }
		val remindBinding = ItemPreferenceBinding.inflate(activity.layoutInflater, dialogTodoBinding.times, false)
			.apply {
				itemTitle.text = activity.getString(R.string.remind)
				itemIcon.setImageResource(R.drawable.alarm)
			}
		remindBinding.root.setOnClickListener {
			setupRemindMenu(remindBinding) { time -> todoInfo.remindTime = time; updatePreferenceUI() }.show()
		}        // --- Section 2: Deadline Info ---
		val ddlBinding = ItemPreferenceBinding.inflate(activity.layoutInflater, dialogTodoBinding.deadlineTimes, false)
			.apply {
				itemTitle.text = activity.getString(R.string.ddl)
				itemIcon.setImageResource(R.drawable.warning)
			}
		setupDateMenu(ddlBinding, ddlPicker, calendarManager) { date -> todoInfo.ddl = date }
		val ddlTimeBinding = ItemPreferenceBinding.inflate(activity.layoutInflater, dialogTodoBinding.deadlineTimes, false)
			.apply {
				itemTitle.text = activity.getString(R.string.time)
				itemIcon.setImageResource(R.drawable.time)
			}
		val ddlTimeMenu = PopupMenu(activity, ddlTimeBinding.root, Gravity.NO_GRAVITY, 0, com.google.android.material.R.style.Widget_Material3_PopupMenu_Overflow)
		ddlTimeMenu.menu.apply {
			add(0, Menu.NONE, Menu.NONE, R.string.none).setOnMenuItemClickListener {
				todoInfo.ddlTime = null; todoInfo.ddlRemindTime = null; updatePreferenceUI(); true
			}
			add(1, Menu.NONE, Menu.NONE, R.string.select).setOnMenuItemClickListener {
				ddlTimePicker.show(activity.supportFragmentManager, "ddl_time_picker"); true
			}
		}
		ddlTimeBinding.root.setOnClickListener { ddlTimeMenu.show() }
		val ddlRemindBinding = ItemPreferenceBinding.inflate(activity.layoutInflater, dialogTodoBinding.deadlineTimes, false)
			.apply {
				itemTitle.text = activity.getString(R.string.remind)
				itemIcon.setImageResource(R.drawable.alarm)
			}
		ddlRemindBinding.root.setOnClickListener {
			setupRemindMenu(ddlRemindBinding) { time -> todoInfo.ddlRemindTime = time; updatePreferenceUI() }.show()
		}        // --- Section 3: Subtask Header ---
		dialogTodoBinding.subtaskHeader.updateAppearance(ListItemLayout.POSITION_FIRST)
		dialogTodoBinding.subtaskHeader.setOnClickListener {
			EditTextDialog(activity).apply {
				setTitle(R.string.subtask)
				setHint(R.string.title)
				getDialog().setButton(AlertDialog.BUTTON_POSITIVE, activity.getString(R.string.confirm)) { _, _ ->
					val title = this.value
					if (!title.isNullOrEmpty()) {
						val subtask = JSONObject.of("title", title, "status", TodoInfo.TODO)
						subtaskAdapter.add(subtask)
						todoInfo.subtask.add(subtask)
					}
				}
				show()
			}
		}
		dialogTodoBinding.subtask.adapter = subtaskAdapter        // Add to containers
		dialogTodoBinding.times.apply { addView(dateBinding.root); addView(timeBinding.root); addView(remindBinding.root) }
		dialogTodoBinding.deadlineTimes.apply { addView(ddlBinding.root); addView(ddlTimeBinding.root); addView(ddlRemindBinding.root) }        // Global Listeners
		datePicker.addOnPositiveButtonClickListener { selection -> todoInfo.dueDate = calendarManager.toDateString(selection); updatePreferenceUI() }
		ddlPicker.addOnPositiveButtonClickListener { selection -> todoInfo.ddl = calendarManager.toDateString(selection); updatePreferenceUI() }
		timePicker.addOnPositiveButtonClickListener { todoInfo.dueTime = String.format(Locale.getDefault(), "%02d:%02d", timePicker.hour, timePicker.minute); updatePreferenceUI() }
		ddlTimePicker.addOnPositiveButtonClickListener { todoInfo.ddlTime = String.format(Locale.getDefault(), "%02d:%02d", ddlTimePicker.hour, ddlTimePicker.minute); updatePreferenceUI() }
	}
	
	private fun setupRemindMenu(binding: ItemPreferenceBinding,
	                            onSelected: (String?) -> Unit): PopupMenu {
		val menu = PopupMenu(activity, binding.root, Gravity.NO_GRAVITY, 0, com.google.android.material.R.style.Widget_Material3_PopupMenu_Overflow)
		menu.menu.apply {
			add(0, Menu.NONE, Menu.NONE, R.string.none).setOnMenuItemClickListener { onSelected(null); true }
			listOf(R.string.on_time, R.string.five_mins, R.string.fifteen_mins, R.string.half_hour, R.string.one_hour, R.string.one_day).forEach { res ->
				add(0, Menu.NONE, Menu.NONE, res).setOnMenuItemClickListener { item -> onSelected(item.title.toString()); true }
			}
			add(1, Menu.NONE, Menu.NONE, R.string.custom).setOnMenuItemClickListener {
				val numberPicker = NumberPicker(activity).apply { minValue = 0; maxValue = 59 }
				MaterialAlertDialogBuilder(activity).setTitle(R.string.custom_remind_title)
					.setView(numberPicker)
					.setPositiveButton(R.string.confirm) { _, _ ->
						onSelected(String.format(Locale.getDefault(), "%02d%s", numberPicker.value, activity.getString(R.string.minute)))
					}
					.show(); true
			}
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) setGroupDividerEnabled(true)
		}
		return menu
	}
	
	private fun setupDateMenu(binding: ItemPreferenceBinding,
	                          picker: MaterialDatePicker<Long>,
	                          calendarManager: CalendarManager,
	                          onDateSelected: (String?) -> Unit) {
		val popupMenu = PopupMenu(activity, binding.root, Gravity.NO_GRAVITY, 0, com.google.android.material.R.style.Widget_Material3_PopupMenu_Overflow)
		popupMenu.menu.apply {
			add(0, Menu.NONE, Menu.NONE, R.string.none).setOnMenuItemClickListener { onDateSelected(null); updatePreferenceUI(); true }
			listOf(R.string.today, R.string.tomorrow, R.string.next_week).forEachIndexed { j, res ->
				add(0, Menu.NONE, Menu.NONE, res).setOnMenuItemClickListener { onDateSelected(calendarManager.toDateStringPLus(listOf(0, 1, 7)[j])); updatePreferenceUI(); true }
			}
			add(1, Menu.NONE, Menu.NONE, R.string.select).setOnMenuItemClickListener { picker.show(activity.supportFragmentManager, "date_picker"); true }
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) setGroupDividerEnabled(true)
		}
		binding.root.setOnClickListener { popupMenu.show() }
	}
	
	private fun updatePreferenceUI() {
		with(dialogTodoBinding) {
			val noneStr = activity.getString(R.string.none)            // Update Due Section
			val dateItem = ItemPreferenceBinding.bind(times[0])
			val timeItem = ItemPreferenceBinding.bind(times[1])
			val remindItem = ItemPreferenceBinding.bind(times[2])
			
			dateItem.itemContent.text = todoInfo.dueDate ?: noneStr
			timeItem.itemContent.text = todoInfo.dueTime ?: noneStr
			val hasDueTime = !todoInfo.dueTime.isNullOrEmpty()
			remindItem.root.visibility = if (hasDueTime) View.VISIBLE else View.GONE
			if (!hasDueTime) todoInfo.remindTime = null
			remindItem.itemContent.text = todoInfo.remindTime ?: noneStr
			val visibleDueCount = if (hasDueTime) 3 else 2
			dateItem.root.updateAppearance(0, visibleDueCount)
			timeItem.root.updateAppearance(1, visibleDueCount)
			if (hasDueTime) remindItem.root.updateAppearance(2, visibleDueCount)            // Update DDL Section
			val ddlItem = ItemPreferenceBinding.bind(deadlineTimes[0])
			val ddlTimeItem = ItemPreferenceBinding.bind(deadlineTimes[1])
			val ddlRemindItem = ItemPreferenceBinding.bind(deadlineTimes[2])
			
			ddlItem.itemContent.text = todoInfo.ddl ?: noneStr
			ddlTimeItem.itemContent.text = todoInfo.ddlTime ?: noneStr
			val hasDdlTime = !todoInfo.ddlTime.isNullOrEmpty()
			ddlRemindItem.root.visibility = if (hasDdlTime) View.VISIBLE else View.GONE
			if (!hasDdlTime) todoInfo.ddlRemindTime = null
			ddlRemindItem.itemContent.text = todoInfo.ddlRemindTime ?: noneStr
			val visibleDdlCount = if (hasDdlTime) 3 else 2
			ddlItem.root.updateAppearance(0, visibleDdlCount)
			ddlTimeItem.root.updateAppearance(1, visibleDdlCount)
			if (hasDdlTime) ddlRemindItem.root.updateAppearance(2, visibleDdlCount)
		}
	}
	
	private fun setupTodoListObserver() {
		todoModel.todoList.observe(activity) { todoList ->
			concatAdapter.adapters.toList().forEach { concatAdapter.removeAdapter(it) }
			val dueMap = mutableMapOf<String?, TodoAdapter>()
			todoList.forEach { todoDetail ->
				val adapter = dueMap.getOrPut(todoDetail.dueDate) {
					concatAdapter.addAdapter(TitleAdapter(todoDetail.dueDate ?: "无预定日期"))
					TodoAdapter().apply {
						listener = createAdapterListener(this)
						concatAdapter.addAdapter(this)
					}
				}
				adapter.add(todoDetail)
			}
		}
	}
	
	private fun createAdapterListener(todoAdapter: TodoAdapter) = object : AdapterListener {
		override fun onBind(adapter: RecyclerView.Adapter<RecyclerView.ViewHolder?>,
		                    holder: RecyclerView.ViewHolder,
		                    position: Int) {
			ItemTodoBinding.bind(holder.itemView).apply {
				val item = todoAdapter.get(position)
				delete.setOnClickListener {
					activity.lifecycleScope.launch {
						cancelReminder(item.id)
						todoModel.deleteTodo(item.id).join()
						performRefresh()
					}
				}
				root.setOnClickListener {
					initDialog(item)
					todoDetailDialog.show()
				}
				copy.setOnClickListener {
					initDialog(item)
					function = TodoInfo.ADD
					todoDetailDialog.show()
				}
				check.setOnCheckedChangeListener { _: CompoundButton?, isChecked: Boolean ->
					item.status = if (isChecked) TodoInfo.DONE else TodoInfo.TODO
					if (isChecked) {
						item.doneDateTime = LocalDateTime.now()
							.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
						cancelReminder(item.id)
					}
					else {
						scheduleReminder(item)
					}
					
					activity.lifecycleScope.launch {
						todoModel.updateTodo(item).join()
						performRefresh()
					}
				}
			}
		}
		
		override fun onCreate(adapter: RecyclerView.Adapter<RecyclerView.ViewHolder?>,
		                      binding: ViewBinding?) {
		}
	}
	
	private fun removeChips(tag: ChipGroup) {
		if (tag.childCount > 2) tag.removeViews(1, tag.childCount - 2)
	}
	
	fun performRefresh() {
		refreshListener?.onRefresh()
	}
	
	fun refresh(where: String, args: Array<String>) {
		todoModel.loadTodos(where, args)
	}
	
	private fun createFilterChip(s: String?, view: ChipGroup, toAddCode: Int) {
		val chip = ItemFilterChipBinding.inflate(activity.layoutInflater, view, false).root.apply {
			text = s ?: ""
			setOnLongClickListener {
				Snackbar.make(this, R.string.delete_warning, Snackbar.LENGTH_LONG)
					.setAction(R.string.delete) {
						if (isChecked) {
							when (toAddCode) {
								0 -> todoInfo.todoType = null
								1 -> todoInfo.subject = null
								2 -> todoInfo.tag.remove(s)
							}
						}
						when (toAddCode) {
							0 -> todoModel.deleteType(s!!)
							1 -> todoModel.deleteSubject(s!!)
							2 -> todoModel.deleteTag(s!!)
						}
					}
					.show()
				true
			}
			
			setOnCheckedChangeListener { _: CompoundButton?, isChecked: Boolean ->
				val value = "$text"
				when (toAddCode) {
					0 -> if (isChecked) todoInfo.todoType = value else if (todoInfo.todoType == value) todoInfo.todoType = null
					1 -> if (isChecked) todoInfo.subject = value else if (todoInfo.subject == value) todoInfo.subject = null
					2 -> if (isChecked) {
						if (value !in todoInfo.tag.map { it as String }) todoInfo.tag.add(value)
					}
					else {
						todoInfo.tag.remove(value)
					}
				}
			}
		}
		view.addView(chip, view.childCount - 1)
	}
	
	fun initDialog(todoInfo: TodoEntity) {
		function = TodoInfo.VIEW
		this.todoInfo = todoInfo
		initDialog()
	}
	
	fun initDialog() {
		with(dialogTodoBinding) {
			title.setText(todoInfo.title)
			description.setText(todoInfo.description)
			todoInfo.priority.let {
				val priority = it.takeIf { p -> p in 0..4 } ?: 0
				prioritySlider.value = priority.toFloat()
				priorityValue.text = activity.resources.getStringArray(R.array.priority)[priority]
			}
			
			updateChips(todoType, types, 0, todoInfo.todoType)
			updateChips(subject, subjects, 1, todoInfo.subject)
			updateChips(tag, tags, 2, todoInfo.tag)
			
			check.isChecked = todoInfo.status == TodoInfo.DONE
			updatePreferenceUI()
			setupColorList()
			subtaskAdapter.set(todoInfo.subtask.map { it as JSONObject }.toMutableList())
		}
	}
	
	private fun setupColorList() {
		val colorGroup = dialogTodoBinding.colorList
		if (colorGroup.isEmpty()) colors.forEach { colorStr -> colorGroup.addView(createColorDot(colorStr)) }
		else colors.forEachIndexed { index, colorStr ->
			(colorGroup[index] as? ShapeableImageView)?.let { updateColorDot(it, colorStr) }
		}
	}
	
	private fun createColorDot(colorStr: String) = ShapeableImageView(activity).apply {
		val size = dp2px(32f)
		val margin = dp2px(4f)
		layoutParams = LinearLayout.LayoutParams(size, size).apply { setMargins(margin) }
		shapeAppearanceModel = ShapeAppearanceModel.builder()
			.setAllCornerSizes(ShapeAppearanceModel.PILL)
			.build()
		updateColorDot(this, colorStr)
		setOnClickListener {
			todoInfo.color = if (todoInfo.color == colorStr) null else colorStr
			setupColorList()
		}
	}
	
	private fun updateColorDot(dot: ShapeableImageView, colorStr: String) {
		val color = colorStr.toColorInt()
		dot.setImageDrawable(GradientDrawable().apply {
			shape = GradientDrawable.OVAL
			setColor(color)
			if (todoInfo.color == colorStr) {
				setStroke(dp2px(3f), if (ColorUtils.calculateLuminance(color) < 0.5) Color.WHITE else Color.GRAY)
			}
		})
	}
	
	private fun dp2px(dp: Float): Int =
		(dp * activity.resources.displayMetrics.density + 0.5f).toInt()
	
	fun showTodoAddDialog() {
		todoInfo = TodoEntity()
		function = TodoInfo.ADD
		initDialog()
		todoDetailDialog.show()
	}
	
	private fun setupDialogWindow(dialog: AlertDialog) {
		dialog.apply {
			setCanceledOnTouchOutside(false)
			window?.apply {
				setWindowAnimations(com.google.android.material.R.style.Animation_Design_BottomSheetDialog)
				addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
			}
			findViewById<FrameLayout?>(android.R.id.content)?.setPadding(48, 48, 48, 0)
		}
	}
	
	private fun selectChipIfPresent(group: ChipGroup, value: String?) {
		if (value.isNullOrEmpty()) return
		for (i in 0 until group.childCount) {
			(group.getChildAt(i) as? Chip)?.let {
				if (it.text.toString() == value) it.isChecked = true
			}
		}
	}
	
	private fun selectChipIfPresent(group: ChipGroup, value: JSONArray) {
		val tagList = value.map { it as String }
		group.children.forEach {
			(it as? Chip)?.let { chip ->
				if (chip.text.toString() in tagList) chip.isChecked = true
			}
		}
	}
	
	fun setOnRefreshListener(listener: OnRefreshListener) {
		refreshListener = listener
		listener.onRefresh()
	}
	
	private fun scheduleReminder(todo: TodoEntity) {        // 1. Schedule Due Reminder
		scheduleSingleReminder(todo.id, todo.title, todo.description, todo.status, todo.dueDate, todo.dueTime, todo.remindTime)        // 2. Schedule DDL Reminder (Use ID offset for DDL)
		scheduleSingleReminder(todo.id + 1000000, "[DDL] ${todo.title}", todo.description, todo.status, todo.ddl, todo.ddlTime, todo.ddlRemindTime)
	}
	
	private fun scheduleSingleReminder(notificationId: Int,
	                                   title: String?,
	                                   description: String?,
	                                   status: Int,
	                                   date: String?,
	                                   time: String?,
	                                   remindStr: String?) {
		if (status == TodoInfo.DONE || date.isNullOrEmpty() || time.isNullOrEmpty() || remindStr.isNullOrEmpty()) {
			cancelSingleReminder(notificationId)
			return
		}
		
		try {
			val dueDate = LocalDate.parse(date)
			val dueTime = LocalTime.parse(time)
			var remindDateTime = LocalDateTime.of(dueDate, dueTime)
			val remindMinutes = when (remindStr) {
				activity.getString(R.string.on_time) -> 0
				activity.getString(R.string.five_mins) -> 5
				activity.getString(R.string.fifteen_mins) -> 15
				activity.getString(R.string.half_hour) -> 30
				activity.getString(R.string.one_hour) -> 60
				activity.getString(R.string.one_day) -> 1440
				else -> {
					val minuteStr = activity.getString(R.string.minute)
					if (remindStr.endsWith(minuteStr)) {
						remindStr.replace(minuteStr, "").toIntOrNull() ?: 0
					}
					else 0
				}
			}
			
			remindDateTime = remindDateTime.minusMinutes(remindMinutes.toLong())
			if (remindDateTime.isBefore(LocalDateTime.now())) return
			val alarmManager = activity.getSystemService(AlarmManager::class.java)
			val intent = Intent(activity, TodoReminderReceiver::class.java).apply {
				putExtra("todo_id", todoInfo.id)
				putExtra("notification_id", notificationId)
				putExtra("todo_title", title)
				putExtra("todo_description", description)
			}
			val pendingIntent = PendingIntent.getBroadcast(activity, notificationId, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
			val triggerAtMillis = remindDateTime.atZone(ZoneId.systemDefault())
				.toInstant()
				.toEpochMilli()
			alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
		} catch (_: Exception) {
		}
	}
	
	private fun cancelReminder(id: Int) {
		cancelSingleReminder(id)
		cancelSingleReminder(id + 1000000)
	}
	
	private fun cancelSingleReminder(notificationId: Int) {
		val alarmManager = activity.getSystemService(Context.ALARM_SERVICE) as AlarmManager
		val intent = Intent(activity, TodoReminderReceiver::class.java)
		val pendingIntent = PendingIntent.getBroadcast(activity, notificationId, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_NO_CREATE)
		if (pendingIntent != null) {
			alarmManager.cancel(pendingIntent)
			pendingIntent.cancel()
		}
	}
	
	fun interface OnRefreshListener {
		fun onRefresh()
	}
}
