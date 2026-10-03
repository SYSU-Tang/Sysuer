package com.miyuyan.sysuer.extra

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.pm.PackageInfoCompat
import androidx.core.net.toUri
import androidx.navigation3.runtime.NavKey
import com.mikepenz.markdown.compose.Markdown
import com.mikepenz.markdown.compose.components.markdownComponents
import com.mikepenz.markdown.compose.elements.MarkdownTable
import com.mikepenz.markdown.compose.elements.MarkdownTableHeader
import com.mikepenz.markdown.compose.elements.MarkdownTableRow
import com.mikepenz.markdown.m3.markdownColor
import com.mikepenz.markdown.m3.markdownTypography
import com.mikepenz.markdown.model.rememberMarkdownState
import com.miyuyan.sysuer.BuildConfig
import com.miyuyan.sysuer.R
import com.miyuyan.sysuer.api.ContextUtil
import com.miyuyan.sysuer.api.DateTimeManager
import com.miyuyan.sysuer.nav.Crash
import com.miyuyan.sysuer.nav.navigateBack
import com.miyuyan.sysuer.view.ActivityPager
import java.time.LocalDateTime
import java.util.Locale
import java.util.TimeZone

@Composable
fun CrashRoute(
	backStack: MutableList<NavKey>,
	navKey: Crash? = backStack.lastOrNull() as? Crash,
	sharedTransitionScope: SharedTransitionScope? = null,
	animatedVisibilityScope: AnimatedVisibilityScope? = null
) {
	val activity = LocalActivity.current
	val context = LocalContext.current
	val contextUtil = remember(context) { ContextUtil(context) }
	val crashInfo = navKey?.crashInfo ?: ""

	val markdownContent = remember(crashInfo) {
		createDetailedIssueBody(context, contextUtil, RuntimeException(crashInfo))
	}

	ActivityPager(
			isNestedScrollEnabled = false,
			title = stringResource(R.string.crash),
			onNavigationClick = { backStack.navigateBack(activity) },
			sharedTransitionScope = sharedTransitionScope,
			animatedVisibilityScope = animatedVisibilityScope,
			pageContent = {
				Box(modifier = Modifier.fillMaxSize()) {
					SelectionContainer(
							modifier = Modifier.fillMaxSize()
					) {
						Column(
								modifier = Modifier
									.fillMaxSize()
									.verticalScroll(rememberScrollState())
									.padding(
											start = 16.dp, end = 16.dp, top = 16.dp, bottom = 88.dp
									)
						) {
							SelectionContainer {
								Markdown(
										markdownState = rememberMarkdownState(markdownContent),
										colors = markdownColor(),
										typography = markdownTypography(
												h1 = MaterialTheme.typography.headlineMedium,
												h2 = MaterialTheme.typography.titleLargeEmphasized,
												h3 = MaterialTheme.typography.titleMediumEmphasized
										),
										modifier = Modifier.fillMaxWidth(),
										components = markdownComponents(table = { model ->
											MarkdownTable(
													content = model.content,
													node = model.node,
													style = model.typography.table,
													headerBlock = { content, header, tableWidth, style ->
														MarkdownTableHeader(
																content = content,
																header = header,
																tableWidth = tableWidth,
																style = style,
																maxLines = Int.MAX_VALUE,
																overflow = TextOverflow.Clip,
														)
													},
													rowBlock = { content, row, tableWidth, style ->
														MarkdownTableRow(
																content = content,
																header = row,
																tableWidth = tableWidth,
																style = style,
																maxLines = Int.MAX_VALUE,
																overflow = TextOverflow.Clip,
														)
													},
											)
										})
								)
							}
						}
					}

					Surface(
							modifier = Modifier
								.align(Alignment.BottomCenter)
								.padding(
										horizontal = dimensionResource(R.dimen.horizontal_margin),
										vertical = dimensionResource(R.dimen.vertical_margin)
								),
							shape = CircleShape,
							color = MaterialTheme.colorScheme.surfaceContainerHigh,
					) {
						Row(
								modifier = Modifier.padding(
										horizontal = dimensionResource(R.dimen.horizontal_margin),
										vertical = dimensionResource(R.dimen.vertical_margin)
								),
								horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.horizontal_gap)),
								verticalAlignment = Alignment.CenterVertically
						) {
							FilledTonalButton(onClick = {
								context.packageManager
									.getLaunchIntentForPackage(context.packageName)?.also {
										context.startActivity(it.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK))
									}
								backStack.navigateBack(activity)
							}, shapes = ButtonDefaults.shapes()) {
								Icon(painterResource(R.drawable.refresh), contentDescription = null)
								Spacer(Modifier.width(ButtonDefaults.IconSpacing))
								Text(stringResource(R.string.restart))
							}

							FilledTonalButton(onClick = {
								contextUtil.copy("crash", crashInfo)
								contextUtil.toast(R.string.copy_successfully)
							}, shapes = ButtonDefaults.shapes()) {
								Icon(painterResource(R.drawable.copy), contentDescription = null)
								Spacer(Modifier.width(ButtonDefaults.IconSpacing))
								Text(stringResource(R.string.copy))
							}

							FilledTonalButton(onClick = {
								openIssueInBrowser(context, contextUtil, crashInfo)
							}, shapes = ButtonDefaults.shapes()) {
								Icon(painterResource(R.drawable.share), contentDescription = null)
								Spacer(Modifier.width(ButtonDefaults.IconSpacing))
								Text(stringResource(R.string.submit_issues))
							}
						}
					}
				}
			})
}

