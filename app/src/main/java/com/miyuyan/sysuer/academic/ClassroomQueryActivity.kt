package com.miyuyan.sysuer.academic

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.LinearLayout
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import androidx.viewbinding.ViewBinding
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
import com.miyuyan.sysuer.BaseActivity
import com.miyuyan.sysuer.R
import com.miyuyan.sysuer.api.DateTimeManager
import com.miyuyan.sysuer.databinding.ActivityClassroomQueryBinding
import com.miyuyan.sysuer.databinding.ItemClassroomResultBinding
import com.miyuyan.sysuer.databinding.ItemFilterChipBinding
import com.miyuyan.sysuer.model.JwxtModel
import com.miyuyan.sysuer.view.AdapterListener
import com.miyuyan.sysuer.view.RecyclerAdapter
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter

class ClassroomQueryActivity : BaseActivity() {
	val office: MutableMap<Int, String> = mutableMapOf()
	val campus: MutableLiveData<String> = MutableLiveData<String>()
	val model: JwxtModel by lazy {
		JwxtModel(this)
	}
	var dateMillis: MutableLiveData<Long> = MutableLiveData(System.currentTimeMillis())
	var startClassTime: Int = 1
	var endClassTime: Int = 11
	var page: Int = 1
	var total: Int = 0
	lateinit var binding: ActivityClassroomQueryBinding
	override fun onDestroy() {
		super.onDestroy()
		model.dispose()
	}

