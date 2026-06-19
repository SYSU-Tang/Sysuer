package com.sysu.edu.academic

import android.os.Bundle
import android.view.MenuItem
import androidx.lifecycle.Observer
import com.alibaba.fastjson2.JSONObject
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.sysu.edu.BaseActivity
import com.sysu.edu.R
import com.sysu.edu.api.CommonUtil
import com.sysu.edu.api.CommonUtil.extractValue
import com.sysu.edu.api.CommonUtil.getString
import com.sysu.edu.databinding.ActivityPagerBinding
import com.sysu.edu.model.XgxtModel
import com.sysu.edu.view.Pager2Adapter
import com.sysu.edu.view.StaggeredFragment

class DormActivity : BaseActivity() {
	lateinit var model: XgxtModel
	override fun onDestroy() {
		super.onDestroy()
		model.dispose()
	}
	
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		val tabs = mutableListOf<String?>()
		val pager2Adapter = Pager2Adapter(this)
		model = XgxtModel(this)
		val binding = ActivityPagerBinding.inflate(layoutInflater).apply {
			toolbar.setTitle(R.string.dorm)
			toolbar.setNavigationOnClickListener { supportFinishAfterTransition() }
			pager.adapter = pager2Adapter
			toolbar.menu.add(R.string.export)
				.setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_IF_ROOM)
				.setIcon(R.drawable.export)
				.setOnMenuItemClickListener {
					pager.currentItem.takeIf {
						!pager2Adapter.isEmpty && it < pager2Adapter.itemCount
					}?.let {
						(pager2Adapter.get(it) as StaggeredFragment).export(toolbar, tabLayout.getTabAt(it)?.text.toString())
					}
					true
				}
			TabLayoutMediator(tabLayout, pager) { tab: TabLayout.Tab?, position: Int -> tab!!.text = tabs[position] }.attach()
		}
		setContentView(binding.getRoot())
		model.message.observe(this, Observer { message: CommonUtil.Tuple2<Int, JSONObject> ->
			var data = message.second
			if (data.containsKey("code") && data.getInteger("code") == 200) {
				data = data.getJSONObject("data")
				val list = StaggeredFragment()
				tabs.add(getString(R.string.personal_info))
				pager2Adapter.add(list)
				list.add(getString(R.string.personal_info), getString(this, intArrayOf(R.string.name, R.string.student_id, R.string.gender, R.string.school, R.string.major, R.string.grade, R.string.training_level, R.string.stay_school_status, R.string.student_status, R.string.contact_number)), extractValue(data, arrayOf("name", "studentNumber", "gender", "academy", "major", "grade", "trainingLevel", "staySchoolStatus", "studentStatus", "contactNumber")))
				val list1 = StaggeredFragment()
				tabs.add(getString(R.string.dorm_info))
				pager2Adapter.add(list1)
				data.getJSONArray("stayRecordList").forEach { e: Any? ->
					list1.add((e as JSONObject).getString("schoolYear"), getString(this, intArrayOf(R.string.year, R.string.campus, R.string.building, R.string.floor, R.string.room_number, R.string.bed_number, R.string.accommodation_fee, R.string.stay_start_date, R.string.stay_end_date)), extractValue(e, arrayOf("schoolYear", "campus", "buildingName", "floorName", "roomNumber", "bedNumber", "accommodationFee", "startDate", "endDate")))
				}
				val list2 = StaggeredFragment()
				tabs.add(getString(R.string.dorm_fee))
				pager2Adapter.add(list2)
				data.getJSONArray("stayChargeRecordList").forEach { e: Any? ->
					list2.add((e as JSONObject).getString("schoolYear"), getString(this, intArrayOf(R.string.year, R.string.accommodation_standard, R.string.should_pay_stay_charge, R.string.real_pay_stay_charge, R.string.arrears)), extractValue(e, arrayOf("schoolYear", "shouldPayStayCharge", "realPayStayCharge", "charge", "arrears")))
				}
			}
		})
		dormInfo
	}
	
	val dormInfo: Unit
		get() {
			model.addAndNext("ssgl/api/sm-ssgl/stu-info", 0)
		}
}