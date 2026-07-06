package com.sysu.edu.home

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
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
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager.Companion.getInstance
import com.alibaba.fastjson2.JSON
import com.alibaba.fastjson2.JSONArray
import com.alibaba.fastjson2.JSONObject
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.datepicker.MaterialPickerOnPositiveButtonClickListener
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
import io.noties.markwon.MarkwonConfiguration
import io.noties.markwon.MarkwonSpansFactory
import io.noties.markwon.MarkwonVisitor
import io.noties.markwon.MarkwonVisitor.BlockHandler
import io.noties.markwon.RenderProps
import io.noties.markwon.core.CoreProps
import org.commonmark.node.Heading
import org.commonmark.node.Node
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.TreeMap
import java.util.concurrent.TimeUnit
import java.util.function.BiConsumer
import java.util.stream.IntStream

class DashboardFragment : BaseFragment() {
	val todayCourse: MutableList<JSONObject> = mutableListOf()
	val tomorrowCourse: MutableList<JSONObject> = mutableListOf()
	val week18Exams: LinkedHashSet<JSONObject?> = linkedSetOf()
	val week19Exams: LinkedHashSet<JSONObject?> = linkedSetOf()
	val todoDate: MutableLiveData<String?> = MutableLiveData("")
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
	override fun onCreateView(inflater: LayoutInflater,
	                          container: ViewGroup?,
	                          savedInstanceState: Bundle?): NestedScrollView {
		if (isRefreshRequired) {
			super.onCreateView(inflater, container, savedInstanceState)
			val date = LocalDate.now()
				.format(DateTimeFormatter.ofPattern("M月dd日", Locale.getDefault()))
			var examSubject = ""
			val examAdapter = ExamAdapter().apply {
				setParams(config)
				setListener(object : AdapterListener {
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
				})
			}
			model = JwxtModel(requireContext()).apply {
				message.observe(requireActivity()) { message: CommonUtil.Tuple2<Int, JSONObject> ->
					println("${message.first} ${message.second}")
					message.second.run {
						if (getInteger("code") == 200) {
							when (message.first) {
								1 -> {
									val beforeArray = mutableListOf<JSONObject?>()
									val afterArray = mutableListOf<JSONObject?>()
									getJSONArray("data").forEach { item: Any? ->
										val status = getTimePosition("${(item as JSONObject).getString("teachingDate")} ${item.getString("startTime")}", "${item.getString("teachingDate")} ${item.getString("endTime")}")
										item["status"] = status
										item["time"] = "${item.getString("startTime")}~${item.getString("endTime")}"
										item["course"] = "第${
											item.getString("startClassTimes")
										}~${
											item.getString("endClassTimes")
										}节课"
										val isToday = "TD" == item.getString("useflag")
										if (isToday) (if (status == "before") beforeArray else afterArray).add(item)
										(if (isToday) todayCourse else tomorrowCourse).add(item)
									}
									binding.progress.setMax(todayCourse.size)
									binding.progress.progress = beforeArray.size
									binding.courseList.scrollToPosition(beforeArray.size)
									Markwon.builder(requireContext())
										.usePlugin(object : AbstractMarkwonPlugin() {
											override fun configureSpansFactory(builder: MarkwonSpansFactory.Builder) {
												super.configureSpansFactory(builder)
												builder.appendFactory(Heading::class.java) { _: MarkwonConfiguration?, configuration: RenderProps? ->
													if (CoreProps.HEADING_LEVEL.require(configuration!!) == 3) ForegroundColorSpan(model.contextUtil.getColorFromAttr(androidx.appcompat.R.attr.colorPrimary))
												}
											}
											
											override fun configureVisitor(builder1: MarkwonVisitor.Builder) {
												super.configureVisitor(builder1)
												builder1.blockHandler(object : BlockHandler {
													override fun blockStart(visitor: MarkwonVisitor,
													                        node: Node) {
													}
													
													override fun blockEnd(visitor: MarkwonVisitor,
													                      node: Node) {
														if (visitor.hasNext(node)) visitor.ensureNewLine()
													}
												})
											}
										})
										.build()
										.setMarkdown(binding.nextClass, if (afterArray.isEmpty()) "### ${
											getString(R.string.noClass)
										}\n\n${
											getString(R.string.next_class)
										}：**${
											if (tomorrowCourse.isEmpty()) getString(R.string.none) else tomorrowCourse[0].getString("courseName")
										}**\n\n${
											getString(R.string.location)
										}：**${
											if (tomorrowCourse.isEmpty()) getString(R.string.none) else tomorrowCourse[0].getString("teachingPlace")
										}**\n\n${
											getString(R.string.time)
										}：**${
											if (tomorrowCourse.isEmpty()) getString(R.string.none) else tomorrowCourse[0].getString("time")
										}**"
										else "### ${
											if (todayCourse.isEmpty()) getString(R.string.none) else todayCourse[beforeArray.size].getString("courseName")
										}\n\n${
											getString(R.string.location)
										}：**${
											if (todayCourse.isEmpty()) getString(R.string.none) else todayCourse[beforeArray.size].getString("teachingPlace")
										}**\n\n${
											getString(R.string.time)
										}：**${
											if (todayCourse.isEmpty()) getString(R.string.none) else todayCourse[beforeArray.size].getString("time")
										}**\n\n${
											getString(R.string.date)
										}：**${
											if (todayCourse.isEmpty()) getString(R.string.none) else todayCourse[beforeArray.size].getString("teachingDate")
										}**")
									binding.toggle.clearChecked()
									binding.toggle.check(R.id.today)
									(if (afterArray.isEmpty()) if (tomorrowCourse.isEmpty()) null else tomorrowCourse[0] else todayCourse[beforeArray.size])?.run {
										val delta = LocalDateTime.parse("${
											getString("teachingDate")
										} ${getString("startTime")}", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
											.atZone(ZoneId.systemDefault())
											.toInstant()
											.toEpochMilli() - System.currentTimeMillis()
										if (delta > 0) getInstance(requireContext().applicationContext).enqueueUniqueWork("next_class_notification_update", ExistingWorkPolicy.KEEP, OneTimeWorkRequest.Builder(ClassNotificationWorker::class.java)
											.setInputData(Data.Builder()
															  .putString("courseName", getString("courseName"))
															  .putString("teachingPlace", getString("teachingPlace"))
															  .putString("time", getString("time"))
															  .build())
											.setInitialDelay(if (delta < 1000 * 60 * 15) 0 else delta - 1000 * 60 * 15, TimeUnit.MILLISECONDS)
											.build())
									}
								}
								2 -> {
									getJSONArray("data").forEachIndexed { i, v ->
										val exams = if (i == 0) week18Exams else week19Exams
										val sortedTimetable = TreeMap<Int?, JSONArray?>()
										(v as JSONObject).getJSONObject("timetable")
											.forEach { (s1: String?, t: Any?) ->
												if (t != null) sortedTimetable[s1!!.toInt()] = t as JSONArray
											}
										sortedTimetable.forEach { (key: Int?, value: JSONArray?) ->
											value?.forEach { c: Any? ->
												(c as JSONObject)["status"] = getDatePosition(c.getString("examDate"))
												(if (key == sortedTimetable.firstKey()) exams::addFirst else exams::addLast)(c)
											}
										}
									}
									val weekExams = if ("19" == week) week19Exams else week18Exams
									var index = weekExams.indexOfFirst { (it as JSONObject).getString("status") == "in" }
									index = if (index < 0) weekExams.indexOfFirst { (it as JSONObject).getString("status") == "after" } else index
									binding.examList.scrollToPosition(if (index < 0 || index >= weekExams.size) 0 else index)
									binding.toggle2.check(if ("19" == week) R.id.week_19 else R.id.week_18)
									binding.noExam.visibility = if (weekExams.isEmpty()) View.VISIBLE else View.GONE
									examAdapter.set(weekExams.toMutableList())
									isRefreshRequired = false
								}
								3 -> {
									getJSONObject("data").getString("acadYearSemester").let {
										termString = it
										binding.dateView.text = getString(R.string.dashboard_time, it, date, resources.getStringArray(R.array.weeks)[LocalDate.now()
											.getDayOfWeek().value - 1])
										getWeek(it)
										getTodayCourses(it)
									}
								}
								4 -> {
									getJSONArray("data").getJSONObject(0)
										.getString("weekTimes")
										.let {
											binding.dateView.text = getString(R.string.dashboard_week, it, binding.dateView.getText())
											week = it
											termString?.let { it1 -> getFinalExam(it1) }
										}
								}
								5 -> getJSONArray("data").first {
									(it as JSONObject).getString("examWeekName") == "18-19周期末考"
								}?.let {
									termString?.let { term -> getExams(term, (it as JSONObject).getString("examWeekId")) }
								}
								6 -> getJSONObject("data").getJSONArray("rows")
									.takeIf { it.isNotEmpty() }
									?.first {
										(it as JSONObject).getString("courseName") == examSubject
									}
									?.also {
										startActivity(Intent(requireContext(), CourseDetailActivity::class.java).putExtra("id", (it as JSONObject).getString("teachingClassId"))
														  .putExtra("code", it.getString("courseNum"))
														  .putExtra("class", it.getString("teachingClassNum")), ActivityOptionsCompat.makeSceneTransitionAnimation(requireActivity(), binding.examList, "miniapp")
														  .toBundle())
									}
							}
						}
					}
				}
			}
			db = HomeCollectionHelper(requireContext())
			viewModel = ViewModelProvider(requireActivity())[HomeViewModel::class.java].apply {
				updateDashboardShortcut.observe(requireActivity()) { _: Boolean? -> this@DashboardFragment.shortcutCollection }
			}
			val courseAdapter = CourseAdapter().apply {
				setParams(config)
				setClick { jsonObject: JSONObject?, view: View? ->
					startActivity(Intent(context, CourseDetailActivity::class.java).putExtra("code", jsonObject!!.getString("courseNum"))
									  .putExtra("class", jsonObject.getString("classesNum")), ActivityOptionsCompat.makeSceneTransitionAnimation(requireActivity(), view
						?: requireView(), "miniapp").toBundle())
				}
			}
			val todoAdapter = ConcatAdapter()
			todoManager = TodoManager(requireActivity(), todoAdapter)
			binding = FragmentDashboardBinding.inflate(inflater, container, false).apply {
				scan.setOnClickListener {
					Intent().setComponent(ComponentName("com.tencent.mm", "com.tencent.mm.ui.LauncherUI"))
						.putExtra("LauncherUI.From.Scaner.Shortcut", true)
						.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
						.setAction("android.intent.initActionDialog.VIEW")
						.takeIf { it.resolveActivity(requireContext().packageManager) != null }
						?.let { startActivity(it) }
				}
				qrcode.setOnClickListener {
					PreferenceManager.getDefaultSharedPreferences(requireContext())
						.getString("qrcode", "")
						?.takeIf { it.isNotEmpty() }
						?.run {
							Intent(Intent.ACTION_VIEW, toUri()).takeIf { i ->
								i.resolveActivity(requireContext().packageManager) != null
							}?.let { startActivity(it) }
								?: model.contextUtil.toast(R.string.fix_sysu_code_warning)
						} ?: model.contextUtil.toast(R.string.set_sysu_code_warning)
				}
				agenda.setOnClickListener(gotoActivity(CourseScheduleActivity::class.java))
				courseList.addItemDecoration(DividerItemDecoration(requireContext(), 0))
				courseList.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
				examList.addItemDecoration(DividerItemDecoration(requireContext(), 0))
				examList.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
				courseTitle.setOnClickListener(gotoActivity(CourseScheduleActivity::class.java))
				examTitle.setOnClickListener(gotoActivity(ExamActivity::class.java))
				todoTitle.setOnClickListener(gotoActivity(TodoActivity::class.java))
				nextClass.setOnClickListener(gotoActivity(CourseScheduleActivity::class.java))
				nextClassCard.setOnClickListener(gotoActivity(CourseScheduleActivity::class.java))
				timeCard.setOnClickListener(gotoActivity(AgendaActivity::class.java))
				courseList.adapter = courseAdapter
				examList.adapter = examAdapter
				toggle.addOnButtonCheckedListener { _: MaterialButtonToggleGroup?, checkedId: Int, isChecked: Boolean ->
					if (R.id.today == checkedId) {
						courseAdapter.set(if (isChecked) todayCourse else tomorrowCourse)
						noClass.visibility = if (courseAdapter.itemCount == 0) View.VISIBLE else View.GONE
					}
				}
				toggle2.addOnButtonCheckedListener { _: MaterialButtonToggleGroup?, checkedId: Int, isChecked: Boolean ->
					if (R.id.week_18 == checkedId) {
						examAdapter.set((if (isChecked) week18Exams else week19Exams).toMutableList())
						noExam.visibility = if (examAdapter.itemCount == 0) View.VISIBLE else View.GONE
					}
				}
				dateView.text = getString(R.string.dashboard_day, date, resources.getStringArray(R.array.weeks)[LocalDate.now()
					.getDayOfWeek().value - 1])
				todoList.layoutManager = LinearLayoutManager(requireActivity(), LinearLayoutManager.VERTICAL, false)
				todoList.adapter = todoAdapter
				add.setOnClickListener { todoManager!!.showTodoAddDialog() }
				todoView.setOnClickListener(gotoActivity(TodoActivity::class.java))
				val pop = PopupMenu(requireActivity(), todoDateButton, 0, 0, com.google.android.material.R.style.Widget_Material3_PopupMenu_Overflow)
				pop.menu.apply {
					add(0, Menu.NONE, 0, R.string.all).setChecked(true).setOnMenuItemClickListener {
						todoDate.value = ""
						false
					}
					add(0, Menu.NONE, 0, R.string.today).setOnMenuItemClickListener { _: MenuItem? ->
						todoDate.value = calendar.toDateStringPLus(0)
						false
					}
					add(0, Menu.NONE, 0, R.string.tomorrow).setOnMenuItemClickListener { _: MenuItem? ->
						todoDate.value = calendar.toDateStringPLus(1)
						false
					}
					add(1, Menu.NONE, 0, R.string.select).setOnMenuItemClickListener { _: MenuItem? ->
						val builder = MaterialDatePicker.Builder.datePicker()
						builder.setSelection(todoDate.value?.takeIf { it.isNotEmpty() }?.let {
							calendar.toMillis(it) + 86400000
						} ?: System.currentTimeMillis())
						val datePicker = builder.build()
						datePicker.show(requireActivity().supportFragmentManager, "date_picker")
						datePicker.addOnPositiveButtonClickListener(MaterialPickerOnPositiveButtonClickListener { l: Long? ->
							todoDate.value = l?.let { calendar.toDateString(it) }
						})
						false
					}
					if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) setGroupDividerEnabled(true)
				}
				todoDateButton.setOnClickListener { pop.show() }
				toggle3.addOnButtonCheckedListener { _: MaterialButtonToggleGroup?, checkedId: Int, _: Boolean ->
					if (checkedId == R.id.filter_todo) refresh()
				}
			}
			binding.toggle3.check(R.id.filter_todo)
			todoDate.observe(viewLifecycleOwner) { refresh() }
			todoManager?.setOnRefreshListener { refresh() }
			initOrder(inflater)
			initAction(inflater)
			shortcutCollection
			ViewModelProvider(requireActivity())[PreferenceViewModel::class.java].apply {
				isAgreeLiveData.observe(viewLifecycleOwner) {
					if (!it) term
				}
			}
		}
		return binding.root
	}
	
	override fun onDestroyView() {
		super.onDestroyView()
		model.dispose()
	}
	
	private fun gotoActivity(cls: Class<*>?): View.OnClickListener {
		return View.OnClickListener { v: View? ->
			startActivity(Intent(context, cls), ActivityOptionsCompat.makeSceneTransitionAnimation(requireActivity(), v!!, "miniapp")
				.toBundle())
		}
	}
	
	fun refresh() {
		binding.todoDateButton.text = todoDate.value?.takeIf { it.isNotEmpty() }?.let {
			todoManager!!.refresh("due_date = ? AND status = ?", arrayOf(it, if (binding.filterTodo.isChecked) "0" else "1"))
			it
		} ?: run {
			todoManager!!.refresh("status = ?", arrayOf(if (binding.filterTodo.isChecked) "0" else "1"))
			getString(R.string.all)
		}
	}
	
	val term: Unit
		get() {
			model.addAndNext("jwxt/base-info/acadyearterm/showNewAcadlist", 3)
		}
	
	fun getWeek(term: String?) {
		model.addAndNext("jwxt/timetable-search/classTableInfo/getDateWeekly?academicYear=$term", 4)
	}
	
	fun getTodayCourses(term: String?) {
		model.addAndNext("jwxt/timetable-search/classTableInfo/queryTodayStudentClassTable?academicYear=$term", 1)
	}
	
	fun getExams(term: String, weekId: String?) {
		model.addAndNext("jwxt/examination-manage/classroomResource/queryStuEaxmInfo?code=jwxsd_ksxxck", "{\"acadYear\":\"$term\",\"examWeekId\":\"$weekId\",\"examWeekName\":\"18-19周期末考\",\"examDate\":\"\"}", 2)
	}
	
	fun getFinalExam(term: String) {
		model.addAndNext("jwxt/schedule/agg/commonScheduleExamTime/queryExamWeekName?yearTerm=$term", 5)
	}
	
	fun getSelectedCourses(courseName: String?) {
		model.addAndNext("jwxt/choose-course-front-server/selectedCourse/list", String.format(Locale.getDefault(), "{\"pageNo\":%d,\"pageSize\":10,\"total\":true,\"param\":{\"courseName\":\"%s\",\"successStatus\":\"1\",\"failureStatus\":\"0\",\"retiredClass\":\"0\",\"waitingScreen\":\"0\"}}", 1, courseName), 6)
	}
	
	fun getTimePosition(from: String?, to: String?): String {
		val now = LocalDateTime.now()
		val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
		return if (now.isBefore(LocalDateTime.parse(from, formatter))) "after" else if (now.isAfter(LocalDateTime.parse(to, formatter))) "before" else "in"
	}
	
	fun getDatePosition(date: String): String {
		val now = LocalDate.now()
		val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
		return if (now.isBefore(LocalDate.parse(date, formatter))) "after" else if (now.isAfter(LocalDate.parse(date, formatter))) "before" else "in"
	}
	
	val shortcutCollection: Unit
		get() {
			binding.shortcutGroup.childCount.takeIf { it > 4 }?.let {
				(0 until it - 4).forEach { _ ->
					binding.shortcutGroup.removeViewAt(3)
				}
			}
			db!!.writableDatabase.query("dashboard_shortcut_collection", null, null, null, null, null, "position")
				.use { cursor ->
					if (cursor.moveToFirst()) {
						collectionAdapter!!.clear()
						do {
							val id = cursor.getInt(cursor.getColumnIndexOrThrow("shortcutId"))
							val shortcut = JSON.parseObject(cursor.getString(cursor.getColumnIndexOrThrow("shortcutJson")))
							val button = MaterialButton(requireContext(), null, com.google.android.material.R.attr.materialButtonTonalStyle)
							button.text = shortcut.getString("name")
							binding.shortcutGroup.addView(button)
							val url = shortcut.getString("url")
							val activity = shortcut.getString("activity")
							if (viewModel!!.actionMap.containsKey(id)) button.setOnClickListener(viewModel!!.actionMap[id])
							button.setOnClickListener(if (viewModel!!.actionMap.containsKey(id)) viewModel!!.actionMap[id]
													  else if (TextUtils.isEmpty(activity)) if (TextUtils.isEmpty(url)) View.OnClickListener {
								model.contextUtil.toast(R.string.undeveloped)
							}
							else View.OnClickListener { v: View? ->
								startActivity(Intent(requireContext(), BrowserActivity::class.java).setData(Uri.parse(url)), ActivityOptionsCompat.makeSceneTransitionAnimation(requireActivity(), v!!, "miniapp")
									.toBundle())
							}
							else View.OnClickListener { v: View? ->
								Intent(requireContext(), Class.forName(requireContext().packageName + activity)).takeIf {
									it.resolveActivity(requireContext().packageManager) != null
								}.let {
									it?.let { intent ->
										startActivity(intent, ActivityOptionsCompat.makeSceneTransitionAnimation(requireActivity(), v!!, "miniapp")
											.toBundle())
									} ?: model.contextUtil.toast(R.string.activity_not_found)
								}
							})
							button.setOnLongClickListener { showActionDialog(shortcut) }
							collectionAdapter!!.add(shortcut)
						} while (cursor.moveToNext())
					}
				}
		}
	
	fun initOrder(inflater: LayoutInflater) {
		val context = requireContext()
		orderDialog = BottomSheetDialog(context)
		val orderBinding = DialogServiceOrderBinding.inflate(inflater)
		orderBinding.recyclerView.setLayoutManager(LinearLayoutManager(context))
		collectionAdapter = ServiceFragment.CollectionAdapter()
		orderBinding.recyclerView.setAdapter(collectionAdapter)
		orderBinding.confirm.setOnClickListener {
			updateShortcut()
			this.shortcutCollection
			orderDialog!!.dismiss()
		}
		orderDialog!!.setContentView(orderBinding.root)
		ItemTouchHelper(object : ItemTouchHelper.Callback() {
			override fun getMovementFlags(recyclerView: RecyclerView,
			                              viewHolder: RecyclerView.ViewHolder): Int {
				return makeMovementFlags(ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0)
			}
			
			override fun onMove(recyclerView: RecyclerView,
			                    source: RecyclerView.ViewHolder,
			                    target: RecyclerView.ViewHolder): Boolean {
				collectionAdapter!!.swap(source.getBindingAdapterPosition(), target.getBindingAdapterPosition())
				return true
			}
			
			override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
			}
		}).attachToRecyclerView(orderBinding.recyclerView)
	}
	
	fun updateShortcut() {
		IntStream.range(0, collectionAdapter!!.itemCount).forEach {
			collectionAdapter!!.get(it)
			db!!.updateDashboardShortcutPosition(collectionAdapter!!.get(it).getInteger("id"), it)
		}
	}
	
	fun initAction(inflater: LayoutInflater) {
		actionDialog = BottomSheetDialog(requireContext())
		actionBinding = DialogServiceActionBinding.inflate(inflater)
		actionBinding!!.order.setOnClickListener { orderDialog!!.show() }
		actionDialog!!.setContentView(actionBinding!!.root)
	}
	
	fun showActionDialog(item: JSONObject): Boolean {
		val itemId = item.getIntValue("id")
		val isServiceCollected = MutableLiveData(db!!.isServiceCollected(itemId))
		val isShortcutCollected = MutableLiveData(db!!.isDashboardShortcutCollected(itemId))
		with(actionBinding!!) {
			collect.setText(if (true == isServiceCollected.getValue()) R.string.cancel_collect else R.string.collect)
			addToDashboard.setText(if (true == isShortcutCollected.getValue()) R.string.cancel_add_shortcut else R.string.add_to_dashboard)
			addToLauncher.setOnClickListener {
				if (ShortcutManagerCompat.isRequestPinShortcutSupported(requireContext())) {
					var intent = Intent(requireContext(), MainActivity::class.java)
					if (item.containsKey("activity")) {
						try {
							intent = Intent(context, Class.forName(context?.packageName + item.getString("activity")))
						} catch (_: ClassNotFoundException) {
						}
					}
					else if (item.containsKey("url")) intent = Intent(requireContext(), BrowserActivity::class.java).setData(CommonUtil.trim(item.getString("url"))
																																 .toUri())
					val pinShortcutInfo = ShortcutInfoCompat.Builder(requireContext(), "$itemId")
						.setShortLabel(item.getString("name"))
						.setLongLabel(item.getString("name"))
						.setIcon(IconCompat.createWithResource(requireContext(), R.mipmap.icon))
						.setIntent(intent.setAction(Intent.ACTION_VIEW))
						.build()
					ShortcutManagerCompat.requestPinShortcut(requireContext(), pinShortcutInfo, PendingIntent.getBroadcast(requireContext(), 0, ShortcutManagerCompat.createShortcutResultIntent(requireContext(), pinShortcutInfo),  /* flags */
					                                                                                                       PendingIntent.FLAG_IMMUTABLE).intentSender)
				}
				else model.contextUtil.toast(R.string.fail_to_add_shortcut)
			}
			collect.setOnClickListener {
				val isServiceCollect = true == isServiceCollected.getValue()
				if (isServiceCollect) {
					db!!.deleteService(itemId)
					model.contextUtil.toast(R.string.cancel_collect_success)
				}
				else {
					db!!.addService(itemId, item.toJSONString(), collectionAdapter!!.itemCount)
					model.contextUtil.toast(R.string.collect_success)
				}
				shortcutCollection
				collect.setText(if (isServiceCollect) R.string.collect else R.string.cancel_collect)
				isServiceCollected.value = !isServiceCollect
			}
			addToDashboard.setOnClickListener {
				val isShortcutCollect = true == isShortcutCollected.getValue()
				if (isShortcutCollect) {
					db!!.deleteDashboardShortcut(itemId)
					model.contextUtil.toast(R.string.cancel_add_shortcut_success)
				}
				else {
					db!!.addDashboardShortcut(itemId, item.toJSONString(), collectionAdapter!!.itemCount)
					model.contextUtil.toast(R.string.add_shortcut_success)
				}
				viewModel!!.updateDashboardShortcut.value = true
				addToDashboard.setText(if (isShortcutCollect) R.string.add_to_dashboard else R.string.cancel_add_shortcut)
				isShortcutCollected.value = !isShortcutCollect
			}
			feedback.setOnClickListener {
				startActivity(Intent(Intent.ACTION_VIEW).setData("https://github.com/SYSU-Tang/Sysuer/issues/new?title=反馈：服务->${
					item.getString("name")
				}&labels=bug,crash-report".toUri()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
			}
			Markwon.create(requireContext()).setMarkdown(description, "### ${
				item.getString("name")
			}\n${item.getString("description")}")
		}
		actionDialog!!.show()
		return true
	}
}

