package com.miyuyan.preference

import android.content.Intent
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey

/**
 * 跳转型偏好项:点击后跳转到指定 Activity / NavKey 路由。
 *
 * 三种触发方式(按优先级):
 * 1. 自定义 [onClick](传了就用)
 * 2. [route] + [backStack](导航 3 添加到返回栈)
 * 3. [activity](启动 Android Activity)
 *
 * @param key 标题对应的 string 资源 id。
 * @param icon 左侧图标 drawable 资源 id。
 * @param activity 目标 Activity(可选)。
 * @param route 目标 NavKey 路由(可选)。
 * @param backStack 导航 3 的 backStack(配合 [route] 使用)。
 * @param onClick 自定义点击回调,优先级最高。
 */
@Composable
fun JumpPreference(
	key: Int,
	icon: Int,
	modifier: Modifier = Modifier,
	activity: Class<*>? = null,
	route: NavKey? = null,
	backStack: MutableList<NavKey>? = null,
	onClick: (() -> Unit)? = null,
) {
	val context = LocalContext.current
	val resolvedOnClick: () -> Unit = onClick ?: {
		if (route != null && backStack != null) {
			backStack.add(route)
		} else if (activity != null) {
			context.startActivity(Intent(context, activity))
		}
	}
	Preference(
		onClick = resolvedOnClick,
		title = stringResource(key),
		modifier = modifier,
		icon = icon,
		trailing = {
			Icon(
				imageVector = Icons.AutoMirrored.Rounded.ArrowForwardIos,
				contentDescription = null,
				modifier = Modifier.size(20.dp),
			)
		},
	)
}
