package com.miyuyan.sysuer.model

import android.content.Context
import com.alibaba.fastjson2.JSONObject
import com.miyuyan.sysuer.api.AuthorizationJar
import com.miyuyan.sysuer.api.AuthorizationManager
import com.miyuyan.sysuer.api.CookieManager
import com.miyuyan.sysuer.api.HttpManager
import com.miyuyan.sysuer.api.TargetUrl

class ZhnyModel(context: Context) : BaseModel(context) {
	override val authorizationManager: AuthorizationManager = AuthorizationManager(
		"zhny.sysu.edu.cn", "zhny.sysu.edu.cn"
	).also {
		it.setTargetUrl(TargetUrl.ZHNY, TargetUrl.ZHNY)
	}
	override val http: HttpManager = HttpManager().apply {
		cookieManager = CookieManager(context)
		isAuthorizationRequired = true
		authorizationJar = AuthorizationJar(context)
	}

	override fun checkResponseStatus(
		code: Int,
		content: String,
		json: JSONObject?
	): ResponseStatus {
		json?.getInteger("code")?.takeIf { it != 200 }?.let {
			return ResponseStatus.NEEDS_LOGIN
		}
		return super.checkResponseStatus(code, content, json)
	}
}
