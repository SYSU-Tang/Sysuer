package com.sysu.edu.life

import android.app.Application
import android.content.Intent
import androidx.compose.runtime.mutableStateSetOf
import androidx.compose.runtime.snapshots.SnapshotStateSet
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.alibaba.fastjson2.JSONArray
import com.alibaba.fastjson2.JSONObject
import com.sysu.edu.R
import com.sysu.edu.model.PayModel
import kotlinx.coroutines.launch
import okhttp3.Call
import okhttp3.Callback
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Response
import org.jsoup.Jsoup.parse
import java.io.IOException

class PayViewModel(application: Application) : AndroidViewModel(application) {
	private val model: PayModel = PayModel(application)
	private val _toPayList = MutableLiveData(JSONArray())
	val toPayList: LiveData<JSONArray> = _toPayList
	private val _selectivePayList = MutableLiveData(JSONArray())
	val selectivePayList: LiveData<JSONArray> = _selectivePayList
	private val _feeList = MutableLiveData(JSONArray())
	val feeList: LiveData<JSONArray> = _feeList
	private val _paymentList = MutableLiveData(JSONArray())
	val paymentList: LiveData<JSONArray> = _paymentList
	private val _refundList = MutableLiveData(JSONArray())
	val refundList: LiveData<JSONArray> = _refundList
	var toPayItems: JSONObject = JSONObject()
	val pendingPay: SnapshotStateSet<String> = mutableStateSetOf()
	
	init {
		model.message.observeForever { (code, response) ->
			println(response)
			if (response.getInteger("code") == 200 && response.get("data") != null) {
				when (code) {
					0 -> _toPayList.value = response.getJSONArray("data")
					1 -> _selectivePayList.value = response.getJSONArray("data")
					2 -> _feeList.value = response.getJSONArray("data")
					3 -> _paymentList.value = response.getJSONArray("data")
					4 -> _refundList.value = response.getJSONArray("data")
					5 -> {
						val data = response.getJSONObject("data")
						toPayItems.fluentPut("ticketTitle", data.getString("ticketTitle"))
							.fluentPut("sucUrl", data.getString("https://pay.sysu.edu.cn/#/result/pay_suc"))
							.fluentPut("pendFees", data.getJSONArray("pendFees"))
							.fluentPut("allMoney", data.getInteger("allMoney"))
							.fluentPut("enableUseCompanyTitle", data.getBoolean("enableUseCompanyTitle")) //							?.fluentPut("userRegNum", null)
						//							?.fluentPut("userAddress", null)
						//							?.fluentPut("userTele", null)
						//							?.fluentPut("userBank", null)
						//							?.fluentPut("userBankNum",
						//							            null)						/*"userRegNum":null,"userAddress":null,"userTele":null,"userBank":null,"userBankNum":null,*/                        /*{"code":200,"data":{"pendFees":[{"itemId":50,"itemName":"医保","canPay":true,"enableUseCompanyTitle":false,"arrearId":3336991,"nowMoney":450,"intervalId":11890,"intervalName":"2027"}],"allMoney":450.0,"ticketTitle":"唐贤标","userCode":"24308152","userName":"唐贤标","enableUseCompanyTitle":false,"operType":"FEE_MUST","invoiceIsDelay":0},"message":"Processes successfully."}*/
						submit()
					}
					6 -> {
						val builder = FormBody.Builder()
						builder.add("page_url", "https%3A%2F%2Fpay.sysu.edu.cn%2F%23%2Fresult%2Fpay_suc")
						//val table = mutableListOf("page_url=https%3A%2F%2Fpay.sysu.edu.cn%2F%23%2Fresult%2Fpay_suc")
						println(response.getString("data"))
						val doc = parse(response.getString("data"))
						doc.selectFirst("input[name='out_trade_no']")?.attr("value")?.also {
							pendingPay.add(it)
						}
						doc.select("input[type=hidden]").forEach {
								val name = it.attr("name")
								val value = it.attr("value")
								if (name.isNotBlank()) {
									builder.add(name, value)
								}
							}
							viewModelScope.launch {
								model.contextUtil.toast(R.string.order_success)
								gotoWechat(builder.build())
							}
					}
					7 -> model.contextUtil.toast(response.getString("message"))
				}
			}
			else if (response.getInteger("code") == 5001) {
				when (code) {
					5 -> {
						pendingPay.addAll(response.getJSONArray("data").filterIsInstance<String>()) //						response.getJSONArray("data").forEach {
						//							model.contextUtil.copy("payOrder", "$it")
						//							openWechat("https://fee.sysu.edu.cn/gateway/cashier/order?orderno=${it}&scene=web")
						//						}
					}
				}
			}
		}
	}
	
	fun fetchToPayList() {
		model.addAndNext("client/api/client/necessary/list", "{}", 0)
	}
	
	fun fetchSelectivePayList() {
		model.addAndNext("client/api/client/chooce/list", "{}", 1)
	}
	
	fun fetchFeeList(year: String) {
		model.addAndNext("client/api/client/record/feelist", "{\"year\":$year}", 2)
	}
	
	fun fetchPaymentList(from: String, to: String?) {
		model.addAndNext("client/api/client/record/paymentlist", "{\"startTime\":\"$from\",\"overTime\":\"$to\"}", 3)
	}
	
	fun fetchRefundList() {
		model.addAndNext("client/api/client/refund/list", "{}", 4)
	}
	
	fun check(data: JSONObject) {
		model.addAndNext("client/api/client/necessary/pay/check", data.toJSONString(), 5)
	}
	
	fun submit() {
		println(toPayItems)
		model.addAndNext("client/api/client/necessary/submitOrder", toPayItems.toJSONString(), 6)
	}
	
	fun cancel(payOrder: String) {
		model.addAndNext("client/api/client/necessary/operate", "{\"operateCode\":\"CANCEL_PAY\",\"orderNo\":\"$payOrder\"}", 7)
	}
	
	private fun gotoWechat(data: FormBody) {
		OkHttpClient.Builder()
			.followRedirects(false)
			.build()
			.newCall(model.http.generateRequest("https://fee.sysu.edu.cn/gateway/unifiedorder/pagepay", null, null).post(data).addHeader("Content-Type", "application/x-www-form-urlencoded").build())
			.enqueue(object : Callback {
				override fun onFailure(call: Call, e: IOException) {
					model.http.handler.post { model.contextUtil.toast(R.string.no_net_connected) }
				}
				
				override fun onResponse(call: Call, response: Response) {
					println(response.body.string())
					println(response.headers.toMutableList())
					val location = response.header("Location")
					if (!location.isNullOrEmpty()) {
						model.contextUtil.copy("payOrder", location)
						openWechat(location)
					}
				}
			})
	}
	
	fun clearPendingPay() {
		pendingPay.clear()
	}
	
	fun openWechat(location: String) {
		model.http.handler.post {
			val app = getApplication<Application>()
			model.contextUtil.copy("recharge", location)
			val intent = Intent.createChooser(
				Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, location).putExtra(Intent.EXTRA_SUBJECT, model.contextUtil.context.getString(R.string.recharge)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
				model.contextUtil.context.getString(R.string.share),
			                                 )
			if (intent.resolveActivity(app.packageManager) != null) {
				intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
				app.startActivity(intent)
			}
		}
	}
	
	override fun onCleared() {
		model.dispose()
	}
}