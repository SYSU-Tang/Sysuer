package com.miyuyan.sysuer.model

import android.content.Context
import com.alibaba.fastjson2.JSONObject
import com.miyuyan.sysuer.api.AuthorizationManager
import com.miyuyan.sysuer.api.CommonUtil
import com.miyuyan.sysuer.api.CookieManager
import com.miyuyan.sysuer.api.HttpManager
import com.miyuyan.sysuer.api.TargetUrl
import okhttp3.Request
import okhttp3.Response

open class JwxtModel(context: Context) : BaseModel(context) {
	override val authorizationManager: AuthorizationManager =
		AuthorizationManager("jwxt.sysu.edu.cn", "jwxt-443.webvpn.sysu.edu.cn").also {
			it.setTargetUrl(TargetUrl.JWXT, TargetUrl.JWXT_WEBVPN)
		}
	override val http: HttpManager = HttpManager().apply {
		cookieManager = CookieManager(context)
		referer = "https://jwxt.sysu.edu.cn/"
	}

	override fun handleResponse(
		request: CommonUtil.Tuple2<Request, Int>, response: Response
	): CommonUtil.Tuple2<Int, JSONObject>? {
		val content = response.body.string()
		var result: CommonUtil.Tuple2<Int, JSONObject>? = null
		response.header("Content-Type")?.takeIf { it.contains("application/json") }?.let {
			val contentJSON = JSONObject.parse(content)
			val code = contentJSON.getInteger("code")
			if (code == 53000007) login(request)
			else {
				if (code != 200) toast(contentJSON.getString("message", ""))
				result = CommonUtil.Tuple2(request.second, contentJSON)
				sendMessage(result)
			}
		} ?: run {
			if (!authorizationManager.isAuthorized(content)) login(request)
			else if (!authorizationManager.isAccessible(content)) retry(request)
		}
		return result
	}
}