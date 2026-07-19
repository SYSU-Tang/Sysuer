package com.sysu.edu.model

import android.content.Context
import com.alibaba.fastjson2.JSONObject
import com.sysu.edu.api.AuthorizationManager
import com.sysu.edu.api.CommonUtil
import com.sysu.edu.api.TargetUrl
import okhttp3.Request
import okhttp3.Response

class PortalModel(context: Context) : BaseModel(context) {
	override val authorizationManager: AuthorizationManager = AuthorizationManager("portal.sysu.edu.cn",
	                                                                               "portal.sysu.edu.cn").also {
		it.setTargetUrl(TargetUrl.PORTAL, TargetUrl.PORTAL)
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
					val meta = contentJSON.getJSONObject("meta")
					if (!meta.getBoolean("success")) {
						if (meta.getInteger("statusCode") == 302) login(request)
						else http.handler.post {
							contextUtil.toast(meta.getString("message",""))
						}
					}
					else {
						if (meta.getInteger("statusCode") != 200) http.handler.post {
							contextUtil.toast(meta.getString("message",""))
						}
						message.postValue(CommonUtil.Tuple2(request.second, contentJSON))
						afterLoginRequest.remove(request)
					}
				} ?: run {
				if (!authorizationManager.isAuthorized(content)) login(request)
			}
		}
		return result
	}
}
