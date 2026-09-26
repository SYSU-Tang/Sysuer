package com.miyuyan.sysuer.academic

import android.widget.Toast
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenu
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import com.miyuyan.sysuer.R
import com.miyuyan.sysuer.api.DateTimeManager
import com.miyuyan.sysuer.nav.LeaveReturnRegistrationDetail
import com.miyuyan.sysuer.nav.navigateBack
import com.miyuyan.sysuer.view.ActivityPager
import com.miyuyan.sysuer.view.RowData
import com.miyuyan.sysuer.view.SectionCard
import com.miyuyan.sysuer.view.SectionData
import com.miyuyan.sysuer.view.StatePage

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3Api::class)
@Composable
fun LeaveReturnRegistrationDetailRoute(
	backStack: MutableList<NavKey>,
	key: LeaveReturnRegistrationDetail,
	sharedTransitionScope: SharedTransitionScope? = null,
	animatedVisibilityScope: AnimatedVisibilityScope? = null,
) {
	val activity = LocalActivity.current
	val context = LocalContext.current
	val viewModel: LeaveReturnRegistrationDetailViewModel = viewModel()

	val detailInfo by viewModel.detailInfo.collectAsStateWithLifecycle()
	val uiState by viewModel.uiState.collectAsStateWithLifecycle()

	val isStayVal by viewModel.isStay.collectAsStateWithLifecycle()
	val leaveDateVal by viewModel.leaveDate.collectAsStateWithLifecycle()
	val returnDateVal by viewModel.returnDate.collectAsStateWithLifecycle()
	val destinationTypeVal by viewModel.destinationType.collectAsStateWithLifecycle()
	val transportationVal by viewModel.transportation.collectAsStateWithLifecycle()
	val countryVal by viewModel.country.collectAsStateWithLifecycle()
	val provinceVal by viewModel.province.collectAsStateWithLifecycle()
	val cityVal by viewModel.city.collectAsStateWithLifecycle()
	val stayReasonVal by viewModel.stayReason.collectAsStateWithLifecycle()
	val outDateVal by viewModel.outDate.collectAsStateWithLifecycle()

	val transportOptionsList by viewModel.transportOptions.collectAsStateWithLifecycle()
	val destinationOptionsList by viewModel.destinationOptions.collectAsStateWithLifecycle()
	val countryOptionsList by viewModel.countryOptions.collectAsStateWithLifecycle()
	val provinceOptionsList by viewModel.provinceOptions.collectAsStateWithLifecycle()
	val cityOptionsList by viewModel.cityOptions.collectAsStateWithLifecycle()

	LaunchedEffect(key.id) {
		viewModel.loadDetail(key.id)
	}

	LaunchedEffect(Unit) {
		viewModel.toastEvent.collect { msg ->
			Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
		}
	}

	val basicInfoTitle = stringResource(R.string.basic_info)
	val basicInfoSection = remember(detailInfo) {
		val rows = mutableStateListOf<RowData>()
		detailInfo?.let { data ->
			val keys = listOf(
					"姓名",
					"学号",
					"年级",
					"培养层次",
					"专业",
					"学院",
					"联系电话",
					"宿舍地址",
					"紧急联系人",
					"紧急联系人联系电话",
					"节假日名称",
					"节假日时间",
					"返校报到时间段"
			)
			val fields = arrayOf(
					"xm",
					"xh",
					"nj",
					"pycc",
					"zymc",
					"bmmc",
					"lxdh",
					"ssdz",
					"jjlxr",
					"jjlxrdh",
					"jjrmc",
					"jjrrq",
					"fxbdsj"
			)
			keys.forEachIndexed { idx, k ->
				rows.add(RowData(k, data.getString(fields[idx])))
			}
		}
		SectionData(title = basicInfoTitle, rows = rows)
	}

	ActivityPager(
			title = stringResource(R.string.leave_return_registration),
			isNestedScrollEnabled = false,
			onNavigationClick = { backStack.navigateBack(activity) },
			sharedTransitionScope = sharedTransitionScope,
			animatedVisibilityScope = animatedVisibilityScope,
			sharedKey = "LeaveReturnRegistrationDetail_${key.id}",
			pageContent = {
				StatePage(state = uiState) {
					Column(
							modifier = Modifier
								.fillMaxSize()
								.verticalScroll(rememberScrollState())
								.padding(dimensionResource(R.dimen.horizontal_padding)),
							verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.vertical_margin))
					) {
						SectionCard(section = basicInfoSection, isExpandable = true)

						ElevatedCard(
								modifier = Modifier.fillMaxWidth()
						) {
							Column(
									modifier = Modifier
										.fillMaxWidth()
										.padding(
												horizontal = dimensionResource(R.dimen.horizontal_padding),
												vertical = dimensionResource(R.dimen.vertical_padding)
										),
									verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.vertical_margin))
							) {
								Text(
										text = stringResource(R.string.registration),
										style = MaterialTheme.typography.titleLarge,
										color = MaterialTheme.colorScheme.primary
								)

								SingleChoiceSegmentedButtonRow(
										modifier = Modifier.fillMaxWidth()
								) {
									SegmentedButton(
											selected = isStayVal == "0",
											onClick = { viewModel.setIsStay("0") },
											shape = SegmentedButtonDefaults.itemShape(
													index = 0, count = 2
											)
									) {
										Text("离校")
									}
									SegmentedButton(
											selected = isStayVal == "1",
											onClick = { viewModel.setIsStay("1") },
											shape = SegmentedButtonDefaults.itemShape(
													index = 1, count = 2
											)
									) {
										Text("留校")
									}
								}

								if (isStayVal == "0") {
									DatePickerField(
											label = "预计离校时间",
											value = leaveDateVal,
											onDateSelected = { viewModel.setLeaveDate(it) })
									DatePickerField(
											label = "预计返校时间",
											value = returnDateVal,
											onDateSelected = { viewModel.setReturnDate(it) })
									ExposedDropdownField(
											label = "去向类型",
											value = destinationTypeVal,
											options = destinationOptionsList.map { it.label },
											onValueChange = { selectedLabel ->
												val opt =
													destinationOptionsList.find { it.label == selectedLabel }
												viewModel.setDestinationType(
														opt?.value ?: selectedLabel
												)
											})
									ExposedDropdownField(
											label = "交通工具",
											value = transportationVal,
											options = transportOptionsList.map { it.label },
											onValueChange = { selectedLabel ->
												val opt =
													transportOptionsList.find { it.label == selectedLabel }
												viewModel.setTransportation(
														opt?.value ?: selectedLabel
												)
											})
									ExposedDropdownField(
											label = "国家",
											value = countryVal,
											options = countryOptionsList.map { it.label },
											onValueChange = { selectedLabel ->
												val opt =
													countryOptionsList.find { it.label == selectedLabel }
												viewModel.setCountry(opt?.value ?: selectedLabel)
											})
									if (countryVal == "中国") {
										ExposedDropdownField(
												label = "省份",
												value = provinceVal,
												options = provinceOptionsList.map { it.label },
												onValueChange = { selectedLabel ->
													val opt =
														provinceOptionsList.find { it.label == selectedLabel }
													viewModel.setProvince(
															opt?.value ?: selectedLabel
													)
												})
										if (provinceVal.isNotEmpty()) {
											ExposedDropdownField(
													label = "城市",
													value = cityVal,
													options = cityOptionsList.map { it.label },
													onValueChange = { selectedLabel ->
														val opt =
															cityOptionsList.find { it.label == selectedLabel }
														viewModel.setCity(
																opt?.value ?: selectedLabel
														)
													})
										}
									}
								} else {
									val stayReasons =
										stringArrayResource(R.array.registration_info_keys).toList()
									ExposedDropdownField(
											label = "留校原因",
											value = stayReasonVal,
											options = stayReasons,
											onValueChange = { viewModel.setStayReason(it) })
								}

								Button(
										onClick = { viewModel.save(key.id) },
										enabled = !outDateVal,
										shapes = ButtonDefaults.shapes(),
										modifier = Modifier
											.fillMaxWidth()
											.padding(vertical = dimensionResource(R.dimen.vertical_margin))
								) {
									Text(stringResource(R.string.save))
								}
							}
						}
					}
				}
			})
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExposedDropdownField(
	label: String,
	value: String,
	options: List<String>,
	onValueChange: (String) -> Unit,
	modifier: Modifier = Modifier,
	enabled: Boolean = true,
) {
	var expanded by remember { mutableStateOf(false) }
	ExposedDropdownMenuBox(
			expanded = expanded && enabled,
			onExpandedChange = { if (enabled) expanded = it },
			modifier = modifier
	) {
		OutlinedTextField(
				value = value,
				onValueChange = {},
				readOnly = true,
				enabled = enabled,
				label = { Text(label) },
				trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
				modifier = Modifier
					.fillMaxWidth()
					.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
		)
		ExposedDropdownMenu(
				expanded = expanded && enabled, onDismissRequest = { expanded = false }) {
			options.forEach { option ->
				DropdownMenuItem(text = { Text(option) }, onClick = {
					onValueChange(option)
					expanded = false
				})
			}
		}
	}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerField(
	label: String,
	value: String,
	onDateSelected: (String) -> Unit,
	modifier: Modifier = Modifier,
) {
	var showDialog by remember { mutableStateOf(false) }

	val initialMillis = remember(value) {
		DateTimeManager.toMillis(value)
	}

	OutlinedTextField(
			value = value,
			onValueChange = {},
			readOnly = true,
			label = { Text(label) },
			trailingIcon = {
				IconButton(onClick = { showDialog = true }) {
					Icon(Icons.Default.CalendarMonth, contentDescription = label)
				}
			},
			modifier = modifier
				.fillMaxWidth()
				.clickable { showDialog = true })

	if (showDialog) {
		val datePickerState = rememberDatePickerState(
				initialSelectedDateMillis = initialMillis
		)
		DatePickerDialog(onDismissRequest = { showDialog = false }, confirmButton = {
			TextButton(onClick = {
				datePickerState.selectedDateMillis?.let { millis ->
					onDateSelected(DateTimeManager.toDateString(millis))
				}
				showDialog = false
			}) {
				Text(stringResource(R.string.confirm))
			}
		}, dismissButton = {
			TextButton(onClick = { showDialog = false }) {
				Text(stringResource(R.string.cancel))
			}
		}) {
			DatePicker(state = datePickerState)
		}
	}
}
