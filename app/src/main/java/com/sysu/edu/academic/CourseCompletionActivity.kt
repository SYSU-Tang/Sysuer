package com.sysu.edu.academic

import android.os.Bundle
import android.view.MenuItem
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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

class CourseCompletionActivity : BaseActivity() {
	lateinit var model: JwxtModel
	override fun onDestroy() {
		super.onDestroy()
		model.dispose()
	}
	
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		val creditFragment = StaggerFragment.newInstance(0)
		model = JwxtModel(this)
		val pager2Adapter = Pager2Adapter(this).add(creditFragment).add(CourseCompletionFragment())
		setContentView(ActivityPagerBinding.inflate(layoutInflater).apply {
			toolbar.setNavigationOnClickListener { supportFinishAfterTransition() }
			toolbar.setTitle(R.string.course_completion)
			pager.adapter = pager2Adapter
			toolbar.menu
				.add(R.string.export)
				.setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_IF_ROOM)
				.setIcon(R.drawable.export)
				.setOnMenuItemClickListener {
					val currentItem = pager.currentItem
					val fragment = (pager2Adapter.get(currentItem) as StaggerFragment)
					fragment.export(toolbar, toolbar.title.toString())
					true
				}
			TabLayoutMediator(tabLayout, pager) { tab: TabLayout.Tab?, position: Int -> tab!!.text = mutableListOf<String?>("学分学时情况", "课程完成情况")[position] }.attach()
		}.root)
		creditHours
		model.message.observe(this, Observer { message: CommonUtil.Tuple2<Int, JSONObject> ->
			val response = message.second
			if (response.getInteger("code") == 200 && response.get("data") != null) {
				if (message.first == 0) response.getJSONArray("data").forEach { a: Any? ->
					val item = a as JSONObject
					creditFragment.addSection(item.getString("courseCategoryName"), mutableListOf("课程类别", "培养方案学分要求", "免修课程学分", "实际毕业学分要求", "实得"), extractValue(item, arrayOf("courseCategoryName", "trainingCredit", "exemptCredit", "actualCredit", "earnedCredit")))
					val sectionIndex = creditFragment.sections.size - 1
					creditFragment.sectionAdapter.addFooter(sectionIndex) {
						val actualCredit = item.getFloatValue("actualCredit")
						val earnedCredit = item.getFloatValue("earnedCredit")
						if (actualCredit > 0) {
							androidx.compose.material3.LinearProgressIndicator(
								progress = { earnedCredit / actualCredit },
								modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)
							)
						}
					}
				}
			}
		})
		model.next()
	}
	
	val creditHours: Unit
		get() {
			model.add("jwxt/gradua-degree/graduatemsg/studentsGraduationExamination/creditHoursStu?cultureTypeCode=01", "", 0)
		}
}