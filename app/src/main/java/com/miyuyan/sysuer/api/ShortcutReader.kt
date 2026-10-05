package com.miyuyan.sysuer.api

import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.ShortcutInfo
import android.graphics.BitmapFactory
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.IBinder
import android.os.ParcelFileDescriptor
import android.os.Process
import android.os.UserHandle
import androidx.core.graphics.drawable.toDrawable
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuBinderWrapper
import rikka.shizuku.SystemServiceHelper
import java.lang.reflect.InvocationTargetException

object ShortcutReader {

	private const val SHELL_PKG = "com.android.shell"
	private const val FLAG_MATCH_CACHED = 1 shl 4                    // API 30+
	private const val FLAG_MATCH_PINNED_BY_ANY_LAUNCHER = 1 shl 10   // API 30+ 隐藏，值请对照源码核实

	private val ILauncherApps: Class<*> by lazy { Class.forName("android.content.pm.ILauncherApps") }

	@Volatile
	private var cached: Any? = null

	fun reset() {
		cached = null
	}

	private fun service(): Any {
		cached?.let { return it }
		synchronized(this) {
			cached?.let { return it }
			val binder = ShizukuBinderWrapper(
					SystemServiceHelper.getSystemService(Context.LAUNCHER_APPS_SERVICE)
			)
			val s = Class.forName($$"android.content.pm.ILauncherApps$Stub")
				.getMethod("asInterface", IBinder::class.java).invoke(null, binder)!!
			cached = s
			return s
		}
	}

	/** callingPackage 必须与实际调用 uid 匹配，否则会抛 "Calling package name mismatch" */
	private fun callerPackage(): String {
		if (Shizuku.getUid() != 2000) {
			error("Calling package name mismatch")
		}
		return SHELL_PKG
	}


	private fun myUserId() = Process.myUid() / 100_000

	private fun findMethod(name: String) = ILauncherApps.methods.firstOrNull { it.name == name }
		?: throw NoSuchMethodException("ILauncherApps.$name")

	private fun invoke(name: String, vararg args: Any?): Any? = try {
		findMethod(name).invoke(service(), *args)
	} catch (e: InvocationTargetException) {
		throw e.targetException ?: e
	}

// ---------- 查询 ----------

	private fun queryFlags(includePinnedByAny: Boolean): Int {
		var f =
			LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC or LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST or LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED
		if (Build.VERSION.SDK_INT >= 30) {
			f = f or FLAG_MATCH_CACHED
			if (includePinnedByAny) f = f or FLAG_MATCH_PINNED_BY_ANY_LAUNCHER
		}
		return f
	}

	/** Android 11+：按构造函数参数类型自动填充，避免写死参数顺序 */
	private fun newQueryWrapper(pkg: String?, flags: Int): Any =
		Class.forName("android.content.pm.ShortcutQueryWrapper")
			.getDeclaredConstructor(LauncherApps.ShortcutQuery::class.java).newInstance(
					LauncherApps.ShortcutQuery().apply {
						setQueryFlags(flags)
						pkg?.let { setPackage(it) }
					})

	/** packageName 为 null 表示不限包名 */
	fun query(
		packageName: String? = null,
		user: UserHandle = Process.myUserHandle(),
		includePinnedByAny: Boolean = true,
	): List<ShortcutInfo> {
		val flags = queryFlags(includePinnedByAny)
		val caller = callerPackage()
		val slice = when (val n = findMethod("getShortcuts").parameterTypes.size) {
			// Android 11+: (callingPackage, ShortcutQueryWrapper, user)
			3 -> invoke("getShortcuts", caller, newQueryWrapper(packageName, flags), user)
			// Android 8–10: (callingPackage, changedSince, packageName, shortcutIds, componentName, flags, user)
			7 -> invoke("getShortcuts", caller, 0L, packageName, null, null, flags, user)
			else -> error("未识别的 getShortcuts 签名，参数个数=$n")
		} ?: return emptyList()
		@Suppress("UNCHECKED_CAST") return (slice.javaClass.getMethod("getList")
			.invoke(slice) as List<ShortcutInfo>)
	}


	/** 优先一次性查全部；若系统不支持不限包名，则逐个桌面应用查询 */
	fun queryAll(ctx: Context, includePinnedByAny: Boolean = false): List<ShortcutInfo> {
		val direct = try {
			query(null, includePinnedByAny = includePinnedByAny)
		} catch (e: SecurityException) {
			throw e
		} catch (_: Throwable) {
			emptyList()
		}
		val infos = direct.ifEmpty {
			val pm = ctx.packageManager
			val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
			pm.queryIntentActivities(intent, 0).map { it.activityInfo.packageName }.distinct()
				.flatMap { pkg ->
					try {
						query(pkg, includePinnedByAny = includePinnedByAny)
					} catch (e: SecurityException) {
						throw e
					} catch (e: Throwable) {
						emptyList()
					}
				}
		}
		return infos/*.map { info -> info.toItem() }
			.sortedWith(compareBy({ it.packageName }, { it.shortLabel }))*/
	}

// ---------- 图标 ----------

	private fun ShortcutInfo.hiddenBool(name: String): Boolean =
		runCatching { javaClass.getMethod(name).invoke(this) as Boolean }.getOrDefault(false)

	/** 逻辑对照 LauncherApps.getShortcutIconDrawable：位图走 FD，资源图标走对方包的 Resources */
	fun loadIcon(ctx: Context, s: ShortcutInfo): Drawable? = runCatching {
		val pkg = s.`package`
		when {
			s.hiddenBool("hasIconFile") -> {
				val pfd = invoke(
						"getShortcutIconFd", callerPackage(), pkg, s.id, myUserId()
				) as? ParcelFileDescriptor
				pfd?.use {
					BitmapFactory.decodeFileDescriptor(it.fileDescriptor)
				}?.let { bmp ->
					val dr = bmp.toDrawable(ctx.resources)
					if (s.hiddenBool("hasAdaptiveBitmap")) AdaptiveIconDrawable(null, dr) else dr
				}
			}

			s.hiddenBool("hasIconResource") -> {
				val resId = s.javaClass.getMethod("getIconResourceId").invoke(s) as Int
				if (resId == 0) null
				else ctx.packageManager.getResourcesForApplication(pkg)
					.getDrawableForDensity(resId, 0, null)
			}

			else -> null   // Uri 图标等情况暂未处理
		}
	}.getOrNull()

	fun start(s: ShortcutInfo): Boolean {
		val caller = callerPackage()
		val result = when (val n = findMethod("startShortcut").parameterTypes.size) {
			// Android 8–10: (callingPackage, packageName, id, sourceBounds, options, userId)
			6 -> invoke("startShortcut", caller, s.`package`, s.id, null, null, myUserId())
			// Android 11+: (callingPackage, packageName, featureId, id, sourceBounds, options, userId)
			7 -> invoke("startShortcut", caller, s.`package`, null, s.id, null, null, myUserId())
			else -> error("未识别的 startShortcut 签名，参数个数=$n")
		}
		return result as? Boolean ?: false
	}
}