package com.sysu.edu.academic

import android.app.Application
import android.content.Intent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.core.net.toUri
import androidx.lifecycle.AndroidViewModel
import com.alibaba.fastjson2.JSONObject
import com.sysu.edu.api.CommonUtil.extractValue
import com.sysu.edu.api.FileRequestBody
import com.sysu.edu.browser.BrowserActivity
import com.sysu.edu.model.JwxtModel
import com.sysu.edu.view.RowData
import com.sysu.edu.view.SectionData
import okhttp3.MultipartBody

class LeaveSlipViewModel(application: Application) : AndroidViewModel(application) {
	private val model = JwxtModel(application)
	val sections: SnapshotStateList<SectionData> = mutableStateListOf()
	val types: SnapshotStateList<JSONObject> = mutableStateListOf()
	val attachments: SnapshotStateList<JSONObject> = mutableStateListOf()
	val attachmentRows: SnapshotStateList<RowData> = mutableStateListOf()
	var selectedAttachmentIndex: Int by mutableIntStateOf(-1)
	private var page by mutableIntStateOf(0)
	private var total by mutableIntStateOf(-1)
	val hasMore: Boolean get() = total > page * 10
	
	init {
		model.message.observeForever { (code, response) ->
			if (response.getInteger("code") == 200) when (code) {
				0 -> {
					response.getJSONObject("data")?.let {
						if (total == -1) total = it.getInteger("total")
						it.getJSONArray("rows").forEach { item: Any? ->
							val title = "${(item as JSONObject).getString("askLeaveReasonName")} · ${item.getString("askLeaveTypeName")}"
							sections.add(SectionData(title,
							                         rows = extractValue(item,
							                                             arrayOf("请假原因",
							                                                     "请假类型",
							                                                     "开始时间",
							                                                     "结束时间",
							                                                     "开始日期",
							                                                     "结束日期",
							                                                     "请假天数",
							                                                     "实际请假天数",
							                                                     "学期累计请假",
							                                                     "审批状态",
							                                                     "审批阶段",
							                                                     "销假",
							                                                     "是否取消",
							                                                     "原因说明",
							                                                     "申请日期",
							                                                     "审批日期",
							                                                     "学期",
							                                                     "校区",
							                                                     "年级专业",
							                                                     "附件"),
							                                             arrayOf("askLeaveReasonName",
							                                                     "askLeaveTypeName",
							                                                     "askLeaveBeginTime",
							                                                     "askLeaveEndTime",
							                                                     "askLeaveBeginDate",
							                                                     "askLeaveEndDate",
							                                                     "askLeaveDaysCount",
							                                                     "trueLeaveDaysCount",
							                                                     "semesterAskLeaveDays",
							                                                     "approveStatusName",
							                                                     "approveStageName",
							                                                     "reportLeaveName",
							                                                     "canceledName",
							                                                     "askLeaveReasonExplanation",
							                                                     "askLeaveApplyDate",
							                                                     "approveDate",
							                                                     "semester",
							                                                     "campusName",
							                                                     "gradeMajorName",
							                                                     "fileName")).apply {
								                         last().onClick = {
									                         getApplication<Application>().startActivity(Intent(getApplication(), BrowserActivity::class.java).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
										                                                                     .setData("https://jwxt.sysu.edu.cn/jwxt/reports-register/askLeaveAgg/downloadFile?filePath=${item.getString("filePath")}&fileName=${
											                                                                     item.getString("fileName")
										                                                                     }".toUri()))/*reports-register/2026-04/12/2043290312362156032.jpg*/                            /*https://jwxt.sysu.edu.cn/jwxt/reports-register/askLeaveAgg/downloadFile?filePath=reports-register/2026-04/12/2043290312362156032.jpg&fileName=Screenshot_2026-04-12-19-27-43-978_com.tencent.mm.jpg*/
								                         }
							                         }))
						}
					}
				}
				1 -> {
					types.addAll(response.getJSONArray("data").filterIsInstance<JSONObject>())
				}
				2 -> {
					val data = response.getJSONObject("data")
					attachments.add(data)
					attachmentRows.add(RowData(data.getString("filePath"), data.getString("fileName")) {
						selectedAttachmentIndex = attachments.indexOf(data).takeUnless { selectedAttachmentIndex == it } ?: -1
					})                    /*{
    "code": 200,
    "data": {
        "fileName": "????????????logo.png",
        "filePath": "reports-register/2026-08/13/2087895235720155136.png"
    }
}*/
				}
			}
		}
	}
	
	fun fetchLeaveSlips() {
		model.addAndNext("jwxt/reports-register/askLeaveAgg/selfAskLeaveInfoList", "{\"param\":{},\"pageNo\":${++page},\"pageSize\":10,\"total\":true}", 0)
	}
	
	fun fetchLeaveTypes() {
		model.addAndNext("jwxt/base-info/codedata/findcodedataNames?datableNumber=436", 1)
	}
	
	fun submitLeaveSlip() {
		model.addAndNext("jwxt/reports-register/askLeaveAgg/askLeaveApply", "", 3)
	}
	
	fun uploadAttachment(fileRequestBody: FileRequestBody) {
		model.request(model.http.generateRequest("https://${model.host}/jwxt//reports-register/askLeaveAgg/importFile?", null, null)
			              .post(MultipartBody.Builder().setType(MultipartBody.FORM).addFormDataPart("file", fileRequestBody.fileName, fileRequestBody.file).build())
			              .build(), 2)
	}
	
	fun deleteAttachment() {
		if (selectedAttachmentIndex >= 0) {
			attachments.removeAt(selectedAttachmentIndex)
			attachmentRows.removeAt(selectedAttachmentIndex)
			selectedAttachmentIndex = -1
		}
	}
	
	override fun onCleared() {
		model.dispose()
	}
}