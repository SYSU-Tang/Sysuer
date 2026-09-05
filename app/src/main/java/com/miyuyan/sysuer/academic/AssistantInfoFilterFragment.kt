package com.miyuyan.sysuer.academic

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import androidx.appcompat.widget.PopupMenu
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.navigation.Navigation.findNavController
import com.alibaba.fastjson2.JSONObject
import com.miyuyan.sysuer.BaseFragment
import com.miyuyan.sysuer.R
import com.miyuyan.sysuer.api.CommonUtil
import com.miyuyan.sysuer.databinding.FragmentAssistantInfoFilterBinding
import com.miyuyan.sysuer.databinding.ItemFilterChipBinding
import com.miyuyan.sysuer.model.JwxtModel
import java.util.function.Consumer

class AssistantInfoFilterFragment : BaseFragment() {
	lateinit var model: JwxtModel
	override fun onDestroyView() {
		super.onDestroyView()
		model.dispose()
	}
	
	override fun onCreateView(inflater: LayoutInflater,
	                          container: ViewGroup?,
	                          savedInstanceState: Bundle?): View {
		super.onCreateView(inflater, container, savedInstanceState)
		val term: MutableLiveData<String?> = MutableLiveData<String?>()
		val campus: MutableLiveData<String?> = MutableLiveData<String?>()
		val binding = FragmentAssistantInfoFilterBinding.inflate(inflater, container, false).apply {
			this.term.itemTitle.setText(R.string.term)
			this.term.itemIcon.setImageResource(R.drawable.calendar)
			filter.setOnClickListener {
				findNavController(getRoot()).navigate(R.id.filter_to_result, Bundle().apply {
					putString("term", term.getValue())
					putString("campus", campus.getValue())
					putString("courseNumber", courseNumber.getText().toString())
					putString("courseName", courseName.getText().toString())
					putString("teacherName", teacher.getText().toString())
				})
			}
		}
		val pop = PopupMenu(requireContext(), binding.term.root)
		binding.term.root.setOnClickListener { pop.show() }
		model = JwxtModel(requireContext())
		term.observe(requireActivity(), Observer { acadYearSemester: String? ->
			acadYearSemester?.let {
				binding.term.itemContent.text = it
			}
		})
		model.message.observe(requireActivity(), Observer { message: CommonUtil.Tuple2<Int, JSONObject> ->
			val response = message.second
			if (response.getInteger("code") == 200) {
				when (message.first) {
					0 -> {
						response.getJSONArray("data").forEach(Consumer { t: Any? ->
							pop.menu.add((t as JSONObject).getString("acadYearSemester"))
								.setOnMenuItemClickListener {
									term.value = t.getString("acadYearSemester")
									false
								}
						})
						this.campuses
					}
					1 -> response.getJSONArray("data").forEach(Consumer { c: Any? ->
						val item = ItemFilterChipBinding.inflate(inflater, binding.campus, false)
						item.getRoot().text = (c as JSONObject).getString("campusName")
						item.getRoot()
							.setOnCheckedChangeListener { _: CompoundButton?, isChecked: Boolean ->
								if (isChecked) campus.value = c.getString("id")
							}
						binding.campus.addView(item.getRoot())
					})
				}
				model.nextAll()
			}
		})
		terms
		model.next()
		return binding.getRoot()
	}
	
	val terms: Unit
		get() {
			model.add("jwxt/base-info/acadyearterm/findAcadyeartermNamesBox", 0)
		}
	val campuses: Unit
		get() {
			model.add("jwxt/base-info/campus/findCampusNamesBox", 1)
		}
}