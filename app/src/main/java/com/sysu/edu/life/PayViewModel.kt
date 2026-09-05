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
	val model: PayModel = PayModel(application)
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
	private val _paymentDetail = MutableLiveData<JSONObject?>(null)
	val paymentDetail: LiveData<JSONObject?> = _paymentDetail
	var toPayItems: JSONObject = JSONObject()
	val pendingPay: SnapshotStateSet<String> = mutableStateSetOf()

	init {
		model.message.observeForever { (code, response) ->
			if (response.getInteger("code") == 200) {
				when (code) {
					0 -> _toPayList.value = response.getJSONArray("data")
					1 -> _selectivePayList.value = response.getJSONArray("data")
					2 -> _feeList.value = response.getJSONArray("data")
					3 -> _paymentList.value = response.getJSONArray("data")
					4 -> _refundList.value = response.getJSONArray("data")
					5 -> {
						val data = response.getJSONObject("data")
						toPayItems.fluentPut("ticketTitle", data.getString("ticketTitle"))
							.fluentPut("sucUrl", "https://pay.sysu.edu.cn/#/result/pay_suc")
							.fluentPut("pendFees", data.getJSONArray("pendFees"))
							.fluentPut("allMoney", data.getInteger("allMoney")).fluentPut(
								"enableUseCompanyTitle", data.getBoolean("enableUseCompanyTitle")
							)
						submit()
					}

					6 -> {
						val builder = FormBody.Builder()
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

					7 -> {
						model.contextUtil.toast(response.getString("message"))
					}

					8 -> {
						_paymentDetail.value = response.getJSONObject("data")
						/*{
    "code": 200,
    "data": {
        "orderNo": "1545478524259930112",
        "money": 15,
        "stateStr": "支付成功",
        "createTime": "2026-09-04 16:59:53",
        "payType": "微信支付",
        "payTime": "2026-09-04 17:01:12",
        "ticketTitle": "个人",
        "downloadToken": "hdxFoVNnH0H8eLeijWpYTOK8%2FF4GgAZgt%2BIthEocEdY%3D%7CR8tihYklArCzAKB4",
        "details": [
            {
                "itemName": "水电费",
                "payMoney": 15
            }
        ]
    },
    "message": "处理成功"
}*/
					}
				}
			} else if (response.getInteger("code") == 5001) {
				when (code) {
					5 -> {
						pendingPay.addAll(response.getJSONArray("data").filterIsInstance<String>())
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
		model.addAndNext(
			"client/api/client/record/paymentlist",
			"{\"startTime\":\"$from\",\"overTime\":\"$to\"}",
			3
		)
	}

	fun fetchRefundList() {
		model.addAndNext("client/api/client/refund/list", "{}", 4)
	}

	fun check(data: JSONObject) {
		model.addAndNext("client/api/client/necessary/pay/check", data.toJSONString(), 5)
	}

	fun submit() {
		model.addAndNext("client/api/client/necessary/submitOrder", toPayItems.toJSONString(), 6)
	}

	fun cancel(payOrder: String) {
		model.addAndNext(
			"client/api/client/necessary/operate",
			"{\"operateCode\":\"CANCEL_PAY\",\"orderNo\":\"$payOrder\"}",
			7
		)
	}

	fun viewDetail(payOrder: String, payNo: String) {
		model.addAndNext(
			"client/api/client/record/paymentlist/detail",
			"{\"orderNo\":\"$payOrder\",\"outPayNo\":\"${payNo}\"}",
			8
		)
	}

	private fun gotoWechat(data: FormBody) {
		OkHttpClient.Builder().followRedirects(false).build().newCall(
			model.http.generateRequest(
				"https://fee.sysu.edu.cn/gateway/unifiedorder/pagepay", null, null
			).post(data).addHeader("Content-Type", "application/x-www-form-urlencoded").build()
		).enqueue(object : Callback {
			override fun onFailure(call: Call, e: IOException) {
				model.http.handler.post { model.contextUtil.toast(R.string.no_net_connected) }
			}

			override fun onResponse(call: Call, response: Response) {
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
				Intent(Intent.ACTION_SEND).setType("text/plain")
					.putExtra(Intent.EXTRA_TEXT, location).putExtra(
						Intent.EXTRA_SUBJECT, model.contextUtil.context.getString(R.string.recharge)
					).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
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