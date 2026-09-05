package com.miyuyan.sysuer.academic

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
import com.alibaba.fastjson2.JSONObject
import com.miyuyan.sysuer.R
import com.miyuyan.sysuer.api.CommonUtil.extractValue
import com.miyuyan.sysuer.nav.navigateBack
import com.miyuyan.sysuer.view.ActivityPager
import com.miyuyan.sysuer.view.MenuItem
import com.miyuyan.sysuer.view.SectionData
import com.miyuyan.sysuer.view.StaggerScreen
import com.miyuyan.sysuer.view.exportMarkdownMenuItem

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun DormRoute(
    backStack: MutableList<NavKey>,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
) {
    val context = LocalContext.current
    val viewModel: DormViewModel = viewModel()
    val dormInfo by viewModel.dormInfo.observeAsState(null)
    val activity = LocalActivity.current
    val title = stringResource(R.string.personal_info)
    val personalInfo = remember(dormInfo) {
        val snapshotList = mutableStateListOf<SectionData>()
        dormInfo?.let {
            snapshotList.add(
                SectionData(
                    title = title,
                    rows = extractValue(
                        context,
                        it,
                        intArrayOf(
                            R.string.name,
                            R.string.student_id,
                            R.string.gender,
                            R.string.school,
                            R.string.major,
                            R.string.grade,
                            R.string.training_level,
                            R.string.stay_school_status,
                            R.string.student_status,
                            R.string.contact_number
                        ),
                        arrayOf(
                            "name",
                            "studentNumber",
                            "gender",
                            "academy",
                            "major",
                            "grade",
                            "trainingLevel",
                            "staySchoolStatus",
                            "studentStatus",
                            "contactNumber"
                        )
                    )
                )
            )
        }
        snapshotList
    }
    val roomInfo = remember(dormInfo) {
        val snapshotList = mutableStateListOf<SectionData>()
        dormInfo?.getJSONArray("stayRecordList")?.forEach { e: Any? ->
            val item = e as JSONObject
            snapshotList.add(
                SectionData(
                    title = item.getString("schoolYear"),
                    rows = extractValue(
                        context,
                        item,
                        intArrayOf(
                            R.string.year,
                            R.string.campus,
                            R.string.building,
                            R.string.floor,
                            R.string.room_number,
                            R.string.bed_number,
                            R.string.accommodation_fee,
                            R.string.stay_start_date,
                            R.string.stay_end_date
                        ),
                        arrayOf(
                            "schoolYear",
                            "campus",
                            "buildingName",
                            "floorName",
                            "roomNumber",
                            "bedNumber",
                            "accommodationFee",
                            "startDate",
                            "endDate"
                        )
                    )
                )
            )
        }
        snapshotList
    }
    val feeInfo = remember(dormInfo) {
        val snapshotList = mutableStateListOf<SectionData>()
        dormInfo?.getJSONArray("stayChargeRecordList")?.forEach { e: Any? ->
            val item = e as JSONObject
            snapshotList.add(
                SectionData(
                    title = item.getString("schoolYear"),
                    rows = extractValue(
                        context,
                        item,
                        intArrayOf(
                            R.string.year,
                            R.string.accommodation_standard,
                            R.string.should_pay_stay_charge,
                            R.string.real_pay_stay_charge,
                            R.string.arrears
                        ),
                        arrayOf(
                            "schoolYear",
                            "shouldPayStayCharge",
                            "realPayStayCharge",
                            "charge",
                            "arrears"
                        )
                    )
                )
            )
        }
        snapshotList
    }

    LaunchedEffect(Unit) {
        viewModel.fetchDormInfo()
    }

    val tabs = listOf(
        MenuItem(stringResource(R.string.personal_info)),
        MenuItem(stringResource(R.string.dorm_info)),
        MenuItem(stringResource(R.string.dorm_fee)),
    )
    ActivityPager(
        title = stringResource(R.string.dorm),
        tabs = tabs,
        topBarMenus = {
            listOf(
                exportMarkdownMenuItem(
                    backStack,
                    listOf(personalInfo, roomInfo, feeInfo),
                    tabs,
                    stringResource(R.string.dorm)
                )
            )
        },
        onNavigationClick = { backStack.navigateBack(activity) },
        sharedTransitionScope = sharedTransitionScope,
        animatedVisibilityScope = animatedVisibilityScope,
        sharedKey = "Dorm",
        pageContent = { page ->
            StaggerScreen(sections = when (page) {
                0 -> personalInfo
                1 -> roomInfo
                2 -> feeInfo
                else -> personalInfo
            })
        }
    )
}
