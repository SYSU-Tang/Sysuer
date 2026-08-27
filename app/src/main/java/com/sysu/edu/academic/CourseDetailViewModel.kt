package com.sysu.edu.academic

import android.app.Application
import android.os.Environment
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.alibaba.fastjson2.JSONArray
import com.alibaba.fastjson2.JSONObject
import com.sysu.edu.R
import com.sysu.edu.api.CommonUtil.extractValue
import com.sysu.edu.api.CommonUtil.trim
import com.sysu.edu.api.DownloadManager
import com.sysu.edu.model.JwxtModel
import com.sysu.edu.view.RowData
import com.sysu.edu.view.RowOrientation
import com.sysu.edu.view.SectionData
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class CourseDetailViewModel(application: Application) : AndroidViewModel(application) {
	private val model = JwxtModel(application)
	private val app = application
	val classNum: MutableState<String?> = mutableStateOf(null)
	val courseId: MutableState<String?> = mutableStateOf(null)
	val courseInfoId: MutableState<String?> = mutableStateOf(null)
	val outlineId: MutableState<String?> = mutableStateOf(null)
	val courseName: MutableState<String?> = mutableStateOf(null)
	private val _detailSections = mutableStateListOf<SectionData>()
	val detailSections: SnapshotStateList<SectionData> = _detailSections
	private val _outlineSections = mutableStateListOf<SectionData>()
	val outlineSections: SnapshotStateList<SectionData> = _outlineSections
	private val _toastEvent = MutableSharedFlow<String>()
	val toastEvent: SharedFlow<String> = _toastEvent.asSharedFlow()
	private var outlineLoaded = false
	private var outline2Loaded = false
	private var outlineInfo: JSONObject? = null
	private var outline2Data: JSONObject? = null
	
	init {
		model.message.observeForever { (code, response) ->
			if (response.getInteger("code") == 200) {
				val data = response.getJSONObject("data")
				if (data != null) when (code) {
					1 -> {
						handleOutlineInfo(data)
						model.nextAll()
					}
					2 -> {
						handleOutline2(data)
						model.nextAll()
					}
					3 -> {
						handleOutlineIdQuery(data)
					}
				}
			}
		}
	}
	
	fun initFromIntent(classNum: String?, courseId: String?) {
		this@CourseDetailViewModel.classNum.value = classNum
		this@CourseDetailViewModel.courseId.value = courseId
		if (classNum != null) fetchCourseOutline()
		else fetchCourseOutline2()
		model.next()
	}
	
	private fun handleOutlineInfo(data: JSONObject) {
		val info = data.getJSONObject("outlineInfo") ?: return
		outlineInfo = info
		courseId.value = info.getString("courseId")
		courseInfoId.value = info.getString("outlineCourseInfoId")
		courseName.value = info.getString("courseName")
		outlineLoaded = true
		val scheduleList = data.getJSONArray("scheduleList")
		if (scheduleList != null) {
			updateOutlineSections(scheduleList)
		}
		if (!outline2Loaded) fetchCourseOutline2()
	}
	
	private fun handleOutline2(data: JSONObject) {
		outline2Data = data
		classNum.value = data.getString("courseNumber")
		outline2Loaded = true
		
		outlineInfo?.let { updateDetailSections(it, data) }
		
		if (!outlineLoaded) fetchCourseOutline()
	}
	
	private fun handleOutlineIdQuery(data: JSONObject) {
		val rows = data.getJSONArray("rows")
		if (rows != null && rows.isNotEmpty()) {
			val id = rows.getJSONObject(0).getString("id")
			if (!id.isNullOrEmpty()) {
				outlineId.value = id
				downloadOutline(id)
				return
			}
		}
		viewModelScope.launch {
			_toastEvent.emit(app.getString(R.string.no_outline_found))
		}
	}
	
	fun fetchCourseOutline() {
		if (outlineLoaded) return
		outlineLoaded = true
		val code = classNum.value ?: return
		model.add("jwxt/training-programe/courseoutline/getalloutlineinfo?courseNum=$code&auditStatus=99", 1)
	}
	
	fun fetchCourseOutline2() {
		if (outline2Loaded) return
		outline2Loaded = true
		val id = courseId.value ?: return
		model.add("jwxt/base-info/courseLibrary/findById?id=$id", 2)
	}
	
	fun getOutlineId() {
		val infoId = courseInfoId.value ?: return
		model.addAndNext("jwxt/training-programe/courseoutline/showOutlineUpdataCourse", "{\"pageNo\":1,\"pageSize\":10,\"total\":true,\"param\":{\"outlineCourseInfoId\":\"$infoId\"}}", 3)
	}
	
	fun downloadOutline(outlineId: String) {
		DownloadManager.downloadFile(app,
		                             model.http.generateRequest("https://jwxt.sysu.edu.cn/jwxt/training-programe/courseoutline/outlineupdateworddownload?outlineUpdateId=$outlineId", null, null)
			                             .header("Cookie", model.cookie)
			                             .header("Referer", "https://jwxt.sysu.edu.cn/")
			                             .build(),
		                             "${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)}/${courseName.value}.zip",
		                             true,
		                             object : DownloadManager.DownloadListener {
			                             override fun onDownloadProgress(progress: Long, total: Long) {}
			                             override fun onDownloadComplete(path: String?) {
				                             viewModelScope.launch {
					                             _toastEvent.emit("${app.getString(R.string.download_complete)}: $path")
				                             }
			                             }
			                             
			                             override fun onDownloadError(code: Int, message: String?) {
				                             viewModelScope.launch {
					                             _toastEvent.emit(message ?: app.getString(R.string.download_error))
				                             }
			                             }
		                             })
	}
	
	private fun updateDetailSections(info: JSONObject, detail: JSONObject) {
		_detailSections.clear()
		val none = app.getString(R.string.none)
		val introRows = extractValue(app,
		                             info,
		                             intArrayOf(
			                             R.string.course_intro,
			                             R.string.course_goal,
			                             R.string.course_method,
			                             R.string.course_score,
			                             R.string.course_reference,
			                             R.string.course_resource,
		                                       ),
		                             arrayOf(
			                             "courseContentInChinese",
			                             "courseObjectiveAndRequirement",
			                             "teachMethod",
			                             "evaluationMethod",
			                             "referenceBook",
			                             "courseResource",
		                                    ),
		                             none)
		val infoKeys = arrayOf("courseName",
		                       "faceProfessionName",
		                       "courseTypeName",
		                       "courseNum",
		                       "courseId",
		                       "subCourseTypeName",
		                       "subTypeModuleName",
		                       "courseTextBook",
		                       "credit",
		                       "totalHours",
		                       "lecturesCreHours",
		                       "labCreHours",
		                       "weekHours",
		                       "totalHoursComment",
		                       "languageName",
		                       "establishUnitNumberName",
		                       "planClassSize",
		                       "teacherName",
		                       "coursePrincipal",
		                       "intendedAcadYear",
		                       "intendedCampusName")
		val names = app.resources.getStringArray(R.array.course_outline)
		val detailRows = mutableStateListOf<RowData>()
		infoKeys.zip(names).forEachIndexed { index, (key, name) ->
			val source = if (index == 9 || index == 12) detail else info
			detailRows.add(RowData(name, source.getString(key, none).trim()))
		}
		_detailSections.add(SectionData(app.getString(R.string.course_detail), rows = detailRows))
		_detailSections.add(SectionData(courseName.value, rows = introRows, rowOrientation = RowOrientation.Vertical))
	}
	
	private fun updateOutlineSections(scheduleList: JSONArray) {
		_outlineSections.clear()
		val none = app.getString(R.string.none)
		scheduleList.forEachIndexed { i, e ->
			if (e is JSONObject) {
				val rows = mutableStateListOf<RowData>()
				val section = trim(e.getString("sectionDesignation", none))
				val hours = trim(e.getString("teachingHours", none))
				val content = trim(e.getString("teachingMainContent", none))
				val elements = trim(e.getString("courseElements", none))
				val keyPoints = trim(e.getString("keyPoints", none))
				rows.add(RowData("章节", section))
				rows.add(RowData("学时", hours))
				rows.add(RowData("教学内容", content))
				rows.add(RowData("育人元素", elements))
				rows.add(RowData("重点、难点", keyPoints))
				_outlineSections.add(SectionData(title = "第${i + 1}章（${hours}${app.getString(R.string.study_hour)}）", rows = rows))
			}
		}
	}
	
	override fun onCleared() {
		model.dispose()
	}
}