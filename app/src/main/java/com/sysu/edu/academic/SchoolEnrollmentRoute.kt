package com.sysu.edu.academic

import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import com.sysu.edu.R
import com.sysu.edu.api.CommonUtil.extractValue
import com.sysu.edu.nav.navigateBack
import com.sysu.edu.view.ActivityPager
import com.sysu.edu.view.MenuItem
import com.sysu.edu.view.SectionData
import com.sysu.edu.view.StaggerScreen
import com.sysu.edu.view.exportMarkdownMenuItem

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun SchoolEnrollmentRoute(
    backStack: MutableList<NavKey>,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
) {
    val viewModel: SchoolEnrollmentViewModel = viewModel()
    val context = LocalContext.current
    val activity = LocalActivity.current

    val basicNames = remember {
        mapOf(
            R.string.school_enrollment_personal_info to intArrayOf(
                R.string.school_enrollment_student_number,
                R.string.school_enrollment_name,
                R.string.school_enrollment_english_name,
                R.string.school_enrollment_name_pinyin,
                R.string.school_enrollment_chinese_name,
                R.string.school_enrollment_former_name,
                R.string.school_enrollment_country,
                R.string.school_enrollment_id_type,
                R.string.school_enrollment_id_number,
                R.string.school_enrollment_former_id_type,
                R.string.school_enrollment_former_id_number,
                R.string.school_enrollment_gender,
                R.string.school_enrollment_birthday,
                R.string.school_enrollment_marital_status,
                R.string.school_enrollment_health_status,
                R.string.school_enrollment_religion,
                R.string.school_enrollment_blood_type,
                R.string.school_enrollment_id_validity,
                R.string.school_enrollment_birthplace,
                R.string.school_enrollment_ethnicity,
                R.string.school_enrollment_political_status,
                R.string.school_enrollment_hometown,
                R.string.school_enrollment_hk_macao_taiwan,
                R.string.school_enrollment_hobby,
                R.string.school_enrollment_hk_passport,
                R.string.school_enrollment_exam_number
            ),
            R.string.school_enrollment_roll_info to intArrayOf(
                R.string.school_enrollment_college,
                R.string.school_enrollment_department,
                R.string.school_enrollment_grade,
                R.string.school_enrollment_grade_direction,
                R.string.school_enrollment_campus,
                R.string.school_enrollment_grade_category,
                R.string.school_enrollment_major_category,
                R.string.school_enrollment_major_direction,
                R.string.school_enrollment_standard_major,
                R.string.school_enrollment_cross_college,
                R.string.school_enrollment_education_system,
                R.string.school_enrollment_student_type,
                R.string.school_enrollment_discipline,
                R.string.school_enrollment_degree_type,
                R.string.school_enrollment_credit_system,
                R.string.school_enrollment_need_confirm,
                R.string.school_enrollment_min_study_years,
                R.string.school_enrollment_max_study_years,
                R.string.school_enrollment_class,
                R.string.school_enrollment_status,
                R.string.school_enrollment_in_school,
                R.string.school_enrollment_study_form,
                R.string.school_enrollment_education_level,
                R.string.school_enrollment_training_method,
                R.string.school_enrollment_admission_method,
                R.string.school_enrollment_admission_date,
                R.string.school_enrollment_expected_graduation,
                R.string.school_enrollment_charge_grade,
                R.string.school_enrollment_graduation_date,
                R.string.school_enrollment_degree_category,
                R.string.school_enrollment_certificate_date,
                R.string.school_enrollment_certificate_number,
                R.string.school_enrollment_principal,
                R.string.school_enrollment_degree_date,
                R.string.school_enrollment_degree_number,
                R.string.school_enrollment_international_type,
                R.string.school_enrollment_funding_source,
                R.string.school_enrollment_csc_number,
                R.string.school_enrollment_teaching_language,
                R.string.school_enrollment_origin,
                R.string.school_enrollment_exam_type,
                R.string.school_enrollment_graduation_type,
                R.string.school_enrollment_high_school,
                R.string.school_enrollment_gaokao_score,
                R.string.school_enrollment_admission_score,
                R.string.school_enrollment_province_enroll,
                R.string.school_enrollment_province_rank,
                R.string.school_enrollment_province_rank_percent,
                R.string.school_enrollment_top_rank,
                R.string.school_enrollment_chinese,
                R.string.school_enrollment_math,
                R.string.school_enrollment_english,
                R.string.school_enrollment_comprehensive,
                R.string.school_enrollment_physics,
                R.string.school_enrollment_chemistry,
                R.string.school_enrollment_biology,
                R.string.school_enrollment_politics,
                R.string.school_enrollment_history,
                R.string.school_enrollment_geography,
                R.string.school_enrollment_graduation_evaluation,
                R.string.school_enrollment_exam_characteristics
            ),
            R.string.school_enrollment_contact_info to intArrayOf(
                R.string.school_enrollment_phone,
                R.string.school_enrollment_email,
                R.string.school_enrollment_train_station,
                R.string.school_enrollment_qq_wechat,
                R.string.school_enrollment_postal_code,
                R.string.school_enrollment_home_phone,
                R.string.school_enrollment_address,
                R.string.school_enrollment_home_address
            )
        )
    }

    val basicKeys = remember {
        mapOf(
            R.string.school_enrollment_personal_info to arrayOf<String?>(
                "studentNumber", "basicName", "basicEngName", "basicNameSpell", "basicChName",
                "basicOnceName", "basicNationalityNAME", "basicIdentityTypeNAME", "basicIdentityNumber",
                "basicOnceIdentityNAME", "basicOnceDocumentCode", "basicSexName", "basicBirthday",
                "basicMarriageNAME", "basicHealthNAME", "basicBeliefNAME", "basicBloodNAME",
                "basicIdentityValidity", "basicBirthplaceNAME", "basicNationNAME", "basicPoliticsNAME",
                "basicNativeNAME", "basicOverseasChNAME", "basicHobby", "basicHongKongPassCheck", "basicExaNumber"
            ),
            R.string.school_enrollment_roll_info to arrayOf<String?>(
                "rollCollegeNumNAME", "rollDepartmentNAME", "rollGrade", "rollGradeDirectionNAME",
                "rollCampusNAME", "rollGradeBroadNAME", "rollBroadNAME", "rollmajorNAME",
                "rollStandardNAME", "rollFacultyName", "rollEdusys", "rollStuTypeName",
                "rollStuSubcategory", "rollStuDegcategory", "rollWhetherCreditShow", "rollAffirmShow",
                "shortest", "longtest", "rollClassNAME", "rollStateNAME", "rollWhetherSchShow",
                "rollShapeNAME", "rollGradationNAME", "rollWayNAME", "rollEnterWayName",
                "rollEnterSchDate", "rollPredGradDate", "rollChargeGrade", "gradDate", "gradDegreeName",
                "gradDetailCertAwardTime", "gradCertNum", "gradPrincipal", "gradDetailDegreeAwardDate",
                "gradDegreeNum", "generalProvinceRank", "basicOverseasTypeNAME", "basicOverseasCostNAME",
                "basicCiscode", "basicLanguageNAME", "origins", "originExamType", "originGradType",
                "originHighSchName", "originExam", "fileGrade", "generalProvinceEnrollNum",
                "generalProvinceRank", "generalProvinceRankPer", "originChPer", "originMathPer",
                "originEnglishPer", "originSynthePer", "originPhysicsPer", "originChemistryPer",
                "originBiologyPer", "originPoliticsPer", "originHistoryPer", "originGeographyPer",
                "originGradAuthen", "originStuTrait"
            ),
            R.string.school_enrollment_contact_info to arrayOf<String?>(
                "contaPhone", "contaLetter", "contaArrive", "contaWeChat", "contaPostalCode",
                "contaFaPhone", "contaEailAddress", "contaFaAddress"
            )
        )
    }

    val tabNames = remember {
        arrayOf(
            intArrayOf(
                R.string.school_enrollment_relation, R.string.school_enrollment_family_name,
                R.string.school_enrollment_work_unit, R.string.school_enrollment_position,
                R.string.school_enrollment_family_phone, R.string.school_enrollment_family_birthday
            ),
            intArrayOf(
                R.string.school_enrollment_study_start, R.string.school_enrollment_study_end,
                R.string.school_enrollment_study_unit, R.string.school_enrollment_study_address
            ),
            intArrayOf(
                R.string.school_enrollment_exchange_start, R.string.school_enrollment_exchange_end,
                R.string.school_enrollment_sent_school, R.string.school_enrollment_sent_major,
                R.string.school_enrollment_exchange_status
            ),
            intArrayOf(
                R.string.school_enrollment_issue_date, R.string.school_enrollment_issue_number,
                R.string.school_enrollment_move_type, R.string.school_enrollment_change_detail,
                R.string.school_enrollment_move_reason, R.string.school_enrollment_former_major,
                R.string.school_enrollment_after_major
            ),
            intArrayOf(
                R.string.school_enrollment_minor_type, R.string.school_enrollment_minor_college,
                R.string.school_enrollment_minor_major, R.string.school_enrollment_minor_grade,
                R.string.school_enrollment_minor_graduation
            ),
            intArrayOf(
                R.string.school_enrollment_academic_year, R.string.school_enrollment_checkin_status,
                R.string.school_enrollment_register_status, R.string.school_enrollment_payment_status
            ),
            intArrayOf(
                R.string.school_enrollment_punish_date, R.string.school_enrollment_punish_brief,
                R.string.school_enrollment_punish_type, R.string.school_enrollment_punish_source,
                R.string.school_enrollment_punish_name, R.string.school_enrollment_punish_reason,
                R.string.school_enrollment_punish_time, R.string.school_enrollment_punish_proof,
                R.string.school_enrollment_punish_repeal_time, R.string.school_enrollment_punish_repeal_proof,
                R.string.school_enrollment_punish_graduate, R.string.school_enrollment_punish_degree,
                R.string.school_enrollment_punish_sponsor, R.string.school_enrollment_punish_department,
                R.string.school_enrollment_punish_clause, R.string.school_enrollment_punish_money,
                R.string.school_enrollment_punish_status, R.string.school_enrollment_punish_in_school
            )
        )
    }

    val tabKeys = remember {
        arrayOf(
            arrayOf<String?>("familyRelationName", "familyMemberName", "familyWorkUnit", "jobName", "familyPhone", "familyBirthday"),
            arrayOf<String?>("experBeginTime", "experEndTime", "experStudyUnit", "experSite"),
            arrayOf<String?>("startTime", "endTime", "sendToCollegeName", "sentToMajorName", "exchangeStatus"),
            arrayOf<String?>("issueDate", "issueNumber", "moveStyle", "changeDetail", "moveReason", "formerGradeMajorProf", "moveAfterGradeMajorProf"),
            arrayOf<String?>("mrollCultureGenreName", "mrollCollegeName", "mrollMajorFieldName", "mrollGrade", "minDouDegMajGradName"),
            arrayOf<String?>("academicYearTerm", "checkInStatusName", "registerStatusName", "payedStatusName"),
            arrayOf<String?>(
                "rewPundate", "rewPunBriefing", "rewPunTypeName", "rewPunSourceName", "rewPunName",
                "rewPunCause", "rewPunTime", "rewPunProof", "rewPunRepealTime", "rewPunRepealProof",
                "rewPunWheGraduate", "rewPunWheDegree", "rewPunSponDeparName", "rewPunDeparName",
                "rewPunAdapt", "rewPunMoney", "rewPunSchrollState", "rewPunWhetherAtsch"
            )
        )
    }

    val basicInfo by viewModel.basicInfo.observeAsState(null)
    val familyList by viewModel.familyList.observeAsState(emptyList())
    val experienceList by viewModel.experienceList.observeAsState(emptyList())
    val exchangeList by viewModel.exchangeList.observeAsState(emptyList())
    val changeList by viewModel.changeList.observeAsState(emptyList())
    val minorList by viewModel.minorList.observeAsState(emptyList())
    val registerList by viewModel.registerList.observeAsState(emptyList())
    val punishList by viewModel.punishList.observeAsState(emptyList())

    LaunchedEffect(Unit) {
        viewModel.fetchBasicInfo()
    }

    val allSections = remember(
        basicInfo, familyList, experienceList, exchangeList,
        changeList, minorList, registerList, punishList
    ) {
        val sections = Array(8) { mutableStateListOf<SectionData>() }
        basicInfo?.let { data ->
            basicNames.forEach { (titleRes, names) ->
                val keys = basicKeys[titleRes] ?: emptyArray()
                sections[0].add(SectionData(title = context.getString(titleRes), rows = extractValue(context, data, names, keys)))
            }
        }
        val lists = listOf(familyList, experienceList, exchangeList, changeList, minorList, registerList, punishList)
        lists.forEachIndexed { i, list ->
            list.forEachIndexed { index, item ->
                sections[i + 1].add(SectionData(title = "${index + 1}", rows = extractValue(context, item, tabNames[i], tabKeys[i])))
            }
        }
        sections
    }

    val tabs = listOf(
        MenuItem(stringResource(R.string.school_enrollment_basic_info)),
        MenuItem(stringResource(R.string.school_enrollment_family_info)),
        MenuItem(stringResource(R.string.school_enrollment_education_info)),
        MenuItem(stringResource(R.string.school_enrollment_exchange_info)),
        MenuItem(stringResource(R.string.school_enrollment_change_info)),
        MenuItem(stringResource(R.string.school_enrollment_major_info)),
        MenuItem(stringResource(R.string.school_enrollment_register_info)),
        MenuItem(stringResource(R.string.school_enrollment_punish_info)),
    )

    ActivityPager(
        title = stringResource(R.string.school_enroll),
        tabs = tabs,
        topBarMenus = {
            listOf(exportMarkdownMenuItem(backStack, allSections.toList(), tabs, stringResource(R.string.school_enroll)))
        },
        onNavigationClick = { backStack.navigateBack(activity) },
        isNestedScrollEnabled = false,
        sharedTransitionScope = sharedTransitionScope,
        animatedVisibilityScope = animatedVisibilityScope,
        sharedKey = "SchoolEnrollment"
    ) {
        StaggerScreen(sections = allSections[it])
    }
}
