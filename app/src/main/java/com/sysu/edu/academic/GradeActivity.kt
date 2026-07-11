package com.sysu.edu.academic

import android.content.res.Configuration
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.alibaba.fastjson2.JSONObject
import com.sysu.edu.BaseActivity
import com.sysu.edu.R
import com.sysu.edu.api.CommonUtil
import com.sysu.edu.api.CommonUtil.extractValue
import com.sysu.edu.databinding.ActivityGradeBinding
import com.sysu.edu.databinding.ItemScoreBinding
import com.sysu.edu.model.JwxtModel
import com.sysu.edu.view.RecyclerAdapter
import com.sysu.edu.view.StaggeredFragment
import io.noties.markwon.Markwon
import java.util.Locale
import java.util.function.Consumer

class GradeActivity : BaseActivity() {
	val trainType: MutableLiveData<String?> = MutableLiveData<String?>()
	val year: MutableLiveData<String?> = MutableLiveData<String?>()
	val term: MutableLiveData<Int?> = MutableLiveData<Int?>()
	var termPop: PopupMenu? = null
	var yearPop: PopupMenu? = null
	var typePop: PopupMenu? = null
	var gridLayoutManager: GridLayoutManager? = null
	var years: MutableList<String> = mutableListOf()
	lateinit var model: JwxtModel
	override fun onDestroy() {
		super.onDestroy()
		model.dispose()
	}
	
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		model = JwxtModel(this)
		val adp = ScoreAdapter()
		gridLayoutManager = GridLayoutManager(this, config.column)
		val gradeMap: Map<Char, Int> = mapOf('A' to 100, 'B' to 90, 'C' to 80, 'D' to 70, 'F' to 60)
		val binding = ActivityGradeBinding.inflate(layoutInflater).apply {
			toolbar.setNavigationOnClickListener { supportFinishAfterTransition() }
			tabs.isHorizontalScrollBarEnabled = false
			scores.adapter = adp
			scores.layoutManager = gridLayoutManager
		}
		setContentView(binding.getRoot())
		termPop = PopupMenu(this, binding.term, 0, 0, com.google.android.material.R.style.Widget_Material3_PopupMenu_Overflow)
		val terms = getResources().getStringArray(R.array.terms)
		terms.indices.forEach { i ->
			termPop!!.menu.add(terms[i]).setOnMenuItemClickListener {
				term.value = i + 1
				false
			}
		}
		yearPop = PopupMenu(this, binding.year, 0, 0, com.google.android.material.R.style.Widget_Material3_PopupMenu_Overflow)
		typePop = PopupMenu(this, binding.type, 0, 0, com.google.android.material.R.style.Widget_Material3_PopupMenu_Overflow)
		binding.term.setOnClickListener { termPop!!.show() }
		binding.year.setOnClickListener { yearPop!!.show() }
		binding.type.setOnClickListener { typePop!!.show() }
		yearPop!!.menu.add(R.string.all).setOnMenuItemClickListener {
			model.addAndNext("jwxt/achievement-manage/score-check/list?trainTypeCode=${trainType.getValue()}&addScoreFlag=true", 1)
			model.addAndNext("jwxt/achievement-manage/score-check/getSortByYear?trainTypeCode=${trainType.getValue()}&addScoreFlag=true", 4)
			binding.year.setText(R.string.all)
			false
		}
		class GradeManager {
			var classNumber: String? = null
			var grade: Int = -1
			var position: Int = -1
			var maxGrade: Int = -1
			var isFetching: Boolean = false
			fun getGrade(classNumber: String, pos: Int, maxGrade: Int) {
				this.classNumber = classNumber
				grade = maxGrade
				isFetching = true
				if (this.maxGrade < 0) this.maxGrade = maxGrade
				if (position < 0) position = pos
				model.addAndNext("jwxt/gradua-degree/graduatemsg/studentsGraduationExamination/studentCourse", "{\"pageNo\":1,\"pageSize\":10,\"total\":true,\"param\":{\"achievementCourseNumber\":\"$classNumber\",\"beforeAchievementPoint\":\"$maxGrade\",\"afterAchievementPoint\":\"$maxGrade\",\"cultureTypeCode\":\"01\"}}", 5)
			}
			
			fun getGrade() {
				if (maxGrade - grade < 60) getGrade(classNumber!!, position, --grade)
				else isFetching = false
			}
			
