package com.sysu.edu.home

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.PopupMenu
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.core.net.toUri
import androidx.core.widget.NestedScrollView
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModelProvider
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.alibaba.fastjson2.JSON
import com.alibaba.fastjson2.JSONObject
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.datepicker.MaterialDatePicker
import com.sysu.edu.BaseFragment
import com.sysu.edu.ClassNotificationWorker
import com.sysu.edu.MainActivity
import com.sysu.edu.R
import com.sysu.edu.academic.AgendaActivity
import com.sysu.edu.academic.CourseDetailActivity
import com.sysu.edu.academic.CourseScheduleActivity
import com.sysu.edu.academic.ExamActivity
import com.sysu.edu.api.CalendarManager
import com.sysu.edu.api.CommonUtil
import com.sysu.edu.api.PreferenceViewModel
import com.sysu.edu.browser.BrowserActivity
import com.sysu.edu.databinding.DialogServiceActionBinding
import com.sysu.edu.databinding.DialogServiceOrderBinding
import com.sysu.edu.databinding.FragmentDashboardBinding
import com.sysu.edu.databinding.ItemExamBinding
import com.sysu.edu.databinding.ItemHomeCourseBinding
import com.sysu.edu.model.JwxtModel
import com.sysu.edu.todo.TodoActivity
import com.sysu.edu.todo.TodoManager
import com.sysu.edu.view.AdapterListener
import com.sysu.edu.view.RecyclerAdapter
import io.noties.markwon.AbstractMarkwonPlugin
import io.noties.markwon.Markwon
import io.noties.markwon.MarkwonSpansFactory
import io.noties.markwon.MarkwonVisitor
import io.noties.markwon.core.CoreProps
import org.commonmark.node.Heading
import org.commonmark.node.Node
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.LinkedList
import java.util.Locale
import java.util.concurrent.TimeUnit

class DashboardFragment : BaseFragment() {
	private val todayCourse = mutableListOf<JSONObject>()
	private val tomorrowCourse = mutableListOf<JSONObject>()
	private val week18Exams = LinkedList<JSONObject>()
	private val week19Exams = LinkedList<JSONObject>()
	private val todoDate = MutableLiveData<String>()
	lateinit var model: JwxtModel
	var db: HomeCollectionHelper? = null
	lateinit var binding: FragmentDashboardBinding
	var isRefreshRequired: Boolean = true
	var viewModel: HomeViewModel? = null
	var orderDialog: BottomSheetDialog? = null
	val calendar: CalendarManager = CalendarManager()
	private var collectionAdapter: ServiceFragment.CollectionAdapter? = null
	var actionDialog: BottomSheetDialog? = null
	var actionBinding: DialogServiceActionBinding? = null
	private var todoManager: TodoManager? = null
	var termString: String? = null
	var week: String? = null
	private var examSubject = ""
	private val markwon by lazy {
		Markwon.builder(requireContext()).usePlugin(object : AbstractMarkwonPlugin() {
			override fun configureSpansFactory(builder: MarkwonSpansFactory.Builder) {
				builder.appendFactory(Heading::class.java) { _, configuration ->
					if (CoreProps.HEADING_LEVEL.require(configuration) == 3) ForegroundColorSpan(model.contextUtil.getColorFromAttr(androidx.appcompat.R.attr.colorPrimary)) else null
				}
			}
			
			override fun configureVisitor(builder: MarkwonVisitor.Builder) {
				builder.blockHandler(object : MarkwonVisitor.BlockHandler {
					override fun blockStart(visitor: MarkwonVisitor, node: Node) {}
					override fun blockEnd(visitor: MarkwonVisitor, node: Node) {
						if (visitor.hasNext(node)) visitor.ensureNewLine()
					}
				})
			}
		}).build()
	}
	
	override fun onCreateView(inflater: LayoutInflater,
	                          container: ViewGroup?,
	                          savedInstanceState: Bundle?): NestedScrollView {
		if (isRefreshRequired) {
			super.onCreateView(inflater, container, savedInstanceState)
			binding = FragmentDashboardBinding.inflate(inflater, container, false)
			initViews(inflater)
			initObservers()
		}
		return binding.root
	}
	
