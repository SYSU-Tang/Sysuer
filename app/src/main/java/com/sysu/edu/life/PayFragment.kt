package com.sysu.edu.life

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import androidx.core.view.size
import androidx.recyclerview.widget.RecyclerView
import com.alibaba.fastjson2.JSONObject
import com.google.android.material.chip.ChipGroup
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.CompositeDateValidator
import com.google.android.material.datepicker.DateValidatorPointBackward
import com.google.android.material.datepicker.DateValidatorPointForward
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.datepicker.MaterialPickerOnPositiveButtonClickListener
import com.sysu.edu.R
import com.sysu.edu.api.CalendarManager
import com.sysu.edu.api.CommonUtil.extractValue
import com.sysu.edu.databinding.FragmentPayNeedBinding
import com.sysu.edu.databinding.FragmentPayRecordBinding
import com.sysu.edu.databinding.FragmentPaySituationBinding
import com.sysu.edu.databinding.ItemFilterChipBinding
import com.sysu.edu.model.PayModel
import com.sysu.edu.view.StaggeredFragment
import java.time.LocalDate

class PayFragment : StaggeredFragment() {
	
	val model: PayModel by lazy { PayModel(requireContext()) }
	val calendarManager: CalendarManager = CalendarManager()
	override fun onCreateView(inflater: LayoutInflater,
	                          container: ViewGroup?,
	                          savedInstanceState: Bundle?): View? {
		var view = super.onCreateView(inflater,
		                              container,
		                              savedInstanceState)        //config.setCallback { page }
		var order = 0
		when (position) {
			0 -> {
				val needBinding = FragmentPayNeedBinding.inflate(inflater).apply {
					root.addView(view)
					pay.setOnClickListener(config.browse("https://pay.sysu.edu.cn/#/confirm/pay-ticket?type=1"))
				}
				binding?.recyclerView?.addOnScrollListener(object : RecyclerView.OnScrollListener() {
					override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
						needBinding.chips.elevation = (if (recyclerView.canScrollVertically(-1)) 6 else 0).toFloat()
						super.onScrolled(recyclerView, dx, dy)
					}
				})
				view = needBinding.root
			}
			2 -> {
				val years = mutableListOf(getString(R.string.all),
				                          getString(R.string.no_interval_year))
				val yearCodes = mutableListOf<String?>("null", "-1")
				(0..5).forEach { i ->
					val year = (calendarManager.year + 1 - i).toString()
					years.add(year)
					yearCodes.add(year)
				}
				val fragmentPaySituationBinding = FragmentPaySituationBinding.inflate(inflater)
					.apply {
						root.addView(view)
						spinner.setText(calendarManager.year.toString())
						spinner.setSimpleItems(years.toTypedArray())
						spinner.setOnItemClickListener { _: AdapterView<*>?, _: View?, i: Int, _: Long ->
							clear()
							getFeeList("${yearCodes[i]}")
						}
						view = root
					}
				binding?.recyclerView?.addOnScrollListener(object : RecyclerView.OnScrollListener() {
					override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
						fragmentPaySituationBinding.p.elevation = (if (recyclerView.canScrollVertically(
								-1)) 6
						else 0).toFloat()
						super.onScrolled(recyclerView, dx, dy)
					}
				})
			}
			3 -> {
				val dm = DateManager()
				dm.fromDate = calendarManager.firstOfMonth
				dm.toDate = calendarManager.endOfMonth
				val fragmentPayRecordBinding = FragmentPayRecordBinding.inflate(inflater)
				fragmentPayRecordBinding.root.addView(view)
				view = fragmentPayRecordBinding.root
				fragmentPayRecordBinding.from.text = dm.fromDateString
				fragmentPayRecordBinding.from.setOnClickListener {
					val fromDatePicker = MaterialDatePicker.Builder.datePicker()
						.setSelection(dm.fromDateTimeMillis)
						.setCalendarConstraints(CalendarConstraints.Builder()
							                        .setValidator(CompositeDateValidator.allOf(
								                        listOf(DateValidatorPointBackward.before(dm.toDateTimeMillis))))
							                        .build())
						.build()
					fromDatePicker.addOnPositiveButtonClickListener(
						MaterialPickerOnPositiveButtonClickListener { selection: Long? ->
							dm.fromDateTimeMillis = selection!!
							fromDatePicker.dismissAllowingStateLoss()
							fragmentPayRecordBinding.from.text = dm.fromDateString
							dm.data
						})
					fromDatePicker.show(requireActivity().supportFragmentManager, null)
				}
				fragmentPayRecordBinding.to.text = dm.toDateString
				fragmentPayRecordBinding.to.setOnClickListener {
					val toDatePicker = MaterialDatePicker.Builder.datePicker()
						.setSelection(dm.toDateTimeMillis)
						.setCalendarConstraints(CalendarConstraints.Builder()
							                        .setValidator(CompositeDateValidator.allOf(
								                        listOf(DateValidatorPointForward.from(dm.fromDateTimeMillis))))
							                        .build())
						.build()
					toDatePicker.addOnPositiveButtonClickListener(
						MaterialPickerOnPositiveButtonClickListener { selection: Long? ->
							dm.toDateTimeMillis = selection!!
							toDatePicker.dismissAllowingStateLoss()
							fragmentPayRecordBinding.to.text = dm.toDateString
							dm.data
						})
					toDatePicker.show(requireActivity().supportFragmentManager, null)
				}
				binding?.recyclerView?.addOnScrollListener(object : RecyclerView.OnScrollListener() {
					override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
						fragmentPayRecordBinding.row.elevation = (if (recyclerView.canScrollVertically(
								-1)) config.dpToPx(2)
						else 0).toFloat()
					}
				})
			}
		}
		model.message.observe(viewLifecycleOwner) { (code, response) ->
			if (response.getInteger("code") == 200) {
				if (response.get("data") != null) {
					clear()
					val data = response.getJSONArray("data")
					when (code) {
						0, 1 -> data.forEach {
							add((it as JSONObject).getString("itemName"),
							    mutableListOf("学号", "交费区间", "当前应交", "本次交费"),
							    extractValue(it,
							                 arrayOf("personCode",
							                         "intervalName",
							                         "nowMoney",
							                         "needMoney")))
						}
						2 -> data.forEach { a: Any? ->
							add((a as JSONObject).getString("itemName"),
							    mutableListOf("学号",
							                  "收费项目",
							                  "交费区间",
							                  "应交",
							                  "缓交",
							                  "实交"),
							    extractValue(a,
							                 arrayOf("personCode",
							                         "itemName",
							                         "intervalName",
							                         "needPay",
							                         "laterPay",
							                         "realPay")))
						}
						3 -> data.forEach { a: Any? ->
							add("${++order}",
							    mutableListOf("订单编号",
							                  "金额",
							                  "支付方式",
							                  "支付时间",
							                  "支付编号"),
							    extractValue(a as JSONObject,
							                 arrayOf("orderNo",
							                         "money",
							                         "payTypeName",
							                         "payTime",
							                         "outPayNo")))
						}
						4 -> data.forEach { a: Any? ->
							add("${++order}",
							    mutableListOf("收费项目",
							                  "收费区间",
							                  "退费金额",
							                  "退费日期",
							                  "退费状态"),
							    extractValue(a as JSONObject,
							                 arrayOf("itemName",
							                         "intervalName",
							                         "refundMoney",
							                         "refundDate",
							                         "refundStateStr")))
						}
					}
				}
			}
			else config.toast(response.getString("message"))
		}
		page
		return view
	}
	
	override fun add(title: String?,
	                 icon: Int?,
	                 keys: MutableList<String?>,
	                 values: MutableList<String?>) {
		super.add(title, icon, keys, values)
		if (position == 0) {
			val chips = requireView().findViewById<ChipGroup>(R.id.chips)
			val chip = ItemFilterChipBinding.inflate(getLayoutInflater(), chips, false).root
			chip.text = title
			chips.addView(chip, chips.size - 1)
		}
	}
	
	val page: Unit
		get() {
			when (position) {
				0 -> toPayList
				1 -> selectivePayList
				2 -> getFeeList(calendarManager.year.toString())
				3 -> paymentList
				4 -> refundList
			}
		}
	val toPayList: Unit
		get() {
			model.addAndNext("client/api/client/necessary/list", "{}", 0)
		}
	val selectivePayList: Unit
		get() {
			model.addAndNext("client/api/client/chooce/list", "{}", 1)
		}
	
	fun getFeeList(year: String) {
		model.addAndNext("client/api/client/record/feelist", "{\"year\":$year}", 2)
	}
	
	fun getPaymentList(from: String, to: String?) {
		model.addAndNext("client/api/client/record/paymentlist",
		                 "{\"startTime\":\"$from\",\"overTime\":\"$to\"}",
		                 3)
	}
	
	val paymentList: Unit
		get() {
			getPaymentList(calendarManager.toDateTimeString(calendarManager.firstOfMonth!!.atStartOfDay())!!,
			               calendarManager.toDateTimeString(calendarManager.endOfMonth!!.atStartOfDay()))
		}
	val refundList: Unit
		get() {
			model.addAndNext("client/api/client/refund/list", "{}", 4)
		}
	
	internal inner class DateManager {
		var fromDate: LocalDate? = null
		var toDate: LocalDate? = null
		val fromDateString: String?
			get() = calendarManager.toDateString(fromDate)
		val toDateString: String?
			get() = calendarManager.toDateString(toDate)
		var fromDateTimeMillis: Long
			get() = calendarManager.toMillis(fromDate!!)
			set(from) {
				fromDate = calendarManager.toDate(from)
			}
		var toDateTimeMillis: Long
			get() = calendarManager.toMillis(toDate!!)
			set(to) {
				toDate = calendarManager.toDate(to)
			}
		val data: Unit
			get() {
				getPaymentList(calendarManager.toDateTimeString(fromDate!!.atStartOfDay())!!,
				               calendarManager.toDateTimeString(toDate!!.atStartOfDay()))
			}
	}
	
	companion object {
		fun newInstance(position: Int): PayFragment {
			val payFragment = PayFragment()
			payFragment.position = position
			return payFragment
		}
	}
}