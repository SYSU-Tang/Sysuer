package com.miyuyan.sysuer.studentAffair

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.alibaba.fastjson2.JSONObject
import com.miyuyan.sysuer.R
import com.miyuyan.sysuer.api.CommonUtil.extractValue
import com.miyuyan.sysuer.model.XgxtModel
import com.miyuyan.sysuer.view.EditTextDialog
import com.miyuyan.sysuer.view.StaggerFragment
import kotlinx.coroutines.launch

class RecruitmentInfoFragment : StaggerFragment() {
	val viewModel: StudentPartTimeViewModel by viewModels()
	var total: Int = -1
	var page: Int = 1
	lateinit var model: XgxtModel
	override fun onDestroyView() {
		super.onDestroyView()
		model.dispose()
	}

	override fun onCreateView(
		inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
	): View? {
		val view = super.onCreateView(inflater, container, savedInstanceState)
		model = XgxtModel(requireContext())
		viewModel.jobNameDialog?.setValueChangeListener(object :
			EditTextDialog.OnValueChangeListener {
			override fun onValueChange(value: String?) {
				viewModel.jobName.value = value
				regetRecruitment()
			}
		})
		viewModel.unitDialog?.setValueChangeListener(object : EditTextDialog.OnValueChangeListener {
			override fun onValueChange(value: String?) {
				viewModel.unitName.value = value
				regetRecruitment()
			}
		})
		setScrollBottom {
			if ((page - 1) * 10 < total) recruitment()
		}
		viewLifecycleOwner.lifecycleScope.launch {
			viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
				model.messageChannel.collect { (code, data) ->
					if (data.containsKey("code") && data.getInteger("code") == 200) {
						when (code) {
							0 -> {
								total = data.getJSONObject("data").getInteger("total")
								data.getJSONObject("data").getJSONArray("list").forEach { i: Any? ->
									addSection(
											(i as JSONObject).getString("qgzxgwmc"), mutableListOf(
											"岗位名称",
											"岗位类型",
											"所在校区",
											"岗位地址",
											"开始时间",
											"结束时间",
											"状态",
											"设岗单位"
									), extractValue(
											i, arrayOf(
											"qgzxgwmc",
											"qgzxgwlxmc",
											"qgzxszxymc",
											"qgzxdwdz",
											"qgzxgwzpkssj",
											"qgzxgwzpjssj",
											"state",
											"sgdwmc"
									)
									)
									)
								}
							}

							1, 2, 3 -> {
								val menu = listOf(
										viewModel.yearPop, viewModel.campusPop, viewModel.typePop
								)[code - 1]?.menu ?: return@collect
								if (menu.hasVisibleItems()) return@collect
								val name = listOf(
										viewModel.yearName,
										viewModel.campusName,
										viewModel.jobTypeName
								)[code - 1]
								val liveData = listOf(
										viewModel.year, viewModel.campus, viewModel.jobType
								)[code - 1]
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
					}
				}
			}
		}
		year()
		campus()
		jobType()
		recruitment()
		return view
	}

	private fun regetRecruitment() {
		reset()
		recruitment()
	}

	private fun reset() {
		page = 1
		total = -1
		clear()
	}

	private fun recruitment() {
		val url = StringBuilder("qgzx/api/sm-qgzx/gwsq?pageSize=10&pageNum=${page++}")
		mapOf(
				viewModel.year to "qgzxnd",
				viewModel.jobType to "gwlxids",
				viewModel.campus to "xqids",
				viewModel.jobName to "qgzxgwmc",
				viewModel.unitName to "sgdwmc"
		).forEach { (k, v) ->
			k.value?.takeUnless { it.isEmpty() }?.let {
				url.append("&$v=$it")
			}
		}
		model.addAndNext("$url", 0)
	}

	private fun year() {
		model.addAndNext("qgzx/api/sm-qgzx/gwsq/ndlist/get", 1)
	}

	private fun campus() {
		model.addAndNext("qgzx/api/sm-qgzx/gwsq/xylist/get", 2)
	}

	private fun jobType() {
		model.addAndNext("qgzx/api/sm-qgzx/gwsq/gwlxlist/get", 3)
	}
}
