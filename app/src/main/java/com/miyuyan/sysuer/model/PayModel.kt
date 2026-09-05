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

open class PayModel(context: Context) : BaseModel(context) {
	override val authorizationManager: AuthorizationManager = AuthorizationManager("pay.sysu.edu.cn",
	                                                                               "pay.sysu.edu.cn").also {
		it.setTargetUrl(TargetUrl.PAY, TargetUrl.PAY)
	}
	override val http: HttpManager = HttpManager(Handler(Looper.getMainLooper())).apply {
		cookieManager = CookieManager(context)
		referrer = "https://pay.sysu.edu.cn/"
		authorizationJar = AuthorizationJar(context)
		setTokenRequired(true)
	}
	
	override fun handleResponse(request: CommonUtil.Tuple2<Request, Int>,
	                            response: Response): CommonUtil.Tuple2<Int, JSONObject>? {
		val content = response.body.string()
		var result: CommonUtil.Tuple2<Int, JSONObject>? = null
		response.header("Content-Type")?.takeIf { it.contains("application/json") }?.let {
			val response = JSONObject.parse(content)
			val code = response.getInteger("code")
			if (code == 1003) login(request)
			else {
				if (code != 200) http.handler.post {
					contextUtil.toast(response.getString("message",""))
				}
				result = CommonUtil.Tuple2(request.second, response)
				message.postValue(result)
				afterLoginRequest.remove(request)
			}
		} /*?: run {
			if (!authorizationManager.isAuthorized(content)) login(request)
			else if (!authorizationManager.isAccessible(content)) retry(request)
		}*/
		return result
	}
}
