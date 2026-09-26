package com.miyuyan.sysuer.model

import android.content.Context
import com.alibaba.fastjson2.JSONObject
import com.miyuyan.sysuer.api.AuthorizationManager
import com.miyuyan.sysuer.api.TargetUrl

class XgxtModel(context: Context) : BaseModel(context) {
	override val authorizationManager: AuthorizationManager = AuthorizationManager(
			"xgxt.sysu.edu.cn", "xgxt-443.webvpn.sysu.edu.cn"
	).also {
		it.setTargetUrl(TargetUrl.XGXT, TargetUrl.XGXT_WEBVPN)
	}

	override fun checkResponseStatus(
		code: Int, content: String, json: JSONObject?
	): ResponseStatus {
		if (code == 0) return ResponseStatus.NEEDS_CAMPUS_NETWORK
		if (code == 302) return ResponseStatus.NEEDS_LOGIN
		json?.getJSONObject("meta")?.getInteger("statusCode")?.let { c ->
			if (c == 302) return ResponseStatus.NEEDS_LOGIN
		}
		return super.checkResponseStatus(code, content, json)
	}
}
