package com.miyuyan.sysuer.extra

import android.content.ComponentName
import android.content.pm.PackageManager
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import com.miyuyan.preference.EditPreference
import com.miyuyan.preference.FilterPreference
import com.miyuyan.preference.ItemPreference
import com.miyuyan.preference.JumpPreference
import com.miyuyan.preference.MenuPreference
import com.miyuyan.preference.PreferenceCategory
import com.miyuyan.preference.PreferenceScreen
import com.miyuyan.preference.SwitchPreference
import com.miyuyan.sysuer.R
import com.miyuyan.sysuer.api.SettingManager
import com.miyuyan.sysuer.nav.Browser
import com.miyuyan.sysuer.nav.Developer
import com.miyuyan.sysuer.nav.JsList
import com.miyuyan.sysuer.nav.Setting
import com.miyuyan.sysuer.nav.navigateBack
import com.miyuyan.sysuer.preference.BrowserPreference
import com.miyuyan.sysuer.preference.SettingPreference
import com.miyuyan.sysuer.view.ActivityPager

@Composable
internal fun PreferenceIcon(drawableId: Int) {
	Icon(
			painter = painterResource(drawableId),
			contentDescription = null,
			tint = MaterialTheme.colorScheme.primary
	)
}

/**
 * 应用设置：读写全部经 [SettingPreference] 的 DataStore（含旧 SharedPreferences 迁移），
 * 界面收集 [SettingPreference.data] 在偏好变化时重组。主题/语言/字体等需要重载
 * Activity 的项在回调里直接 recreate；图标主题切换启用对应的启动器图标别名。
 */
