package com.miyuyan.sysuer.home

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ColorLens
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.navigation3.runtime.NavKey
import com.miyuyan.preference.ChoicePreference
import com.miyuyan.preference.EditPreference
import com.miyuyan.preference.FilterPreference
import com.miyuyan.preference.FilteredEditMenuPreference
import com.miyuyan.preference.JumpPreference
import com.miyuyan.preference.MenuPreference
import com.miyuyan.preference.PreferenceCategory
import com.miyuyan.preference.PreferenceScreen
import com.miyuyan.preference.SliderPreference
import com.miyuyan.preference.WheelPreference
import com.miyuyan.sysuer.R
import com.miyuyan.sysuer.browser.BrowserActivity
import com.miyuyan.sysuer.extra.AboutActivity
import com.miyuyan.sysuer.extra.PrivacyActivity
import com.miyuyan.sysuer.extra.SettingActivity
import com.miyuyan.sysuer.extra.UpdateActivity
import com.miyuyan.sysuer.nav.About
import com.miyuyan.sysuer.nav.Privacy
import com.miyuyan.sysuer.nav.Update
import kotlin.math.roundToInt

@Composable
fun AccountScreen(
	backStack: MutableList<NavKey>,
	sharedTransitionScope: SharedTransitionScope? = null,
	animatedVisibilityScope: AnimatedVisibilityScope? = null,
	recreate: () -> Unit
) {
	val context = LocalContext.current
	val settingLauncher = rememberLauncherForActivityResult(
			contract = ActivityResultContracts.StartActivityForResult(),
			onResult = { o: ActivityResult ->
				if (o.resultCode == Activity.RESULT_OK) recreate()
			},
	)

	PreferenceScreen {
		PreferenceCategory(title = stringResource(R.string.account)) {
			item {
				JumpPreference(
						title = R.string.privacy,
						icon = R.drawable.account,
						route = Privacy,
						backStack = backStack,
						activity = PrivacyActivity::class.java,
						sharedTransitionScope = sharedTransitionScope,
						animatedVisibilityScope = animatedVisibilityScope,
				)
			}
		}

		PreferenceCategory(title = stringResource(R.string.app)) {
			item {
				JumpPreference(
						title = R.string.setting,
						icon = R.drawable.setting,
				) {
					settingLauncher.launch(Intent(context, SettingActivity::class.java))
				}
			}
			item {
				JumpPreference(
						title = R.string.about,
						icon = R.drawable.info,
						route = About,
						backStack = backStack,
						activity = AboutActivity::class.java,
						sharedTransitionScope = sharedTransitionScope,
						animatedVisibilityScope = animatedVisibilityScope,
				)
			}
			item {
				JumpPreference(
						title = R.string.update,
						icon = R.drawable.refresh,
						route = Update,
						backStack = backStack,
						activity = UpdateActivity::class.java,
						sharedTransitionScope = sharedTransitionScope,
						animatedVisibilityScope = animatedVisibilityScope,
				)
			}
			item {
				JumpPreference(
						title = R.string.help,
						icon = R.drawable.help,
				) {
					context.startActivity(
							Intent(
									context, BrowserActivity::class.java
							).setData("https://sysu-tang.github.io/sysuer-website/docs/user/introduction".toUri())
					)
				}
			}
			/*item {
				MenuPreference(
						icon = {
							Icon(
									Icons.Rounded.ColorLens,
									stringResource(R.string.theme),
							)
						}, title = stringResource(R.string.theme), entries = listOf(
						stringResource(R.string.light),
						stringResource(R.string.dark),
						stringResource(R.string.follow_system)
				), entryValues = listOf(1, 2, 3)
				)
			}
			item {
				EditPreference(
						title = "昵称",
						onChange = { _, _, _ -> },
				)
			}
			item {
				SliderPreference(
						title = "周数上限",
						valueRange = 1f..25f,
						steps = 23,
						initialValue = 17f,
						valueText = { "第 ${it.roundToInt()} 周" },
						onValueChangeFinished = {},
				)
			}
			item {
				ChoicePreference(
						title = "主题模式",
						entries = listOf("亮色", "暗色", "跟随系统"),
						entryValues = listOf("light", "dark", "auto"),
						icon = { Icon(painterResource(R.drawable.info), null) },
				)
			}
			item {
				FilteredEditMenuPreference(
						title = "开课单位",
						entries = listOf("全部", "大一", "大二", "大三", "大四"),   // 选项文案
						entryValues = listOf("all", "1", "2", "3", "4"), // 业务值（如院系编号）
						name = "全部",                          // 当前值：entryValue 或自定义文本
						onChange = { _, _, _ -> },
						requireSelection = false,                      // false 时允许自由输入
						placeholder = "点击输入筛选",
						icon = { Icon(painterResource(R.drawable.home), null) },
				)
			}
			item {
				WheelPreference(
						title = "入学年级",
						entries = listOf("2023级", "2024级", "2025级", "2026级"),
						entryValues = listOf("2023", "2024", "2025", "2026"),
						selectedIndex = 0,          // Int?，越界/null 落到首个有效项
						onChange = { index, name, value -> },
						pickerWidth = 160.dp,                   // 滚轮宽度
						itemHeight = 44.dp, visibleCount = 3,   // 滚轮高度 = itemHeight × visibleCount
						icon = { Icon(painterResource(R.drawable.calendar), null) },
				)
			}*/
		}
	}
}