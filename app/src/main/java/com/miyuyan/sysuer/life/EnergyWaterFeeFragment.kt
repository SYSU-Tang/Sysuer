package com.miyuyan.sysuer.life

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import com.alibaba.fastjson2.JSONObject
import com.haibin.calendarview.Calendar
import com.miyuyan.sysuer.R
import com.miyuyan.sysuer.api.CommonUtil
import com.miyuyan.sysuer.api.CommonUtil.extractValue
import com.miyuyan.sysuer.databinding.FragmentWaterFeeBinding
import com.miyuyan.sysuer.model.ZhnyModel
import com.miyuyan.sysuer.todo.TitleAdapter
import com.miyuyan.sysuer.view.ButtonAdapter
import com.miyuyan.sysuer.view.FeeMonthView
import com.miyuyan.sysuer.view.FeeWeekView
import com.miyuyan.sysuer.view.PreferenceAdapter
import com.miyuyan.sysuer.view.PreferenceDialog
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class EnergyWaterFeeFragment : EnergyBaseFragment() {
	val roomCode: MutableLiveData<String?> = MutableLiveData<String?>()
	override val model: ZhnyModel by lazy { ZhnyModel(requireContext()) }
	override fun onCreateView(
		inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
	): View {
		super.onCreateView(inflater, container, savedInstanceState)
		val adapter = ConcatAdapter()
		val formatter = DateTimeFormatter.ofPattern("yyyy-MM")
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
				override fun onItemSelected(
					parent: AdapterView<*>?, view: View?, position: Int, id: Long
				) {
					resetAdapter(adapter)
					roomCode.value = rooms.valueAt(position)!!.second
				}

				override fun onNothingSelected(parent: AdapterView<*>?) {
				}
			}
		}
		val detailDialog = PreferenceDialog(requireContext())
		val paymentStatuses = resources.getStringArray(R.array.payment_status)
		viewLifecycleOwner.lifecycleScope.launch {
			viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
				model.message.collect { (code, response) ->
					if (response.getInteger("code") == 200) {
						when (code) {
							0 -> getRoom(response.getJSONObject("data").getString("username"))
							1 -> {
								val items = ArrayAdapter<Any?>(
										requireContext(), android.R.layout.simple_list_item_1
								)
								binding.spinner.setAdapter(items)
								response.getJSONArray("data").forEach {
									rooms.add(
											CommonUtil.Tuple2(
													(it as JSONObject).getString("roomName"),
													it.getString("roomCode")
											)
									)
									items.add(it.getString("roomName"))
								}

							}

							2 -> {
								val preferenceAdapter = PreferenceAdapter()
								println(response)
								response.getJSONObject("data").getJSONArray("waterUsageList")
									.forEach { item: Any? ->
										val totalWaterUsage: Any? =
											(item as JSONObject).getString("totalWaterUsage")
										val content = totalWaterUsage?.toString()
											?: getString(R.string.no_data_available)
										val calendar = Calendar().apply {
											scheme = content
											year = binding.calendarView.selectedCalendar.year
											month = binding.calendarView.selectedCalendar.month
											day = item.getInteger("timeLabel")
										}
										binding.calendarView.addSchemeDate(calendar)
									}
								adapter.addAdapter(preferenceAdapter)
							}

							3 -> {
							resetAdapter(adapter)
								response.getJSONObject("data").getJSONArray("billList")
									.forEach { item: Any? ->
										val duration =
											"${(item as JSONObject).getString("originalBillStartDate")}~${
												item.getString("originalBillEndDate")
											}"
										adapter.addAdapter(TitleAdapter(duration))
										val preferenceAdapter = PreferenceAdapter()
										val value: ArrayList<String?> = extractValue(
												item, arrayOf(
												"billStartDate",
												"paymentStatus",
												"useWaterTypeName",
												"finalWaterUsage",
												"waterPayment",
												"paidPayment"
										)
										)
										val paymentStatus = item.getInteger("paymentStatus")
										value[0] = duration
						value[1] = paymentStatuses.getOrNull(paymentStatus - 1) ?: getString(R.string.none)
										preferenceAdapter.set(
												mutableListOf(
														R.string.bill_period,
														R.string.status,
														R.string.type,
														R.string.electricity_consumption,
														R.string.fee,
														R.string.paid_fee
												), value, mutableListOf(
												R.drawable.calendar,
												if (paymentStatus == 3 || paymentStatus == 5) R.drawable.check else R.drawable.uncheck,
												R.drawable.water,
												R.drawable.water,
												R.drawable.money,
												R.drawable.money
										), requireContext()
										)
										preferenceAdapter.hideNull = true
										val buttonAdapter = ButtonAdapter()
										buttonAdapter.add(getString(R.string.view_detail))
										if (paymentStatus == 1) buttonAdapter.add(getString(R.string.pay_fee))
										buttonAdapter.setListener { button: Button?, position: Int ->
											when (position) {
												0 -> button!!.setOnClickListener {
													getDetail(
															item.getString(
																	"id"
															)
													)
												}

												1 -> button!!.setOnClickListener {
													recharge(
															item.getString(
																	"id"
															),
															roomCode.value!!,
															item.getFloat("waterPayment")
													)
												}
											}
										}
										adapter.addAdapter(preferenceAdapter)
										adapter.addAdapter(buttonAdapter)
									}
							}

							4 -> {
								detailDialog.clear()
								val item = response.getJSONObject("data")
								val value: ArrayList<String?> = extractValue(
										item, arrayOf(
										"billStartDate",
										"billStatus",
										"remark",
										"finalWaterUsage",
										"useWaterTypeName",
										"areaInfo",
										"unitPrice",
										"waterPayment",
										"paidPayment",
										"unpaidPayment",
										"createTime"
								)
								)
								val billStatus = item.getInteger("paymentStatus")
								value[0] =
									"${item.getString("billStartDate")}~${item.getString("billEndDate")}"
					value[2] = item.getString("remark") ?: "-"
					value[1] = paymentStatuses.getOrNull(billStatus - 1) ?: getString(R.string.none)
								value[3] =
									"${item.getString("currMeterReading")}-${item.getString("lastMeterReading")}=${
										item.getString("finalWaterUsage")
									}"
								detailDialog.getAdapter().set(
										mutableListOf(
												R.string.bill_period,
												R.string.status,
												R.string.remark,
												R.string.water_consumption,
												R.string.type,
												R.string.dorm,
												R.string.price,
												R.string.fee,
												R.string.paid_fee,
												R.string.unpaid_fee,
												R.string.pay_time
										), value, mutableListOf(
										R.drawable.calendar,
										if (billStatus == 3 || billStatus == 5) R.drawable.check else R.drawable.uncheck,
										R.drawable.text,
										R.drawable.menu,
										R.drawable.dashboard,
										R.drawable.home,
										R.drawable.money,
										R.drawable.money,
										R.drawable.money,
										R.drawable.money,
										R.drawable.time
								), requireContext()
								)
								detailDialog.show()
							}

							5 -> {
								config.toast(response.getString("msg"))
								getWaterBill(roomCode.value!!)
							}
						}
					} else if (response.getInteger("code") == 201) config.toast(
							response.getString(
									"msg", ""
							)
					)
				}
			}
		}
		loadUserInfo()
		roomCode.observe(viewLifecycleOwner) { v: String? ->
			v?.let {
				getWaterConsumption(
						it, LocalDate.of(
						binding.calendarView.selectedCalendar.year,
						binding.calendarView.selectedCalendar.month,
						1
				).format(formatter)
				)
				getWaterBill(it)
			}
		}
		return binding.root
	}


	fun getWaterConsumption(room: String, date: String?) {
		model.enqueue(
			"kbp/cwbs/month/usage/stats",
			JSONObject.of("roomCode", room, "staticsMonth", date).toJSONString(),
			2
		)
	}

	fun getWaterBill(room: String) {
		model.enqueue("kbp/cwbs/mobile/room/bill/list", JSONObject.of("roomCode", room).toJSONString(), 3)
	}

	fun getDetail(billId: String?) {
		model.enqueue("kbp/cwbs/mobile/room/bill/get/$billId", 4)
	}

	fun recharge(billId: String?, room: String, amount: Float) {
		model.enqueue(
				"kbp/cwbs/mobile/room/bill/pay",
				"{\"roomCode\":\"$room\",\"billAmount\":$amount,\"idList\":[\"$billId\"],\"isMobile\":true,\"rechargeChannel\":6,\"rechargeMethod\":16}",
				5
		)
	}
	override fun onDestroyView() {
		super.onDestroyView()
		model.dispose()
	}
}
