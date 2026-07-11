package com.sysu.edu.life

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.alibaba.fastjson2.JSONArray
import com.alibaba.fastjson2.JSONObject
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.sysu.edu.BaseFragment
import com.sysu.edu.R
import com.sysu.edu.databinding.DialogGymReservationBinding
import com.sysu.edu.databinding.FragmentGymDetailBinding
import com.sysu.edu.databinding.ItemDateBinding
import com.sysu.edu.databinding.ItemFieldDetailBinding
import com.sysu.edu.model.GymModel
import com.sysu.edu.view.RecyclerAdapter
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID
import java.util.function.Consumer
import java.util.regex.Pattern

class GymDetailFragment : BaseFragment() {
	val fee: MutableMap<String?, JSONObject?> = mutableMapOf()
	var viewModel: GymReservationViewModel? = null
	var id: String? = null
	var hash: String? = null
	var dateAdapter: DateAdapter? = null
	var userId: String? = null
	var type: String? = null
	val model: GymModel by lazy {
		GymModel(requireContext())
	}
	
	override fun onCreateView(inflater: LayoutInflater,
	                          container: ViewGroup?,
	                          savedInstanceState: Bundle?): View {
		super.onCreateView(inflater, container, savedInstanceState)
		viewModel = ViewModelProvider(requireActivity())[GymReservationViewModel::class.java]
		id = requireArguments().getString("id")
		val gridLayoutManager = GridLayoutManager(requireContext(), 4, GridLayoutManager.HORIZONTAL, false)
		val fieldAdapter = FieldAdapter().apply {
			action = {
				viewModel!!.selected.value = selected
			}
		}
		dateAdapter = DateAdapter().apply {
			action = { value: Int? -> viewModel!!.position.value = value }
			select((viewModel!!.position.value ?: 0))
		}
		val binding = FragmentGymDetailBinding.inflate(inflater, container, false).apply {
			date.recyclerView.adapter = dateAdapter
			date.recyclerView.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
			date.recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
				override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
					super.onScrolled(recyclerView, dx, dy)
					if (!recyclerView.canScrollHorizontally(1) && dx > 0) dateAdapter?.offset(7)
				}
			})
			field.recyclerView.layoutManager = gridLayoutManager
			field.recyclerView.adapter = fieldAdapter
		}
		val dialogBinding = DialogGymReservationBinding.inflate(inflater, container, false).apply {
			field.key.setText(R.string.field)
			date.key.setText(R.string.date)
			time.key.setText(R.string.time)
			fee.key.setText(R.string.fee)
			type.key.setText(R.string.type)
		}
		val dialog = BottomSheetDialog(requireContext())
		dialog.setContentView(dialogBinding.root)
		if (viewModel!!.position.value == null) viewModel!!.position.postValue(0)
		viewModel!!.position.observe(viewLifecycleOwner) { p: Int? ->
			if (p != null) info
		}
		model.message.observe(viewLifecycleOwner) { (code, response) ->
			println(response)
			when (code) {
				0 -> {
					reset(fieldAdapter)
					hash = md5("$response")
					var availableCapacity = 0
					var rows = -1
					val name = MutableLiveData(false)
					response.getJSONArray("data").forEach(Consumer { item: Any? ->
						val timeslots = (item as JSONObject).getJSONArray("Timeslots")
						if (timeslots != null) {
							if (rows == -1) {
								fieldAdapter.add(JSONObject.of("Name", getString(R.string.time), "Type", 2))
								timeslots.forEach { o: Any? -> fieldAdapter.add(JSONObject.of("Name", "${(o as JSONObject).getString("Start")}\n${o.getString("End")}", "Type", 2)) }
								name.value = true
								rows = timeslots.size + 1
								gridLayoutManager.spanCount = rows
							} // 第一列
							val fieldName = Pattern.compile("(.+)-")
								.matcher(item.getString("VenueName"))
								.replaceAll("") // 第一行
							fieldAdapter.add(JSONObject().fluentPut("VenueName", fieldName)
												 .fluentPut("Type", 0))
							timeslots.forEach { data: Any? ->
								fieldAdapter.add((data as JSONObject).clone()
													 .fluentPut("VenueBooking", item.clone()
														 .fluentPut("Timeslots", JSONArray.of(data)))
													 .fluentPut("Type", 1)
													 .fluentPut("Venue", fieldName)
													 .fluentPut("Duration", "${data.getString("Start")}~${data.getString("End")}"))
								data.getInteger("AvailableCapacity")?.let {
									availableCapacity += it
								}
							}
							if (fieldAdapter.itemCount % rows != 0) (0..<(rows - fieldAdapter.itemCount % rows)).forEach { _ -> fieldAdapter.add(JSONObject.of("Type", 3)) }
						}
					})
					if (viewModel!!.position.value != null) dateAdapter!!.setAvailableCapacity(viewModel!!.position.value!!, availableCapacity)
					getFee(id!!)
				}
				1 -> {
					fee.clear()
					response.getJSONArray("data")?.run {
						forEach { fee[(it as JSONObject).getString("UserRole")] = it }
					}
					me
				}
				2 -> {
					response.getJSONArray("data")?.takeUnless { it.isEmpty() }?.let {
						userId = it.getJSONObject(0).getString("UserId")
					}
					getType(id)
				}
				3 -> {
					response.getJSONArray("data")?.takeUnless { it.isEmpty() }?.let {
						type = it.getJSONObject(0).getString("TypeIdentity")
					}
				}
				4 -> {
					with(response.getJSONObject("data")) {
						if (getInteger("Code") == 200) config.toast(R.string.reserve_success)
						else config.toast(getString("Result")) // 订单编号
					}
				}
			}
		}
		viewModel!!.selected.observe(viewLifecycleOwner, Observer { selected: MutableSet<Int>? ->
			fieldAdapter.selected = selected
			val studentFee = fee["学生"]
			if (studentFee != null) {
				fieldAdapter.selected?.isEmpty().let {
					binding.submit.setEnabled(it == false)
					if (it == true) binding.info.text = getString(R.string.unselected)
					else {
						val info = StringBuilder()
						val items = JSONArray()
						fieldAdapter.selected?.forEach { e: Int? ->
							info.append(fieldAdapter.get(e!!).getString("Venue"))
								.append(" ")
								.append(fieldAdapter.get(e).getString("Duration"))
								.append("+")
							items.add(fieldAdapter.get(e).getJSONObject("VenueBooking"))
						}
						val venueName = fieldAdapter.get(fieldAdapter.selected?.toList()?.get(0)!!)
							.getString("VenueName")
						val creditFee = studentFee.getInteger("CreditFee") * fieldAdapter.selected?.size!!
						binding.submit.setOnClickListener { reserve(items, venueName, creditFee) }
						binding.info.text = String.format(Locale.getDefault(), "%s=%d元", info.deleteCharAt(info.length - 1), creditFee)
					}
				}
			}
		})
		return binding.root
	}
	
	fun reset(field: FieldAdapter) {
		field.clear()
		field.clearSelected()
		viewModel!!.selected.value = HashSet()
	}
	
	val info: Unit
		get() {
			viewModel!!.position.value?.let {
				getInfo(id!!, dateAdapter!!.getFormattedDate(it), dateAdapter!!.getFormattedDate(it))
			}
		}
	
	fun getInfo(id: String, from: String?, to: String?) {
		model.addAndNext("api/venue/available-slots/range?venueTypeId=$id&start=$from&end=$to", 0)
	}
	
	fun getFee(id: String) {
		model.addAndNext("api/venuetype/$id/feetemplates", 1)
	}
	
	val me: Unit
		get() {
			model.addAndNext("api/swimmer/me", 2)
		}
	
	fun getType(id: String?) {
		model.addAndNext("api/venue/type/$id", 3)
	}
	
	fun reserve(payload: String?) {
		model.addAndNext("api/BookingRequestVenue", payload, 4)
	}
	
	/**
	 * 生成 UUID
	 *
	 * @return 生成的 UUID
	 */
	fun generateUUID(): String {
		return UUID.randomUUID().toString()
	}
	
	/**
	 * 生成 Token
	 *
	 * @param hash 哈希值
	 * @return 生成的 Token
	 */
	fun genToken(uuid: String?, hash: String?): String {
		val timestamp = System.currentTimeMillis() / 1000L
		return md5("SYSUBOOKING-$uuid$timestamp") + "." + timestamp + "." + hash
	}
	
	/**
	 * 计算 MD5 哈希值
	 *
	 * @param input 输入字符串
	 * @return 计算得到的 MD5 哈希值（十六进制小写字符串）
	 */
	fun md5(input: String): String {
		try {
			val hexString = StringBuilder()
			(MessageDigest.getInstance("MD5")
				.digest(input.toByteArray(StandardCharsets.UTF_8))).forEach {
					hexString.append(Integer.toHexString(0xff and it.toInt())
										 .takeUnless { it1 -> it1.length == 1 } ?: "0")
				}
			return "$hexString"
		} catch (e: NoSuchAlgorithmException) {
			throw RuntimeException("MD5 algorithm not available", e)
		}
	}
	
	//    void updateReservationDialog(DialogGymReservationBinding binding, JSONObject item, JSONObject studentFee) {
	//        binding.field.value.setText(item.getString("Venue"));
	//        binding.date.value.setText(item.getString("Date"));
	//        binding.time.value.setText(item.getString("Duration"));
	//        Integer creditFee = studentFee.getInteger("CreditFee");
	//        Integer cashFee = studentFee.getInteger("CashFee");
	//        binding.fee.value.setText(String.format(Locale.getDefault(), "运动时￥%d或现金￥%d", creditFee, cashFee));
	//        binding.type.value.setText(item.getString("Type"));
	//        binding.reserve.setOnClickListener(_ -> reserve(item.getJSONArray("VenueBooking"), item.getString("VenueName"), creditFee));
	//    }
	private fun reserve(items: JSONArray?, venueName: String?, creditFee: Int?) {
		val uuid = generateUUID()
		val time = LocalDateTime.now()
			.atZone(ZoneId.systemDefault())
			.withZoneSameInstant(ZoneOffset.UTC)
			.toString()
		val payload = JSONObject.of("Identity", uuid, "BookingId", genToken(uuid, hash), "VenueTypeId", type, "VenueBookings", items, "Participants", JSONArray.of(), "Status", "Accepted", "Description", venueName, "CreatedAt", time, "UpdatedAt", time, "ActionedBy", userId,  /*NetID*/
		                            "IsCash", false, "Charge", creditFee) //        System.out.println(payload);
		reserve(payload.toJSONString())
	}
	
	class DateAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder?>() {
		val availableCapacity: MutableMap<Int?, Int?> = mutableMapOf()
		var action: ((Int) -> Unit)? = null
		var page: Int = 7
		var selected: Int = -1
		override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
			return object :
				RecyclerView.ViewHolder(ItemDateBinding.inflate(LayoutInflater.from(parent.context), parent, false).root) {}
		}
		
		fun select(position: Int) {
			val tmp = selected
			selected = position
			action!!(position)
			notifyItemChanged(position)
			notifyItemChanged(tmp)
		}
		
		override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
			val binding = ItemDateBinding.bind(holder.itemView).apply {
				date.text = getDate(position)
				week.text = getWeek(holder.itemView.context, position)
				root.setOnClickListener { select(position) }
				root.isChecked = position == selected
			}
			binding.availableCapacity.text = availableCapacity.getOrDefault(position, -1)
				?.let { if (it >= 0) "$it" else "" }
		}
		
		fun setAvailableCapacity(position: Int, i: Int) {
			availableCapacity[position] = i
			notifyItemChanged(position)
		}
		
		override fun getItemCount(): Int = page
		fun offset(offset: Int) {
			page += offset
			notifyItemRangeInserted(page - 1, offset)
		}
		
		private fun getDate(distanceDay: Int, pattern: String?): String? {
			return LocalDate.now()
				.plusDays(distanceDay.toLong())
				.format(DateTimeFormatter.ofPattern(pattern))
		}
		
		fun getDate(distanceDay: Int): String? = getDate(distanceDay, "M月dd日")
		fun getFormattedDate(distanceDay: Int): String? = getDate(distanceDay, "M-dd")
		fun getWeek(context: Context, distanceDay: Int): String? =
			context.resources.getStringArray(R.array.weeks)[LocalDate.now()
				.plusDays(distanceDay.toLong())
				.getDayOfWeek().value - 1]
	}
	
	class FieldAdapter : RecyclerAdapter<JSONObject>() {
		var action: ((Int) -> Unit)? = null
		var selected: MutableSet<Int>? = null
			set(selected) {
				if (field != selected) {
					field = selected?.also {
						it.forEach { i -> notifyItemChanged(i) }
					}
				}
			}
		
		override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
			return object :
				RecyclerView.ViewHolder(ItemFieldDetailBinding.inflate(LayoutInflater.from(parent.context), parent, false).root) {}
		}
		
		fun clearSelected() {
			val s = selected
			selected?.clear()
			s?.forEach { notifyItemChanged(it) }
		}
		
		override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
			val pos = holder.getBindingAdapterPosition()
			val item = get(pos)
			ItemFieldDetailBinding.bind(holder.itemView).apply {
				val context = root.context
				fieldDetail.alpha = 1.0f
				root.setOnClickListener {
					if (item.getInteger("Type") == 1 && item.getInteger("AvailableCapacity") > 0) {
						if (selected!!.contains(pos)) selected!!.remove(pos)
						else selected!!.add(pos)
						action!!(pos)
						notifyItemChanged(pos)
					}
				}
				when (item.getInteger("Type")) {
					0 -> fieldDetail.text = item.getString("VenueName", "")
					2 -> fieldDetail.text = item.getString("Name", "")
					1 -> {
						if (item.getInteger("AvailableCapacity") > 0) {
							fieldDetail.text = "${context.getString(R.string.reservable)}/${item.getString("AvailableCapacity", "")}"
						}
						else {
							fieldDetail.setText(R.string.reserved)
							fieldDetail.setAlpha(0.5f)
						}
						root.isChecked = selected!!.contains(pos)
					}
					else -> fieldDetail.text = ""
				}
			}
		}
	}
}