	private fun initViews(inflater: LayoutInflater) {
		val date = LocalDate.now()
			.format(DateTimeFormatter.ofPattern("M月dd日", Locale.getDefault()))
		val examAdapter = ExamAdapter().apply {
			setParams(config)
			listener = object : AdapterListener {
				override fun onBind(adapter: RecyclerView.Adapter<RecyclerView.ViewHolder?>,
				                    holder: RecyclerView.ViewHolder,
				                    position: Int) {
					holder.itemView.setOnClickListener {
						examSubject = get(position).getString("examSubjectName")
						getSelectedCourses(examSubject)
					}
				}
				
				override fun onCreate(adapter: RecyclerView.Adapter<RecyclerView.ViewHolder?>,
				                      binding: ViewBinding?) {
				}
			}
		}
		val courseAdapter = CourseAdapter().apply {
			setParams(config)
			setClick { json, view ->
				startActivity(Intent(context, CourseDetailActivity::class.java).putExtra("code", json!!.getString("courseNum"))
								  .putExtra("class", json.getString("classesNum")), ActivityOptionsCompat.makeSceneTransitionAnimation(requireActivity(), view
					?: requireView(), "miniapp").toBundle())
			}
		}
		todoManager = TodoManager(requireActivity(), ConcatAdapter().also { binding.todoList.adapter = it })
		with(binding) {
			setupClickListeners()
			courseList.addItemDecoration(DividerItemDecoration(requireContext(), 0))
			courseList.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
			courseList.adapter = courseAdapter
			examList.addItemDecoration(DividerItemDecoration(requireContext(), 0))
			examList.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
			examList.adapter = examAdapter
			toggle.addOnButtonCheckedListener { _, checkedId, isChecked ->
				if (checkedId == R.id.today) {
					courseAdapter.set(if (isChecked) todayCourse else tomorrowCourse)
					noClass.visibility = if (courseAdapter.itemCount == 0) View.VISIBLE else View.GONE
				}
			}
			toggle2.addOnButtonCheckedListener { _, checkedId, isChecked ->
				if (checkedId == R.id.week_18) {
					examAdapter.set((if (isChecked) week18Exams else week19Exams).toMutableList())
					noExam.visibility = if (examAdapter.itemCount == 0) View.VISIBLE else View.GONE
				}
			}
			toggle3.addOnButtonCheckedListener { _, checkedId, _ -> if (checkedId == R.id.filter_todo) refresh() }
			dateView.text = getString(R.string.dashboard_day, date, resources.getStringArray(R.array.weeks)[LocalDate.now().dayOfWeek.value - 1])
			todoList.layoutManager = LinearLayoutManager(requireActivity(), LinearLayoutManager.VERTICAL, false)
			add.setOnClickListener { todoManager?.showTodoAddDialog() }
			todoDateButton.setOnClickListener { showTodoPopup() }
		}
		db = HomeCollectionHelper(requireContext())
		initOrder(inflater)
		initAction(inflater)
		shortcutCollection
	}
	
	private fun FragmentDashboardBinding.setupClickListeners() {
		val scheduleClick = gotoActivity(CourseScheduleActivity::class.java)
		scan.setOnClickListener { openWechatScan() }
		qrcode.setOnClickListener { openQrCode() }
		agenda.setOnClickListener(scheduleClick)
		courseTitle.setOnClickListener(scheduleClick)
		examTitle.setOnClickListener(gotoActivity(ExamActivity::class.java))
		todoTitle.setOnClickListener(gotoActivity(TodoActivity::class.java))
		nextClass.setOnClickListener(scheduleClick)
		nextClassCard.setOnClickListener(scheduleClick)
		timeCard.setOnClickListener(gotoActivity(AgendaActivity::class.java))
		todoView.setOnClickListener(gotoActivity(TodoActivity::class.java))
	}
	
