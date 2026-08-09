package com.sysu.edu.academic

import android.os.Bundle
import android.view.MenuItem
import androidx.lifecycle.Observer
import com.alibaba.fastjson2.JSONObject
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.sysu.edu.BaseActivity
import com.sysu.edu.R
import com.sysu.edu.api.CommonUtil
import com.sysu.edu.api.CommonUtil.extractValue
import com.sysu.edu.api.CommonUtil.getString
import com.sysu.edu.databinding.ActivityPagerBinding
import com.sysu.edu.model.JwxtModel
import com.sysu.edu.view.Pager2Adapter
import com.sysu.edu.view.StaggerFragment
import java.util.function.Consumer

class SchoolEnrollmentActivity : BaseActivity() {
	var page: Int = 0
	lateinit var model: JwxtModel
	override fun onDestroy() {
		super.onDestroy()
		model.dispose()
	}
	
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		model = JwxtModel(this)
		val dataMap = mapOf(R.string.school_enrollment_personal_info to listOf(R.string.school_enrollment_student_number, R.string.school_enrollment_name, R.string.school_enrollment_english_name, R.string.school_enrollment_name_pinyin, R.string.school_enrollment_chinese_name, R.string.school_enrollment_former_name, R.string.school_enrollment_country, R.string.school_enrollment_id_type, R.string.school_enrollment_id_number, R.string.school_enrollment_former_id_type, R.string.school_enrollment_former_id_number, R.string.school_enrollment_gender, R.string.school_enrollment_birthday, R.string.school_enrollment_marital_status, R.string.school_enrollment_health_status, R.string.school_enrollment_religion, R.string.school_enrollment_blood_type, R.string.school_enrollment_id_validity, R.string.school_enrollment_birthplace, R.string.school_enrollment_ethnicity, R.string.school_enrollment_political_status, R.string.school_enrollment_hometown, R.string.school_enrollment_hk_macao_taiwan, R.string.school_enrollment_hobby, R.string.school_enrollment_hk_passport, R.string.school_enrollment_exam_number), R.string.school_enrollment_roll_info to mutableListOf(R.string.school_enrollment_college, R.string.school_enrollment_department, R.string.school_enrollment_grade, R.string.school_enrollment_grade_direction, R.string.school_enrollment_campus, R.string.school_enrollment_grade_category, R.string.school_enrollment_major_category, R.string.school_enrollment_major_direction, R.string.school_enrollment_standard_major, R.string.school_enrollment_cross_college, R.string.school_enrollment_education_system, R.string.school_enrollment_student_type, R.string.school_enrollment_discipline, R.string.school_enrollment_degree_type, R.string.school_enrollment_credit_system, R.string.school_enrollment_need_confirm, R.string.school_enrollment_min_study_years, R.string.school_enrollment_max_study_years, R.string.school_enrollment_class, R.string.school_enrollment_status, R.string.school_enrollment_in_school, R.string.school_enrollment_study_form, R.string.school_enrollment_education_level, R.string.school_enrollment_training_method, R.string.school_enrollment_admission_method, R.string.school_enrollment_admission_date, R.string.school_enrollment_expected_graduation, R.string.school_enrollment_charge_grade, R.string.school_enrollment_graduation_date, R.string.school_enrollment_degree_category, R.string.school_enrollment_certificate_date, R.string.school_enrollment_certificate_number, R.string.school_enrollment_principal, R.string.school_enrollment_degree_date, R.string.school_enrollment_degree_number, R.string.school_enrollment_international_type, R.string.school_enrollment_funding_source, R.string.school_enrollment_csc_number, R.string.school_enrollment_teaching_language, R.string.school_enrollment_origin, R.string.school_enrollment_exam_type, R.string.school_enrollment_graduation_type, R.string.school_enrollment_high_school, R.string.school_enrollment_gaokao_score, R.string.school_enrollment_admission_score, R.string.school_enrollment_province_enroll, R.string.school_enrollment_province_rank, R.string.school_enrollment_province_rank_percent, R.string.school_enrollment_top_rank, R.string.school_enrollment_chinese, R.string.school_enrollment_math, R.string.school_enrollment_english, R.string.school_enrollment_comprehensive, R.string.school_enrollment_physics, R.string.school_enrollment_chemistry, R.string.school_enrollment_biology, R.string.school_enrollment_politics, R.string.school_enrollment_history, R.string.school_enrollment_geography, R.string.school_enrollment_graduation_evaluation, R.string.school_enrollment_exam_characteristics), R.string.school_enrollment_contact_info to mutableListOf(R.string.school_enrollment_phone, R.string.school_enrollment_email, R.string.school_enrollment_train_station, R.string.school_enrollment_qq_wechat, R.string.school_enrollment_postal_code, R.string.school_enrollment_home_phone, R.string.school_enrollment_address, R.string.school_enrollment_home_address))
		val keys = listOf(listOf("studentNumber", "basicName", "basicEngName", "basicNameSpell", "basicChName", "basicOnceName", "basicNationalityNAME", "basicIdentityTypeNAME", "basicIdentityNumber", "basicOnceIdentityNAME", "basicOnceDocumentCode", "basicSexName", "basicBirthday", "basicMarriageNAME", "basicHealthNAME", "basicBeliefNAME", "basicBloodNAME", "basicIdentityValidity", "basicBirthplaceNAME", "basicNationNAME", "basicPoliticsNAME", "basicNativeNAME", "basicOverseasChNAME", "basicHobby", "basicHongKongPassCheck", "basicExaNumber"), mutableListOf("rollCollegeNumNAME", "rollDepartmentNAME", "rollGrade", "rollGradeDirectionNAME", "rollCampusNAME", "rollGradeBroadNAME", "rollBroadNAME", "rollmajorNAME", "rollStandardNAME", "rollFacultyName", "rollEdusys", "rollStuTypeName", "rollStuSubcategory", "rollStuDegcategory", "rollWhetherCreditShow", "rollAffirmShow", "shortest", "longtest", "rollClassNAME", "rollStateNAME", "rollWhetherSchShow", "rollShapeNAME", "rollGradationNAME", "rollWayNAME", "rollEnterWayName", "rollEnterSchDate", "rollPredGradDate", "rollChargeGrade", "gradDate", "gradDegreeName", "gradDetailCertAwardTime", "gradCertNum", "gradPrincipal", "gradDetailDegreeAwardDate", "gradDegreeNum", "generalProvinceRank", "basicOverseasTypeNAME", "basicOverseasCostNAME", "basicCiscode", "basicLanguageNAME", "origins", "originExamType", "originGradType", "originHighSchName", "originExam", "fileGrade", "generalProvinceEnrollNum", "generalProvinceRank", "generalProvinceRankPer", "originChPer", "originMathPer", "originEnglishPer", "originSynthePer", "originPhysicsPer", "originChemistryPer", "originBiologyPer", "originPoliticsPer", "originHistoryPer", "originGeographyPer", "originGradAuthen", "originStuTrait"), mutableListOf("contaPhone", "contaLetter", "contaArrive", "contaWeChat", "contaPostalCode", "contaFaPhone", "contaEailAddress", "contaFaAddress"))
		val pager2Adapter = Pager2Adapter(this)
		val binding = ActivityPagerBinding.inflate(layoutInflater).apply {
			pager.adapter = pager2Adapter
			toolbar.setTitle(R.string.school_enroll)
			toolbar.menu.add(R.string.export)
				.setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_IF_ROOM)
				.setIcon(R.drawable.export)
				.setOnMenuItemClickListener {
					pager.currentItem.takeIf {
						!pager2Adapter.isEmpty && it < pager2Adapter.itemCount
					}?.let {
						val fragment = (pager2Adapter.get(it) as StaggerFragment)
						fragment.export(toolbar, tabLayout.getTabAt(it)?.text.toString())
					}
					true
				}
			TabLayoutMediator(tabLayout, pager) { tab: TabLayout.Tab?, position: Int ->
				tab?.setText(arrayOf(R.string.school_enrollment_basic_info, R.string.school_enrollment_family_info, R.string.school_enrollment_education_info, R.string.school_enrollment_exchange_info, R.string.school_enrollment_change_info, R.string.school_enrollment_major_info, R.string.school_enrollment_register_info, R.string.school_enrollment_punish_info)[position])
			}.attach()
			toolbar.setNavigationOnClickListener { supportFinishAfterTransition() }
		}
		setContentView(binding.getRoot())
		model.message.observe(this, Observer { massage: CommonUtil.Tuple2<Int, JSONObject> ->
			val response = massage.second
			if (response.getInteger("code") == 200) {
				val data = response.getJSONObject("data")
				if (data != null) {
					val what: Int = massage.first
					if (what == 0) {
						dataMap.forEach { (title: Int, keyName: List<Int>) ->
							(pager2Adapter.get(0) as StaggerFragment).addSection(getString(title), R.drawable.calendar, getString(this, keyName.toMutableList()), extractValue(data, keys[listOf(R.string.school_enrollment_personal_info, R.string.school_enrollment_roll_info, R.string.school_enrollment_contact_info).indexOf(title)].toMutableList()))
						}
						addNextPage(1)
					} else {
						var order = 1
						data.getJSONArray("rows").forEach(Consumer { a: Any? ->
							val keyName: IntArray = arrayOf<IntArray?>(intArrayOf(R.string.school_enrollment_relation, R.string.school_enrollment_family_name, R.string.school_enrollment_work_unit, R.string.school_enrollment_position, R.string.school_enrollment_family_phone, R.string.school_enrollment_family_birthday), intArrayOf(R.string.school_enrollment_study_start, R.string.school_enrollment_study_end, R.string.school_enrollment_study_unit, R.string.school_enrollment_study_address), intArrayOf(R.string.school_enrollment_exchange_start, R.string.school_enrollment_exchange_end, R.string.school_enrollment_sent_school, R.string.school_enrollment_sent_major, R.string.school_enrollment_exchange_status), intArrayOf(R.string.school_enrollment_issue_date, R.string.school_enrollment_issue_number, R.string.school_enrollment_move_type, R.string.school_enrollment_change_detail, R.string.school_enrollment_move_reason, R.string.school_enrollment_former_major, R.string.school_enrollment_after_major), intArrayOf(R.string.school_enrollment_minor_type, R.string.school_enrollment_minor_college, R.string.school_enrollment_minor_major, R.string.school_enrollment_minor_grade, R.string.school_enrollment_minor_graduation), intArrayOf(R.string.school_enrollment_academic_year, R.string.school_enrollment_checkin_status, R.string.school_enrollment_register_status, R.string.school_enrollment_payment_status), intArrayOf(R.string.school_enrollment_punish_date, R.string.school_enrollment_punish_brief, R.string.school_enrollment_punish_type, R.string.school_enrollment_punish_source, R.string.school_enrollment_punish_name, R.string.school_enrollment_punish_reason, R.string.school_enrollment_punish_time, R.string.school_enrollment_punish_proof, R.string.school_enrollment_punish_repeal_time, R.string.school_enrollment_punish_repeal_proof, R.string.school_enrollment_punish_graduate, R.string.school_enrollment_punish_degree, R.string.school_enrollment_punish_sponsor, R.string.school_enrollment_punish_department, R.string.school_enrollment_punish_clause, R.string.school_enrollment_punish_money, R.string.school_enrollment_punish_status, R.string.school_enrollment_punish_in_school))[what - 1]!!
							(pager2Adapter.get(what) as StaggerFragment).addSection("${order++}", R.drawable.calendar, getString(this, keyName), extractValue((a as JSONObject?)!!, arrayOf<Array<String?>?>(arrayOf("familyRelationName", "familyMemberName", "familyWorkUnit", "jobName", "familyPhone", "familyBirthday"), arrayOf("experBeginTime", "experEndTime", "experStudyUnit", "experSite"), arrayOf("startTime", "endTime", "sendToCollegeName", "sentToMajorName", "exchangeStatus"), arrayOf("issueDate", "issueNumber", "moveStyle", "changeDetail", "moveReason", "formerGradeMajorProf", "moveAfterGradeMajorProf"), arrayOf("mrollCultureGenreName", "mrollCollegeName", "mrollMajorFieldName", "mrollGrade", "minDouDegMajGradName"), arrayOf("academicYearTerm", "checkInStatusName", "registerStatusName", "payedStatusName"), arrayOf("rewPundate", "rewPunBriefing", "rewPunTypeName", "rewPunSourceName", "rewPunName", "rewPunCause", "rewPunTime", "rewPunProof", "rewPunRepealTime", "rewPunRepealProof", "rewPunWheGraduate", "rewPunWheDegree", "rewPunSponDeparName", "rewPunDeparName", "rewPunAdapt", "rewPunMoney", "rewPunSchrollState", "rewPunWhetherAtsch"))[what - 1]!!))
						})
						if (data.getInteger("total") > (page - 1) * 10) addNextPage(what)
						else if (what < 7) {
							page = 0
							addNextPage(what + 1)
						}
					}
				}
			}
		})
		(0..<8).forEach { pager2Adapter.add(StaggerFragment.newInstance(it)) }
		addNextPage(0)
	}
	
	fun addNextPage(what: Int) {
		arrayOf({ this.enrollment }, { this.family }, { this.experience }, { this.exchange }, { this.change }, { this.min }, { this.register }, { this.punish })[what]()
	}
	
	val enrollment: Unit
		get() {
			model.addAndNext("jwxt/student-status/countrystu/studentRollView", 0)
		}
	val family: Unit
		get() {
			getData("jwxt/student-status/stuFamily/showStudentFamily", 1)
		}
	val experience: Unit
		get() {
			getData("jwxt/student-status/stuExperience/showStudentExperience", 2)
		}
	val exchange: Unit
		get() {
			getData("jwxt/student-status/abroadInformation/myStulistInformation", 3)
		}
	val change: Unit
		get() {
			getData("jwxt/student-status-move/moveStuAgg/showStuChangeRoll", 4)
		}
	val min: Unit
		get() {
			getData("jwxt/minor-status/minDouDegMajRoll/queryMinDouDegMajRoll", 5)
		}
	val register: Unit
		get() {
			getData("jwxt/reports-register/stuRegistration/getSelfRegisterList", 6)
		}
	val punish: Unit
		get() {
			getData("jwxt/student-status/stuRewPunish/showMyStudentRewPunish", 7)
		}
	
	fun getData(url: String?, code: Int) {
		model.addAndNext(url, "{\"pageNo\":${++page},\"pageSize\":10,\"total\":true,\"param\":{}}", code)
	}
}