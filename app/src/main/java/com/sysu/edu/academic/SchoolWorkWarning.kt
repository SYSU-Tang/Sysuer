package com.sysu.edu.academic

import android.os.Bundle
import androidx.lifecycle.Observer
import com.alibaba.fastjson2.JSONObject
import com.sysu.edu.BaseActivity
import com.sysu.edu.R
import com.sysu.edu.api.CommonUtil
import com.sysu.edu.api.CommonUtil.extractValue
import com.sysu.edu.databinding.ActivityListBinding
import com.sysu.edu.model.JwxtModel
import com.sysu.edu.view.StaggerFragment
import java.util.Locale

class SchoolWorkWarning : BaseActivity() {
	var alarmOperationTerm: String? = null
	var alarmTerm: String? = null
	var page: Int = 0
	var total: Int = -1
	lateinit var model: JwxtModel
	override fun onDestroy() {
		super.onDestroy()
		model.dispose()
	}
	
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		model = JwxtModel(this)
		val binding = ActivityListBinding.inflate(layoutInflater).apply {
			toolbar.setTitle(R.string.school_work_warning)
			toolbar.setNavigationOnClickListener { supportFinishAfterTransition() }
		}
		setContentView(binding.root)
		val fragment = binding.list.getFragment<StaggerFragment>().apply {
			setScrollBottom {
				if (total > page * 10) warning
			}
			addExportMenu(binding.toolbar)
		}
		model.message.observe(this, Observer { message: CommonUtil.Tuple2<Int, JSONObject> ->
			val response = message.second
			if (response.getInteger("code") == 200) response.getJSONObject("data")?.let {
				if (total == -1) total = it.getInteger("total")
				var order = 0
				it.getJSONArray("rows").forEach { a: Any? ->
					fragment.addSection("${++order}", R.drawable.warning, mutableListOf("预警结果", "预警操作学期", "预警学期", "生成预警档案时间", "档案ID", "警告程度"), extractValue(a as JSONObject, arrayOf("alarmResultName", "alarmOperationTerm", "alarmTerm", "createTime", "archivceID", "alarmResult")))
				}
			}
		})
		warning
	}
	
	val warning: Unit
		get() {
			model.addAndNext("jwxt/alarm/alarm-archives/student/archives", String.format(Locale.getDefault(), "{\"pageNo\":%d,\"pageSize\":10,\"total\":true,\"param\":{\"publicationStatus\":\"1\"%s%s}}", ++page, getTerm(alarmTerm), getTerm(alarmOperationTerm)), 0)
		}
	
	fun getTerm(s: String?): String {
		return if (s.isNullOrEmpty()) "" else ",\"alarmOperationTerm\":\"$s\""
	}
}