	@OptIn(ExperimentalStdlibApi::class)
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		val roomAdapter = RoomAdapter()
		val dateDialog =
			MaterialDatePicker.Builder.datePicker().setSelection(dateMillis.value).build()
		val classroom = mutableMapOf<String, MutableList<Chip>>()
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
			result.layoutManager =
				StaggeredGridLayoutManager(config.column, StaggeredGridLayoutManager.VERTICAL)
			BottomSheetBehavior.from<LinearLayout?>(resultSheet)
				.setState(BottomSheetBehavior.STATE_HIDDEN)
			date.setOnClickListener {
				dateDialog.show(supportFragmentManager, null)
			}
			timeSlider.addOnChangeListener { slider: RangeSlider, _, _ ->
				val (start, end) = slider.values
				startClassTime = start.toInt()
				endClassTime = end.toInt()
				time.text = getString(R.string.from_to_section, startClassTime, endClassTime)
			}
			query.setOnClickListener {
				roomAdapter.clear()
				page = 1
				room()
			}
			result.addOnScrollListener(object : RecyclerView.OnScrollListener() {
				override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
					if (!recyclerView.canScrollVertically(1) && total / 20 + 1 >= page) room()
				}
			})
			reset.setOnClickListener {
				officeGroup.checkedChipIds.forEach { e: Int ->
					(officeGroup.findViewById<View>(e) as Chip).isChecked = false
				}
				campusGroup.checkedChipIds.forEach { e: Int ->
					(campusGroup.findViewById<View>(e) as Chip).isChecked = false
				}
				typeGroup.checkedChipIds.forEach { e: Int ->
					(typeGroup.findViewById<View>(e) as Chip).isChecked = true
				}
				timeSlider.values = mutableListOf(1.0f, 11.0f)
				dateMillis.value = System.currentTimeMillis()
			}
		}
		setContentView(binding.root)
		dateMillis.observe(this) { dateMillis ->
			binding.dateText.text = DateTimeManager.toDateString(
					dateMillis, DateTimeFormatter.ofPattern("yyyy年MM月dd日")
			)
		}
		dateDialog.addOnPositiveButtonClickListener(MaterialPickerOnPositiveButtonClickListener { selection: Long ->
			dateMillis.value = selection
		})
		campus()
		roomAdapter.listener = object : AdapterListener {
			override fun onBind(
				adapter: RecyclerView.Adapter<RecyclerView.ViewHolder?>,
				holder: RecyclerView.ViewHolder,
				position: Int
			) {
				val item = roomAdapter.get(position)
				ItemClassroomResultBinding.bind(holder.itemView).apply {
					location.text = item.getString("teachingBuildingName")
					time.text = item.getString("classTimes")
					floor.text = item.getString("floor")
					seat.text = item.getString("seats")
					type.text = item.getString("classRoomTag")
					name.text = item.getString("classRoomNum")
					root.setOnClickListener {}
					Glide.with(root.context).load(
							GlideUrl(
									"https://${model.host}/jwxt/base-info/classroom/classRoomView?fileName=jspic.png&filePath=" + item.get(
											"photoPath"
									),
									LazyHeaders.Builder().addHeader("Cookie", model.cookie)
										.addHeader("Referer", "https://jwxt.sysu.edu.cn/").build()
							)
					).placeholder(R.drawable.logo)
						.override((145 * 3.6).toInt(), (132 * 3.6).toInt()).fitCenter().into(image)
				}
			}

			override fun onCreate(
				adapter: RecyclerView.Adapter<RecyclerView.ViewHolder?>, binding: ViewBinding?
			) {
			}
		}
		lifecycleScope.launch {
			repeatOnLifecycle(Lifecycle.State.STARTED) {
				model.messageChannel.collect { (code, response) ->
					if (response.getInteger("code") == 200) {
						when (code) {
							3 -> {
								val data = response.getJSONObject("data")
								total = data.getInteger("total")
								data.getJSONArray("rows")
									.forEach { a: Any? -> roomAdapter.add(a as JSONObject) }
								BottomSheetBehavior.from<LinearLayout?>(binding.resultSheet)
									.setState(BottomSheetBehavior.STATE_EXPANDED)
							}

							else -> {
								binding.timeSlider.valueFrom = 1f
								response.getJSONArray("data").forEach { campusInfo: Any? ->
									when (code) {
										1 -> {
											val id = (campusInfo as JSONObject).getString("id")
											val chip = ItemFilterChipBinding.inflate(
													layoutInflater, binding.campusGroup, false
											).root.apply {
												text = campusInfo.getString("campusName")
												setOnCheckedChangeListener { _: CompoundButton?, isChecked: Boolean ->
													if (isChecked) {
														if (classroom.containsKey(id)) classroom[id]?.forEach { e: Chip ->
															e.isVisible = true
														}
														else getOffice(id)
													} else classroom[id]?.forEach { e: Chip ->
														e.isVisible = false
													}
												}
											}
											binding.campusGroup.addView(chip)
										}

										2 -> {
											val chip = ItemFilterChipBinding.inflate(
													layoutInflater, binding.officeGroup, false
											).root
											binding.officeGroup.addView(chip)
											office[chip.id] =
												(campusInfo as JSONObject).getString("id")
											chip.text = campusInfo.getString("dataName")
											classroom.getOrPutIfNull(
													campus.value ?: ""
											) { mutableListOf() }.add(chip)
										}
									}
								}
							}
						}
					}
				}
			}
		}
	}

	private fun campus() {
		model.addAndNext("jwxt/base-info/campus/findCampusNamesBox", 1)
	}

	fun getOffice(campusName: String) {
		campus.value = campusName
		model.addAndNext(
				"jwxt/schedule/agg/selfStudyClassRoom/buildingConditionPull",
				"{\"campusIdList\":[\"$campusName\"]}",
				2
		)
	}

	private fun room() {
		val teachingBuildIDs = mutableListOf<String?>()
		val classType = mutableListOf<String>()
		binding.typeGroup.checkedChipIds.forEach { e: Int ->
			classType.add(if (e == R.id.self_study_room) "003" else "002")
		}
		binding.officeGroup.checkedChipIds.filter { findViewById<View>(it).isVisible }
			.forEach { e: Int ->
				teachingBuildIDs.add(office[e])
			}
		if (teachingBuildIDs.isEmpty()) model.contextUtil.toast(R.string.select_teaching_building)
		else model.addAndNext(
				"jwxt/schedule/agg/selfStudyClassRoom/pageListStudyClassroom",
				"{\"pageNo\":${page++},\"pageSize\":20,\"param\":{\"dateStr\":\"${
					DateTimeManager.toDateString(dateMillis.value ?: System.currentTimeMillis())
				}\",\"teachingBuildIDs\":${
					JSONArray.toJSONString(teachingBuildIDs)
				},\"startClassTimes\":$startClassTime,\"endClassTimes\":$endClassTime,\"classRoomTagList\":${
					JSONArray.toJSONString(
							classType
					)
				}}}",
				3
		)
	}

	class RoomAdapter : RecyclerAdapter<JSONObject>() {
		override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
			object : RecyclerView.ViewHolder(
					ItemClassroomResultBinding.inflate(
							LayoutInflater.from(parent.context), parent, false
					).root
			) {}
	}
}