internal class CourseAdapter : RecyclerAdapter<JSONObject>() {
	var onClick: BiConsumer<JSONObject?, View?>? = null
	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
		return object :
			RecyclerView.ViewHolder(ItemHomeCourseBinding.inflate(LayoutInflater.from(parent.context), parent, false).root) {}
	}
	
	fun setClick(onClick: BiConsumer<JSONObject?, View?>) {
		this.onClick = onClick
	}
	
	override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
		val binding = ItemHomeCourseBinding.bind(holder.itemView)
		val item = get(position)
		holder.itemView.setOnClickListener { v: View? -> onClick!!.accept(item, v) }
		mapOf<TextView, String>(binding.courseTitle to "courseName", binding.location to "teachingPlace", binding.time to "time", binding.teacher to "teacherName", binding.course to "course").forEach { (v: TextView, s: String) ->
			v.text = item.getString(s)
			v.setOnLongClickListener {
				config.copy(s, item.getString(s))
				config.toast(R.string.copy_successfully)
				true
			}
		}
		val isBefore = item.getString("status") == "before"
		holder.itemView.background.setTint(if (item.getString("status") == "in") config.contextUtil.getColorFromAttr(com.google.android.material.R.attr.colorSurfaceDim) else if (isBefore) 0x0 else config.contextUtil.getColorFromAttr(com.google.android.material.R.attr.colorSurface))
		binding.courseTitle.setTextAppearance(if (isBefore) com.google.android.material.R.style.TextAppearance_Material3_TitleMedium else com.google.android.material.R.style.TextAppearance_Material3_TitleMedium_Emphasized)
		binding.item.setAlpha(if (isBefore) 0.64f else 1.0f)
		super.onBindViewHolder(holder, position)
	}
}

