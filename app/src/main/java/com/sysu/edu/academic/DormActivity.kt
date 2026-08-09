package com.sysu.edu.academic

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModelProvider
import com.alibaba.fastjson2.JSONObject
import com.sysu.edu.BaseActivity
import com.sysu.edu.R
import com.sysu.edu.api.CommonUtil.extractValue
import com.sysu.edu.api.DataStoreManager
import com.sysu.edu.browser.RichTextActivity
import com.sysu.edu.view.ActivityPager
import com.sysu.edu.view.MenuItem
import com.sysu.edu.view.SectionData
import com.sysu.edu.view.StaggerScreen
import com.sysu.edu.view.toMarkdown

class DormActivity : BaseActivity() {
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		enableEdgeToEdge()
		val viewModel = ViewModelProvider(this)[DormViewModel::class.java]
		setContent {
			val dormInfo by viewModel.dormInfo.observeAsState(null)
			val personalInfo = remember(dormInfo) {
				val snapshotList = mutableStateListOf<SectionData>()
				dormInfo?.let {
					snapshotList.add(SectionData(title = getString(R.string.personal_info),
					                             rows = extractValue(this,
					                                                 it,
					                                                 intArrayOf(R.string.name, R.string.student_id, R.string.gender, R.string.school, R.string.major, R.string.grade, R.string.training_level, R.string.stay_school_status, R.string.student_status, R.string.contact_number),
					                                                 arrayOf("name", "studentNumber", "gender", "academy", "major", "grade", "trainingLevel", "staySchoolStatus", "studentStatus", "contactNumber"))))
				}
				snapshotList
			}
			val roomInfo = remember(dormInfo) {
				val snapshotList = mutableStateListOf<SectionData>()
				dormInfo?.getJSONArray("stayRecordList")?.forEach { e: Any? ->
					val item = e as JSONObject
					snapshotList.add(SectionData(title = item.getString("schoolYear"),
						rows = extractValue(this,
							item,
							intArrayOf(R.string.year, R.string.campus, R.string.building, R.string.floor, R.string.room_number, R.string.bed_number, R.string.accommodation_fee, R.string.stay_start_date, R.string.stay_end_date),
							arrayOf("schoolYear", "campus", "buildingName", "floorName", "roomNumber", "bedNumber", "accommodationFee", "startDate", "endDate"))))
				}
				snapshotList
			}
			val feeInfo = remember(dormInfo) {
				val snapshotList = mutableStateListOf<SectionData>()
				dormInfo?.getJSONArray("stayChargeRecordList")?.forEach { e: Any? ->
					val item = e as JSONObject
					snapshotList.add(SectionData(title = item.getString("schoolYear"),
						rows = extractValue(this,
					                                                     item,
					                                                     intArrayOf(R.string.year, R.string.accommodation_standard, R.string.should_pay_stay_charge, R.string.real_pay_stay_charge, R.string.arrears),
					                                                     arrayOf("schoolYear", "shouldPayStayCharge", "realPayStayCharge", "charge", "arrears"))))
				}
				snapshotList
			}
			LaunchedEffect(Unit) {
				viewModel.fetchDormInfo()
			}
			ActivityPager(title = stringResource(R.string.dorm),
			              tabs = listOf(
				              MenuItem(stringResource(R.string.personal_info)),
				              MenuItem(stringResource(R.string.dorm_info)),
				              MenuItem(stringResource(R.string.dorm_fee)),
			              ),
			              actions = {
				              IconButton(onClick = {
					              val markdown = "##### ${getString(R.string.personal_info)}\n\n${personalInfo.toMarkdown()}\n\n##### ${getString(R.string.dorm_info)}\n\n${roomInfo.toMarkdown()}\n\n##### ${getString(R.string.dorm_fee)}\n\n${feeInfo.toMarkdown()}"
					              DataStoreManager.saveContent(this@DormActivity, getString(R.string.dorm), markdown) {
						              startActivity(Intent(this@DormActivity, RichTextActivity::class.java).putExtra("type", DataStoreManager.ContentType.MARKDOWN.name).putExtra("title", getString(R.string.dorm)))
					              }
				              }) {
					              Icon(painter = painterResource(R.drawable.export), contentDescription = stringResource(R.string.export))
				              }
			              },
			              onNavigationClick = { supportFinishAfterTransition() }) { page ->
				StaggerScreen(sections = when (page) {
					0 -> personalInfo
					1 -> roomInfo
					2 -> feeInfo
					else -> personalInfo
				})
			}
		}
	}
}