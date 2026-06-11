package com.sysu.edu.model

import android.content.Context
import com.alibaba.fastjson2.JSONObject
import com.sysu.edu.R
import com.sysu.edu.api.AuthorizationManager
import com.sysu.edu.api.CommonUtil
import com.sysu.edu.api.TargetUrl
import okhttp3.Request
import okhttp3.Response
import java.io.IOException

class ZhnyModel(context: Context) : BaseModel(context) {
	override val authorizationManager: AuthorizationManager =
		AuthorizationManager("zhny.sysu.edu.cn", "zhny-443.webvpn.sysu.edu.cn").also {
			it.setTargetUrl(TargetUrl.ZHNY, TargetUrl.ZHNY)
		}
	
	override fun handleFailure(request: CommonUtil.Tuple2<Request, Int>, e: IOException) {
		http.handler.post { contextUtil.toast(R.string.no_net_connected) }
	}
	
	override fun handleResponse(request: CommonUtil.Tuple2<Request, Int>, response: Response): CommonUtil.Tuple2<Int, JSONObject>? {
		val content = response.body.string()
		var result: CommonUtil.Tuple2<Int, JSONObject>? = null
		when (response.code) {
			200 ->
				response.header("Content-Type")
					?.takeIf { it.contains("application/json") }
					?.let {
						val data =
							JSONObject.parse(content)
						result = CommonUtil.Tuple2<Int, JSONObject>(request.second, data)
						data?.takeIf { it.containsKey("code") && it.getString("code") != "200" }?.let {
//							http.handler.post {
//								contextUtil.toast(CommonUtil.toStringOrDefault(data.getString("msg")))
//							}
							login(request)
						}
						message.postValue(CommonUtil.Tuple2<Int, JSONObject>(request.second, data))
						afterLoginRequest.remove(request)
					} ?: run {
					if (!authorizationManager.isAuthorized(content)) login(request)
					else if (!authorizationManager.isAccessible(content)) retry(request)
				}
		}
		return result
	}
}
