package com.sysu.edu.studentAffair

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.alibaba.fastjson2.JSONObject
import com.sysu.edu.R
import com.sysu.edu.api.CommonUtil
import com.sysu.edu.api.CommonUtil.extractValue
import com.sysu.edu.model.XgxtModel
import com.sysu.edu.view.StaggeredFragment

class RecruitmentInfoFragment : StaggeredFragment() {
	lateinit var viewModel: StudentPartTimeViewModel
	var total: Int = -1
	var page: Int = 1
	lateinit var model: XgxtModel
	override fun onDestroyView() {
		super.onDestroyView()
		model.dispose()
	}
	
	override fun onCreateView(inflater: LayoutInflater,
	                          container: ViewGroup?,
	                          savedInstanceState: Bundle?): View? {
		val view = super.onCreateView(inflater, container, savedInstanceState)
		model = XgxtModel(requireContext())
		viewModel = ViewModelProvider(requireActivity())[StudentPartTimeViewModel::class.java]
		viewModel.jobNameDialog?.setValueChangeListener { v: String? ->
			viewModel.jobName.value = v
			regetRecruitment()
		}
		viewModel.unitDialog?.setValueChangeListener { v: String? ->
			viewModel.unitName.value = v
			regetRecruitment()
		}
		setScrollBottom {
			if ((page - 1) * 10 < total) recruitment
		}
		model.message.observe(requireActivity(), Observer { message: CommonUtil.Tuple2<Int, JSONObject> ->
			val data = message.second
			if (data.containsKey("code") && data.getInteger("code") == 200) {
				when (message.first) {
					0 -> {
						total = data.getJSONObject("data").getInteger("total")
						data.getJSONObject("data").getJSONArray("list").forEach { i: Any? ->
							add((i as JSONObject).getString("qgzxgwmc"), mutableListOf("岗位名称", "岗位类型", "所在校区", "岗位地址", "开始时间", "结束时间", "状态", "设岗单位"), extractValue(i, arrayOf("qgzxgwmc", "qgzxgwlxmc", "qgzxszxymc", "qgzxdwdz", "qgzxgwzpkssj", "qgzxgwzpjssj", "state", "sgdwmc")))
						}
					}
					1, 2, 3 -> {
						val menu = listOf(viewModel.yearPop, viewModel.campusPop, viewModel.typePop)[message.first - 1]?.menu!!
						if (menu.hasVisibleItems()) return@Observer
						val name = listOf(viewModel.yearName, viewModel.campusName, viewModel.jobTypeName)[message.first - 1]
						val liveData = listOf(viewModel.year, viewModel.campus, viewModel.jobType)[message.first - 1]
						menu.add(R.string.all).setOnMenuItemClickListener {
							liveData.value = ""
							name.value = ""
							regetRecruitment()
							true
						}
						data.getJSONArray("data").forEach { i: Any? ->
							menu.add((i as JSONObject).getString("label"))
								.setOnMenuItemClickListener {
									liveData.value = i.getString("value")
									name.value = i.getString("label")
									regetRecruitment()
									true
								}
						}
					}
				}
				model.nextAll()
			}
		})
		year
		campus
		jobType
		recruitment
		model.next()
		return view
	}
	
	private fun regetRecruitment() {
		reset()
		recruitment
	}
	
	private fun reset() {
		page = 1
		total = -1
		clear()
	}
	
	val recruitment: Unit
		get() {
			var url = "qgzx/api/sm-qgzx/gwsq?pageSize=10&pageNum=${page++}" //			val query = mutableMapOf<String, String>()
			mapOf(viewModel.year to "qgzxnd", viewModel.jobType to "gwlxids", viewModel.campus to "xqids", viewModel.jobName to "qgzxgwmc", viewModel.unitName to "sgdwmc").forEach { (k, v) ->
				k.value?.takeUnless { it.isEmpty() }?.let {
					url += "&$v=$it"
				}
			}
			model.addAndNext(url, 0)
		}
	val year: Unit
		get() {
			model.add("qgzx/api/sm-qgzx/gwsq/ndlist/get", 1)
		}
	val campus: Unit
		get() {
			model.add("qgzx/api/sm-qgzx/gwsq/xylist/get", 2)
		}
	val jobType: Unit
		get() {
			model.add("qgzx/api/sm-qgzx/gwsq/gwlxlist/get", 3)
		}
}
