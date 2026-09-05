package com.miyuyan.sysuer.academic

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.AndroidViewModel
import com.alibaba.fastjson2.JSONObject
import com.miyuyan.sysuer.R
import com.miyuyan.sysuer.api.CommonUtil.extractValue
import com.miyuyan.sysuer.model.JwxtModel
import com.miyuyan.sysuer.view.RowData
import com.miyuyan.sysuer.view.SectionData

class GradeViewModel(application: Application) : AndroidViewModel(application) {
	val model = JwxtModel(application)
	val scores = mutableStateListOf<JSONObject>()
	val sections = mutableStateListOf<SectionData>()

	var trainType: String? by mutableStateOf(null)
	var trainTypeName: String by mutableStateOf("")
	var year: String? by mutableStateOf(null)
	var yearName: String by mutableStateOf("")
	var termIndex: Int by mutableIntStateOf(0)

	val trainTypes = mutableStateListOf<JSONObject>()
	val years = mutableStateListOf<JSONObject>()
	val terms = application.resources.getStringArray(R.array.terms)

	private var isFetchingGrade = false
	private var gradeManager = GradeManager()

	inner class GradeManager {
		var classNumber: String? = null
		var grade: Int = -1
		var position: Int = -1
		var maxGrade: Int = -1

		fun getGrade(classNumber: String, pos: Int, maxGrade: Int) {
			this.classNumber = classNumber
			this.grade = maxGrade
			isFetchingGrade = true
			if (this.maxGrade < 0) this.maxGrade = maxGrade
			if (position < 0) position = pos
			model.addAndNext(
				"jwxt/gradua-degree/graduatemsg/studentsGraduationExamination/studentCourse",
				"{\"pageNo\":1,\"pageSize\":10,\"total\":true,\"param\":{\"achievementCourseNumber\":\"$classNumber\",\"beforeAchievementPoint\":\"$maxGrade\",\"afterAchievementPoint\":\"$maxGrade\",\"cultureTypeCode\":\"01\"}}",
				5
			)
		}

		fun getGrade() {
			if (maxGrade - grade < 60) getGrade(classNumber!!, position, --grade)
			else isFetchingGrade = false
		}

		fun setGrade() {
			if (position in scores.indices) {
				val updated = JSONObject.from(scores[position])
				updated["originalScore"] = "$grade"
				scores[position] = updated
			}
			model.contextUtil.toast("$grade")
			grade = -1
			position = -1
			maxGrade = -1
			classNumber = ""
			isFetchingGrade = false
		}
	}

