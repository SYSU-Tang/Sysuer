package com.miyuyan.sysuer.academic

import android.content.Intent
import android.os.Bundle
import android.view.GestureDetector
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.GridLayout
import android.widget.PopupMenu
import androidx.core.app.ActivityOptionsCompat
import androidx.lifecycle.MutableLiveData
import com.alibaba.fastjson2.JSONObject
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.color.MaterialColors
import com.google.android.material.textview.MaterialTextView
import com.miyuyan.sysuer.BaseActivity
import com.miyuyan.sysuer.R
import com.miyuyan.sysuer.api.CommonUtil
import com.miyuyan.sysuer.api.DownloadManager
import com.miyuyan.sysuer.databinding.ActivityCourseScheduleBinding
import com.miyuyan.sysuer.databinding.ItemAgendaBinding
import com.miyuyan.sysuer.databinding.ItemDetailBinding
import com.miyuyan.sysuer.databinding.ItemDurationBinding
import com.miyuyan.sysuer.databinding.ItemWeekdayBinding
import com.miyuyan.sysuer.model.JwxtModel
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

class CourseScheduleActivity : BaseActivity() {
	private var targetSubject: String? = null
	val weeks: MutableList<Int> = mutableListOf()
	val realTime: CommonUtil.Tuple2<String?, Int?> = CommonUtil.Tuple2(null, null)
	var currentTerm: String = ""
	var currentWeekIndex: Int = -1
	var currentWeek: Int = 0
	lateinit var binding: ActivityCourseScheduleBinding
	var selectedCourses: MutableMap<String, List<JSONObject>> = mutableMapOf()
	lateinit var detailBinding: ItemDetailBinding
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
		binding = ActivityCourseScheduleBinding.inflate(layoutInflater).apply {
			toolbar.setNavigationOnClickListener { supportFinishAfterTransition() }
			today.setOnClickListener {
				changeTerm(realTime.first!!)
				changeWeek(realTime.second!!)
			}
			export.setOnClickListener {
				printTable()
			}
			month.text = resources.getStringArray(R.array.months)[LocalDate.now().monthValue - 1]
			last.setOnClickListener { changeWeek(currentWeekIndex - 1) }
			next.setOnClickListener { changeWeek(currentWeekIndex + 1) }
		}
		val weekPop = PopupMenu(
			this,
			binding.weekTime,
			0,
			0,
			com.google.android.material.R.style.Widget_Material3_PopupMenu_Overflow
		)
		val termPop = PopupMenu(
			this,
			binding.term,
			0,
			0,
			com.google.android.material.R.style.Widget_Material3_PopupMenu_Overflow
		)
		binding.term.setOnClickListener {
			termPop.show()
		}
		binding.weekTime.setOnClickListener {
			weekPop.show()
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
					root.setLayoutParams(
						GridLayout.LayoutParams(
							GridLayout.spec(i, 1.0f), GridLayout.spec(0)
						)
					)
				}
			if (i == 10) {
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
						setGravity(Gravity.FILL)
					}
					setBackgroundColor(color)
				})
			}
			binding.day.addView(durationBinding.root)
		} // 初始化课程时间
		for (i in 0..6) {
			val itemBinding = ItemWeekdayBinding.inflate(layoutInflater, binding.week, false)
			itemBinding.courseWeek.text = resources.getStringArray(R.array.weeks_simple)[i]
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
		val detailDialog = BottomSheetDialog(this)
		detailBinding = ItemDetailBinding.inflate(layoutInflater)
		detailDialog.setContentView(detailBinding.root)
		model.message.observe(this) { (code, response) ->
			println("$code $response")
			if (response.getInteger("code") == 200) {
				when (code) {
					1 -> {
						views.forEach { e: View? -> binding.day.removeView(e) }
						views.clear()
						response.getJSONArray("data").forEach { e: Any? ->
							val data = e as JSONObject
							val week = data.getString("week")
							if (week != null) {
								val startClassTimes = data.getString("startClassTimes")
								val endClassTimes = data.getString("endClassTimes")
								val info = data.getJSONArray("teachingInfoList")
								info.forEach { detail: Any? ->
									val course = (detail as JSONObject).getString("courseName", "")
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
										item.setEnabled(false)
										item.setCardBackgroundColor(
											model.contextUtil.getColorFromAttr(
												com.google.android.material.R.attr.colorErrorContainer
											)
										)
									} else {
										val palettes = intArrayOf(
											com.google.android.material.R.attr.colorPrimaryContainer,
											com.google.android.material.R.attr.colorSecondaryContainer,
											com.google.android.material.R.attr.colorTertiaryContainer,
											com.google.android.material.R.attr.colorSurface,
//											com.google.android.material.R.color.m3_ref_palette_cyan20,
										)
										val colorAttr = palettes[abs(course.hashCode()) % palettes.size]
										item.setCardBackgroundColor(
											MaterialColors.getColor(item, colorAttr)
										)
									}
									views.add(item)
									item.setOnClickListener {
										val location = "$campus-$teachingBuildingName-$classroomNum"
										setDialogDetail(
											course, location, teacher, String.format(
												getString(R.string.from_to),
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
											startClassTimes.toInt() - 1,
											endClassTimes.toInt() - startClassTimes.toInt() + 1,
											1.0f
										)
									})
									binding.day.addView(item)
								}
							}
						}
					}

					2 -> {
						currentTerm = response.getJSONObject("data").getString("acadYearSemester")
						binding.term.text = currentTerm
						availableTerms
						getAvailableWeeks(currentTerm)
						getTable(currentTerm, currentWeek)
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
									.findViewById<View?>(R.id.course_date) as MaterialTextView).text =
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
						termPop.menu.clear()
						response.getJSONArray("data")
							.forEach { e: Any? -> terms.add((e as JSONObject).getString("acadYearSemester")) }
						terms.forEach { e: String ->
							termPop.menu.add(getString(R.string.term_x, e))
								.setOnMenuItemClickListener {
									changeTerm(e)
									true
								}
						}
					}

					5 -> {
						weeks.clear()
						weekPop.menu.clear()
						val nowWeekly = response.getJSONObject("data").getString("nowWeekly")
						if (nowWeekly != null) currentWeek = nowWeekly.toInt()
						response.getJSONObject("data").getJSONArray("weeklyList")
							.forEach { e: Any? -> weeks.add((e as JSONObject).getInteger("weekly")) }
						weeks.forEach { e: Int ->
							weekPop.menu.add(getString(R.string.week_d, e))
								.setOnMenuItemClickListener {
									changeWeek(e)
									true
								}
						}
						currentWeekIndex = weeks.indexOf(currentWeek)
						binding.weekTime.text =
							String.format(getString(R.string.week_d), currentWeek)
						getTable(currentTerm, currentWeek)
						realTime.second = currentWeekIndex
					}

					6 -> response.getJSONObject("data").getJSONArray("rows").also {
						selectedCourses[currentTerm] = it.filterIsInstance<JSONObject>()
					}.takeIf { it.isNotEmpty() }?.first {
						(it as JSONObject).getString("courseName") == targetSubject
					}?.also {
						startActivity(
							Intent(this, CourseDetailActivity::class.java).putExtra(
								"id", (it as JSONObject).getString("teachingClassId")
							).putExtra("code", it.getString("courseNum"))
								.putExtra("class", it.getString("teachingClassNum")),
							ActivityOptionsCompat.makeSceneTransitionAnimation(
								this, binding.week, "miniapp"
							).toBundle()
						)
					}
				}
				model.nextAll()
			}
		}
		term
		model.next()
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
		model.add("jwxt/base-info/school-calender/weekly?academicYear=$academicYear", 5)
	}

	val availableTerms: Unit
		get() {
			model.add("jwxt/base-info/acadyearterm/findAcadyeartermNamesBox", 4)
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
			model.nextAll()
		}
	}

	fun getRange(academicYear: String, week: Int) {
		model.add(
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
			selectedCourse.first {
				it.getString("courseName") == course
			}.also {
				startActivity(
					Intent(this, CourseDetailActivity::class.java).putExtra(
						"id", it.getString("teachingClassId")
					).putExtra("code", it.getString("courseNum"))
						.putExtra("class", it.getString("teachingClassNum")),
					ActivityOptionsCompat.makeSceneTransitionAnimation(
						this, binding.week, "miniapp"
					).toBundle()
				)
			}
		}
	}

	fun changeWeek(newWeekIndex: Int) {
		if (newWeekIndex < 0) model.contextUtil.toast(R.string.first_week_warning)
		else if (newWeekIndex >= weeks.size) model.contextUtil.toast(R.string.last_week_warning)
		else {
			currentWeek = weeks[newWeekIndex]
			currentWeekIndex = newWeekIndex
			binding.weekTime.text = String.format(getString(R.string.week_d), currentWeek)
			getTable(currentTerm, currentWeek)
			getRange(currentTerm, currentWeek)
			model.nextAll()
		}
	}

	fun getTable(academicYear: String, week: Int) {
		if (academicYear.isNotEmpty() && week > 0) model.add(
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

	val term: Unit
		get() {
			model.add("jwxt/base-info/acadyearterm/showNewAcadlist", 2)
		}
}