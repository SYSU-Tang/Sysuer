package com.miyuyan.sysuer.extra

import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import com.miyuyan.preference.PreferenceCategory
import com.miyuyan.preference.PreferenceScreen
import com.miyuyan.preference.SwitchPreference
import com.miyuyan.sysuer.R
import com.miyuyan.sysuer.nav.Developer
import com.miyuyan.sysuer.nav.navigateBack
import com.miyuyan.sysuer.preference.SettingPreference
import com.miyuyan.sysuer.view.ActivityPager

/**
 * 开发者选项：开关读写经 [SettingPreference] 的 DataStore，界面收集 [SettingPreference.data]
 * 在偏好变化时重组。开启开发者模式后，设置页会出现开发者选项入口。
 */
@Composable
fun DevelopRoute(
	backStack: MutableList<NavKey>,
	sharedTransitionScope: SharedTransitionScope? = null,
	animatedVisibilityScope: AnimatedVisibilityScope? = null,
) {
	val context = LocalContext.current
	val activity = LocalActivity.current
	val sp = remember { SettingPreference(context) }

	// DataStore 变化流：写入即时更新快照并发出新值，驱动本页重组
	val prefs by sp.data.collectAsStateWithLifecycle()

	ActivityPager(
			title = stringResource(R.string.developer_mode),
			isNestedScrollEnabled = false,
			sharedKey = "Developer",
			sharedTransitionScope = sharedTransitionScope,
			animatedVisibilityScope = animatedVisibilityScope,
			onNavigationClick = { backStack.navigateBack(activity) },
	) { _ ->
		PreferenceScreen {
			PreferenceCategory {
				item {
					SwitchPreference(
							title = stringResource(R.string.developer_mode),
							checked = prefs[SettingPreference.DEVELOPER_MODE] == true,
							onCheckedChange = { sp.developerMode = it },
							icon = { PreferenceIcon(R.drawable.developer) },
					)
				}
				item {
					SwitchPreference(
							title = stringResource(R.string.beta_check),
							checked = prefs[SettingPreference.BETA_CHECK] == true,
							onCheckedChange = { sp.betaCheck = it },
							icon = { PreferenceIcon(R.drawable.flask) },
					)
				}
			}
		}
	}
}
