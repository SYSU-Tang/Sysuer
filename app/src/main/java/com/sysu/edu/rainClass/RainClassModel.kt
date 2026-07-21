package com.sysu.edu.rainClass

import android.content.Context
import androidx.compose.ui.graphics.Color
import com.alibaba.fastjson2.JSONObject
import com.sysu.edu.api.AuthorizationManager
import com.sysu.edu.api.CommonUtil
import com.sysu.edu.model.BaseModel
import okhttp3.Request
import okhttp3.Response

class RainClassModel(context: Context) : BaseModel(context) {
	override val authorizationManager: AuthorizationManager =
		AuthorizationManager("www.yuketang.cn", "www.yuketang.cn")
	
	companion object {
		const val GET_COURSE_LIST: Int = 0
		const val GET_USER_INFO: Int = 1
		
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
	}
	
	fun getCourseList() {
		addAndNext("v2/api/web/courses/list?identity=2", GET_COURSE_LIST)
	}
	
	fun getUserInfo() {
		addAndNext("v/course_meta/user_info", GET_USER_INFO)
	}
	
	override fun handleResponse(request: CommonUtil.Tuple2<Request, Int>,
	                            response: Response): CommonUtil.Tuple2<Int, JSONObject> {
		val content = response.body.string()
		val contentJSON = JSONObject.parseObject(content)
		val result = CommonUtil.Tuple2(request.second, contentJSON)
		message.postValue(result)
		return result
	}
}
