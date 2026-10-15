package com.miyuyan.sysuer.life

import android.app.Application
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateSetOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.runtime.snapshots.SnapshotStateSet
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.alibaba.fastjson2.JSONArray
import com.alibaba.fastjson2.JSONObject
import com.miyuyan.sysuer.R
import com.miyuyan.sysuer.api.DateTimeManager
import com.miyuyan.sysuer.model.GymModel
import com.miyuyan.sysuer.view.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.UUID
import java.util.regex.Pattern

/**
 * 体育场馆预订页面的状态持有者。
 *
 * 原实现由 GymListFragment / GymDetailFragment / GymReservationFragment /
 * GymOrderFragment / GymAccountFragment 各自新建 [GymModel] 并各占用请求码；
 * 迁移到 Compose 后统一收敛到这里：请求码集中定义，UI 只消费其中的 State。
 */
class GymViewModel(application: Application) : AndroidViewModel(application) {

	private val model = GymModel(application)

	val host: String get() = model.host
	val cookie: String get() = model.cookie
	val authorization: String get() = model.authorization

	private object Code {
		const val SLOTS = 0
		const val FEE = 1
		const val SWIMMER = 2
		const val VENUE_TYPE = 3
		const val RESERVE = 4
		const val CAMPUS = 5
		const val VENUE = 6
		const val RESERVATIONS = 7
		const val ORDERS = 8
		const val CREDIT = 9
		const val SWIMMER_ACCOUNT = 10
	}

	private fun str(resId: Int): String = getApplication<Application>().getString(resId)

	// ============================================================================================
	// 场地：校区 → 场馆 → 时段详情
	// ============================================================================================

	/** 当前层级的列表：0 层为校区，1 层为场馆 */
	val fields: SnapshotStateList<JSONObject> = mutableStateListOf()

	private val _fieldState = MutableStateFlow(UiState.Unstarted)
	val fieldState: StateFlow<UiState> = _fieldState.asStateFlow()

	fun loadCampus() {
		if (_fieldState.value != UiState.Content) _fieldState.value = UiState.Loading
		model.enqueue("api/Campus/active", Code.CAMPUS)
	}

	fun loadVenues() {
		if (_fieldState.value != UiState.Content) _fieldState.value = UiState.Loading
		model.enqueue("api/venuetype/all", Code.VENUE)
	}

	// ============================================================================================
	// 详情：日期条 + 时段网格 + 签名下单
	// ============================================================================================

	/** 时段网格的扁平单元格，每 [columnCount] 个渲染成一行 */
	val slots: SnapshotStateList<JSONObject> = mutableStateListOf()

	/** 每行的单元格个数（1 列场地名 + N 列时段），0 表示尚无数据 */
	private val _columnCount = MutableStateFlow(0)
	val columnCount: StateFlow<Int> = _columnCount.asStateFlow()

	/** UserRole -> 费用模板 */
	val fees: SnapshotStateMap<String, JSONObject> = mutableStateMapOf()

	/** 已选中的时段单元格下标 */
	val selected: SnapshotStateSet<Int> = mutableStateSetOf()

	/** 日期下标 -> 当日可预约总数，用于日期卡片角标 */
	val availableCapacity: SnapshotStateMap<Int, Int> = mutableStateMapOf()

	private val _detailState = MutableStateFlow(UiState.Unstarted)
	val detailState: StateFlow<UiState> = _detailState.asStateFlow()

	/** 当前选中的日期下标（0 表示今天） */
	private val _position = MutableStateFlow(0)
	val position: StateFlow<Int> = _position.asStateFlow()

	private var venueTypeId: String = ""
	private var hash: String = ""
	private var userId: String? = null
	private var type: String? = null

	/** 已选项的价格与说明；未选或当前身份无学生价时返回 null */
	fun selectionSummary(): Pair<String, Int>? {
		if (selected.isEmpty()) return null
		val studentFee = fees["学生"] ?: return null
		val creditFee = studentFee.getInteger("CreditFee") * selected.size
		val text = selected.sorted().joinToString("+") { index ->
			val cell = slots.getOrNull(index)
			if (cell == null) "" else "${cell.getString("Venue")} ${cell.getString("Duration")}"
		}
		return "$text=${creditFee}元" to creditFee
	}

