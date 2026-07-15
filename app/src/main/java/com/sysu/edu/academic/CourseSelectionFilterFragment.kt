package com.sysu.edu.academic

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.EditText
import androidx.activity.OnBackPressedCallback
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.alibaba.fastjson2.JSONObject
import com.google.android.material.R
import com.google.android.material.transition.MaterialContainerTransform
import com.sysu.edu.BaseFragment
import com.sysu.edu.databinding.FragmentCourseFilterBinding
import com.sysu.edu.model.JwxtModel

class CourseSelectionFilterFragment : BaseFragment() {
	var filterValue: CourseFilterValueData? = null
	var filterName: CourseFilterNameData? = null
	
	//lateinit var vm: CourseSelectionViewModel
	lateinit var binding: FragmentCourseFilterBinding
	lateinit var model: JwxtModel
	override fun onDestroyView() {
		super.onDestroyView()
		model.dispose()
	}
	
	private val args: CourseSelectionFilterFragmentArgs by navArgs()
	override fun onCreateView(inflater: LayoutInflater,
	                          container: ViewGroup?,
	                          savedInstanceState: Bundle?): View {
		super.onCreateView(inflater, container, savedInstanceState)
		model = JwxtModel(requireContext())		//vm = ViewModelProvider(requireActivity())[CourseSelectionViewModel::class.java]
		binding = FragmentCourseFilterBinding.inflate(inflater, container, false)
		binding.container.setColumnCount(config.column)
		model.message.observe(viewLifecycleOwner) { (code, response) ->
			if (response.getInteger("code") == 200) {
				val data = response.getJSONArray("data")
				if (data != null) {
					val items = mutableListOf<String?>()
					val itemCodes = mutableListOf<String?>()
					items.add("")
					itemCodes.add("")
					data.forEach { a: Any? ->
						items.add((a as JSONObject).getString(arrayOf("campusName", "dataName", "minorName", "dataName", "dataName")[code]))
						itemCodes.add(a.getString(arrayOf("id", "dataNumber", "sectionNumber", "dataNumber", "dataNumber")[code]))
					}
					val textView = arrayOf(binding.campus, binding.days, binding.sections, binding.languages, binding.special)[code]
					textView.setSimpleItems(items.toTypedArray())
					textView.setOnItemClickListener { _: AdapterView<*>?, _: View?, i: Int, _: Long ->
						filterValue?.set(arrayOf("campus", "day", "section", "language", "special")[code], itemCodes[i])
						filterName?.set(arrayOf("campus", "day", "section", "language", "special")[code], items[i])
					}
				}
			}
			model.nextAll()
		}
		(0..<5).forEach { getData(it) }
		model.next()
		requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object :
			OnBackPressedCallback(true) {
			override fun handleOnBackPressed() {
				submit()
			}
		})
		args.courseSelectionNameFilter?.let {
			filterName = it
			load()
		}
		args.courseSelectionValueFilter?.let {
			filterValue = it
		}
		return binding.root
	}
	
	private fun load() {
		binding.campus.setText(filterName?.studyCampusId, false)
		binding.course.setText(filterName?.courseName)
		binding.days.setText(filterName?.week, false)
		binding.sections.setText(filterName?.classTimes, false)
		binding.languages.setText(filterName?.teachingLanguageCode, false)
		binding.special.setText(filterName?.specialClassCode, false)
		binding.school.setText(filterName?.courseUnitNum)
		binding.teacher.setText(filterName?.teachingTeacherNum)
	}
	
	fun reset() {
		filterValue = CourseFilterValueData()
		filterName = CourseFilterNameData()
		load()
	}
	
	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		view.transitionName = "miniapp"
		binding.reset.setOnClickListener { reset() }
		binding.submit.setOnClickListener { submit() }
		val transition = MaterialContainerTransform()
		transition.scrimColor = Color.TRANSPARENT
		transition.setAllContainerColors(requireContext().getColor(R.color.design_default_color_surface))
		sharedElementEnterTransition = transition
		sharedElementReturnTransition = transition
	}
	
	fun submit() {
		map
		findNavController().previousBackStackEntry?.savedStateHandle?.apply {
			set("filter_name", filterName)
			set("filter_value", filterValue)
		}
		findNavController().popBackStack()
	}
	
	fun getData(i: Int) {
		model.add(arrayOf("jwxt/base-info/campus/findCampusNamesBox", "jwxt/base-info/codedata/findcodedataNames?datableNumber=233", "jwxt/base-info/AcadyeartermSet/minorName?schoolYear=2025-1", "jwxt/base-info/codedata/findcodedataNames?datableNumber=204", "jwxt/base-info/codedata/findcodedataNames?datableNumber=387")[i], i)
	}
	
	val map: Unit
		get() {
			filterValue?.courseName = getEditText(binding.course)
			filterValue?.teachingTeacherNum = getEditText(binding.teacher)
			filterValue?.courseUnitNum = getEditText(binding.school)
			filterName?.courseName = getEditText(binding.course)
			filterName?.teachingTeacherNum = getEditText(binding.teacher)
			filterName?.courseUnitNum = getEditText(binding.school)
		}
	
	fun getEditText(editText: EditText): String {
		return editText.text.toString().trim()
	}
}