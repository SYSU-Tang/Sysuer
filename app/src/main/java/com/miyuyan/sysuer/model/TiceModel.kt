package com.miyuyan.sysuer.model

import android.app.Application
import com.miyuyan.sysuer.api.AuthorizationManager
import com.miyuyan.sysuer.api.TargetUrl


class TiceModel(context: Application) : BaseModel(context) {
	override val authorizationManager: AuthorizationManager =
		AuthorizationManager("tice.sysu.edu.cn", "tice-443.webvpn.sysu.edu.cn").also {
			it.setTargetUrl(TargetUrl.TICE, TargetUrl.TICE_WEBVPN)
		}
}