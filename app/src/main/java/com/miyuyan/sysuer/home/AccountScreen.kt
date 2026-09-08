package com.miyuyan.sysuer.home

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.net.toUri
import androidx.navigation3.runtime.NavKey
import com.miyuyan.preference.JumpPreference
import com.miyuyan.preference.PreferenceCategory
import com.miyuyan.preference.PreferenceScreen
import com.miyuyan.sysuer.R
import com.miyuyan.sysuer.browser.BrowserActivity
import com.miyuyan.sysuer.extra.AboutActivity
import com.miyuyan.sysuer.extra.PrivacyActivity
import com.miyuyan.sysuer.extra.SettingActivity
import com.miyuyan.sysuer.extra.UpdateActivity
import com.miyuyan.sysuer.nav.About
import com.miyuyan.sysuer.nav.Update

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
					R.string.privacy, R.drawable.account, activity = PrivacyActivity::class.java
				)
			}
		}

		PreferenceCategory(title = stringResource(R.string.app)) {
			item {
				JumpPreference(R.string.setting, R.drawable.setting) {
					settingLauncher.launch(Intent(context, SettingActivity::class.java))
				}
			}
			item {
				JumpPreference(
					R.string.about, R.drawable.info,
					route = About,
					backStack = backStack,
					activity = AboutActivity::class.java,
					sharedTransitionScope = sharedTransitionScope,
					animatedVisibilityScope = animatedVisibilityScope,
				)
			}
			item {
				JumpPreference(
					R.string.update, R.drawable.refresh,
					route = Update,
					backStack = backStack,
					activity = UpdateActivity::class.java,
					sharedTransitionScope = sharedTransitionScope,
					animatedVisibilityScope = animatedVisibilityScope,
				)
			}
			item {
				JumpPreference(R.string.help, R.drawable.help) {
					context.startActivity(
						Intent(
							context, BrowserActivity::class.java
						).setData("https://sysu-tang.github.io/sysuer-website/docs/user/introduction".toUri())
					)
				}
			}
		}
	}
}