package com.miyuyan.sysuer.academic

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.GestureDetector
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.widget.GridLayout
import androidx.core.app.ActivityOptionsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.alibaba.fastjson2.JSONObject
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.color.MaterialColors
import com.google.android.material.textview.MaterialTextView
import com.miyuyan.sysuer.BaseActivity
import com.miyuyan.sysuer.R
import com.miyuyan.sysuer.api.CommonUtil
import com.miyuyan.sysuer.api.DownloadManager
import com.miyuyan.sysuer.databinding.ActivityCourseScheduleBinding
import com.miyuyan.sysuer.databinding.DialogCourseScheduleAddBinding
import com.miyuyan.sysuer.databinding.DialogCourseScheduleDetailBinding
import com.miyuyan.sysuer.databinding.ItemAgendaBinding
import com.miyuyan.sysuer.databinding.ItemCourseAddTimeBinding
import com.miyuyan.sysuer.databinding.ItemCourseCellBinding
import com.miyuyan.sysuer.databinding.ItemDurationBinding
import com.miyuyan.sysuer.databinding.ItemWeekdayBinding
import com.miyuyan.sysuer.model.JwxtModel
import com.miyuyan.sysuer.view.MaterialPopupMenu
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

data class CourseData(
	val name: String,
	val location: String,
	val segments: List<CourseTimeSegmentData>,
)

data class CourseTimeSegmentData(
	val term: String?,
	val dayIndex: Int,
	val weekStart: Int,
	val weekEnd: Int,
	val sectionStart: Int,
	val sectionEnd: Int,
)

class CourseScheduleActivity : BaseActivity() {
	private var targetSubject: String? = null
	val weeks: MutableList<Int> = mutableListOf()
	val realTime: CommonUtil.Tuple2<String?, Int?> = CommonUtil.Tuple2(null, null)
	var currentTerm: String = ""
	var currentWeekIndex: Int = -1
	var currentWeek: Int = 0
	lateinit var binding: ActivityCourseScheduleBinding
	var selectedCourses: MutableMap<String, List<JSONObject>> = mutableMapOf()
	lateinit var detailBinding: DialogCourseScheduleDetailBinding
	lateinit var model: JwxtModel
	override fun onDestroy() {
		super.onDestroy()
		model.dispose()
	}

	private lateinit var gestureDetector: GestureDetector

	override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
		gestureDetector.onTouchEvent(ev)
		return super.dispatchTouchEvent(ev)
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		model = JwxtModel(this)
		val id: MutableLiveData<String?> = MutableLiveData<String?>()
		val views: MutableList<View> = mutableListOf()
		val terms: MutableList<String> = mutableListOf()
		val courseSegments = mutableListOf<CourseAddTime>()
		val daySimpleNames = resources.getStringArray(R.array.weeks_simple)
		val addDialogBinding = DialogCourseScheduleAddBinding.inflate(layoutInflater)
		val addDialog = BottomSheetDialog(this)
		addDialog.setContentView(addDialogBinding.root)
		fun MutableList<CourseAddTime>.updateSegmentLabels() {
			forEachIndexed { index, segment ->
				segment.setIndex(index + 1)
			}
		}

		fun MutableList<CourseAddTime>.clearAll() {
			forEach { segment ->
				addDialogBinding.timeSegments.removeView(segment.addBinding.root)
			}
			clear()
		}