	private fun initObservers() {
		model = JwxtModel(requireContext()).apply {
			message.observe(requireActivity()) { (id, data) ->
				if (data.getInteger("code") == 200) {
					when (id) {
						1 -> handleCourses(data)
						2 -> handleExams(data)
						3 -> handleTerm(data)
						4 -> handleWeek(data)
						5 -> handleFinalExam(data)
						6 -> handleSelectedCourses(data)
					}
				}
			}
		}
		viewModel = ViewModelProvider(requireActivity())[HomeViewModel::class.java].apply {
			updateDashboardShortcut.observe(requireActivity()) { shortcutCollection }
		}
		todoDate.observe(viewLifecycleOwner) { refresh() }
		todoManager?.setOnRefreshListener { refresh() }
		ViewModelProvider(requireActivity())[PreferenceViewModel::class.java].isAgreeLiveData.observe(viewLifecycleOwner) {
			if (!it) term
		}
	}
	
	private fun handleCourses(data: JSONObject) {
		val courseArray = data.getJSONArray("data") ?: return
		todayCourse.clear()
		tomorrowCourse.clear()
		val (beforeArray, afterArray) = courseArray.map { it as JSONObject }.filter { item ->
			val status = getTimePosition("${item.getString("teachingDate")} ${item.getString("startTime")}", "${item.getString("teachingDate")} ${item.getString("endTime")}")
			item["status"] = status
			item["time"] = "${item.getString("startTime")}~${item.getString("endTime")}"
			item["course"] = "第${item.getString("startClassTimes")}~${item.getString("endClassTimes")}节课"
			val isToday = "TD" == item.getString("useflag")
			if (isToday) todayCourse.add(item) else tomorrowCourse.add(item)
			isToday
		}.partition { it.getString("status") == "before" }
		
		binding.progress.max = todayCourse.size
		binding.progress.progress = beforeArray.size
		binding.courseList.scrollToPosition(beforeArray.size)
		
		updateNextClassMarkdown(beforeArray.size, afterArray.isEmpty())
		scheduleNotification(beforeArray.size, afterArray.isEmpty())
		
		binding.toggle.clearChecked()
		binding.toggle.check(R.id.today)
	}
	
	private fun updateNextClassMarkdown(beforeSize: Int, isAfterEmpty: Boolean) {
		val markdown = if (isAfterEmpty) {
			val next = tomorrowCourse.getOrNull(0)
			"### ${getString(R.string.noClass)}\n\n${getString(R.string.next_class)}：**${next?.getString("courseName") ?: getString(R.string.none)}**\n\n${getString(R.string.location)}：**${next?.getString("teachingPlace") ?: getString(R.string.none)}**\n\n${getString(R.string.time)}：**${next?.getString("time") ?: getString(R.string.none)}**"
		}
		else {
			val current = todayCourse.getOrNull(beforeSize)
			"### ${current?.getString("courseName") ?: getString(R.string.none)}\n\n${getString(R.string.location)}：**${current?.getString("teachingPlace") ?: getString(R.string.none)}**\n\n${getString(R.string.time)}：**${current?.getString("time") ?: getString(R.string.none)}**\n\n${getString(R.string.date)}：**${current?.getString("teachingDate") ?: getString(R.string.none)}**"
		}
		markwon.setMarkdown(binding.nextClass, markdown)
	}
	
