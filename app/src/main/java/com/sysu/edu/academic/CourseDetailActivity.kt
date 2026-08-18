package com.sysu.edu.academic

import android.os.Bundle
import android.os.Environment
import android.view.MenuItem
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.sysu.edu.BaseActivity
import com.sysu.edu.R
import com.sysu.edu.api.DownloadManager
import com.sysu.edu.databinding.ActivityCourseDetailBinding
import com.sysu.edu.model.JwxtModel
import com.sysu.edu.view.Pager2Adapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CourseDetailActivity : BaseActivity() {
	lateinit var model: JwxtModel
	var classNum: String? = null // code: EIT228
	var courseId: String? = null // id: 1434807773283471360
	var courseInfoId: String? = null
	var outlineId: String? = null
	var courseName: String? = null
	private var outlineLoaded = false
	private var outline2Loaded = false
	override fun onDestroy() {
		super.onDestroy()
		model.dispose()
	}
	
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		model = JwxtModel(this)
		classNum = intent.getStringExtra("code")
		courseId = intent.getStringExtra("id")
		val courseDetailPageAdapter = Pager2Adapter(this).add(CourseDetailFragment()).add(CourseOutlineFragment())
		setContentView(ActivityCourseDetailBinding.inflate(layoutInflater).apply {
			toolbar.menu.add(getString(R.string.download)).setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_IF_ROOM).setOnMenuItemClickListener {
				outlineId?.let {
					downloadOutline(it)
				} ?: getOutlineId()
				true
			}
			toolbar.setNavigationOnClickListener { supportFinishAfterTransition() }
			pager.adapter = courseDetailPageAdapter
			TabLayoutMediator(tabs, pager) { tab: TabLayout.Tab, i: Int -> tab.text = getString(intArrayOf(R.string.course_detail, R.string.course_draft)[i]) }.attach()
		}.root)
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
						courseId = data.getJSONObject("outlineInfo").getString("courseId")
						courseInfoId = data.getJSONObject("outlineInfo").getString("outlineCourseInfoId")
						courseName = data.getJSONObject("outlineInfo").getString("courseName")
						outlineLoaded = true
						if (!outline2Loaded) courseOutline2
					}
					2 -> {
						courseDetailPageAdapter.get(0).setArguments(Bundle().apply {
							putInt("what", 2)
							putString("data", "$data")
						})
						classNum = data.getString("courseNumber")
						outline2Loaded = true
						if (!outlineLoaded) courseOutline
					}
					3 -> {
						val rows = data.getJSONArray("rows")
						if (rows.isNotEmpty()) rows.getJSONObject(0).getString("id").takeIf { it.isNotEmpty() }?.also {
							outlineId = it
							downloadOutline(it)
							return@observe
						}
						model.contextUtil.toast(getString(R.string.no_outline_found))
					}
				}
				model.nextAll()
			}
		}
		if (classNum != null) courseOutline
		else courseOutline2
		model.next()
	}
	
	private fun downloadOutline(outlineId: String) {
		DownloadManager.downloadFile(this@CourseDetailActivity,
		                             model.http.generateRequest("https://jwxt.sysu.edu.cn/jwxt/training-programe/courseoutline/outlineupdateworddownload?outlineUpdateId=$outlineId", null, null)
			                             .header("Cookie", model.cookie)
			                             .header("Referer", "https://jwxt.sysu.edu.cn/")
			                             .build(),
		                             "${
			                             Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
		                             }/$courseName.zip",
		                             true,
		                             object : DownloadManager.DownloadListener {
			                             override fun onDownloadProgress(progress: Long, total: Long) {
				                             println("$progress/$total")
			                             }
			                             
			                             override fun onDownloadComplete(path: String?) {
				                             CoroutineScope(Dispatchers.Main).launch {
					                             model.contextUtil.toast("${getString(R.string.download_complete)}: $path")
				                             }
			                             }
			                             
			                             override fun onDownloadError(code: Int, message: String?) {
				                             CoroutineScope(Dispatchers.Main).launch {
					                             model.contextUtil.toast(message ?: getString(R.string.download_error))
				                             }
			                             }
		                             })
	}
	
	val courseOutline: Unit
		get() {
			if (outlineLoaded) return
			outlineLoaded = true
			model.add("jwxt/training-programe/courseoutline/getalloutlineinfo?courseNum=$classNum&auditStatus=99", 1)
		}
	val courseOutline2: Unit
		get() {
			if (outline2Loaded) return
			outline2Loaded = true
			model.add("jwxt/base-info/courseLibrary/findById?id=$courseId", 2)
		}
	
	fun getOutlineId() {
		model.addAndNext("jwxt/training-programe/courseoutline/showOutlineUpdataCourse", "{\"pageNo\":1,\"pageSize\":10,\"total\":true,\"param\":{\"outlineCourseInfoId\":\"$courseInfoId\"}}", 3)
	}
}