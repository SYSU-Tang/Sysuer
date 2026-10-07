package com.miyuyan.sysuer.life

import android.app.Application
import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.alibaba.fastjson2.JSONArray
import com.alibaba.fastjson2.JSONObject
import com.haibin.calendarview.SysuerCalendar
import com.miyuyan.sysuer.R
import com.miyuyan.sysuer.model.ZhnyModel
import com.miyuyan.sysuer.view.UiState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import okhttp3.Call
import okhttp3.Callback
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Response
import java.io.IOException
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** 单行键值信息（对应原 PreferenceAdapter 的 title/content/icon） */
data class InfoRow(val icon: Int, val key: String, val value: String?)

/** 用量统计：上月 / 本月 / 变化量 */
data class UsageStats(val lastMonth: Double, val thisMonth: Double, val delta: Double)

/** 账单明细行组：标题 + 若干键值行 */
data class TradeDetail(val title: String, val rows: List<InfoRow>)

/** 按日期分组的缴费记录 */
data class DayRecord(val date: String, val details: List<TradeDetail>)

/** 水费账单 */
data class WaterBill(
	val id: String, val duration: String, val rows: List<InfoRow>,
	val canPay: Boolean, val amount: Float,
)

/** 电费账单 */
data class ElectricityBill(
	val id: String, val title: String, val rows: List<InfoRow>,
	val canPay: Boolean, val amount: Float,
)

/** 从 JSONObject 按 (字符串资源, 图标, 字段) 构建键值行。
 *  保留空值以维持与 spec 一一对应的下标（调用方可能按下标替换行），
 *  渲染时由 UI 过滤空值（对应原 PreferenceAdapter 的 hideNull）。 */
internal fun buildInfoRows(
	context: Context, item: JSONObject, spec: List<Triple<Int, Int, String>>
): List<InfoRow> = spec.map { (nameRes, icon, key) ->
	InfoRow(icon, context.getString(nameRes), item.getString(key))
}

/** UI → ViewModel 的交互事件，经 SharedFlow 单向传递 */
sealed interface EnergyEvent {
	data class SelectWaterRoom(val roomCode: String?) : EnergyEvent
	data class WaterMonthChange(val year: Int, val month: Int) : EnergyEvent
	data class WaterBillDetail(val billId: String) : EnergyEvent
	data class PayWaterBill(val bill: WaterBill) : EnergyEvent
	data object DismissWaterDetail : EnergyEvent

	data class SelectElectricityRoom(val roomCode: String?) : EnergyEvent
	data class ElectricityMonthChange(val year: Int, val month: Int) : EnergyEvent
	data class ElectricityBillDetail(val billId: String) : EnergyEvent
	data class PayElectricityBill(val bill: ElectricityBill) : EnergyEvent
	data object DismissElectricityDetail : EnergyEvent

	/** 金额单位为分 */
	data class Recharge(val amountFen: Int, val remark: String) : EnergyEvent
}

/**
 * 综合能源平台（智慧能源）：单一 ViewModel 承载仪表盘 / 水费 / 电费 / 账户四页。
 * 各页共用一个 [ZhnyModel] 与一次用户信息/房间列表请求，请求码全局唯一；
 * 水费与电费页各自维护所选房间与日历月份。
 */
class EnergyViewModel(application: Application) : AndroidViewModel(application){
	private val model = ZhnyModel(application)
	val uiState = model.getUiState(USER_INFO)

	// 仪表盘
	val electricityStats = MutableStateFlow<UsageStats?>(null)
	val waterStats = MutableStateFlow<UsageStats?>(null)
	val records = mutableStateListOf<DayRecord>()

	// 房间：名称 to 房间编码（水费、电费页各自选择）
	val rooms = mutableStateListOf<Pair<String, String?>>()
	val waterRoomCode = MutableStateFlow<String?>(null)
	val waterRoomName = MutableStateFlow<String?>(null)
	val electricityRoomCode = MutableStateFlow<String?>(null)
	val electricityRoomName = MutableStateFlow<String?>(null)

