package com.sysu.edu.academic

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.navigation.NavOptions
import androidx.navigation.Navigation.findNavController
import androidx.preference.ListPreference
import androidx.preference.PreferenceFragmentCompat
import com.alibaba.fastjson2.JSONObject
import com.sysu.edu.R
import com.sysu.edu.api.CommonUtil
import com.sysu.edu.databinding.FragmentQueryBinding
import com.sysu.edu.model.JwxtModel
import com.sysu.edu.preference.FilterPreference
import com.sysu.edu.preference.PreferenceUtil
import com.sysu.edu.preference.RangeSliderPreference
import com.sysu.edu.preference.SliderPreference
import rikka.preference.SimpleMenuPreference

class CourseQueryFilterFragment : PreferenceFragmentCompat() {
	lateinit var model: JwxtModel
	override fun onDestroyView() {
		super.onDestroyView()
		model.dispose()
	}
	
	override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
		setPreferencesFromResource(R.xml.course_query_filter, rootKey)
	}
	
	override fun onCreateView(inflater: LayoutInflater,
	                          container: ViewGroup?,
	                          savedInstanceState: Bundle?): View {
		model = JwxtModel(requireContext())
		val binding = FragmentQueryBinding.inflate(inflater, container, false).apply {
			root.addView(super.onCreateView(inflater, container, savedInstanceState))
			fab.setOnClickListener {
				findNavController(root).navigate(R.id.query_to_result, Bundle().apply {
					putString("params", "$params")
				}, NavOptions.Builder().build())
			}
		}
		findPreference<FilterPreference>("department")?.getValueLiveData()
			?.observe(requireActivity(), Observer { text: String? -> getTeachingBuilding(text) })
		findPreference<FilterPreference>("classroom")?.getValueLiveData()
			?.observe(requireActivity(), Observer { text: String? -> getClassroom(text ?: "") })
		(0..<6).forEach { pos: Int -> this.getData(pos) }
		model.message.observe(requireActivity(), Observer { message: CommonUtil.Tuple2<Int, JSONObject> ->
			val response = message.second
			if (response.getInteger("code") == 200) {
				val option = ArrayList<String?>()
				val number = ArrayList<String?>()
				val data = response.getJSONArray("data")
				when (val what: Int = message.first) {
					0, 1, 2, 3, 4, 5 -> {
						option.add("")
						number.add("")
						data.forEach { e: Any? ->
							val item = e as JSONObject
							option.add(item.getString(mutableListOf<String?>("acadYearSemester", "campusName", "dataName", "dataName", "name", "departmentName")[what]))
							number.add(item.getString(mutableListOf<String?>("acadYearSemester", "id", "dataNumber", "dataNumber", "id", "departmentNumber")[what]))
						}
						findPreference<ListPreference>(mutableListOf<String?>("yearSemester", "campus", "classLevel", "teachingType", "teachingBuilding", "department")[what]!!)?.apply {
							entries = option.toArray<String?>(arrayOf<String?>())
							entryValues = number.toArray<String?>(arrayOf<String?>())
						}
						if (what == 0) findPreference<SimpleMenuPreference>("endYear")?.apply {
							entries = option.toArray<String?>(arrayOf<String?>())
							entryValues = number.toArray<String?>(arrayOf<String?>())
						}
					}
					6 -> {
						data.forEach { e: Any? ->
							option.add((e as JSONObject).getString("number"))
							number.add(e.getString("id"))
						}
						findPreference<FilterPreference>("classroom")?.apply {
							entries = option.toArray<String?>(arrayOf<String?>())
							entryValues = number.toArray<String?>(arrayOf<String?>())
						}
					}
				}
				model.nextAll()
			}
		})
		model.next()
		return binding.getRoot()
	}
	
	//    public void getYearSemester() {
	//        model.add("jwxt/base-info/acadyearterm/findAcadyeartermNamesBox", 0);
	//    }
	//
	//    public void getCampus() {
	//        model.add("jwxt/base-info/campus/findCampusNamesBox", 1);
	//    }
	//
	//    public void getDepartment() {
	//        model.add("jwxt/base-info/department/findCommonDepartmentPull", 2);
	//    }
	//
	//    public void getLevel() {
	//        model.add("jwxt/base-info/codedata/findcodedataNames?datableNumber=216", 3);
	//    }
	//
	//    public void getType() {
	//        model.add("jwxt/base-info/codedata/findcodedataNames?datableNumber=350", 4);
	//    }
	fun getTeachingBuilding(text: String?) {
		model.add("jwxt/base-info/department/findCommonDepartmentPull?nameParm=$text", 5)
	}
	
	fun getClassroom(text: String) {
		model.add("jwxt/base-info/classroom/getClassRoomAllPull", "{\"queryParam\":\"$text\"}", 6)
	}
	
	fun getData(pos: Int) {
		model.add(mutableListOf<String?>("jwxt/base-info/acadyearterm/findAcadyeartermNamesBox", "jwxt/base-info/campus/findCampusNamesBox", "jwxt/base-info/codedata/findcodedataNames?datableNumber=216", "jwxt/base-info/codedata/findcodedataNames?datableNumber=350", "jwxt/base-info/teaching-building/pull", "jwxt/base-info/department/findCommonDepartmentPull")[pos], pos)
	}
	
	val params: JSONObject
		get() {
			val preferenceUtil = PreferenceUtil(this)
			val week = findPreference<SliderPreference>("week")
			val weekRange = findPreference<RangeSliderPreference>("weekRange")
			val classRange = findPreference<RangeSliderPreference>("classRange")
			week?.let { if (it.value != 0) preferenceUtil.insert("weekDay", it.value.toString()) }
			weekRange?.let {
				if (it.values[0].toInt() != 0) preferenceUtil.insert("beginWeek", it.values[0].toInt()
					.toString())
				if (it.values[1].toInt() != 0) preferenceUtil.insert("endWeek", it.values[1].toInt()
					.toString())
			}
			classRange?.let {
				if (it.values[0].toInt() != 0) preferenceUtil.insert("beginLesson", it.values[0].toInt()
					.toString())
				if (it.values[1].toInt() != 0) preferenceUtil.insert("endLesson", it.values[1].toInt()
					.toString())
			}
			preferenceUtil.insertMenuValue("yearSemester", "yearTerm")
			preferenceUtil.insertMenuValue("endYear", "endYearTerm")
			preferenceUtil.insertMenuValue("classLevel", "classLevelNumber")
			preferenceUtil.insertMenuValue("campus", "openingSchoolNumber")
			preferenceUtil.insertMenuValue("courseType", "courseCategoryNumber")
			preferenceUtil.insertMenuValue("teachingBuilding", "teachingBuildingID")
			preferenceUtil.insertMenuValue("teachingType", "teachingTypeNumber")
			preferenceUtil.insertFilterValue("classroom", "classRoomID") //教室
			preferenceUtil.insertFilterValue("department", "openingUnitNumber") //开课单位
			preferenceUtil.insertEditValue("courseName", "courseName") //课程名称
			preferenceUtil.insertEditValue("teacher", "teachingNum") //教师
			preferenceUtil.insertEditValue("classNumber", "classNumber") //班号
			preferenceUtil.insertEditValue("className", "className") //教学班
			preferenceUtil.insertEditValue("courseNumber", "courseNumber") //课程编码
			return preferenceUtil.params/*{"pageNo":1,"pageSize":10,"total":true,"param":{"yearTerm":"2025-1","endYearTerm":"2026-1","openingUnitNumber":"1","courseName":"名称","teachingNum":"教师","openingSchoolNumber":"5063559","courseCategoryNumber":"3286159","classLevelNumber":"1","classNumber":"班号","className":"教学班","teachingTypeNumber":"1","courseNumber":"编码","teachingBuildingID":"2513856","classRoomID":"2514104","weekDay":"1","beginWeek":"1","endWeek":"5","beginLesson":"2","endLesson":"3"}}*/
		}
}