		fun MutableList<CourseAddTime>.add(initial: CourseAddTime.() -> Unit = {}) {
			val segment = CourseAddTime(this@CourseScheduleActivity).apply {
				loadTerms(terms)
				setTerm(currentTerm)
				setWeekValueTo(weeks.lastOrNull() ?: 17)
				onDelete = {
					remove(this)
					addDialogBinding.timeSegments.removeView(addBinding.root)
					updateSegmentLabels()
				}
				initial()
			}
			add(segment)
			addDialogBinding.timeSegments.addView(
					segment.addBinding.root, addDialogBinding.timeSegments.childCount - 1
			)
			updateSegmentLabels()
		}
		addDialogBinding.apply {
			addTimeSegment.setOnClickListener {
				courseSegments.add()
			}
			addButton.setOnClickListener {
				val name = courseName.text?.toString().orEmpty()
				if (name.isBlank()) {
					config.toast(R.string.course_name_empty_warning)
					return@setOnClickListener
				}
				if (courseSegments.isEmpty()) {
					config.toast(R.string.no_time_segment_warning)
					return@setOnClickListener
				}
				val data = CourseData(
						name = name,
						location = location.text?.toString().orEmpty(),
						segments = courseSegments.map { it.collectData() },
				)
				saveCourse(data)
				addDialog.dismiss()
			}
		}
		binding = ActivityCourseScheduleBinding.inflate(layoutInflater).apply {
			toolbar.setNavigationOnClickListener { supportFinishAfterTransition() }
			today.setOnClickListener {
				changeTerm(realTime.first!!)
				changeWeek(realTime.second!!)
			}

			month.text = resources.getStringArray(R.array.months)[LocalDate.now().monthValue - 1]
			last.setOnClickListener { changeWeek(currentWeekIndex - 1) }
			next.setOnClickListener { changeWeek(currentWeekIndex + 1) }
			toolbar.menu.add(0, 0, 0, "新增").setIcon(R.drawable.add)
				.setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_IF_ROOM).setOnMenuItemClickListener {
					courseSegments.clearAll()
					courseSegments.add()
					addDialog.show()
					true
				}
			toolbar.menu.add(0, 0, 0, "导出").setIcon(R.drawable.export)
				.setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_IF_ROOM).setOnMenuItemClickListener {
					printTable()
					true
				}

		}
		val weekPop = MaterialPopupMenu<Int>(
				this
		).apply {
			onSelectChange = {
				it?.let { newWeekIndex -> changeWeek(newWeekIndex) }
			}
		}
		val termPop = MaterialPopupMenu<String>(
				this
		).apply {
			onValueChange = {
				it?.let { newTerm -> changeTerm(newTerm) }
			}
		}
		binding.term.setOnClickListener {
			termPop.show(binding.bar, binding.mask, it.x.toInt())
		}
		binding.weekTime.setOnClickListener {
			weekPop.show(binding.bar, binding.mask, it.x.toInt())
		}
		setContentView(binding.root)
		gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
			override fun onFling(
				e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float
			): Boolean {
				if (e1 == null) return false
				val diffX = e2.x - e1.x
				val diffY = e2.y - e1.y
				return if (abs(diffX) > abs(diffY) && abs(diffX) > 80 && abs(velocityX) > 200) {
					changeWeek(currentWeekIndex + if (diffX > 0) -1 else 1)
					true
				} else false
			}
		})
		val duration = resources.getStringArray(R.array.duration)
		val weekday = LocalDate.now().getDayOfWeek().value - 1
		val color =
			model.contextUtil.getColorFromAttr(com.google.android.material.R.attr.colorSurfaceDim)
		val nowTime = LocalTime.now()
		duration.forEachIndexed { i, period ->
			val durationBinding =
				ItemDurationBinding.inflate(layoutInflater, binding.day, false).apply {
					courseDuration.text = period.replace("~", "\n")
					courseOrder.text = "${i + 1}"
					root.setBackgroundResource(R.drawable.course_grid_cell)
					root.setLayoutParams(
							GridLayout.LayoutParams(
									GridLayout.spec(i, 1.0f), GridLayout.spec(0)
							).apply {
								setGravity(Gravity.FILL)
								if (i == 4 || i == 8) topMargin = 16
							})
				}
			if (i == 0) {
				durationBinding.root.measure(View.MEASURED_SIZE_MASK, View.MEASURED_SIZE_MASK)
				binding.month.layoutParams.width = durationBinding.root.measuredWidth
			}
			val (startStr, endStr) = period.split("~")
			val start = LocalTime.parse(startStr)
			val end = LocalTime.parse(endStr)
			if (nowTime.isAfter(start) && nowTime.isBefore(end)) {
				durationBinding.root.setBackgroundResource(R.drawable.weekday)
				durationBinding.courseDuration.setTextColor(color)
				durationBinding.courseOrder.setTextColor(color)
				binding.day.addView(View(this).apply {
					layoutParams = GridLayout.LayoutParams(
							GridLayout.spec(i, 1.0f), GridLayout.spec(0, 8, 1.0f)
					).apply {
						width = 0
						height = 0
						if (i == 4 || i == 8) topMargin = 16
						setGravity(Gravity.FILL)
					}
					setBackgroundColor(color)
				})
			}
			binding.day.addView(durationBinding.root)
		} // 初始化课程时间
		daySimpleNames.forEachIndexed { i, week ->
			val itemBinding = ItemWeekdayBinding.inflate(layoutInflater, binding.week, false)
			itemBinding.courseWeek.text = week
			itemBinding.courseDate.text = getOldDate(i - weekday)
			val column = View(this)
			if (i == weekday) {
				val color =
					model.contextUtil.getColorFromAttr(com.google.android.material.R.attr.colorSurfaceDim)
				itemBinding.courseDate.setTextColor(color)
				itemBinding.courseWeek.setTextColor(color)
				itemBinding.root.setBackgroundResource(R.drawable.weekday)
				column.setBackgroundColor(color)
			}
			column.setLayoutParams(
					GridLayout.LayoutParams(
							GridLayout.spec(0, 11, 1.0f), GridLayout.spec(i + 1, 1.0f)
					).apply {
						width = 0
						height = 0
						setGravity(Gravity.FILL)
					})
			binding.day.addView(column)
			binding.week.addView(itemBinding.root)
		} // 初始化周历
		for (row in 0..<11) {
			for (col in 1..<8) {
				var lastClickTime = 0L
				binding.day.addView(
						ItemCourseCellBinding.inflate(
								layoutInflater, binding.day, false
						).apply {
							root.layoutParams = GridLayout.LayoutParams(
									GridLayout.spec(row, 1.0f), GridLayout.spec(col, 1.0f)
							).apply {
								width = 0
								height = 0
								setGravity(Gravity.FILL)
								if (row == 4 || row == 8) topMargin = 16
							}
							root.setOnClickListener {
								val now = System.currentTimeMillis()
								if (now - lastClickTime in 0..300) {
									lastClickTime = 0L
//									val weekday = resources.getStringArray(R.array.weeks)[col - 1]
//									config.toast("$weekday 第${row + 1}节 ${duration[row]}")

									courseSegments.clearAll()
									courseSegments.add {
										setSectionValue(row + 1)
										setDay(col - 1)
									}
									addDialog.show()
								} else {
									lastClickTime = now
								}
							}
						}.root
				)
			}
		}
		val detailDialog = BottomSheetDialog(this)
		detailBinding = DialogCourseScheduleDetailBinding.inflate(layoutInflater)
		detailDialog.setContentView(detailBinding.root)


		val palettes = arrayOf(
				androidx.appcompat.R.attr.colorPrimary to com.google.android.material.R.attr.colorOnPrimary,
				com.google.android.material.R.attr.colorSecondary to com.google.android.material.R.attr.colorOnSecondary,
				com.google.android.material.R.attr.colorTertiary to com.google.android.material.R.attr.colorOnTertiary,
				com.google.android.material.R.attr.colorPrimaryContainer to com.google.android.material.R.attr.colorOnPrimaryContainer,
				com.google.android.material.R.attr.colorSecondaryContainer to com.google.android.material.R.attr.colorOnSecondaryContainer,
				com.google.android.material.R.attr.colorTertiaryContainer to com.google.android.material.R.attr.colorOnTertiaryContainer,
				com.google.android.material.R.attr.colorPrimaryFixed to com.google.android.material.R.attr.colorOnPrimaryFixed,
				com.google.android.material.R.attr.colorSecondaryFixed to com.google.android.material.R.attr.colorOnSecondaryFixed,
				com.google.android.material.R.attr.colorTertiaryFixed to com.google.android.material.R.attr.colorOnTertiaryFixed,
				com.google.android.material.R.attr.colorPrimaryFixedDim to com.google.android.material.R.attr.colorOnPrimaryFixed,
				com.google.android.material.R.attr.colorSecondaryFixedDim to com.google.android.material.R.attr.colorOnSecondaryFixed,
				com.google.android.material.R.attr.colorTertiaryFixedDim to com.google.android.material.R.attr.colorOnTertiaryFixed,
				com.google.android.material.R.attr.colorSurfaceContainerLow to com.google.android.material.R.attr.colorOnSurface,
				com.google.android.material.R.attr.colorSurfaceContainer to com.google.android.material.R.attr.colorOnSurface,
				com.google.android.material.R.attr.colorSurfaceContainerHigh to com.google.android.material.R.attr.colorOnSurface,
				com.google.android.material.R.attr.colorSurfaceContainerHighest to com.google.android.material.R.attr.colorOnSurface,
		)
		val assignedColors = mutableMapOf<String, Int>()
		lifecycleScope.launch {
			repeatOnLifecycle(Lifecycle.State.STARTED) {
				model.messageChannel.collect { (code, response) ->
					if (response.getInteger("code") == 200) {
						when (code) {
							1 -> {
								views.forEach { e: View? -> binding.day.removeView(e) }
								views.clear()
								response.getJSONArray("data").forEach { e: Any? ->
									val data = e as JSONObject
									val week = data.getString("week")
									if (week != null) {
										val startClassTimes = data.getInteger("startClassTimes")
										val endClassTimes = data.getInteger("endClassTimes")
										val info = data.getJSONArray("teachingInfoList")
										info.forEach { detail: Any? ->
											val course =
												(detail as JSONObject).getString("courseName", "")
											val teacher = detail.getString("teacherName", "")
											val campus = detail.getString("teachingCampusName", "")
											val isStop = detail.getString("whetherStopClass", "")
											val teachingBuildingName =
												detail.getString("teachingBuildingName", "")
											val classroomNum = detail.getString("classroomNum", "")
											val itemAgendaBinding = ItemAgendaBinding.inflate(
													layoutInflater, binding.day, false
											)
											val item = itemAgendaBinding.root
											if (isStop != null && "0" != isStop) {
												item.isEnabled = false
												item.setAlpha(0.5f)
//												item.setCardBackgroundColor(
//														MaterialColors.getColor(
//																item,
//																com.google.android.material.R.attr.colorErrorContainer
//														)
//												)
											} else {
												val colorIndex = assignedColors.getOrPut(course) {
													var idx = abs(course.hashCode()) % palettes.size
													val used = assignedColors.values.toSet()
													while (idx in used && assignedColors.size < palettes.size) {
														idx = (idx + 1) % palettes.size
													}
													idx
												}
												val (bgAttr, fgAttr) = palettes[colorIndex]
												item.setCardBackgroundColor(
														MaterialColors.getColor(item, bgAttr)
												)
												itemAgendaBinding.content.setTextColor(
														MaterialColors.getColor(item, fgAttr)
												)
											}
											views.add(item)
											item.setOnClickListener {
												val location =
													"$campus-$teachingBuildingName-$classroomNum"
												setDialogDetail(
														course, location, teacher, getString(
														R.string.from_to_section,
														startClassTimes,
														endClassTimes
												), detail.getString("assistantInfo")
												)
												id.value = detail.getString("classesId")
												detailDialog.show()
											}
											itemAgendaBinding.content.text =
												"$course/$teachingBuildingName-$classroomNum"
											item.setLayoutParams(GridLayout.LayoutParams().apply {
												columnSpec = GridLayout.spec(week.toInt(), 1.0f)
												width = 0
												height = 0
												setGravity(Gravity.FILL)
												rowSpec = GridLayout.spec(
														startClassTimes - 1,
														endClassTimes - startClassTimes + 1,
														1.0f
												)
												if (startClassTimes == 5 || startClassTimes == 9) topMargin =
													16

											})
											binding.day.addView(item)
										}
									}
								}
							}

							2 -> {
								currentTerm =
									response.getJSONObject("data").getString("acadYearSemester")
								binding.term.text = currentTerm
								availableTerms()
								getAvailableWeeks(currentTerm)
								getTable(currentTerm, currentWeek)
								termPop.value = currentTerm
								realTime.first = currentTerm
							}

							3 -> {
								val data = response.getJSONObject("data")
								if (data != null) {
									val date = LocalDate.parse(
											data.getString("startTime"),
											DateTimeFormatter.ofPattern("yyyy-MM-dd")
									)
									if (date != null) {
										binding.month.text =
											resources.getStringArray(R.array.months)[date.monthValue - 1]
										for (i in 0..6) (binding.week.getChildAt(i + 1)
											.findViewById<View>(R.id.course_date) as MaterialTextView).text =
											String.format(
													Locale.getDefault(),
													"%2d%s",
													date.plusDays(i.toLong()).dayOfMonth,
													getString(R.string.day)
											)
									}
								}
							}

							4 -> {
								terms.clear()
								response.getJSONArray("data")
									.forEach { e: Any? -> terms.add((e as JSONObject).getString("acadYearSemester")) }
								terms.forEach { e: String ->
									termPop.addItem(getString(R.string.term_x, e), e)
								}
								termPop.value = currentTerm
							}

							5 -> {
								weeks.clear()
								val nowWeekly =
									response.getJSONObject("data").getString("nowWeekly")
								if (nowWeekly != null) currentWeek = nowWeekly.toInt()
								response.getJSONObject("data").getJSONArray("weeklyList")
									.forEach { e: Any? -> weeks.add((e as JSONObject).getInteger("weekly")) }
								weeks.forEach { e: Int ->
									weekPop.addItem(getString(R.string.week_d, e))
								}
								currentWeekIndex = weeks.indexOf(currentWeek)
								weekPop.select(currentWeekIndex)
								binding.weekTime.text =
									String.format(getString(R.string.week_d), currentWeek)
								getTable(currentTerm, currentWeek)
								realTime.second = currentWeekIndex
							}

							6 -> response.getJSONObject("data").getJSONArray("rows").also {
								selectedCourses[currentTerm] = it.filterIsInstance<JSONObject>()
							}.takeIf { it.isNotEmpty() }?.firstOrNull {
								(it as JSONObject).getString("courseName") == targetSubject
							}?.also {
								startActivity(
										Intent(
												this@CourseScheduleActivity,
												CourseDetailActivity::class.java
										).putExtra(
												"id",
												(it as JSONObject).getString("teachingClassId")
										).putExtra("code", it.getString("courseNum"))
											.putExtra("class", it.getString("teachingClassNum")),
										ActivityOptionsCompat.makeSceneTransitionAnimation(
												this@CourseScheduleActivity, binding.week, "miniapp"
										).toBundle()
								)
							} ?: config.toast(getString(R.string.course_not_found))
						}
					}
				}
			}
		}
		term()
	}

	fun getSelectedCourses(courseName: String?) {
		targetSubject = courseName
		model.addAndNext(
				"jwxt/choose-course-front-server/electiveCourseResult/queryHistory",
				"{\"pageNo\":1,\"pageSize\":100,\"total\":true,\"param\":{\"yearTerm\":\"$currentTerm\",\"successStatus\":\"1\",\"failureStatus\":\"0\",\"retiredClass\":\"0\",\"waitingScreen\":\"0\"}}",
				6
		)
	}

	fun getAvailableWeeks(academicYear: String?) {
		model.addAndNext("jwxt/base-info/school-calender/weekly?academicYear=$academicYear", 5)
	}

	private fun availableTerms() {
		model.addAndNext("jwxt/base-info/acadyearterm/findAcadyeartermNamesBox", 4)
	}

	fun getOldDate(distanceDay: Int): String {
		return LocalDate.now()
			.plusDays(distanceDay.toLong()).dayOfMonth.toString() + getString(R.string.day)
	}

	fun changeTerm(newTerm: String) {
		if (newTerm != currentTerm) {
			currentTerm = newTerm
			binding.term.text = currentTerm
			getAvailableWeeks(currentTerm)
			getTable(currentTerm, currentWeek)
			getRange(currentTerm, currentWeek)
		}
	}

	fun getRange(academicYear: String, week: Int) {
		model.addAndNext(
				String.format(
						Locale.getDefault(),
						"jwxt/base-info/school-calender?academicYear=%s&weekly=%d",
						academicYear,
						week
				), 3
		)
	}

	fun setDialogDetail(
		course: String?,
		location: String?,
		teacher: String?,
		classTime: String?,
		assistant: String?,
	) {
		detailBinding.course.text = course
		detailBinding.location.text = location
		detailBinding.teacher.text = teacher
		detailBinding.classTime.text = classTime
		detailBinding.assistant.text = assistant
		detailBinding.open.setOnClickListener {
			val selectedCourse = selectedCourses[currentTerm] ?: run {
				getSelectedCourses(course)
				return@setOnClickListener
			}
			selectedCourse.firstOrNull {
				it.getString("courseName") == course
			}?.also {
				startActivity(
						Intent(this, CourseDetailActivity::class.java).putExtra(
								"id", it.getString("teachingClassId")
						).putExtra("code", it.getString("courseNum"))
							.putExtra("class", it.getString("teachingClassNum")),
						ActivityOptionsCompat.makeSceneTransitionAnimation(
								this, binding.week, "miniapp"
						).toBundle()
				)
			} ?: config.toast(R.string.course_not_found)
		}
	}

	fun changeWeek(newWeekIndex: Int) {
		if (newWeekIndex < 0) model.contextUtil.toast(R.string.first_week_warning)
		else if (newWeekIndex >= weeks.size) model.contextUtil.toast(R.string.last_week_warning)
		else {
			currentWeek = weeks[newWeekIndex]
			currentWeekIndex = newWeekIndex
			binding.weekTime.text = getString(R.string.week_d, currentWeek)
			getTable(currentTerm, currentWeek)
			getRange(currentTerm, currentWeek)
			model.nextAll()
		}
	}

	fun getTable(academicYear: String, week: Int) {
		if (academicYear.isNotEmpty() && week > 0) model.addAndNext(
				"jwxt/timetable-search/classTableInfo/queryStudentClassTable?academicYear=$academicYear&weekly=$week",
				1
		)
	}

	fun printTable() {
		val request = model.http.generateRequest(
				"https://${model.host}/jwxt/timetable-search/stuTimeTabPrint/output",
				"acadYear=$currentTerm&submitFlag=1&containKey=1%2C2%2C3%2C4%2C5",
				"application/x-www-form-urlencoded"
		).header("Cookie", model.cookie).header("Referer", "https://jwxt.sysu.edu.cn/")
			.header("Accept-Encoding", "identity").build()
		DownloadManager.downloadFile(this, request, "")
	}

	private fun term() {
		model.addAndNext("jwxt/base-info/acadyearterm/showNewAcadlist", 2)
	}

	fun saveCourse(data: CourseData) {
		android.util.Log.d(TAG, "saveCourse: $data")
		config.toast(R.string.course_add_success)
	}

	companion object {
		private const val TAG = "CourseScheduleActivity"
	}

	class CourseAddTime(context: Context) {
		val daySimpleNames = context.resources.getStringArray(R.array.weeks_simple)
		val termPop = MaterialPopupMenu<String>(context)
		val dayPop = MaterialPopupMenu<String>(context)
		var onDelete: (() -> Unit)? = null
		val addBinding = ItemCourseAddTimeBinding.inflate(LayoutInflater.from(context)).apply {
			weekSlider.addOnChangeListener { slider, _, _ ->
				weekContent.text = context.getString(
						R.string.from_to_week,
						slider.values.getOrNull(0)?.toInt(),
						slider.values.getOrNull(1)?.toInt()
				)
			}
			weekSlider.setValues(1f, 17f)
			weekContent.text = context.getString(R.string.from_to_week, 1, 17)
			sectionSlider.addOnChangeListener { slider, _, _ ->
				sectionContent.text = context.getString(
						R.string.from_to_section,
						slider.values.getOrNull(0)?.toInt(),
						slider.values.getOrNull(1)?.toInt()
				)
			}
			sectionContent.text = context.getString(R.string.from_to_section, 1, 11)
			listOf(termItem, dayItem, weekItem, sectionItem).forEachIndexed { index, layout ->
				layout.updateAppearance(index, 4)
			}
			termPop.onNameChange = {
				termContent.text = it
			}
			termItem.setOnClickListener {
				termPop.show(termItem, root, termTitle.x.toInt())
			}
			dayPop.onNameChange = {
				dayContent.text = it
			}
			dayPop.setItems(daySimpleNames.map { s -> "星期$s" })
			dayItem.setOnClickListener {
				dayPop.show(it, it.parent as View, dayTitle.x.toInt())
			}
			close.setOnClickListener {
				onDelete?.invoke()
			}
		}

		fun setIndex(index: Int) {
			addBinding.timeSegment.text = addBinding.root.context.getString(
					R.string.time_segment_x, index
			)
		}

		fun loadTerms(terms: List<String>) {
			termPop.setItems(terms)
		}

		fun setTerm(term: String) {
			termPop.name = term
		}

		fun setDay(dayIndex: Int) {
			dayPop.selectedIndex = dayIndex
		}

		fun setSectionValue(value: Int) {
			addBinding.sectionSlider.values = listOf(value.toFloat(), value.toFloat())
		}

		fun setWeekValueTo(valueTo: Int) {
			addBinding.weekSlider.valueTo = valueTo.toFloat()
		}

		fun collectData(): CourseTimeSegmentData {
			val weekValues = addBinding.weekSlider.values
			val sectionValues = addBinding.sectionSlider.values
			return CourseTimeSegmentData(
					term = termPop.value,
					dayIndex = dayPop.selectedIndex ?: 0,
					weekStart = weekValues.getOrNull(0)?.toInt() ?: 1,
					weekEnd = weekValues.getOrNull(1)?.toInt() ?: 1,
					sectionStart = sectionValues.getOrNull(0)?.toInt() ?: 1,
					sectionEnd = sectionValues.getOrNull(1)?.toInt() ?: 1,
			)
		}
	}
}