	// 水费
	val waterSchemes = MutableStateFlow<Map<String, SysuerCalendar>>(emptyMap())
	val waterBills = mutableStateListOf<WaterBill>()

	/** 非空时弹出账单详情底部弹层 */
	val waterDetail = MutableStateFlow<List<InfoRow>?>(null)
	val waterMonthText = MutableStateFlow(LocalDate.now().format(MONTH_FORMATTER))

	// 电费
	val electricitySchemes = MutableStateFlow<Map<String, SysuerCalendar>>(emptyMap())
	val electricityBills = mutableStateListOf<ElectricityBill>()
	val electricityDetail = MutableStateFlow<List<InfoRow>?>(null)
	val electricityMonthText = MutableStateFlow(LocalDate.now().format(MONTH_FORMATTER))

	// 账户
	val userRows = MutableStateFlow<List<InfoRow>>(emptyList())
	val dormRows = MutableStateFlow<List<InfoRow>>(emptyList())
	val balance = MutableStateFlow<String?>(null)

	/** 充值弹层可见性 */
	val showRecharge = MutableStateFlow(false)

	/** 支付网关重定向地址，由 UI 层复制并分享 */
	val wechatLink = MutableSharedFlow<String>(extraBufferCapacity = 16)

	private val _snackbarMessage = MutableSharedFlow<String>(extraBufferCapacity = 16)
	val snackbarMessage: SharedFlow<String> = _snackbarMessage.asSharedFlow()

	private val _events = MutableSharedFlow<EnergyEvent>(extraBufferCapacity = 16)

	/** UI 层通过 [dispatch] 发送交互事件 */
	fun dispatch(event: EnergyEvent) {
		_events.tryEmit(event)
	}

	private val paymentStatuses = application.resources.getStringArray(R.array.payment_status)

