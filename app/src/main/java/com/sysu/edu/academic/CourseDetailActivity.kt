package com.sysu.edu.academic

import android.os.Bundle
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.sysu.edu.BaseActivity
import com.sysu.edu.R
import com.sysu.edu.databinding.ActivityCourseDetailBinding
import com.sysu.edu.model.JwxtModel
import com.sysu.edu.view.Pager2Adapter

class CourseDetailActivity : BaseActivity() {
	lateinit var model: JwxtModel
	var code: String? = null
	var id: String? = null
//	var classNum: String? = null
	override fun onDestroy() {
		super.onDestroy()
		model.dispose()
	}
	
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		model = JwxtModel(this)
		val courseDetailPageAdapter = Pager2Adapter(this).add(CourseDetailFragment())
			.add(CourseOutlineFragment())
		setContentView(ActivityCourseDetailBinding.inflate(layoutInflater).apply {
			toolbar.setNavigationOnClickListener { supportFinishAfterTransition() }
			pager.adapter = courseDetailPageAdapter
			TabLayoutMediator(tabs, pager) { tab: TabLayout.Tab, i: Int -> tab.text = getString(intArrayOf(R.string.course_detail, R.string.course_draft)[i]) }.attach()
		}.root)
		code = intent.getStringExtra("code")
		id = intent.getStringExtra("id")
//		classNum = intent.getStringExtra("class") // code: EIT228, id: null, classNum: 202511441
//		println(code)
//		println(id)
//		println(classNum)
        model.message.observe(this) { (code, response) ->
			if (response.getInteger("code") == 200) {
				val data = response.getJSONObject("data")
				if (data != null) when (code) {
					1 -> {
						courseDetailPageAdapter.get(0).setArguments(Bundle().apply {
							putInt("what", 1)
							putString("data", data.getJSONObject("outlineInfo").toJSONString())
						})
						courseDetailPageAdapter.get(1).setArguments(Bundle().apply {
							putString("data", data.getJSONArray("scheduleList").toJSONString())
						})
						id = data.getJSONObject("outlineInfo").getString("courseId")
						courseOutline2
					}
					2 -> courseDetailPageAdapter.get(0).setArguments(Bundle().apply {
						putInt("what", 2)
						putString("data", "$data")
					})
				}
				model.nextAll()
			}
//			else if (response.getInteger("code") == 52000000) ActivityCourseDetailBinding.inflate(layoutInflater).apply {
//					toolbar.setNavigationOnClickListener { supportFinishAfterTransition() }
//					pager.adapter = courseDetailPageAdapter
//					TabLayoutMediator(tabs, pager) { tab: TabLayout.Tab?, i: Int -> tab!!.text = this@CourseDetailActivity.getString(intArrayOf(R.string.course_detail, R.string.course_draft)[i]) }.attach()
//				}.pager.visibility = View.GONE
		}
		if (code == null) courseOutline2
		else courseOutline
		model.next()
	}
	
	val courseOutline: Unit
		get() {
			model.add("jwxt/training-programe/courseoutline/getalloutlineinfo?courseNum=$code&auditStatus=99", 1)
		}
	val courseOutline2: Unit
		get() {
			model.add("jwxt/base-info/courseLibrary/findById?id=$id", 2)
		}
}