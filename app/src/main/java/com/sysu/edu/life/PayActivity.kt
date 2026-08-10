package com.sysu.edu.life

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
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
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModelProvider
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
			val toPayList by viewModel.toPayList.observeAsState(emptyList())
			val selectivePayList by viewModel.selectivePayList.observeAsState(emptyList())
			val feeList by viewModel.feeList.observeAsState(emptyList())
			val paymentList by viewModel.paymentList.observeAsState(emptyList())
			val refundList by viewModel.refundList.observeAsState(emptyList())
			
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
						sections.add(SectionData(title = item.getString("itemName"),
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
						sections.add(SectionData(title = item.getString("itemName"),
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
						sections.add(SectionData(title = item.getString("itemName"),
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
						                                             item,
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
						                                             item,
						                                             intArrayOf(R.string.fee_item, R.string.charge_interval, R.string.refund_amount, R.string.refund_date, R.string.refund_status),
						                                             arrayOf("itemName", "intervalName", "refundMoney", "refundDate", "refundStateStr"))))
					}
				}
			}
			var currentPage by remember { mutableIntStateOf(0) }
			var yearExpanded by remember { mutableStateOf(false) }
			var selectedYear by remember { mutableStateOf(calendarManager.year.toString()) }
			val yearItems = remember {
				listOf(getString(R.string.all), getString(R.string.no_interval_year)) + (0..5).map { (calendarManager.year + 1 - it).toString() }
			}
			val yearCodes = remember {
				listOf("null", "-1") + (0..5).map { (calendarManager.year + 1 - it).toString() }
			}
			var showFromPicker by remember { mutableStateOf(false) }
			var showToPicker by remember { mutableStateOf(false) }
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
						              config.browse("https://pay.sysu.edu.cn/#/confirm/pay-ticket?type=1")
					              }) {
						              Icon(painter = painterResource(R.drawable.money), contentDescription = "pay")
					              }
				              }
				              IconButton(onClick = {
					              val markdown = allSections.joinToString("\n\n") { it.toMarkdown() }
					              DataStoreManager.saveContent(this@PayActivity, getString(R.string.pay_fee), markdown) {
						              startActivity(android.content.Intent(this@PayActivity, com.sysu.edu.browser.RichTextActivity::class.java)
							                            .putExtra("type", DataStoreManager.ContentType.MARKDOWN.name)
							                            .putExtra("title", getString(R.string.pay_fee)))
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
								ExposedDropdownMenu(expanded = yearExpanded, onDismissRequest = { yearExpanded = false }) {
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
		}
	}
}