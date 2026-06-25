package com.sysu.edu.life

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.alibaba.fastjson2.JSONObject
import com.google.android.material.textfield.TextInputLayout
import com.sysu.edu.BaseFragment
import com.sysu.edu.R
import com.sysu.edu.api.CommonUtil
import com.sysu.edu.api.CommonUtil.toStringOrDefault
import com.sysu.edu.databinding.FragmentComplaintResponseBinding
import com.sysu.edu.life.ComplaintSquareFragment.SquareAdapter
import com.sysu.edu.model.XinfangModel
import java.util.function.Consumer

class ComplaintResponseFragment : BaseFragment() {
	lateinit var model: XinfangModel
	override fun onCreateView(inflater: LayoutInflater,
	                          container: ViewGroup?,
	                          savedInstanceState: Bundle?): View {
		super.onCreateView(inflater, container, savedInstanceState)
		model = XinfangModel(requireContext())
		val binding = FragmentComplaintResponseBinding.inflate(inflater, container, false)
		val adapter = SquareAdapter()
		binding.recyclerView.adapter = adapter
		binding.phone.setEndIconOnClickListener { getResponse(binding.phone) }
		if (binding.phone.getEditText() != null) {
			binding.phone.getEditText()!!.addTextChangedListener(object : TextWatcher {
				override fun afterTextChanged(s: Editable?) {
				}
				
				override fun beforeTextChanged(s: CharSequence?,
				                               start: Int,
				                               count: Int,
				                               after: Int) {
				}
				
				override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
					if (ComplaintModel.isInvalidPhone(toStringOrDefault<CharSequence?>(s))) binding.phone.error = getString(R.string.invalid_phone)
					else binding.phone.error = null
				}
			})
		}
		binding.recyclerView.setLayoutManager(StaggeredGridLayoutManager(config.column, StaggeredGridLayoutManager.VERTICAL))
		model.message.observe(requireActivity(), Observer { message: CommonUtil.Tuple2<Int, JSONObject> ->
			val response = message.second
			when (message.first) {
				0 -> {
					if (response.getBoolean("ok")) response.getString("data")
					else config.toast(response.getString("msg"))
				}
				1 -> {
					if (response.getBoolean("ok")) response.getJSONArray("data")
						.forEach(Consumer { v: Any? -> adapter.add(v as JSONObject?) })
					else config.toast(response.getString("msg"))
				}
			}
		})
		return binding.getRoot()
	}
	
	fun getResponse(textInputLayout: TextInputLayout) {
		var phone: String? = null
		if (textInputLayout.getEditText() != null) phone = textInputLayout.getEditText()!!
			.getText()
			.toString()
		if (ComplaintModel.isInvalidPhone(phone)) textInputLayout.error = getString(R.string.invalid_phone)
		else getResponse(phone)
	}
	
	fun getCode(phone: String?) {
		model.addAndNext("jsp_api/code_send", "{\"m\":\"$phone\",\"t\":\"jsjb\"}", 0)
	}
	
	fun getResponse(phone: String?) {
		model.addAndNext("jsp_api/jsjb_list", "{\"mobile\":\"$phone\"}", 1)
	}
}