package com.sysu.edu

import android.app.ActivityManager
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.Point
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import android.os.Bundle
import android.os.Environment
import android.os.StatFs
import android.view.View
import androidx.core.content.pm.PackageInfoCompat
import androidx.core.net.toUri
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.google.firebase.installations.BuildConfig
import com.sysu.edu.api.Params
import com.sysu.edu.databinding.ActivityCrashBinding
import io.noties.markwon.Markwon
import io.noties.markwon.ext.tables.TablePlugin
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.TimeZone

class CrashActivity : BaseActivity() {
	val crash: MutableLiveData<String?> = MutableLiveData<String?>()
	var binding: ActivityCrashBinding? = null
	var crashInfo: String? = null
	var params: Params? = null
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		binding = ActivityCrashBinding.inflate(layoutInflater)
		setContentView(binding!!.getRoot())
		params = Params(this)
		binding!!.toolbar.setNavigationOnClickListener { _: View? -> supportFinishAfterTransition() }
		binding!!.copy.setOnClickListener { _: View? ->
			params!!.copy("crash", crash.getValue())
			params!!.toast(R.string.copy_successfully)
		}
		binding!!.submit.setOnClickListener { _: View? ->
			openIssueInBrowser()
		}
		
		crash.observe(this, Observer { s: String? -> Markwon.builder(this).usePlugin(TablePlugin.create(this)).build().setMarkdown(binding!!.crashContent, s!!) })
		crashInfo = intent.getStringExtra("crash")
		if (crashInfo != null) crash.value = createDetailedIssueBody(RuntimeException(crashInfo))
		binding!!.restart.setOnClickListener { _: View? ->
			packageManager.getLaunchIntentForPackage(packageName)
				?.also {
					it.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
					startActivity(it)
				}
			supportFinishAfterTransition()
		}
	}
	
	fun openIssueInBrowser() {
		try {
			val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
			var exceptionType = "Unknown Exception"
			if (crashInfo != null && !crashInfo!!.isEmpty()) {
				val lines = crashInfo!!.split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
				if (lines.isNotEmpty()) {
					val firstLine = lines[0]
					if (firstLine.contains(":")) exceptionType = firstLine.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[0]
				}
			}
			val title = "[崩溃报告] $exceptionType - $timestamp"
			val githubUrl = generateGitHubWebIssueUrl(title)
			
			params!!.copy("crash_issue", crash.getValue())
			params!!.toast(R.string.copy_successfully)
			
			startActivity(Intent(Intent.ACTION_VIEW).setData(githubUrl.toUri()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
		} catch (_: Exception) {
		}
	}
	
	fun generateGitHubWebIssueUrl(title: String?): String {
		return "https://github.com/SYSU-Tang/Sysuer/issues/new?title=$title&labels=bug,crash-report"
	}
	
	val availableMemory: String
		get() {
			val activityManager = getSystemService(ActivityManager::class.java)
			val memoryInfo = ActivityManager.MemoryInfo()
			activityManager.getMemoryInfo(memoryInfo)
			return String.format(Locale.getDefault(),
			                     "%.2f MB / %.2f MB",
			                     (memoryInfo.availMem / (1024.0 * 1024.0)),
			                     (memoryInfo.totalMem / (1024.0 * 1024.0)))
		}
	val storageInfo: String
		get() {
			try {
				val statFs = StatFs(Environment.getDataDirectory().path)
				return String.format(Locale.getDefault(),
				                     "%.2f GB / %.2f GB",
				                     (statFs.availableBlocksLong * statFs.blockSizeLong / (1024.0 * 1024.0 * 1024.0)),
				                     (statFs.blockCountLong * statFs.blockSizeLong / (1024.0 * 1024.0 * 1024.0)))
			} catch (_: Exception) {
				return "Unknown"
			}
		}
	
	/**
	 * 生成更详细的Markdown格式Issue内容
	 */
	fun createDetailedIssueBody(throwable: Throwable): String {
		val markdown = StringBuilder()
		// 用户描述
		markdown.append("## 📝 用户描述\n")
		markdown.append("请简单描述崩溃发生时的场景和操作步骤。").append("\n\n")
		// 应用信息
		markdown.append("## 📱 应用信息\n")
		markdown.append("| 项目 | 值 |\n")
		markdown.append("|------|-----|\n")
		val packageInfo: PackageInfo
		try {
			packageInfo = packageManager.getPackageInfo(packageName, 0)
			markdown.append("| 应用版本 | ").append(packageInfo.versionName)
				.append(" (").append(PackageInfoCompat.getLongVersionCode(packageInfo)).append(") |\n")
			markdown.append("| 包名 | ").append(packageName).append(" |\n")
		} catch (e: PackageManager.NameNotFoundException) {
			throw RuntimeException(e)
		}
		markdown.append("| 构建类型 | ").append(BuildConfig.BUILD_TYPE).append(" |\n\n")
		// 设备信息表格
		markdown.append("## 📱 设备信息\n")
		markdown.append("| 项目 | 值 |\n")
		markdown.append("|------|-----|\n")
		markdown.append("| 设备型号 | ").append(Build.MANUFACTURER).append(" ")
			.append(Build.MODEL).append(" |\n")
		markdown.append("| Android版本 | ").append(Build.VERSION.RELEASE)
			.append(" (API ").append(SDK_INT).append(") |\n")
		// 屏幕信息
		if (SDK_INT >= Build.VERSION_CODES.R) {
			val bounds = windowManager?.currentWindowMetrics?.bounds
			bounds?.let {
				markdown.append("| 屏幕分辨率 | ").append(it.width())
					.append("×").append(it.height())
			}
		} else {
			val realSize = Point()
			windowManager?.defaultDisplay?.getRealSize(realSize)
			markdown.append("| 屏幕分辨率 | ").append(realSize.x)
				.append("×").append(realSize.y).append(" |\n")
		}
		markdown.append("| 屏幕密度 | ").append(resources.displayMetrics.densityDpi).append("dpi |\n")
		markdown.append("| 时区 | ").append(TimeZone.getDefault().id).append(" |\n")
		markdown.append("| 语言 | ").append(Locale.getDefault().language).append(" |\n\n")
		// 崩溃详情
		markdown.append("## 💥 崩溃详情\n")
		markdown.append("**异常类型**: `").append(throwable.javaClass.getSimpleName()).append("`\n\n")
		markdown.append("**异常消息**: \n```txt\n").append(if (throwable.message != null) throwable.message else "无消息").append("\n```\n\n")
		// 复现步骤
		markdown.append("## 🔄 复现步骤\n")
		markdown.append("1. [请描述如何复现这个问题]\n")
		markdown.append("2. \n")
		markdown.append("3. \n\n")
		// 期望行为与实际行为
		markdown.append("## ✅ 期望行为\n")
		markdown.append("[描述期望发生的行为]\n\n")
		
		markdown.append("## ❌ 实际行为\n")
		markdown.append("[描述实际发生的行为]\n\n")
		// 设备状态信息
		markdown.append("## 📊 设备状态\n")
		markdown.append("- **可用内存**: ").append(availableMemory).append("\n")
		markdown.append("- **存储空间**: ").append(storageInfo).append("\n")
		markdown.append("- **网络状态**: ").append(networkStatus).append("\n")
		markdown.append("- **电池状态**: ").append(batteryStatus).append("\n\n")
		// 崩溃时间
		markdown.append("## ⏰ 崩溃时间\n")
		markdown.append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n\n")
		return "$markdown"
	}
	
	private val networkStatus: String
		get() {
			var result = "No Network"
			val cm = getSystemService(ConnectivityManager::class.java)
			if (cm != null) {
				val networkCapabilities: NetworkCapabilities? = cm.getNetworkCapabilities(cm.activeNetwork)
				networkCapabilities?.let {
					if (it.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
						result = "WiFi"
					}
					if (it.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
						result = "Mobile"
					}
				}
			}
			return result
		}
	private val batteryStatus: String
		get() {
			val batteryStatus = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
			if (batteryStatus != null) {
				val level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
				val scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
				val batteryPct = level * 100 / scale.toFloat()
				val status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
				val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
					status == BatteryManager.BATTERY_STATUS_FULL
				return String.format(Locale.getDefault(),
				                     "%.1f%% %s", batteryPct,
				                     if (isCharging) "(充电中)" else "(未充电)")
			}
			return "未知"
		}
}