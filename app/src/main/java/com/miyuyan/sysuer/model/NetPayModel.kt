package com.miyuyan.sysuer.model

import android.content.Context
import com.alibaba.fastjson2.JSONObject
import com.miyuyan.sysuer.api.AuthorizationManager
import com.miyuyan.sysuer.api.CookieManager
import com.miyuyan.sysuer.api.HttpManager
import com.miyuyan.sysuer.api.TargetUrl

open class NetPayModel(context: Context) : BaseModel(context) {
	override val authorizationManager: AuthorizationManager =
		AuthorizationManager("netpay.sysu.edu.cn", "netpay.sysu.edu.cn").also {
			it.setTargetUrl(TargetUrl.NETPAY, TargetUrl.NETPAY)
		}
	override val http: HttpManager = HttpManager().apply {
		cookieManager = CookieManager(context)
		header = mutableMapOf("accept-language" to "zh-CN,zh;q=0.9,en-US;q=0.8,en;q=0.7")
	}

	override fun checkResponseStatus(
		code: Int,
		content: String,
		json: JSONObject?
	): ResponseStatus {
		if (json?.getBoolean("success") == false) return ResponseStatus.NEEDS_LOGIN
		return super.checkResponseStatus(code, content, json)
	}
}
