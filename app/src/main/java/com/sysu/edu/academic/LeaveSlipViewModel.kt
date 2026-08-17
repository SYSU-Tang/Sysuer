package com.sysu.edu.academic

import android.app.Application
import android.content.Intent
import android.os.Environment
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Print
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.core.net.toUri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.application
import androidx.lifecycle.viewModelScope
import com.alibaba.fastjson2.JSONObject
import com.sysu.edu.R
import com.sysu.edu.api.CommonUtil.extractValue
import com.sysu.edu.api.DateTimeManager
import com.sysu.edu.api.DownloadManager
import com.sysu.edu.api.FileRequestBody
import com.sysu.edu.browser.BrowserActivity
import com.sysu.edu.model.JwxtModel
import com.sysu.edu.view.MenuItem
import com.sysu.edu.view.RowData
import com.sysu.edu.view.SectionData
import kotlinx.coroutines.launch
import okhttp3.MultipartBody

class LeaveSlipViewModel(application: Application) : AndroidViewModel(application) {
	private val model = JwxtModel(application)
	val sections: SnapshotStateList<SectionData> = mutableStateListOf()
	val leaveReasons: SnapshotStateList<JSONObject> = mutableStateListOf()
	val attachment: MutableLiveData<JSONObject?> = MutableLiveData()
	val attachmentRows: SnapshotStateList<RowData> = mutableStateListOf()
	private var page by mutableIntStateOf(0)
	private var total by mutableIntStateOf(-1)
	val hasMore: Boolean get() = total > page * 10
	private var resetVersion by mutableIntStateOf(0)
	val resetTrigger: Int get() = resetVersion
	private val _submitSuccess = MutableLiveData<Boolean>()
	val submitSuccess: LiveData<Boolean> = _submitSuccess
	val leaveData: JSONObject = JSONObject.of("whetherStuApply", "1")
	
	/*{"semester":"2025-2","askLeaveDaysCount":0.5,"askLeaveTypeCode":"1","askLeaveTypeName":"短假","askLeaveBeginDate":"2026-08-16 08:00:00","askLeaveEndDate":"2026-08-16 12:00:00","askLeaveReasonCode":"1","askLeaveReasonExplanation":".","fileName":"中山大学logo.png","filePath":"reports-register/2026-08/16/2088838751325556736.png","whetherStuApply":"1"}*/
	var leaveDays: String by mutableStateOf("")
	var leaveType: String by mutableStateOf("")
	var leaveTypeName: String by mutableStateOf("")
	var leaveReasonDescription: String by mutableStateOf("")
	var leaveReason: String? by mutableStateOf(null)
	var leaveReasonName: String? by mutableStateOf(null)
	var startPeriod: Int by mutableIntStateOf(0)
	var endPeriod: Int by mutableIntStateOf(0)
	var startMillis: Long by mutableLongStateOf(System.currentTimeMillis())
	var endMillis: Long by mutableLongStateOf(System.currentTimeMillis())
	