	private fun scheduleNotification(beforeSize: Int, isAfterEmpty: Boolean) {
		(if (isAfterEmpty) tomorrowCourse.getOrNull(0) else todayCourse.getOrNull(beforeSize))?.run {
			val startTimeStr = "${getString("teachingDate")} ${getString("startTime")}"
			val delta = LocalDateTime.parse(startTimeStr, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
				.atZone(ZoneId.systemDefault())
				.toInstant()
				.toEpochMilli() - System.currentTimeMillis()
			if (delta > 0) {
				val delay = if (delta < 15 * 60 * 1000) 0L else delta - 15 * 60 * 1000
				val workRequest = OneTimeWorkRequest.Builder(ClassNotificationWorker::class.java)
					.setInputData(workDataOf("courseName" to getString("courseName"), "teachingPlace" to getString("teachingPlace"), "time" to getString("time")))
					.setInitialDelay(delay, TimeUnit.MILLISECONDS)
					.build()
				WorkManager.getInstance(requireContext().applicationContext)
					.enqueueUniqueWork("next_class_notification_update", ExistingWorkPolicy.KEEP, workRequest)
			}
		}
	}
	
	private fun handleExams(data: JSONObject) {
		data.getJSONArray("data")?.forEachIndexed { i, v ->
			val exams = if (i == 0) week18Exams else week19Exams
			val timetable = (v as JSONObject).getJSONObject("timetable")
			val sortedKeys = timetable.keys.mapNotNull { it.toIntOrNull() }.sorted()
			sortedKeys.forEach { key ->
				timetable.getJSONArray("$key")?.forEach { c ->
					val exam = c as JSONObject
					exam["status"] = getDatePosition(exam.getString("examDate"))
					if (key == sortedKeys.first()) exams.addFirst(exam) else exams.addLast(exam)
				}
			}
		}
		val weekExams = if ("19" == week) week19Exams else week18Exams
		val index = weekExams.indexOfFirst { it.getString("status") == "in" }.let {
			if (it < 0) weekExams.indexOfFirst { e -> e.getString("status") == "after" } else it
		}
		binding.examList.scrollToPosition(if (index < 0 || index >= weekExams.size) 0 else index)
		binding.toggle2.check(if ("19" == week) R.id.week_19 else R.id.week_18)
		binding.noExam.visibility = if (weekExams.isEmpty()) View.VISIBLE else View.GONE
		(binding.examList.adapter as? ExamAdapter)?.set(weekExams.toMutableList())
		isRefreshRequired = false
	}
	
	private fun handleTerm(data: JSONObject) {
		data.getJSONObject("data").getString("acadYearSemester").let {
			termString = it
			val date = LocalDate.now()
				.format(DateTimeFormatter.ofPattern("M月dd日", Locale.getDefault()))
			binding.dateView.text = getString(R.string.dashboard_time, it, date, resources.getStringArray(R.array.weeks)[LocalDate.now().dayOfWeek.value - 1])
			getWeek(it)
			getTodayCourses(it)
		}
	}
	
	private fun handleWeek(data: JSONObject) {
		data.getJSONArray("data").getJSONObject(0).getString("weekTimes").let {
			binding.dateView.text = getString(R.string.dashboard_week, it, binding.dateView.text)
			week = it
			termString?.let { t -> getFinalExam(t) }
		}
	}
	
	private fun handleFinalExam(data: JSONObject) {
		data.getJSONArray("data")
			.firstOrNull { (it as JSONObject).getString("examWeekName") == "18-19周期末考" }
			?.let {
				termString?.let { term -> getExams(term, (it as JSONObject).getString("examWeekId")) }
			}
	}
	
	private fun handleSelectedCourses(data: JSONObject) {
		data.getJSONObject("data").getJSONArray("rows").takeIf { it.isNotEmpty() }?.firstOrNull {
			(it as JSONObject).getString("courseName") == examSubject
		}?.also {
			startActivity(Intent(requireContext(), CourseDetailActivity::class.java).putExtra("id", (it as JSONObject).getString("teachingClassId"))
							  .putExtra("code", it.getString("courseNum"))
							  .putExtra("class", it.getString("teachingClassNum")), ActivityOptionsCompat.makeSceneTransitionAnimation(requireActivity(), binding.examList, "miniapp")
							  .toBundle())
		}
	}
	
	private fun openWechatScan() {
		Intent().setComponent(ComponentName("com.tencent.mm", "com.tencent.mm.ui.LauncherUI"))
			.putExtra("LauncherUI.From.Scaner.Shortcut", true)
			.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
			.setAction("android.intent.initActionDialog.VIEW")
			.takeIf { it.resolveActivity(requireContext().packageManager) != null }
			?.let { startActivity(it) }
	}
	
	private fun openQrCode() {
		PreferenceManager.getDefaultSharedPreferences(requireContext())
			.getString("qrcode", "")
			?.takeIf { it.isNotEmpty() }
			?.run {
				Intent(Intent.ACTION_VIEW, toUri()).takeIf { it.resolveActivity(requireContext().packageManager) != null }
					?.let { startActivity(it) }
					?: model.contextUtil.toast(R.string.fix_sysu_code_warning)
			} ?: model.contextUtil.toast(R.string.set_sysu_code_warning)
	}
	
	private fun showTodoPopup() {
		val pop = PopupMenu(requireActivity(), binding.todoDateButton, 0, 0, com.google.android.material.R.style.Widget_Material3_PopupMenu_Overflow)
		val datePicker = MaterialDatePicker.Builder.datePicker()
		pop.menu.apply {
			add(0, Menu.NONE, 0, R.string.all).setChecked(true)
				.setOnMenuItemClickListener { todoDate.value = ""; false }
			add(0, Menu.NONE, 0, R.string.today).setOnMenuItemClickListener { todoDate.value = calendar.toDateStringPLus(0); false }
			add(0, Menu.NONE, 0, R.string.tomorrow).setOnMenuItemClickListener { todoDate.value = calendar.toDateStringPLus(1); false }
			add(1, Menu.NONE, 0, R.string.select).setOnMenuItemClickListener {
				todoDate.value?.takeIf { it.isNotEmpty() }
					?.let { datePicker.setSelection(calendar.toMillis(it) + 86400000) }
				datePicker.build().apply {
					show(this@DashboardFragment.childFragmentManager, "date_picker")
					addOnPositiveButtonClickListener { l -> todoDate.value = l?.let { calendar.toDateString(it) } }
				}
				false
			}
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) setGroupDividerEnabled(true)
		}
		pop.show()
	}
	
