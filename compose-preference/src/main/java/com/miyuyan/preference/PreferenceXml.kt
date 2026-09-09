package com.miyuyan.preference

import android.annotation.SuppressLint
import android.content.SharedPreferences
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.edit
import androidx.core.graphics.drawable.toBitmap
import androidx.preference.CheckBoxPreference
import androidx.preference.PreferenceManager
import androidx.preference.SwitchPreference
import androidx.preference.children
import androidx.preference.Preference as AndroidXPreference
import androidx.preference.PreferenceCategory as AndroidXPreferenceCategory
import androidx.preference.PreferenceScreen as AndroidXPreferenceScreen

/**
 * 从 XML 资源 inflate 得到的 Preference 中,当前无法自动识别的自定义类型
 * 会通过这个回调交给调用方自行渲染。
 *
 * 典型的自定义类型例子:
 * - `com.miyuyan.sysuer.preference.MenuPreference`
 * - `com.miyuyan.sysuer.preference.EditPreference`
 * - `rikka.preference.SimpleMenuPreference`
 */
interface CustomPreferenceHandler {
	/**
	 * 把一个无法自动识别的 AndroidX Preference 渲染到当前 [PreferenceCategoryScope] 中。
	 */
	@Composable
	fun PreferenceCategoryScope.Render(preference: AndroidXPreference)
}

/**
 * 从 AndroidX Preference XML 资源渲染出 Compose 设置界面。
 *
 * ## 自动处理的类型
 * | XML 标签                             | Compose 映射                   | 说明                                     |
 * |--------------------------------------|--------------------------------|------------------------------------------|
 * | `<PreferenceCategory>`               | [PreferenceCategory]           | 分组标题 + 子项列表                       |
 * | `<SwitchPreference>`                 | [SwitchPreference]             | 自动读写 SharedPreferences               |
 * | `<rikka.material.preference.MaterialSwitchPreference>` | 同上(继承自 SwitchPreference) | 子类自动识别 |
 * | `<CheckBoxPreference>`               | [CheckBoxPreference]           | 自动读写 SharedPreferences               |
 * | `<Preference>` (带 `<intent>`)       | [Preference] + startActivity   | 点击自动执行 intent                      |
 * | `<Preference>` (无 intent)           | [Preference] 只读展示          | 作为静态信息项                            |
 *
 * ## 自定义类型
 * 遇到无法识别的自定义 Preference 类时,会退化为普通 [Preference]。
 * 如果需要完整支持,传一个 [customHandler] 回调在里面手动渲染。
 *
 * ## 用法
 * ```
 * PreferenceScreen(
 *     xmlResId = R.xml.settings,
 *     title = "设置",
 * )
 * ```
 *
 * @param xmlResId `R.xml.xxx` 格式的 Preference XML 资源 id。
 * @param title 屏幕顶部大标题(可选,传 null 则不显示)。
 * @param customHandler 处理自定义 Preference 类型的回调(可选)。
 * @param sharedPreferences 读写 Preference 值用的 SP,默认取 [PreferenceManager.getDefaultSharedPreferences]。
 */
@SuppressLint("RestrictedApi")
@Composable
fun PreferenceScreen(
	xmlResId: Int,
	modifier: Modifier = Modifier,
	title: String? = null,
	customHandler: CustomPreferenceHandler? = null,
	sharedPreferences: SharedPreferences? = null,
) {
	val context = LocalContext.current
	val sp = sharedPreferences ?: PreferenceManager.getDefaultSharedPreferences(context)

	var version by remember { mutableIntStateOf(0) }
	DisposableEffect(sp) {
		val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> version++ }
		sp.registerOnSharedPreferenceChangeListener(listener)
		onDispose { sp.unregisterOnSharedPreferenceChangeListener(listener) }
	}

	val rootScreen = remember(xmlResId) {
		PreferenceManager(context).inflateFromResource(context, xmlResId, null)
	}

	PreferenceScreen(modifier = modifier, title = title) {
		rootScreen.children.forEach { child ->
			when (child) {
				is AndroidXPreferenceCategory -> {
					XmlCategory(child, sp, customHandler, version)
				}

				is AndroidXPreferenceScreen -> {
					child.children.forEach { sub ->
						if (sub is AndroidXPreferenceCategory) {
							XmlCategory(sub, sp, customHandler, version)
						} else {
							PreferenceCategory(title = null) {
								item { XmlPreference(sub, sp, customHandler, version) }
							}
						}
					}
				}

				else -> {
					PreferenceCategory(title = null) {
						item { XmlPreference(child, sp, customHandler, version) }
					}
				}
			}
		}
	}
}

@Composable
private fun XmlCategory(
	category: AndroidXPreferenceCategory,
	sp: SharedPreferences,
	customHandler: CustomPreferenceHandler?,
	version: Int,
) {
	val title = category.title?.toString() ?: ""
	PreferenceCategory(title = title) {
		category.children.forEach { child ->
			item { XmlPreference(child, sp, customHandler, version) }
		}
	}
}

@Composable
private fun XmlPreference(
	pref: AndroidXPreference,
	sp: SharedPreferences,
	customHandler: CustomPreferenceHandler?,
	version: Int,
) {
	if (!pref.isVisible) return
	val key = pref.key
	val title = pref.title?.toString().orEmpty()
	val summary = pref.summary?.toString()
	val icon = pref.icon


	when (pref) {
		is SwitchPreference -> {
			if (key == null) return
			val defaultVal = pref.isChecked
			var checked by remember(key, version) {
				mutableStateOf(sp.getBoolean(key, defaultVal))
			}
			SwitchPreference(
				title = title,
				checked = checked,
				onCheckedChange = { newValue ->
					sp.edit { putBoolean(key, newValue) }
					checked = newValue
				},
				summary = summary,
				enabled = pref.isEnabled,
				icon = if (icon != null) {
					{ Icon(icon.toBitmap().asImageBitmap(), contentDescription = null) }
				} else null,
			)
		}

		is CheckBoxPreference -> {
			if (key == null) return
			val defaultVal = pref.isChecked
			var checked by remember(key, version) {
				mutableStateOf(sp.getBoolean(key, defaultVal))
			}
			CheckBoxPreference(
				title = title,
				checked = checked,
				onCheckedChange = { newValue ->
					sp.edit { putBoolean(key, newValue) }
					checked = newValue
				},
				summary = summary,
				enabled = pref.isEnabled,
			)
		}

		else -> {
			val context = LocalContext.current
			val clickAction: (() -> Unit)? = pref.intent?.let { intent ->
				{ runCatching { context.startActivity(intent) } }
			}
			Preference(
				onClick = clickAction,
				title = title,
				summary = summary,
				enabled = pref.isEnabled,
				icon = if (icon != null) {
					{ Icon(icon.toBitmap().asImageBitmap(), contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
				} else null,
			)
		}
	}
}