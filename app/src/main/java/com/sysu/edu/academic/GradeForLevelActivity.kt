package com.sysu.edu.academic

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.widget.PopupWindow
import androidx.appcompat.widget.PopupMenu
import androidx.core.app.ActivityOptionsCompat
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.alibaba.fastjson2.JSONObject
import com.sysu.edu.BaseActivity
import com.sysu.edu.R
import com.sysu.edu.api.CommonUtil
import com.sysu.edu.api.CommonUtil.extractValue
import com.sysu.edu.api.CommonUtil.isEmpty
import com.sysu.edu.api.CommonUtil.toStringOrDefault
import com.sysu.edu.databinding.ActivityGradeForLevelBinding
import com.sysu.edu.databinding.PreferenceEditBinding
import com.sysu.edu.model.JwxtModel
import com.sysu.edu.view.StaggeredFragment

class GradeForLevelActivity : BaseActivity() {
	val trainType: MutableLiveData<String?> = MutableLiveData<String?>()
	val year: MutableLiveData<String?> = MutableLiveData<String?>()
	val courseType: MutableLiveData<String?> = MutableLiveData<String?>()
	val courseName: MutableLiveData<String?> = MutableLiveData<String?>()
	val courseNumber: MutableLiveData<String?> = MutableLiveData<String?>()
	val minGrade: MutableLiveData<String?> = MutableLiveData<String?>()
	var page: Int = 1
	var total: Int = -1
	lateinit var fragment: StaggeredFragment
	var yearPop: PopupMenu? = null
	var trainTypePop: PopupMenu? = null
	var courseTypePop: PopupMenu? = null
	var input: MutableLiveData<String?>? = null
	lateinit var model: JwxtModel
	override fun onDestroy() {
		super.onDestroy()
		model.dispose()
	}
	
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		val binding = ActivityGradeForLevelBinding.inflate(layoutInflater)
		setContentView(binding.getRoot())
		model = JwxtModel(this)
		binding.toolbar.setNavigationOnClickListener { supportFinishAfterTransition() }
		fragment = binding.fragment.getFragment()
		fragment.setScrollBottom {
			if ((page - 1) * 10 < total) grade
		}
		binding.toolbar.menu.add("导出").setIcon(R.drawable.export).setOnMenuItemClickListener {
			startActivity(Intent(this, MarkdownViewActivity::class.java).putExtra("content", fragment.toTable())
							  .putExtra("title", "成绩"), ActivityOptionsCompat.makeSceneTransitionAnimation(this, binding.toolbar, "miniapp")
							  .toBundle())
			false
		}.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
		yearPop = PopupMenu(this, binding.year)
		trainTypePop = PopupMenu(this, binding.trainType)
		courseTypePop = PopupMenu(this, binding.courseType)
		val courseNameEditText = PreferenceEditBinding.inflate(layoutInflater)
		val courseNamePop = getPopupWindow(courseNameEditText)
		binding.year.setOnClickListener { yearPop!!.show() }
		binding.trainType.setOnClickListener { trainTypePop!!.show() }
		binding.courseType.setOnClickListener { courseTypePop!!.show() }
		binding.courseName.setOnClickListener { v: View? ->
			input = courseName
			courseNamePop.showAsDropDown(v)
			courseNameEditText.textInputLayout.setHint(R.string.course_name)
			courseNameEditText.textInputLayout.requestFocus()
			courseNameEditText.textField.setText(courseName.value)
		}
		binding.courseNumber.setOnClickListener { v: View? ->
			input = courseNumber
			courseNamePop.showAsDropDown(v)
			courseNameEditText.textInputLayout.requestFocus()
			courseNameEditText.textInputLayout.setHint(R.string.course_number)
			courseNameEditText.textField.setText(courseNumber.value)
		}
		binding.minGrade.setOnClickListener { v: View? ->
			input = minGrade
			courseNamePop.showAsDropDown(v)
			courseNameEditText.textInputLayout.requestFocus()
			courseNameEditText.textInputLayout.setHint(R.string.min_grade)
			courseNameEditText.textField.setText(minGrade.value)
		}
		year.observe(this, Observer { regetGrade() })
		trainType.observe(this, Observer { regetGrade() })
		courseType.observe(this, Observer { regetGrade() })
		courseName.observe(this, Observer { s: String? ->
			binding.courseName.text = if (s!!.isEmpty()) getString(R.string.course_name) else s
			regetGrade()
		})
		courseNumber.observe(this, Observer { s: String? ->
			binding.courseNumber.text = if (s!!.isEmpty()) getString(R.string.course_number) else s
			regetGrade()
		})
		minGrade.observe(this, Observer { s: String? ->
			binding.minGrade.text = if (s!!.isEmpty()) getString(R.string.min_grade) else s
			regetGrade()
		})
		grade
		(0..<3).forEach { getData(it) }
		model.message.observe(this, Observer { message: CommonUtil.Tuple2<Int, JSONObject> ->
			val response = message.second
			if (response.getInteger("code") == 200) {
				when (val what: Int = message.first) {
					3 -> {
						if (total == -1) total = response.getJSONObject("data").getInteger("total")
						response.getJSONObject("data").getJSONArray("rows").forEach { item: Any? ->
							fragment.add((item as JSONObject).getString("courseName"), mutableListOf<String?>("绩点", "教学班编号", "课程类别", "课程ID", "课程名称", "课程编号", "学分", "考试性质", "等级", "年级", "开设单位", "学期", "总学时", "培养类别", "总成绩"), extractValue(item, arrayOf("achievementPoint", "classesNum", "courseCategoryName", "courseId", "courseName", "courseNum", "credit", "examNatureName", "finalAchievementStr", "grade", "openClassUnitName", "schoolSemester", "sumHours", "trainingCategoryName", "totalAchievement")))
						}
					}
					0, 1, 2 -> {
						val menu = listOf(trainTypePop, yearPop, courseTypePop)[what]?.menu //                        menu.dispose();
						if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) menu?.setGroupDividerEnabled(true)
						val realLiveDataValue = listOf(trainType, year, courseType)[what]
						val button = listOf(binding.trainType, binding.year, binding.courseType)[what]
						menu?.add(0, 0, 0, R.string.reset)?.setOnMenuItemClickListener {
							button.setText(listOf(R.string.train_type, R.string.year, R.string.course_type)[what])
							realLiveDataValue.value = ""
							true
						}
						response.getJSONArray("data").forEach { e: Any? ->
							menu?.add(1, 0, 0, (e as JSONObject).getString(arrayOf("dataName", "acadYearSemester", "catName")[what]))
								?.setOnMenuItemClickListener { item: MenuItem? ->
									val menuValue = e.getString(arrayOf("dataNumber", "acadYearSemester", "catCode")[what])
									if (menuValue != realLiveDataValue.value) {
										button.text = item!!.title
										realLiveDataValue.value = menuValue
									}
									true
								}
						}
					}
				}
				model.nextAll()
			}
		})
	}
	
	private fun getPopupWindow(courseNameEditText: PreferenceEditBinding): PopupWindow {
		val courseNamePop = PopupWindow(this, null, rikka.preference.simplemenu.R.attr.popupMenuStyle).apply {
			isFocusable = true
			setContentView(courseNameEditText.getRoot())
			isOutsideTouchable = false
			softInputMode = WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE
			inputMethodMode = PopupWindow.INPUT_METHOD_NEEDED
			width = -1
			setOnDismissListener {
				val text = toStringOrDefault<Editable?>(courseNameEditText.textField.getText())
				if (input!!.value != text) input!!.value = text
			}
		}
		
		return courseNamePop
	}
	
	private fun regetGrade() {
		clear()
		grade
	}
	
	private fun clear() {
		fragment.clear()
		page = 1
		total = -1
	}
	
	val grade: Unit
		get() {
			model.addAndNext("jwxt/achievement-manage/achievement/selfPageList", "{\"pageNo\":${page++},\"pageSize\":10,\"total\":true,\"param\":${args}}", 3)
		}
	
	fun getData(pos: Int) {
		model.add(mutableListOf<String?>("jwxt/base-info/codedata/findcodedataNames?datableNumber=97", "jwxt/base-info/acadyearterm/findAcadyeartermNamesBox", "jwxt/base-info/base-category/SfqyBox")[pos], pos)
	}
	
	val args: JSONObject
		/*
			 * {"categoryCode":"01","schoolSemester":"2025-1","courseTypeCode":"10","courseNum":"编码","courseName":"名称","finalAchievement":0,"achievementState":null}
			 * */
		get() {
			val args = JSONObject()
			if (!isEmpty(trainType.value)) args["categoryCode"] = trainType.value
			if (!isEmpty(year.value)) args["schoolSemester"] = year.value
			if (!isEmpty(courseType.value)) args["courseTypeCode"] = courseType.value
			if (!isEmpty(courseName.value)) args["courseName"] = courseName.value
			if (!isEmpty(courseNumber.value)) args["courseNum"] = courseNumber.value
			if (!isEmpty(minGrade.value)) args["finalAchievement"] = minGrade.value!!.toInt()
			return args
		}
}