	override fun onDestroyView() {
		super.onDestroyView()
		model.dispose()
	}
	
	private fun gotoActivity(cls: Class<*>?): View.OnClickListener = View.OnClickListener { v ->
		startActivity(Intent(context, cls), ActivityOptionsCompat.makeSceneTransitionAnimation(requireActivity(), v!!, "miniapp")
			.toBundle())
	}
	
	fun refresh() {
		binding.todoDateButton.text = todoDate.value?.takeIf { it.isNotEmpty() }?.let {
			todoManager?.refresh("due_date = ? AND status = ?", arrayOf(it, if (binding.filterTodo.isChecked) "0" else "1"))
			it
		} ?: run {
			todoManager?.refresh("status = ?", arrayOf(if (binding.filterTodo.isChecked) "0" else "1"))
			getString(R.string.all)
		}
	}
	
	val term: Unit
		get() = model.addAndNext("jwxt/base-info/acadyearterm/showNewAcadlist", 3)
	
	fun getWeek(term: String?): Unit =
		model.addAndNext("jwxt/timetable-search/classTableInfo/getDateWeekly?academicYear=$term", 4)
	
	fun getTodayCourses(term: String?): Unit =
		model.addAndNext("jwxt/timetable-search/classTableInfo/queryTodayStudentClassTable?academicYear=$term", 1)
	
	fun getExams(term: String, weekId: String?): Unit =
		model.addAndNext("jwxt/examination-manage/classroomResource/queryStuEaxmInfo?code=jwxsd_ksxxck", "{\"acadYear\":\"$term\",\"examWeekId\":\"$weekId\",\"examWeekName\":\"18-19周期末考\",\"examDate\":\"\"}", 2)
	
	fun getFinalExam(term: String): Unit =
		model.addAndNext("jwxt/schedule/agg/commonScheduleExamTime/queryExamWeekName?yearTerm=$term", 5)
	
