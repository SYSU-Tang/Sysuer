package com.miyuyan.sysuer.model

import android.content.Context
import com.alibaba.fastjson2.JSONObject
import com.miyuyan.sysuer.api.AuthorizationJar
import com.miyuyan.sysuer.api.AuthorizationManager
import com.miyuyan.sysuer.api.CookieManager
import com.miyuyan.sysuer.api.HttpManager
import com.miyuyan.sysuer.api.TargetUrl

class IportalModel(context: Context) : BaseModel(context) {
	override val authorizationManager: AuthorizationManager = AuthorizationManager(
		"iportal.sysu.edu.cn", "iportal-443.webvpn.sysu.edu.cn"
	).also {
		it.setTargetUrl(TargetUrl.NEWS, TargetUrl.NEWS_WEBVPN)
	}
	override val http: HttpManager = HttpManager().apply {
		cookieManager = CookieManager(context)
		authorizationJar = AuthorizationJar(context)
		isAuthorizationRequired = true
		header = mutableMapOf("clientid" to "sysuer")
	}

	override fun checkResponseStatus(
		code: Int,
		content: String,
		json: JSONObject?
	): ResponseStatus {
		if (code == 302) return ResponseStatus.NEEDS_LOGIN
		json?.getInteger("code")?.let { c ->
			if (c in listOf(10003, 496, 497)) return ResponseStatus.NEEDS_LOGIN
		}
		return super.checkResponseStatus(code, content, json)
	}
}
