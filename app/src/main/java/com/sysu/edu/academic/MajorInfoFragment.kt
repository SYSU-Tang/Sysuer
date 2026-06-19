package com.sysu.edu.academic

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.RecyclerView
import com.alibaba.fastjson2.JSONObject
import com.sysu.edu.api.CommonUtil
import com.sysu.edu.api.CommonUtil.extractValue
import com.sysu.edu.model.JwxtModel
import com.sysu.edu.view.StaggeredFragment
import java.util.Locale

class MajorInfoFragment : StaggeredFragment() {
	var page: Int = 0
	var total: Int = -1
	var code: String? = null
	lateinit var model: JwxtModel
	override fun onDestroyView() {
		super.onDestroyView()
		model.dispose()
	}
	
	override fun onCreateView(inflater: LayoutInflater,
	                          container: ViewGroup?,
	                          savedInstanceState: Bundle?): View? {
		val view = super.onCreateView(inflater, container, savedInstanceState)
		code = requireArguments().getString("code")
		model = JwxtModel(requireContext())
		binding.recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
			override fun onScrolled(v: RecyclerView, dx: Int, dy: Int) {
				if (!v.canScrollVertically(1) && total > page * 10) list
			}
		})
		model.message.observe(requireActivity(), Observer { message: CommonUtil.Tuple2<Int, JSONObject> ->
			val response = message.second
			if (response.getInteger("code") == 200 && response.get("data") != null) {
				val data = response.getJSONObject("data")
				if (message.first == 0) {
					if (total == -1) total = data.getInteger("total")
					data.getJSONArray("rows").forEach { a: Any? ->
						add((a as JSONObject).getString("name"), mutableListOf("专业代码", "专业名称", "学制", "修业年限", "学科门类", "学位授予门类"), extractValue(a, arrayOf("code", "name", "educationalSystem", "maxStudyYear", "disciplineCateName", "degreeGrantName")))
					}
				}
			}
		})
		list
		return view
	}
	
	val list: Unit
		get() {
			model.addAndNext("jwxt/base-info/profession-direction/list", String.format(Locale.getDefault(), "{\"pageNo\":%d,\"pageSize\":10,\"total\":true,\"param\":{\"majorProfessionDircetion\":\"0\",\"disciplineCateCode\":\"%s\"}}", ++page, code), 0)
		}
	
	companion object {
		fun newInstance(args: Bundle?): MajorInfoFragment {
			val fragment = MajorInfoFragment()
			fragment.setArguments(args)
			return fragment
		}
	}
}