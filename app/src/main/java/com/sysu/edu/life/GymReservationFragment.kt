package com.sysu.edu.life

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
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
import com.sysu.edu.view.ButtonAdapter
import com.sysu.edu.view.OnBindListener
import com.sysu.edu.view.PreferenceAdapter
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale
import java.util.function.Consumer

class GymReservationFragment : BaseFragment() {
	val dateFormat: SimpleDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
	var viewModel: GymReservationViewModel? = null
	private var concatAdapter: ConcatAdapter? = null
	lateinit var model: GymModel
	override fun onCreateView(inflater: LayoutInflater,
	                          container: ViewGroup?,
	                          savedInstanceState: Bundle?): View {
		super.onCreateView(inflater, container, savedInstanceState)
		model = GymModel(requireContext())
		val calendarManager = CalendarManager()
		viewModel = ViewModelProvider(requireActivity())[GymReservationViewModel::class.java]
		concatAdapter = ConcatAdapter(ConcatAdapter.Config.Builder()
										  .setIsolateViewTypes(true)
										  .build())
		val picker = MaterialDatePicker.Builder.datePicker()
		val binding = FragmentGymOrderBinding.inflate(inflater, container, false).apply {
			recyclerView.layoutManager = LinearLayoutManager(context)
			recyclerView.adapter = concatAdapter
			from.setOnClickListener {
				viewModel!!.reservationFromTo.getValue()?.second?.let {
					val datePicker = picker.setSelection(viewModel!!.reservationFromTo.getValue()!!.first)
						.setCalendarConstraints(CalendarConstraints.Builder()
													.setValidator(CompositeDateValidator.allOf(listOf(DateValidatorPointBackward.before(it))))
													.build())
						.build()
					datePicker.show(getParentFragmentManager(), "datePicker")
					datePicker.addOnPositiveButtonClickListener(MaterialPickerOnPositiveButtonClickListener { selection: Long? -> viewModel!!.reservationFromTo.value = CommonUtil.Tuple2(selection, it) })
				}
			}
			to.setOnClickListener {
				viewModel!!.reservationFromTo.getValue()?.first?.let {
					val datePicker = picker.setSelection(viewModel!!.reservationFromTo.getValue()!!.second)
						.setCalendarConstraints(CalendarConstraints.Builder()
													.setValidator(CompositeDateValidator.allOf(listOf(DateValidatorPointForward.from(it))))
													.build())
						.build()
					datePicker.show(getParentFragmentManager(), "datePicker")
					datePicker.addOnPositiveButtonClickListener(MaterialPickerOnPositiveButtonClickListener { selection: Long? -> viewModel!!.reservationFromTo.value = CommonUtil.Tuple2(it, selection) })
				}
			}
		}
		model.message.observe(requireActivity(), Observer { message: CommonUtil.Tuple2<Int, JSONObject> ->
			if (message.first == 0) message.second.getJSONArray("data")
				.forEach(Consumer { item: Any? ->
					val preferenceAdapter = PreferenceAdapter()
					val titleAdapter = TitleAdapter((item as JSONObject).getString("Description")).apply {
						header = 1
					}
					val buttonAdapter = ButtonAdapter().apply {
						add(getString(R.string.cancel_reservation))
						setListener(object : OnBindListener {
							override fun onBind(button: Button?, position: Int) {
								button?.setOnClickListener { deleteReservation(item.getString("Identity")) }
								regetReservation()
							}
						})
					}
					concatAdapter!!.addAdapter(titleAdapter)
					concatAdapter!!.addAdapter(preferenceAdapter)
					concatAdapter!!.addAdapter(buttonAdapter)
					val value: ArrayList<String?> = extractValue(item, arrayOf("VenueName", "StartDateTime", "EndDateTime", "Charge", "CreatedAt"))
					try {
						val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")
						value[1] = LocalDateTime.parse(value[1], formatter)
							.atZone(ZoneId.of("UTC"))
							.withZoneSameInstant(ZoneId.systemDefault())
							.format(formatter) //                                    value.set(4, LocalDateTime.parse(value.get(4), FORMATTER).atZone(ZoneId.of("UTC")).withZoneSameInstant(ZoneId.systemDefault()).format(FORMATTER));
						value[2] = LocalDateTime.parse(value[2], formatter)
							.atZone(ZoneId.of("UTC"))
							.withZoneSameInstant(ZoneId.systemDefault())
							.format(formatter)
					} catch (e: DateTimeParseException) {
						throw IllegalArgumentException("Invalid Time, which is required to format as yyyy-MM-dd'T'HH:mm:ss'Z'", e)
					}
					preferenceAdapter.set(mutableListOf(getString(R.string.venue), getString(R.string.start_time), getString(R.string.end_time), getString(R.string.money), getString(R.string.order_time)), value, mutableListOf(R.drawable.location, R.drawable.time, R.drawable.alarm, R.drawable.money))
					preferenceAdapter.add(getString(R.string.pay_way), if (item.getBoolean("IsCash")) getString(R.string.cash) else getString(R.string.pe_credit), R.drawable.money)
				})
		})
		viewModel!!.reservationFromTo.observe(getViewLifecycleOwner(), Observer { o: CommonUtil.Tuple2<Long?, Long?>? ->
			if (o != null && o.second != null && o.first != null) {
				binding.from.text = calendarManager.toDateString(o.first!!)
				binding.to.text = calendarManager.toDateString(o.second!!)
				regetReservation()
			}
		})
		return binding.root
	}
	
	private fun regetReservation() {
		reset()
		reservation
	}
	
	fun reset() {
		concatAdapter!!.adapters.forEach { adapter: RecyclerView.Adapter<out RecyclerView.ViewHolder?>? -> concatAdapter!!.removeAdapter(adapter!!) }
	}
	
	val reservation: Unit
		get() {
			if (viewModel!!.reservationFromTo.getValue() != null && viewModel!!.reservationFromTo.getValue()!!.second != null && viewModel!!.reservationFromTo.getValue()!!.first != null) model.addAndNext("api/BookingRequestVenue?all=false&startDate=${dateFormat.format(viewModel!!.reservationFromTo.getValue()!!.first)}&endDate=${dateFormat.format(viewModel!!.reservationFromTo.getValue()!!.second)}&waitingList=false", 0)
		}
	
	fun deleteReservation(bookingId: String) {
		model.http.deleteRequest(model.authorizationManager.host + "api/BookingRequestVenue/$bookingId", 1)
	}
}
