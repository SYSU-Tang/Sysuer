package com.sysu.edu.model

import android.content.Context
import com.alibaba.fastjson2.JSONObject
import com.sysu.edu.api.AuthorizationManager
import com.sysu.edu.api.CommonUtil
import okhttp3.Request
import okhttp3.Response

class PortalModel(context: Context) : BaseModel(context) {
	override val authorizationManager: AuthorizationManager =
		AuthorizationManager("portal.sysu.edu.cn", "portal.sysu.edu.cn")
	
	override fun handleResponse(request: CommonUtil.Tuple2<Request, Int>, response: Response): CommonUtil.Tuple2<Int, JSONObject>? {
		val content = response.body.string()
		val code = response.code
		var result: CommonUtil.Tuple2<Int, JSONObject>? = null
		when (code) {
			302 -> login(request)
			200 ->
				response.header("Content-Type")
					?.takeIf { it.contains("application/json") }
					?.let {
						val contentJSON = JSONObject.parse(content)
						result = CommonUtil.Tuple2<Int, JSONObject>(request.getSecond(), contentJSON)
						val meta = contentJSON.getJSONObject("meta")
						if (!meta.getBoolean("success")) {
							if (meta.getInteger("statusCode") == 302) login(request)
							else http.handler.post {
								contextUtil.toast(
									CommonUtil.toStringOrDefault(meta.getString("message"))
								)
							}
						} else {
							if (meta.getInteger("statusCode") != 200)
								http.handler.post {
									contextUtil.toast(
										CommonUtil.toStringOrDefault(
											meta.getString("message")
										)
									)
								}
							message.postValue(
								CommonUtil.Tuple2<Int, JSONObject>(
									request.getSecond(),
									contentJSON
								)
							)
							afterLoginRequest.remove(request)
						}
					} ?: run {
					if (!authorizationManager.isAuthorized(content)) login(request)
				}
		}
		return result
	}
}
