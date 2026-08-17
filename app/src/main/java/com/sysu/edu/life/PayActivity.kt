package com.sysu.edu.life

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.ripple
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import com.alibaba.fastjson2.JSONArray
import com.alibaba.fastjson2.JSONObject
import com.sysu.edu.BaseActivity
import com.sysu.edu.R
import com.sysu.edu.api.CalendarManager
import com.sysu.edu.api.CommonUtil.extractValue
import com.sysu.edu.api.DataStoreManager
import com.sysu.edu.view.ActivityPager
import com.sysu.edu.view.MenuItem
import com.sysu.edu.view.SectionData
import com.sysu.edu.view.StaggerScreen
import com.sysu.edu.view.toMarkdown
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class PayActivity : BaseActivity() {
	@OptIn(ExperimentalMaterial3Api::class) override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		enableEdgeToEdge()
		val viewModel = ViewModelProvider(this)[PayViewModel::class.java]
		val calendarManager = CalendarManager()
		val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
		
		setContent {
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
				viewModel.fetchPaymentList(calendarManager.toDateTimeString(calendarManager.firstOfMonth!!.atStartOfDay())!!, calendarManager.toDateTimeString(calendarManager.endOfMonth!!.atStartOfDay()))
				viewModel.fetchRefundList()
			}
			val toPaySections = remember(toPayList) {
				mutableStateListOf<SectionData>().also { sections ->
					toPayList.forEach { item ->
						sections.add(SectionData(title = (item as JSONObject).getString("itemName"),
						                         rows = extractValue(this@PayActivity,
						                                             item,
						                                             intArrayOf(R.string.student_id, R.string.fee_interval, R.string.current_due, R.string.this_payment),
						                                             arrayOf("personCode", "intervalName", "nowMoney", "needMoney"))))
					}
				}
			}
			val selectivePaySections = remember(selectivePayList) {
				mutableStateListOf<SectionData>().also { sections ->
					selectivePayList.forEach { item ->
						sections.add(SectionData(title = (item as JSONObject).getString("itemName"),
						                         rows = extractValue(this@PayActivity,
						                                             item,
						                                             intArrayOf(R.string.student_id, R.string.fee_interval, R.string.current_due, R.string.this_payment),
						                                             arrayOf("personCode", "intervalName", "nowMoney", "needMoney"))))
					}
				}
			}
			val feeSections = remember(feeList) {
				mutableStateListOf<SectionData>().also { sections ->
					feeList.forEach { item ->
						sections.add(SectionData(title = (item as JSONObject).getString("itemName"),
						                         rows = extractValue(this@PayActivity,
						                                             item,
						                                             intArrayOf(R.string.student_id, R.string.fee_item, R.string.fee_interval, R.string.should_pay, R.string.deferred_pay, R.string.actual_pay),
						                                             arrayOf("personCode", "itemName", "intervalName", "needPay", "laterPay", "realPay"))))
					}
				}
			}
			val paymentSections = remember(paymentList) {
				mutableStateListOf<SectionData>().also { sections ->
					paymentList.forEachIndexed { index, item ->
						sections.add(SectionData(title = "${index + 1}",
						                         rows = extractValue(this@PayActivity,
						                                             (item as JSONObject),
						                                             intArrayOf(R.string.order_number, R.string.amount_yuan, R.string.pay_method, R.string.pay_time, R.string.pay_number),
						                                             arrayOf("orderNo", "money", "payTypeName", "payTime", "outPayNo"))))
					}
				}
			}
			val refundSections = remember(refundList) {
				mutableStateListOf<SectionData>().also { sections ->
					refundList.forEachIndexed { index, item ->
						sections.add(SectionData(title = "${index + 1}",
						                         rows = extractValue(this@PayActivity,
						                                             (item as JSONObject),
						                                             intArrayOf(R.string.fee_item, R.string.charge_interval, R.string.refund_amount, R.string.refund_date, R.string.refund_status),
						                                             arrayOf("itemName", "intervalName", "refundMoney", "refundDate", "refundStateStr"))))
					}
				}
			}
			var currentPage by remember { mutableIntStateOf(0) }
			var yearExpanded by remember { mutableStateOf(false) }
			val maxMenuHeight = LocalConfiguration.current.screenHeightDp.dp * 0.5f
			var selectedYear by remember { mutableStateOf(calendarManager.year.toString()) }
			var ticketRemark by remember { mutableStateOf("") }
			var ticketTitle by remember { mutableStateOf("") }
			val yearItems = remember {
				listOf(getString(R.string.all), getString(R.string.no_interval_year)) + (0..5).map { (calendarManager.year + 1 - it).toString() }
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
			val allSections = listOf(toPaySections, selectivePaySections, feeSections, paymentSections, refundSections)
			
			ActivityPager(title = stringResource(R.string.pay_fee),
			              tabs = listOf(
				              MenuItem(stringResource(R.string.pay_to_pay)),
				              MenuItem(stringResource(R.string.pay_selective)),
				              MenuItem(stringResource(R.string.pay_fee_situation)),
				              MenuItem(stringResource(R.string.pay_record)),
				              MenuItem(stringResource(R.string.pay_refund)),
			                           ),
			              actions = {
				              if (currentPage == 0) {
					              IconButton(onClick = {
						              selectedPayItems.clear()
						              showPayDialog = true
					              }) {
						              Icon(painter = painterResource(R.drawable.money), contentDescription = stringResource(R.string.pay))
					              }
				              }
				              IconButton(onClick = {
					              val markdown = allSections.joinToString("\n\n") { it.toMarkdown() }
					              DataStoreManager.saveContent(this@PayActivity, getString(R.string.pay_fee), markdown) {
						              startActivity(Intent(this@PayActivity, com.sysu.edu.browser.RichTextActivity::class.java).putExtra("type", DataStoreManager.ContentType.MARKDOWN.name).putExtra("title", getString(R.string.pay_fee)))
					              }
				              }) {
					              Icon(painter = painterResource(R.drawable.export), contentDescription = stringResource(R.string.export))
				              }
			              },
			              onNavigationClick = { supportFinishAfterTransition() },
			              onPageChange = { currentPage = it }) { page ->
				Column(modifier = Modifier.fillMaxSize()) {
					when (page) {
						2 -> {
							ExposedDropdownMenuBox(expanded = yearExpanded, onExpandedChange = { yearExpanded = it }) {
								OutlinedTextField(
									modifier = Modifier
										.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
										.padding(dimensionResource(R.dimen.horizontal_margin), dimensionResource(R.dimen.vertical_margin))
										.fillMaxWidth(),
									value = selectedYear,
									onValueChange = {},
									readOnly = true,
									singleLine = true,
									label = { Text(stringResource(R.string.year)) },
									trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = yearExpanded) },
									colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
								                 )
								ExposedDropdownMenu(expanded = yearExpanded, onDismissRequest = { yearExpanded = false }, modifier = Modifier
									.fillMaxWidth()
									.heightIn(max = maxMenuHeight)) {
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
											modifier = Modifier.background(if (isSelected) androidx.compose.ui.graphics.Color.LightGray else androidx.compose.ui.graphics.Color.Transparent),
										                )
									}
								}
							}
						}
						3 -> {
							Row(verticalAlignment = Alignment.CenterVertically,
							    horizontalArrangement = Arrangement.Center,
							    modifier = Modifier
								    .fillMaxWidth()
								    .padding(dimensionResource(R.dimen.horizontal_margin), dimensionResource(R.dimen.vertical_margin))) {
								OutlinedButton(onClick = { showFromPicker = true }, shapes = ButtonDefaults.shapes()) {
									Text(fromDate?.format(DateTimeFormatter.ISO_LOCAL_DATE) ?: "")
								}
								Text(" ~ ")
								OutlinedButton(onClick = { showToPicker = true }, shapes = ButtonDefaults.shapes()) {
									Text(toDate?.format(DateTimeFormatter.ISO_LOCAL_DATE) ?: "")
								}
							}
						}
					}
					allSections.getOrNull(page)?.let { sections ->
						StaggerScreen(sections = sections)
					}
				}
			}
			
			if (showFromPicker) {
				val fromState = rememberDatePickerState(initialSelectedDateMillis = fromDate?.atTime(LocalTime.NOON)?.atZone(ZoneId.systemDefault())?.toInstant()?.toEpochMilli())
				DatePickerDialog(onDismissRequest = { showFromPicker = false }, confirmButton = {
					TextButton(onClick = {
						fromDate = fromState.selectedDateMillis?.let {
							Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
						}
						showFromPicker = false
						fromDate?.let { from ->
							toDate?.let { to ->
								viewModel.fetchPaymentList(dateTimeFormatter.format(from.atStartOfDay()), dateTimeFormatter.format(to.atStartOfDay()))
							}
						}
					}, shapes = ButtonDefaults.shapes()) { Text(getString(R.string.confirm)) }
				}, dismissButton = {
					TextButton(onClick = { showFromPicker = false }, shapes = ButtonDefaults.shapes()) { Text(getString(R.string.cancel)) }
				}) { DatePicker(state = fromState) }
			}
			
			if (showToPicker) {
				val toState = rememberDatePickerState(initialSelectedDateMillis = toDate?.atTime(LocalTime.NOON)?.atZone(ZoneId.systemDefault())?.toInstant()?.toEpochMilli())
				DatePickerDialog(onDismissRequest = { showToPicker = false }, confirmButton = {
					TextButton(onClick = {
						toDate = toState.selectedDateMillis?.let {
							Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
						}
						showToPicker = false
						fromDate?.let { from ->
							toDate?.let { to ->
								viewModel.fetchPaymentList(dateTimeFormatter.format(from.atStartOfDay()), dateTimeFormatter.format(to.atStartOfDay()))
							}
						}
					}, shapes = ButtonDefaults.shapes()) { Text(getString(R.string.confirm)) }
				}, dismissButton = {
					TextButton(onClick = { showToPicker = false }, shapes = ButtonDefaults.shapes()) { Text(getString(R.string.cancel)) }
				}) { DatePicker(state = toState) }
			}
			
			if (showPayDialog) {
				AlertDialog(onDismissRequest = { showPayDialog = false }, title = { Text(stringResource(R.string.select_pay_items)) }, text = {
					Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
						toPayList.forEachIndexed { index, item ->
							val itemName = (item as JSONObject).getString("itemName")
							val needMoney = item.getString("needMoney")
							Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier
								.fillMaxWidth()
								.clickable(interactionSource = remember { MutableInteractionSource() }, indication = ripple(bounded = true)) {
									if (selectedPayItems.contains(index)) selectedPayItems.remove(index) else selectedPayItems.add(index)
								}
								.padding(vertical = dimensionResource(R.dimen.vertical_margin))) {
								Checkbox(checked = selectedPayItems.contains(index), onCheckedChange = {
									if (it) selectedPayItems.add(index) else selectedPayItems.remove(index)
								})
								Column(modifier = Modifier.weight(1f)) {
									Text(itemName ?: "", style = MaterialTheme.typography.titleMedium)
									if (!needMoney.isNullOrEmpty()) {
										Text("${stringResource(R.string.this_payment)}: ¥$needMoney", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
									}
								}
							}
						}
						if (selectedPayItems.isNotEmpty()) {
							OutlinedTextField(value = ticketRemark, onValueChange = { ticketRemark = it }, label = { Text(stringResource(R.string.ticket_remark)) })
							OutlinedTextField(value = ticketTitle, onValueChange = { ticketTitle = it }, label = { Text(stringResource(R.string.ticket_title)) }, supportingText = { Text(stringResource(R.string.ticket_title_tip)) })
						}
					}
				}, confirmButton = {
					val total = selectedPayItems.sumOf { toPayList.getJSONObject(it).getFloatValue("needMoney").toInt() }
					Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.horizontal_margin))) {
						if (selectedPayItems.isNotEmpty()) {
							Text("${stringResource(R.string.total)}: ¥${"%d".format(total)}", color = MaterialTheme.colorScheme.primary)
						}
						TextButton(onClick = {
							showPayDialog = false
							if (selectedPayItems.isNotEmpty()) {
								val pendFees = JSONArray()
								val data = JSONObject.of("allMoney", total, "pendFees", pendFees, "enableUseCompanyTitle", false)
								selectedPayItems.forEach {
									pendFees.add(JSONObject.of(
										"arrearId", toPayList.getJSONObject(it).getString("arrearId"),
										"nowMoney", toPayList.getJSONObject(it).getString("nowMoney"),
										"itemId", toPayList.getJSONObject(it).getString("itemId"),
										"itemName", toPayList.getJSONObject(it).getString("itemName"),
										"intervalId", toPayList.getJSONObject(it).getString("intervalId"),
										"intervalName", toPayList.getJSONObject(it).getString("intervalName"),
									                          ))
								}
								if (ticketTitle.isNotEmpty()) viewModel.toPayItems.fluentPut("ticketTitle", ticketTitle)
								viewModel.toPayItems.fluentPut("remark", ticketRemark)
								viewModel.check(data)
							}
						}, shapes = ButtonDefaults.shapes()) {
							Text(stringResource(R.string.confirm))
						}
					}
				}, dismissButton = {
					TextButton(onClick = { showPayDialog = false }, shapes = ButtonDefaults.shapes()) {
						Text(stringResource(R.string.cancel))
					}
				})
			}
			
			if (pendingPay.isNotEmpty()) {
				AlertDialog(onDismissRequest = { viewModel.clearPendingPay() }, title = { Text(stringResource(R.string.pending_pay_order)) }, text = {
					Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
						pendingPay.forEachIndexed { index, orderNo ->
							Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier
								.fillMaxWidth()
								.padding(vertical = dimensionResource(R.dimen.vertical_margin))) {
								Text("${index + 1}. $orderNo", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
//								Row(horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.horizontal_margin))) {
									TextButton(onClick = {
										viewModel.openWechat("https://fee.sysu.edu.cn/gateway/cashier/order?orderno=$orderNo&scene=web")
									}, shapes = ButtonDefaults.shapes()) {
										Text(stringResource(R.string.pay))
									}
									TextButton(onClick = {
										viewModel.cancel(orderNo)
										viewModel.clearPendingPay()
									}, shapes = ButtonDefaults.shapes()) {
										Text(stringResource(R.string.cancel_pay))
									}
//								}
							}
						}
					}
				}, confirmButton = {
					TextButton(onClick = {
						viewModel.clearPendingPay()
					}, shapes = ButtonDefaults.shapes()) {
						Text(stringResource(R.string.close))
					}
				})
			}
		}
	}
}