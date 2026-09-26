package com.miyuyan.sysuer.rainClass

import android.app.Application
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.alibaba.fastjson2.JSONArray
import com.alibaba.fastjson2.JSONObject
import com.miyuyan.sysuer.R
import com.miyuyan.sysuer.api.DateTimeManager
import com.miyuyan.sysuer.model.RainClassModel
import com.miyuyan.sysuer.view.UiState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

class RainClassViewModel(application: Application) : AndroidViewModel(application) {
	private val model = RainClassModel(application)
	private val _courseList = MutableStateFlow<List<JSONObject>>(emptyList())
	val courseList = _courseList.asStateFlow()
	val courseUiState = model.getUiState(GET_COURSE_LIST)

	private val _examList = MutableStateFlow<List<JSONObject>>(emptyList())
	val examList = _examList.asStateFlow()
	val examsUiState = model.getUiState(GET_EXAMS_LIST)

	private val _examInfo = MutableStateFlow<JSONObject?>(null)
	val examInfo = _examInfo.asStateFlow()
	val examInfoUiState = model.getUiState(GET_EXAM_INFO)

	private val _problemList = MutableStateFlow<List<JSONObject>>(emptyList())
	val problemList = _problemList.asStateFlow()
	val problemUiState = model.getUiState(GET_PROBLEM_INFO)

	private val _studyLogList = MutableSharedFlow<List<JSONObject>>(1)
	val studyLogList = _studyLogList.asSharedFlow()
//	val _studyLogUiState = model.getUiState(GET_STUDY_LOG)

	private val _hasMore = MutableStateFlow(false)
	val hasMore = _hasMore.asStateFlow()

	private val _studyLogStatus = MutableSharedFlow<JSONObject>(1)
	val studyLogStatus = _studyLogStatus.asSharedFlow()

	private val _userInfo = MutableStateFlow<JSONObject?>(null)
	val userInfo = _userInfo.asStateFlow()
	private val _isLoginRequired = MutableStateFlow(false)
	val isLoginRequired = _isLoginRequired.asStateFlow()
	val userUiState = model.getUiState(GET_USER_INFO)

	private val _courseInfo = MutableStateFlow<JSONObject?>(null)
	val courseInfo = _courseInfo.asStateFlow()
	val courseInfoUiState = model.getUiState(GET_CLASSROOM_INFO)

	private val _chapterList = MutableStateFlow<List<JSONObject>>(emptyList())
	val chapterList = _chapterList.asStateFlow()
	val chapterUiState = model.getUiState(GET_CHAPTER)

	private val _loginRequired = MutableSharedFlow<Unit>()
	val loginRequired = _loginRequired.asSharedFlow()

	init {
		viewModelScope.launch {
			model.messageChannel.collect { (what, response) ->
				when (what) {
					GET_COURSE_LIST -> if (response.containsKey("errcode") && response.getInteger("errcode") == 401002) {
						_loginRequired.tryEmit(Unit)
						courseUiState.value = UiState.Error
					} else if (response.containsKey("errcode") && response.getInteger("errcode") == 0) {
						response.getJSONObject("data")?.getJSONArray("list")?.let { list ->
							courseUiState.value =
								if (list.isEmpty()) UiState.Empty else UiState.Content
							_courseList.value = list.filterIsInstance<JSONObject>()
						}
					}

					GET_EXAMS_LIST -> {
						if (response.containsKey("errcode") && response.getInteger("errcode") == 401002) {
							_loginRequired.tryEmit(Unit)
							examsUiState.value = UiState.Error
						} else if (response.containsKey("code") && response.getInteger("code") == 0) {
							response.getJSONObject("data")?.getJSONArray("upcomingExam")
								?.let { upcoming ->
									examsUiState.value =
										if (upcoming.isEmpty()) UiState.Empty else UiState.Content
									_examList.value = upcoming.filterIsInstance<JSONObject>()
								}
						}
					}

					GET_EXAM_INFO -> if (response.containsKey("success") && response.getBoolean("success")) {
						_examInfo.value = response.getJSONObject("data")
					}

					GET_PROBLEM_INFO -> if (response.containsKey("errcode") && response.getInteger("errcode") == 0) {
						response.getJSONObject("data")?.getJSONArray("problems")?.let {
							if (it.isEmpty()) UiState.Empty else UiState.Content
							_problemList.value = it.filterIsInstance<JSONObject>()
						}
					}

					GET_USER_INFO -> {
						userUiState.value = UiState.Content
						if (response.containsKey("op") && response.getString("op") == "web_redirect") {
							_isLoginRequired.value = true
						} else {
							_userInfo.value =
								response.getJSONObject("data")?.getJSONObject("user_profile")
							_isLoginRequired.value = false
						}
					}

					GET_CLASSROOM_INFO -> {
						if (response.containsKey("errcode") && response.getInteger("errcode") == 0) {
							_courseInfo.value = response.getJSONObject("data")
							courseInfoUiState.value = UiState.Content
						}
					}

					GET_STUDY_LOG -> {
						if (response.containsKey("errcode") && response.getInteger("errcode") == 401002) {
							_loginRequired.tryEmit(Unit)
						} else if (response.containsKey("errcode") && response.getInteger("errcode") == 0) {
							response.getJSONObject("data")?.let { data ->
								data.getJSONArray("activities")?.filterIsInstance<JSONObject>()
									?.let { _studyLogList.tryEmit(it) }
								_hasMore.value = data.getBooleanValue("has_more", false)
							}
						}
					}

					GET_STUDY_LOG_STATUS -> {
						_studyLogStatus.tryEmit(response.getJSONObject("data"))
					}

					GET_CHAPTER -> {
						if (response.containsKey("success") && response.getBoolean("success")) {
							response.getJSONObject("data")?.getJSONArray("course_chapter")
								?.filterIsInstance<JSONObject>()?.let { chapters ->
									chapterUiState.value =
										if (chapters.isEmpty()) UiState.Empty else UiState.Content
									_chapterList.value = chapters
								}
						}
					}
				}
			}
		}
	}