	fun toggleSelection(index: Int) {
		if (!selected.add(index)) selected.remove(index)
	}

	fun loadSlots(id: String, dateIndex: Int) {
		if (venueTypeId != id) {
			fees.clear()
			slots.clear()
			selected.clear()
			_columnCount.value = 0
			type = null
			userId = null
		}
		venueTypeId = id
		_position.value = dateIndex
		selected.clear()
		_detailState.value = UiState.Loading
		val date = dateAt(dateIndex)
//		println("api/venue/available-slots/range?venueTypeId=$id&start=$date&end=$date")
		model.enqueue("api/venue/available-slots/range?venueTypeId=$id&start=$date&end=$date", Code.SLOTS)
	}

	/** 提交当前选择；需在已选状态下调用 */
	fun submit() {
		val summary = selectionSummary() ?: return
		val items = JSONArray()
		selected.sorted().forEach { index ->
			slots.getOrNull(index)?.getJSONObject("VenueBooking")?.let { items.add(it) }
		}
		val venueName = slots.getOrNull(selected.first())
			?.getJSONObject("VenueBooking")?.getString("VenueName")
		reserve(items, venueName, summary.second)
	}

	private fun dateAt(index: Int): String =
		LocalDate.now().plusDays(index.toLong()).format(DateTimeFormatter.ofPattern("M-dd"))

	private fun getFee(id: String) {
		model.enqueue("api/venuetype/$id/feetemplates", Code.FEE)
	}

	private fun loadMe() {
		model.enqueue("api/swimmer/me", Code.SWIMMER)
	}

	private fun getType() {
		model.enqueue("api/venue/type/$venueTypeId", Code.VENUE_TYPE)
	}

	/**
	 * 生成 UUID
	 *
	 * @return 生成的 UUID
	 */
	private fun generateUUID(): String = UUID.randomUUID().toString()

	/**
	 * 生成 Token
	 *
	 * @param uuid 请求标识
	 * @param hash 时段信息摘要
	 * @return 生成的 Token
	 */
	private fun genToken(uuid: String, hash: String): String {
		val timestamp = LocalDateTime.now().atZone(ZoneId.systemDefault()).toEpochSecond()
		return md5("SYSU888BOOKING-$uuid$timestamp") + "." + timestamp + "." + hash
	}

	/**
	 * 计算 MD5 哈希值
	 *
	 * @param input 输入字符串
	 * @return 计算得到的 MD5 哈希值（十六进制小写字符串）
	 */
	private fun md5(input: String): String {
		try {
			val hexString = StringBuilder()
			MessageDigest.getInstance("MD5").digest(input.toByteArray(StandardCharsets.UTF_8))
				.forEach {
					hexString.append(Integer.toHexString(0xff and it.toInt()).padStart(2, '0'))
				}
			return "$hexString"
		} catch (e: NoSuchAlgorithmException) {
			throw RuntimeException("MD5 algorithm not available", e)
		}
	}

	private fun reserve(items: JSONArray, venueName: String?, creditFee: Int) {
		val uuid = generateUUID()
		val time =
			LocalDateTime.now().atZone(ZoneId.systemDefault()).withZoneSameInstant(ZoneOffset.UTC)
				.toString()
		val payload = JSONObject.of(
			"Identity", uuid,
			"BookingId", genToken(uuid, hash),
			"VenueTypeId", type,
			"VenueBookings", items,
			"Participants", JSONArray.of(),
			"Status", "Accepted",
			"Description", venueName,
			"CreatedAt", time,
			"UpdatedAt", time,
			"ActionedBy", userId,
			"IsCash", false,
			"Charge", creditFee
		)
		model.enqueue("api/BookingRequestVenue", payload.toJSONString(), Code.RESERVE)
	}

	// ============================================================================================
	// 我的预约
	// ============================================================================================

