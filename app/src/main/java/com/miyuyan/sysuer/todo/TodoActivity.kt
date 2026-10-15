package com.miyuyan.sysuer.todo

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.SharedTransitionLayout
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import com.miyuyan.sysuer.BaseActivity
import com.miyuyan.sysuer.nav.SysuerNavDisplay
import com.miyuyan.sysuer.nav.Todo
import com.miyuyan.sysuer.theme.SysuerTheme

/**
 * 待办页独立 Activity：直接承载 Compose 的 [TodoRoute]。
 *
 * 提醒通知由 [TodoReminderReceiver] 用 Intent 拉起本页，因此保留为独立 Activity
 * （而非只在 MainActivity 内导航）。返回时 [TodoRoute] 经 navigateBack(activity)
 * 触发 finishAfterTransition，栈深为 1 时即关闭本 Activity。
 */
class TodoActivity : BaseActivity() {
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		enableEdgeToEdge()
		setContent {
			val backStack = rememberNavBackStack(Todo)
			SysuerTheme(settingManager) {
				SharedTransitionLayout {
					SysuerNavDisplay(backStack = backStack, entryProvider = entryProvider {
						entry<Todo> {
							TodoRoute(backStack)
						}
					})
				}
			}
		}
	}
}
