package com.miyuyan.sysuer.extra

import android.content.Intent
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.navigation3.runtime.NavKey
import com.miyuyan.preference.Preference
import com.miyuyan.preference.PreferenceCategory
import com.miyuyan.preference.PreferenceScreen
import com.miyuyan.sysuer.R
import com.miyuyan.sysuer.api.ContextUtil
import com.miyuyan.sysuer.api.SettingManager
import com.miyuyan.sysuer.browser.BrowserActivity
import com.miyuyan.sysuer.nav.navigateBack
import com.miyuyan.sysuer.view.ActivityPager
import top.yukonga.miuix.kmp.squircle.squircleClip

@Composable
fun AboutRoute(
	backStack: MutableList<NavKey>,
	sharedTransitionScope: SharedTransitionScope? = null,
	animatedVisibilityScope: AnimatedVisibilityScope? = null
) {
	val context = LocalContext.current
	val settingManager = remember { SettingManager.getInstance(context) }
	val clicks = remember { mutableStateListOf<Long>() }

	ActivityPager(
		title = stringResource(R.string.about),
		expandable = true,
		onNavigationClick = { backStack.navigateBack() },
		sharedTransitionScope = sharedTransitionScope,
		animatedVisibilityScope = animatedVisibilityScope,
		isNestedScrollEnabled = false,
		sharedKey = "About",
		pageContent = {
			val context = LocalContext.current
			val openBrowser: (String) -> Unit = { url ->
				context.startActivity(
					Intent(
						context, BrowserActivity::class.java
					).setData(url.toUri())
				)
			}

			PreferenceScreen(modifier = Modifier.fillMaxSize()) {
				Column(
					modifier = Modifier
						.fillMaxWidth()
						.squircleClip(cornerRadius = 16.dp)
						.clickable(
							interactionSource = remember { MutableInteractionSource() },
							indication = ripple()
						) {
							val currentTime = System.currentTimeMillis()
							if (clicks.isEmpty() || currentTime - (clicks.lastOrNull()
									?: 0L) < 500
							) {
								clicks.add(currentTime)
								if (clicks.size == 5) {
									settingManager.developerMode = !settingManager.developerMode
									ContextUtil.getInstance(context).toast(
										if (settingManager.developerMode) R.string.developer_enabled
										else R.string.developer_disabled
									)
									clicks.clear()
								}
							} else {
								clicks.clear()
								clicks.add(currentTime)
							}
						}
						.padding(vertical = dimensionResource(R.dimen.vertical_padding)),
					horizontalAlignment = Alignment.CenterHorizontally,
				) {
					Image(
						modifier = Modifier.size(96.dp),
						painter = painterResource(id = R.drawable.ic_launcher_foreground),
						contentDescription = stringResource(R.string.app_name)
					)
					Text(
						text = stringResource(R.string.app_name),
						style = MaterialTheme.typography.headlineSmall,
						color = MaterialTheme.colorScheme.primary
					)
				}

				PreferenceCategory(title = stringResource(R.string.version)) {
					item {
						Preference(
							onClick = {
								context.startActivity(
									Intent(
										context, UpdateActivity::class.java
									)
								)
							},
							title = stringResource(R.string.current_version),
							summary = context.packageManager.getPackageInfo(
								context.packageName, 0
							).versionName,
							icon = {
								Icon(
									painterResource(R.drawable.submit), contentDescription = null
								)
							})
					}
					item {
						Preference(
							onClick = { openBrowser("https://github.com/SYSU-Tang/Sysuer/releases") },
							title = stringResource(R.string.download_link),
							summary = "https://github.com/SYSU-Tang/Sysuer/releases",
							icon = {
								Icon(
									painterResource(R.drawable.down), contentDescription = null
								)
							})
					}
					item {
						Preference(
							onClick = { openBrowser("https://sysu-tang.github.io/sysuer-website/") },
							title = stringResource(R.string.official_website),
							summary = "https://sysu-tang.github.io/sysuer-website/",
							icon = {
								Icon(
									painterResource(R.drawable.web), contentDescription = null
								)
							})
					}
				}

				PreferenceCategory(title = stringResource(R.string.project)) {
					item {
						Preference(
							onClick = { openBrowser("https://github.com/SYSU-Tang") },
							title = "SYSU—Tang",
							summary = "https://github.com/SYSU-Tang",
							icon = {
								Icon(
									painterResource(R.drawable.account), contentDescription = null
								)
							})
					}
					item {
						Preference(
							onClick = { openBrowser("https://github.com/SYSU-Tang/Sysuer/") },
							title = stringResource(R.string.app_name),
							summary = "https://github.com/SYSU-Tang/Sysuer/",
							icon = {
								Icon(
									painterResource(R.drawable.version), contentDescription = null
								)
							})
					}
					item {
						Preference(
							onClick = { openBrowser("mqqopensdkapi://bizAgent/qm/qr?url=http%3A%2F%2Fqm.qq.com%2Fcgi-bin%2Fqm%2Fqr%3Ffrom%3Dapp%26p%3Dandroid%26jump_from%3Dwebapi%26k%3Di4M0YfNzHskNiXgiYTAvA1EAWZh7HNQx") },
							title = stringResource(R.string.qq_group),
							summary = "1063579244",
							icon = {
								Icon(
									painterResource(R.drawable.group), contentDescription = null
								)
							})
					}
				}

				PreferenceCategory(title = stringResource(R.string.feedback)) {
					item {
						Preference(
							onClick = { openBrowser("https://github.com/SYSU-Tang/Sysuer/issues") },
							title = stringResource(R.string.submit_issues),
							summary = "https://github.com/SYSU-Tang/Sysuer/issues",
							icon = {
								Icon(
									painterResource(R.drawable.question), contentDescription = null
								)
							})
					}
					item {
						Preference(
							onClick = { openBrowser("https://sysu-tang.github.io/sysuer-website/docs/sponsor") },
							title = stringResource(R.string.sponsor),
							summary = stringResource(R.string.sponsor_appreciation),
							icon = {
								Icon(
									painterResource(R.drawable.money), contentDescription = null
								)
							})
					}
				}

				PreferenceCategory(title = stringResource(R.string.permission)) {
					item {
						Preference(
							onClick = { openBrowser("https://sysu-tang.github.io/sysuer-website/docs/privacyPolicy/") },
							title = stringResource(R.string.privacy_policy),
							summary = "https://sysu-tang.github.io/sysuer-website/docs/privacyPolicy/",
							icon = {
								Icon(
									painterResource(R.drawable.book), contentDescription = null
								)
							})
					}
					item {
						Preference(
							onClick = { openBrowser("https://sysu-tang.github.io/sysuer-website/docs/userAgreement/") },
							title = stringResource(R.string.user_agreement),
							summary = "https://sysu-tang.github.io/sysuer-website/docs/userAgreement/",
							icon = {
								Icon(
									painterResource(R.drawable.book), contentDescription = null
								)
							})
					}
				}

				PreferenceCategory(title = stringResource(R.string.open_source)) {
					listOf(
						"androidx" to "https://github.com/androidx/androidx",
						"Compose" to "https://github.com/androidx/androidx",
						"Room3" to "https://github.com/androidx/room",
						"Navigation3" to "https://github.com/androidx/navigation",
						"Lifecycle" to "https://github.com/androidx/lifecycle",
						"Datastore" to "https://github.com/androidx/datastore",
						"WorkManager" to "https://github.com/androidx/work",
						"Material3" to "https://github.com/material-components/material-components-android",
						"Kotlin" to "https://github.com/JetBrains/kotlin",
						"kotlinx.serialization" to "https://github.com/Kotlin/kotlinx.serialization",
						"OkHttp" to "https://github.com/square/okhttp",
						"Okio" to "https://github.com/square/okio",
						"fastjson2" to "https://github.com/alibaba/fastjson2",
						"Coil" to "https://github.com/coil-kt/coil",
						"Glide" to "https://github.com/bumptech/glide",
						"Shizuku" to "https://github.com/RikkaApps/Shizuku",
						"RikkaX" to "https://github.com/RikkaApps/RikkaX",
						"MIUIX" to "https://github.com/topYukonga/Miuix",
						"Markwon" to "https://github.com/noties/Markwon",
						"compose-richtext" to "https://github.com/halilozercan/compose-richtext",
						"multiplatform-markdown-renderer" to "https://github.com/mikepenz/multiplatform-markdown-renderer",
						"Commonmark" to "https://github.com/commonmark-java/commonmark-java",
						"CalendarView" to "https://github.com/huanghaibin-dev/CalendarView",
						"Rosemoe Editor" to "https://github.com/Rosemoe/CodeEditor",
						"RxJava" to "https://github.com/ReactiveX/RxJava",
						"RxAndroid" to "https://github.com/ReactiveX/RxAndroid",
						"Jsoup" to "https://github.com/jhy/jsoup",
						"ZXing" to "https://github.com/zxing/zxing",
						"Tink" to "https://github.com/google/tink",
						"Firebase" to "https://github.com/firebase/firebase-android-sdk",
					).forEach { (name, url) ->
						item {
							Preference(
								onClick = { openBrowser(url) },
								title = name,
								summary = url,
								icon = {
									Icon(
										painterResource(R.drawable.version),
										contentDescription = null,
										tint = MaterialTheme.colorScheme.primary
									)
								})
						}
					}
				}
			}
		})
}