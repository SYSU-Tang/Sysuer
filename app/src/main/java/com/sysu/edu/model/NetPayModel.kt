package com.sysu.edu.model

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.alibaba.fastjson2.JSONObject
import com.alibaba.fastjson2.JSONValidator
import com.sysu.edu.api.AuthorizationManager
import com.sysu.edu.api.CommonUtil
import com.sysu.edu.api.CookieManager
import com.sysu.edu.api.HttpManager
import com.sysu.edu.api.TargetUrl
import okhttp3.Request
import okhttp3.Response

open class NetPayModel(context: Context) : BaseModel(context) {
	override val authorizationManager: AuthorizationManager = AuthorizationManager("netpay.sysu.edu.cn", "netpay.sysu.edu.cn").also {
		it.setTargetUrl(TargetUrl.NETPAY, TargetUrl.NETPAY)
	}
	override val http: HttpManager = HttpManager(Handler(Looper.getMainLooper())).apply {
		cookieManager = CookieManager(context)
		setHeader(mutableMapOf("accept-language" to "zh-CN,zh;q=0.9,en-US;q=0.8,en;q=0.7"))
	}
	
	override fun handleResponse(
		request: CommonUtil.Tuple2<Request, Int>,
		response: Response,
	                           ): CommonUtil.Tuple2<Int, JSONObject>? {
		val content = response.body.string()
		val result = CommonUtil.Tuple2(request.second, when {
			JSONValidator.from(content).validate() -> {
				val json = JSONObject.parse(content)
				if (!json.getBoolean("success")) {
					login(request)
					return null
				}
				json
			}
//			contentType.contains("text/html") -> JSONObject.of("data", content)
			else -> JSONObject.of("data", content)
		})
		message.postValue(result)
		afterLoginRequest.remove(request)
		return result
	}
}
