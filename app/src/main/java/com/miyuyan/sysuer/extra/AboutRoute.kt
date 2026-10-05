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
import com.miyuyan.preference.ItemPreference
import com.miyuyan.preference.JumpPreference
import com.miyuyan.preference.PreferenceCategory
import com.miyuyan.preference.PreferenceScreen
import com.miyuyan.sysuer.R
import com.miyuyan.sysuer.api.ContextUtil
import com.miyuyan.sysuer.api.SettingManager
import com.miyuyan.sysuer.nav.Browser
import com.miyuyan.sysuer.nav.Update
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

	@Composable
	fun LinkPreference(
		name: String, url: String, icon: Int = R.drawable.version, summary: String? = null
	) {
		ItemPreference(
				modifier = if (sharedTransitionScope != null && animatedVisibilityScope != null) {
			with(sharedTransitionScope) {
				Modifier.sharedBounds(
						sharedContentState = rememberSharedContentState(
								key = url
						),
						animatedVisibilityScope = animatedVisibilityScope,
				)
			}
		} else Modifier,
				onClick = { backStack.add(Browser(url)) },
				title = name,
				summary = summary ?: url,
				icon = icon
		)
	}

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
											settingManager.developerMode =
												!settingManager.developerMode
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
							JumpPreference(
									backStack = backStack,
									route = Update,
									sharedTransitionScope = sharedTransitionScope,
									animatedVisibilityScope = animatedVisibilityScope,
									title = R.string.current_version,
									summary = context.packageManager.getPackageInfo(
											context.packageName, 0
									).versionName,
									icon = R.drawable.submit,
							)
						}
						item {
							LinkPreference(
									stringResource(R.string.download_link),
									"https://github.com/SYSU-Tang/Sysuer/releases",
									R.drawable.down
							)
						}
						item {
							LinkPreference(
									stringResource(R.string.official_website),
									"https://sysu-tang.github.io/sysuer-website/",
									R.drawable.web
							)
						}
					}

					PreferenceCategory(title = stringResource(R.string.project)) {
						item {
							LinkPreference(
									"SYSU—Tang", "https://github.com/SYSU-Tang", R.drawable.account
							)
						}
						item {
							LinkPreference(
									stringResource(R.string.app_name),
									"https://github.com/SYSU-Tang/Sysuer/",
									R.drawable.version
							)
						}
						item {
							ItemPreference(
									onClick = {
										context.startActivity(
												Intent(
														Intent.ACTION_VIEW,
														"mqqopensdkapi://bizAgent/qm/qr?url=http%3A%2F%2Fqm.qq.com%2Fcgi-bin%2Fqm%2Fqr%3Ffrom%3Dapp%26p%3Dandroid%26jump_from%3Dwebapi%26k%3Di4M0YfNzHskNiXgiYTAvA1EAWZh7HNQx".toUri()
												)
										)
									},
									title = stringResource(R.string.qq_group),
									summary = "1063579244",
									icon = R.drawable.group
							)
						}
					}

					PreferenceCategory(title = stringResource(R.string.feedback)) {
						item {
							LinkPreference(
									stringResource(R.string.submit_issues),
									"https://github.com/SYSU-Tang/Sysuer/issues",
									R.drawable.question
							)
						}
						item {
							LinkPreference(
									stringResource(R.string.sponsor),
									"https://sysu-tang.github.io/sysuer-website/docs/sponsor",
									R.drawable.money,
									stringResource(R.string.sponsor_appreciation)
							)
						}
					}

					PreferenceCategory(title = stringResource(R.string.permission)) {
						item {
							LinkPreference(
									stringResource(R.string.privacy_policy),
									"https://sysu-tang.github.io/sysuer-website/docs/privacyPolicy/",
									R.drawable.book
							)
						}
						item {
							LinkPreference(
									stringResource(R.string.user_agreement),
									"https://sysu-tang.github.io/sysuer-website/docs/userAgreement/",
									R.drawable.book
							)
						}
					}

					PreferenceCategory(title = stringResource(R.string.open_source)) {
						listOf(
								"androidx" to "https://github.com/androidx/androidx",
								"compose" to "https://developer.android.com/compose",
								"room3" to "https://github.com/androidx/room",
								"navigation3" to "https://github.com/androidx/navigation",
								"lifecycle" to "https://github.com/androidx/lifecycle",
								"datastore" to "https://github.com/androidx/datastore",
								"workManager" to "https://github.com/androidx/work",
								"material3" to "https://github.com/material-components/material-components-android",
								"Kotlin" to "https://github.com/JetBrains/kotlin",
								"kotlinx.serialization" to "https://github.com/Kotlin/kotlinx.serialization",
								"okHttp" to "https://github.com/square/okhttp",
								"okio" to "https://github.com/square/okio",
								"fastjson2" to "https://github.com/alibaba/fastjson2",
								"coil" to "https://github.com/coil-kt/coil",
								"glide" to "https://github.com/bumptech/glide",
								"Shizuku" to "https://github.com/RikkaApps/Shizuku",
								"RikkaX" to "https://github.com/RikkaApps/RikkaX",
								"Miuix" to "https://github.com/topYukonga/Miuix",
								"compose-richtext" to "https://github.com/halilozercan/compose-richtext",
								"multiplatform-markdown-renderer" to "https://github.com/mikepenz/multiplatform-markdown-renderer",
								"CalendarView" to "https://github.com/huanghaibin-dev/CalendarView",
								"Rosemoe Editor" to "https://github.com/Rosemoe/sora-editor",
								"RxJava" to "https://github.com/ReactiveX/RxJava",
								"RxAndroid" to "https://github.com/ReactiveX/RxAndroid",
								"jsoup" to "https://github.com/jhy/jsoup",
								"zxing" to "https://github.com/zxing/zxing",
								"tink" to "https://github.com/google/tink",
								"firebase" to "https://github.com/firebase/firebase-android-sdk",
						).forEach { (name, url) ->
							item {
								LinkPreference(name, url)
							}
						}
					}
				}
			})
}