			fun setGrade() {
				adp.setGrade(position, "$grade")
				config.toast("$grade")
				grade = -1
				position = -1
				maxGrade = -1
				classNumber = ""
				isFetching = false
			}
		}
		
		val gradeManager = GradeManager()
		adp.action = { position: Int? ->
			if (gradeManager.isFetching) model.contextUtil.toast(R.string.grade_fetching)
			else {
				val level = adp.getLevel(position!!)
				if (!TextUtils.isEmpty(level)) {
					val minGrade = gradeMap.getOrDefault(level[0], 0)
						.minus((if (level.length == 2) 0 else 6))
					gradeManager.getGrade(adp.getClassNumber(position), position, minGrade)
				}
			}
		}
		val header = binding.header.getFragment<StaggeredFragment>()
		header.setNested(false)
		trainType.observe(this, Observer { score })
		year.observe(this, Observer { s: String? ->
			if (year.getValue() != null && term.getValue() != null) {
				binding.year.text = s
				score
			}
		})
		term.observe(this, Observer { s: Int? ->
			binding.term.text = terms[s!! - 1]
			score
		})
		model.message.observe(this, Observer { message: CommonUtil.Tuple2<Int, JSONObject> ->
			val response = message.second
			if (response.getInteger("code") == 200) {
				when (message.first) {
					1 -> {
						adp.clear()
						response.getJSONArray("data").forEach { adp.add(it as JSONObject) }
					}
					2 -> {
						val pull = response.getJSONObject("data") // 初始化培养类型选项
						val type = pull.getJSONArray("selectTrainType")
						type.forEach { a: Any? ->
							val typeItem = a as JSONObject
							typePop!!.menu.add(typeItem.getString("dataName"))
								.setOnMenuItemClickListener {
									binding.type.text = typeItem.getString("dataName")
									trainType.value = typeItem.getString("dataNumber")
									false
								}
						} // 选择培养类型的第一个选项
						if (!type.isEmpty()) {
							binding.type.text = type.getJSONObject(0).getString("dataName")
							trainType.value = type.getJSONObject(0).getString("dataNumber")
						}
						else model.contextUtil.toast(R.string.no_train_type) // 初始化学年选项
						years.clear()
						val selectYearPull = pull.getJSONArray("selectYearPull")
						if (selectYearPull != null && !selectYearPull.isEmpty()) selectYearPull.forEach(Consumer { a: Any? ->
							years.add((a as JSONObject).getString("dataName"))
							yearPop!!.menu.add(a.getString("dataName")).setOnMenuItemClickListener {
								year.postValue(a.getString("dataName"))
								binding.year.text = a.getString("dataNumber")
								false
							}
						}) //获取这个学期的信息
						this.now
					}
					3 -> { // 初始化学期选项
						val pull = response.getJSONObject("data")
						if (!years.contains(pull.getString("acadYear"))) yearPop!!.menu.add(pull.getString("acadYear"))
							.setOnMenuItemClickListener {
								term.postValue(pull.getInteger("acadSemester"))
								year.postValue(pull.getString("acadYear"))
								false
							}
						term.postValue(pull.getInteger("acadSemester"))
						year.postValue(pull.getString("acadYear"))
					}
					4 -> {
						val pull = response.getJSONObject("data")
						val compulsorySelectTotal = pull.getJSONArray("compulsorySelectTotal")
							.getJSONObject(0)
						val totalRank = compulsorySelectTotal.getString("rank")
						val totalPoint = compulsorySelectTotal.getString("vegPoint")
						val totalCredit = compulsorySelectTotal.getString("totalCredit")
						var rank = ""
						var point = ""
						val compulsorySelectList = pull.getJSONArray("compulsorySelectList")
						if (!compulsorySelectList.isEmpty()) {
							rank = compulsorySelectList.getJSONObject(0).getString("rank")
							point = compulsorySelectList.getJSONObject(0).getString("vegPoint")
						}
						val total = pull.getString("stuTotal")
						header.clear()
						header.add(getString(R.string.total_year), CommonUtil.getString(this, intArrayOf(R.string.total_rank, R.string.total_credit, R.string.total_point)), mutableListOf("$totalRank/$total", totalCredit, totalPoint))
						header.add(terms[if (term.getValue() == null) 1 else term.getValue()!! - 1], CommonUtil.getString(this, intArrayOf(R.string.current_rank, R.string.current_point)), mutableListOf("$rank/$total", point))
						header.add(getString(R.string.credit), CommonUtil.getString(this, intArrayOf(R.string.term_credit, R.string.public_compulsory_credit, R.string.public_select_credit, R.string.major_compulsory_credit, R.string.major_select_credit, R.string.honor_credit)), extractValue(pull.getJSONObject("stuCredit"), arrayOf("allGetCredit", "publicGetCredit", "publicSelectGetCredit", "majorGetCredit", "majorSelectGetCredit", "honorCourseGetCredit")))
					}
					5 -> {
						if (response.containsKey("data") && response.getJSONObject("data")
								.getInteger("total") != 0) gradeManager.setGrade()
						else gradeManager.getGrade()
					}
				}
			}
		})
		this.pull
	}
	
	override fun onConfigurationChanged(newConfig: Configuration) {
		super.onConfigurationChanged(newConfig)
		gridLayoutManager!!.setSpanCount(config.column)
	}
	
	val now: Unit
		get() {
			model.addAndNext("jwxt/base-info/acadyearterm/showNewAcadlist", 3)
		}
	val score: Unit
		get() {
			if (year.getValue() != null && term.getValue() != null && trainType.getValue() != null) {
				getScore(year.getValue()!!, term.getValue()!!, trainType.getValue())
				getTotalScore(year.getValue()!!, term.getValue()!!, trainType.getValue())
			}
		}
	
	fun getScore(year: String, term: Int, type: String?) {
		model.addAndNext(String.format(Locale.getDefault(), "jwxt/achievement-manage/score-check/list?scoSchoolYear=%s&trainTypeCode=%s&addScoreFlag=true&scoSemester=%d", year, type, term), 1)
	}
	
	fun getTotalScore(year: String, term: Int, type: String?) {
		model.addAndNext(String.format(Locale.getDefault(), "jwxt/achievement-manage/score-check/getSortByYear?scoSchoolYear=%s&trainTypeCode=%s&addScoreFlag=true&scoSemester=%d", year, type, term), 4)
	}
	
	val pull: Unit
		get() {
			model.addAndNext("jwxt/achievement-manage/score-check/getPull", 2)
		}
	
	internal class ScoreAdapter : RecyclerAdapter<JSONObject>() {
		var action: Consumer<in Int?>? = null
		override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
			return object :
				RecyclerView.ViewHolder(ItemScoreBinding.inflate(LayoutInflater.from(parent.context), parent, false)
											.getRoot()) {}
		}
		
		fun setGrade(position: Int, grade: String?) {
			get(position)["originalScore"] = grade
			notifyItemChanged(position)
		}
		
		fun getLevel(position: Int): String {
			return get(position).getString("scoFinalScore")
		}
		
		fun getClassNumber(position: Int): String {
			return get(position).getString("scoCourseNumber")
		}
		
		override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
			val binding = ItemScoreBinding.bind(holder.itemView)
			val info = data[position]
			binding.getRoot().setOnClickListener {
				if (info.getString("originalScore") == null) action!!.accept(position)
			}
			val grade = MutableLiveData("")
			if (info.containsKey("scoreList")) info.getJSONArray("scoreList")
				.forEach(Consumer { a: Any? -> grade.value = String.format("%s（%s）%s×%s%%+", grade, (a as JSONObject).getString("FXMC"), a.getString("FXCJ"), a.getString("MRQZ")) })
			binding.subject.text = info.getString("scoCourseName")
			binding.score.text = "${info.getString("scoFinalScore")}${if (info.getString("scoPoint") == null) "" else "/" + info.getString("scoPoint")}"
			Markwon.builder(binding.getRoot().context)
				.build()
				.setMarkdown(binding.info, "- 学期：**${"${info.getString("scoSchoolYear")}第${info.getString("scoSemester")}学期"}**\n- 学分：**${info.getString("scoCredit")}**\n- 班级排名：**${info.getString("teachClassRank")}**\n- 年级排名：**${info.getString("gradeMajorRank")}**\n- 课程类别：**${info.getString("scoCourseCategoryName")}**\n- 老师：**${info.getString("scoTeacherName")}**\n- 是否通过：**${info.getString("accessFlag")}**\n- 考试性质：**${info.getString("examCharacter")}**\n- 班级号：**${info.getString("scoCourseNumber")}**\n- 教学班号：**${info.getString("teachClassNumber")}**\n- 成绩：**${
					if (info.getString("originalScore") == null) binding.getRoot().context.getString(R.string.click_for_grade) else grade.getValue() + "=" + info.getString("originalScore")
				}**")
		}
	}
}
