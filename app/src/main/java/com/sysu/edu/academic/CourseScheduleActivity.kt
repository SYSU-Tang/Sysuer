package com.sysu.edu.academic

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.MenuItem
import android.view.View
import android.widget.GridLayout
import android.widget.PopupMenu
import androidx.core.app.ActivityOptionsCompat
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.alibaba.fastjson2.JSONObject
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.textview.MaterialTextView
import com.sysu.edu.BaseActivity
import com.sysu.edu.R
import com.sysu.edu.api.CommonUtil
import com.sysu.edu.api.CommonUtil.toStringOrDefault
import com.sysu.edu.databinding.ActivityCourseScheduleBinding
import com.sysu.edu.databinding.ItemAgendaBinding
import com.sysu.edu.databinding.ItemDetailBinding
import com.sysu.edu.databinding.ItemDurationBinding
import com.sysu.edu.databinding.ItemWeekdayBinding
import com.sysu.edu.model.JwxtModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

class CourseScheduleActivity : BaseActivity() {
	val weeks: MutableList<Int?> = mutableListOf()
	val realTime: CommonUtil.Tuple2<String?, Int?> = CommonUtil.Tuple2(null, null)
	var currentTerm: String = ""
	var currentWeekIndex: Int = -1
	var currentWeek: Int = 0
	lateinit var binding: ActivityCourseScheduleBinding
	lateinit var detailBinding: ItemDetailBinding
	lateinit var model: JwxtModel
	override fun onDestroy() {
		super.onDestroy()
		model.dispose()
	}
	
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		model = JwxtModel(this)
		val id: MutableLiveData<String?> = MutableLiveData<String?>()
		val views: MutableList<View?> = mutableListOf()
		var termPop: PopupMenu? = null
		var weekPop: PopupMenu? = null
		val terms: MutableList<String?> = mutableListOf()
		binding = ActivityCourseScheduleBinding.inflate(layoutInflater).apply {
			toolbar.setNavigationOnClickListener { supportFinishAfterTransition() }
			toolbar.getMenu().add(R.string.today).setOnMenuItemClickListener {
				changeTerm(realTime.first!!)
				changeWeek(realTime.second!!)
				false
			}.setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_IF_ROOM)
			month.text = resources.getStringArray(R.array.months)[LocalDate.now().monthValue - 1]
			last.setOnClickListener { changeWeek(currentWeekIndex - 1) }
			next.setOnClickListener { changeWeek(currentWeekIndex + 1) }
			term.setOnClickListener { v: View? ->
				if (termPop == null) {
					termPop = PopupMenu(term.context, v, 0, 0, com.google.android.material.R.style.Widget_Material3_PopupMenu_Overflow)
					terms.forEach { e: String? ->
						termPop.menu.add(String.format(getString(R.string.term_x), e))
							.setOnMenuItemClickListener {
								changeTerm(e!!)
								true
							}
					}
				}
				termPop.show()
			} // 初始化学期选择
			weekTime.setOnClickListener { v: View? ->
				if (weekPop == null) {
					weekPop = PopupMenu(weekTime.context, v, 0, 0, com.google.android.material.R.style.Widget_Material3_PopupMenu_Overflow)
					weeks.forEach { e: Int? ->
						weekPop.menu.add(String.format(getString(R.string.week_d), e))
							.setOnMenuItemClickListener {
								changeWeek(weeks.indexOf(e))
								true
							}
					}
				}
				weekPop.show()
			} // 初始化周次选择
		}
		setContentView(binding.root)
		val duration = getResources().getStringArray(R.array.duration)
		val weekday = LocalDate.now().getDayOfWeek().value - 1
		duration.forEachIndexed { i, period ->
			val durationBinding = ItemDurationBinding.inflate(layoutInflater, binding.day, false)
				.apply {
					courseDuration.text = period!!.replace("~", "\n")
					courseOrder.text = "${i + 1}"
					root.setLayoutParams(GridLayout.LayoutParams())
				}
			if (i == 10) {
				durationBinding.root.measure(View.MEASURED_SIZE_MASK, View.MEASURED_SIZE_MASK)
				binding.month.layoutParams.width = durationBinding.root.measuredWidth
			}
			binding.day.addView(durationBinding.root)
		} // 初始化课程时间
		for (i in 0..6) {
			val itemBinding = ItemWeekdayBinding.inflate(layoutInflater, binding.week, false)
			itemBinding.courseWeek.text = getResources().getStringArray(R.array.weeks_simple)[i]
			itemBinding.courseDate.text = getOldDate(i - weekday)
			val column = View(this)
			if (i == weekday) {
				val color = model.contextUtil.getColorFromAttr(com.google.android.material.R.attr.colorSurfaceDim)
				itemBinding.courseDate.setTextColor(color)
				itemBinding.courseWeek.setTextColor(color)
				itemBinding.root.setBackgroundResource(R.drawable.weekday)
				column.setBackgroundColor(color)
			}
			column.setLayoutParams(GridLayout.LayoutParams(GridLayout.spec(0, 11, 1.0f), GridLayout.spec(i + 1, 1.0f))
									   .apply {
										   width = 0
										   height = 0
										   setGravity(Gravity.FILL)
									   })
			binding.day.addView(column)
			binding.week.addView(itemBinding.root)
		} // 初始化周历
		val detailDialog = BottomSheetDialog(this)
		detailBinding = ItemDetailBinding.inflate(layoutInflater).apply {
			open.setOnClickListener {
				startActivity(Intent(this@CourseScheduleActivity, CourseDetailActivity::class.java).putExtra("id", id.getValue()), ActivityOptionsCompat.makeSceneTransitionAnimation(this@CourseScheduleActivity, open, "miniapp")
					.toBundle())
			}
		} // 初始化打开链接
		detailDialog.setContentView(detailBinding.root)
		model.message.observe(this, Observer { message: CommonUtil.Tuple2<Int, JSONObject> ->
			val response = message.second
			if (response.getInteger("code") == 200) {
				when (message.first) {
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
									val course = (detail as JSONObject).getString("courseName")
									val teacher = detail.getString("teacherName")
									val campus = detail.getString("teachingCampusName")
									val isStop = detail.getString("whetherStopClass")
									val teachingBuildingName = detail.getString("teachingBuildingName")
									val classroomNum = detail.getString("classroomNum")
									val itemAgendaBinding = ItemAgendaBinding.inflate(layoutInflater, binding.day, false)
									val item = itemAgendaBinding.root
									if (isStop != null && "0" != isStop) {
										item.setEnabled(false)
										item.setCardBackgroundColor(model.contextUtil.getColorFromAttr(com.google.android.material.R.attr.colorErrorContainer))
									}
									views.add(item)
									item.setOnClickListener {
										val location = toStringOrDefault<String?>(campus) + "-" + toStringOrDefault<String?>(teachingBuildingName) + "-" + toStringOrDefault<String?>(classroomNum)
										setDialogDetail(course, location, teacher, String.format(getString(R.string.from_to), startClassTimes, endClassTimes), detail.getString("assistantInfo"))
										id.value = detail.getString("classesId")
										detailDialog.show()
									}
									itemAgendaBinding.content.text = "$course/${toStringOrDefault<String?>(teachingBuildingName)}-${toStringOrDefault<String?>(classroomNum)}"
									item.setLayoutParams(GridLayout.LayoutParams().apply {
										columnSpec = GridLayout.spec(week.toInt(), 1.0f)
										width = 0
										height = 0
										setGravity(Gravity.FILL)
										rowSpec = GridLayout.spec(startClassTimes.toInt() - 1, endClassTimes.toInt() - startClassTimes.toInt() + 1, 1.0f)
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
							val date = LocalDate.parse(data.getString("startTime"), DateTimeFormatter.ofPattern("yyyy-MM-dd"))
							if (date != null) {
								binding.month.text = resources.getStringArray(R.array.months)[date.monthValue - 1]
								for (i in 0..6) (binding.week.getChildAt(i + 1)
									.findViewById<View?>(R.id.course_date) as MaterialTextView).text = String.format(Locale.getDefault(), "%2d%s", date.plusDays(i.toLong()).dayOfMonth, getString(R.string.day))
							}
						}
					}
					4 -> {
						terms.clear()
						response.getJSONArray("data")
							.forEach { e: Any? -> terms.add((e as JSONObject).getString("acadYearSemester")) }
					}
					5 -> {
						weeks.clear()
						val nowWeekly = response.getJSONObject("data").getString("nowWeekly")
						if (nowWeekly != null) currentWeek = nowWeekly.toInt()
						response.getJSONObject("data")
							.getJSONArray("weeklyList")
							.forEach { e: Any? -> weeks.add((e as JSONObject).getInteger("weekly")) }
						currentWeekIndex = weeks.indexOf(currentWeek)
						binding.weekTime.text = String.format(getString(R.string.week_d), currentWeek)
						getTable(currentTerm, currentWeek)
						realTime.second = currentWeekIndex
					}
				}
				model.nextAll()
			}
		})
		term
		model.next()
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
		model.add(String.format(Locale.getDefault(), "jwxt/base-info/school-calender?academicYear=%s&weekly=%d", academicYear, week), 3)
	}
	
	fun setDialogDetail(course: String?,
	                    location: String?,
	                    teacher: String?,
	                    classTime: String?,
	                    assistant: String?) {
		detailBinding.course.text = course
		detailBinding.location.text = location
		detailBinding.teacher.text = teacher
		detailBinding.classTime.text = classTime
		detailBinding.assistant.text = assistant
	}
	
	fun changeWeek(newWeek: Int) {
		if (newWeek >= 0 && newWeek < weeks.size) {
			currentWeek = weeks[newWeek]!!
			currentWeekIndex = newWeek
			binding.weekTime.text = String.format(getString(R.string.week_d), currentWeek)
			getTable(currentTerm, currentWeek)
			getRange(currentTerm, currentWeek)
			model.nextAll()
		} else if (newWeek == weeks.size) model.contextUtil.toast(R.string.last_week_warning)
	}
	
	fun getTable(academicYear: String, week: Int) {
		if (academicYear.isNotEmpty() && week > 0) model.add("jwxt/timetable-search/classTableInfo/queryStudentClassTable?academicYear=$academicYear&weekly=$week", 1)
	}
	
	val term: Unit
		get() {
			model.add("jwxt/base-info/acadyearterm/showNewAcadlist", 2)
		}
}