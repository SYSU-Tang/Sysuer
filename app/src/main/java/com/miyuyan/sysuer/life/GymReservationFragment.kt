package com.miyuyan.sysuer.life

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
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
import com.miyuyan.sysuer.api.CommonUtil
import com.miyuyan.sysuer.api.CommonUtil.extractValue
import com.miyuyan.sysuer.api.DateTimeManager
import com.miyuyan.sysuer.databinding.FragmentGymOrderBinding
import com.miyuyan.sysuer.model.GymModel
import com.miyuyan.sysuer.todo.TitleAdapter
import com.miyuyan.sysuer.view.ButtonAdapter
import com.miyuyan.sysuer.view.PreferenceAdapter
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
	private val concatAdapter: ConcatAdapter = ConcatAdapter(
			ConcatAdapter.Config.Builder().setIsolateViewTypes(true).build()
	)
	lateinit var model: GymModel
	override fun onCreateView(
		inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
	): View {
		super.onCreateView(inflater, container, savedInstanceState)
		model = GymModel(requireContext())
		val picker = MaterialDatePicker.Builder.datePicker()
		val binding = FragmentGymOrderBinding.inflate(inflater, container, false).apply {
			recyclerView.layoutManager = LinearLayoutManager(context)
			recyclerView.adapter = concatAdapter
			from.setOnClickListener {
				viewModel.reservationFromTo.value?.second?.let {
					val datePicker = picker.setSelection(viewModel.reservationFromTo.value!!.first)
						.setCalendarConstraints(
								CalendarConstraints.Builder().setValidator(
										CompositeDateValidator.allOf(
												listOf(DateValidatorPointBackward.before(it))
										)
								).build()
						).build()
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
						.setCalendarConstraints(
								CalendarConstraints.Builder().setValidator(
										CompositeDateValidator.allOf(
												listOf(DateValidatorPointForward.from(it))
										)
								).build()
						).build()
					datePicker.show(getParentFragmentManager(), "datePicker")
					datePicker.addOnPositiveButtonClickListener(
							MaterialPickerOnPositiveButtonClickListener { selection: Long? ->
								viewModel.reservationFromTo.value = CommonUtil.Tuple2(it, selection)
							})
				}
			}
		}
		viewLifecycleOwner.lifecycleScope.launch {
			repeatOnLifecycle(Lifecycle.State.STARTED) {
				model.message.collect { (code, response) ->
					if (code == 0) response.getJSONArray("data").forEach { item: Any? ->
						val preferenceAdapter = PreferenceAdapter()
						val titleAdapter =
							TitleAdapter((item as JSONObject).getString("Description")).apply {
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
						val value: ArrayList<String?> = extractValue(
								item, arrayOf(
								"VenueName", "StartDateTime", "EndDateTime", "Charge", "CreatedAt"
						)
						)
						try {
							val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")
							value[1] =
								LocalDateTime.parse(value[1], formatter).atZone(ZoneId.of("UTC"))
									.withZoneSameInstant(ZoneId.systemDefault())
									.format(formatter) //                                    value.set(4, LocalDateTime.parse(value.get(4), FORMATTER).atZone(ZoneId.of("UTC")).withZoneSameInstant(ZoneId.systemDefault()).format(FORMATTER));
							value[2] =
								LocalDateTime.parse(value[2], formatter).atZone(ZoneId.of("UTC"))
									.withZoneSameInstant(ZoneId.systemDefault()).format(formatter)
						} catch (e: DateTimeParseException) {
							throw IllegalArgumentException(
									"Invalid Time, which is required to format as yyyy-MM-dd'T'HH:mm:ss'Z'",
									e
							)
						}
						preferenceAdapter.set(
								mutableListOf(
										getString(R.string.venue),
										getString(R.string.start_time),
										getString(R.string.end_time),
										getString(R.string.money),
										getString(R.string.order_time)
								), value, mutableListOf(
								R.drawable.location,
								R.drawable.time,
								R.drawable.alarm,
								R.drawable.time,
								R.drawable.money,
								R.drawable.time
						)
						)
						preferenceAdapter.add(
								getString(R.string.pay_way),
								if (item.getBoolean("IsCash")) getString(R.string.cash)
								else getString(R.string.pe_credit),
								R.drawable.money
						)
					}
				}
			}
		}
		viewModel.reservationFromTo.observe(viewLifecycleOwner) { (from, to) ->
			if (from != null && to != null) {
				binding.from.text = DateTimeManager.toDateString(from)
				binding.to.text = DateTimeManager.toDateString(to)
				regetReservation()
			}
		}
		return binding.root
	}

	override fun onDestroyView() {
		super.onDestroyView()
		model.dispose()
	}

	private fun regetReservation() {
		reset()
		loadReservation()
	}

	fun reset() {
		concatAdapter.adapters.forEach { adapter: RecyclerView.Adapter<out RecyclerView.ViewHolder?> ->
			concatAdapter.removeAdapter(adapter)
		}
	}

	private fun loadReservation() {
		val (from, to) = viewModel.reservationFromTo.value ?: return
		if (from != null && to != null) {
			model.enqueue(
					"api/BookingRequestVenue?all=false&startDate=${DateTimeManager.toDateString(from)}&endDate=${
						DateTimeManager.toDateString(to)
					}&waitingList=false", 0
			)
		}
	}

	fun deleteReservation(bookingId: String) {
		model.call(
				model.http
					.generateRequest("https://${model.host}/api/BookingRequestVenue/$bookingId")
					.delete().build(), object : Callback {
			override fun onFailure(call: Call, e: IOException) {
				model.contextUtil.toast(R.string.no_net_connected)
			}

			override fun onResponse(call: Call, response: Response) {
				//println(response.body.string())
				//println(response.code)
				//println(response.message)
				//println(response.headers.toMultimap())
				if (response.isSuccessful) lifecycleScope.launch {
					regetReservation()
				}
			}
		})
	}
}