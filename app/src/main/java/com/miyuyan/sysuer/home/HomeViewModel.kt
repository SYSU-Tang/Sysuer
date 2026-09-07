package com.miyuyan.sysuer.home

import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.miyuyan.sysuer.R
import com.miyuyan.sysuer.api.ContextUtil
import com.miyuyan.sysuer.api.SettingManager

class HomeViewModel : ViewModel() {
	val updateDashboardShortcut: MutableLiveData<Boolean?> = MutableLiveData<Boolean?>()
	val actionMap = mutableMapOf<Int, (Context) -> Unit>(302 to { context ->
		context.packageManager.getLaunchIntentForPackage("com.comingx.zanao")?.let { intent ->
			context.startActivity(intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
		} ?: ContextUtil.getInstance(context).toast(R.string.no_app)
	}, 604 to { context ->
		SettingManager.getInstance(context).qrCode.takeIf { qrcode -> qrcode.isNotEmpty() }
			?.let { qrcode ->
				context.startActivity(Intent(Intent.ACTION_VIEW, qrcode.toUri()))
			} ?: ContextUtil.getInstance(context).toast(R.string.no_app)
	}, 602 to { context ->
		context.packageManager.getLaunchIntentForPackage("com.tencent.wework")?.let {
			context.startActivity(it.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
		} ?: ContextUtil.getInstance(context).toast(R.string.no_app)
	})
}
