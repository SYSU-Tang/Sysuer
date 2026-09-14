package com.miyuyan.sysuer.home

import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import com.miyuyan.sysuer.R
import com.miyuyan.sysuer.api.ContextUtil
import com.miyuyan.sysuer.api.SettingManager
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

class HomeViewModel : ViewModel() {
	private val _updateDashboardShortcut = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
	val updateDashboardShortcut: SharedFlow<Unit> = _updateDashboardShortcut

	fun triggerUpdateDashboardShortcut() {
		_updateDashboardShortcut.tryEmit(Unit)
	}

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