package com.sysu.edu.life

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.collection.ArraySet
import androidx.collection.arraySetOf
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import com.alibaba.fastjson2.JSONObject
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.sysu.edu.BaseFragment
import com.sysu.edu.R
import com.sysu.edu.api.CommonUtil
import com.sysu.edu.api.CommonUtil.extractValue
import com.sysu.edu.databinding.DialogRechargeBinding
import com.sysu.edu.databinding.FragmentEnergyOrderBinding
import com.sysu.edu.model.ZhnyModel
import com.sysu.edu.todo.TitleAdapter
import com.sysu.edu.view.ButtonAdapter
import com.sysu.edu.view.PreferenceAdapter
import okhttp3.Call
import okhttp3.Callback
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Response
import java.io.IOException

class EnergyAccountFragment : BaseFragment() {
	val model: ZhnyModel by lazy { ZhnyModel(requireContext()) }
	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?,
	                         ): View {
		super.onCreateView(inflater, container, savedInstanceState)
		val rooms: ArraySet<CommonUtil.Tuple2<String?, String?>?> = arraySetOf()
		var roomCode: String? = null
		val adapter = ConcatAdapter()
		val rechargeDialog = BottomSheetDialog(requireContext())
		val rechargeBinding = DialogRechargeBinding.inflate(inflater)
		rechargeDialog.setContentView(rechargeBinding.root)
		rechargeBinding.submit.setOnClickListener {
			"${rechargeBinding.rmb.text}".toFloat().times(100).toInt().takeIf { it > 0 }?.let {
				recharge(it, roomCode, rechargeBinding.remark.text.toString())
			}
		}
		model.message.observe(viewLifecycleOwner) { (code, response) ->
			println("code = $code , response = $response")
			if (response.getInteger("code") == 200) {
				when (code) {
					0 -> {
						val userInfo = response.getJSONObject("data")
						adapter.addAdapter(TitleAdapter(getString(R.string.account)))
						val preferenceAdapter = PreferenceAdapter()
						preferenceAdapter.set(mutableListOf(R.string.name, R.string.student_id), extractValue(userInfo, arrayOf("name", "username")), mutableListOf(R.drawable.account, R.drawable.id), requireContext())
						adapter.addAdapter(preferenceAdapter)
						getRoom(userInfo.getString("username"))
					}
					1 -> {
						response.getJSONArray("data").forEach { e: Any? ->
							val roomInfo = e as JSONObject
							adapter.addAdapter(TitleAdapter(getString(R.string.dorm)))
							val preferenceAdapter = PreferenceAdapter()
							preferenceAdapter.set(mutableListOf(R.string.location, R.string.room_name), extractValue(roomInfo, arrayOf("areaInfo", "roomName")), mutableListOf(R.drawable.location, R.drawable.home), requireContext())
							adapter.addAdapter(preferenceAdapter)
							rooms.add(CommonUtil.Tuple2(roomInfo.getString("roomName"), roomInfo.getString("roomCode")))
						}
						if (!rooms.isEmpty()) {
							roomCode = rooms.valueAt(0)!!.getSecond()
							getBalance(roomCode)
						}
					}
					2 -> {
						adapter.addAdapter(TitleAdapter(getString(R.string.balance)))
						val preferenceAdapter = PreferenceAdapter()
						preferenceAdapter.add(getString(R.string.balance), response.getJSONObject("data").getString("balance"), R.drawable.money)
						adapter.addAdapter(preferenceAdapter)
						val buttonAdapter = ButtonAdapter()
						buttonAdapter.add(getString(R.string.pay_fee))
						buttonAdapter.setListener { button: Button?, _: Int -> button!!.setOnClickListener { rechargeDialog.show() } }
						adapter.addAdapter(buttonAdapter)
					}
					3 -> gotoWechat(response.getJSONObject("data").getJSONObject("data"))
				}
			}
		}
		val binding = FragmentEnergyOrderBinding.inflate(inflater, container, false).apply {
			recyclerViewScroll.root.layoutManager = LinearLayoutManager(requireContext())
			recyclerViewScroll.root.adapter = adapter
		}
		userInfo
		return binding.root
	}
	
	val userInfo: Unit
		get() {
			model.addAndNext("kbp/auth/userInfo", 0)
		}
	
	fun getRoom(username: String?) {
		model.addAndNext("kbp/admin/sys/personRoom/list", "{\"username\":\"$username\"}", 1)
	}
	
	fun getBalance(room: String?) {
		model.addAndNext("kbp/pay/roomBalance?roomCode=$room", 2)
	}
	
	fun recharge(amount: Int, room: String?, remark: String?) {
		model.addAndNext("kbp/pay/recharge/zdPay", "{\"payAmount\":$amount,\"body\":\"房间钱包充值\",\"rechargeChannel\":6,\"accountType\":7,\"rechargeType\":7,\"params\":{\"roomCode\":\"$room\"},\"remark\":\"$remark\"}", 3)
	}
	
	fun gotoWechat(data: JSONObject) {
		val form = FormBody.Builder()
		data.forEach { (key: String?, value: Any?) ->
			key?.let { form.add(it, "$value") }
		}
		OkHttpClient.Builder()
			.followRedirects(false)
			.build()
			.newCall(model.http.generateRequest("https://fee.sysu.edu.cn/gateway/unifiedorder/pagepay", null, null).post(form.build()).header("Content-Type", "application/x-www-form-urlencoded").build())
			.enqueue(object : Callback {
				override fun onFailure(call: Call, e: IOException) {
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
}