	fun getSelectedCourses(courseName: String?): Unit =
		model.addAndNext("jwxt/choose-course-front-server/selectedCourse/list", String.format(Locale.getDefault(), "{\"pageNo\":%d,\"pageSize\":10,\"total\":true,\"param\":{\"courseName\":\"%s\",\"successStatus\":\"1\",\"failureStatus\":\"0\",\"retiredClass\":\"0\",\"waitingScreen\":\"0\"}}", 1, courseName), 6)
	
	fun getTimePosition(from: String?, to: String?): String {
		val now = LocalDateTime.now()
		val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
		val start = LocalDateTime.parse(from, formatter)
		val end = LocalDateTime.parse(to, formatter)
		return when {
			now.isBefore(start) -> "after"
			now.isAfter(end) -> "before"
			else -> "in"
		}
	}
	
	fun getDatePosition(date: String): String {
		val now = LocalDate.now()
		val d = LocalDate.parse(date, DateTimeFormatter.ofPattern("yyyy-MM-dd"))
		return when {
			now.isBefore(d) -> "after"
			now.isAfter(d) -> "before"
			else -> "in"
		}
	}
	
	val shortcutCollection: Unit
		get() {
			binding.shortcutGroup.childCount.takeIf { it > 4 }
				?.let { repeat(it - 4) { binding.shortcutGroup.removeViewAt(3) } }
			db?.writableDatabase?.query("dashboard_shortcut_collection", null, null, null, null, null, "position")
				?.use { cursor ->
					if (cursor.moveToFirst()) {
						collectionAdapter?.clear()
						do {
							val id = cursor.getInt(cursor.getColumnIndexOrThrow("shortcutId"))
							val shortcut = JSON.parseObject(cursor.getString(cursor.getColumnIndexOrThrow("shortcutJson")))
							val button = MaterialButton(requireContext(), null, com.google.android.material.R.attr.materialButtonTonalStyle).apply {
								text = shortcut.getString("name")
								setOnClickListener { v ->
									viewModel?.actionMap?.get(id)?.run {
										onClick(v)
									} ?: run {
										val activity = shortcut.getString("activity")
										val url = shortcut.getString("url")
										when {
											!activity.isNullOrEmpty() -> {
												Intent(requireContext(), Class.forName(requireContext().packageName + activity)).takeIf { it.resolveActivity(requireContext().packageManager) != null }
													?.let {
														startActivity(it, ActivityOptionsCompat.makeSceneTransitionAnimation(requireActivity(), v!!, "miniapp")
															.toBundle())
													}
													?: model.contextUtil.toast(R.string.activity_not_found)
											}
											!url.isNullOrEmpty() -> startActivity(Intent(requireContext(), BrowserActivity::class.java).setData(url.toUri()), ActivityOptionsCompat.makeSceneTransitionAnimation(requireActivity(), v!!, "miniapp")
												.toBundle())
											else -> model.contextUtil.toast(R.string.undeveloped)
										}
									}
								}
								setOnLongClickListener { showActionDialog(shortcut) }
							}
							binding.shortcutGroup.addView(button)
							collectionAdapter?.add(shortcut)
						} while (cursor.moveToNext())
					}
				}
		}
	
