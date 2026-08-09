package com.sysu.edu.life

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import com.alibaba.fastjson2.JSONException
import com.alibaba.fastjson2.JSONObject
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.snackbar.Snackbar
import com.sysu.edu.R
import com.sysu.edu.databinding.DialogNetPayBinding
import com.sysu.edu.model.NetPayModel
import com.sysu.edu.view.StaggerFragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Response
import org.jsoup.Jsoup
import java.io.IOException
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class NetOrderFragment : StaggerFragment() {
	var fee: Int = 0
	var time: Number? = null
	val model: NetPayModel by lazy {
		NetPayModel(requireContext())
	}
	
	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?,
	                         ): View? {
		val formatter = DateTimeFormatter.ofPattern("yyyy-M-d")
		val view = super.onCreateView(inflater, container, savedInstanceState)
		val dialogNetBinding = DialogNetPayBinding.inflate(inflater).apply {
			service.key.setText(R.string.service)
			oldOutDate.key.setText(R.string.old_out_date)
			newOutDate.key.setText(R.string.new_out_date)
			fee.key.setText(R.string.fee)
			time.key.setText(R.string.time)
			time.value.setText(R.string.click_to_select)
		}
		val popupMenu = PopupMenu(requireActivity(), dialogNetBinding.time.value, 0, 0, com.google.android.material.R.style.Widget_Material3_PopupMenu_Overflow)
		val menu = popupMenu.menu
		dialogNetBinding.time.root.setOnClickListener { popupMenu.show() }
		var oldDate: LocalDate? = null
		val strings = arrayOf("15天", "1个月", "2个月", "3个月", "4个月", "5个月", "6个月", "7个月", "8个月", "9个月", "10个月", "11个月", "1年", "2年")
		val months = longArrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 24, 48)
		val fees = intArrayOf(15, 30, 60, 90, 120, 150, 180, 210, 240, 270, 300, 330, 300, 600)
		strings.indices.forEach { i ->
			menu.add(0, 0, 0, strings[i]).setOnMenuItemClickListener {
				dialogNetBinding.time.value.text = strings[i]
				dialogNetBinding.newOutDate.value.text = (if (i == 0) oldDate!!.plusDays(15) else oldDate!!.plusMonths(months[i])).format(formatter)
				time = if (i == 0) 0.5 else months[i].toDouble()
				fee = fees[i]
				dialogNetBinding.fee.value.text = "${fee}元"
				popupMenu.dismiss()
				true
			}
		}
		val payDialog = BottomSheetDialog(requireActivity())
		payDialog.setContentView(dialogNetBinding.root)
		model.message.observe(viewLifecycleOwner) { (code, response) ->
			when (code) {
				0, 1, 6 -> {
					val statusKeys = mutableListOf<String?>("序号", "服务", "地址", "MAC地址", "部门", "使用者", "状态", "过期日期", "暂停日期")
					val orderKeys = mutableListOf<String?>("订单号", "所有者", "金额", "支付方式", "订单时间", "订单状态", "服务", "代支付者")
					Jsoup.parse(response.getString("data")).selectFirst("tbody")?.select("tr")?.forEach {
						val row = mutableListOf<String?>()
						it.select("td").forEach { td ->
							row.add(td.text())
						}
						when (code) {
							0 -> addSection(row[4], orderKeys, row)
							1 -> addSection(row[0], statusKeys, row)
						}
						when (code) {
							0 -> {
								val pay = it.select("a").firstOrNull { elements -> elements.text() == "支付" }
								if (pay != null) {
									val payId = row[0] ?: ""
									sectionAdapter.addFooter {
										FilledTonalButton(onClick = {
											continuePay(payId)
										}, modifier = Modifier.weight(1f), shapes = ButtonDefaults.shapes()) {
											Text(getString(R.string.pay))
										}
									}
									sectionAdapter.addFooter {
										FilledTonalButton(onClick = {
											cancelPay(payId)
										}, modifier = Modifier.weight(1f), shapes = ButtonDefaults.shapes()) {
											Text(getString(R.string.cancel_pay))
										}
									}
								}
							}
							1 -> {
								val action = it.select("a").firstOrNull { elements -> elements.text() == "暂停" }
								if (action != null) {
									"(.+?)\\((.+?),(.+?)\\)".toRegex().find(action.attr("onclick"))?.let { result ->
											val leftDay = result.groups[3]?.value ?: ""
											val serviceId = result.groups[2]?.value ?: ""
											val actionText = action.text()
											val isStopAction = (result.groups[1]?.value ?: "") == "stop"
											sectionAdapter.addFooter {
												FilledTonalButton(onClick = {
													Snackbar.make(requireView(),
													              if (isStopAction) "暂停网络将即时生效，暂停最小时长：7天。是否确定要暂停？" else if (leftDay.toInt() < 7) "网络服务已暂停" + leftDay + "天，不足暂停最小时长（7天），提前恢复本次暂停作废，过期日期不顺延！是否仍要提前恢复网络？" else "网络服务已暂停" + leftDay + "天，执行恢复将即时生效，是否确定要恢复？",
													              Snackbar.LENGTH_SHORT).setAction(R.string.confirm) {
														if (isStopAction) stop(serviceId)
														else resume(serviceId)
													}.show()
												}, modifier = Modifier.weight(1f), shapes = ButtonDefaults.shapes()) {
													Text(actionText)
												}
											}
											sectionAdapter.addFooter {
												FilledTonalButton(onClick = {
													payDialog.show()
													oldDate = LocalDate.parse(row[7], formatter)
													dialogNetBinding.oldOutDate.value.text = row[7]
													dialogNetBinding.service.value.text = row[1]
													dialogNetBinding.submit.setOnClickListener { order(time!!, fee, serviceId) }
												}, modifier = Modifier.weight(1f), shapes = ButtonDefaults.shapes()) {
													Text(getString(R.string.pay_fee))
												}
											}
										}
								}
								else serviceId
							}
							6 -> {
								val select = it.select("select")
								val serviceId = select.attr("s")
								sectionAdapter.addFooter {
									FilledTonalButton(onClick = {
										payDialog.show()
										oldDate = LocalDate.parse(row[8], formatter)
										dialogNetBinding.oldOutDate.value.text = row[8]
										dialogNetBinding.service.value.text = row[1]
										dialogNetBinding.submit.setOnClickListener { order(time!!, fee, serviceId) }
									}, modifier = Modifier.weight(1f), shapes = ButtonDefaults.shapes()) {
										Text(getString(R.string.pay_fee))
									}
								}
							}
						}
					}
				}
				2, 3, 7 -> {
					regetInfo()
				}
			}
		}
		info
		return view
	}
	
	val order: Unit
		get() {
			model.addAndNext("netpay/c/site/orders", "", 0)
		}
	val net: Unit
		get() {
			model.addAndNext("netpay/c/site/stopAndResumeList", "personal=1", "application/x-www-form-urlencoded", 1)
		}
	val info: Unit
		get() {
			when (requireArguments().getInt("code")) {
				0 -> order
				1 -> net
			}
		}
	
	fun stop(serviceId: String?) {
		model.addAndNext("netpay/c/site/stop", "serviceId=$serviceId", "application/x-www-form-urlencoded", 2)
	}
	
	fun resume(serviceId: String?) {
		model.addAndNext("netpay/c/site/resume", "serviceId=$serviceId", "application/x-www-form-urlencoded", 3)
	}
	
	fun order(time: Number, fee: Int, serviceId: String?) {
		model.run("netpay/c/site/prepareOrder", "type=web&months=${
			if (time.toFloat() < 1) time.toFloat()
			else time.toInt().toFloat()
		}&moneys=$fee&serviceIds=$serviceId", "application/x-www-form-urlencoded", object : Callback {
			override fun onFailure(call: Call, e: okio.IOException) {
				model.http.handler.post {
					config.toast(R.string.no_net_connected)
				}
			}
			
			override fun onResponse(call: Call, response: Response) {
				val text = response.body.string()
				try {
					val json = JSONObject.parse(text)
					if (!json.getBoolean("success")) {
						CoroutineScope(Dispatchers.Main).launch {
							config.toast(R.string.order_fail)
						}
					}
					else {
						CoroutineScope(Dispatchers.Main).launch {
							config.toast(R.string.order_success)
							regetInfo()
						}
					}
				} catch (_: JSONException) {
					val data = JSONObject()
					Jsoup.parse(text).select("input").forEach {
						data[it.attr("name")] = it.attr("value")
					}
					CoroutineScope(Dispatchers.Main).launch {
						config.toast(R.string.order_success)
						gotoWechat(data)
					}
				}
			}
		})
	}
	
	fun continuePay(orderId: String) {
		model.run("netpay/c/site/continueToPay", "out_trade_no=$orderId", "application/x-www-form-urlencoded", object : Callback {
			override fun onFailure(call: Call, e: okio.IOException) {
				model.http.handler.post {
					config.toast(R.string.no_net_connected)
				}
			}
			
			override fun onResponse(call: Call, response: Response) {
				val text = response.body.string()
				try {
					val json = JSONObject.parse(text)
					if (!json.getBoolean("success")) {
						CoroutineScope(Dispatchers.Main).launch {
							config.toast(R.string.order_fail)
						}
					}
					else {
						CoroutineScope(Dispatchers.Main).launch {
							config.toast(R.string.order_success)
							regetInfo()
						}
					}
				} catch (_: JSONException) {
					val data = JSONObject()
					Jsoup.parse(text).select("input").forEach {
						data[it.attr("name")] = it.attr("value")
					}
					CoroutineScope(Dispatchers.Main).launch {
						config.toast(R.string.order_success)
						gotoWechat(data)
					}
				}
			}
		})
	}
	
	private fun regetInfo() {
		clear()
		info
	}
	
	fun cancelPay(orderId: String) {
		model.addAndNext("netpay/c/site/cancelOrder", "orderId=$orderId&type=web", "application/x-www-form-urlencoded", 7)
	}
	
	fun gotoWechat(data: JSONObject) {
		val info = StringBuilder()
		data.forEach { (key: String?, value: Any?) ->
			info.append(key).append("=").append(value).append("&")
		}
		OkHttpClient.Builder().followRedirects(false).build().newCall(model.http.generateRequest("https://fee.sysu.edu.cn/gateway/unifiedorder/pagepay", "$info", "application/x-www-form-urlencoded").build()).enqueue(object : Callback {
			override fun onFailure(call: Call, e: IOException) {
				model.http.handler.post {
					config.toast(R.string.no_net_connected)
				}
			}
			
			override fun onResponse(call: Call, response: Response) {
				val location = response.header("Location")
				if (!location.isNullOrEmpty()) {
					model.http.handler.post {
						config.copy("recharge", location)
						val intent = Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, location).putExtra(Intent.EXTRA_SUBJECT, getString(R.string.recharge)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
						                                  getString(R.string.share))
						if (intent.resolveActivity(requireContext().packageManager) != null) startActivity(intent)
					}
				}
			}
		})
	}
	
	val serviceId: Unit
		get() {
			model.addAndNext("netpay/c/site/bills", "personal=1", "application/x-www-form-urlencoded", 6)
		}
}
