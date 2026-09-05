package com.sysu.edu.academic

import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FilterAlt
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateSetOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
fun RegistrationRoute(
    backStack: MutableList<NavKey>,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
) {
    val viewModel: RegistrationViewModel = viewModel()
    val context = LocalContext.current
    val activity = LocalActivity.current

    val registerInfo by viewModel.registerInfo.observeAsState(null)
    val payList by viewModel.payList.observeAsState(emptyList())
    val historyList by viewModel.historyList.observeAsState(emptyList())

    LaunchedEffect(Unit) {
        viewModel.fetchRegisterInfo()
        viewModel.fetchPayInfo()
        viewModel.fetchHistoryNextPage()
    }

    val years = remember { mutableStateSetOf<String>() }
    val title = stringResource(R.string.school_enrollment_student_report_info)
    val registerSections = remember(registerInfo) {
        println(registerInfo)
        val sections = mutableStateListOf<SectionData>()
        registerInfo?.let { data ->
            sections.add(SectionData(
                title = title,
                icon = R.drawable.calendar,
                rows = extractValue(
                    context,
                    data,
                    intArrayOf(
                        R.string.student_id,
                        R.string.school_enrollment_academic_year,
                        R.string.school_enrollment_checkin_status,
                        R.string.school_enrollment_register_status,
                        R.string.school_enrollment_payment_status
                    ),
                    arrayOf("stuNum", "academicYearTerm", "checkInStatusName", "registerStatusName", "payedStatusName")
                )
            ))
        }
        sections
    }
    val paySections = remember(payList) {
        val sections = mutableStateListOf<SectionData>()
        payList.forEach { item ->
            sections.add(SectionData(
                title = item.getString("acadYear"),
                icon = R.drawable.money,
                rows = extractValue(
                    context,
                    item,
                    intArrayOf(
                        R.string.year,
                        R.string.category,
                        R.string.item_name,
                        R.string.amount_yuan,
                        R.string.interval,
                        R.string.time
                    ),
                    arrayOf("acadYear", "typeName", "feeTypeName", "payedItemAmount", "feeTimeSection", "editeTime")
                )
            ))
        }
        sections
    }
    val historySections = remember(historyList) {
        val sections = mutableStateListOf<SectionData>()
        years.clear()
        historyList.forEach { item ->
            years.add(item.getString("academicYearTerm").split("-")[0])
            sections.add(SectionData(
                title = item.getString("academicYearTerm"),
                icon = R.drawable.calendar,
                rows = extractValue(
                    context,
                    item,
                    intArrayOf(
                        R.string.school_enrollment_academic_year,
                        R.string.campus,
                        R.string.college,
                        R.string.grade_major,
                        R.string.school_enrollment_payment_status,
                        R.string.school_enrollment_checkin_status,
                        R.string.school_enrollment_register_status,
                        R.string.check_in_date,
                        R.string.register_date
                    ),
                    arrayOf(
                        "academicYearTerm",
                        "campusName",
                        "collegeName",
                        "gradeMajorName",
                        "payedStatusName",
                        "checkInStatusName",
                        "registerStatusName",
                        "checkInDate",
                        "registerDate"
                    )
                )
            ))
        }
        sections
    }

    var dropDownExpanded by remember { mutableStateOf(false) }
    var currentPage by remember { mutableIntStateOf(0) }
    val tabs = listOf(
        MenuItem(stringResource(R.string.registration_current_year)),
        MenuItem(stringResource(R.string.registration_payment)),
        MenuItem(stringResource(R.string.registration_history)),
    )

    ActivityPager(
        title = stringResource(R.string.register_info),
        tabs = tabs,
        topBarMenus = { page ->
            mutableListOf(
                exportMarkdownMenuItem(
                    backStack,
                    listOf(registerSections, paySections, historySections),
                    tabs,
                    stringResource(R.string.register_info)
                )
            ).also {
                if (page == 1) it.add(MenuItem(stringResource(R.string.year), Icons.Rounded.FilterAlt, content = {
                    DropdownMenu(expanded = dropDownExpanded, onDismissRequest = { dropDownExpanded = false }) {
                        years.forEach { year ->
                            DropdownMenuItem(text = { Text(year) }, onClick = {
                                dropDownExpanded = false
                                viewModel.fetchPayInfo(year)
                            })
                        }
                    }
                }) {
                    dropDownExpanded = true
                    true
                })
            }
        },
        onNavigationClick = { backStack.navigateBack(activity) },
        onPageChange = { currentPage = it },
        sharedTransitionScope = sharedTransitionScope,
        animatedVisibilityScope = animatedVisibilityScope,
        sharedKey = "Registration",
        isNestedScrollEnabled = false
    ) { page ->
        StaggerScreen(
            sections = when (page) {
                0 -> registerSections
                1 -> paySections
                2 -> historySections
                else -> registerSections
            },
            isHideNull = page == 1,
            onScrollBottom = {
                if (page == 2 && viewModel.hasMoreHistory()) {
                    viewModel.fetchHistoryNextPage()
                }
            }
        )
    }
}
