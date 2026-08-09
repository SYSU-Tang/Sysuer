package com.sysu.edu.studentAffair

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import com.alibaba.fastjson2.JSONObject
import com.sysu.edu.R
import com.sysu.edu.api.CommonUtil
import com.sysu.edu.api.CommonUtil.extractValue
import com.sysu.edu.model.XgxtModel
import com.sysu.edu.view.StaggerFragment

class CVFragment : StaggerFragment() {
	@JvmField var view: View? = null
	lateinit var model: XgxtModel
	override fun onDestroyView() {
		super.onDestroyView()
		model.dispose()
	}
	
	override fun onCreateView(inflater: LayoutInflater,
	                          container: ViewGroup?,
	                          savedInstanceState: Bundle?): View? {
		if (view == null) {
			view = super.onCreateView(inflater, container, savedInstanceState)
			model = XgxtModel(requireContext())
			model.message.observe(requireActivity(), Observer { message: CommonUtil.Tuple2<Int, JSONObject> ->
				var data = message.second
				if (data.getInteger("code") == 200) {
					data = data.getJSONObject("data")
					addSection(getString(R.string.cv), mutableListOf("学号", "姓名", "培养单位", "专业", "培养层次", "电话号码", "邮箱", "最后修改时间", "家庭人均月收入(元)", "在校每月平均消费(元)", "爱好特长", "勤工助学经历",  /*"", */"工作时间", "性别", "住宿地址"), extractValue(data, arrayOf("xh", "xm", "pydw", "zymc", "pycc", "dhhm", "email", "zhxgsj", "jtrjysr", "zxmypjxf", "ahtc", "qgzxjls",  /*"kqgzxsjs",*/"gzsjs", "xb", "ssdz")))
					data.getJSONArray("hjqks").forEach { i: Any? ->
						addSection(getString(R.string.award), mutableListOf("颁奖单位", "颁奖日期", "奖项"), extractValue(i as JSONObject, arrayOf("bjdw", "bjrq", "jxmc")))
					}
					data.getJSONArray("rzjls").forEach { i: Any? ->
						addSection(getString(R.string.experience), mutableListOf("工作单位", "工作开始年月", "工作结束年月", "工作职务", "证明人", "证明人单位"), extractValue(i as JSONObject, arrayOf("gzdw", "gzksny", "gzjsny", "gzzw", "zmr", "zmrdwhzw")))
					}
				}
			})
			cV
		}
		return view
	}
	
	val cV: Unit
		get() {
			model.addAndNext("qgzx/api/sm-qgzx/xsjl/get", 0)
		}
}
