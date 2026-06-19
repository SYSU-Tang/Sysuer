package com.sysu.edu.academic

import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.EditText
import androidx.activity.OnBackPressedCallback
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.Navigation.findNavController
import com.alibaba.fastjson2.JSONObject
import com.google.android.material.R
import com.google.android.material.transition.MaterialContainerTransform
import com.sysu.edu.BaseFragment
import com.sysu.edu.api.CommonUtil
import com.sysu.edu.api.CommonUtil.toStringOrDefault
import com.sysu.edu.databinding.FragmentCourseFilterBinding
import com.sysu.edu.model.JwxtModel

class CourseSelectionFilterFragment : BaseFragment() {
	var filterValue: MutableMap<String?, String?> = mutableMapOf()
	var filterName: MutableMap<String?, String?> = mutableMapOf()
	lateinit var vm: CourseSelectionViewModel
	lateinit var binding: FragmentCourseFilterBinding
	var navController: NavController? = null
	lateinit var model: JwxtModel
	override fun onDestroyView() {
		super.onDestroyView()
		model.dispose()
	}
	
	override fun onCreateView(inflater: LayoutInflater,
	                          container: ViewGroup?,
	                          savedInstanceState: Bundle?): View {
		super.onCreateView(inflater, container, savedInstanceState)
		model = JwxtModel(requireContext())
		vm = ViewModelProvider(requireActivity())[CourseSelectionViewModel::class.java]
		vm.getFilterValue()?.also { filterValue = it }
		vm.getFilterName()?.also { filterName = it }
		binding = FragmentCourseFilterBinding.inflate(inflater, container, false)
		binding.container.setColumnCount(config.column)
		model.message.observe(requireActivity(), Observer { message: CommonUtil.Tuple2<Int, JSONObject> ->
			val response = message.second
			val what: Int = message.first
			if (response.getInteger("code") == 200) {
				val data = response.getJSONArray("data")
				if (data != null) {
					val items = mutableListOf<String?>()
					val itemCodes = mutableListOf<String?>()
					items.add("")
					itemCodes.add("")
					data.forEach { a: Any? ->
						items.add((a as JSONObject).getString(arrayOf("campusName", "dataName", "minorName", "dataName", "dataName")[what]))
						itemCodes.add(a.getString(arrayOf("id", "dataNumber", "sectionNumber", "dataNumber", "dataNumber")[what]))
					}
					val textView = arrayOf(binding.campus, binding.days, binding.sections, binding.languages, binding.special)[what]
					textView.setSimpleItems(items.toTypedArray())
					textView.setOnItemClickListener { _: AdapterView<*>?, _: View?, i: Int, _: Long ->
						filterValue[arrayOf("campus", "day", "section", "language", "special")[what]] = itemCodes[i]
						filterName[arrayOf("campus", "day", "section", "language", "special")[what]] = items[i]
					}
				}
			}
			model.nextAll()
		})
		(0..<5).forEach { getData(it) }
		load()
		model.next()
		requireActivity().onBackPressedDispatcher.addCallback(getViewLifecycleOwner(), object :
			OnBackPressedCallback(true) {
			override fun handleOnBackPressed() {
				submit()
			}
		})
		return binding.getRoot()
	}
	
	fun reset() {
		filterValue.clear()
		filterName.clear()
		vm.setFilterName(filterName)
		vm.setFilterValue(filterValue)
		load()
	}
	
	fun load() {
		vm.getFilterName()?.also { filterName = it }
		vm.getFilterValue()?.also { filterValue = it }
		binding.campus.setText(filterName.getOrDefault("campus", ""), false)
		binding.course.setText(filterName.getOrDefault("course", ""))
		binding.days.setText(filterName["day"], false)
		binding.sections.setText(filterName["section"], false)
		binding.languages.setText(filterName["language"], false)
		binding.special.setText(filterName.getOrDefault("special", ""), false)
		binding.school.setText(filterName["school"])
		binding.teacher.setText(filterName["teacher"])
	}
	
	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		view.transitionName = "miniapp"
		navController = findNavController(view)
		binding.reset.setOnClickListener { reset() }
		binding.submit.setOnClickListener { submit() }
		val transition = MaterialContainerTransform()
		transition.scrimColor = Color.TRANSPARENT
		transition.setAllContainerColors(requireContext().getColor(R.color.design_default_color_surface))
		sharedElementEnterTransition = transition
		sharedElementReturnTransition = transition
	}
	
	fun submit() {
		vm.returnData = parseFilter(map)
		vm.setFilterName(filterName)
		vm.setFilterValue(filterValue)
		navController!!.navigateUp()
	}
	
	fun getData(i: Int) {
		model.add(arrayOf("jwxt/base-info/campus/findCampusNamesBox", "jwxt/base-info/codedata/findcodedataNames?datableNumber=233", "jwxt/base-info/AcadyeartermSet/minorName?schoolYear=2025-1", "jwxt/base-info/codedata/findcodedataNames?datableNumber=204", "jwxt/base-info/codedata/findcodedataNames?datableNumber=387")[i], i)
	}
	
	val map: MutableMap<String?, String?>
		get() {
			filterValue["course"] = getEditText(binding.course)
			filterValue["teacher"] = getEditText(binding.teacher)
			filterValue["school"] = getEditText(binding.school)
			filterName["course"] = getEditText(binding.course)
			filterName["teacher"] = getEditText(binding.teacher)
			filterName["school"] = getEditText(binding.school)
			return filterValue
		}
	
	fun getEditText(editText: EditText): String {
		return toStringOrDefault<Editable?>(editText.getText())
	}
	
	fun parseFilter(filter: MutableMap<String?, String?>): String {
		val json = JSONObject()
		val keys: Array<String?> = arrayOf("course", "campus", "day", "section", "school", "teacher", "language", "special")
		val name: Array<String?> = arrayOf("courseName", "studyCampusId", "week", "classTimes", "courseUnitNum", "teachingTeacherNum", "teachingLanguageCode", "specialClassCode")
		keys.forEachIndexed { i, k ->
			filter.getOrDefault(k, "")?.takeIf { it.isNotEmpty() }?.let {
				json[name[i]] = it
			}
		}
		return "$json"
	}
}