package com.miyuyan.sysuer

import android.app.ActivityManager
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import android.os.Bundle
import android.os.Environment
import android.os.StatFs
import androidx.core.content.pm.PackageInfoCompat
import androidx.core.net.toUri
import com.google.firebase.installations.BuildConfig
import com.miyuyan.sysuer.api.DateTimeManager
import com.miyuyan.sysuer.databinding.ActivityCrashBinding
import java.time.LocalDateTime
import java.util.Locale
import java.util.TimeZone

class CrashActivity : BaseActivity() {
	val crashInfo: String by lazy { intent.getStringExtra("crash") ?: "" }
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		val binding = ActivityCrashBinding.inflate(layoutInflater).apply {
			toolbar.setNavigationOnClickListener { supportFinishAfterTransition() }
			copy.setOnClickListener {
				config.copy("crash", crashInfo)
				config.toast(R.string.copy_successfully)
			}
			submit.setOnClickListener {
				openIssueInBrowser()
			}
			restart.setOnClickListener {
				packageManager.getLaunchIntentForPackage(packageName)?.also {
					startActivity(it.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK))
				}
				supportFinishAfterTransition()
			}
		}
		setContentView(binding.root)
		binding.crashContent.setMarkdown(createDetailedIssueBody(RuntimeException(crashInfo)))
	}

	fun openIssueInBrowser() {
		try {
			val timestamp = DateTimeManager.toDateTimeString(LocalDateTime.now())
			var exceptionType = "Unknown Exception"
			if (!crashInfo.isEmpty()) {
				val lines =
					crashInfo.split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
				if (lines.isNotEmpty()) {
					val firstLine = lines[0]
					if (firstLine.contains(":")) exceptionType =
						firstLine.split(":".toRegex()).dropLastWhile { it.isEmpty() }
							.toTypedArray()[0]
				}
			}
			val title = "[崩溃报告] $exceptionType - $timestamp"

			config.copy("crash_issue", crashInfo)
			config.toast(R.string.copy_successfully)

			startActivity(
					Intent(Intent.ACTION_VIEW)
						.setData("https://github.com/SYSU-Tang/Sysuer/issues/new?title=$title&labels=bug,crash-report".toUri())
						.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
			)
		} catch (_: Exception) {
		}
	}

	val availableMemory: String
		get() {
			val memoryInfo = ActivityManager.MemoryInfo()
			getSystemService(ActivityManager::class.java).getMemoryInfo(memoryInfo)
			return String.format(
					Locale.getDefault(),
					"%.2f MB / %.2f MB",
					(memoryInfo.availMem / (1024.0 * 1024.0)),
					(memoryInfo.totalMem / (1024.0 * 1024.0))
			)
		}
	val storageInfo: String
		get() = try {
			val statFs = StatFs(Environment.getDataDirectory().path)
			String.format(
					Locale.getDefault(),
					"%.2f GB / %.2f GB",
					(statFs.availableBlocksLong * statFs.blockSizeLong / (1024.0 * 1024.0 * 1024.0)),
					(statFs.blockCountLong * statFs.blockSizeLong / (1024.0 * 1024.0 * 1024.0))
			)
		} catch (_: Exception) {
			getString(R.string.unknown)
		}


	/**
	 * 生成更详细的Markdown格式Issue内容
	 */
	fun createDetailedIssueBody(throwable: Throwable): String = buildString {
		append("## 📝 用户描述\n")
		append("请简单描述崩溃发生时的场景和操作步骤。").append("\n\n")        // 应用信息
		append("## 📱 应用信息\n")
		append("| 项目 | 值 |\n")
		append("|------|-----|\n")
		try {
			val packageInfo = packageManager.getPackageInfo(packageName, 0)
			append("| 应用版本 | ").append(packageInfo.versionName).append(" (")
				.append(PackageInfoCompat.getLongVersionCode(packageInfo)).append(") |\n")
			append("| 包名 | ").append(packageName).append(" |\n")
		} catch (e: PackageManager.NameNotFoundException) {
			e.printStackTrace()
		}
		append("| 构建类型 | ").append(BuildConfig.BUILD_TYPE).append(" |\n\n")        // 设备信息表格
		append("## 📱 设备信息\n")
		append("| 项目 | 值 |\n")
		append("|------|-----|\n")
		append("| 设备型号 | ").append(Build.MANUFACTURER).append(" ").append(Build.MODEL)
			.append(" |\n")
		append("| Android版本 | ").append(Build.VERSION.RELEASE).append(" (API ").append(SDK_INT)
			.append(") |\n")        // 屏幕信息

		append("| 屏幕分辨率 | ").append(config.width).append("×").append(config.height)
			.append(" |\n")
		append("| 屏幕密度 | ").append(resources.displayMetrics.densityDpi).append("dpi |\n")
		append("| 时区 | ").append(TimeZone.getDefault().id).append(" |\n")
		append("| 语言 | ").append(Locale.getDefault().language).append(" |\n\n")        // 崩溃详情
		append("## 💥 崩溃详情\n")
		append("**异常类型**: `").append(throwable.javaClass.getSimpleName()).append("`\n\n")
		append("**异常消息**: \n```txt\n").append(throwable.message ?: "无消息")
			.append("\n```\n\n")        // 复现步骤
		append("## 🔄 复现步骤\n")
		append("1. [请描述如何复现这个问题]\n")
		append("2. \n")
		append("3. \n\n")        // 期望行为与实际行为
		append("## ✅ 期望行为\n")
		append("[描述期望发生的行为]\n\n")

		append("## ❌ 实际行为\n")
		append("[描述实际发生的行为]\n\n")        // 设备状态信息
		append("## 📊 设备状态\n")
		append("- **可用内存**: ").append(availableMemory).append("\n")
		append("- **存储空间**: ").append(storageInfo).append("\n")
		append("- **网络状态**: ").append(networkStatus).append("\n")
		append("- **电池状态**: ").append(batteryStatus).append("\n\n")        // 崩溃时间
		append("## ⏰ 崩溃时间\n")
		append(
				DateTimeManager.toDateTimeString(LocalDateTime.now())
		).append("\n\n")
	}

	private val networkStatus: String
		get() {
			val list = mutableListOf<String>()
			val cm = getSystemService(ConnectivityManager::class.java)
			val networkCapabilities: NetworkCapabilities? =
				cm.getNetworkCapabilities(cm.activeNetwork)
			networkCapabilities?.let {
				if (it.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) list.add("WiFi")
				if (it.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) list.add("Mobile")
			}
			return if (list.isEmpty()) "No Network" else list.joinToString("|")
		}
	private val batteryStatus: String
		get() {
			val batteryStatus = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
			return if (batteryStatus != null) {
				val level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
				val scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
				val batteryPct = level * 100 / scale.toFloat()
				val status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
				val isCharging =
					status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
				String.format(
						Locale.getDefault(),
						"%.1f%% (%s)",
						batteryPct,
						getString(if (isCharging) R.string.charging else R.string.uncharge)
				)
			} else getString(R.string.unknown)
		}
}