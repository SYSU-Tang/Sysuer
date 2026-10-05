package com.miyuyan.sysuer.api

import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
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

	private val handler = Handler(Looper.getMainLooper())
	private var retry: Runnable? = null

	/** 客户端与 Shizuku 服务器的握手未完成时延迟重试，避免 "Not an attached client" 崩溃 */
	private fun scheduleRetry() {
		retry?.let { handler.removeCallbacks(it) }
		val r = Runnable {
			retry = null
			refresh()
		}
		retry = r
		handler.postDelayed(r, 500)
	}

	fun register() {
		Shizuku.addBinderReceivedListenerSticky(onBinderReceived)
		Shizuku.addBinderDeadListener(onBinderDead)
		Shizuku.addRequestPermissionResultListener(onPermissionResult)
	}

	fun unregister() {
		retry?.let { handler.removeCallbacks(it) }
		retry = null
		Shizuku.removeBinderReceivedListener(onBinderReceived)
		Shizuku.removeBinderDeadListener(onBinderDead)
		Shizuku.removeRequestPermissionResultListener(onPermissionResult)
	}

	fun refresh() {
		_state.value = try {
			when {
				!Shizuku.pingBinder() -> State.NOT_RUNNING
				Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED -> State.GRANTED
				else -> State.NEED_PERMISSION
			}
		} catch (e: IllegalStateException) {
			// binder 已收到但 attach 握手未完成：服务端抛 "Not an attached client"。
			// 视作未连接并延迟重试；握手完成后得到真实状态，
			// binder 失效时 pingBinder 为 false，重试链自然终止
			scheduleRetry()
			State.NOT_RUNNING
		}
	}

	/** 返回 false 表示用户曾选择"拒绝且不再询问"或 Shizuku 尚未就绪，需要引导去 Shizuku App 里手动授权 */
	fun requestPermission(): Boolean = try {
		if (Shizuku.shouldShowRequestPermissionRationale()) false
		else {
			Shizuku.requestPermission(REQUEST_CODE)
			true
		}
	} catch (e: IllegalStateException) {
		refresh()
		false
	}
}