	val reservations: SnapshotStateList<JSONObject> = mutableStateListOf()

	private val _reservationState = MutableStateFlow(UiState.Unstarted)
	val reservationState: StateFlow<UiState> = _reservationState.asStateFlow()

	private val _reservationFromTo = MutableStateFlow(
		System.currentTimeMillis() to
				LocalDateTime.now().plusDays(7).atZone(ZoneId.systemDefault()).toInstant()
					.toEpochMilli()
	)
	val reservationFromTo: StateFlow<Pair<Long, Long>> = _reservationFromTo.asStateFlow()

	fun setReservationFromTo(from: Long, to: Long) {
		_reservationFromTo.value = from to to
	}

	fun fetchReservations(from: Long, to: Long) {
		_reservationState.value = UiState.Loading
		model.enqueue(
			"api/BookingRequestVenue?all=false&startDate=${DateTimeManager.toDateString(from)}" +
					"&endDate=${DateTimeManager.toDateString(to)}&waitingList=false", Code.RESERVATIONS
		)
	}

	/** 取消预约：成功后就地刷新当前区间 */
	fun deleteReservation(bookingId: String) {
		val request = model.http
			.generateRequest("https://${model.host}/api/BookingRequestVenue/$bookingId")
			.delete().build()
		model.call(request, object : Callback {
			override fun onFailure(call: Call, e: IOException) {
				model.contextUtil.toast(R.string.no_net_connected)
			}

			override fun onResponse(call: Call, response: Response) {
				response.use {
					if (it.isSuccessful) {
						val (from, to) = _reservationFromTo.value
						fetchReservations(from, to)
					}
				}
			}
		})
	}

	// ============================================================================================
	// 消费流水（分页）
	// ============================================================================================

	val orders: SnapshotStateList<JSONObject> = mutableStateListOf()

	private val _orderState = MutableStateFlow(UiState.Unstarted)
	val orderState: StateFlow<UiState> = _orderState.asStateFlow()

	private val _orderFromTo = MutableStateFlow(
		System.currentTimeMillis() to System.currentTimeMillis()
	)
	val orderFromTo: StateFlow<Pair<Long, Long>> = _orderFromTo.asStateFlow()

	private var total = -1
	private var page = 0

	val hasMoreOrder: Boolean get() = total > 0 && page * 10 < total

	fun setOrderFromTo(from: Long, to: Long) {
		_orderFromTo.value = from to to
		orders.clear()
		total = -1
		page = 0
		fetchOrders(from, to)
	}

	fun fetchOrders(from: Long, to: Long) {
		if (page == 0) _orderState.value = UiState.Loading
		model.enqueue(
			"api/transaction/Me?StartDate=${DateTimeManager.toDateString(from)}" +
					"&EndDate=${DateTimeManager.toDateString(to)}&Page=${++page}&PageSize=10", Code.ORDERS
		)
	}

	// ============================================================================================
	// 账户
	// ============================================================================================

	private val _account = MutableStateFlow<JSONObject?>(null)
	val account: StateFlow<JSONObject?> = _account.asStateFlow()

	val certificates: SnapshotStateList<JSONObject> = mutableStateListOf()

	private val _accountState = MutableStateFlow(UiState.Unstarted)
	val accountState: StateFlow<UiState> = _accountState.asStateFlow()

	fun loadAccount() {
		_accountState.value = UiState.Loading
		model.enqueue("api/Credit/Me", Code.CREDIT)
	}

	// ============================================================================================
	// 消息分发
	// ============================================================================================

