package com.sysu.edu.academic

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.alibaba.fastjson2.JSONArray
import com.alibaba.fastjson2.JSONObject
import com.google.android.material.snackbar.Snackbar
import com.sysu.edu.BaseActivity
import com.sysu.edu.R
import com.sysu.edu.api.CommonUtil
import com.sysu.edu.api.CommonUtil.extractValue
import com.sysu.edu.databinding.ActivityExamBinding
import com.sysu.edu.model.JwxtModel
import com.sysu.edu.view.StaggeredFragment

class ExamActivity : BaseActivity() {
	lateinit var model: JwxtModel
	override fun onDestroy() {
		super.onDestroy()
		model.dispose()
	}
	
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		val binding = ActivityExamBinding.inflate(layoutInflater).apply { }
		model = JwxtModel(this)
		val examViewModel = ViewModelProvider(this).get<ExamViewModel>(ExamViewModel::class.java)
		examViewModel.termList.observe(this, Observer { terms: ArrayList<String?>? -> binding.terms.setSimpleItems(terms!!.toArray<String?>(arrayOf<String?>())) })
		examViewModel.term.observe(this, Observer { term: String? ->
			binding.terms.setText(term, false)
			getExamWeek(term)
		})
		examViewModel.examWeekList.observe(this, Observer { examWeeks: ArrayList<String?>? -> binding.examWeeks.setSimpleItems(examWeeks!!.toArray<String?>(arrayOf<String?>())) })
		setContentView(binding.getRoot())
		binding.toolbar.setNavigationOnClickListener { supportFinishAfterTransition() }
		binding.fab.setOnClickListener {
			if (examViewModel.term.value == null || examViewModel.examWeekId.value == null) Snackbar.make(binding.fab, R.string.please_select_exam_week, Snackbar.LENGTH_LONG)
				.setAnchorView(R.id.fab)
				.show()
			else {
				Snackbar.make(binding.fab, R.string.querying, Snackbar.LENGTH_LONG)
					.setAnchorView(R.id.fab)
					.show()
				getResult(examViewModel.term.value, examViewModel.examWeekId.value)
			}
		}
		binding.terms.setOnItemClickListener { _: AdapterView<*>?, _: View?, _: Int, _: Long ->
			examViewModel.setTerm(binding.terms.getText().toString())
		}
		binding.examWeeks.setOnItemClickListener { _: AdapterView<*>?, _: View?, i: Int, _: Long ->
			examViewModel.setExamWeekId(examViewModel.examWeekInfo.value?.get(i)!!
											.getString("examWeekId"))
			binding.date.text = "${
				examViewModel.examWeekInfo.value!![i]?.getString("startDate")
			}~${
				examViewModel.examWeekInfo.value!![i]?.getString("endDate")
			}"
			examViewModel.setExamWeek(examViewModel.examWeekList.value?.get(i))
		}
		examViewModel.examResult.observe(this, Observer { result: String? ->
			(binding.examFragment.getFragment<Fragment?>() as StaggeredFragment).clear()
			JSONArray.parse(result).forEach { a: Any? ->
				(a as JSONObject).getJSONObject("timetable").forEach { (_: String?, detail: Any?) ->
					detail?.let{
						val values = ArrayList<String?>()
						(detail as JSONArray).forEach { o: Any? ->
							for (i in arrayOf("examSubjectName", "classroomNumber", "durationTime", "examDate", "acadYear")) values.add((o as JSONObject).getString(i))
						}
						(binding.examFragment.getFragment<Fragment?>() as StaggeredFragment).add(values[0], mutableListOf("科目", "考场", "时长", "日期", "学年"), values)
					}
				}
			}
		})
		model.message.observe(this, Observer { message: CommonUtil.Tuple2<Int, JSONObject> ->
			val response = message.second
			if (response.getInteger("code") == 200) {
				when (message.first) {
					1 -> {
						examViewModel.setTermList(extractValue(response.getJSONArray("data"), "acadYearSemester"))
						term
					}
					2 -> examViewModel.setTerm(response.getJSONObject("data")
												   .getString("acadYearSemester"))
					3 -> {
						val examWeeks = ArrayList<String?>()
						val examWeekInfo = ArrayList<JSONObject?>()
						response.getJSONArray("data").forEach { item: Any? ->
							examWeeks.add((item as JSONObject).getString("examWeekName"))
							examWeekInfo.add(item)
						}
						examViewModel.setExamWeekInfo(examWeekInfo)
						examViewModel.setExamWeekList(examWeeks) //binding.examWeek.setText(response.getJSONObject("data").getString("examWeekName"),false);
					}
					4 -> examViewModel.setExamResult(response.getJSONArray("data").toJSONString())
				}
			}
		})
		terms
	}
	
	val terms: Unit
		get() {
			model.addAndNext("jwxt/base-info/acadyearterm/findAcadyeartermNamesBox", 1)
		}
	val term: Unit
		get() {
			model.addAndNext("jwxt/base-info/acadyearterm/showNewAcadlist", 2)
		}
	
	fun getExamWeek(term: String?) {
		model.addAndNext("jwxt/schedule/agg/commonScheduleExamTime/queryExamWeekName?yearTerm=$term", 3)
	}
	
	fun getResult(term: String?, examWeek: String?) {
		val data = JSONObject()
		if (term != null) data["acadYear"] = term
		if (examWeek != null) data["examWeekName"] = examWeek
		model.addAndNext("jwxt/examination-manage/classroomResource/queryStuEaxmInfo", "$data", 4)
	}
}