private fun openIssueInBrowser(context: Context, contextUtil: ContextUtil, crashInfo: String) {
	try {
		val timestamp = DateTimeManager.toDateTimeString(LocalDateTime.now())
		var exceptionType = "Unknown Exception"
		if (crashInfo.isNotEmpty()) {
			val lines =
				crashInfo.split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
			if (lines.isNotEmpty()) {
				val firstLine = lines[0]
				if (firstLine.contains(":")) {
					exceptionType = firstLine.split(":".toRegex()).dropLastWhile { it.isEmpty() }
						.toTypedArray()[0]
				}
			}
		}
		val title = "[崩溃报告] $exceptionType - $timestamp"

		contextUtil.copy("crash_issue", crashInfo)
		contextUtil.toast(R.string.copy_successfully)

		context.startActivity(
				Intent(Intent.ACTION_VIEW)
					.setData("https://github.com/SYSU-Tang/Sysuer/issues/new?title=$title&labels=bug,crash-report".toUri())
					.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
		)
	} catch (_: Exception) {
	}
}

private fun getAvailableMemory(context: Context): String {
	val memoryInfo = ActivityManager.MemoryInfo()
	context.getSystemService(ActivityManager::class.java)?.getMemoryInfo(memoryInfo)
	return String.format(
			Locale.getDefault(),
			"%.2f MB / %.2f MB",
			(memoryInfo.availMem / (1024.0 * 1024.0)),
			(memoryInfo.totalMem / (1024.0 * 1024.0))
	)
}

private fun getStorageInfo(context: Context): String = try {
	val statFs = StatFs(Environment.getDataDirectory().path)
	String.format(
			Locale.getDefault(),
			"%.2f GB / %.2f GB",
			(statFs.availableBlocksLong * statFs.blockSizeLong / (1024.0 * 1024.0 * 1024.0)),
			(statFs.blockCountLong * statFs.blockSizeLong / (1024.0 * 1024.0 * 1024.0))
	)
} catch (_: Exception) {
	context.getString(R.string.unknown)
}

private fun getNetworkStatus(context: Context): String {
	val list = mutableListOf<String>()
	val cm = context.getSystemService(ConnectivityManager::class.java)
	val networkCapabilities: NetworkCapabilities? = cm?.getNetworkCapabilities(cm.activeNetwork)
	networkCapabilities?.let {
		if (it.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) list.add("WiFi")
		if (it.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) list.add("Mobile")
	}
	return if (list.isEmpty()) "No Network" else list.joinToString("|")
}

private fun getBatteryStatus(context: Context): String {
	val batteryStatus = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
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
				context.getString(if (isCharging) R.string.charging else R.string.uncharge)
		)
	} else context.getString(R.string.unknown)
}

private fun createDetailedIssueBody(
	context: Context, contextUtil: ContextUtil, throwable: Throwable
): String = buildString {
	append("## 📝 用户描述\n")
	append("请简单描述崩溃发生时的场景和操作步骤。").append("\n\n")
	append("## 📱 应用信息\n")
	append("| 项目 | 值 |\n")
	append("|------|-----|\n")
	try {
		val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
		append("| 应用版本 | ").append(packageInfo.versionName).append(" (")
			.append(PackageInfoCompat.getLongVersionCode(packageInfo)).append(") |\n")
		append("| 包名 | ").append(context.packageName).append(" |\n")
	} catch (e: PackageManager.NameNotFoundException) {
		e.printStackTrace()
	}
	append("| 构建类型 | ").append(BuildConfig.BUILD_TYPE).append(" |\n\n")
	append("## 📱 设备信息\n")
	append("| 项目 | 值 |\n")
	append("|------|-----|\n")
	append("| 设备型号 | ").append(Build.MANUFACTURER).append(" ").append(Build.MODEL)
		.append(" |\n")
	append("| Android版本 | ").append(Build.VERSION.RELEASE).append(" (API ")
		.append(Build.VERSION.SDK_INT).append(") |\n")
	append("| 屏幕分辨率 | ").append(contextUtil.width).append("×").append(contextUtil.height)
		.append(" |\n")
	append("| 屏幕密度 | ").append(context.resources.displayMetrics.densityDpi).append("dpi |\n")
	append("| 时区 | ").append(TimeZone.getDefault().id).append(" |\n")
	append("| 语言 | ").append(Locale.getDefault().language).append(" |\n\n")
	append("## 💥 崩溃详情\n")
	append("**异常类型**: `").append(throwable.javaClass.simpleName).append("`\n\n")
	append("**异常消息**: \n```txt\n").append(throwable.message ?: "无消息").append("\n```\n\n")
	append("## 🔄 复现步骤\n")
	append("1. [请描述如何复现这个问题]\n")
	append("2. \n")
	append("3. \n\n")
	append("## ✅ 期望行为\n")
	append("[描述期望发生的行为]\n\n")
	append("## ❌ 实际行为\n")
	append("[描述实际发生的行为]\n\n")
	append("## 📊 设备状态\n")
	append("- **可用内存**: ").append(getAvailableMemory(context)).append("\n")
	append("- **存储空间**: ").append(getStorageInfo(context)).append("\n")
	append("- **网络状态**: ").append(getNetworkStatus(context)).append("\n")
	append("- **电池状态**: ").append(getBatteryStatus(context)).append("\n\n")
	append("## ⏰ 崩溃时间\n")
	append(DateTimeManager.toDateTimeString(LocalDateTime.now())).append("\n\n")
}