	init {
		viewModelScope.launch {
			_events.collect { event ->
				when (event) {
					is EnergyEvent.SelectWaterRoom -> {
						rooms.firstOrNull { it.second == event.roomCode }?.let { (name, code) ->
							waterRoomName.value = name
							waterRoomCode.value = code
							getWaterMonthUsage(code)
							getWaterBillList(code)
						}
					}

					is EnergyEvent.WaterMonthChange -> {
						waterMonthText.value =
							LocalDate.of(event.year, event.month, 1).format(MONTH_FORMATTER)
						waterRoomCode.value?.let { getWaterMonthUsage(it) }
					}

					is EnergyEvent.WaterBillDetail -> getWaterBillDetail(event.billId)

					is EnergyEvent.PayWaterBill -> payWaterBill(event.bill)

					EnergyEvent.DismissWaterDetail -> waterDetail.value = null

					is EnergyEvent.SelectElectricityRoom -> {
						rooms.firstOrNull { it.second == event.roomCode }?.let { (name, code) ->
							electricityRoomName.value = name
							electricityRoomCode.value = code
							val (year, month) = parseMonth(electricityMonthText.value)
							val date = LocalDate.of(year, month, 1)
							getElectricityDayConsume(
									code,
									date.withDayOfMonth(1).format(DAY_FORMATTER),
									date.withDayOfMonth(date.lengthOfMonth()).format(DAY_FORMATTER)
							)
							getElectricityBillList(code)
						}
					}

					is EnergyEvent.ElectricityMonthChange -> {
						electricityMonthText.value =
							LocalDate.of(event.year, event.month, 1).format(MONTH_FORMATTER)
						electricityRoomCode.value?.takeUnless { it.isEmpty() }?.let { room ->
							val date = LocalDate.of(event.year, event.month, 1)
							getElectricityDayConsume(
									room,
									date.withDayOfMonth(1).format(DAY_FORMATTER),
									date.withDayOfMonth(date.lengthOfMonth()).format(DAY_FORMATTER)
							)
						}
					}

					is EnergyEvent.ElectricityBillDetail -> getElectricityBillDetail(event.billId)

					is EnergyEvent.PayElectricityBill -> payElectricityBill(event.bill)

					EnergyEvent.DismissElectricityDetail -> electricityDetail.value = null

					is EnergyEvent.Recharge -> rechargeAccount(event.amountFen, event.remark)
				}
			}
		}
		viewModelScope.launch {
			model.message.collect { (code, response) ->
				val context = getApplication<Application>()
				val data = response.get("data")
				when (code) {
					USER_INFO -> {
						val username = (data as JSONObject).getString("username")
						userRows.value = buildInfoRows(
								context, data, listOf(
								Triple(R.string.name, R.drawable.account, "name"),
								Triple(R.string.student_id, R.drawable.id, "username"),
						)
						)
						getWaterUsageStats()
						getElectricitySituation(username)
						getRooms(username)
					}

					ROOMS -> {
						rooms.clear()
						val dorms = mutableListOf<InfoRow>()
						(data as JSONArray).filterIsInstance<JSONObject>().forEach { roomInfo ->
							rooms.add(
									(roomInfo.getString("roomName")
										?: "") to roomInfo.getString("roomCode")
							)
							dorms += buildInfoRows(
									context, roomInfo, listOf(
									Triple(R.string.location, R.drawable.location, "areaInfo"),
									Triple(R.string.room_name, R.drawable.home, "roomName"),
							)
							)
						}
						dormRows.value = dorms
						rooms.firstOrNull()?.let { (name, code) ->
							waterRoomName.value = name
							waterRoomCode.value = code
							getWaterMonthUsage(code)
							getWaterBillList(code)
							electricityRoomName.value = name
							electricityRoomCode.value = code
							val (year, month) = parseMonth(electricityMonthText.value)
							val date = LocalDate.of(year, month, 1)
							getElectricityDayConsume(
									code,
									date.withDayOfMonth(1).format(DAY_FORMATTER),
									date.withDayOfMonth(date.lengthOfMonth()).format(DAY_FORMATTER)
							)
							getElectricityBillList(code)
							getBalance(code)
							getRoomBalanceRecords(
									code, LocalDate.now().format(MONTH_FORMATTER)
							)
						}
						uiState.value = if (rooms.isEmpty()) UiState.Empty else UiState.Content
					}

					WATER_USAGE_STATS -> (data as? JSONObject)?.let {
						waterStats.value = UsageStats(
								it.getDouble("lastMonthUsage"),
								it.getDouble("thisMonthUsage"),
								it.getDouble("growthUsage"),
						)
						markContent()
					}

					ELE_SITUATION -> (data as? JSONObject)?.let {
						electricityStats.value = UsageStats(
								it.getDouble("lastMonthUsage"),
								it.getDouble("currentMonthUsage"),
								it.getDouble("usageChange"),
						)
						markContent()
					}

					ROOM_BALANCE_DETAIL -> {
						records.clear()
						(data as JSONObject).getJSONArray("records").filterIsInstance<JSONObject>()
							.forEach { item ->
								val details = item.getJSONArray("detailRecords")
									.filterIsInstance<JSONObject>().map { detail ->
										TradeDetail(
												title = detail.getString("tradeTypeDesc") ?: "",
												rows = buildInfoRows(
														context, detail, listOf(
														Triple(
																R.string.type,
																R.drawable.menu,
																"tradeTypeDesc"
														),
														Triple(
																R.string.time,
																R.drawable.time,
																"tradeTime"
														),
														Triple(
																R.string.fee,
																R.drawable.money,
																"tradeAmount"
														),
														Triple(
																R.string.payer,
																R.drawable.account,
																"name"
														),
														Triple(
																R.string.student_id,
																R.drawable.id,
																"username"
														),
												)
												)
										)
									}
								records.add(DayRecord(item.getString("date") ?: "", details))
							}
						markContent()
					}

					WATER_MONTH_USAGE -> {
						val (year, month) = parseMonth(waterMonthText.value)
						waterSchemes.value = (data as JSONObject).getJSONArray("waterUsageList")
							.filterIsInstance<JSONObject>().map { item ->
								val usage = item.getString("totalWaterUsage")
									?: context.getString(R.string.no_data_available)
								SysuerCalendar().apply {
									this.year = year
									this.month = month
									day = item.getIntValue("timeLabel")
									scheme = usage
								}
							}.associateBy { it.toString() }
					}

					WATER_BILL_LIST -> {
						waterBills.clear()
						(data as JSONObject).getJSONArray("billList").filterIsInstance<JSONObject>()
							.forEach { item ->
								val paymentStatus = item.getInteger("paymentStatus")
								val rows = buildInfoRows(
										context, item, listOf(
										Triple(
												R.string.bill_period,
												R.drawable.calendar,
												"originalBillStartDate"
										),
										Triple(R.string.status, R.drawable.check, "paymentStatus"),
										Triple(R.string.type, R.drawable.water, "useWaterTypeName"),
										Triple(
												R.string.electricity_consumption,
												R.drawable.water,
												"finalWaterUsage"
										),
										Triple(R.string.fee, R.drawable.money, "waterPayment"),
										Triple(R.string.paid_fee, R.drawable.money, "paidPayment"),
								)
								).toMutableList()
								rows[0] = rows[0].copy(
										value = "${item.getString("originalBillStartDate")}~${
											item.getString("originalBillEndDate")
										}"
								)
								rows[1] = rows[1].copy(
										value = paymentStatuses.getOrNull(paymentStatus - 1)
											?: context.getString(R.string.none),
										icon = if (paymentStatus == 3 || paymentStatus == 5) R.drawable.check else R.drawable.uncheck
								)
								waterBills.add(
										WaterBill(
												id = item.getString("id") ?: "",
												duration = rows[0].value ?: "",
												rows = rows,
												canPay = paymentStatus == 1,
												amount = item.getFloat("waterPayment") ?: 0f,
										)
								)
							}
					}

					WATER_BILL_DETAIL -> {
						val item = data as JSONObject
						val billStatus = item.getInteger("paymentStatus")
						val rows = buildInfoRows(
								context, item, listOf(
								Triple(R.string.bill_period, R.drawable.calendar, "billStartDate"),
								Triple(R.string.status, R.drawable.check, "paymentStatus"),
								Triple(R.string.remark, R.drawable.text, "remark"),
								Triple(
										R.string.water_consumption,
										R.drawable.menu,
										"finalWaterUsage"
								),
								Triple(R.string.type, R.drawable.dashboard, "useWaterTypeName"),
								Triple(R.string.dorm, R.drawable.home, "areaInfo"),
								Triple(R.string.price, R.drawable.money, "unitPrice"),
								Triple(R.string.fee, R.drawable.money, "waterPayment"),
								Triple(R.string.paid_fee, R.drawable.money, "paidPayment"),
								Triple(R.string.unpaid_fee, R.drawable.money, "unpaidPayment"),
								Triple(R.string.pay_time, R.drawable.time, "createTime"),
						)
						).toMutableList()
						rows[0] = rows[0].copy(
								value = "${item.getString("billStartDate")}~${item.getString("billEndDate")}"
						)
						rows[1] = rows[1].copy(
								value = paymentStatuses.getOrNull(billStatus - 1)
									?: context.getString(R.string.none),
								icon = if (billStatus == 3 || billStatus == 5) R.drawable.check
								else R.drawable.uncheck
						)
						rows[3] = rows[3].copy(
								value = "${item.getString("currMeterReading")}-${
									item.getString("lastMeterReading")
								}=${item.getString("finalWaterUsage")}"
						)
						waterDetail.value = rows
					}

					WATER_PAY -> {
						_snackbarMessage.tryEmit(response.getString("msg") ?: "")
						waterRoomCode.value?.let { getWaterBillList(it) }
					}

					ELE_DAY_CONSUME -> {
						val schemes = mutableMapOf<String, SysuerCalendar>()
						(data as JSONObject).getJSONArray("useEleByDayList")
							.filterIsInstance<JSONObject>().forEach { item ->
								val usage = item.getString("useElectric")
									?: context.getString(R.string.no_data_available)
								val date = LocalDate.parse(item.getString("date"), DAY_FORMATTER)
								SysuerCalendar().apply {
									year = date.year
									month = date.monthValue
									day = date.dayOfMonth
									scheme = usage
								}.let { schemes[it.toString()] = it }
							}
						electricitySchemes.value = schemes
					}

					ELE_BILL_LIST -> {
						electricityBills.clear()
						(data as JSONObject).getJSONArray("list").filterIsInstance<JSONObject>()
							.forEach { item ->
								val billStatus = item.getInteger("billStatus")
								val rows = buildInfoRows(
										context, item, listOf(
										Triple(
												R.string.bill_period,
												R.drawable.calendar,
												"billPeriod"
										),
										Triple(R.string.status, R.drawable.check, "billStatus"),
										Triple(R.string.remark, R.drawable.text, "remark"),
										Triple(
												R.string.electricity_consumption,
												R.drawable.flash,
												"useElectric"
										),
										Triple(R.string.payer, R.drawable.account, "name"),
										Triple(R.string.campus, R.drawable.location, "campusName"),
										Triple(R.string.dorm, R.drawable.home, "areaInfo"),
										Triple(R.string.price, R.drawable.money, "unitPrice"),
										Triple(R.string.fee, R.drawable.money, "totalUseAmount"),
										Triple(
												R.string.paid_fee,
												R.drawable.money,
												"payedUseAmount"
										),
										Triple(R.string.pay_time, R.drawable.time, "billTime"),
								)
								).toMutableList()
								rows[1] = rows[1].copy(
										value = paymentStatuses.getOrNull(billStatus - 1)
											?: context.getString(R.string.none),
										icon = if (billStatus == 3 || billStatus == 5) R.drawable.check
										else R.drawable.uncheck
								)
								rows[3] = rows[3].copy(
										value = "${item.getString("currReportElectric")}-${
											item.getString("lastReportElectric")
										}=${item.getString("useElectric")}"
								)
								electricityBills.add(
										ElectricityBill(
												id = item.getString("id") ?: "",
												title = item.getString("billPeriod") ?: "",
												rows = rows,
												canPay = billStatus == 1,
												amount = item.getFloat("totalUseAmount") ?: 0f,
										)
								)
							}
					}

					ELE_BILL_DETAIL -> (data as JSONObject).getJSONArray("list").firstOrNull()
						?.let { item ->
							item as JSONObject
							val billStatus = item.getInteger("billStatus")
							val rows = buildInfoRows(
									context, item, listOf(
									Triple(R.string.bill_period, R.drawable.calendar, "billPeriod"),
									Triple(R.string.status, R.drawable.check, "billStatus"),
									Triple(R.string.remark, R.drawable.text, "remark"),
									Triple(
											R.string.electricity_consumption,
											R.drawable.flash,
											"useElectric"
									),
									Triple(R.string.payer, R.drawable.account, "name"),
									Triple(R.string.campus, R.drawable.location, "campusName"),
									Triple(R.string.dorm, R.drawable.home, "areaInfo"),
									Triple(R.string.price, R.drawable.money, "unitPrice"),
									Triple(R.string.fee, R.drawable.money, "totalUseAmount"),
									Triple(R.string.paid_fee, R.drawable.money, "payedUseAmount"),
									Triple(R.string.unpaid_fee, R.drawable.money, "useAmount"),
									Triple(R.string.pay_time, R.drawable.time, "billTime"),
							)
							).toMutableList()
							rows[1] = rows[1].copy(
									value = paymentStatuses.getOrNull(billStatus - 1)
										?: context.getString(R.string.none),
									icon = if (billStatus == 3 || billStatus == 5) R.drawable.check
									else R.drawable.uncheck
							)
							rows[3] = rows[3].copy(
									value = "${item.getString("currReportElectric")}-${
										item.getString("lastReportElectric")
									}=${item.getString("useElectric")}"
							)
							electricityDetail.value = rows
						}

