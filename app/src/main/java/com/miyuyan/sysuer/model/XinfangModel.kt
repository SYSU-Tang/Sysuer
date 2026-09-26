package com.miyuyan.sysuer.model

import android.content.Context
import com.alibaba.fastjson2.JSONObject
import com.miyuyan.sysuer.api.AuthorizationManager
import com.miyuyan.sysuer.api.CookieManager
import com.miyuyan.sysuer.api.HttpManager
import com.miyuyan.sysuer.api.TargetUrl

class XinfangModel(context: Context) : BaseModel(context) {
	override val authorizationManager: AuthorizationManager = AuthorizationManager(
		"xinfang.sysu.edu.cn", "xinfang-443.webvpn.sysu.edu.cn"
	).also {
		it.setTargetUrl(TargetUrl.XINFANG, TargetUrl.XINFANG_WEBVPN)
	}
	override val http: HttpManager = HttpManager().apply {
		cookieManager = CookieManager(context)
	}

	override fun checkResponseStatus(
		code: Int,
		content: String,
		json: JSONObject?
	): ResponseStatus {
		if (code == 401) return ResponseStatus.NEEDS_LOGIN
		return super.checkResponseStatus(code, content, json)
	}
}
