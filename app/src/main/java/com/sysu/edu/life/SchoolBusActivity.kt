package com.sysu.edu.life

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import androidx.lifecycle.MutableLiveData
import com.alibaba.fastjson2.JSONObject
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.sysu.edu.BaseActivity
import com.sysu.edu.R
import com.sysu.edu.api.CommonUtil
import com.sysu.edu.api.CommonUtil.extractValue
import com.sysu.edu.databinding.ActivityPagerBinding
import com.sysu.edu.databinding.ItemSchoolBusNoticeBinding
import com.sysu.edu.model.PortalModel
import com.sysu.edu.view.Pager2Adapter
import com.sysu.edu.view.StaggerFragment
import java.util.stream.IntStream

class SchoolBusActivity : BaseActivity() {
	val day: MutableLiveData<Boolean?> = MutableLiveData<Boolean?>(true)
	lateinit var model: PortalModel
	override fun onDestroy() {
		super.onDestroy()
		model.dispose()
	}
	
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		val pager2Adapter = Pager2Adapter(this)
		var data: JSONObject? = null
		model = PortalModel(this)
		val routes = mutableListOf<String?>()
		val binding = ActivityPagerBinding.inflate(layoutInflater).apply {
			toolbar.setNavigationOnClickListener { supportFinishAfterTransition() }
			toolbar.setTitle(R.string.school_bus)
			pager.adapter = pager2Adapter
			toolbar.menu.add(R.string.export).setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_IF_ROOM).setIcon(R.drawable.export).setOnMenuItemClickListener {
					if (pager2Adapter.itemCount > 0) {
						val currentItem = pager.currentItem
						val fragment = (pager2Adapter.get(currentItem) as StaggerFragment)
						fragment.export(toolbar, tabLayout.getTabAt(currentItem)?.text.toString())
					}
					true
				}
			TabLayoutMediator(tabLayout, pager) { tab: TabLayout.Tab?, position: Int -> tab?.text = routes[position] }.attach()
		}
		setContentView(binding.root)
		val notice = MaterialAlertDialogBuilder(this).setTitle(R.string.notice).setPositiveButton(R.string.confirm, null).create()
		val header = ItemSchoolBusNoticeBinding.inflate(layoutInflater, binding.appBarLayout, false).apply {
				date.addOnButtonCheckedListener { _: MaterialButtonToggleGroup?, i: Int, b: Boolean ->
					if (i == R.id.workday) day.value = b
				}
				this@apply.notice.setOnClickListener { notice.show() }
				option.setOnItemClickListener { _: AdapterView<*>?, _: View?, position: Int, _: Long -> binding.pager.currentItem = position }
			}
		day.observe(this) { b: Boolean? ->
			val key = if (b == true) "workDay" else "holiday"
			data?.run {
				if (getJSONArray(key).isEmpty()) IntStream.range(0, pager2Adapter.itemCount).forEach { j -> (pager2Adapter.get(j) as StaggerFragment).clear() }
				else {
					var i = 0
					getJSONArray(key).forEach { item: Any? ->
						val fragment: StaggerFragment
						notice.setMessage((item as JSONObject).getString("note"))
						if (pager2Adapter.itemCount > i) {
							fragment = pager2Adapter.get(i) as StaggerFragment
							fragment.clear()
						}
						else {
							routes.add(item.getString("drivingDirectionName"))
							fragment = StaggerFragment()
							pager2Adapter.add(fragment)
						}
						i++
						fragment.addSection(getString(R.string.route_detail),
						                    R.drawable.bus,
						                    CommonUtil.getString(this@SchoolBusActivity, intArrayOf(R.string.route, R.string.start, R.string.end)),
						                    extractValue(item, arrayOf("drivingDirectionName", "startStation", "endStation")))
						item.getJSONArray("schoolBusShuttleMomentList").forEach {
							fragment.addSection((it as JSONObject).getString("time"),
							                    R.drawable.bus,
							                    CommonUtil.getString(this@SchoolBusActivity, intArrayOf(R.string.passenger, R.string.vehicles, R.string.time, R.string.route)),
							                    extractValue(it, arrayOf("passenger", "vehiclesType", "time", "drivingRoute")))
						}
					}
					header.option.setSimpleItems(routes.toTypedArray<String?>())
				}
			}
		}
		binding.appBarLayout.addView(header.root)
		model.message.observe(this) { (code, response) ->
			if (response.getJSONObject("meta").getInteger("statusCode") == 200) {
				if (code == 0) {
					data = response.getJSONObject("data")
					day.value = true
				}
			}
		}
		getData()
	}
	
	fun getData() {
		model.addAndNext("newClient/api/extraCard/schoolBusShuttleInfo/selectSchoolBusMap", 0)
	}
}