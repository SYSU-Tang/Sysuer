package com.sysu.edu.academic

import android.os.Bundle
import androidx.lifecycle.Observer
import com.alibaba.fastjson2.JSONObject
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.sysu.edu.BaseActivity
import com.sysu.edu.R
import com.sysu.edu.api.CommonUtil
import com.sysu.edu.api.CommonUtil.extractValue
import com.sysu.edu.databinding.ActivityPagerBinding
import com.sysu.edu.model.JwxtModel
import com.sysu.edu.view.Pager2Adapter
import com.sysu.edu.view.StaggerFragment
import java.util.Locale

class RegistrationActivity : BaseActivity() {
	lateinit var model: JwxtModel
	var page: Int = 0
	override fun onDestroy() {
		super.onDestroy()
		model.dispose()
	}
	
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		model = JwxtModel(this)
		val adp = Pager2Adapter(this)
		val binding = ActivityPagerBinding.inflate(layoutInflater).apply {
			pager.adapter = adp
			TabLayoutMediator(tabLayout, pager) { tab: TabLayout.Tab?, position: Int -> tab?.text = resources.getStringArray(R.array.registration_info)[position] }.attach()
			toolbar.setNavigationOnClickListener { supportFinishAfterTransition() }
			toolbar.setTitle(R.string.register_info)
		}
		setContentView(binding.root)
		listOf("2024", "2025", "2026", "2027").forEach { i: String? ->
			binding.toolbar.getMenu().add(i).setOnMenuItemClickListener {
				(adp.get(1) as StaggerFragment).clear()
				getPay(i)
				model.nextAll()
				false
			}
		}
		(0..<3).forEach { i: Int ->
			adp.add(StaggerFragment.newInstance(i))
			getNextPage(i)
		}
		model.message.observe(this, Observer { msg: CommonUtil.Tuple2<Int, JSONObject> ->
			val response = msg.second
			if (response.getInteger("code") == 200 && response.get("data") != null) {
				when (msg.first) {
					0 -> (adp.get(0) as StaggerFragment).addSection("学生报到信息", R.drawable.calendar, mutableListOf("学号", "注册学年学期", "报到状态", "注册状态", "缴费状态"), extractValue(response.getJSONObject("data"), arrayOf("stuNum", "academicYearTerm", "checkInStatusName", "registerStatusName", "payedStatusName")))
					1 -> response.getJSONArray("data").forEach { v: Any? ->
						(adp.get(1) as StaggerFragment).apply {
							setHideNull(true)
							addSection((v as JSONObject).getString("acadYear"), R.drawable.money, mutableListOf("年份", "类别", "项目名称", "金额（元）", "区间", "时间"), extractValue(v, arrayOf("acadYear", "typeName", "feeTypeName", "payedItemAmount", "feeTimeSection", "editeTime")))
						}
					}
					2 -> {
						val data = response.getJSONObject("data")
						val total = data.getInteger("total")
						data.getJSONArray("rows").forEach { a: Any? ->
							(adp.get(2) as StaggerFragment).addSection((a as JSONObject).getString("academicYearTerm"), R.drawable.calendar, mutableListOf("学年学期", "校区", "学院", "年级专业", "缴费状态", "报到状态", "注册状态", "报到日期", "注册日期"), extractValue(a, arrayOf("academicYearTerm", "campusName", "collegeName", "gradeMajorName", "payedStatusName", "checkInStatusName", "registerStatusName", "checkInDate", "registerDate")))
						}
						if (total / 10 > page - 1) list
					}
				}
				model.nextAll()
			}
		})
		model.next()
	}
	
	fun getNextPage(what: Int) {
		when (what) {
			0 -> info
			1 -> pay
			2 -> list
			else -> {}
		}
	}
	
	val info: Unit
		get() {
			model.add("jwxt/reports-register/stuRegistration/getSelfRegisterInfo", 0)
		}
	val pay: Unit
		get() {
			getPay("2025")
		}
	
	fun getPay(year: String?) {
		model.add("jwxt/reports-register/stuRegistration/getSelfPayInfoDetail?acadYear=$year", 1)
	}
	
	val list: Unit
		get() {
			model.add("jwxt/reports-register/stuRegistration/getSelfRegisterList", String.format(Locale.getDefault(), "{\"pageNo\":%d,\"pageSize\":10,\"total\":true,\"param\":{}}", ++page), 2)
		}
}