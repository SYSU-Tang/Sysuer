package com.miyuyan.sysuer.academic

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.AndroidViewModel
import com.alibaba.fastjson2.JSONObject
import com.miyuyan.sysuer.R
import com.miyuyan.sysuer.api.CommonUtil.extractValue
import com.miyuyan.sysuer.model.JwxtModel
import com.miyuyan.sysuer.view.SectionData

class SchoolWorkWarningViewModel(application: Application) : AndroidViewModel(application) {
	private val model = JwxtModel(application)
	val sections: SnapshotStateList<SectionData> = mutableStateListOf()
	var alarmOperationTerm: String? = null
	var alarmTerm: String? = null
	private var page by mutableIntStateOf(0)
	private var total by mutableIntStateOf(-1)
	val hasMore: Boolean get() = total > page * 10
	
	init {
		model.message.observeForever { (_, response) ->
			if (response.getInteger("code") == 200) response.getJSONObject("data")?.let {
				if (total == -1) total = it.getInteger("total")
				var order = sections.size
				it.getJSONArray("rows").forEach { a: Any? ->
					sections.add(SectionData("${++order}",
					                         R.drawable.warning,
					                         extractValue(a as JSONObject,
					                                      arrayOf("预警结果", "预警操作学期", "预警学期", "生成预警档案时间", "档案ID", "警告程度"),
					                                      arrayOf("alarmResultName", "alarmOperationTerm", "alarmTerm", "createTime", "archivceID", "alarmResult"))))
				}
			}
		}
	}
	
	fun fetchWarning() {
		val param = buildString {
			append("{\"pageNo\":${++page},\"pageSize\":10,\"total\":true,\"param\":{\"publicationStatus\":\"1\"")
			alarmOperationTerm?.let { append(",\"alarmOperationTerm\":\"$it\"") }
			alarmTerm?.let { append(",\"alarmTerm\":\"$it\"") }
			append("}}")
		}
		model.addAndNext("jwxt/alarm/alarm-archives/student/archives", param, 0)
	}
	
	fun refresh() {
		page = 0
		total = -1
		sections.clear()
		fetchWarning()
	}
	
	override fun onCleared() {
		model.dispose()
	}
}