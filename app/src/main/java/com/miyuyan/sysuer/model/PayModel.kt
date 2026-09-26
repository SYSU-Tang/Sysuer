package com.miyuyan.sysuer.model

import android.content.Context
import com.alibaba.fastjson2.JSONObject
import com.miyuyan.sysuer.api.AuthorizationJar
import com.miyuyan.sysuer.api.AuthorizationManager
import com.miyuyan.sysuer.api.CookieManager
import com.miyuyan.sysuer.api.HttpManager
import com.miyuyan.sysuer.api.TargetUrl

open class PayModel(context: Context) : BaseModel(context) {
	override val authorizationManager: AuthorizationManager = AuthorizationManager(
		"pay.sysu.edu.cn", "pay.sysu.edu.cn"
	).also {
		it.setTargetUrl(TargetUrl.PAY, TargetUrl.PAY)
	}
	override val http: HttpManager = HttpManager().apply {
		cookieManager = CookieManager(context)
		referer = "https://pay.sysu.edu.cn/"
		authorizationJar = AuthorizationJar(context)
		isTokenRequired = true
	}

	override fun checkResponseStatus(
		code: Int,
		content: String,
		json: JSONObject?
	): ResponseStatus {
		if (json?.getInteger("code") == 1003) return ResponseStatus.NEEDS_LOGIN
		return super.checkResponseStatus(code, content, json)
	}
}
