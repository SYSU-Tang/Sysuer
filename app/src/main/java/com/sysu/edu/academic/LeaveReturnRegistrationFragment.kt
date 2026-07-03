package com.sysu.edu.academic

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.appcompat.widget.PopupMenu
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.alibaba.fastjson2.JSONArray
import com.alibaba.fastjson2.JSONObject
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.datepicker.MaterialPickerOnPositiveButtonClickListener
import com.sysu.edu.R
import com.sysu.edu.api.CommonUtil
import com.sysu.edu.api.CommonUtil.extractValue
import com.sysu.edu.api.CommonUtil.isEmpty
import com.sysu.edu.databinding.DialogRegionBinding
import com.sysu.edu.databinding.ItemCardBinding
import com.sysu.edu.databinding.ItemTitleBinding
import com.sysu.edu.model.XgxtModel
import com.sysu.edu.view.AdapterListener
import com.sysu.edu.view.RecyclerAdapter
import com.sysu.edu.view.StaggeredFragment
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class LeaveReturnRegistrationFragment : StaggeredFragment() {
	val leaveDate: MutableLiveData<Long?> = MutableLiveData<Long?>()
	val returnDate: MutableLiveData<Long?> = MutableLiveData<Long?>()
	var root: View? = null
	var transportation: JSONArray? = null
	var destination: JSONArray? = null
	var country: String? = ""
	var province: String? = ""
	var city: String? = ""
	var isStay: String? = ""
	var id: String? = null
	lateinit var model: XgxtModel
	override fun onDestroyView() {
		super.onDestroyView()
		model.dispose()
	}
	
	override fun onCreateView(inflater: LayoutInflater,
	                          container: ViewGroup?,
	                          savedInstanceState: Bundle?): View? {
		if (root == null) {
			root = super.onCreateView(inflater, container, savedInstanceState)
			id = requireArguments().getString("Id")
			model = XgxtModel(requireContext())
			val leave: MutableList<String?> = mutableListOf()
			val stay: MutableList<String?> = mutableListOf()
			val leaveKeys: MutableList<String> = mutableListOf("假期去向", "预计离校时间", "预计返校时间", "去向类型", "交通工具", "外出地")
			val stayKeys: MutableList<String> = mutableListOf("假期去向", "留校原因")
			val regionDialog = BottomSheetDialog(requireContext())
			val dialogRegionBinding = DialogRegionBinding.inflate(inflater, container, false)
			regionDialog.setContentView(dialogRegionBinding.root)
			dialogRegionBinding.country.recyclerView.setLayoutManager(LinearLayoutManager(requireContext()))
			val countryAdapter = OneColumnAdapter()
			dialogRegionBinding.country.recyclerView.adapter = countryAdapter
			dialogRegionBinding.country.recyclerView.isNestedScrollingEnabled = false
			dialogRegionBinding.country.recyclerView.overScrollMode = RecyclerView.OVER_SCROLL_ALWAYS
			dialogRegionBinding.province.recyclerView.layoutManager = LinearLayoutManager(requireContext())
			val provinceAdapter = OneColumnAdapter()
			dialogRegionBinding.province.recyclerView.adapter = provinceAdapter
			dialogRegionBinding.province.recyclerView.isNestedScrollingEnabled = false
			dialogRegionBinding.province.recyclerView.overScrollMode = RecyclerView.OVER_SCROLL_ALWAYS
			dialogRegionBinding.county.recyclerView.layoutManager = LinearLayoutManager(requireContext())
			val cityAdapter = OneColumnAdapter()
			dialogRegionBinding.county.recyclerView.adapter = cityAdapter
			dialogRegionBinding.county.recyclerView.isNestedScrollingEnabled = false
			dialogRegionBinding.county.recyclerView.overScrollMode = RecyclerView.OVER_SCROLL_ALWAYS
			model.message.observe(requireActivity(), Observer { message: CommonUtil.Tuple2<Int, JSONObject> ->
				val response = message.second
				if (response.getInteger("code") == 200) {
					when (message.first) {
						0 -> {
							clear()
							val data = response.getJSONObject("data")
							add("基本信息", mutableListOf<String?>("姓名", "学号", "年级", "培养层次", "专业", "学院", "联系电话", "宿舍地址", "紧急联系人", "紧急联系人联系电话", "节假日名称", "节假日时间", "返校报到时间段"), extractValue(data, arrayOf("xm", "xh", "nj", "pycc", "zymc", "bmmc", "lxdh", "jjlxr", "jjlxrdh", "ssdz", "jjrmc", "jjrrq", "fxbdsj")))
							isStay = data.getString("sflx")
							if (data.containsKey("yjlxsj") && !data.getString("yjlxsj", "")
									.isEmpty()) leaveDate.value = LocalDate.parse(data.getString("yjlxsj"))
								.atStartOfDay(ZoneId.systemDefault())
								.toInstant()
								.toEpochMilli()
							if (data.containsKey("yjfxsj") && !data.getString("yjfxsj", "")
									.isEmpty()) returnDate.value = LocalDate.parse(data.getString("yjfxsj"))
								.atStartOfDay(ZoneId.systemDefault())
								.toInstant()
								.toEpochMilli()
							country = data.getString("wcdgj", "")
							province = data.getString("wcdsf", "")
							city = data.getString("wcdcs", "")
							leave.addAll(mutableListOf<String?>("离校", data.getString("yjlxsj", ""), data.getString("yjfxsj", ""), data.getString("qxlx", ""), data.getString("jtgj", ""), "$country $province $city"))
							stay.addAll( mutableListOf<String?>("留校", data.getString("lxyy", "")))
							if ("0" == isStay) add(getString(R.string.registration), leaveKeys, leave)
							else add(getString(R.string.registration), stayKeys, stay)
							getDestination()
							getTransportation()
							getCountry()
						}
						1 -> transportation = response.getJSONArray("data")
						2 -> destination = response.getJSONArray("data")
						3 -> {
							countryAdapter.clear()
							response.getJSONArray("data")
								.forEach { e: Any? -> countryAdapter.add((e as JSONObject).getString("label")) }
							countryAdapter.action = { pos: Int? ->
								country = response.getJSONArray("data")
									.getJSONObject(pos!!)
									.getString("value")
								if ("中国" == country) getProvince()
								else {
									city = ""
									province = ""
								}
							}
							countryAdapter.result = country
							if ("中国" == country) getProvince() // dialogRegionBinding.regionList.setAdapter(new TwoColumnsAdapter(destination));
						}
						4 -> {
							provinceAdapter.clear()
							response.getJSONArray("data")
								.forEach { e: Any? -> provinceAdapter.add((e as JSONObject).getString("label")) }
							provinceAdapter.action = { pos: Int? ->
								province = response.getJSONArray("data")
									.getJSONObject(pos!!)
									.getString("value")
								getCity(province)
							}
							getCity(province)
							provinceAdapter.result = province
						}
						5 -> {
							cityAdapter.clear()
							response.getJSONArray("data")
								.forEach { e: Any? -> cityAdapter.add((e as JSONObject).getString("label")) }
							cityAdapter.action = { pos: Int? ->
								city = response.getJSONArray("data")
									.getJSONObject(pos!!)
									.getString("value")
							}
							cityAdapter.result = city
						}
						else -> config.toast(response.getString("message"))
					}
				}
			})
			setListener(object : AdapterListener {
				override fun onBind(adapter: RecyclerView.Adapter<RecyclerView.ViewHolder?>,
				                    holder: RecyclerView.ViewHolder,
				                    position: Int) {
					staggeredAdapter.getTwoColumnsAdapter(position)
						.setListener(object : AdapterListener {
							override fun onBind(adapter: RecyclerView.Adapter<RecyclerView.ViewHolder?>,
							                    holder: RecyclerView.ViewHolder,
							                    position: Int) {
								holder.itemView.setOnClickListener {
									if (position == 1) {
										if (position == 0) {
											val menu = PopupMenu(requireContext(), holder.itemView)
											mutableListOf<String?>("离校", "留校").forEach { i: String? ->
												menu.menu.add(i)
													.setOnMenuItemClickListener { //value.set(pos, i);
														isStay = if ("离校" == i) "0" else "1"
														(adapter as TwoColumnsAdapter).setValue(if ("离校" == i) leave else stay)
														adapter.setKey(if ("离校" == i) leaveKeys else stayKeys)
														true
													}
											}
											menu.show()
										}
										if (adapter.itemCount == 6) {
											when (position) {
												3, 4 -> {
													val menu = PopupMenu(requireContext(), holder.itemView)
													(if (position == 4) transportation else destination)!!.forEach { e: Any? ->
														menu.menu.add((e as JSONObject).getString("label"))
															.setOnMenuItemClickListener {
																leave[position] = e.getString("label")
																(adapter as TwoColumnsAdapter).setValue(leave)
																true
															}
													}
													menu.show()
												}
												2, 1 -> {
													val calendar = MaterialDatePicker.Builder.datePicker()
														.setSelection(if (position == 2) if (returnDate.getValue() != null) returnDate.getValue() else MaterialDatePicker.todayInUtcMilliseconds() else if (leaveDate.getValue() != null) leaveDate.getValue() else MaterialDatePicker.todayInUtcMilliseconds())
														.build()
													calendar.show(getParentFragmentManager(), "calendar")
													calendar.addOnPositiveButtonClickListener(MaterialPickerOnPositiveButtonClickListener { aLong: Long? ->
														leave[position] = calendar.headerText
														(adapter as TwoColumnsAdapter).setValue(leave)
														(if (position == 2) returnDate else leaveDate).value = aLong
													})
												}
												5 -> {
													regionDialog.show()
													dialogRegionBinding.confirm.setOnClickListener {
														leave[position] = countryAdapter.result + " " + provinceAdapter.result + " " + cityAdapter.result
														(adapter as TwoColumnsAdapter).setValue(leave)
														regionDialog.dismiss()
													}
												}
											}
										} else if (adapter.itemCount == 2) {
											if (position == 1) {
												val menu = PopupMenu(requireContext(), holder.itemView)
												resources.getStringArray(R.array.registration_info_keys)
													.forEach { i: String? ->
														menu.menu.add(i)
															.setOnMenuItemClickListener {
																stay[position] = i
																(adapter as TwoColumnsAdapter).setValue(stay)
																true
															}
													}
												menu.show()
											}
										}
									}
								}
							}
							
							override fun onCreate(adapter: RecyclerView.Adapter<RecyclerView.ViewHolder?>,
							                      binding: ViewBinding?) {
							}
						})
					holder.itemView.findViewById<View>(R.id.button).visibility = if (position == 0) View.GONE else View.VISIBLE
				}
				
				override fun onCreate(adapter: RecyclerView.Adapter<RecyclerView.ViewHolder?>,
				                      binding: ViewBinding?) {
					(binding as ItemCardBinding).root.addView(MaterialButton(requireContext(), null, com.google.android.material.R.attr.materialButtonTonalStyle).apply {
						id = R.id.button
						layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
							.apply {
								gravity = Gravity.END
								setMargins(0, 0, config.dpToPx(16), config.dpToPx(16))
							}
						setOnClickListener {
							if ("0" == isStay) save(this@LeaveReturnRegistrationFragment.id!!, isStay, if (isEmpty<Long?>(leaveDate.getValue())) "" else Instant.ofEpochMilli(leaveDate.getValue()!!)
								.atZone(ZoneId.systemDefault())
								.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")), if (isEmpty<Long?>(returnDate.getValue())) "" else Instant.ofEpochMilli(returnDate.getValue()!!)
								.atZone(ZoneId.systemDefault())
								.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")), leave[3], leave[4], country, province, city)
							else save(this@LeaveReturnRegistrationFragment.id!!, isStay, stay[1])
						}
						setText(R.string.save)
					})
				}
			})
			getInfo(id)
		}
		return root
	}
	
	fun save(id: String,
	         isStay: String?,
	         leaveTime: String?,
	         returnTime: String?,
	         leaveType: String?,
	         transportation: String?,
	         country: String?,
	         province: String?,
	         city: String?) {
		model.addAndNext("jjrlfx/api/sm-jjrlfx/student/register", "{\"cjlfxgzId\":\"$id\",\"sflx\":\"$isStay\",\"yjlxsj\":\"$leaveTime\",\"yjfxsj\":\"$returnTime\",\"qxlx\":\"$leaveType\",\"jtgj\":\"$transportation\",\"wcd\":{\"gj\":\"$country\",\"sf\":\"$province\",\"cs\":\"$city\"},\"wcdgj\":\"$country\",\"wcdsf\":\"$province\",\"wcdcs\":\"$city\"}", 6)
	}
	
	fun save(id: String, isStay: String?, reason: String?) {
		model.addAndNext("jjrlfx/api/sm-jjrlfx/student/register", "{\"cjlfxgzId\":\"$id\",\"sflx\":\"$isStay\",\"lxyy\":\"$reason\"}", 6)
	}
	
	fun getInfo(id: String?) {
		model.addAndNext("jjrlfx/api/sm-jjrlfx/student/$id/info", 0)
	}
	
	fun getTransportation() {
		model.addAndNext("jjrlfx/api/sm-jjrlfx/student/transport", 1)
	}
	
	fun getDestination() {
		model.addAndNext("jjrlfx/api/sm-jjrlfx/student/destination-type", 2)
	}
	
	fun getCountry() {
		model.addAndNext("jjrlfx/api/sm-jjrlfx/student/country/drop", 3)
	}
	
	fun getProvince() {
		model.addAndNext("jjrlfx/api/sm-jjrlfx/student/province/drop?0=%E4%B8%AD&1=%E5%9B%BD", 4)
	}
	
	fun getCity(province: String?) {
		model.addAndNext("jjrlfx/api/sm-jjrlfx/student/city/drop?fdm=$province", 5)
	}
	
	internal class OneColumnAdapter : RecyclerAdapter<String?>() {
		var action: ((Int?) -> Unit)? = null
		var selection: Int = -1
		override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
			return object :
				RecyclerView.ViewHolder(ItemTitleBinding.inflate(LayoutInflater.from(parent.context), parent, false).root) {}
		}
		
		override fun onBindViewHolder(holder: RecyclerView.ViewHolder, pos: Int) {
			val position = holder.getBindingAdapterPosition()
			ItemTitleBinding.bind(holder.itemView).apply {
				title.text = get(position)
				root.setBackgroundResource(if (position == selection) R.drawable.bg_selected else R.drawable.box_background)
				root.setOnClickListener {
					action?.invoke(position)
					selection = position
					notifyItemRangeChanged(0, itemCount)
				}
			}
			
			super.onBindViewHolder(holder, pos)
		}
		
		var result: String?
			get() = if (selection == -1) "" else get(selection)
			set(result) {
				if (data.contains(result)) selection = data.indexOf(result)
			}
	}
}
