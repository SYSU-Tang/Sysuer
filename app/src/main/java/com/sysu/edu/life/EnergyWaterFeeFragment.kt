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
import androidx.lifecycle.Observer
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
import com.sysu.edu.view.FeeMonthView
import com.sysu.edu.view.FeeWeekView
import com.sysu.edu.view.PreferenceAdapter
import com.sysu.edu.view.PreferenceDialog
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class EnergyWaterFeeFragment : BaseFragment() {
	val rooms: ArraySet<CommonUtil.Tuple2<String?, String?>?> = ArraySet<CommonUtil.Tuple2<String?, String?>?>()
	val roomCode: MutableLiveData<String?> = MutableLiveData<String?>()
	val model: ZhnyModel by lazy { ZhnyModel(requireContext()) }
	override fun onCreateView(inflater: LayoutInflater,
	                          container: ViewGroup?,
	                          savedInstanceState: Bundle?): View {
		super.onCreateView(inflater, container, savedInstanceState)
		val adapter = ConcatAdapter()
		val formatter = DateTimeFormatter.ofPattern("yyyy-MM")
		fun reset() {
			adapter.adapters.forEach { adapter.removeAdapter(it) }
		}
		
		val binding = FragmentWaterFeeBinding.inflate(inflater, container, false).apply {
			list.layoutManager = LinearLayoutManager(requireContext())
			list.adapter = adapter
			calendarView.setMonthView(FeeMonthView::class.java)
			calendarView.setWeekView(FeeWeekView::class.java)
			calendarView.setOnMonthChangeListener { year: Int, month: Int ->
				roomCode.value?.let {
					getWaterConsumption(it, LocalDate.of(year, month, 1).format(formatter))
				}
				date.text = LocalDate.of(year, month, 1).format(formatter)
			}
			date.text = LocalDate.now().format(formatter)
			spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
				override fun onItemSelected(parent: AdapterView<*>?,
				                            view: View?,
				                            position: Int,
				                            id: Long) {
					reset()
					roomCode.value = rooms.valueAt(position)!!.second
				}
				
				override fun onNothingSelected(parent: AdapterView<*>?) {
				}
			}
		}
		val detailDialog = PreferenceDialog(requireContext())
		val paymentStatuses = resources.getStringArray(R.array.payment_status)
		model.message.observe(getViewLifecycleOwner()) { (code, response) ->
			if (response.getInteger("code") == 200) {
				when (code) {
					0 -> getRoom(response.getJSONObject("data").getString("username"))
					1 -> {
						val items = ArrayAdapter<Any?>(requireContext(), android.R.layout.simple_list_item_1)
						binding.spinner.setAdapter(items)
						response.getJSONArray("data").forEach {
							rooms.add(CommonUtil.Tuple2((it as JSONObject).getString("roomName"), it.getString("roomCode")))
							items.add(it.getString("roomName"))
						}
					}
					2 -> {
						val preferenceAdapter = PreferenceAdapter()
						response.getJSONObject("data")
							.getJSONArray("waterUsageList")
							.forEach { item: Any? ->
								val totalWaterUsage: Any? = (item as JSONObject).getString("totalWaterUsage")
								val content = totalWaterUsage?.toString()
									?: getString(R.string.no_data_available)
								val calendar = Calendar()
								calendar.scheme = content
								calendar.year = binding.calendarView.selectedCalendar.year
								calendar.month = binding.calendarView.selectedCalendar.month
								calendar.day = item.getString("timeLabel").toInt()
								binding.calendarView.addSchemeDate(calendar)
							}
						adapter.addAdapter(preferenceAdapter)
					}
					3 -> {
						reset()
						response.getJSONObject("data")
							.getJSONArray("billList")
							.forEach { item: Any? ->
								val duration = "${(item as JSONObject).getString("originalBillStartDate")}~${item.getString("originalBillEndDate")}"
								adapter.addAdapter(TitleAdapter(duration))
								val preferenceAdapter = PreferenceAdapter()
								val value: ArrayList<String?> = extractValue(item, arrayOf("billStartDate", "paymentStatus", "useWaterTypeName", "finalWaterUsage", "waterPayment", "paidPayment"))
								val paymentStatus = item.getInteger("paymentStatus")
								value[0] = duration
								value[1] = paymentStatuses[paymentStatus - 1]
								preferenceAdapter.set(mutableListOf(R.string.bill_period, R.string.status, R.string.type, R.string.electricity_consumption, R.string.fee, R.string.paid_fee), value, mutableListOf(R.drawable.calendar, if (paymentStatus == 3 || paymentStatus == 5) R.drawable.check else R.drawable.uncheck, R.drawable.water, R.drawable.water, R.drawable.money, R.drawable.money), requireContext())
								preferenceAdapter.hideNull = true
								val buttonAdapter = ButtonAdapter()
								buttonAdapter.add(getString(R.string.view_detail))
								if (paymentStatus == 1) buttonAdapter.add(getString(R.string.pay))
								buttonAdapter.setListener { button: Button?, position: Int ->
									when (position) {
										0 -> button!!.setOnClickListener { getDetail(item.getString("id")) }
										1 -> button!!.setOnClickListener { recharge(item.getString("id"), roomCode.value!!, item.getFloat("waterPayment")) }
									}
								}
								adapter.addAdapter(preferenceAdapter)
								adapter.addAdapter(buttonAdapter)
							}
					}
					4 -> {
						detailDialog.clear()
						val item = response.getJSONObject("data")
						val value: ArrayList<String?> = extractValue(item, arrayOf("billStartDate", "billStatus", "remark", "finalWaterUsage", "useWaterTypeName", "areaInfo", "unitPrice", "waterPayment", "paidPayment", "unpaidPayment", "createTime"))
						val billStatus = item.getInteger("paymentStatus")
						value[0] = "${item.getString("billStartDate")}~${item.getString("billEndDate")}"
						value[2] = item.getString("remark")?.let { "-" } ?: item.getString("remark")
						value[1] = paymentStatuses[billStatus - 1]
						value[3] = "${item.getString("currMeterReading")}-${item.getString("lastMeterReading")}=${item.getString("finalWaterUsage")}"
						detailDialog.getAdapter()
							.set(mutableListOf(R.string.bill_period, R.string.status, R.string.remark, R.string.water_consumption, R.string.type, R.string.dorm, R.string.price, R.string.fee, R.string.paid_fee, R.string.unpaid_fee, R.string.pay_time), value, mutableListOf(R.drawable.calendar, if (billStatus == 3 || billStatus == 5) R.drawable.check else R.drawable.uncheck, R.drawable.text, R.drawable.menu, R.drawable.dashboard, R.drawable.home, R.drawable.money, R.drawable.money, R.drawable.money, R.drawable.money, R.drawable.time), requireContext())
						detailDialog.show()
					}
					5 -> {
						config.toast(response.getString("msg"))
						getWaterBill(roomCode.value!!)
					}
				}
				model.nextAll()
			}
			else if (response.getInteger("code") == 201) config.toast(response.getString("msg", ""))
		}
		userInfo
		roomCode.observe(getViewLifecycleOwner(), Observer { v: String? ->
			v?.let {
				getWaterConsumption(it, LocalDate.of(binding.calendarView.selectedCalendar.year, binding.calendarView.selectedCalendar.month, 1)
					.format(formatter))
				getWaterBill(it)
			}
		})
		return binding.root
	}
	
	val userInfo: Unit
		get() {
			model.addAndNext("kbp/auth/userInfo", 0)
		}
	
	fun getRoom(username: String?) {
		model.add("kbp/admin/sys/personRoom/list", "{\"username\":\"$username\"}", 1)
	}
	
	fun getWaterConsumption(room: String, date: String?) {
		model.add("kbp/cwbs/month/usage/stats", "{\"roomCode\":\"$room\",\"staticsMonth\":\"$date\"}", 2)
	}
	
	fun getWaterBill(room: String) {
		model.add("kbp/cwbs/mobile/room/bill/list", "{\"roomCode\":\"$room\"}", 3)
	}
	
	fun getDetail(billId: String?) {
		model.addAndNext("kbp/cwbs/mobile/room/bill/get/$billId", 4)
	}
	
	fun recharge(billId: String?, room: String, amount: Float) {
		model.addAndNext("kbp/cwbs/mobile/room/bill/pay", "{\"roomCode\":\"$room\",\"billAmount\":$amount,\"idList\":[\"$billId\"],\"isMobile\":true,\"rechargeChannel\":6,\"rechargeMethod\":16}", 5)
	}
}
