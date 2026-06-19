package com.sysu.edu.academic

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.LinearLayout
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.alibaba.fastjson2.JSONArray
import com.alibaba.fastjson2.JSONObject
import com.bumptech.glide.Glide
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.model.LazyHeaders
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.chip.Chip
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.datepicker.MaterialPickerOnPositiveButtonClickListener
import com.google.android.material.slider.RangeSlider
import com.sysu.edu.BaseActivity
import com.sysu.edu.R
import com.sysu.edu.api.CommonUtil
import com.sysu.edu.databinding.ActivityClassroomQueryBinding
import com.sysu.edu.databinding.ItemClassroomResultBinding
import com.sysu.edu.databinding.ItemFilterChipBinding
import com.sysu.edu.model.JwxtModel
import com.sysu.edu.view.RecyclerAdapter
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class ClassroomQueryActivity : BaseActivity() {
	val office: MutableMap<Int?, String?> = mutableMapOf()
	val campusLiveData: MutableLiveData<String?> = MutableLiveData<String?>()
	lateinit var model: JwxtModel
	var dateStr: String? = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
	var startClassTime: String = "1"
	var endClassTime: String = "11"
	var page: Int = 1
	var total: Int = 0
	lateinit var binding: ActivityClassroomQueryBinding
	override fun onDestroy() {
		super.onDestroy()
		model.dispose()
	}
	
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		val roomAdapter = RoomAdapter()
		val dateDialog = MaterialDatePicker.Builder.datePicker().build()
		val classroom = mutableMapOf<String?, ArrayList<Chip?>?>()
		binding = ActivityClassroomQueryBinding.inflate(layoutInflater).apply {
			campusSelectAll.setOnClickListener {
				campusGroup.children.drop(0).forEach {
					(it as Chip).toggle()
				}
			}
			officeSelectAll.setOnClickListener {
				officeGroup.children.drop(0).forEach {
					(it as Chip).toggle()
				}
			}
			toolbar.setNavigationOnClickListener { supportFinishAfterTransition() }
			result.adapter = roomAdapter
			result.layoutManager = StaggeredGridLayoutManager(config.column, StaggeredGridLayoutManager.VERTICAL)
			BottomSheetBehavior.from<LinearLayout?>(resultSheet)
				.setState(BottomSheetBehavior.STATE_HIDDEN)
			date.setOnClickListener {
				dateDialog.show(supportFragmentManager, null)
			}
			timeSlider.addOnChangeListener { slider: RangeSlider?, _, _ ->
				startClassTime = String.format(Locale.getDefault(), "%.0f", slider!!.values[0])
				endClassTime = String.format(Locale.getDefault(), "%.0f", slider.values[1])
				time.text = String.format(getString(R.string.section_range_x), startClassTime, endClassTime)
			}
			query.setOnClickListener {
				roomAdapter.clear()
				page = 1
				room
			}
			result.addOnScrollListener(object : RecyclerView.OnScrollListener() {
				override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
					if (!recyclerView.canScrollVertically(1) && total / 20 + 1 >= page) room
				}
			})
			reset.setOnClickListener {
				officeGroup.checkedChipIds.forEach { e: Int? ->
					(officeGroup.findViewById<View>(e!!) as Chip).isChecked = false
				}
				campusGroup.checkedChipIds.forEach { e: Int? ->
					(campusGroup.findViewById<View>(e!!) as Chip).isChecked = false
				}
				typeGroup.checkedChipIds.forEach { e: Int? ->
					(typeGroup.findViewById<View>(e!!) as Chip).isChecked = true
				}
				timeSlider.values = mutableListOf(1.0f, 11.0f)
				dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
				dateText.text = LocalDate.now()
					.format(DateTimeFormatter.ofPattern("yyyy年MM月dd日"))
			}
			dateText.text = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy年MM月dd日"))
		}
		setContentView(binding.getRoot())
		model = JwxtModel(this)
		dateDialog.addOnPositiveButtonClickListener(MaterialPickerOnPositiveButtonClickListener { selection: Long? ->
			val date = Instant.ofEpochMilli(selection!!)
				.atZone(ZoneId.systemDefault())
				.toLocalDate()
			dateStr = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
			binding.dateText.text = date.format(DateTimeFormatter.ofPattern("yyyy年MM月dd日"))
		})
		campus
		model.message.observe(this, Observer { message: CommonUtil.Tuple2<Int, JSONObject> ->
			val response = message.second
			if (response.getInteger("code") == 200) {
				if (message.first == 3) {
					val data = response.getJSONObject("data")
					total = data.getInteger("total")
					data.getJSONArray("rows")
						.forEach { a: Any? -> roomAdapter.add(a as JSONObject?) }
					BottomSheetBehavior.from<LinearLayout?>(binding.resultSheet)
						.setState(BottomSheetBehavior.STATE_EXPANDED)
					roomAdapter.setHost(model.host)
					roomAdapter.setCookie(model.cookieManager!!.toSimpleString(model.host))
				} else {
					binding.timeSlider.valueFrom = 1f
					response.getJSONArray("data").forEach { campusInfo: Any? ->
						when (message.first) {
							1 -> {
								val id = (campusInfo as JSONObject).getString("id")
								val chip = ItemFilterChipBinding.inflate(layoutInflater, binding.campusGroup, false)
									.getRoot()
								binding.campusGroup.addView(chip)
								chip.setOnCheckedChangeListener { _: CompoundButton?, isChecked: Boolean ->
									if (isChecked) {
										if (classroom.containsKey(id)) classroom[id]?.forEach { e: Chip? ->
											e!!.visibility = View.VISIBLE
										}
										else getOffice(id)
									} else classroom[id]?.forEach { e: Chip? ->
										e!!.visibility = View.GONE
									}
								}
								chip.text = campusInfo.getString("campusName")
							}
							2 -> {
								classroom.computeIfAbsent(campusLiveData.getValue()) { _: String? -> ArrayList() }
								val chip = ItemFilterChipBinding.inflate(layoutInflater, binding.officeGroup, false)
									.getRoot()
								binding.officeGroup.addView(chip)
								office[chip.id] = (campusInfo as JSONObject).getString("id")
								chip.text = campusInfo.getString("dataName")
								classroom[campusLiveData.getValue()]?.add(chip)
							}
						}
					}
				}
				model.nextAll()
			}
		})
		model.next()
	}
	
	val campus: Unit
		get() {
			model.add("jwxt/base-info/campus/findCampusNamesBox", 1)
		}
	
	fun getOffice(campus: String?) {
		campusLiveData.value = campus
		model.addAndNext("jwxt/schedule/agg/selfStudyClassRoom/buildingConditionPull", "{\"campusIdList\":[\"$campus\"]}", 2)
	}
	
	val room: Unit
		get() {
			val teachingBuildIDs = mutableListOf<String?>()
			val classType = mutableListOf<String>()
			binding.typeGroup.checkedChipIds.forEach { e: Int? ->
				classType.add(if (e == R.id.self_study_room) "003" else "002")
			}
			binding.officeGroup.checkedChipIds.forEach { e: Int? ->
				if (findViewById<View>(e!!).isVisible) teachingBuildIDs.add(office[e])
			}
			if (teachingBuildIDs.isEmpty()) model.contextUtil.toast(R.string.select_teaching_building)
			else model.addAndNext("jwxt/schedule/agg/selfStudyClassRoom/pageListStudyClassroom", "{\"pageNo\":${page++},\"pageSize\":20,\"param\":{\"dateStr\":\"$dateStr\",\"teachingBuildIDs\":${
				JSONArray.toJSONString(teachingBuildIDs)
			},\"startClassTimes\":$startClassTime,\"endClassTimes\":$endClassTime,\"classRoomTagList\":${JSONArray.toJSONString(classType)}}", 3)
		}
	
	class RoomAdapter : RecyclerAdapter<JSONObject>() {
		private var host: String? = null
		private var cookie: String? = null
		override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
			return object :
				RecyclerView.ViewHolder(ItemClassroomResultBinding.inflate(LayoutInflater.from(parent.context), parent, false)
											.getRoot()) {}
		}
		
		fun setHost(host: String?) {
			this.host = host
		}
		
		fun setCookie(cookie: String) {
			this.cookie = cookie
		}
		
		override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
			val item = get(position)
			ItemClassroomResultBinding.bind(holder.itemView).apply {
				location.text = item.getString("teachingBuildingName")
				time.text = item.getString("classTimes")
				floor.text = item.getString("floor")
				seat.text = item.getString("seats")
				type.text = item.getString("classRoomTag")
				name.text = item.getString("classRoomNum")
				getRoot().setOnClickListener {}
				Glide.with(root.context)
					.load(GlideUrl("https://$host/jwxt/base-info/classroom/classRoomView?fileName=jspic.png&filePath=" + item.get("photoPath"), LazyHeaders.Builder()
						.addHeader("Cookie", cookie!!)
						.addHeader("Referer", "https://jwxt.sysu.edu.cn/")
						.build()))
					.placeholder(R.drawable.logo)
					.override((145 * 3.6).toInt(), (132 * 3.6).toInt())
					.fitCenter()
					.into(image)
			}
			super.onBindViewHolder(holder, position)
		}
	}
}