	init {
		model.message.observeForever { (code, response) ->
			if (response.getInteger("code") == 200) when (code) {
				0 -> {
					response.getJSONObject("data")?.let {
						if (total == -1) total = it.getInteger("total")
						it.getJSONArray("rows").forEach { item: Any? ->
							val title = "${(item as JSONObject).getString("askLeaveReasonName")} · ${item.getString("askLeaveTypeName")}"
							val context = getApplication<Application>()
						sections.add(SectionData(title,
							                         footerMenus = mutableStateListOf(
													 MenuItem(context.getString(R.string.print_leave_slip), Icons.Rounded.Print){
														 printLeaveSlip(item.getString("askLeaveId"))
														 true
													 }
																					  ),
							                         rows = extractValue(context,item,
							                                             intArrayOf(R.string.leave_reason,
													                                                     R.string.leave_type,
													                                                     R.string.leave_start_time,
													                                                     R.string.leave_end_time,
													                                                     R.string.leave_start_date,
													                                                     R.string.leave_end_date,
													                                                     R.string.leave_day,
													                                                     R.string.actual_leave_days,
													                                                     R.string.semester_cumulative_leave,
													                                                     R.string.approval_status,
													                                                     R.string.approval_stage,
													                                                     R.string.cancel_leave,
													                                                     R.string.is_canceled,
													                                                     R.string.reason_explanation,
													                                                     R.string.application_date,
													                                                     R.string.approval_date,
													                                                     R.string.term,
													                                                     R.string.campus,
													                                                     R.string.grade_major,
													                                                     R.string.attachment),
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
										                                                                     }".toUri()))
								                         }
							                         }))
						}
					}
				}
				1 -> {
					leaveReasons.addAll(response.getJSONArray("data").filterIsInstance<JSONObject>())
				}
				2 -> {
					val data = response.getJSONObject("data")
					attachment.value = data
					attachmentRows.clear()
					attachmentRows.add(RowData(data.getString("filePath"), data.getString("fileName")) {
						application.startActivity(Intent(application,
						                                                   BrowserActivity::class.java).setData("https://jwxt.sysu.edu.cn/jwxt/reports-register/askLeaveAgg/downloadFile?filePath=${data.getString("filePath")}&fileName=${
							data.getString("fileName")
						}".toUri()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
					})
				}
				3 -> {
					model.contextUtil.toast(response.getString("message",""))
					reset()
					_submitSuccess.postValue(true)
					refreshLeaveSlips()
				}
				4 -> {
					leaveData["semester"] = response.getJSONObject("data").getString("acadYearSemester")
				}
			}
		}
	}
	
	fun reset() {
		leaveDays = ""
		leaveType = ""
		leaveTypeName = ""
		leaveReasonDescription = ""
		leaveReason = null
		leaveReasonName = null
		startPeriod = 0
		endPeriod = 0
		startMillis = System.currentTimeMillis()
		endMillis = System.currentTimeMillis()
		attachment.value = null
		attachmentRows.clear()
		resetVersion++
	}
	
	fun fetchLeaveSlips() {
		model.addAndNext("jwxt/reports-register/askLeaveAgg/selfAskLeaveInfoList", "{\"param\":{},\"pageNo\":${++page},\"pageSize\":10,\"total\":true}", 0)
	}
	
	fun refreshLeaveSlips() {
		page = 0
		total = -1
		sections.clear()
		fetchLeaveSlips()
	}
	
	fun fetchLeaveTypes() {
		model.addAndNext("jwxt/base-info/codedata/findcodedataNames?datableNumber=436", 1)
	}
	
	/*{"semester":"2025-2","askLeaveDaysCount":0.5,"askLeaveTypeCode":"1","askLeaveTypeName":"短假","askLeaveBeginDate":"2026-08-16 08:00:00","askLeaveEndDate":"2026-08-16 12:00:00","askLeaveReasonCode":"1","askLeaveReasonExplanation":".","fileName":"中山大学logo.png","filePath":"reports-register/2026-08/16/2088838751325556736.png","whetherStuApply":"1"}*/
	fun resetSubmitSuccess() {
		_submitSuccess.postValue(false)
	}
	
	fun submitLeaveSlip() {
		val days = leaveDays.toDoubleOrNull()
		if (days == null || days <= 0) {
			model.contextUtil.toast(R.string.enter_valid_leave_days)
			return
		}
		if (leaveReason.isNullOrEmpty()) {
			model.contextUtil.toast(R.string.select_leave_reason)
			return
		}
		if (leaveReasonName.isNullOrEmpty()) {
			model.contextUtil.toast(R.string.select_leave_reason)
			return
		}
		val startDateFormat = DateTimeManager.toDateString(startMillis)
		val endDateFormat = DateTimeManager.toDateString(endMillis)
		val startTime = if (startPeriod == 0) "08:00:00" else "14:00:00"
		val endTime = if (endPeriod == 0) "12:00:00" else "18:00:00"
		leaveData["askLeaveTypeCode"] = leaveType
		leaveData["askLeaveTypeName"] = leaveTypeName
		leaveData["askLeaveDaysCount"] = days
		leaveData["askLeaveBeginDate"] = "$startDateFormat $startTime"
		leaveData["askLeaveEndDate"] = "$endDateFormat $endTime"
		leaveData["askLeaveReasonCode"] = leaveReason
		leaveData["askLeaveReasonExplanation"] = leaveReasonDescription
		attachment.value?.let {
			leaveData["fileName"] = it.getString("fileName")
			leaveData["filePath"] = it.getString("filePath")
		}
//		println(leaveData.toJSONString())
		model.addAndNext("jwxt/reports-register/askLeaveAgg/applyLeave", leaveData.toJSONString(), 3)
	}
	
	fun fetchTerms() {
		model.addAndNext("jwxt/base-info/acadyearterm/showNewAcadlist", 4)
	}
	
	fun uploadAttachment(fileRequestBody: FileRequestBody) {
		model.request(model.http.generateRequest("https://${model.host}/jwxt//reports-register/askLeaveAgg/importFile?", null, null)
			              .post(MultipartBody.Builder().setType(MultipartBody.FORM).addFormDataPart("file", fileRequestBody.fileName, fileRequestBody.file).build())
			              .build(), 2)
	}
	
	fun deleteAttachment() {
		attachment.value = null
		attachmentRows.clear()
	}
	fun printLeaveSlip(askLeaveId : String) {
		val path = "${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
		                             }/${getApplication<Application>().getString(R.string.leave_slip_filename, askLeaveId)}"
		DownloadManager.downloadFile(application,"https://${model.host}/jwxt/reports-register/askLeaveAgg/selfAskLeavePaper?askLeaveId=$askLeaveId",
		                             path,object : DownloadManager.DownloadListener {
			override fun onDownloadProgress(progress: Long, total: Long) {
			}
			
			override fun onDownloadComplete(path: String?) {
				viewModelScope.launch { model.contextUtil.toast("${model.contextUtil.context.getString(R.string.download_complete)}：$path")
				}
			}
			
			override fun onDownloadError(code: Int, message: String?) {
				viewModelScope.launch { model.contextUtil.toast("${model.contextUtil.context.getString(R.string.download_error)}：$message")
				}
			}})
	}
	
	override fun onCleared() {
		model.dispose()
	}
}