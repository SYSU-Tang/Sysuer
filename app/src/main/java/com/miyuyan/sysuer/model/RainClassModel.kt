package com.miyuyan.sysuer.model

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.alibaba.fastjson2.JSONObject
import com.miyuyan.sysuer.api.AuthorizationManager
import com.miyuyan.sysuer.api.CommonUtil
import com.miyuyan.sysuer.api.CookieManager
import com.miyuyan.sysuer.api.HttpManager
import okhttp3.Request
import okhttp3.Response

class RainClassModel(context: Context) : BaseModel(context) {
	override val authorizationManager: AuthorizationManager =
		AuthorizationManager("www.yuketang.cn", "www.yuketang.cn")

//	override val http: HttpManager = HttpManager(Handler(Looper.getMainLooper())).apply {
//		cookieManager = CookieManager(context)
//		header = mutableMapOf("xtbz" to "ykt")
//	}

	override fun handleResponse(
		request: CommonUtil.Tuple2<Request, Int>,
		response: Response,
	): CommonUtil.Tuple2<Int, JSONObject>? {
		val content = response.body.string()
		println("code ${response.code} content $content")
		var result: CommonUtil.Tuple2<Int, JSONObject>? = null
		when (response.code) {
			200 -> {
				response.header("Content-Type")?.takeIf { it.contains("application/json") }?.let {
					val contentJSON = JSONObject.parseObject(content)
					result = CommonUtil.Tuple2(request.second, contentJSON)
					sendMessage(result)
				}
			}

			401 -> {}
		}
		return result
	}
}