@Composable
fun SettingRoute(
	backStack: MutableList<NavKey>,
	sharedTransitionScope: SharedTransitionScope? = null,
	animatedVisibilityScope: AnimatedVisibilityScope? = null,
) {
	val context = LocalContext.current
	val activity = LocalActivity.current
	val sp = remember { SettingPreference(context) }
	val browserPreference = remember { BrowserPreference(context) }

	// DataStore 变化流：写入即时更新快照并发出新值，驱动本页重组
	val prefs by sp.data.collectAsStateWithLifecycle()

	ActivityPager(
			title = stringResource(R.string.setting),
			isNestedScrollEnabled = false,
			expandable = true,
			sharedKey = Setting.toString(),
			sharedTransitionScope = sharedTransitionScope,
			animatedVisibilityScope = animatedVisibilityScope,
			onNavigationClick = { backStack.navigateBack(activity) },
	) { _ ->
		PreferenceScreen {
			PreferenceCategory(title = stringResource(R.string.appearance)) {
				item {
					val theme = prefs[SettingPreference.THEME] ?: "2"
					val themeEntries = stringArrayResource(R.array.theme).toList()
					val themeValues = stringArrayResource(R.array.values_3).toList()
					MenuPreference(
							title = stringResource(R.string.theme),
							required = true,
							icon = { PreferenceIcon(R.drawable.light) },
							summary = themeEntries.getOrElse(themeValues.indexOf(theme)) { null },
							entries = themeEntries,
							entryValues = themeValues,
							selectedIndex = themeValues.indexOf(theme).takeIf { it >= 0 },
							onChange = { _, _, value ->
								value?.let {
									sp.theme = it
									activity?.recreate()
								}
							},
					)
				}
				item {
					SwitchPreference(
							title = stringResource(R.string.dynamic_color),
							summary = stringResource(R.string.dynamic_color_summary),
							checked = prefs[SettingPreference.DYNAMIC_COLOR] ?: true,
							onCheckedChange = {
								sp.dynamicColor = it
								activity?.recreate()
							},
							icon = { PreferenceIcon(R.drawable.palette) },
					)
				}
				item {
					val blurNavigationBar = prefs[SettingPreference.NAVIGATION_BAR] ?: true
					SwitchPreference(
							title = stringResource(R.string.navigation_bar),
							summary = stringResource(
									if (blurNavigationBar) R.string.floating_liquid_glass_navigation_bar_summary
									else R.string.material_navigation_bar_summary
							),
							checked = blurNavigationBar,
							onCheckedChange = { sp.blurNavigationBar = it },
							icon = { PreferenceIcon(R.drawable.navigation_bar) },
					)
				}
				item {
					val iconTheme = prefs[SettingPreference.ICON_THEME] ?: "0"
					val themeEntries = stringArrayResource(R.array.theme).toList()
					val themeValues = stringArrayResource(R.array.values_3).toList()
					MenuPreference(
							title = stringResource(R.string.icon_theme),
							icon = { PreferenceIcon(R.drawable.color) },
							summary = themeEntries.getOrElse(themeValues.indexOf(iconTheme)) { null },
							entries = themeEntries,
							entryValues = themeValues,
							required = true,
							selectedIndex = themeValues.indexOf(iconTheme).takeIf { it >= 0 },
							onChange = { _, _, value ->
								value?.let { newValue ->
									sp.iconTheme = newValue
									// 通过启用/停用启动器别名切换桌面图标
									val pm = context.packageManager
									val flags = PackageManager.DONT_KILL_APP
									pm.setComponentEnabledSetting(
											ComponentName(
													context, "${context.packageName}.MainActivity"
											),
											PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
											flags
									)
									pm.setComponentEnabledSetting(
											ComponentName(
													context,
													"${context.packageName}.MainActivityLight"
											), intArrayOf(1, 2, 2)[newValue.toInt()], flags
									)
									pm.setComponentEnabledSetting(
											ComponentName(
													context,
													"${context.packageName}.MainActivityDark"
											), intArrayOf(2, 1, 2)[newValue.toInt()], flags
									)
									pm.setComponentEnabledSetting(
											ComponentName(
													context,
													"${context.packageName}.MainActivityDefault"
											), intArrayOf(2, 2, 1)[newValue.toInt()], flags
									)
								}
							},
					)
				}
				item {
					val language = prefs[SettingPreference.LANGUAGE] ?: "2"
					val languageEntries = stringArrayResource(R.array.language).toList()
					MenuPreference(
							title = stringResource(R.string.language),
							icon = { PreferenceIcon(R.drawable.language) },
							summary = languageEntries.getOrElse(language.toInt()) { null },
							entries = languageEntries,
							entryValues = languageEntries.indices.map { it.toString() }.toList(),
							required = true,
							selectedIndex = language.toInt().takeIf { it >= 0 },
							onChange = { _, _, value ->
								value?.let {
									sp.language = it
									// 必须在 Activity 存活时应用语言：attachBaseContext 里调用
									// setApplicationLocales 会因没有已注册的 delegate 而被忽略，
									// 这里由 appcompat/系统应用并自动重建所有 Activity
									SettingManager.getInstance(context).setLanguage()
								}
							},
					)
				}
				item {
					val fontSize = prefs[SettingPreference.FONT_SIZE] ?: "0"
					val fontSizeEntries = stringArrayResource(R.array.font_size).toList()
					val fontSizeValues = stringArrayResource(R.array.values_6).toList()
					MenuPreference(
							title = stringResource(R.string.font_size),
							icon = { PreferenceIcon(R.drawable.font) },
							summary = fontSizeEntries.getOrElse(fontSizeValues.indexOf(fontSize)) { null },
							entries = fontSizeEntries,
							entryValues = fontSizeValues,
							required = true,
							selectedIndex = fontSizeValues.indexOf(fontSize).takeIf { it >= 0 },
							onChange = { _, _, value ->
								value?.let {
									sp.fontSize = it
									activity?.recreate()
								}
							},
					)
				}
				item {
					val home = prefs[SettingPreference.HOME] ?: "2"
					val homeEntries = stringArrayResource(R.array.home).toList()
					val homeValues = stringArrayResource(R.array.values_3).toList()
					MenuPreference(
							title = stringResource(R.string.home),
							icon = { PreferenceIcon(R.drawable.home) },
							summary = homeEntries.getOrElse(homeValues.indexOf(home)) { null },
							entries = homeEntries,
							entryValues = homeValues,
							required = true,
							selectedIndex = homeValues.indexOf(home).takeIf { it >= 0 },
							onChange = { _, _, value -> value?.let { sp.home = it } },
					)
				}
			}
			PreferenceCategory(title = stringResource(R.string.dashboard)) {
				item {
					EditPreference(
							title = stringResource(R.string.qrcode),
							value = prefs[SettingPreference.QRCODE] ?: "",
							onChange = { _, _, value -> sp.qrCode = value.orEmpty() },
							icon = { PreferenceIcon(R.drawable.qrcode) },
					)
				}
				item {
					val dashboardSelection =
						prefs[SettingPreference.DASHBOARD]?.mapNotNull { it.toIntOrNull() }?.toSet()
							?: (0..5).toSet()
					FilterPreference(
							title = stringResource(R.string.dashboard),
							entries = stringArrayResource(R.array.dashboard).toList(),
							entryValues = stringArrayResource(R.array.values_6).toList(),
							selections = dashboardSelection,
							onChange = { index, _, _ ->
								index?.let {
									// values_6 的业务值即索引本身，切换对应分区的显隐
									val current = sp.dashboard.toMutableSet()
									if (!current.add("$it")) current.remove("$it")
									sp.dashboard = current
								}
							},
							icon = { PreferenceIcon(R.drawable.window) },
					)
				}
				item {
					val courseDate = prefs[SettingPreference.COURSE_DATE]?.toIntOrNull() ?: 0
					MenuPreference(
							title = stringResource(R.string.course_date),
							icon = { PreferenceIcon(R.drawable.calendar) },
							summary = stringArrayResource(R.array.course_date).getOrElse(courseDate) { null },
							entries = stringArrayResource(R.array.course_date).toList(),
							entryValues = stringArrayResource(R.array.values_2).toList(),
							selectedIndex = courseDate.takeIf { it in 0..1 },
							onChange = { _, _, value ->
								value?.let { sp.courseDate = it.toIntOrNull() ?: 0 }
							},
					)
				}
			}
			PreferenceCategory(title = stringResource(R.string.browser)) {
				item {
					JumpPreference(
							title = R.string.javascript,
							icon = R.drawable.js,
							route = JsList(),
							backStack = backStack,
							activity = com.miyuyan.sysuer.browser.JsActivity::class.java,
							sharedTransitionScope = sharedTransitionScope,
							animatedVisibilityScope = animatedVisibilityScope,
					)
				}
				item {
					var imageBlocked by remember { mutableStateOf(browserPreference.isImageBlocked) }
					SwitchPreference(
							title = stringResource(R.string.image_blocked),
							checked = imageBlocked,
							onCheckedChange = {
								imageBlocked = it
								browserPreference.isImageBlocked = it
							},
							icon = { PreferenceIcon(R.drawable.image_block) },
					)
				}
				item {
					var jsEnabled by remember { mutableStateOf(browserPreference.isJSEnabled) }
					SwitchPreference(
							title = stringResource(R.string.js),
							checked = jsEnabled,
							onCheckedChange = {
								jsEnabled = it
								browserPreference.isJSEnabled = it
							},
							icon = { PreferenceIcon(R.drawable.refresh) },
					)
				}
				item {
					var saveMobileData by remember { mutableStateOf(browserPreference.isSaveMobileDataMode) }
					SwitchPreference(
							title = stringResource(R.string.save_mobile_data_mode),
							checked = saveMobileData,
							onCheckedChange = {
								saveMobileData = it
								browserPreference.isSaveMobileDataMode = it
							},
							icon = { PreferenceIcon(R.drawable.wifi) },
					)
				}
				item {
					var privacyMode by remember { mutableStateOf(browserPreference.isPrivacyMode) }
					SwitchPreference(
							title = stringResource(R.string.privacy_mode),
							checked = privacyMode,
							onCheckedChange = {
								privacyMode = it
								browserPreference.isPrivacyMode = it
							},
							icon = { PreferenceIcon(R.drawable.privacy) },
					)
				}
				item {
					var mobileMode by remember { mutableStateOf(browserPreference.isSaveMobileDataMode) }
					SwitchPreference(
							title = stringResource(R.string.mobile_mode),
							checked = mobileMode,
							onCheckedChange = {
								mobileMode = it
								// 注意：旧界面同样绑定 save_mobile_data_mode，此处保持一致
								browserPreference.isSaveMobileDataMode = it
							},
							icon = { PreferenceIcon(R.drawable.phone) },
					)
				}
			}
			PreferenceCategory(title = stringResource(R.string.app)) {
				item {
					SwitchPreference(
							title = stringResource(R.string.check_update),
							summary = stringResource(R.string.auto_update_when_launched),
							checked = prefs[SettingPreference.UPDATE] ?: true,
							onCheckedChange = { sp.update = it },
							icon = { PreferenceIcon(R.drawable.refresh) },
					)
				}
				if (prefs[SettingPreference.DEVELOPER_MODE] == true) {
					item {
						JumpPreference(
								title = R.string.developer_mode,
								icon = R.drawable.developer,
								route = Developer,
								backStack = backStack,
								activity = DeveloperActivity::class.java,
								sharedTransitionScope = sharedTransitionScope,
								animatedVisibilityScope = animatedVisibilityScope,
						)
					}
				}
				item {
					JumpPreference(
							title = R.string.about,
							icon = R.drawable.info,
							route = com.miyuyan.sysuer.nav.About,
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
							route = com.miyuyan.sysuer.nav.Update,
							backStack = backStack,
							activity = UpdateActivity::class.java,
							sharedTransitionScope = sharedTransitionScope,
							animatedVisibilityScope = animatedVisibilityScope,
					)
				}
				item {
					ItemPreference(
							modifier = if (sharedTransitionScope != null && animatedVisibilityScope != null) {
						with(sharedTransitionScope) {
							Modifier.sharedBounds(
									sharedContentState = rememberSharedContentState(
											key = "https://sysu-tang.github.io/sysuer-website/docs/user/introduction"
									),
									animatedVisibilityScope = animatedVisibilityScope,
							)
						}
					} else Modifier,
							onClick = { backStack.add(Browser("https://sysu-tang.github.io/sysuer-website/docs/user/introduction")) },
							title = stringResource(R.string.help),
							summary = "https://sysu-tang.github.io/sysuer-website/docs/user/introduction",
							icon = R.drawable.help
					)
				}
			}
		}
	}
}