	companion object {
		const val GET_COURSE_LIST: Int = 0
		const val GET_USER_INFO: Int = 1
		const val GET_EXAMS_LIST: Int = 2
		const val GET_EXAM_INFO: Int = 3
		const val GET_CLASSROOM_INFO: Int = 4
		const val GET_PROBLEM_INFO: Int = 5
		const val GET_STUDY_LOG: Int = 6
		const val GET_STUDY_LOG_STATUS: Int = 7
		const val GET_CHAPTER: Int = 8

		val STUDY_LOG_TYPES = mapOf(
				0 to -1,  // 全部日志
				1 to 14,  // 课堂
				2 to 15,  // 线上学习
				3 to 5,   // 试卷
				4 to 9    // 公告
		)

		fun getStudyLogTypeText(type: Int?): String = when (type) {
			14 -> "课堂"
			15 -> "线上学习"
			5 -> "试卷"
			9 -> "公告"
			16 -> "课件"
			19 -> "作业"
			else -> "其他"
		}

		fun getLeafTypeText(type: Int?): String = when (type) {
			3 -> "课件"
			4 -> "视频"
			5 -> "试卷"
			6 -> "作业"
			7 -> "讨论"
			else -> "其他"
		}

		fun getLeafTypeIcon(type: Int?): Int = when (type) {
			3 -> R.drawable.book
			4 -> R.drawable.voice
			5 -> R.drawable.exam
			6 -> R.drawable.todo
			7 -> R.drawable.forum
			else -> R.drawable.course
		}

		fun formatTerm(term: Int?): String = term?.let {
			"${term / 100} ${
				when (val semester = term % 100) {
					1 -> "秋"
					2 -> "春"
					3 -> "夏"
					else -> "$semester"
				}
			}"
		} ?: ""

		fun getTermColor(term: Int?): Color = Color(term?.let {
			when (term % 100) {
				1 -> 0xFF1A237E // 秋季 - 深蓝
				2 -> 0xFF1B5E20 // 春季 - 深绿
				3 -> 0xFFB71C1C // 夏季 - 深红
				else -> 0xFF424242
			}
		} ?: 0xFF212121)

		val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
		fun formatTimestampSecond(timestamp: Long?): String = timestamp?.let {
			DateTimeManager.toDateTimeString(
					timestamp, formatter, TimeUnit.SECONDS
			)
		} ?: ""