internal class ExamAdapter : RecyclerAdapter<JSONObject>() {
	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
		return object :
			RecyclerView.ViewHolder(ItemExamBinding.inflate(LayoutInflater.from(parent.context), parent, false).root) {}
	}
	
	override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
		val binding = ItemExamBinding.bind(holder.itemView)
		val context = holder.itemView.context
		holder.itemView.isClickable = true
		val examData = get(position)
		val startClassTimes = examData.getIntValue("startClassTimes")
		val endClassTimes = examData.getIntValue("endClassTimes")
		val text = arrayOf(examData.getString("examSubjectName"), examData.getString("classroomNumber"), "${examData.getString("examDate")} ${context.resources.getStringArray(R.array.weeks)[examData.getInteger("week") - 1]}", "${examData.getString("duration")}${
			context.getString(R.string.minute)
		}", examData.getString("durationTime"), String.format(context.getString(R.string.section_range), startClassTimes, endClassTimes), "${context.getString(R.string.exam_mode)}：${
			examData.getString("examMode")
		}", "${context.getString(R.string.exam_stage)}：${
			examData.getString("examStage")
		}")
		val materialTextButtons = arrayOf<TextView>(binding.examName, binding.examLocation, binding.examDate, binding.examDuration, binding.examTime, binding.examClassTime, binding.examMode, binding.examStage)
		(0..7).forEach { i ->
			materialTextButtons[i].text = text[i]
			materialTextButtons[i].setOnClickListener {
				config.copy("exam", text[i])
				config.toast(R.string.copy_successfully)
			}
		}
		val isBefore = examData.getString("status") == "before"
		binding.root.background.setTint(if (examData.getString("status") == "in") config.contextUtil.getColorFromAttr(com.google.android.material.R.attr.colorSurfaceDim) else if (isBefore) 0x0 else config.contextUtil.getColorFromAttr(com.google.android.material.R.attr.colorSurface))
		binding.examName.setTextAppearance(if (isBefore) com.google.android.material.R.style.TextAppearance_Material3_TitleMedium else com.google.android.material.R.style.TextAppearance_Material3_TitleMedium_Emphasized)
		binding.root.alpha = if (isBefore) 0.64f else 1.0f
		super.onBindViewHolder(holder, position)
	}
}