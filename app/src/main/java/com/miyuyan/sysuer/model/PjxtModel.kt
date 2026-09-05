package com.miyuyan.sysuer.model

import android.content.Context
import com.alibaba.fastjson2.JSONObject
import com.miyuyan.sysuer.R
import com.miyuyan.sysuer.api.AuthorizationManager
import com.miyuyan.sysuer.api.CommonUtil
import com.miyuyan.sysuer.api.TargetUrl
import okhttp3.Request
import okhttp3.Response
import java.io.IOException

class PjxtModel(context: Context) : BaseModel(context) {
	override val authorizationManager: AuthorizationManager =
		AuthorizationManager("pjxt.sysu.edu.cn", "pjxt-443.webvpn.sysu.edu.cn").also {
			it.setTargetUrl(TargetUrl.PJXT, TargetUrl.PJXT_WEBVPN)
		}
	
	override fun handleFailure(request: CommonUtil.Tuple2<Request, Int>, e: IOException) {
		e.message?.takeIf {
			it.endsWith("21")
		}?.let {
			login(request)
		} ?: http.handler.post { contextUtil.toast(R.string.no_net_connected) }
	}
	
	override fun handleResponse(request: CommonUtil.Tuple2<Request, Int>, response: Response): CommonUtil.Tuple2<Int, JSONObject>? {
		val content = response.body.string()
		var result: CommonUtil.Tuple2<Int, JSONObject>? = null
		when (response.code) {
			403 -> {
				authorizationManager.isAccessible = false
				retry(request)
			}
			0 -> login(request)
			200 ->
				response.header("Content-Type")
					?.takeIf { it.contains("application/json") }
					?.let {
						val data =
							JSONObject.parse(content)
						result = CommonUtil.Tuple2(request.second, data)
						data?.takeIf { it.containsKey("code") && it.getString("code") != "200" }?.let {
							http.handler.post {
								contextUtil.toast(CommonUtil.toStringOrDefault(data.getString("msg")))
							}
						}
						message.postValue(CommonUtil.Tuple2(request.second, data))
						afterLoginRequest.remove(request)
					} ?: run {
					if (!authorizationManager.isAuthorized(content)) login(request)
					else if (!authorizationManager.isAccessible(content)) retry(request)
				}
		}
		return result
	}
}