	fun initOrder(inflater: LayoutInflater) {
		orderDialog = BottomSheetDialog(requireContext())
		val orderBinding = DialogServiceOrderBinding.inflate(inflater).apply {
			recyclerView.layoutManager = LinearLayoutManager(requireContext())
			collectionAdapter = ServiceFragment.CollectionAdapter()
				.also { recyclerView.adapter = it }
			confirm.setOnClickListener { updateShortcut(); shortcutCollection; orderDialog?.dismiss() }
		}
		orderDialog?.setContentView(orderBinding.root)
		ItemTouchHelper(object :
							ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0) {
			override fun onMove(r: RecyclerView,
			                    s: RecyclerView.ViewHolder,
			                    t: RecyclerView.ViewHolder): Boolean {
				collectionAdapter?.swap(s.bindingAdapterPosition, t.bindingAdapterPosition)
				return true
			}
			
			override fun onSwiped(vh: RecyclerView.ViewHolder, d: Int) {}
		}).attachToRecyclerView(orderBinding.recyclerView)
	}
	
	fun updateShortcut() {
		repeat(collectionAdapter?.itemCount ?: 0) {
			db?.updateDashboardShortcutPosition(collectionAdapter!!.get(it).getInteger("id"), it)
		}
	}
	
	fun initAction(inflater: LayoutInflater) {
		actionDialog = BottomSheetDialog(requireContext())
		actionBinding = DialogServiceActionBinding.inflate(inflater)
			.apply { order.setOnClickListener { orderDialog?.show() } }
		actionDialog?.setContentView(actionBinding!!.root)
	}
	
	fun showActionDialog(item: JSONObject): Boolean {
		val itemId = item.getIntValue("id")
		val isServiceCollected = MutableLiveData(db?.isServiceCollected(itemId))
		val isShortcutCollected = MutableLiveData(db?.isDashboardShortcutCollected(itemId))
		with(actionBinding!!) {
			collect.text = getString(if (isServiceCollected.value == true) R.string.cancel_collect else R.string.collect)
			addToDashboard.text = getString(if (isShortcutCollected.value == true) R.string.cancel_add_shortcut else R.string.add_to_dashboard)
			addToLauncher.setOnClickListener {
				if (ShortcutManagerCompat.isRequestPinShortcutSupported(requireContext())) {
					val intent = when {
						item.containsKey("activity") -> {
							try {
								Intent(requireContext(), Class.forName(requireContext().packageName + item.getString("activity")))
							} catch (_: Exception) {
								Intent(requireContext(), MainActivity::class.java)
							}
						}
						item.containsKey("url") -> Intent(requireContext(), BrowserActivity::class.java).setData(CommonUtil.trim(item.getString("url"))
																													 .toUri())
						else -> Intent(requireContext(), MainActivity::class.java)
					}
					val info = ShortcutInfoCompat.Builder(requireContext(), "$itemId")
						.setShortLabel(item.getString("name"))
						.setLongLabel(item.getString("name"))
						.setIcon(IconCompat.createWithResource(requireContext(), R.mipmap.icon))
						.setIntent(intent.setAction(Intent.ACTION_VIEW))
						.build()
					ShortcutManagerCompat.requestPinShortcut(requireContext(), info, PendingIntent.getBroadcast(requireContext(), 0, ShortcutManagerCompat.createShortcutResultIntent(requireContext(), info), PendingIntent.FLAG_IMMUTABLE).intentSender)
				}
				else model.contextUtil.toast(R.string.fail_to_add_shortcut)
			}
			collect.setOnClickListener {
				val collected = isServiceCollected.value == true
				if (collected) db?.deleteService(itemId)
				else db?.addService(itemId, item.toJSONString(), collectionAdapter?.itemCount ?: 0)
				model.contextUtil.toast(if (collected) R.string.cancel_collect_success else R.string.collect_success)
				shortcutCollection
				isServiceCollected.value = !collected
				collect.text = getString(if (isServiceCollected.value == true) R.string.cancel_collect else R.string.collect)
			}
			addToDashboard.setOnClickListener {
				val collected = isShortcutCollected.value == true
				if (collected) db?.deleteDashboardShortcut(itemId)
				else db?.addDashboardShortcut(itemId, item.toJSONString(), collectionAdapter?.itemCount
					?: 0)
				model.contextUtil.toast(if (collected) R.string.cancel_add_shortcut_success else R.string.add_shortcut_success)
				viewModel?.updateDashboardShortcut?.value = true
				isShortcutCollected.value = !collected
				addToDashboard.text = getString(if (isShortcutCollected.value == true) R.string.cancel_add_shortcut else R.string.add_to_dashboard)
			}
			feedback.setOnClickListener { startActivity(Intent(Intent.ACTION_VIEW, "https://github.com/SYSU-Tang/Sysuer/issues/new?title=反馈：服务->${item.getString("name")}&labels=bug,crash-report".toUri()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
			markwon.setMarkdown(description, "### ${item.getString("name")}\n${item.getString("description")}")
		}
		actionDialog!!.show()
		return true
	}
}

internal class CourseAdapter : RecyclerAdapter<JSONObject>() {
	private var onClick: ((JSONObject?, View?) -> Unit)? = null
	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = object :
		RecyclerView.ViewHolder(ItemHomeCourseBinding.inflate(LayoutInflater.from(parent.context), parent, false).root) {}
	
	fun setClick(onClick: (JSONObject?, View?) -> Unit) {
		this.onClick = onClick
	}
	
	override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
		val binding = ItemHomeCourseBinding.bind(holder.itemView)
		val item = get(position)
		holder.itemView.setOnClickListener { v -> onClick?.invoke(item, v) }
		mapOf(binding.courseTitle to "courseName", binding.location to "teachingPlace", binding.time to "time", binding.teacher to "teacherName", binding.course to "course").forEach { (v, s) ->
			v.text = item.getString(s)
			v.setOnLongClickListener { config?.copy(s, item.getString(s)); config?.toast(R.string.copy_successfully); true }
		}
		val status = item.getString("status")
		val isBefore = status == "before"
		val tint = when (status) {
			"in" -> config?.contextUtil?.getColorFromAttr(com.google.android.material.R.attr.colorSurfaceDim)
			"before" -> 0
			else -> config?.contextUtil?.getColorFromAttr(com.google.android.material.R.attr.colorSurface)
		} ?: 0
		holder.itemView.background.setTint(tint)
		binding.courseTitle.setTextAppearance(if (isBefore) com.google.android.material.R.style.TextAppearance_Material3_TitleMedium else com.google.android.material.R.style.TextAppearance_Material3_TitleMedium_Emphasized)
		binding.item.alpha = if (isBefore) 0.64f else 1.0f
		super.onBindViewHolder(holder, position)
	}
}

internal class ExamAdapter : RecyclerAdapter<JSONObject>() {
	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = object :
		RecyclerView.ViewHolder(ItemExamBinding.inflate(LayoutInflater.from(parent.context), parent, false).root) {}
	
	override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
		val binding = ItemExamBinding.bind(holder.itemView)
		val context = holder.itemView.context
		val examData = get(position)
		val text = arrayOf(examData.getString("examSubjectName"), examData.getString("classroomNumber"), "${examData.getString("examDate")} ${context.resources.getStringArray(R.array.weeks)[examData.getInteger("week") - 1]}", "${examData.getString("duration")}${context.getString(R.string.minute)}", examData.getString("durationTime"), String.format(context.getString(R.string.section_range), examData.getIntValue("startClassTimes"), examData.getIntValue("endClassTimes")), "${context.getString(R.string.exam_mode)}：${examData.getString("examMode")}", "${context.getString(R.string.exam_stage)}：${examData.getString("examStage")}")
		val views = arrayOf<TextView>(binding.examName, binding.examLocation, binding.examDate, binding.examDuration, binding.examTime, binding.examClassTime, binding.examMode, binding.examStage)
		views.forEachIndexed { i, v ->
			v.text = text[i]
			v.setOnClickListener { config?.copy("exam", text[i]); config?.toast(R.string.copy_successfully) }
		}
		val status = examData.getString("status")
		val isBefore = status == "before"
		val tint = when (status) {
			"in" -> config?.contextUtil?.getColorFromAttr(com.google.android.material.R.attr.colorSurfaceDim)
			"before" -> 0
			else -> config?.contextUtil?.getColorFromAttr(com.google.android.material.R.attr.colorSurface)
		} ?: 0
		binding.root.background.setTint(tint)
		binding.examName.setTextAppearance(if (isBefore) com.google.android.material.R.style.TextAppearance_Material3_TitleMedium else com.google.android.material.R.style.TextAppearance_Material3_TitleMedium_Emphasized)
		binding.root.alpha = if (isBefore) 0.64f else 1.0f
		super.onBindViewHolder(holder, position)
	}
}