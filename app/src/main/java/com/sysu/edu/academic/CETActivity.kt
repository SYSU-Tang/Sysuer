package com.sysu.edu.academic

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModelProvider
import com.sysu.edu.BaseActivity
import com.sysu.edu.R
import com.sysu.edu.view.ActivityPager
import com.sysu.edu.view.RowData
import com.sysu.edu.view.SectionData
import com.sysu.edu.view.StaggerScreen
import com.sysu.edu.view.exportMarkdownMenuItem

class CETActivity : BaseActivity() {
	private val keys = listOf("考试年份",
	                          "上/下半年",
	                          "语言级别",
	                          "学号",
	                          "姓名",
	                          "笔试考试时间",
	                          "笔试准考证号",
	                          "笔试成绩总分",
	                          "听力分数",
	                          "阅读分数",
	                          "综合分数",
	                          "写作分数",
	                          "口试考试时间",
	                          "口试准考证号",
	                          "口语成绩",
	                          "所属学校",
	                          "院系",
	                          "专业",
	                          "年级",
	                          "班级",
	                          "笔试科目名称",
	                          "笔试报名号",
	                          "笔试报名学校",
	                          "笔试报名校区",
	                          "是否缺考",
	                          "是否违纪",
	                          "违纪类型",
	                          "是否听力障碍",
	                          "口试科目名称",
	                          "口试报名号",
	                          "口试报名学校",
	                          "口试报名校区")
	private val fields = arrayOf("examYear",
	                             "thePastOrNextHalfYearName",
	                             "languageLevel",
	                             "stuNum",
	                             "stuName",
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
	                             "oralExamApplyCampus")
	
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		enableEdgeToEdge()
		val viewModel = ViewModelProvider(this)[CETViewModel::class.java]
		setContent {
			val scores by viewModel.scores.observeAsState(emptyList())
			LaunchedEffect(Unit) {
				viewModel.fetchNextPage()
			}
			val sections = remember(scores) {
				val snapshotList = mutableStateListOf<SectionData>()
				scores.forEachIndexed { index, score ->
					val rows = mutableStateListOf<RowData>()
					keys.forEachIndexed { i, key ->
						rows.add(RowData(key, score.getString(fields[i])))
					}
					snapshotList.add(SectionData(title = "${index + 1}", rows = rows))
				}
				snapshotList
			}
			ActivityPager(title = stringResource(R.string.cet), onNavigationClick = { supportFinishAfterTransition() },
			              topBarMenus = {
				              listOf(
					              exportMarkdownMenuItem(
						              sections,
						              stringResource(R.string.cet),stringResource(R.string.cet))
				                    )
			              }, isNestedScrollEnabled = false, pageContent = {
				StaggerScreen(sections = sections, onScrollBottom = { viewModel.fetchNextPage() })
			})
		}
	}
}