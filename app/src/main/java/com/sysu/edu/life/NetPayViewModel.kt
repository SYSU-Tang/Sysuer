package com.sysu.edu.life

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.application
import androidx.lifecycle.viewModelScope
import com.alibaba.fastjson2.JSONException
import com.alibaba.fastjson2.JSONObject
import com.sysu.edu.R
import com.sysu.edu.model.NetPayModel
import com.sysu.edu.view.MenuItem
import com.sysu.edu.view.RowData
import com.sysu.edu.view.SectionData
import kotlinx.coroutines.launch
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Response
import org.jsoup.Jsoup.parse
import java.io.IOException
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class NetPayViewModel(application: Application) : AndroidViewModel(application) {
	private val model = NetPayModel(application)
	private val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-M-d")
	val orderSections: SnapshotStateList<SectionData> = mutableStateListOf()
	val statusSections: SnapshotStateList<SectionData> = mutableStateListOf()
	var fee: Int by mutableIntStateOf(0)
		private set
	var timeIndex: Int by mutableIntStateOf(-1)
		private set
	var oldDateStr: String by mutableStateOf("")
		private set
	var serviceName: String by mutableStateOf("")
		private set
	var newOutDateStr: String by mutableStateOf("")
		private set
	var showPayDialog: Boolean by mutableStateOf(false)
		private set
	private var selectedServiceId by mutableStateOf<String?>(null)
	var snackbarMessage: String? by mutableStateOf(null)
		private set
	var snackbarActionLabel: String? by mutableStateOf(null)
		private set
	var snackbarAction: (() -> Unit)? by mutableStateOf(null)
		private set
	val timeOptions: List<String> = listOf(
		"15天", "1个月", "2个月", "3个月", "4个月", "5个月", "6个月",
		"7个月", "8个月", "9个月", "10个月", "11个月", "1年", "2年",
	)
	private val months = longArrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 24, 48)
	private val fees = intArrayOf(15, 30, 60, 90, 120, 150, 180, 210, 240, 270, 300, 330, 300, 600)
	private var oldDate: LocalDate? = null
	private val statusKeys = mutableListOf<String?>(
		"序号", "服务", "地址", "MAC地址", "部门", "使用者", "状态", "过期日期", "暂停日期",
	)
	private val orderKeys = mutableListOf<String?>(
		"订单号", "所有者", "金额", "支付方式", "订单时间", "订单状态", "服务", "代支付者",
	)
	private val status2Keys = mutableListOf<String?>(
		"序号",
		"服务",
		"校区（园）",
		"地址",
		"MAC地址",
		"部门",
		"使用者",
		"状态",
		"过期日期",
		"续费时长",
		"金额"
	)

	init {
		model.message.observeForever { (code, data) ->
			when (code) {
				0, 1, 6 -> {
					parse(data.getString("data")).selectFirst("tbody")?.select("tr")
						?.forEach { tr ->
							val row = mutableListOf<String?>()
							tr.select("td").forEach { td -> row.add(td.text()) }
							val section = when (code) {
								0 -> {
									getSection(row[4], orderKeys, row)
								}

								1 -> getSection(row[0], statusKeys, row)
								else -> getSection(row[0], status2Keys, row)
							}
							when (code) {
								0 -> {
									val payLink = tr.select("a").firstOrNull { it.text() == "支付" }
									if (payLink != null) {
										val orderId = row[0] ?: ""
										section.footerMenus.add(
											MenuItem(
												getString(R.string.pay_fee),
												null
											) {
												continuePay(orderId)
												true
											})
										section.footerMenus.add(
											MenuItem(
												getString(R.string.cancel_pay),
												null
											) {
												cancelPay(orderId)
												true
											})
									}
									orderSections.add(section)
								}

								1 -> {
									val actionLink = tr.select("a").firstOrNull {
										val txt = it.text()
										txt == "暂停" || txt == "恢复"
									}
									if (actionLink != null) {
										val onclick = actionLink.attr("onclick")
										val regex = """(.+?)\("(.*?)","?(.*?)"?.*\)""".toRegex()
										val match = regex.find(onclick)
										if (match != null) {
											val serviceId = match.groups[2]?.value ?: ""
											val leftDay = match.groups[3]?.value ?: ""
											val isStop = match.groups[1]?.value == "stop"
											section.footerMenus.add(
												MenuItem(
													actionLink.text(),
													null
												) {
													showStopOrResumeConfirmation(
														serviceId,
														isStop,
														leftDay
													)
													true
												})
											section.footerMenus.add(
												MenuItem(
													getString(R.string.pay_fee),
													null,
													isStop
												) {
													openPayDialog(
														serviceId,
														row[1] ?: "",
														row[7] ?: ""
													)
													true
												})
											statusSections.add(section)
										}
									} else loadBills()
								}

								6 -> {
									val select = tr.select("select")
									val serviceId = select.attr("s")
									section.footerMenus.add(
										MenuItem(
											getString(R.string.pay_fee),
											null,
											serviceId.isNotEmpty()
										) {
											openPayDialog(serviceId, row[1] ?: "", row[8] ?: "")
											true
										})
									statusSections.add(section)
								}
							}
						}
				}

				2, 3, 7 -> {
					reloadStatus()
					reloadOrders()
				}
			}
		}
		loadOrders()
		loadStatus()
	}

	fun loadOrders() {
		model.addAndNext("netpay/c/site/orders", "", 0)
	}

	fun loadStatus() {
		model.addAndNext(
			"netpay/c/site/stopAndResumeList",
			"personal=1",
			"application/x-www-form-urlencoded",
			1,
		)
	}

	fun loadBills() {
		model.addAndNext(
			"netpay/c/site/bills",
			"personal=1",
			"application/x-www-form-urlencoded",
			6
		)
	}

	fun reloadOrders() {
		orderSections.clear()
		loadOrders()
	}

	fun reloadStatus() {
		statusSections.clear()
		loadStatus()
		loadBills()
	}

	fun stop(serviceId: String?) {
		model.addAndNext(
			"netpay/c/site/stop",
			"serviceId=$serviceId",
			"application/x-www-form-urlencoded",
			2,
		)
	}

	fun resume(serviceId: String?) {
		model.addAndNext(
			"netpay/c/site/resume",
			"serviceId=$serviceId",
			"application/x-www-form-urlencoded",
			3,
		)
	}

	fun cancelPay(orderId: String) {
		model.addAndNext(
			"netpay/c/site/cancelOrder",
			"orderId=$orderId&type=web",
			"application/x-www-form-urlencoded",
			7,
		)
	}

	fun openPayDialog(serviceId: String, service: String, oldOutDate: String) {
		selectedServiceId = serviceId
		serviceName = service
		oldDateStr = oldOutDate
		oldDate = LocalDate.parse(oldOutDate, formatter)
		timeIndex = -1
		fee = 0
		newOutDateStr = ""
		showPayDialog = true
	}

	fun closePayDialog() {
		showPayDialog = false
	}

	fun selectTime(index: Int) {
		timeIndex = index
		newOutDateStr = if (index == 0) {
			oldDate!!.plusDays(15).format(formatter)
		} else {
			oldDate!!.plusMonths(months[index]).format(formatter)
		}
		fee = fees[index]
	}

	fun submitOrder() {
		val serviceId = selectedServiceId ?: return
		val time = if (timeIndex == 0) 0.5 else months[timeIndex].toDouble()
		orderNetPay(time, fee, serviceId)
		showPayDialog = false
	}

	private fun orderNetPay(time: Double, fee: Int, serviceId: String) {
		val timeStr = if (time < 1) time.toFloat() else time.toInt().toFloat()
		model.run(
			"netpay/c/site/prepareOrder",
			"type=web&months=$timeStr&moneys=$fee&serviceIds=$serviceId",
			"application/x-www-form-urlencoded",
			object : Callback {
				override fun onFailure(call: Call, e: IOException) {
					model.http.handler.post { toast(R.string.no_net_connected) }
				}

				override fun onResponse(call: Call, response: Response) {
					val text = response.body.string()
					try {
						val json = JSONObject.parse(text)
						viewModelScope.launch {
							if (!json.getBoolean("success")) {
								toast(R.string.order_fail)
							} else {
								toast(R.string.order_success)
								loadOrders()
							}
						}
					} catch (_: JSONException) {
						val data = JSONObject()
						parse(text).select("input").forEach {
							data[it.attr("name")] = it.attr("value")
						}
						viewModelScope.launch {
							toast(R.string.order_success)
							gotoWechat(data)
						}
					}
				}
			},
		)
	}

	fun continuePay(orderId: String) {
		model.run(
			"netpay/c/site/continueToPay",
			"out_trade_no=$orderId",
			"application/x-www-form-urlencoded",
			object : Callback {
				override fun onFailure(call: Call, e: IOException) {
					model.http.handler.post { toast(R.string.no_net_connected) }
				}

				override fun onResponse(call: Call, response: Response) {
					val text = response.body.string()
					try {
						val json = JSONObject.parse(text)
						if (!json.getBoolean("success")) {
							viewModelScope.launch { toast(R.string.order_fail) }
						} else {
							viewModelScope.launch {
								toast(R.string.order_success)
							}
						}
					} catch (_: JSONException) {
						viewModelScope.launch { toast(R.string.order_fail) }
					}
				}
			},
		)
	}

	fun showStopOrResumeConfirmation(serviceId: String, isStop: Boolean, leftDay: String) {
		val message = if (isStop) {
			"暂停网络将即时生效，暂停最小时长：7天。是否确定要暂停？"
		} else if ((leftDay.toIntOrNull() ?: 0) < 7) {
			"网络服务已暂停${leftDay}天，不足暂停最小时长（7天），提前恢复本次暂停作废，过期日期不顺延！是否仍要提前恢复网络？"
		} else {
			"网络服务已暂停${leftDay}天，执行恢复将即时生效，是否确定要恢复？"
		}
		snackbarMessage = message
		snackbarActionLabel = "确定"
		snackbarAction = if (isStop) {
			{ stop(serviceId) }
		} else {
			{ resume(serviceId) }
		}
	}

	fun clearSnackbar() {
		snackbarMessage = null
		snackbarActionLabel = null
		snackbarAction = null
	}

	private fun gotoWechat(data: JSONObject) {
		val info = StringBuilder()
		data.forEach { (key, value) ->
			info.append(key).append("=").append(value).append("&")
		}
		OkHttpClient.Builder().followRedirects(false).build().newCall(
				model.http.generateRequest(
					"https://fee.sysu.edu.cn/gateway/unifiedorder/pagepay",
					"$info",
					"application/x-www-form-urlencoded",
				).build(),
			).enqueue(object : Callback {
				override fun onFailure(call: Call, e: IOException) {
					model.http.handler.post { toast(R.string.no_net_connected) }
				}

				override fun onResponse(call: Call, response: Response) {
					val location = response.header("Location")
					if (!location.isNullOrEmpty()) {
						model.http.handler.post {
							val app = getApplication<Application>()
							val clipboard =
								app.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
							clipboard.setPrimaryClip(ClipData.newPlainText("recharge", location))
							val intent = Intent.createChooser(
								Intent(Intent.ACTION_SEND).setType("text/plain")
									.putExtra(Intent.EXTRA_TEXT, location)
									.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.recharge))
									.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
								getString(R.string.share),
							)
							if (intent.resolveActivity(app.packageManager) != null) {
								intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
								app.startActivity(intent)
							}
						}
					}
				}
			})
	}

	private fun getSection(
		title: String?,
		keys: List<String?>,
		values: List<String?>
	): SectionData {
		val rows = mutableStateListOf<RowData>()
		keys.zip(values).forEach { (k, v) -> rows.add(RowData(k, v)) }
		return SectionData(title = title, rows = rows)
	}

	private fun toast(resId: Int) {
		Toast.makeText(
			getApplication(),
			resId,
			Toast.LENGTH_SHORT,
		).show()
	}

	private fun getString(resId: Int): String = application.getString(resId)

	override fun onCleared() {
		model.dispose()
	}
}