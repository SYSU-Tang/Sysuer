package com.miyuyan.sysuer.model

import android.content.Context
import com.alibaba.fastjson2.JSONObject
import com.miyuyan.sysuer.api.AuthorizationJar
import com.miyuyan.sysuer.api.AuthorizationManager
import com.miyuyan.sysuer.api.CookieManager
import com.miyuyan.sysuer.api.HttpManager
import com.miyuyan.sysuer.api.TargetUrl
import java.util.regex.Pattern

class GymModel(context: Context) : BaseModel(context) {
	override val authorizationManager: AuthorizationManager = AuthorizationManager(
		"gym.sysu.edu.cn", "gym-443.webvpn.sysu.edu.cn"
	).also {
		it.setTargetUrl(TargetUrl.GYM, TargetUrl.GYM_WEBVPN)
	}
	override val http: HttpManager = HttpManager().apply {
		cookieManager = CookieManager(context)
		ua = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36 Edg/143.0.0.0"
		isAuthorizationRequired = true
		authorizationJar = AuthorizationJar(context)
		header = mutableMapOf("Accept" to "application/json, text/plain, */*")
	}

	override fun checkResponseStatus(
		code: Int,
		content: String,
		json: JSONObject?
	): ResponseStatus {
		if (code == 401) return ResponseStatus.NEEDS_LOGIN
		if (json == null && Pattern.compile("人机识别检测").matcher(content).find()) {
			return ResponseStatus.NEEDS_LOGIN
		}
		return super.checkResponseStatus(code, content, json)
	}
}
