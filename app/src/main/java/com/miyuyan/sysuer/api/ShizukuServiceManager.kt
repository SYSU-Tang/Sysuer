package com.miyuyan.sysuer.api

import android.content.ComponentName
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import com.miyuyan.sysuer.IUserService
import com.miyuyan.sysuer.UserService
import rikka.shizuku.Shizuku
import rikka.shizuku.Shizuku.bindUserService
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

object ShizukuServiceManager {

	private var service: IUserService? = null

	private val args = Shizuku.UserServiceArgs(
			ComponentName(
					"com.miyuyan.sysuer", UserService::class.java.name
			)
	).daemon(false).processNameSuffix("shellService").version(2)

	private var connectionLatch: CountDownLatch? = null

	// 2. 建立连接回调
	private val serviceConnection = object : ServiceConnection {
		override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
			if (binder != null && binder.isBinderAlive) {
				service = IUserService.Stub.asInterface(binder)
				println("Shizuku UserService 连接成功!")
			}
			connectionLatch?.countDown()
		}

		override fun onServiceDisconnected(name: ComponentName?) {
			service = null
			println("Shizuku UserService 连接断开")
		}
	}

	/**
	 * 绑定 UserService 并阻塞等待连接成功（带超时）。
	 *
	 * @return 是否已连接成功
	 */
	fun bindServiceBlocking(timeoutSeconds: Long = 15): Boolean {
		if (service != null) return true
		connectionLatch = CountDownLatch(1)
		if (service == null) {
			bindUserService(args, serviceConnection)
		}
		val connected = connectionLatch?.await(timeoutSeconds, TimeUnit.SECONDS) ?: false
		connectionLatch = null
		return connected && service != null
	}

	/**
	 * 解绑并销毁 UserService
	 */
	fun unbindService() {
		service?.let {
			try {
				it.destroy() // 触发 UserService 内部 exitProcess(0)
			} catch (e: Exception) {
				e.printStackTrace()
			}
		}
		Shizuku.unbindUserService(args, serviceConnection, true)
		service = null
	}

	/**
	 * 执行 Shell 命令
	 */
	fun runCommand(command: String): String {
		if (!bindServiceBlocking()) {
			return "Error: Shizuku UserService 连接失败"
		}
		return try {
			service?.execLine(command) ?: "Error: 执行命令失败"
		} catch (e: Exception) {
			"RPC Call Error: ${e.message}"
		}
	}

	/**
	 * 检查 Shizuku 是否可用且已授权。
	 */
	fun isShizukuAvailable(): Boolean {
		return runCatching {
			Shizuku.pingBinder() && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
		}.getOrDefault(false)
	}
}