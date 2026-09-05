package com.miyuyan.sysuer.life

import android.os.Bundle
import android.util.ArraySet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import com.alibaba.fastjson2.JSONObject
import com.miyuyan.sysuer.BaseFragment
import com.miyuyan.sysuer.R
import com.miyuyan.sysuer.api.CommonUtil
import com.miyuyan.sysuer.api.CommonUtil.extractValue
import com.miyuyan.sysuer.databinding.FragmentEnergyDashboardBinding
import com.miyuyan.sysuer.model.ZhnyModel
import com.miyuyan.sysuer.todo.TitleAdapter
import com.miyuyan.sysuer.view.PreferenceAdapter
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

class EnergyDashboardFragment : BaseFragment() {
	val model: ZhnyModel by lazy { ZhnyModel(requireContext()) }
	override fun onCreateView(inflater: LayoutInflater,
	                          container: ViewGroup?,
	                          savedInstanceState: Bundle?): View {
		super.onCreateView(inflater, container, savedInstanceState)
		val adapter = ConcatAdapter()
		fun reset() {
			adapter.adapters.forEach { adapter.removeAdapter(it) }
		}
		
		val binding = FragmentEnergyDashboardBinding.inflate(inflater, container, false).apply {
			list.layoutManager = LinearLayoutManager(requireContext())
			list.adapter = adapter
		}
		model.message.observe(getViewLifecycleOwner()) { (code, response) ->
//			println("code = $code , response = $response")
			if (response.getInteger("code") == 200) {
				val data = response.getJSONObject("data")
				when (code) {
					0 -> {
						val name = data.getString("username")
						waterInfo
						getElectricityInfo(name)
						getRoom(name)
					}
					1 -> {
						binding.lastMonthElectricity.text = String.format(Locale.getDefault(), "上月用电\n%.2f", data.getDouble("lastMonthUsage"))
						binding.thisMonthElectricity.text = String.format(Locale.getDefault(), "本月用电\n%.2f", data.getDouble("currentMonthUsage"))
						binding.deltaElectricity.text = String.format(Locale.getDefault(), "用电变化\n%.2f", data.getDouble("usageChange"))
					}
					2 -> {
						binding.lastMonthWater.text = String.format(Locale.getDefault(), "上月用水\n%.2f", data.getDouble("lastMonthUsage"))
						binding.thisMonthWater.text = String.format(Locale.getDefault(), "本月用水\n%.2f", data.getDouble("thisMonthUsage"))
						binding.deltaWater.text = String.format(Locale.getDefault(), "用水变化\n%.2f", data.getDouble("growthUsage"))
					}
					3 -> {
						reset()
						response.getJSONObject("data")
							.getJSONArray("records")
							.forEach { item: Any? ->
								val titleAdapter = TitleAdapter((item as JSONObject).getString("date"))
								titleAdapter.header = 2
								adapter.addAdapter(titleAdapter)
								item.getJSONArray("detailRecords").forEach { detail: Any? ->
									adapter.addAdapter(TitleAdapter((detail as JSONObject).getString("tradeTypeDesc")))
									val preferenceAdapter = PreferenceAdapter()
									preferenceAdapter.set(mutableListOf(R.string.type, R.string.time, R.string.fee, R.string.payer, R.string.student_id), extractValue(detail, arrayOf("tradeTypeDesc", "tradeTime", "tradeAmount", "name", "username", "paidPayment")), mutableListOf(R.drawable.menu, R.drawable.time, R.drawable.money, R.drawable.account, R.drawable.id), requireContext())
									preferenceAdapter.hideNull = true
									adapter.addAdapter(preferenceAdapter)
								}
							}
					}
					4 -> {
						val rooms: ArraySet<CommonUtil.Tuple2<String?, String?>?> = ArraySet<CommonUtil.Tuple2<String?, String?>?>()
						val items = ArrayAdapter<Any?>(requireContext(), android.R.layout.simple_list_item_1)
						response.getJSONArray("data").forEach { roomInfo: Any? ->
							rooms.add(CommonUtil.Tuple2((roomInfo as JSONObject).getString("roomName"), roomInfo.getString("roomCode")))
							items.add(roomInfo.getString("roomName"))
						}
						if (!rooms.isEmpty()) getOrderInfo(rooms.valueAt(0)!!.second, LocalDate.now()
							.format(DateTimeFormatter.ofPattern("yyyy-MM")))
					}
				}
			}
		}
		userInfo
		return binding.root
	}
	
	val userInfo: Unit
		get() {
			model.addAndNext("kbp/auth/userInfo", 0)
		}
	
	fun getElectricityInfo(username: String?) {
		model.addAndNext("kbp/ele/wechat/eleSituation?username=$username", 1)
	}
	
	val waterInfo: Unit
		get() {
			model.addAndNext("kbp/cwbs/user/usage/stats", "",2)
		}
	
	fun getOrderInfo(room: String?, date: String?) {
		model.addAndNext("kbp/record/roomBalance/detail", "{\"dateType\":\"month\",\"roomCode\":\"$room\",\"dateRange\":\"$date\",\"id\":null,\"tradeTime\":\"\"}", 3)
	}
	
	fun getRoom(username: String?) {
		model.addAndNext("kbp/admin/sys/personRoom/list", "{\"username\":\"$username\"}", 4)
	}
}
