package com.miyuyan.sysuer

import java.io.BufferedReader
import java.io.InputStreamReader
import kotlin.system.exitProcess

class UserService : IUserService.Stub() {

	override fun destroy() {
		exitProcess(0)
	}

	override fun exit() {
		try {
			exitProcess(0)
		} catch (e: Exception) {
			e.printStackTrace()
		}
	}

	override fun execLine(command: String?): String? {
		if (command.isNullOrEmpty()) return null
		val cmdArr = arrayOf("sh", "-c", command)
		return executeProcess(cmdArr)
	}

	override fun execArr(command: Array<out String?>?): String? {
		if (command.isNullOrEmpty()) return null
		val filteredCmd = command.filterNotNull().toTypedArray()
		if (filteredCmd.isEmpty()) return null

		return executeProcess(filteredCmd)
	}

	/**
	 * 通用进程执行辅助方法
	 * 将标准输出与错误输出同时捕获并返回
	 */
	private fun executeProcess(cmdArray: Array<String>): String {
		val resultBuilder = StringBuilder()
		var process: Process? = null

		try {
			val processBuilder = ProcessBuilder(*cmdArray)
			processBuilder.redirectErrorStream(true)
			process = processBuilder.start()
			BufferedReader(InputStreamReader(process.inputStream, Charsets.UTF_8)).use { reader ->
				var line: String?
				while (reader.readLine().also { line = it } != null) {
					resultBuilder.append(line).append("\n")
				}
			}

			process.waitFor()

		} catch (e: Exception) {
			resultBuilder.append("Execution error: ").append(e.localizedMessage)
		} finally {
			process?.destroy()
		}

		return resultBuilder.toString().trim()
	}

//	private val context: Context by lazy {
//		val activityThreadClass = Class.forName("android.app.ActivityThread")
//		val systemMainMethod = activityThreadClass.getMethod("systemMain")
//		val activityThread = systemMainMethod.invoke(null)
//		val getSystemContextMethod = activityThreadClass.getMethod("getSystemContext")
//		getSystemContextMethod.invoke(activityThread) as Context
//	}

}