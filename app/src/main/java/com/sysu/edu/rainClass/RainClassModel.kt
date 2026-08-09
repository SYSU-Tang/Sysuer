package com.sysu.edu.rainClass

import android.content.Context
import androidx.compose.ui.graphics.Color
import com.alibaba.fastjson2.JSONObject
import com.sysu.edu.api.AuthorizationManager
import com.sysu.edu.api.CommonUtil
import com.sysu.edu.model.BaseModel
import okhttp3.Request
import okhttp3.Response
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class RainClassModel(context: Context) : BaseModel(context) {
	override val authorizationManager: AuthorizationManager = AuthorizationManager("www.yuketang.cn",
	                                                                               "www.yuketang.cn")
	
	companion object {
		const val GET_COURSE_LIST: Int = 0
		const val GET_USER_INFO: Int = 1
		const val GET_EXAMS_LIST: Int = 2
		const val GET_EXAM_INFO: Int = 3
		const val GET_CLASSROOM_INFO: Int = 4
		const val GET_PROBLEM_INFO: Int = 5
		fun formatTerm(term: Int?): String {
			if (term == null) return ""
			val year = term / 100
			val semesterStr = when (val semester = term % 100) {
				1 -> "秋"
				2 -> "春"
				3 -> "夏"
				else -> "$semester"
			}
			return "$year $semesterStr"
		}
		
		fun getTermColor(term: Int?): Color {
			if (term == null) return Color(0xFF212121)
			return when (term % 100) {
				1 -> Color(0xFF1A237E) // 秋季 - 深蓝
				2 -> Color(0xFF1B5E20) // 春季 - 深绿
				3 -> Color(0xFFB71C1C) // 夏季 - 深红
				else -> Color(0xFF424242)
			}
		}
		
		fun formatTimestamp(timestamp: Long?): String {
			if (timestamp == null) return ""
			val instant = Instant.ofEpochSecond(timestamp)
			val localDateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault())
			val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
			localDateTime.format(formatter)
			return localDateTime.format(formatter)
		}
		
		fun formatTimestampMillis(timestamp: Long?): String {
			if (timestamp == null) return ""
			val instant = Instant.ofEpochMilli(timestamp)
			val localDateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault())
			val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
			return localDateTime.format(formatter)
		}
	}
	
	/*
	* {
                "term": 202502,
                "name": "2026春-202523664",
                "short_name": null,
                "students_count": 67,
                "time": 1770607329000,
                "teacher": {
                    "user_id": 67276306,
                    "name": "郑泽波",
                    "avatar": "https://thirdwx.qlogo.cn/mmopen/vi_32/scqYuwYM7IPia6HRJtfuGDibvFCjf98w1jHPIZ4MicmTOV4CUMku3lqkiceJwwicr6LymthS6ia1eKiaNe47u3HCz1pEw/132"
                },
                "published_kg_id": null,
                "create_time": 1770604659000,
                "role": 5,
                "agent_id": null,
                "project_type": null,
                "ways_of_operation": null,
                "university_course_series_id": "202502-202523664",
                "color_code": 3,
                "classroom_id": 29789011,
                "top_time": 0,
                "course": {
                    "en_name": null,
                    "manage_permission": false,
                    "name": "微机原理与嵌入式系统实验",
                    "short_name": null,
                    "admin_id": 13310088,
                    "platform": null,
                    "university_id": 2668,
                    "type": 2,
                    "id": 2150645,
                    "is_pro": true,
                    "university_mini_logo": "https://qn-sx.yuketang.cn/sysu_mini_logo_pic.png",
                    "university_name": "中山大学"
                }
            }
	* */
	fun getCourseList() {
		addAndNext("v2/api/web/courses/list?identity=2", GET_COURSE_LIST)
	}
	
	fun getUserInfo() {
		addAndNext("v/course_meta/user_info", GET_USER_INFO)
	}
	
	/*
	* {
    "code": 0,
    "msg": "OK",
    "data": {
        "onLessonClassrooms": [],
        "upcomingExam": [
            {
                "classroom_name": "2026春-202525174",
                "user_avatar": "https://qn-sx.yuketang.cn/avatar/QLR5aKwLYaMobVKTzZapamWx0ZE4icboWMKUwdekBsbT9d8ykWrKicblxqYpTI9E5iafJZz3WT56KpXydv42kheqZ26TqnMl9vm",
                "end_time": 1784865600,
                "title": "劳动教育考查-2025-2026春",
                "start_time": 1784633801,
                "classroom_id": 29789054,
                "type": "exam",
                "id": 4447243
            },
            {
                "classroom_name": "2026春-202525162",
                "user_avatar": "https://qn-sx.yuketang.cn/avatar/QLR5aKwLYaMobVKTzZapamWx0ZE4icboWMKUwdekBsbT9d8ykWrKicblxqYpTI9E5iafJZz3WT56KpXydv42kheqZ26TqnMl9vm",
                "end_time": 1784865600,
                "title": "国家安全教育考查-2025-2026春",
                "start_time": 1784633725,
                "classroom_id": 29789060,
                "type": "exam",
                "id": 4447240
            },
            {
                "classroom_name": "2026春-202527607",
                "user_avatar": "https://qn-sx.yuketang.cn/avatar/QLR5aKwLYaMobVKTzZapamWx0ZE4icboWMKUwdekBsbT9d8ykWrKicblxqYpTI9E5iafJZz3WT56KpXydv42kheqZ26TqnMl9vm",
                "end_time": 1784865600,
                "title": "形势与政策考查-2025-2026春",
                "start_time": 1784633445,
                "classroom_id": 30440882,
                "type": "exam",
                "id": 4447237
            }
        ]
    }
}
	* */
	fun getExams() {
		addAndNext("api/v3/classroom/on-lesson-upcoming-exam", GET_EXAMS_LIST)
	}
	
	/*
	* {
    "msg": "",
    "status": 200,
    "data": {
        "server_time": 1784698502486,
        "app_front_random_snap": 0,
        "paper_count": 1,
        "max_retry_random": 0,
        "exam_exercise_video_attachment_1G": 0,
        "identity_auth": 0,
        "deadline": 1784865600000,
        "university_id": 2668,
        "capture_screen": 0,
        "max_screen_cuts_num": 0,
        "max_retry": 1,
        "is_sku_time": false,
        "organize_problem_method": 0,
        "classroom_name": null,
        "course_name": null,
        "title": "\u52b3\u52a8\u6559\u80b2\u8003\u67e5-2025-2026\u6625",
        "OCR_type": "0",
        "is_single_paper": true,
        "organize_paper_method": 0,
        "show_score_time": -1000,
        "en_copy": true,
        "is_offline": false,
        "open_screen_cuts": 0,
        "access_restriction_info_for_graph": "",
        "start_time": 1784633801000,
        "open_access_restriction_for_graph": false,
        "online_proctor": 0,
        "show_score": false,
        "description": "",
        "show_answer": false,
        "total_score": 100.0,
        "en_crypt": false,
        "user_role": 5,
        "user": {
            "user_name": "\u5510\u8d24\u6807",
            "user_id": 82386986,
            "avatar": "http://qn-sx.yuketang.cn/tougao_pic_BmEhrLg83pm.png",
            "language": null,
            "school_number": "24308152"
        },
        "access_restriction_info": "",
        "app_capture_screen": 0,
        "encrypt": "True",
        "limit_early_submission_time": 0,
        "show_perm": true,
        "limit_early_submission": false,
        "page_switch_detection": 0,
        "is_manual_review": 0,
        "is_partial_participation": false,
        "classroom_id": 29789054,
        "face_auth_status": {
            "online_proctor": 0,
            "has_idphoto": false
        },
        "problem_dict_count": 0,
        "has_problem_dict": false,
        "problem_count": 1,
        "platform_type": 0,
        "client_type": "None",
        "limit": 0,
        "open_access_restriction": false,
        "way_of_score": 1,
        "web_random_take_face_photo": 0,
        "force_confirm": false,
        "result": {
            "status": 0,
            "score": null,
            "incorrect_count": null,
            "unfinished_count": null
        }
    },
    "success": true
}
	* */
	fun getExamInfo(examId: Int, classroomId: Int) {
		addAndNext("v/exam/cover?exam_id=$examId&classroom_id=$classroomId", GET_EXAM_INFO)
	}
	
	/*
	* {
    "errcode": 0,
    "errmsg": "Success",
    "data": {
        "id": 30440882,
        "name": "2026春-202527607",
        "short_name": null,
        "course_id": "3615141",
        "course_name": "形势与政策",
        "students_count": 289,
        "course_short_name": null,
        "teacher_name": "林玥琪",
        "teacher_avatar": "http://thirdwx.qlogo.cn/mmopen/GibOcMnJ0cVLUBTHWuJYeBjdyLgOKicOhVK2hTTgTicVjCEcXUwqblibXeRYXK83z1vL6ErpyibHFUbXS0ckdDLicvCJ5nOyibmsCBT/132",
        "uv_id": 2668,
        "university_logo": "https://qn-sx.yuketang.cn/sysu_mini_logo_pic.png",
        "university_domain": "https://sysu.xuetangx.com",
        "user_role": 5,
        "settings": {
            "group": true,
            "discussion": true,
            "classmates": true,
            "questions": true
        },
        "transcript_status": true,
        "free_sku_id": 15371554,
        "platform": 3,
        "course_sign": "9mFkCxwMx9k",
        "extra_info": {
            "has_classend": false,
            "is_continue_study_of_classend_by_classroom": true,
            "is_continue_study_of_classend_by_uv": true
        },
        "class_start": 1769875200000,
        "class_end": 1785513599000
    }
}
	* */
	fun getClassroomInfo(classroomId: Int) {
		addAndNext("v2/api/web/classrooms/$classroomId?role=5", GET_CLASSROOM_INFO)
	}
	
	/*
	* {
    "errcode": 0,
    "errmsg": "",
    "data": {
        "problems": [
            {
                "Body": "<div class=\"custom_ueditor_cn_body\"><p style=\"text-indent:43px\"><span style=\"font-family: &#39;Times New Roman&#39;;font-size: 21px\"><span style=\"font-family:仿宋_GB2312\">当下部分青年存在职业等级偏见：填报志愿、选择就业时，一味追捧写字楼、科研、公职类岗位，轻视建筑工人、管网维修工、保洁、餐饮服务、物流配送等一线岗位，认为这类劳动</span> <span style=\"font-family:Times New Roman\">“</span><span style=\"font-family:仿宋_GB2312\">不体面、价值低</span><span style=\"font-family:Times New Roman\">”</span><span style=\"font-family:仿宋_GB2312\">。</span></span></p><p style=\"text-indent:43px\"><span style=\"font-family: &#39;Times New Roman&#39;;font-size: 21px\"><span style=\"font-family:仿宋_GB2312\">现实社会运转证明：城市排水工坚守地下管网</span> 17 <span style=\"font-family:仿宋_GB2312\">年，保障全城汛期安全；塔吊司机高空作业，支撑城市高楼建设；畜牧育种工人扎根养殖场，突破种禽技术垄断；基层骑手化身社区流动网格员，参与基层治理、应急帮扶。没有千千万万基层体力、服务劳动者，城市生活、生产建设、民生保障都会全面停摆。</span></span></p><p style=\"text-indent:43px\"><span style=\"font-family: &#39;Times New Roman&#39;;font-size: 21px\"><span style=\"font-family:仿宋_GB2312\">中共中央、国务院《关于全面加强新时代大中小学劳动教育的意见》提出，要引导学生懂得劳动创造美好生活，体认劳动不分贵贱，尊重普通劳动者。社会分工是生产力发展的必然结果，社会主义制度消除了旧式分工带来的阶级对立，所有合法劳动者都是国家、社会的主人，人格尊严完全平等。</span></span></p><p><span style=\"font-family: &#39;Times New Roman&#39;;font-size: 21px\">&nbsp;</span></p><p style=\"text-indent:43px\"><span style=\"font-family: &#39;Times New Roman&#39;;font-size: 21px\"><span style=\"font-family:仿宋_GB2312\">有人依据岗位环境、劳动形式将职业划分高低，认为一线体力劳动不值一提。请</span></span><span style=\"font-family: 仿宋_GB2312;font-size: 21px\">从理论、实践等角度出发</span><span style=\"font-family: &#39;Times New Roman&#39;;font-size: 21px\"><span style=\"font-family:仿宋_GB2312\">，批驳该错误观点，阐释劳动无贵贱的内在依据。</span></span></p><p><br/></p></div>",
                "Type": "ShortAnswer",
                "data": {},
                "Score": 100,
                "index": 0,
                "score": 100,
                "isEdit": false,
                "Options": [],
                "Version": "c811175e87e94e41b420fa8e0c6b0139",
                "FolderID": 0,
                "TypeText": "主观题",
                "HasRemark": false,
                "LibraryID": 173830,
                "ProblemID": 204561851,
                "folder_id": 0,
                "TemplateID": 74016594,
                "difficulty": 4,
                "library_id": 173830,
                "problem_id": 204561851,
                "ProblemType": 5,
                "template_id": 74016594,
                "AllowResults": [
                    "text",
                    "pic",
                    "file"
                ],
                "review_detail": {},
                "TypeRenameText": "",
                "isContinueWithWrong": 0,
                "scope_of_question_answer": 0
            }
        ],
        "has_problem_dict": false,
        "font": "",
        "title": "劳动教育考查-2025-2026春"
    }
}
	* */
	fun getProblem(examId: Int) {
		setAndNext("https://examination.xuetangx.com/exam_room/show_paper?exam_id=$examId",
		           null,
		           null,
		           GET_PROBLEM_INFO)
	}
	
	override fun handleResponse(request: CommonUtil.Tuple2<Request, Int>,
	                            response: Response): CommonUtil.Tuple2<Int, JSONObject>? {
		val content = response.body.string()
		println("code ${response.code} content $content")
		val result = null
		when (response.code) {
			200 -> {
				response.header("Content-Type")?.takeIf { it.contains("application/json") }?.let {
					val contentJSON = JSONObject.parseObject(content)
					val result = CommonUtil.Tuple2(request.second, contentJSON)
					message.postValue(result)
				}
			}
			401 -> {}
		}
		return result
	}
}
