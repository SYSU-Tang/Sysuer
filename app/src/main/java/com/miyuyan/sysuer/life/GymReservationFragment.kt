package com.miyuyan.sysuer.life

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import com.miyuyan.sysuer.BaseFragment
import com.miyuyan.sysuer.R
import com.miyuyan.sysuer.api.CalendarManager
import com.miyuyan.sysuer.api.CommonUtil
import com.miyuyan.sysuer.api.CommonUtil.extractValue
import com.miyuyan.sysuer.databinding.FragmentGymOrderBinding
import com.miyuyan.sysuer.model.GymModel
import com.miyuyan.sysuer.todo.TitleAdapter
import com.miyuyan.sysuer.view.ButtonAdapter
import com.miyuyan.sysuer.view.PreferenceAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import java.io.IOException
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

class GymReservationFragment : BaseFragment() {
	val dateFormat: SimpleDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
	val viewModel: GymReservationViewModel by lazy {
		ViewModelProvider(requireActivity())[GymReservationViewModel::class.java]
	}
	private val concatAdapter: ConcatAdapter = ConcatAdapter(ConcatAdapter.Config.Builder()
	.setIsolateViewTypes(true)
	.build())
	lateinit var model: GymModel
	override fun onCreateView(inflater: LayoutInflater,
	                          container: ViewGroup?,
	                          savedInstanceState: Bundle?): View {
		super.onCreateView(inflater, container, savedInstanceState)
		model = GymModel(requireContext())
		val calendarManager = CalendarManager()
		val picker = MaterialDatePicker.Builder.datePicker()
		val binding = FragmentGymOrderBinding.inflate(inflater, container, false).apply {
			recyclerView.layoutManager = LinearLayoutManager(context)
			recyclerView.adapter = concatAdapter
			from.setOnClickListener {
				viewModel.reservationFromTo.value?.second?.let {
					val datePicker = picker.setSelection(viewModel.reservationFromTo.value!!.first)
						.setCalendarConstraints(CalendarConstraints.Builder()
							                        .setValidator(CompositeDateValidator.allOf(
								                        listOf(DateValidatorPointBackward.before(it))))
							                        .build())
						.build()
					datePicker.show(getParentFragmentManager(), "datePicker")
					datePicker.addOnPositiveButtonClickListener(
						MaterialPickerOnPositiveButtonClickListener { selection: Long? ->
							viewModel.reservationFromTo.value = CommonUtil.Tuple2(selection, it)
						})
				}
			}
			to.setOnClickListener {
				viewModel.reservationFromTo.value?.first?.let {
					val datePicker = picker.setSelection(viewModel.reservationFromTo.value!!.second)
						.setCalendarConstraints(CalendarConstraints.Builder()
							                        .setValidator(CompositeDateValidator.allOf(
								                        listOf(DateValidatorPointForward.from(it))))
							                        .build())
						.build()
					datePicker.show(getParentFragmentManager(), "datePicker")
					datePicker.addOnPositiveButtonClickListener(
						MaterialPickerOnPositiveButtonClickListener { selection: Long? ->
							viewModel.reservationFromTo.value = CommonUtil.Tuple2(it, selection)
						})
				}
			}
		}
		model.message.observe(requireActivity()) { (code, response) ->
			if (code == 0) response.getJSONArray("data").forEach { item: Any? ->
				val preferenceAdapter = PreferenceAdapter()
				val titleAdapter = TitleAdapter((item as JSONObject).getString("Description")).apply {
					header = 1
				}
				val buttonAdapter = ButtonAdapter().apply {
					add(getString(R.string.cancel_reservation))
					setListener { button, _ ->
						button.setOnClickListener {
							println(item.getString("Identity"))
							deleteReservation(item.getString("Identity"))
						}
					}
				}
				concatAdapter.addAdapter(titleAdapter)
				concatAdapter.addAdapter(preferenceAdapter)
				concatAdapter.addAdapter(buttonAdapter)
				val value: ArrayList<String?> = extractValue(item,
				                                             arrayOf("VenueName",
				                                                     "StartDateTime",
				                                                     "EndDateTime",
				                                                     "Charge",
				                                                     "CreatedAt"))
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
					throw IllegalArgumentException("Invalid Time, which is required to format as yyyy-MM-dd'T'HH:mm:ss'Z'",
					                               e)
				}
				preferenceAdapter.set(mutableListOf(getString(R.string.venue),
				                                    getString(R.string.start_time),
				                                    getString(R.string.end_time),
				                                    getString(R.string.money),
				                                    getString(R.string.order_time)),
				                      value,
				                      mutableListOf(R.drawable.location,
				                                    R.drawable.time,
				                                    R.drawable.alarm,
				                                    R.drawable.time,
				                                    R.drawable.money,
				                                    R.drawable.time))
				preferenceAdapter.add(getString(R.string.pay_way),
				                      if (item.getBoolean("IsCash")) getString(R.string.cash)
				                      else getString(R.string.pe_credit),
				                      R.drawable.money)
			}
		}
		viewModel.reservationFromTo.observe(viewLifecycleOwner) { (from,to) ->
			if (from != null && to != null) {
				binding.from.text = calendarManager.toDateString(from)
				binding.to.text = calendarManager.toDateString(to)
				regetReservation()
			}
		}
		return binding.root
	}
	
	private fun regetReservation() {
		reset()
		reservation
	}
	
	fun reset() {
		concatAdapter.adapters.forEach { adapter: RecyclerView.Adapter<out RecyclerView.ViewHolder?>? ->
			concatAdapter.removeAdapter(adapter!!)
		}
	}
	
	val reservation: Unit
		get() {
			if (viewModel.reservationFromTo.value != null && viewModel.reservationFromTo.value!!.second != null && viewModel.reservationFromTo.value!!.first != null) model.addAndNext(
				"api/BookingRequestVenue?all=false&startDate=${dateFormat.format(viewModel.reservationFromTo.value!!.first)}&endDate=${
					dateFormat.format(viewModel.reservationFromTo.value!!.second)
				}&waitingList=false",
				0)
		}
	
	fun deleteReservation(bookingId: String) {
		model.run(model.http.generateGetRequest("https://${model.host}/api/BookingRequestVenue/$bookingId")
			          .delete()
			          .build(), object : Callback {
			override fun onFailure(call: Call, e: IOException) {
				model.http.handler.post { model.contextUtil.toast(R.string.no_net_connected) }
			}
			
			override fun onResponse(call: Call, response: Response) {
				//println(response.body.string())
				//println(response.code)
				//println(response.message)
				//println(response.headers.toMultimap())
				if (response.isSuccessful) CoroutineScope(Dispatchers.Main).launch {
					regetReservation()
				}
			}
		})
	}
}
