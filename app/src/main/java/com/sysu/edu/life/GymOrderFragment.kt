package com.sysu.edu.life

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.alibaba.fastjson2.JSONObject
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.CompositeDateValidator
import com.google.android.material.datepicker.DateValidatorPointBackward
import com.google.android.material.datepicker.DateValidatorPointForward
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.datepicker.MaterialPickerOnPositiveButtonClickListener
import com.sysu.edu.BaseFragment
import com.sysu.edu.R
import com.sysu.edu.api.CalendarManager
import com.sysu.edu.api.CommonUtil
import com.sysu.edu.api.CommonUtil.extractValue
import com.sysu.edu.databinding.FragmentGymOrderBinding
import com.sysu.edu.model.GymModel
import com.sysu.edu.todo.TitleAdapter
import com.sysu.edu.view.PreferenceAdapter

class GymOrderFragment : BaseFragment() {
	var viewModel: GymReservationViewModel? = null
	lateinit var model: GymModel
	val calendarManager: CalendarManager = CalendarManager()
	private var total = -1
	private var page = 0
	private var concatAdapter: ConcatAdapter? = null
	private lateinit var binding: FragmentGymOrderBinding
	override fun onCreateView(inflater: LayoutInflater,
	                          container: ViewGroup?,
	                          savedInstanceState: Bundle?): View {
		super.onCreateView(inflater, container, savedInstanceState)
		model = GymModel(requireContext())
		viewModel = ViewModelProvider(requireActivity())[GymReservationViewModel::class.java]
		concatAdapter = ConcatAdapter(ConcatAdapter.Config.Builder()
										  .setIsolateViewTypes(true)
										  .build())
		val picker = MaterialDatePicker.Builder.datePicker()
		binding = FragmentGymOrderBinding.inflate(inflater, container, false).apply {
			recyclerView.layoutManager = LinearLayoutManager(requireContext())
			recyclerView.adapter = concatAdapter
			recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
				override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
					if (dy > 0 && total > 0 && page * 10 < total) order
				}
			})
			from.setOnClickListener {
				val datePicker = picker.setSelection(viewModel!!.from)
					.setCalendarConstraints(CalendarConstraints.Builder()
												.setValidator(CompositeDateValidator.allOf(listOf(DateValidatorPointBackward.before(viewModel!!.to))))
												.build())
					.build()
				datePicker.show(getParentFragmentManager(), "datePicker")
				datePicker.addOnPositiveButtonClickListener(MaterialPickerOnPositiveButtonClickListener { selection: Long? ->
					viewModel!!.from = selection!!
					from.text = datePicker.headerText
					regetOrder()
				})
			}
			from.text = calendarManager.toDateString(viewModel!!.from)
			to.text = calendarManager.toDateString(viewModel!!.to)
			to.setOnClickListener {
				val datePicker = picker.setSelection(viewModel!!.to)
					.setCalendarConstraints(CalendarConstraints.Builder()
												.setValidator(CompositeDateValidator.allOf(listOf(DateValidatorPointForward.from(viewModel!!.from))))
												.build())
					.build()
				datePicker.show(getParentFragmentManager(), "datePicker")
				datePicker.addOnPositiveButtonClickListener(MaterialPickerOnPositiveButtonClickListener { selection: Long? ->
					viewModel!!.to = selection!!
					to.text = datePicker.headerText
					regetOrder()
				})
			}
		}
		model.message.observe(requireActivity(), Observer { message: CommonUtil.Tuple2<Int, JSONObject> ->
			val response = message.second
			if (message.first == 0) {
				response.getJSONArray("Transactions").forEach { item: Any? ->
					concatAdapter?.addAdapter(TitleAdapter((item as JSONObject).getString("Description")).apply {
						header = 1
					})
					concatAdapter?.addAdapter(PreferenceAdapter().apply {
						set(CommonUtil.getString(requireContext(), intArrayOf(R.string.date, R.string.type, R.string.money, R.string.balance))
								.toMutableList(), extractValue(item as JSONObject, arrayOf("Date", "TransactionType", "Amount", "Balance")), mutableListOf(R.drawable.calendar, R.drawable.text, R.drawable.money, R.drawable.money))
					})
				}
				total = response.getInteger("TotalCount")
			}
		})
		order
		return binding.root
	}
	
	private fun regetOrder() {
		reset()
		order
	}
	
	fun reset() {
		total = -1
		page = 0
		concatAdapter!!.adapters.forEach { adapter: RecyclerView.Adapter<out RecyclerView.ViewHolder?>? -> concatAdapter!!.removeAdapter(adapter!!) }
	}
	
	val order: Unit
		get() {
			model.addAndNext("api/transaction/Me?StartDate=${calendarManager.toDateString(viewModel!!.from)}&EndDate=${calendarManager.toDateString(viewModel!!.to)}&Page=${++page}&PageSize=10", 0)
		}
}
