package com.miyuyan.sysuer.model

import android.content.Context
import com.alibaba.fastjson2.JSONObject
import com.miyuyan.sysuer.api.AuthorizationManager
import com.miyuyan.sysuer.api.CookieManager
import com.miyuyan.sysuer.api.HttpManager
import com.miyuyan.sysuer.api.TargetUrl

open class JwxtModel(context: Context) : BaseModel(context) {
	override val authorizationManager: AuthorizationManager =
		AuthorizationManager("jwxt.sysu.edu.cn", "jwxt-443.webvpn.sysu.edu.cn").also {
			it.setTargetUrl(TargetUrl.JWXT, TargetUrl.JWXT_WEBVPN)
		}
	override val http: HttpManager = HttpManager().apply {
		cookieManager = CookieManager(context)
		referer = "https://jwxt.sysu.edu.cn/"
	}

	override fun checkResponseStatus(
		code: Int,
		content: String,
		json: JSONObject?
	): ResponseStatus {
		if (json?.getInteger("code") == 53000007) return ResponseStatus.NEEDS_LOGIN
		return super.checkResponseStatus(code, content, json)
	}
}