	init {
		viewModelScope.launch {
			model.message.collect { (code, response) ->
				when (code) {
					Code.CAMPUS, Code.VENUE -> {
						fields.clear()
						response.getJSONArray("data")?.forEach { fields.add(it as JSONObject) }
						_fieldState.value =
							if (fields.isEmpty()) UiState.Empty else UiState.Content
					}

					Code.SLOTS -> handleSlots(response)

					Code.FEE -> {
						fees.clear()
						response.getJSONArray("data")?.forEach {
							fees[(it as JSONObject).getString("UserRole")] = it
						}
						loadMe()
					}

					Code.SWIMMER -> {
						val data = response.getJSONArray("data")
						userId = data?.takeUnless { it.isEmpty() }?.getJSONObject(0)
							?.getString("UserId")
						getType()
					}

					Code.VENUE_TYPE -> {
						type = response.getJSONArray("data")?.takeUnless { it.isEmpty() }
							?.getJSONObject(0)?.getString("TypeIdentity")
					}

					Code.RESERVE -> {
						if (response.getIntValue("Code") == 200) {
							model.toast(response.getString("data") ?: str(R.string.reserve_success))
						} else {
							model.toast(response.getString("Result") ?: str(R.string.submit_fail))
						}
					}

					Code.RESERVATIONS -> {
						reservations.clear()
						response.getJSONArray("data")?.forEach {
							reservations.add(it as JSONObject)
						}
						_reservationState.value =
							if (reservations.isEmpty()) UiState.Empty else UiState.Content
					}

					Code.ORDERS -> {
						response.getJSONArray("Transactions")?.forEach {
							orders.add(it as JSONObject)
						}
						total = response.getIntValue("TotalCount")
						_orderState.value =
							if (orders.isEmpty()) UiState.Empty
							else if (hasMoreOrder) UiState.LoadMore else UiState.Content
					}

					Code.CREDIT -> {
						_account.value = response
						_accountState.value = UiState.Content
						model.enqueue("api/swimmer/me", Code.SWIMMER_ACCOUNT)
					}

					Code.SWIMMER_ACCOUNT -> {
						certificates.clear()
						response.getJSONArray("data")?.forEach {
							certificates.add(it as JSONObject)
						}
					}
				}
			}
		}
	}

	/** 把「场地 × 时段」二维表铺成一维单元格，并记录可预约总数 */
	private fun handleSlots(response: JSONObject) {
		slots.clear()
		selected.clear()
		var capacity = 0
		var columns = -1
		println(response)
		response.getJSONArray("data").filterIsInstance<JSONObject>().forEach { item ->
			val timeslots = item.getJSONArray("Timeslots") ?: return@forEach
			if (columns == -1) {
				// 表头：首格为「时间」，其余为各时段起止
				slots.add(JSONObject.of("Name", str(R.string.time), "Type", 2))
				timeslots.forEach { slot ->
					val time = slot as JSONObject
					slots.add(
							JSONObject.of(
									"Name",
									"${time.getString("Start")}\n${time.getString("End")}",
									"Type",
									2
							)
					)
				}
				columns = timeslots.size + 1
			}
			val venueName = Pattern.compile("(.+)-").matcher(item.getString("VenueName")).replaceAll("")
			slots.add(JSONObject().fluentPut("VenueName", venueName).fluentPut("Type", 0))
			timeslots.forEach { slot ->
				val time = slot as JSONObject
				val venueBooking = item.clone()
				venueBooking.remove("Timeslots")
				venueBooking["TimeSlots"] = JSONArray.of(
						time.clone().fluentPut("Date", "${time.getString("Date")}T00:00:00.000Z").apply { remove("AvailableCapacity") })
				slots.add(
						time.clone().fluentPut("VenueBooking", venueBooking).fluentPut("Type", 1)
							.fluentPut("Venue", venueName).fluentPut("Duration", "${time.getString("Start")}~${time.getString("End")}")
				)
				time.getInteger("AvailableCapacity")?.let { capacity += it }
			}
			// 补齐整行，保证网格不错位
			if (columns > 0 && slots.size % columns != 0) {
				repeat(columns - slots.size % columns) { slots.add(JSONObject.of("Type", 3)) }
			}
		}
		hash = md5("$response")
		_columnCount.value = if (columns > 0) columns else 0
		availableCapacity[_position.value] = capacity
		_detailState.value = if (columns > 0) UiState.Content else UiState.Empty
		getFee(venueTypeId)
	}

	override fun onCleared() {
		model.dispose()
	}
}
