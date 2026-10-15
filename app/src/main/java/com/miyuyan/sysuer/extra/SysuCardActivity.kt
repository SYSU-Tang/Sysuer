package com.miyuyan.sysuer.extra

import android.content.Intent
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.Bundle
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import com.miyuyan.sysuer.BaseActivity
import com.miyuyan.sysuer.R
import com.miyuyan.sysuer.api.ShortcutReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku

/**
 * 桌面快捷方式「逸仙卡」的入口，用于打开微信「中山大学校园卡」小程序。
 * 优先通过 Shizuku 启动微信的校园卡快捷方式，失败时回退到设置中保存的逸仙码链接，
 * 与 [com.miyuyan.sysuer.home.DashboardViewModel.openQrCode] 行为一致。
 */
class SysuCardActivity : BaseActivity() {

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		lifecycleScope.launch {
			val started = withContext(Dispatchers.IO) {
				shizukuGranted() && startWechatCardShortcut()
			}
			if (started) finish() else openByQrCode()
		}
	}

	private fun shizukuGranted(): Boolean = runCatching {
		Shizuku.pingBinder() && (!Shizuku.isPreV11()) &&
				(Shizuku.checkSelfPermission() == PERMISSION_GRANTED)
	}.getOrDefault(defaultValue = false)

	private fun startWechatCardShortcut(): Boolean {
		val shortcut = runCatching { ShortcutReader.query("com.tencent.mm") }.getOrNull()
			?.firstOrNull { it.shortLabel?.contains(CARD_SHORTCUT_LABEL) == true } ?: return false
		return runCatching { ShortcutReader.start(shortcut) }.getOrDefault(defaultValue = false)
	}

	private fun openByQrCode() {
		settingManager.qrCode.takeIf { it.isNotEmpty() }?.let { qrCode ->
			Intent(Intent.ACTION_VIEW, qrCode.toUri()).takeIf {
				it.resolveActivity(packageManager) != null
			}?.let(::startActivity) ?: config.toast(R.string.fix_sysu_code_warning)
		} ?: config.toast(R.string.set_sysu_code_warning)
		finish()
	}

	companion object {
		private const val CARD_SHORTCUT_LABEL = "中山大学校园卡"
	}
}