					ELE_PAY -> {
						_snackbarMessage.tryEmit(response.getString("msg") ?: "")
						electricityRoomCode.value?.let { getElectricityBillList(it) }
					}

					ACCOUNT_BALANCE -> (data as? JSONObject)?.let {
						balance.value = it.getString("balance")
					}

					ACCOUNT_RECHARGE -> (data as? JSONObject)?.getJSONObject("data")?.let {
						gotoWechat(it)
					}
				}
			}
		}
	}

	private fun markContent() {
		if (uiState.value == UiState.Loading || uiState.value == UiState.Unstarted) {
			uiState.value = UiState.Content
		}
	}

	fun retry() {
		if (uiState.value == UiState.Loading) return
		uiState.value = UiState.Loading
		getUserInfo()
	}

	fun showRecharge() {
		showRecharge.value = true
	}

	fun dismissRecharge() {
		showRecharge.value = false
	}

	// ---- 私有请求 ----

	// ---- 网络请求 ----

	private fun getUserInfo() {
		model.enqueue("kbp/auth/userInfo", USER_INFO)
	}

	private fun getWaterUsageStats() {
		model.enqueue("kbp/cwbs/user/usage/stats", "", WATER_USAGE_STATS)
	}

	private fun getElectricitySituation(username: String?) {
		model.enqueue("kbp/ele/wechat/eleSituation?username=$username", ELE_SITUATION)
	}

	private fun getRoomBalanceRecords(room: String?, date: String) {
		model.enqueue(
				"kbp/record/roomBalance/detail",
				"{\"dateType\":\"month\",\"roomCode\":\"$room\",\"dateRange\":\"$date\",\"id\":null,\"tradeTime\":\"\"}",
				ROOM_BALANCE_DETAIL
		)
	}

	private fun getWaterBillDetail(billId: String) {
		model.enqueue("kbp/cwbs/mobile/room/bill/get/$billId", WATER_BILL_DETAIL)
	}

	private fun payWaterBill(bill: WaterBill) {
		val room = waterRoomCode.value ?: return
		model.enqueue(
				"kbp/cwbs/mobile/room/bill/pay",
				"{\"roomCode\":\"$room\",\"billAmount\":${bill.amount},\"idList\":[\"${bill.id}\"],\"isMobile\":true,\"rechargeChannel\":6,\"rechargeMethod\":16}",
				WATER_PAY
		)
	}

	private fun getElectricityBillDetail(billId: String) {
		model.enqueue(
				"kbp/ele/mobile/billRecord",
				JSONObject.of("id", billId, "roomCode", electricityRoomCode.value).toJSONString(),
				ELE_BILL_DETAIL
		)
	}

	private fun payElectricityBill(bill: ElectricityBill) {
		val room = electricityRoomCode.value ?: return
		model.enqueue(
				"kbp/ele/mobile/pay/bill/recharge",
				String.format(
						java.util.Locale.getDefault(),
						"{\"roomCode\":\"%s\",\"actualBillAmount\":%.2f,\"useTypeEleAndMoneyList\":[{\"billAmount\":%.2f,\"useEleType\":1,\"idList\":[\"%s\"]}],\"rechargeType\":16}",
						room, bill.amount, bill.amount, bill.id
				), ELE_PAY
		)
	}

	private fun rechargeAccount(amountFen: Int, remark: String) {
		if (amountFen <= 0) return
		model.enqueue(
				"kbp/pay/recharge/zdPay",
				"{\"payAmount\":$amountFen,\"body\":\"房间钱包充值\",\"rechargeChannel\":6,\"accountType\":7,\"rechargeType\":7,\"params\":{\"roomCode\":\"${
					rooms.firstOrNull()?.second
				}\"},\"remark\":\"$remark\"}",
				ACCOUNT_RECHARGE
		)
	}

	private fun getRooms(username: String?) {
		model.enqueue(
				"kbp/admin/sys/personRoom/list",
				JSONObject.of("username", username).toJSONString(),
				ROOMS
		)
	}

	private fun getWaterMonthUsage(room: String?) {
		model.enqueue(
				"kbp/cwbs/month/usage/stats",
				JSONObject.of("roomCode", room, "staticsMonth", waterMonthText.value)
					.toJSONString(),
				WATER_MONTH_USAGE
		)
	}

	private fun getWaterBillList(room: String?) {
		model.enqueue(
				"kbp/cwbs/mobile/room/bill/list",
				JSONObject.of("roomCode", room).toJSONString(),
				WATER_BILL_LIST
		)
	}

	private fun getElectricityDayConsume(roomCode: String?, startDate: String, endDate: String) {
		model.enqueue(
				"kbp/ele/wechat/eleConsume",
				JSONObject.of("roomCode", roomCode, "startDate", startDate, "endDate", endDate)
					.toJSONString(),
				ELE_DAY_CONSUME
		)
	}

	private fun getElectricityBillList(roomCode: String?) {
		model.enqueue(
				"kbp/ele/mobile/billRecord",
				JSONObject.of("roomCode", roomCode, "billType", 1).toJSONString(),
				ELE_BILL_LIST
		)
	}

	private fun getBalance(room: String?) {
		model.enqueue("kbp/pay/roomBalance?roomCode=$room", ACCOUNT_BALANCE)
	}

	/** 用支付参数请求支付网关，取重定向地址交由调用方复制/分享 */
	private fun gotoWechat(data: JSONObject) {
		val form = FormBody.Builder()
		data.forEach { (key, value) ->
			key?.let { form.add(it, "$value") }
		}
		val request = model.http.generateRequest(
				"https://fee.sysu.edu.cn/gateway/unifiedorder/pagepay", null, null
		).post(form.build()).header("Content-Type", "application/x-www-form-urlencoded").build()
		OkHttpClient.Builder().followRedirects(false).build().newCall(request)
			.enqueue(object : Callback {
				override fun onFailure(call: Call, e: IOException) {
				}

				override fun onResponse(call: Call, response: Response) {
					response.use {
						val location = it.header("Location")
						if (!location.isNullOrEmpty()) wechatLink.tryEmit(location)
					}
				}
			})
	}

	override fun onCleared() {
		model.dispose()
	}

	companion object {
		private const val USER_INFO = 0
		private const val ROOMS = 1
		private const val WATER_USAGE_STATS = 2
		private const val ELE_SITUATION = 3
		private const val ROOM_BALANCE_DETAIL = 4
		private const val WATER_MONTH_USAGE = 5
		private const val WATER_BILL_LIST = 6
		private const val WATER_BILL_DETAIL = 7
		private const val WATER_PAY = 8
		private const val ELE_DAY_CONSUME = 9
		private const val ELE_BILL_LIST = 10
		private const val ELE_BILL_DETAIL = 11
		private const val ELE_PAY = 12
		private const val ACCOUNT_BALANCE = 13
		private const val ACCOUNT_RECHARGE = 14

		private val MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM")
		private val DAY_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd")

		private fun parseMonth(text: String): Pair<Int, Int> =
			text.split("-").let { (y, m) -> y.toInt() to m.toInt() }
	}
}
