package com.miyuyan.sysuer.academic

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import androidx.appcompat.widget.PopupMenu
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.Navigation.findNavController
import com.alibaba.fastjson2.JSONObject
import com.miyuyan.sysuer.BaseFragment
import com.miyuyan.sysuer.R
import com.miyuyan.sysuer.databinding.FragmentAssistantInfoFilterBinding
import com.miyuyan.sysuer.databinding.ItemFilterChipBinding
import com.miyuyan.sysuer.model.JwxtModel
import kotlinx.coroutines.launch
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
term.observe(viewLifecycleOwner, Observer { acadYearSemester: String? ->
			acadYearSemester?.let {
				binding.term.itemContent.text = it
			}
		})
		viewLifecycleOwner.lifecycleScope.launch {
			viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
				model.messageChannel.collect { (code, response) ->
					if (response.getInteger("code") == 200) {
						when (code) {
							0 -> {
								response.getJSONArray("data").forEach(Consumer { t: Any? ->
									pop.menu.add((t as JSONObject).getString("acadYearSemester"))
										.setOnMenuItemClickListener {
											term.value = t.getString("acadYearSemester")
											false
										}
								})
								this@AssistantInfoFilterFragment.campuses()
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
				}
			}
		}
		terms()
		model.next()
		return binding.getRoot()
	}
	
	private fun terms() {
		model.add("jwxt/base-info/acadyearterm/findAcadyeartermNamesBox", 0)
	}
	private fun campuses() {
		model.add("jwxt/base-info/campus/findCampusNamesBox", 1)
	}
}