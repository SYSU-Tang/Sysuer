package com.sysu.edu.academic

import android.os.Bundle
import androidx.lifecycle.Observer
import com.alibaba.fastjson2.JSONObject
import com.sysu.edu.BaseActivity
import com.sysu.edu.api.CommonUtil
import com.sysu.edu.api.CommonUtil.extractValue
import com.sysu.edu.databinding.ActivityListBinding
import com.sysu.edu.model.JwxtModel
import com.sysu.edu.view.StaggeredFragment

class CETActivity : BaseActivity() {
	var page: Int = 0
	lateinit var model: JwxtModel
	override fun onDestroy() {
		super.onDestroy()
		model.dispose()
	}
	
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		val fragment: StaggeredFragment
		val binding = ActivityListBinding.inflate(layoutInflater).apply {
			toolbar.setNavigationOnClickListener { supportFinishAfterTransition() }
			fragment = list.getFragment()
			fragment.addExportMenu(toolbar)
		}
		setContentView(binding.getRoot())
		model = JwxtModel(this)
		model.message.observe(this, Observer { message: CommonUtil.Tuple2<Int, JSONObject> ->
			val response = message.second
			if (response.getInteger("code") == 200) {
				if (message.first == 0) {
					val data = response.getJSONObject("data")
					if (data != null) {
						val total = data.getInteger("total")
						var order = 1
						data.getJSONArray("rows").forEach { a: Any? ->
							fragment.add("${order++}",
							             mutableListOf("考试年份", "上/下半年", "语言级别",
							                           "学号", "姓名", "笔试考试时间",
							                           "笔试准考证号", "笔试成绩总分",
							                           "听力分数", "阅读分数", "综合分数",
							                           "写作分数", "口试考试时间",
							                           "口试准考证号", "口语成绩",
							                           "所属学校", "院系", "专业", "年级",
							                           "班级", "笔试科目名称",
							                           "笔试报名号", "笔试报名学校",
							                           "笔试报名校区", "是否缺考",
							                           "是否违纪", "违纪类型",
							                           "是否听力障碍", "口试科目名称",
							                           "口试报名号", "口试报名学校",
							                           "口试报名校区"),
							             extractValue(a as JSONObject, arrayOf("examYear",
							                                                   "thePastOrNextHalfYearName",
							                                                   "languageLevel",
							                                                   "stuNum", "stuName",
							                                                   "writtenExaminationTime",
							                                                   "writtenExaminationNumber",
							                                                   "writtenExaminationTotalScore",
							                                                   "hearingScore",
							                                                   "readingScore",
							                                                   "comprehensiveScore",
							                                                   "writingScore",
							                                                   "oralExamTime",
							                                                   "oralExamNumber",
							                                                   "oralExamAchievement",
							                                                   "schoolName",
							                                                   "collegeName",
							                                                   "professionName",
							                                                   "grade",
							                                                   "stuClassName",
							                                                   "writtenExaminationSubject",
							                                                   "writtenExaminationApplyNumber",
							                                                   "writtenExaminationApplySchool",
							                                                   "writtenExaminationApplyCampus",
							                                                   "whetherMissingTest",
							                                                   "whetherViolation",
							                                                   "violationType",
							                                                   "whetherHearingObstacle",
							                                                   "oralExamSubject",
							                                                   "oralExamApplyNumber",
							                                                   "oralExamApplySchool",
							                                                   "oralExamApplyCampus")))
						}
						if (total > page * 10) this.exchange
					}
				}
				model.nextAll()
			}
		})
		exchange
		model.next()
	}
	
	val exchange: Unit
		get() {
			model.add("jwxt/achievement-manage/englishGradeAchievement/stuPageList",
			          "{\"pageNo\":${++page},\"pageSize\":10,\"total\":true,\"param\":{}}", 0)
		}
}