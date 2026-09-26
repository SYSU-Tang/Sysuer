package com.miyuyan.sysuer.model

import android.content.Context
import com.alibaba.fastjson2.JSONObject
import com.miyuyan.sysuer.api.AuthorizationManager
import com.miyuyan.sysuer.api.TargetUrl

class PortalModel(context: Context) : BaseModel(context) {
	override val authorizationManager: AuthorizationManager = AuthorizationManager(
			"portal.sysu.edu.cn", "portal.sysu.edu.cn"
	).also {
		it.setTargetUrl(TargetUrl.PORTAL, TargetUrl.PORTAL)
	}

	override fun checkResponseStatus(
		code: Int, content: String, json: JSONObject?
	): ResponseStatus {
		if (code == 302) return ResponseStatus.NEEDS_LOGIN
		json?.getJSONObject("meta")?.let { meta ->
			if (meta.getBoolean("success") == false && meta.getInteger("statusCode") == 302) {
				return ResponseStatus.NEEDS_LOGIN
			}
		}
		return super.checkResponseStatus(code, content, json)
	}
}
