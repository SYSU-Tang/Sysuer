package com.miyuyan.sysuer.api

import android.content.pm.PackageManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import rikka.shizuku.Shizuku

object ShizukuHelper {
	enum class State { NOT_RUNNING, NEED_PERMISSION, GRANTED }

	private const val REQUEST_CODE = 1001
	private val _state = MutableStateFlow(State.NOT_RUNNING)
	val state = _state.asStateFlow()

	private val onBinderReceived = Shizuku.OnBinderReceivedListener { refresh() }
	private val onBinderDead = Shizuku.OnBinderDeadListener {
		ShortcutReader.reset()
		refresh()
	}
	private val onPermissionResult = Shizuku.OnRequestPermissionResultListener { _, _ -> refresh() }

	fun register() {
		Shizuku.addBinderReceivedListenerSticky(onBinderReceived)
		Shizuku.addBinderDeadListener(onBinderDead)
		Shizuku.addRequestPermissionResultListener(onPermissionResult)
	}

	fun unregister() {
		Shizuku.removeBinderReceivedListener(onBinderReceived)
		Shizuku.removeBinderDeadListener(onBinderDead)
		Shizuku.removeRequestPermissionResultListener(onPermissionResult)
	}

	fun refresh() {
		_state.value = when {
			!Shizuku.pingBinder() -> State.NOT_RUNNING
			Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED -> State.GRANTED
			else -> State.NEED_PERMISSION
		}
	}

	/** 返回 false 表示用户曾选择"拒绝且不再询问"，需要引导去 Shizuku App 里手动授权 */
	fun requestPermission(): Boolean {
		if (Shizuku.shouldShowRequestPermissionRationale()) return false
		Shizuku.requestPermission(REQUEST_CODE)
		return true
	}
}