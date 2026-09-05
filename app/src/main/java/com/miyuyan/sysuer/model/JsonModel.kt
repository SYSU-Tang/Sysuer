package com.miyuyan.sysuer.model

import android.content.Context
import com.alibaba.fastjson2.JSON
import com.alibaba.fastjson2.JSONArray
import com.alibaba.fastjson2.JSONObject
import com.miyuyan.sysuer.api.AuthorizationManager
import com.miyuyan.sysuer.api.CommonUtil
import okhttp3.Request
import okhttp3.Response

class JsonModel(context: Context) : BaseModel(context) {
	override val authorizationManager: AuthorizationManager = AuthorizationManager("", "")
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
					data?.takeIf { it.containsKey("code") && it.getInteger("code") != 200 }?.let {
						login(request)
					}
					message.postValue(result)
					afterLoginRequest.remove(request)
				} ?: run {
				if (!authorizationManager.isAuthorized(content)) login(request)
				else if (!authorizationManager.isAccessible(content)) retry(request)
			}
		}
		return result
	}
}
