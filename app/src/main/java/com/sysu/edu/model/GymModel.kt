package com.sysu.edu.model

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.alibaba.fastjson2.JSON
import com.alibaba.fastjson2.JSONArray
import com.alibaba.fastjson2.JSONObject
import com.sysu.edu.api.AuthorizationJar
import com.sysu.edu.api.AuthorizationManager
import com.sysu.edu.api.CommonUtil
import com.sysu.edu.api.CookieManager
import com.sysu.edu.api.HttpManager
import com.sysu.edu.api.TargetUrl
import okhttp3.Request
import okhttp3.Response
import java.util.regex.Pattern

class GymModel(context: Context) : BaseModel(context) {
	override val authorizationManager: AuthorizationManager = AuthorizationManager("gym.sysu.edu.cn",
	                                                                               "gym-443.webvpn.sysu.edu.cn").also {
		it.setTargetUrl(TargetUrl.GYM, TargetUrl.GYM_WEBVPN)
	}
	override val http: HttpManager = HttpManager(Handler(Looper.getMainLooper())).apply {
		cookieManager = CookieManager(context)
		ua = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36 Edg/143.0.0.0"
		setAuthorizationRequired(true)
		authorizationJar = AuthorizationJar(context)
		header = mutableMapOf("Accept" to "application/json, text/plain, */*")
	}
	
	override fun handleResponse(request: CommonUtil.Tuple2<Request, Int>,
	                            response: Response): CommonUtil.Tuple2<Int, JSONObject>? {
		val content = response.body.string()
		var result: CommonUtil.Tuple2<Int, JSONObject>? = null
		when (response.code) {
			200 -> response.header("Content-Type")
				?.takeIf { it.contains("application/json") }
				?.let {
					var data = JSON.parse(content)
					data = if (data is JSONArray) JSONObject.of("data", data)
					else data as JSONObject
					result = CommonUtil.Tuple2(request.second, data)
					message.postValue(result)
				} ?: run {
				if (!authorizationManager.isAuthorized(content)) login(request)
				else if (Pattern.compile("人机识别检测").matcher(content).find()) login(request)
				else if (!authorizationManager.isAccessible(content)) retry(request)
				else http.handler.post { contextUtil.toast(content) }
			}
			400 -> {
				println("GymModel: ${response.code} $content")
				http.handler.post { contextUtil.toast(content) }
			}
			401 -> login(request)
		}
		return result
	}
}
