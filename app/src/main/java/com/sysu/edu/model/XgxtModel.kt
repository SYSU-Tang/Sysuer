package com.sysu.edu.model

import android.content.Context
import com.alibaba.fastjson2.JSONObject
import com.sysu.edu.api.AuthorizationManager
import com.sysu.edu.api.CommonUtil
import com.sysu.edu.api.TargetUrl
import okhttp3.Request
import okhttp3.Response

class XgxtModel(context: Context) : BaseModel(context) {
	override val authorizationManager: AuthorizationManager = AuthorizationManager("xgxt.sysu.edu.cn", "xgxt-443.webvpn.sysu.edu.cn").also {
		it.setTargetUrl(TargetUrl.XGXT, TargetUrl.XGXT_WEBVPN)
	}
	
	override fun handleResponse(request: CommonUtil.Tuple2<Request, Int>,
	                            response: Response): CommonUtil.Tuple2<Int, JSONObject>? {
		val content = response.body.string()
		var result: CommonUtil.Tuple2<Int, JSONObject>? = null
		when (response.code) {
			0 -> {
				authorizationManager.isAccessible = false
				retry(request)
			}
			302 -> login(request)
			200 -> response.header("Content-Type")
				?.takeIf { it.contains("application/json") }
				?.let {
					val data = JSONObject.parse(content)
					result = CommonUtil.Tuple2(request.second, data)
					val meta: JSONObject? = if (data.containsKey("meta")) data.getJSONObject("meta") else null
					meta?.let {
						if (meta.containsKey("statusCode") && meta.getInteger("statusCode") == 302) login(request)
						else http.handler.post {
							contextUtil.toast(CommonUtil.toStringOrDefault(meta.getString("message", "")))
						}
					} ?: run {
						if (data.containsKey("code") && data.getInteger("code") != 200) http.handler.post {
							contextUtil.toast(data.getString("msg", ""))
						}
						message.postValue(result)
						afterLoginRequest.remove(request)
					}
				} ?: run {
				if (!authorizationManager.isAuthorized(content)) login(request)
				else if (!authorizationManager.isAccessible(content)) retry(request)
			}
		}
		return result
	}
}
