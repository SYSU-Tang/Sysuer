package com.miyuyan.sysuer.model

import android.content.Context
import com.alibaba.fastjson2.JSONObject
import com.miyuyan.sysuer.R
import com.miyuyan.sysuer.api.AuthorizationManager
import com.miyuyan.sysuer.api.TargetUrl
import okhttp3.Request
import java.io.IOException

class PjxtModel(context: Context) : BaseModel(context) {
	override val authorizationManager: AuthorizationManager =
		AuthorizationManager("pjxt.sysu.edu.cn", "pjxt-443.webvpn.sysu.edu.cn").also {
			it.setTargetUrl(TargetUrl.PJXT, TargetUrl.PJXT_WEBVPN)
		}

	override fun handleFailure(request: Pair<Request, Int>, e: IOException) {
		if (e.message?.endsWith("21") == true) {
			login(request)
		} else {
			toast(R.string.no_net_connected)
		}
	}

	override fun checkResponseStatus(
		code: Int, content: String, json: JSONObject?
	): ResponseStatus {
		if (code == 403) return ResponseStatus.NEEDS_CAMPUS_NETWORK
		if (code == 0) return ResponseStatus.NEEDS_LOGIN
		return super.checkResponseStatus(code, content, json)
	}
}
