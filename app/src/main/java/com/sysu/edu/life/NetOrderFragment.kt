package com.sysu.edu.life

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.alibaba.fastjson2.JSONException
import com.alibaba.fastjson2.JSONObject
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButtonGroup
import com.google.android.material.snackbar.Snackbar
import com.sysu.edu.R
import com.sysu.edu.api.HttpManager
import com.sysu.edu.api.TargetUrl
import com.sysu.edu.databinding.DialogNetPayBinding
import com.sysu.edu.databinding.ItemButtonGroupBinding
import com.sysu.edu.databinding.ItemButtonOutlineBinding
import com.sysu.edu.databinding.ItemCardBinding
import com.sysu.edu.view.AdapterListener
import com.sysu.edu.view.StaggeredFragment
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Response
import java.io.IOException
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.regex.Pattern

class NetOrderFragment : StaggeredFragment() {
	lateinit var http: HttpManager
	var oldDate: LocalDate? = null
	var fee: Int = 0
	var time: Number? = null
	override fun onCreateView(inflater: LayoutInflater,
	                          container: ViewGroup?,
	                          savedInstanceState: Bundle?): View? {
		val formatter = DateTimeFormatter.ofPattern("yyyy-M-d")
		val view = super.onCreateView(inflater, container, savedInstanceState)
		config.setCallback { info }
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
		val strings = arrayOf("15天", "1个月", "2个月", "3个月", "4个月", "5个月", "6个月", "7个月", "8个月", "9个月", "10个月", "11个月", "1年", "2年")
		val months = longArrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 24, 48)
		val fees = intArrayOf(15, 30, 60, 90, 120, 150, 180, 210, 240, 270, 300, 330, 300, 600)
		strings.indices.forEach { i ->
			menu.add(0, 0, 0, strings[i]).setOnMenuItemClickListener {
				dialogNetBinding.time.value.text = strings[i]
				dialogNetBinding.newOutDate.value.text = (if (i == 0) oldDate!!.plusDays(15) else oldDate!!.plusMonths(months[i])).format(formatter)
				time = if (i == 0) 0.5 else months[i].toDouble()
				fee = fees[i]
				dialogNetBinding.fee.value.text = String.format(Locale.getDefault(), "%d元", fee)
				popupMenu.dismiss()
				true
			}
		}
		val payDialog = BottomSheetDialog(requireActivity())
		payDialog.setContentView(dialogNetBinding.root)
		http = HttpManager(object : Handler(Looper.getMainLooper()) {
			override fun handleMessage(msg: Message) {
				super.handleMessage(msg)
				val response = msg.obj as String
				when (msg.what) {
					-1 -> config.toast(R.string.no_net_connected)
					5 -> {
						config.copy("recharge", msg.obj as String?)
						val intent = Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain")
															  .putExtra(Intent.EXTRA_TEXT, msg.obj as String?)
															  .putExtra(Intent.EXTRA_SUBJECT, getString(R.string.recharge))
															  .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), getString(R.string.share))
						if (intent.resolveActivity(requireContext().packageManager) != null) startActivity(intent)
					}
					0, 1, 6 -> {
						staggeredAdapter.clear()
						try {
							val json = JSONObject.parse(response)
							if (!json.getBoolean("success")) {
								config.gotoLogin(TargetUrl.NETPAY)
							}
						} catch (_: JSONException) {
							val matcher = Pattern.compile("<tr .*?>(.+?)</tr>", Pattern.DOTALL)
								.matcher(response)
							while (matcher.find()) {
								val orderDetail = mutableListOf<String?>()
								val item = matcher.group(1)
								if (item != null) {
									val keys = mutableListOf<String?>("序号", "服务", "地址", "MAC地址", "部门", "使用者", "状态", "过期日期", "暂停日期")
									val isStop: Boolean
									val matcher2 = Pattern.compile("<td .*?>(.+?)</td>", Pattern.DOTALL)
										.matcher(item)
									while (matcher2.find()) orderDetail.add(matcher2.group(1)
																				?.replace("<.+?>".toRegex(), "")
																				?.trim { it <= ' ' })
									if (requireArguments().getInt("code") == 1) {
										if (msg.what == 1) {
											val action = Pattern.compile("onclick='(.+?)\\((.+?)\\)'>(.+?)</a>")
												.matcher(item)
											if (action.find()) {
												isStop = action.group(1) == "stop"
												val actionMatcher = Pattern.compile("(.+?),(.+?),")
													.matcher("${action.group(2)},".replace("\"", ""))
												if (actionMatcher.find()) {
													staggeredAdapter.setListener(object :
																					 AdapterListener {
														override fun onBind(adapter: RecyclerView.Adapter<RecyclerView.ViewHolder?>?,
														                    holder: RecyclerView.ViewHolder?,
														                    position: Int) {
														}
														
														override fun onCreate(adapter: RecyclerView.Adapter<RecyclerView.ViewHolder?>?,
														                      b: ViewBinding) {
															val line = ItemButtonGroupBinding.inflate(inflater, (b as ItemCardBinding).root, false).root
															val leftDay = actionMatcher.group(2)
															orderDetail[9] = leftDay
															val serviceId = actionMatcher.group(1)
															val isStop = action.group(1) == "stop"
															getMaterialButton(inflater, line, action.group(3)) { v: View? ->
																if (leftDay != null) Snackbar.make(v!!, if (isStop) "暂停网络将即时生效，暂停最小时长：7天。是否确定要暂停？" else if (leftDay.toInt() < 7) "网络服务已暂停" + leftDay + "天，不足暂停最小时长（7天），提前恢复本次暂停作废，过期日期不顺延！是否仍要提前恢复网络？" else "网络服务已暂停" + leftDay + "天，执行恢复将即时生效，是否确定要恢复？", Snackbar.LENGTH_SHORT)
																	.setAction(R.string.confirm) {
																		if (isStop) stop(serviceId)
																		else resume(serviceId)
																	}
																	.show()
															}
															getMaterialButton(inflater, line, getString(R.string.pay)) {
																payDialog.show()
																oldDate = LocalDate.parse(orderDetail[7], formatter)
																dialogNetBinding.oldOutDate.value.text = orderDetail[7]
																dialogNetBinding.service.value.text = orderDetail[1]
																dialogNetBinding.submit.setOnClickListener { order(time!!, fee, serviceId) }
															}
															b.root.addView(line)
														}
													})
												}
											} else {
												serviceId
												break
											}
											keys.add(if (isStop) getString(R.string.left_time) else getString(R.string.pause_time))
										} else {
											val action = Pattern.compile("<select .*? s='(.*?)'")
												.matcher(item)
											if (action.find()) {
												val serviceId = action.group(1)
												staggeredAdapter.setListener(object :
																				 AdapterListener {
													override fun onBind(adapter: RecyclerView.Adapter<RecyclerView.ViewHolder?>?,
													                    holder: RecyclerView.ViewHolder?,
													                    position: Int) {
													}
													
													override fun onCreate(adapter: RecyclerView.Adapter<RecyclerView.ViewHolder?>?,
													                      b: ViewBinding) {
														val line = ItemButtonGroupBinding.inflate(inflater, (b as ItemCardBinding).root, false).root
														getMaterialButton(inflater, line, getString(R.string.pay)) {
															payDialog.show()
															oldDate = LocalDate.parse(orderDetail[8], formatter)
															dialogNetBinding.oldOutDate.value.text = orderDetail[8]
															dialogNetBinding.service.value.text = orderDetail[1]
															dialogNetBinding.submit.setOnClickListener { order(time!!, fee, serviceId) }
														}
														b.root.addView(line)
													}
												})
											}
										}
									}
									add(orderDetail[if (msg.what == 0) 4 else 0], if (msg.what == 0) mutableListOf<String?>("订单号", "所有者", "金额", "支付方式", "订单时间", "订单状态", "服务", "代支付者") else keys, orderDetail)
								}
							}
						}
					}
					2, 3 -> {
						try {
							clear()
							info
						} catch (_: JSONException) {
						}
					}
					4 -> {
						try {
							println(response)
							val json = JSONObject.parse(response)
							if (json.getBoolean("success")) {
								config.toast(R.string.order_success)
								clear()
								info
							} else {
								config.toast(R.string.order_fail)
							}
						} catch (_: JSONException) {
							config.toast(R.string.order_success)
							val data = JSONObject()
							val matcher = Pattern.compile("<input.*?name='(.*?)' value='(.*?)'/>", Pattern.DOTALL)
								.matcher(response)
							while (matcher.find()) data[matcher.group(1)] = matcher.group(2)
							gotoWechat(data)
							clear()
							info
						}
					}
				}
			}
		}).apply {
			setHeader(mutableMapOf("accept-language" to "zh-CN,zh;q=0.9,en-US;q=0.8,en;q=0.7"))
			setParams(this@NetOrderFragment.config)
		}
		info
		return view
	}
	
	private fun getMaterialButton(inflater: LayoutInflater,
	                              parent: MaterialButtonGroup,
	                              str: String?,
	                              onClick: View.OnClickListener?) {
		parent.addView(ItemButtonOutlineBinding.inflate(inflater, parent, false).root.apply {
			text = str
			layoutParams = MaterialButtonGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
				.apply {
					setMargins(0, 0, config.dpToPx(16), config.dpToPx(16))
				}
			setOnClickListener(onClick)
		})
	}
	
	val order: Unit
		get() {
			http.postRequest("https://netpay.sysu.edu.cn/netpay/c/site/orders", "", 0)
		}
	val net: Unit
		get() {
			http.postRequest("https://netpay.sysu.edu.cn/netpay/c/site/stopAndResumeList", "personal=1", "application/x-www-form-urlencoded", 1)
		}
	val info: Unit
		get() {
			arrayOf(Runnable { this.order }, Runnable { this.net })[requireArguments().getInt("code")].run()
		}
	
	fun stop(serviceId: String?) {
		http.postRequest("https://netpay.sysu.edu.cn/netpay/c/site/stop", "serviceId=$serviceId", "application/x-www-form-urlencoded", 2)
	}
	
	fun resume(serviceId: String?) {
		http.postRequest("https://netpay.sysu.edu.cn/netpay/c/site/resume", "serviceId=$serviceId", "application/x-www-form-urlencoded", 3)
	}
	
	fun order(time: Number, fee: Int, serviceId: String?) {
		http.postRequest("https://netpay.sysu.edu.cn/netpay/c/site/prepareOrder", String.format(Locale.getDefault(), "type=web&months=%s&moneys=%d&serviceIds=%s", if (time.toFloat() < 1) time.toFloat() else time.toInt()
			.toFloat(), fee, serviceId), "application/x-www-form-urlencoded", 4)
	}
	
	fun gotoWechat(data: JSONObject) {
		val info = StringBuilder()
		data.forEach { (key: String?, value: Any?) ->
			info.append(key).append("=").append(value).append("&")
		}
		OkHttpClient.Builder()
			.followRedirects(false)
			.build()
			.newCall(http.generateRequest("https://fee.sysu.edu.cn/gateway/unifiedorder/pagepay", "$info", "application/x-www-form-urlencoded")
						 .build())
			.enqueue(object : Callback {
				override fun onFailure(call: Call, e: IOException) {
					http.sendFailure()
				}
				
				override fun onResponse(call: Call, response: Response) {
					response.header("Location")
					if (!TextUtils.isEmpty(response.header("Location"))) {
						val message = Message()
						message.what = 5
						message.obj = response.header("Location")
						http.handler.sendMessage(message)
					}
				}
			})
	}
	
	val serviceId: Unit
		get() {
			http.postRequest("https://netpay.sysu.edu.cn/netpay/c/site/bills", "personal=1", "application/x-www-form-urlencoded", 6)
		}
}
