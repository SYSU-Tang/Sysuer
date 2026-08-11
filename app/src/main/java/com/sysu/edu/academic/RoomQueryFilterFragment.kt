package com.sysu.edu.academic

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.util.Pair
import androidx.lifecycle.Observer
import androidx.navigation.NavOptions
import androidx.navigation.Navigation.findNavController
import androidx.preference.ListPreference
import androidx.preference.MultiSelectListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import com.alibaba.fastjson2.JSONObject
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.datepicker.MaterialPickerOnPositiveButtonClickListener
import com.sysu.edu.R
import com.sysu.edu.databinding.FragmentQueryBinding
import com.sysu.edu.model.JwxtModel
import com.sysu.edu.preference.FilterPreference
import com.sysu.edu.preference.PreferenceUtil
import rikka.material.preference.MaterialSwitchPreference
import rikka.preference.SimpleMenuPreference
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Objects

class RoomQueryFilterFragment : PreferenceFragmentCompat() {
	lateinit var model: JwxtModel
	lateinit var datePicker: MaterialDatePicker<Pair<Long?, Long?>?>
	override fun onDestroyView() {
		super.onDestroyView()
		model.dispose()
	}
	
	override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
		setPreferencesFromResource(R.xml.room_query_filter, rootKey)
	}
	
	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?,
	                         ): View {
		val list = super.onCreateView(inflater, container, savedInstanceState) as LinearLayout
		model = JwxtModel(requireContext())
		val binding = FragmentQueryBinding.inflate(inflater, container, false).apply {
			root.addView(list)
			fab.setOnClickListener {
				findNavController(root).navigate(R.id.query_to_result, Bundle().apply {
					putString("params", "$params")
				}, NavOptions.Builder().build())
			}
		}
		val isWeekPreference = findPreference<MaterialSwitchPreference>("isWeek")!!
		val campusPreference = findPreference<SimpleMenuPreference>("campus")!!
		val buildingPreference = findPreference<SimpleMenuPreference>("teachingBuilding")!!
		val classroomPreference = findPreference<FilterPreference>("classroom")!!
		val weekSelection = findPreference<PreferenceCategory>("weekSelection")!!
		val dateSelection = findPreference<PreferenceCategory>("dateSelection")!!
		model.message.observe(requireActivity()) { (what, response) ->
			val code = response.getInteger("code")
			if (code == 200) {
				val option = mutableListOf<String>()
				val number = mutableListOf<String>()
				val data = response.getJSONArray("data")
				option.add("")
				number.add("")
				if (what < 4) {
					data.forEach { e: Any? ->
						option.add((e as JSONObject).getString(mutableListOf("campusName", "name", "acadYearSemester", "number")[what]))
						number.add(e.getString(mutableListOf("id", "id", "acadYearSemester", "id")[what]))
					}
					preferenceManager.findPreference<ListPreference>(mutableListOf("campus", "teachingBuilding", "yearSemester", "classroom")[what])?.let {
						it.entries = option.toTypedArray()
						it.entryValues = number.toTypedArray()
					}
				}
				else {
					data.forEach { e: Any? ->
						val item = e as JSONObject
						option.add(item.getString(mutableListOf("name", "number")[what - 4]))
						number.add(item.getString(mutableListOf("id", "id")[what - 4]))
					}
					preferenceManager.findPreference<ListPreference>(mutableListOf("teachingBuilding", "classroom")[what - 4])?.let {
						it.entries = option.toTypedArray()
						it.entryValues = number.toTypedArray()
					}
				}
				model.nextAll()
			}
		}
		(0..<4).forEach { getData(it) }
		isWeekPreference.setOnPreferenceChangeListener { _: Preference?, newValue: Any? ->
			val isWeek = newValue as Boolean
			weekSelection.isVisible = isWeek
			dateSelection.isVisible = !isWeek
			true
		}
		campusPreference.setOnPreferenceChangeListener { _: Preference?, newValue: Any? ->
			getTeachingBuilding(newValue as String?)
			getClassRoom(newValue, buildingPreference.value, classroomPreference.valueLiveData.getValue())
			true
		}
		buildingPreference.setOnPreferenceChangeListener { _: Preference?, newValue: Any? ->
			getClassRoom(campusPreference.value, newValue as String?, classroomPreference.valueLiveData.getValue())
			true
		}
		val datePreference = findPreference<Preference>("date")
		datePicker = MaterialDatePicker.Builder.dateRangePicker().build()
		datePicker.addOnPositiveButtonClickListener(MaterialPickerOnPositiveButtonClickListener { _: Pair<Long?, Long?>? ->
			datePreference?.setSummary(datePicker.headerText)
		})
		classroomPreference.valueLiveData.observe(requireActivity(), Observer { value: String? -> getClassRoom(campusPreference.value, buildingPreference.value, value) })
		datePreference?.setOnPreferenceClickListener { _: Preference? ->
			datePicker.show(getChildFragmentManager(), "date_picker")
			true
		}
		return binding.root
	}
	
	fun getData(pos: Int) {
		model.add(mutableListOf<String?>("jwxt/base-info/campus/findCampusNamesBox", "jwxt/base-info/teaching-building/pull", "jwxt/base-info/acadyearterm/findAcadyeartermNamesBox", "jwxt/base-info/classroom/queryclassroombymulticondition")[pos],
		          pos)
	}
	
	fun getTeachingBuilding(campus: String?) {
		model.addAndNext("jwxt/base-info/teaching-building/pull?campusId=${campus ?: ""}", 4)
	}
	
	fun getClassRoom(campus: String?, building: String?, value: String?) {
		model.addAndNext("jwxt/base-info/classroom/queryclassroombymulticondition?campusId=${campus ?: ""}&buildingId=${building ?: ""}&classroomCode=${value ?: ""}", 5)
	}
	
	val params: JSONObject
		/*
			 * {"campusId":"5062201","teachingBuildID":"2513856","classroomID":"2514104","sectionA":"1","sectionB":"12","checkType":"2","yearTerm":"2025-1","weekA":"11","weekB":"11","singleOrDoubleWeek":"0","dayWeeks":["日","一","二"],"weekOrTime":"week"}
			 * */
		get() {
			val preferenceUtil = PreferenceUtil(this).apply {
				insertMenuValue("campus", "campusId")
				insertMenuValue("teachingBuilding", "teachingBuildID")
				insertFilterValue("classroom", "classroomID")
				insertSliderValue("classBegin", "sectionA")
				insertSliderValue("classEnd", "sectionB")
				insertMenuValue("checkType", "checkType")
				insertMenuValue("occupySource", "occupySource")
				insertEditValue("occupyReason", "occupyReason")
			}
			val isWeek = findPreference<MaterialSwitchPreference>("isWeek")?.isChecked
			preferenceUtil.params["weekOrTime"] = if (isWeek == true) "week" else "time"
			if (isWeek == true) {
				preferenceUtil.insertMenuValue("yearSemester", "yearTerm")
				preferenceUtil.insertSliderValue("weekBegin", "weekA")
				preferenceUtil.insertSliderValue("weekEnd", "weekB")
				preferenceUtil.insertMenuValue("weekTime", "singleOrDoubleWeek")
				preferenceUtil.insert("dayWeeks", ((Objects.requireNonNull<Any?>(findPreference("weekdays")) as MultiSelectListPreference).values))
			}
			else if (datePicker.getSelection() != null) {
				if (datePicker.getSelection()!!.first != null) preferenceUtil.insert("dateA", Instant.ofEpochMilli(datePicker.getSelection()!!.first!!).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
				if (datePicker.getSelection()!!.second != null) preferenceUtil.insert("dateB", Instant.ofEpochMilli(datePicker.getSelection()!!.second!!).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
			}
			return preferenceUtil.params
		}
}