		fun formatTimestampMillie(timestamp: Long?): String =
			timestamp?.let { DateTimeManager.toDateTimeString(timestamp) } ?: ""
	}

	fun getCourseList() {
		model.enqueue("v2/api/web/courses/list?identity=2", GET_COURSE_LIST)
	}

	fun getUserInfo() {
		model.enqueue("v/course_meta/user_info", GET_USER_INFO)
	}

	fun getExams() {
		model.enqueue("api/v3/classroom/on-lesson-upcoming-exam", GET_EXAMS_LIST)
	}

	fun getExamInfo(examId: Int, classroomId: Int) {
		model.enqueue("v/exam/cover?exam_id=$examId&classroom_id=$classroomId", GET_EXAM_INFO)
	}

	fun getCourseInfo(courseId: String) {
		model.enqueue("v2/api/web/classrooms/$courseId?role=5", GET_CLASSROOM_INFO)
	}

	fun getProblem(examId: Int) {
		model.enqueueUrl(
				"https://examination.xuetangx.com/exam_room/show_paper?exam_id=$examId",
				code = GET_PROBLEM_INFO
		)
	}

	fun getStudyLog(courseId: String, type: Int, page: Int = 0) {
		model.enqueue(
				"v2/api/web/logs/learn/$courseId?actype=$type&page=$page&offset=20&sort=-1",
				GET_STUDY_LOG
		)
	}

	fun getStudyLogStatus(courseId: String, activityId: JSONArray) {
		model.enqueue(
				model.http.generateRequest(
						"https://${model.host}/mooc-api/v1/lms/learn/course/pub_new_pro",
						JSONObject.of(
								"cid", courseId, "new_id", activityId
						).toJSONString(),
						null
				).header("xtbz", "ykt").header("classroom-id", courseId)
					.header("x-csrftoken", model.cookieManager?.get(model.host, "csrftoken") ?: "")
					.build(), GET_STUDY_LOG_STATUS
		)
	}

	/*
	* {
  "data": {
    "course_id": 2150675,
    "course_chapter": [
      {
        "section_leaf_list": [
          {
            "name": "Ch0-绪论【课件】",
            "is_locked": false,
            "start_time": 1789097865000,
            "chapter_id": 17001173,
            "section_id": null,
            "leaf_type": 3,
            "id": 89398037,
            "is_show": true,
            "end_time": 0,
            "score_deadline": 1801411199000,
            "is_open_type": false,
            "is_score": false,
            "is_assessed": false,
            "order": 1,
            "node_id": 0,
            "leafinfo_id": 89398705
          },
          {
            "is_assessed": false,
            "is_third_party": false,
            "start_time": 1789098091000,
            "is_group": false,
            "section_id": null,
            "score_type": 1,
            "is_score": true,
            "is_show": true,
            "score_deadline": 1791647999000,
            "related_agent_info": {
              "answer_order": "none",
              "open_work_type": "",
              "agent_id": null,
              "content_score": null,
              "ai_workflow_id": null,
              "agent_score": null,
              "agent_scene_ids": []
            },
            "id": 89398110,
            "leafinfo_id": 89398778,
            "group_id": 0,
            "name": "绪论课作业",
            "is_locked": false,
            "chapter_id": 17001173,
            "leaf_type": 6,
            "snapshot_group_id": 0,
            "node_id": 0,
            "end_time": 0,
            "create_type": 0,
            "is_open_type": false,
            "open_exercise_type_label": "",
            "order": 2
          }
        ],
        "order": 1,
        "id": 17001173,
        "name": "绪论"
      },
      {
        "section_leaf_list": [
          {
            "name": "Ch1-微加工与集成实验环境",
            "is_locked": false,
            "start_time": 1789564164000,
            "chapter_id": 17087630,
            "section_id": null,
            "leaf_type": 3,
            "id": 89978045,
            "is_show": true,
            "end_time": 0,
            "score_deadline": 1801411199000,
            "is_open_type": false,
            "is_score": false,
            "is_assessed": false,
            "order": 1,
            "node_id": 0,
            "leafinfo_id": 89978713
          }
        ],
        "order": 3,
        "id": 17087630,
        "name": "Ch1-微加工与集成实验环境"
      },
      {
        "section_leaf_list": [
          {
            "name": "Ch2-1-光学光刻技术",
            "is_locked": false,
            "start_time": 1789567272000,
            "chapter_id": 17088112,
            "section_id": null,
            "leaf_type": 3,
            "id": 89982285,
            "is_show": true,
            "end_time": 0,
            "score_deadline": 1801411199000,
            "is_open_type": false,
            "is_score": false,
            "is_assessed": false,
            "order": 1,
            "node_id": 0,
            "leafinfo_id": 89982953
          }
        ],
        "order": 4,
        "id": 17088112,
        "name": "Ch2-1-光学光刻技术"
      }
    ],
    "course_name": "光电器件集成技术"
  },
  "success": true
}
	* */
	fun getChapter(courseId: String) {
		model.enqueue(
				model.http.generateRequest(
						"https://${model.host}/mooc-api/v1/lms/learn/course/chapter?cid=$courseId",
				).header("xtbz", "ykt").build(), GET_CHAPTER
		)
	}
}