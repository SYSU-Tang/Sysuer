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
import com.sysu.edu.api.CommonUtil.toStringOrDefault
import com.sysu.edu.databinding.ActivityPagerBinding
import com.sysu.edu.model.XgxtModel
import com.sysu.edu.view.Pager2Adapter
import com.sysu.edu.view.StaggeredFragment

class PersonalInformationActivity : BaseActivity() {
	lateinit var model: XgxtModel
	override fun onDestroy() {
		super.onDestroy()
		model.dispose()
	}
	
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		model = XgxtModel(this)
		val tabs = mutableListOf<String?>()
		val pager2Adapter = Pager2Adapter(this)
		val binding = ActivityPagerBinding.inflate(layoutInflater).apply {
			toolbar.setTitle(R.string.personal_info)
			toolbar.setNavigationOnClickListener { supportFinishAfterTransition() }
			toolbar.menu.add(R.string.export)
				.setIcon(R.drawable.export)
				.setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_IF_ROOM)
				.setOnMenuItemClickListener {
					if (pager2Adapter.itemCount > 0) {
						val currentItem = pager.currentItem
						(pager2Adapter.get(currentItem) as StaggeredFragment).export(toolbar, tabs[currentItem])
					}
					true
				}
			pager.adapter = pager2Adapter
			TabLayoutMediator(tabLayout, pager) { tab: TabLayout.Tab?, position: Int -> tab?.text = tabs[position] }.attach()
		}
		setContentView(binding.root)
		model.message.observe(this, Observer { message: CommonUtil.Tuple2<Int, JSONObject> ->
			val data = message.second
			if (data.containsKey("code") && data.getInteger("code") == 200) {
				val dict = HashMap<String?, String?>()
				dict["bmmc"] = "部门"
				dict["id"] = "ID"
				dict["jgmc"] = "籍贯"
				dict["hjszdText"] = "高中所在地"
				dict["zjxymc"] = "宗教信仰"
				dict["sfzszdmc"] = "身份证所在地"
				dict["jkzkmc"] = "健康状况"
				dict["csd"] = "出生地"
				dict["kslbmc"] = "考生类别"
				dict["hyzk"] = "婚姻状况"
				dict["cjrbjText"] = "残疾人标记"
				dict["xxmc"] = "学校"
				dict["hyzkmc"] = "婚姻状况描述"
				data.getJSONArray("data").forEach { item: Any? ->
					(item as JSONObject).getJSONArray("fields").forEach { field: Any? ->
						dict[(field as JSONObject).getString("zdmc")] = field.getString("zdzwm")
					}
					val list = StaggeredFragment()
					tabs.add(item.getString("zdflmc"))
					pager2Adapter.add(list)
					if (item.getJSONObject("data").isEmpty()) {
						var count = 1
						item.getJSONArray("dataList").forEach { j: Any? ->
							val keys = ArrayList<String?>()
							val values = ArrayList<String?>()
							(j as JSONObject).forEach { (k: String?, v: Any?) ->
								keys.add(dict.getOrDefault(k, k))
								if ("gx" == k || "gxrzzmm" == k || "qdxl" == k) values.add((v as JSONObject).getString("label"))
								else values.add(toStringOrDefault<Any?>(v))
							}
							list.add("${count++}", keys, values)
						}
					} else {
						val keys = ArrayList<String?>()
						val values = ArrayList<String?>()
						item.getJSONObject("data").forEach { (k: String?, v: Any?) ->
							keys.add(dict.getOrDefault(k, k))
							values.add(toStringOrDefault<Any?>(v))
						}
						list.add(item.getString("zdflmc"), keys, values)
					}
				}
			}
		})
		personalInfo
	}
	
	val personalInfo: Unit
		get() {
			model.addAndNext("xsxx/api/sm-xsxx/info/student/view", 0)
		}
}