	init {
		model.message.observeForever { (code, response) ->
			if (response.getInteger("code") == 200) {
				when (code) {
					1 -> {
						scores.clear()
						response.getJSONArray("data").forEach { scores.add(it as JSONObject) }
					}

					2 -> {
						val pull = response.getJSONObject("data")
						val type = pull.getJSONArray("selectTrainType")
						trainTypes.clear()
						type.forEach { trainTypes.add(it as JSONObject) }

						if (trainTypes.isNotEmpty()) {
							trainTypeName = trainTypes[0].getString("dataName")
							trainType = trainTypes[0].getString("dataNumber")
						}

						years.clear()
						val selectYearPull = pull.getJSONArray("selectYearPull")
						if (selectYearPull != null) {
							selectYearPull.forEach { years.add(it as JSONObject) }
						}
						fetchNow()
					}

					3 -> {
						val pull = response.getJSONObject("data")
						year = pull.getString("acadYear")
						yearName = pull.getString("acadYear")
						termIndex = pull.getInteger("acadSemester")
						fetchScore()
					}

					4 -> {
						val pull = response.getJSONObject("data")
						val compulsorySelectTotal =
							pull.getJSONArray("compulsorySelectTotal").getJSONObject(0)
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
						sections.clear()

						fun createRows(
							names: IntArray,
							values: List<String?>
						): SnapshotStateList<RowData> {
							val rows = mutableStateListOf<RowData>()
							names.forEachIndexed { index, nameRes ->
								rows.add(
									RowData(
										application.getString(nameRes),
										values.getOrNull(index) ?: ""
									)
								)
							}
							return rows
						}

						sections.add(
							SectionData(
								title = application.getString(R.string.total_year),
								rows = createRows(
									intArrayOf(
										R.string.total_rank,
										R.string.total_credit,
										R.string.total_point
									),
									listOf(
										"$totalRank/$total=${totalRank.toFloat() / total.toFloat()}",
										totalCredit,
										totalPoint
									)
								)
							)
						)
						sections.add(
							SectionData(
								title = terms[termIndex],
								rows = createRows(
									intArrayOf(R.string.current_rank, R.string.current_point),
									listOf(
										"$rank/$total${if (rank.isNotEmpty()) "(=${rank.toFloat() / total.toFloat()})" else ""}",
										point
									)
								)
							)
						)
						sections.add(
							SectionData(
								title = application.getString(R.string.credit),
								rows = extractValue(
									application,
									pull.getJSONObject("stuCredit"),
									intArrayOf(
										R.string.term_credit,
										R.string.public_compulsory_credit,
										R.string.public_select_credit,
										R.string.major_compulsory_credit,
										R.string.major_select_credit,
										R.string.honor_credit
									),
									arrayOf(
										"allGetCredit",
										"publicGetCredit",
										"publicSelectGetCredit",
										"majorGetCredit",
										"majorSelectGetCredit",
										"honorCourseGetCredit"
									)
								)
							)
						)
					}

					5 -> {
						if (response.containsKey("data") && response.getJSONObject("data")
								.getInteger("total") != 0
						) gradeManager.setGrade()
						else gradeManager.getGrade()
					}
				}
			} else if (response.getInteger("code") == 52011421) {
				model.contextUtil.toast(
					String.format(
						application.getString(R.string.arrears_warning),
						response.getString("message")
					)
				)
			}
		}
	}

	fun fetchPull() {
		model.addAndNext("jwxt/achievement-manage/score-check/getPull", 2)
	}

	fun fetchNow() {
		model.addAndNext("jwxt/base-info/acadyearterm/showNewAcadlist", 3)
	}

	fun fetchScore() {
		if (year != null && trainType != null) {
			val termValue = if (termIndex == 0) "" else termIndex.toString()
			model.addAndNext(
				"jwxt/achievement-manage/score-check/list?scoSchoolYear=$year&trainTypeCode=$trainType&addScoreFlag=true&scoSemester=$termValue",
				1
			)
			model.addAndNext(
				"jwxt/achievement-manage/score-check/getSortByYear?scoSchoolYear=${year ?: ""}&trainTypeCode=$trainType&addScoreFlag=true&scoSemester=$termValue",
				4
			)
		}
	}

	fun fetchAllYear() {
		if (trainType != null) {
			model.addAndNext(
				"jwxt/achievement-manage/score-check/list?trainTypeCode=$trainType&addScoreFlag=true",
				1
			)
			model.addAndNext(
				"jwxt/achievement-manage/score-check/getSortByYear?trainTypeCode=$trainType&addScoreFlag=true",
				4
			)
		}
	}

	fun requestGrade(position: Int) {
		if (isFetchingGrade) {
			model.contextUtil.toast(R.string.grade_fetching)
			return
		}
		val info = scores[position]
		val level = info.getString("scoFinalScore")
		if (!level.isNullOrEmpty()) {
			val gradeMap = mapOf('A' to 100, 'B' to 90, 'C' to 80, 'D' to 70, 'F' to 60)
			val minGrade =
				gradeMap.getOrDefault(level[0], 0).minus((if (level.length == 2) 0 else 6))
			gradeManager.getGrade(info.getString("scoCourseNumber"), position, minGrade)
		}
	}

	override fun onCleared() {
		model.dispose()
	}
}
