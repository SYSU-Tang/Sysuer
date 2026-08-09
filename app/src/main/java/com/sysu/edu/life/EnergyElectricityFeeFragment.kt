package com.sysu.edu.life

import android.os.Bundle
import android.util.ArraySet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import com.alibaba.fastjson2.JSONObject
import com.haibin.calendarview.Calendar
import com.sysu.edu.BaseFragment
import com.sysu.edu.R
import com.sysu.edu.api.CommonUtil
import com.sysu.edu.api.CommonUtil.extractValue
import com.sysu.edu.databinding.FragmentWaterFeeBinding
import com.sysu.edu.model.ZhnyModel
import com.sysu.edu.todo.TitleAdapter
import com.sysu.edu.view.ButtonAdapter
import com.sysu.edu.view.PreferenceAdapter
import com.sysu.edu.view.PreferenceDialog
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale

class EnergyElectricityFeeFragment : BaseFragment() {
	val model: ZhnyModel by lazy {
		ZhnyModel(requireContext())
	}
	
	override fun onCreateView(inflater: LayoutInflater,
	                          container: ViewGroup?,
	                          savedInstanceState: Bundle?): View {
		super.onCreateView(inflater, container, savedInstanceState)
		val adapter = ConcatAdapter()
		fun reset() {
			adapter.adapters.forEach { adapter.removeAdapter(it) }
		}
		
		val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
		val rooms: ArraySet<CommonUtil.Tuple2<String?, String?>?> = ArraySet<CommonUtil.Tuple2<String?, String?>?>()
		val roomCode: MutableLiveData<String?> = MutableLiveData<String?>()
		val paymentStatus = resources.getStringArray(R.array.payment_status)
		val binding = FragmentWaterFeeBinding.inflate(inflater, container, false).apply {
			list.layoutManager = LinearLayoutManager(requireContext())
			list.adapter = adapter
			calendarView.setOnMonthChangeListener { year: Int, month: Int ->
				getElectricityConsumption(roomCode.value!!, LocalDate.of(year, month, 1)
					.with(TemporalAdjusters.firstDayOfMonth())
					.format(formatter), LocalDate.of(year, month, 1)
											  .with(TemporalAdjusters.lastDayOfMonth())
											  .format(formatter))
				date.text = LocalDate.of(year, month, 1).format(formatter)
			}
			date.text = LocalDate.now().format(formatter)
			spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
				override fun onItemSelected(parent: AdapterView<*>?,
				                            view: View?,
				                            position: Int,
				                            id: Long) {
					roomCode.value = rooms.valueAt(position)!!.second
				}
				
				override fun onNothingSelected(parent: AdapterView<*>?) {
				}
			}
		}
		val detailDialog = PreferenceDialog(requireContext())
		model.message.observe(getViewLifecycleOwner()) { (code, response) ->
			if (response.getInteger("code") == 200) {
				when (code) {
					0 -> getRoom(response.getJSONObject("data").getString("username"))
					1 -> {
						val items = ArrayAdapter<Any?>(requireContext(), android.R.layout.simple_list_item_1)
						binding.spinner.setAdapter(items)
						response.getJSONArray("data").forEach { e: Any? ->
							val roomInfo = e as JSONObject
							rooms.add(CommonUtil.Tuple2(roomInfo.getString("roomName"), roomInfo.getString("roomCode")))
							items.add(roomInfo.getString("roomName"))
						}
					}
					2 -> response.getJSONObject("data")
						.getJSONArray("useEleByDayList")
						.forEach { item: Any? ->
							val useElectric = (item as JSONObject).get("useElectric")
							val content = useElectric?.toString() ?: getString(R.string.no_data_available)
							val date = LocalDate.parse(item.getString("date"), DateTimeFormatter.ofPattern("yyyy-MM-dd"))
							val calendar = Calendar().apply {
								scheme = content
								year = date.year
								month = date.monthValue
								day = date.dayOfMonth
							}
							binding.calendarView.addSchemeDate(calendar)
						}
					3 -> {
						reset()
						response.getJSONObject("data").getJSONArray("list").forEach { item: Any? ->
							adapter.addAdapter(TitleAdapter((item as JSONObject).getString("billPeriod")))
							val preferenceAdapter = PreferenceAdapter()
							val value: ArrayList<String?> = extractValue(item, arrayOf("billPeriod", "billStatus", "remark", "useElectric", "name", "campusName", "areaInfo", "unitPrice", "totalUseAmount", "payedUseAmount", "billTime"))
							val billStatus = item.getInteger("billStatus")
							value[1] = paymentStatus[billStatus - 1]
							value[3] = "${item.getString("currReportElectric")}-${item.getString("lastReportElectric")}=${item.getString("useElectric")}"
							preferenceAdapter.set(mutableListOf(R.string.bill_period, R.string.status, R.string.remark, R.string.electricity_consumption, R.string.payer, R.string.campus, R.string.dorm, R.string.price, R.string.fee, R.string.paid_fee, R.string.pay_time), value, mutableListOf(R.drawable.calendar, if (billStatus == 3 || billStatus == 5) R.drawable.check else R.drawable.uncheck, R.drawable.text, R.drawable.flash, R.drawable.account, R.drawable.location, R.drawable.home, R.drawable.money, R.drawable.money, R.drawable.money, R.drawable.time), requireContext())
							preferenceAdapter.hideNull = true
							val buttonAdapter = ButtonAdapter().apply {
								add(getString(R.string.view_detail))
								if (billStatus == 1) add(getString(R.string.pay_fee))
								setListener { button: Button?, position: Int ->
									when (position) {
										0 -> button!!.setOnClickListener { getDetail(item.getString("id"), roomCode.value) }
										1 -> button!!.setOnClickListener { recharge(item.getString("id"), roomCode.value!!, item.getFloat("totalUseAmount")) }
									}
								}
							}
							
							adapter.addAdapter(preferenceAdapter)
							adapter.addAdapter(buttonAdapter)
						}
					}
					4 -> {
						detailDialog.clear()
						response.getJSONObject("data").getJSONArray("list").forEach { item: Any? ->
							val value: ArrayList<String?> = extractValue((item as JSONObject), arrayOf("billPeriod", "billStatus", "remark", "useElectric", "name", "campusName", "areaInfo", "unitPrice", "totalUseAmount", "payedUseAmount", "useAmount", "billTime"))
							val billStatus = item.getInteger("billStatus")
							value[1] = paymentStatus[billStatus - 1]
							value[3] = "${item.getString("currReportElectric")}-${item.getString("lastReportElectric")}=${item.getString("useElectric")}"
							detailDialog.getAdapter()
								.set(mutableListOf(R.string.bill_period, R.string.status, R.string.remark, R.string.electricity_consumption, R.string.payer, R.string.campus, R.string.dorm, R.string.price, R.string.fee, R.string.paid_fee, R.string.unpaid_fee, R.string.pay_time), value, mutableListOf(R.drawable.calendar, if (billStatus == 3 || billStatus == 5) R.drawable.check else R.drawable.uncheck, R.drawable.text, R.drawable.flash, R.drawable.account, R.drawable.location, R.drawable.home, R.drawable.money, R.drawable.money, R.drawable.money, R.drawable.money, R.drawable.time), requireContext())
							detailDialog.getAdapter().hideNull = true
						}
						detailDialog.show()
					}
					5 -> {
						config.toast(response.getString("msg"))
						getElectricityBill(roomCode.value!!)
					}
				}
			}
			else config.toast(response.getString("msg"))
		}
		roomCode.observe(viewLifecycleOwner) { v: String? ->
			v?.takeUnless { it.isEmpty() }?.let {
				val date = LocalDate.of(binding.calendarView.selectedCalendar.year, binding.calendarView.selectedCalendar.month, 1)
				getElectricityConsumption(it, date.with(TemporalAdjusters.firstDayOfMonth())
					.format(formatter), date.with(TemporalAdjusters.lastDayOfMonth())
											  .format(formatter))
				getElectricityBill(it)
			}
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
	
	fun getElectricityConsumption(roomCode: String, startDate: String?, endDate: String?) {
		model.addAndNext("kbp/ele/wechat/eleConsume", "{\"roomCode\":\"$roomCode\",\"startDate\":\"$startDate\",\"endDate\":\"$endDate\"}", 2)
	}
	
	fun getElectricityBill(roomCode: String) {
		model.addAndNext("kbp/ele/mobile/billRecord", "{\"roomCode\":\"$roomCode\",\"billType\":1}", 3)
	}
	
	fun getDetail(id: String, room: String?) {
		model.addAndNext("kbp/ele/mobile/billRecord", "{\"id\":\"$id\",\"roomCode\":\"$room\"}", 4)
	}
	
	fun recharge(id: String?, roomCode: String, amount: Float) {
		model.addAndNext("kbp/ele/mobile/pay/bill/recharge", String.format(Locale.getDefault(), "{\"roomCode\":\"%s\",\"actualBillAmount\":%.2f,\"useTypeEleAndMoneyList\":[{\"billAmount\":%.2f,\"useEleType\":1,\"idList\":[\"%s\"]}],\"rechargeType\":16}", roomCode, amount, amount, id), 5)
	}
}
