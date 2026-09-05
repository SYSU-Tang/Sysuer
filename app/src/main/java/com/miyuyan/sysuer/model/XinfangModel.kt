package com.miyuyan.sysuer.model

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.alibaba.fastjson2.JSON
import com.alibaba.fastjson2.JSONArray
import com.alibaba.fastjson2.JSONObject
import com.miyuyan.sysuer.api.AuthorizationManager
import com.miyuyan.sysuer.api.CommonUtil
import com.miyuyan.sysuer.api.CookieManager
import com.miyuyan.sysuer.api.HttpManager
import com.miyuyan.sysuer.api.TargetUrl
import okhttp3.Request
import okhttp3.Response

class XinfangModel(context: Context) : BaseModel(context) {
	override val authorizationManager: AuthorizationManager = AuthorizationManager("xinfang.sysu.edu.cn", "xinfang-443.webvpn.sysu.edu.cn").also {
		it.setTargetUrl(TargetUrl.XINFANG, TargetUrl.XINFANG_WEBVPN)
	}
	override val http: HttpManager = HttpManager(Handler(Looper.getMainLooper())).apply {
		cookieManager = CookieManager(context)
	}
	override fun handleResponse(request: CommonUtil.Tuple2<Request, Int>,
	                            response: Response): CommonUtil.Tuple2<Int, JSONObject>? {
		val content = response.body.string()
		var result: CommonUtil.Tuple2<Int, JSONObject>? = null
		println(content)
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
				else if (!authorizationManager.isAccessible(content)) retry(request)
			}
			401 -> login(request)
		}
		return result
	}
}
