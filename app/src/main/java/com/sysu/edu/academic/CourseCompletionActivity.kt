package com.sysu.edu.academic

import android.os.Bundle
import android.view.MenuItem
import android.widget.LinearLayout
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.alibaba.fastjson2.JSONObject
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.sysu.edu.BaseActivity
import com.sysu.edu.R
import com.sysu.edu.api.CommonUtil
import com.sysu.edu.api.CommonUtil.extractValue
import com.sysu.edu.databinding.ActivityPagerBinding
import com.sysu.edu.databinding.ItemCardBinding
import com.sysu.edu.model.JwxtModel
import com.sysu.edu.view.AdapterListener
import com.sysu.edu.view.Pager2Adapter
import com.sysu.edu.view.StaggeredFragment
import com.sysu.edu.view.StaggeredFragment.StaggeredAdapter

class CourseCompletionActivity : BaseActivity() {
	lateinit var model: JwxtModel
	override fun onDestroy() {
		super.onDestroy()
		model.dispose()
	}
	
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		val creditFragment = StaggeredFragment.newInstance(0)
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
					(pager2Adapter.get(currentItem) as StaggeredFragment).export(toolbar, tabLayout.getTabAt(currentItem)?.text.toString())
					true
				}
			TabLayoutMediator(tabLayout, pager) { tab: TabLayout.Tab?, position: Int -> tab!!.text = mutableListOf<String?>("学分学时情况", "课程完成情况")[position] }.attach()
		}.root)
		creditHours
		model.message.observe(this, Observer { message: CommonUtil.Tuple2<Int, JSONObject> ->
			val response = message.second
			if (response.getInteger("code") == 200 && response.get("data") != null) {
				if (message.first == 0) response.getJSONArray("data").forEach { a: Any? ->
					creditFragment.setListener(object : AdapterListener {
						override fun onBind(adapter: RecyclerView.Adapter<RecyclerView.ViewHolder?>,
						                    holder: RecyclerView.ViewHolder,
						                    position: Int) {
							val item = (adapter as StaggeredAdapter).getValues(position)
							holder.itemView.findViewById<LinearProgressIndicator>(R.id.progress)
								.apply {
									max = item[3]!!.toFloat().toInt()
									progress = item[4]!!.toFloat().toInt()
								}
						}
						
						override fun onCreate(adapter: RecyclerView.Adapter<RecyclerView.ViewHolder?>?,
						                      binding: ViewBinding) {
							(binding as ItemCardBinding).getRoot()
								.addView(LinearProgressIndicator(this@CourseCompletionActivity).apply {
									id = R.id.progress
									layoutParams = LinearLayout.LayoutParams(-1, -2).apply {
										setMargins(config.dpToPx(12), config.dpToPx(6), config.dpToPx(12), config.dpToPx(12))
									}
								})
						}
					})
					creditFragment.add((a as JSONObject).getString("courseCategoryName"), mutableListOf<String?>("课程类别", "培养方案学分要求", "免修课程学分", "实际毕业学分要求", "实得"), extractValue(a, arrayOf("courseCategoryName", "trainingCredit", "exemptCredit", "actualCredit", "earnedCredit")))
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