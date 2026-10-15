package com.miyuyan.sysuer.studentAffair

import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import com.alibaba.fastjson2.JSONObject
import com.miyuyan.sysuer.R
import com.miyuyan.sysuer.api.CommonUtil.extractValue
import com.miyuyan.sysuer.nav.navigateBack
import com.miyuyan.sysuer.view.ActivityPager
import com.miyuyan.sysuer.view.MenuItem
import com.miyuyan.sysuer.view.SectionData
import com.miyuyan.sysuer.view.SingleSelectChipDropdown
import com.miyuyan.sysuer.view.StaggerScreen
import com.miyuyan.sysuer.view.StatePage
import com.miyuyan.sysuer.view.WarningCard

/**
 * 勤工俭学：单个 ActivityPager 承载招聘信息、简历与申请记录三页（申请记录暂未开发）。
 * 招聘信息页顶部提供学年/校区/岗位类型下拉与岗位名称/设岗单位输入筛选，列表滚动到底部自动加载下一页。
 */
@Composable
fun StudentPartTimeRoute(
	backStack: MutableList<NavKey>,
	sharedTransitionScope: SharedTransitionScope? = null,
	animatedVisibilityScope: AnimatedVisibilityScope? = null,
) {
	val viewModel: StudentPartTimeViewModel = viewModel()
	val activity = LocalActivity.current

	val years by viewModel.years.collectAsStateWithLifecycle()
	val campuses by viewModel.campuses.collectAsStateWithLifecycle()
	val jobTypes by viewModel.jobTypes.collectAsStateWithLifecycle()
	val year by viewModel.year.collectAsStateWithLifecycle()
	val campus by viewModel.campus.collectAsStateWithLifecycle()
	val jobType by viewModel.jobType.collectAsStateWithLifecycle()
	val jobName by viewModel.jobName.collectAsStateWithLifecycle()
	val unitName by viewModel.unitName.collectAsStateWithLifecycle()
	val jobs by viewModel.jobs.collectAsStateWithLifecycle()
	val cv by viewModel.cv.collectAsStateWithLifecycle()
	val uiState by viewModel.uiState.collectAsStateWithLifecycle()
	val cvState by viewModel.cvState.collectAsStateWithLifecycle()

	val cvTitle = stringResource(R.string.cv)
	val awardTitle = stringResource(R.string.award)
	val experienceTitle = stringResource(R.string.experience)

	val jobSections = remember(jobs) {
		val list = mutableStateListOf<SectionData>()
		jobs.forEach { job ->
			list.add(
					SectionData(
							title = job.getString("qgzxgwmc"), rows = extractValue(
							job, arrayOf(
							"岗位名称",
							"岗位类型",
							"所在校区",
							"岗位地址",
							"开始时间",
							"结束时间",
							"状态",
							"设岗单位"
					), arrayOf(
							"qgzxgwmc",
							"qgzxgwlxmc",
							"qgzxszxymc",
							"qgzxdwdz",
							"qgzxgwzpkssj",
							"qgzxgwzpjssj",
							"state",
							"sgdwmc"
					)
					)
					)
			)
		}
		list
	}

	val cvSections = remember(cv, cvTitle, awardTitle, experienceTitle) {
		val list = mutableStateListOf<SectionData>()
		cv?.let { data ->
			list.add(
					SectionData(
							title = cvTitle, rows = extractValue(
							data, arrayOf(
							"学号",
							"姓名",
							"培养单位",
							"专业",
							"培养层次",
							"电话号码",
							"邮箱",
							"最后修改时间",
							"家庭人均月收入(元)",
							"在校每月平均消费(元)",
							"爱好特长",
							"勤工助学经历",
							"工作时间",
							"性别",
							"住宿地址"
					), arrayOf(
							"xh",
							"xm",
							"pydw",
							"zymc",
							"pycc",
							"dhhm",
							"email",
							"zhxgsj",
							"jtrjysr",
							"zxmypjxf",
							"ahtc",
							"qgzxjls",
							"gzsjs",
							"xb",
							"ssdz"
					)
					)
					)
			)
			data.getJSONArray("hjqks")?.forEach { i ->
				list.add(
						SectionData(
								title = awardTitle, rows = extractValue(
								i as JSONObject,
								arrayOf("颁奖单位", "颁奖日期", "奖项"),
								arrayOf("bjdw", "bjrq", "jxmc")
						)
						)
				)
			}
			data.getJSONArray("rzjls")?.forEach { i ->
				list.add(
						SectionData(
								title = experienceTitle, rows = extractValue(
								i as JSONObject, arrayOf(
								"工作单位",
								"工作开始年月",
								"工作结束年月",
								"工作职务",
								"证明人",
								"证明人单位"
						), arrayOf(
								"gzdw", "gzksny", "gzjsny", "gzzw", "zmr", "zmrdwhzw"
						)
						)
						)
				)
			}
		}
		list
	}

	ActivityPager(
			title = stringResource(R.string.student_job),
			navs = listOf(
					MenuItem(
							stringResource(R.string.recruitment_info),
							iconResource = R.drawable.money
					),
					MenuItem(stringResource(R.string.cv), iconResource = R.drawable.account),
					MenuItem(
							stringResource(R.string.application_record),
							iconResource = R.drawable.email
					),
			),
			onNavigationClick = { backStack.navigateBack(activity) },
			onPageChange = { page ->
				if (page == 1) viewModel.loadCv()
			},
			isNestedScrollEnabled = false,
			sharedTransitionScope = sharedTransitionScope,
			animatedVisibilityScope = animatedVisibilityScope,
			sharedKey = "StudentPartTime",
			topBarContent = { page ->
				Column(
						modifier = Modifier
							.fillMaxWidth()
							.padding(
									dimensionResource(R.dimen.horizontal_padding),
									dimensionResource(R.dimen.vertical_padding)
							),


						) {
					WarningCard()
					if (page == 0) FlowRow(
							modifier = Modifier.fillMaxWidth(),
							horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.horizontal_gap)),
//						verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.vertical_margin))
					) {
						SingleSelectChipDropdown(
								title = stringResource(R.string.year),
								entries = years.map { it.getString("label") },
								entryValues = years.map { it.getString("value") },
								selectedIndex = years.indexOfFirst {
									it.getString("value") == year
								}.takeIf { it >= 0 },
								onChange = { _, _, value -> viewModel.selectYear(value.orEmpty()) },
						)
						SingleSelectChipDropdown(
								title = stringResource(R.string.campus),
								entries = campuses.map { it.getString("label") },
								entryValues = campuses.map { it.getString("value") },
								selectedIndex = campuses.indexOfFirst {
									it.getString("value") == campus
								}.takeIf { it >= 0 },
								onChange = { _, _, value -> viewModel.selectCampus(value.orEmpty()) },
						)
						SingleSelectChipDropdown(
								title = stringResource(R.string.job_type),
								entries = jobTypes.map { it.getString("label") },
								entryValues = jobTypes.map { it.getString("value") },
								selectedIndex = jobTypes.indexOfFirst {
									it.getString("value") == jobType
								}.takeIf { it >= 0 },
								onChange = { _, _, value -> viewModel.selectJobType(value.orEmpty()) },
						)
						FilterInputDialog(
								label = stringResource(R.string.job_name),
								value = jobName,
								onConfirm = viewModel::setJobName,
						)
						FilterInputDialog(
								label = stringResource(R.string.employ_unit),
								value = unitName,
								onConfirm = viewModel::setUnitName,
						)
					}
				}
			},
	) { page ->
		when (page) {
			0 -> StatePage(state = uiState, onRetry = { viewModel.reload() }) {
				StaggerScreen(
						sections = jobSections,
						onScrollBottom = { viewModel.loadMore() },
						sharedTransitionScope = sharedTransitionScope,
						animatedVisibilityScope = animatedVisibilityScope,
				)
			}

			1 -> StatePage(state = cvState, onRetry = { viewModel.loadCv() }) {
				StaggerScreen(
						sections = cvSections,
						sharedTransitionScope = sharedTransitionScope,
						animatedVisibilityScope = animatedVisibilityScope,
				)
			}
		}
	}
}

