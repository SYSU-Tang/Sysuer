package com.miyuyan.sysuer.life

import android.os.Environment
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AttachMoney
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenu
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import com.alibaba.fastjson2.JSONArray
import com.alibaba.fastjson2.JSONObject
import com.miyuyan.sysuer.R
import com.miyuyan.sysuer.api.CalendarManager
import com.miyuyan.sysuer.api.CommonUtil.extractValue
import com.miyuyan.sysuer.api.DownloadManager
import com.miyuyan.sysuer.nav.navigateBack
import com.miyuyan.sysuer.view.ActivityPager
import com.miyuyan.sysuer.view.MenuItem
import com.miyuyan.sysuer.view.RowData
import com.miyuyan.sysuer.view.SectionData
import com.miyuyan.sysuer.view.StaggerScreen
import com.miyuyan.sysuer.view.exportMarkdownMenuItem
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun PayRoute(
	backStack: MutableList<NavKey>,
	sharedTransitionScope: SharedTransitionScope? = null,
	animatedVisibilityScope: AnimatedVisibilityScope? = null,
) {
	val viewModel: PayViewModel = viewModel()
	val context = LocalContext.current
	val calendarManager = remember { CalendarManager() }
	val dateTimeFormatter = remember { DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss") }

	val toPayList by viewModel.toPayList.observeAsState(JSONArray())
	val selectivePayList by viewModel.selectivePayList.observeAsState(JSONArray())
	val feeList by viewModel.feeList.observeAsState(JSONArray())
	val paymentList by viewModel.paymentList.observeAsState(JSONArray())
	val refundList by viewModel.refundList.observeAsState(JSONArray())
	val pendingPay = viewModel.pendingPay

	LaunchedEffect(Unit) {
		viewModel.fetchToPayList()
		viewModel.fetchSelectivePayList()
		viewModel.fetchFeeList(calendarManager.year.toString())
		viewModel.fetchPaymentList(
			calendarManager.toDateTimeString(calendarManager.firstOfMonth!!.atStartOfDay())!!,
			calendarManager.toDateTimeString(calendarManager.endOfMonth!!.atStartOfDay())
		)
		viewModel.fetchRefundList()
	}

	val toPaySections = remember(toPayList) {
		mutableStateListOf<SectionData>().also { sections ->
			toPayList.forEach { item ->
				sections.add(
					SectionData(
						title = (item as JSONObject).getString("itemName"), rows = extractValue(
							context, item, intArrayOf(
								R.string.student_id,
								R.string.fee_interval,
								R.string.current_due,
								R.string.this_payment
							), arrayOf("personCode", "intervalName", "nowMoney", "needMoney")
						)
					)
				)
			}
		}
	}
	val selectivePaySections = remember(selectivePayList) {
		mutableStateListOf<SectionData>().also { sections ->
			selectivePayList.forEach { item ->
				sections.add(
					SectionData(
						title = (item as JSONObject).getString("itemName"), rows = extractValue(
							context, item, intArrayOf(
								R.string.student_id,
								R.string.fee_interval,
								R.string.current_due,
								R.string.this_payment
							), arrayOf("personCode", "intervalName", "nowMoney", "needMoney")
						)
					)
				)
			}
		}
	}
	val feeSections = remember(feeList) {
		mutableStateListOf<SectionData>().also { sections ->
			feeList.forEach { item ->
				sections.add(
					SectionData(
						title = (item as JSONObject).getString("itemName"), rows = extractValue(
							context, item, intArrayOf(
								R.string.student_id,
								R.string.fee_item,
								R.string.fee_interval,
								R.string.should_pay,
								R.string.deferred_pay,
								R.string.actual_pay
							), arrayOf(
								"personCode",
								"itemName",
								"intervalName",
								"needPay",
								"laterPay",
								"realPay"
							)
						)
					)
				)
			}
		}
	}
	val viewDetail = stringResource(R.string.view_detail)
	val export = stringResource(R.string.export)
	val paymentDetail by viewModel.paymentDetail.observeAsState(null)
	var paymentDetailIndex by remember { mutableIntStateOf(-1) }
	val paymentSections = remember(paymentList) {
		mutableStateListOf<SectionData>().also { sections ->
			paymentList.forEachIndexed { index, item ->
				sections.add(
					SectionData(
						title = "${index + 1}", rows = extractValue(
							context, (item as JSONObject), intArrayOf(
								R.string.order_number,
								R.string.amount_yuan,
								R.string.pay_method,
								R.string.pay_time,
								R.string.pay_number
							), arrayOf("orderNo", "money", "payTypeName", "payTime", "outPayNo")
						), footerMenus = mutableStateListOf(MenuItem(viewDetail) {
							viewModel.viewDetail(
								item.getString("orderNo"), item.getString("outPayNo")
							)
							paymentDetailIndex = index
							true
						})
					)
				)
			}
		}
	}

	LaunchedEffect(paymentDetail) {
		paymentDetail?.let { detail ->
			val orderNo = detail.getString("orderNo")
			val targetIndex = paymentDetailIndex.takeIf { it in paymentSections.indices }
				?: paymentList.indexOfFirst { (it as JSONObject).getString("orderNo") == orderNo }
			if (targetIndex in paymentSections.indices) {
				val section = paymentSections[targetIndex]
				section.rows.clear()
				section.rows.addAll(
					extractValue(
						context, detail, intArrayOf(
							R.string.order_number,
							R.string.amount_yuan,
							R.string.pay_method,
							R.string.pay_time,
							R.string.create_time,
							R.string.pay_status,
							R.string.invoice_title,
						), arrayOf(
							"orderNo",
							"money",
							"payType",
							"payTime",
							"createTime",
							"stateStr",
							"ticketTitle"
						)
					)
				)
				detail.getJSONArray("details")?.forEach { detail ->
					section.rows.add(
						RowData(
							(detail as JSONObject).getString("itemName"),
							"¥${detail.getString("payMoney")}"
						)
					)
				}
				section.footerMenus.clear()
				section.footerMenus.add(MenuItem(export) {
					DownloadManager.downloadFile(
						context, viewModel.model.http.generateRequest(
							"https://${viewModel.model.host}/client/api/client/record/exportPdf",
							"{\"downloadToken\":\"${detail.getString("downloadToken")}\"}",
							null
						).build(), "${
							Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
						}/支付凭据.pdf"
					)
					true
				})
			}
		}
	}
	val refundSections = remember(refundList) {
		mutableStateListOf<SectionData>().also { sections ->
			refundList.forEachIndexed { index, item ->
				sections.add(
					SectionData(
						title = "${index + 1}", rows = extractValue(
							context, (item as JSONObject), intArrayOf(
								R.string.fee_item,
								R.string.charge_interval,
								R.string.refund_amount,
								R.string.refund_date,
								R.string.refund_status
							), arrayOf(
								"itemName",
								"intervalName",
								"refundMoney",
								"refundDate",
								"refundStateStr"
							)
						)
					)
				)
			}
		}
	}

	var currentPage by remember { mutableIntStateOf(0) }
	var yearExpanded by remember { mutableStateOf(false) }
	val maxMenuHeight = LocalWindowInfo.current.containerDpSize.height * 0.5f
	var selectedYear by remember { mutableStateOf(calendarManager.year.toString()) }
	var ticketRemark by remember { mutableStateOf("") }
	var ticketTitle by remember { mutableStateOf("") }
	val all = stringResource(R.string.all)
	val noIntervalYear = stringResource(R.string.no_interval_year)
	val yearItems = remember {
		listOf(
			all, noIntervalYear
		) + (0..5).map { (calendarManager.year + 1 - it).toString() }
	}
	val yearCodes = remember {
		listOf("null", "-1") + (0..5).map { (calendarManager.year + 1 - it).toString() }
	}
	var showFromPicker by remember { mutableStateOf(false) }
	var showToPicker by remember { mutableStateOf(false) }
	var showPayDialog by remember { mutableStateOf(false) }
	val selectedPayItems = remember { mutableStateListOf<Int>() }
	var fromDate by remember { mutableStateOf(calendarManager.firstOfMonth) }
	var toDate by remember { mutableStateOf(calendarManager.endOfMonth) }
	val allSections =
		listOf(toPaySections, selectivePaySections, feeSections, paymentSections, refundSections)
	val tabs = listOf(
		MenuItem(stringResource(R.string.pay_to_pay)),
		MenuItem(stringResource(R.string.pay_selective)),
		MenuItem(stringResource(R.string.pay_fee_situation)),
		MenuItem(stringResource(R.string.pay_record)),
		MenuItem(stringResource(R.string.pay_refund)),
	)

	ActivityPager(
		title = stringResource(R.string.pay_fee),
		tabs = tabs,
		topBarMenus = { page ->
			mutableListOf(
				exportMarkdownMenuItem(
					backStack, allSections, tabs, stringResource(R.string.pay_fee)
				)
			).also {
				if (page == 0) {
					it.add(
						MenuItem(
						stringResource(R.string.pay_record), icon = Icons.Rounded.AttachMoney
					) {
						selectedPayItems.clear()
						showPayDialog = true
						true
					})
				}
			}
		},
		onNavigationClick = { backStack.navigateBack() },
		topBarContent = {
			when (it) {
				2 -> {
					ExposedDropdownMenuBox(
						expanded = yearExpanded,
						onExpandedChange = { it1 -> yearExpanded = it1 }) {
						OutlinedTextField(
							modifier = Modifier
								.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
								.padding(
									dimensionResource(R.dimen.horizontal_margin),
									dimensionResource(R.dimen.vertical_margin)
								)
								.fillMaxWidth(),
							value = selectedYear,
							onValueChange = {},
							readOnly = true,
							singleLine = true,
							label = { Text(stringResource(R.string.year)) },
							trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = yearExpanded) },
							colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
						)
						ExposedDropdownMenu(
							expanded = yearExpanded,
							onDismissRequest = { yearExpanded = false },
							modifier = Modifier
								.fillMaxWidth()
								.heightIn(max = maxMenuHeight)
						) {
							yearItems.forEachIndexed { index, option ->
								val isSelected = option == selectedYear
								DropdownMenuItem(
									text = { Text(option) },
									onClick = {
										selectedYear = option
										yearExpanded = false
										viewModel.fetchFeeList(yearCodes[index])
									},
									contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
									modifier = Modifier.background(if (isSelected) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent),
								)
							}
						}
					}
				}

				3 -> {
					Row(
						verticalAlignment = Alignment.CenterVertically,
						horizontalArrangement = Arrangement.Center,
						modifier = Modifier
							.fillMaxWidth()
							.padding(
								dimensionResource(R.dimen.horizontal_margin),
								dimensionResource(R.dimen.vertical_margin)
							)
					) {
						OutlinedButton(
							onClick = { showFromPicker = true }, shape = ButtonDefaults.shape
						) {
							Text(fromDate?.format(DateTimeFormatter.ISO_LOCAL_DATE) ?: "")
						}
						Text(" ~ ")
						OutlinedButton(
							onClick = { showToPicker = true }, shape = ButtonDefaults.shape
						) {
							Text(toDate?.format(DateTimeFormatter.ISO_LOCAL_DATE) ?: "")
						}
					}
				}
			}
		},
		onPageChange = { currentPage = it },
		sharedTransitionScope = sharedTransitionScope,
		animatedVisibilityScope = animatedVisibilityScope,
		sharedKey = "Pay",
		isNestedScrollEnabled = false
	) { page ->
		allSections.getOrNull(page)?.let { sections ->
			StaggerScreen(sections = sections)
		}
	}

	if (showFromPicker) {
		val fromState = rememberDatePickerState(
			initialSelectedDateMillis = fromDate?.atTime(LocalTime.NOON)
				?.atZone(ZoneId.systemDefault())?.toInstant()?.toEpochMilli()
		)
		DatePickerDialog(onDismissRequest = { showFromPicker = false }, confirmButton = {
			TextButton(onClick = {
				fromDate = fromState.selectedDateMillis?.let {
					Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
				}
				showFromPicker = false
				fromDate?.let { from ->
					toDate?.let { to ->
						viewModel.fetchPaymentList(
							dateTimeFormatter.format(from.atStartOfDay()),
							dateTimeFormatter.format(to.atStartOfDay())
						)
					}
				}
			}, shape = ButtonDefaults.shape) { Text(stringResource(R.string.confirm)) }
		}, dismissButton = {
			TextButton(onClick = { showFromPicker = false }, shape = ButtonDefaults.shape) {
				Text(
					stringResource(R.string.cancel)
				)
			}
		}) { DatePicker(state = fromState) }
	}

	if (showToPicker) {
		val toState = rememberDatePickerState(
			initialSelectedDateMillis = toDate?.atTime(LocalTime.NOON)
				?.atZone(ZoneId.systemDefault())?.toInstant()?.toEpochMilli()
		)
		DatePickerDialog(onDismissRequest = { showToPicker = false }, confirmButton = {
			TextButton(onClick = {
				toDate = toState.selectedDateMillis?.let {
					Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
				}
				showToPicker = false
				fromDate?.let { from ->
					toDate?.let { to ->
						viewModel.fetchPaymentList(
							dateTimeFormatter.format(from.atStartOfDay()),
							dateTimeFormatter.format(to.atStartOfDay())
						)
					}
				}
			}, shape = ButtonDefaults.shape) { Text(stringResource(R.string.confirm)) }
		}, dismissButton = {
			TextButton(onClick = { showToPicker = false }, shape = ButtonDefaults.shape) {
				Text(
					stringResource(R.string.cancel)
				)
			}
		}) { DatePicker(state = toState) }
	}

	if (showPayDialog) {
		AlertDialog(
			onDismissRequest = { showPayDialog = false },
			title = { Text(stringResource(R.string.select_pay_items)) },
			text = {
				Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
					toPayList.forEachIndexed { index, item ->
						val itemName = (item as JSONObject).getString("itemName")
						val needMoney = item.getString("needMoney")
						Row(
							verticalAlignment = Alignment.CenterVertically,
							modifier = Modifier
								.fillMaxWidth()
								.clip(RoundedCornerShape(dimensionResource(R.dimen.content_padding)))
								.clickable(
									interactionSource = remember { MutableInteractionSource() },
									indication = ripple(bounded = true)
								) {
									if (selectedPayItems.contains(index)) selectedPayItems.remove(
										index
									) else selectedPayItems.add(index)
								}
								.padding(vertical = dimensionResource(R.dimen.vertical_margin))) {
							Checkbox(checked = selectedPayItems.contains(index), onCheckedChange = {
								if (it) selectedPayItems.add(index) else selectedPayItems.remove(
									index
								)
							})
							Column(modifier = Modifier.weight(1f)) {
								Text(itemName ?: "", style = MaterialTheme.typography.titleMedium)
								if (!needMoney.isNullOrEmpty()) {
									Text(
										"¥$needMoney",
										style = MaterialTheme.typography.bodyMedium,
										color = MaterialTheme.colorScheme.onSurfaceVariant
									)
								}
							}
						}
					}
					if (selectedPayItems.isNotEmpty()) {
						OutlinedTextField(
							value = ticketRemark,
							onValueChange = { ticketRemark = it },
							label = { Text(stringResource(R.string.ticket_remark)) })
						OutlinedTextField(
							value = ticketTitle,
							onValueChange = { ticketTitle = it },
							label = { Text(stringResource(R.string.ticket_title)) },
							supportingText = { Text(stringResource(R.string.ticket_title_tip)) })
					}
				}
			},
			confirmButton = {
				val total = selectedPayItems.sumOf {
					toPayList.getJSONObject(it).getFloatValue("needMoney").toInt()
				}
				Row(
					verticalAlignment = Alignment.CenterVertically,
					horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.horizontal_margin))
				) {
					if (selectedPayItems.isNotEmpty()) {
						Text(
							"${stringResource(R.string.total)}: ¥${"%d".format(total)}",
							color = MaterialTheme.colorScheme.primary
						)
					}
					TextButton(onClick = {
						showPayDialog = false
						if (selectedPayItems.isNotEmpty()) {
							val pendFees = JSONArray()
							val data = JSONObject.of(
								"allMoney",
								total,
								"pendFees",
								pendFees,
								"enableUseCompanyTitle",
								false
							)
							selectedPayItems.forEach {
								pendFees.add(
									JSONObject.of(
										"arrearId",
										toPayList.getJSONObject(it).getString("arrearId"),
										"nowMoney",
										toPayList.getJSONObject(it).getString("nowMoney"),
										"itemId",
										toPayList.getJSONObject(it).getString("itemId"),
										"itemName",
										toPayList.getJSONObject(it).getString("itemName"),
										"intervalId",
										toPayList.getJSONObject(it).getString("intervalId"),
										"intervalName",
										toPayList.getJSONObject(it).getString("intervalName"),
									)
								)
							}
							if (ticketTitle.isNotEmpty()) viewModel.toPayItems.fluentPut(
								"ticketTitle", ticketTitle
							)
							viewModel.toPayItems.fluentPut("remark", ticketRemark)
							viewModel.check(data)
						}
					}, shape = ButtonDefaults.shape) {
						Text(stringResource(R.string.confirm))
					}
				}
			},
			dismissButton = {
				TextButton(onClick = { showPayDialog = false }, shape = ButtonDefaults.shape) {
					Text(stringResource(R.string.cancel))
				}
			})
	}

	if (pendingPay.isNotEmpty()) {
		AlertDialog(
			onDismissRequest = { viewModel.clearPendingPay() },
			title = { Text(stringResource(R.string.pending_pay_order)) },
			text = {
				Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
					pendingPay.forEachIndexed { index, orderNo ->
						Row(
							verticalAlignment = Alignment.CenterVertically,
							horizontalArrangement = Arrangement.SpaceBetween,
							modifier = Modifier
								.fillMaxWidth()
								.padding(vertical = dimensionResource(R.dimen.vertical_margin))
						) {
							Text(
								"${index + 1}. $orderNo",
								modifier = Modifier.weight(1f),
								style = MaterialTheme.typography.bodyMedium
							)
							TextButton(onClick = {
								viewModel.openWechat("https://fee.sysu.edu.cn/gateway/cashier/order?orderno=$orderNo&scene=web")
							}, shape = ButtonDefaults.shape) {
								Text(stringResource(R.string.pay))
							}
							TextButton(onClick = {
								viewModel.cancel(orderNo)
								viewModel.clearPendingPay()
							}, shape = ButtonDefaults.shape) {
								Text(stringResource(R.string.cancel_pay))
							}
						}
					}
				}
			},
			confirmButton = {
				TextButton(onClick = {
					viewModel.clearPendingPay()
				}, shape = ButtonDefaults.shape) {
					Text(stringResource(R.string.close))
				}
			})
	}
}