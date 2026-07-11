package com.sysu.edu.model

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.alibaba.fastjson2.JSONObject
import com.sysu.edu.R
import com.sysu.edu.api.AuthorizationJar
import com.sysu.edu.api.AuthorizationManager
import com.sysu.edu.api.CommonUtil
import com.sysu.edu.api.CookieManager
import com.sysu.edu.api.HttpManager
import com.sysu.edu.api.TargetUrl
import okhttp3.Request
import okhttp3.Response
import java.io.IOException

class ZhnyModel(context: Context) : BaseModel(context) {
	override val authorizationManager: AuthorizationManager = AuthorizationManager("zhny.sysu.edu.cn", "zhny.sysu.edu.cn").also {
		it.setTargetUrl(TargetUrl.ZHNY, TargetUrl.ZHNY)
	}
	override val http: HttpManager = HttpManager(Handler(Looper.getMainLooper())).apply {
		cookieManager = CookieManager(context)
		setAuthorizationRequired(true)
		authorizationJar = AuthorizationJar(context)
	}
	override fun handleFailure(request: CommonUtil.Tuple2<Request, Int>, e: IOException) {
		http.handler.post { contextUtil.toast(R.string.no_net_connected) }
	}
	
	override fun handleResponse(request: CommonUtil.Tuple2<Request, Int>,
	                            response: Response): CommonUtil.Tuple2<Int, JSONObject>? {
		val content = response.body.string()
		var result: CommonUtil.Tuple2<Int, JSONObject>? = null
		when (response.code) {
			200 -> response.header("Content-Type")
				?.takeIf { it.contains("application/json") }
				?.let {
					val data = JSONObject.parse(content)
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
