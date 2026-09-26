package com.miyuyan.sysuer.model

import android.content.Context
import com.alibaba.fastjson2.JSONObject
import com.miyuyan.sysuer.api.AuthorizationManager

class RainClassModel(context: Context) : BaseModel(context) {
	override val authorizationManager: AuthorizationManager =
		AuthorizationManager("www.yuketang.cn", "www.yuketang.cn")

	override fun checkResponseStatus(
		code: Int,
		content: String,
		json: JSONObject?
	): ResponseStatus {
		if (code == 401) return ResponseStatus.NEEDS_LOGIN
		return super.checkResponseStatus(code, content, json)
	}
}
