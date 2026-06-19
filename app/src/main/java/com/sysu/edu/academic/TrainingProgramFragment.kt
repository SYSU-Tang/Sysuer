package com.sysu.edu.academic

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.CompoundButton
import android.widget.NumberPicker
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.navigation.Navigation.findNavController
import androidx.navigation.fragment.FragmentNavigator
import com.alibaba.fastjson2.JSONObject
import com.google.android.material.chip.Chip
import com.sysu.edu.BaseFragment
import com.sysu.edu.R
import com.sysu.edu.api.CommonUtil
import com.sysu.edu.databinding.FragmentTrainingScheduleBinding
import com.sysu.edu.databinding.ItemFilterChipBinding
import com.sysu.edu.model.JwxtModel

class TrainingProgramFragment : BaseFragment() {
	lateinit var binding: FragmentTrainingScheduleBinding
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
		val department: MutableLiveData<String?> = MutableLiveData<String?>()
		val profession: MutableLiveData<String?> = MutableLiveData<String?>()
		val type: MutableLiveData<String?> = MutableLiveData<String?>()
		val grade: MutableLiveData<String?> = MutableLiveData<String?>()
		binding = FragmentTrainingScheduleBinding.inflate(inflater).apply {
			unit.addTextChangedListener(object : TextWatcher {
				override fun beforeTextChanged(s: CharSequence?,
				                               start: Int,
				                               count: Int,
				                               after: Int) {
				}
				
				override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
					getColleges("$s")
					model.nextAll()
				}
				
				override fun afterTextChanged(s: Editable?) {
				}
			})
			this.profession.addTextChangedListener(object : TextWatcher {
				override fun beforeTextChanged(s: CharSequence?,
				                               start: Int,
				                               count: Int,
				                               after: Int) {
				}
				
				override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
					getProfessions("$s")
					model.nextAll()
				}
				
				override fun afterTextChanged(s: Editable?) {
				}
			})
			query.setOnClickListener {
				val arg = Bundle().apply {
					putSerializable("unit", department.value)
					putSerializable("profession", profession.value)
					putSerializable("grade", grade.value)
					putSerializable("type", type.value)
				}
				findNavController(binding.root).navigate(R.id.confirmationAction, arg, null, FragmentNavigator.Extras.Builder()
					.addSharedElement(binding.root, "result")
					.build())
			}
		}
		model.message.observe(requireActivity(), Observer { message: CommonUtil.Tuple2<Int, JSONObject> ->
			val data = message.second
			if (data.getInteger("code") == 200) {
				when (message.first) {
					1 -> {
						val list = ArrayList<String?>()
						val unitIDs = ArrayList<String?>()
						data.getJSONArray("data").forEach { e: Any? ->
							unitIDs.add((e as JSONObject).getString("departmentNumber"))
							list.add(e.getString("departmentName"))
						}
						binding.unit.setSimpleItems(list.toArray<String?>(arrayOf<String?>()))
						if (binding.unit.hasFocus()) binding.unit.showDropDown()
						binding.unit.setOnItemClickListener { _: AdapterView<*>?, _: View?, position: Int, _: Long -> department.value = unitIDs[position] }
					}
					2 -> {
						val list = mutableListOf<String?>()
						val professionIDs = mutableListOf<String?>()
						data.getJSONArray("data").forEach { e: Any? ->
							professionIDs.add((e as JSONObject).getString("dataNumber"))
							list.add(e.getString("dataName"))
						}
						binding.grade.apply {
							minValue = 1
							maxValue = list.size
							displayedValues = list.toTypedArray<String?>()
							setOnValueChangedListener { _: NumberPicker?, _: Int, fromUser: Int -> grade.value = professionIDs[fromUser - 1] }
							value = list.size
						}
						grade.value = professionIDs.last()
					}
					3 -> {
						data.getJSONArray("data").forEach { e: Any? ->
							binding.types.addView(ItemFilterChipBinding.inflate(inflater, binding.types, false).root.apply {
								setOnCheckedChangeListener { _: CompoundButton?, isChecked: Boolean ->
									if (isChecked) type.value = (e as JSONObject).getString("dataNumber")
								}
								text = (e as JSONObject).getString("dataName")
							})
						}
						(binding.types.getChildAt(0) as Chip).isChecked = true
					}
					4 -> {
						val list = mutableListOf<String?>()
						val professionIDs = mutableListOf<String?>()
						data.getJSONArray("data").forEach { e: Any? ->
							professionIDs.add((e as JSONObject).getString("code"))
							list.add(e.getString("name"))
						}
						binding.profession.setSimpleItems(list.toTypedArray<String?>())
						if (binding.profession.hasFocus()) binding.profession.showDropDown()
						binding.profession.setOnItemClickListener { _: AdapterView<*>?, _: View?, position: Int, _: Long -> profession.value = professionIDs[position] } // 处理专业
					}
				}
				model.nextAll()
			}
		})
		getColleges("")
		grades
		types
		getProfessions("")
		model.next()
		return binding.root
	}
	
	fun getProfessions(keyword: String?) {
		model.add("jwxt/base-info/profession-direction/pull?majorProfessionDircetion=1&nameCode=$keyword", 4)
	}
	
	val types: Unit
		get() {
			model.add("jwxt/base-info/codedata/findcodedataNames?datableNumber=97", 3)
		}
	
	fun getColleges(keyword: String?) {
		model.add("jwxt/base-info/department/recruitUnitPull", "{\"departmentName\":\"$keyword\",\"subordinateDepartmentNumber\":null,\"id\":null}", 1)
	}
	
	val grades: Unit
		get() {
			model.add("jwxt/base-info/codedata/findcodedataNames?datableNumber=127", 2)
		}
}