@Composable
private fun FilterInputDialog(
	label: String,
	value: String,
	onConfirm: (String) -> Unit,
) {
	var show by remember { mutableStateOf(false) }
	var text by remember(value) { mutableStateOf(value) }
	Box {
		InputChip(
				selected = value.isNotEmpty(),
				onClick = { show = true },
				label = { Text(text = value.ifEmpty { label }) },
				trailingIcon = if (value.isNotEmpty()) {
					{
						// 占位保持布局，实际可点击的关闭图标由覆盖层实现
						Spacer(modifier = Modifier.size(FilterChipDefaults.IconSize))
					}
				} else null,
		)
		if (value.isNotEmpty()) {
			Box(
					modifier = Modifier
						.align(Alignment.CenterEnd)
						.padding(end = 4.dp)
						.size(28.dp)
						.clickable { onConfirm("") },
					contentAlignment = Alignment.Center,
			) {
				Icon(
						imageVector = Icons.Rounded.Close,
						contentDescription = stringResource(R.string.reset),
						modifier = Modifier.size(FilterChipDefaults.IconSize),
				)
			}
		}
	}
	if (show) {
		AlertDialog(
				onDismissRequest = { show = false },
				title = { Text(text = label) },
				text = {
					OutlinedTextField(
							value = text,
							onValueChange = { text = it },
							singleLine = true,
					)
				},
				confirmButton = {
					TextButton(
							onClick = {
								onConfirm(text)
								show = false
							}, shapes = ButtonDefaults.shapes()
					) { Text(stringResource(R.string.confirm)) }
				},
				dismissButton = {
					TextButton(
							onClick = { show = false }, shapes = ButtonDefaults.shapes()
					) { Text(stringResource(R.string.cancel)) }
				},
		)
	}
}
