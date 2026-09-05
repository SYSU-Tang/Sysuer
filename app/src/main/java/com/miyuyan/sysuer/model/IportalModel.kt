package com.miyuyan.sysuer.model

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.alibaba.fastjson2.JSONObject
import com.miyuyan.sysuer.api.AuthorizationJar
import com.miyuyan.sysuer.api.AuthorizationManager
import com.miyuyan.sysuer.api.CommonUtil
import com.miyuyan.sysuer.api.CookieManager
import com.miyuyan.sysuer.api.HttpManager
import com.miyuyan.sysuer.api.TargetUrl
import okhttp3.Request
import okhttp3.Response

class IportalModel(context: Context) : BaseModel(context) {
	override val authorizationManager: AuthorizationManager = AuthorizationManager("iportal.sysu.edu.cn", "iportal-443.webvpn.sysu.edu.cn").also {
		it.setTargetUrl(TargetUrl.NEWS, TargetUrl.NEWS_WEBVPN)
	}
	override val http: HttpManager = HttpManager(Handler(Looper.getMainLooper())).apply {
		cookieManager = CookieManager(context)
		authorizationJar = AuthorizationJar(context)
		setAuthorizationRequired(true)
		header = mutableMapOf("clientid" to "sysuer")
	}
	override fun handleResponse(request: CommonUtil.Tuple2<Request, Int>,
	                            response: Response): CommonUtil.Tuple2<Int, JSONObject>? {
		val content = response.body.string()
		var result: CommonUtil.Tuple2<Int, JSONObject>? = null
		when (response.code) {
			302 -> login(request)
			200 -> response.header("Content-Type")
				?.takeIf { it.contains("application/json") }
				?.let {
					val contentJSON = JSONObject.parse(content)
					result = CommonUtil.Tuple2(request.second, contentJSON)
					when (contentJSON.getInteger("code")) {
						10000 -> {
							message.postValue(CommonUtil.Tuple2(request.second, contentJSON))
						}
						10003,496, 497 -> {
							login(request)
						}
					}
				} ?: run {
				if (!authorizationManager.isAuthorized(content)) login(request)
				else if (!authorizationManager.isAccessible(content)) retry(request)
			}
		}
		return result
	}
}
