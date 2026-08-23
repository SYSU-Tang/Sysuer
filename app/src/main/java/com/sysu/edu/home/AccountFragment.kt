package com.sysu.edu.home

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.sysu.edu.R
import com.sysu.edu.browser.BrowserActivity
import com.sysu.edu.extra.AboutActivity
import com.sysu.edu.extra.PrivacyActivity
import com.sysu.edu.extra.SettingActivity
import com.sysu.edu.extra.UpdateActivity

@Composable internal fun AccountScreen(recreate: () -> Unit) {
	val context = LocalContext.current
	val settingLauncher = rememberLauncherForActivityResult(
		contract = ActivityResultContracts.StartActivityForResult(),
		onResult = { o: ActivityResult? ->
			if (o?.resultCode == Activity.RESULT_OK) recreate()
		},
	                                                       )
	
	@Composable fun JumpPreference(
		key: Int,
		icon: Int,
		index: Int = 0,
		count: Int = 1,
		activity: Class<*>? = null,
		onClick: () -> Unit = {
			activity?.let {
				context.startActivity(Intent(context, activity))
			}
		},
	                              ) {
		SegmentedListItem(onClick = onClick, shapes = ListItemDefaults.segmentedShapes(index, count), colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainer), leadingContent = {
			Icon(painter = painterResource(icon), contentDescription = stringResource(key))
		}, /*overlineContent = {
			},*/ trailingContent = {
			Icon(imageVector = Icons.AutoMirrored.Rounded.ArrowForwardIos, contentDescription = stringResource(R.string.forward), modifier = Modifier.size(20.dp))
		}) {
			Text(stringResource(key), style = MaterialTheme.typography.bodyLarge)
		}
	}
	Column(modifier = Modifier
		.fillMaxSize()
		.nestedScroll(rememberNestedScrollInteropConnection())
		.verticalScroll(rememberScrollState())
		.padding(dimensionResource(R.dimen.horizontal_margin), dimensionResource(R.dimen.vertical_margin)),
	       verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.vertical_gap))) {
		Text(text = stringResource(R.string.account),
		     style = MaterialTheme.typography.titleSmall,
		     color = MaterialTheme.colorScheme.primary,
		     modifier = Modifier.padding(dimensionResource(R.dimen.horizontal_gap), dimensionResource(R.dimen.vertical_gap)))
		JumpPreference(R.string.privacy, R.drawable.account, activity = PrivacyActivity::class.java)
		Text(text = stringResource(R.string.app), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(dimensionResource(R.dimen.horizontal_gap), dimensionResource(R.dimen.vertical_gap)))
		JumpPreference(R.string.setting, R.drawable.setting, 0, 4) {
			settingLauncher.launch(Intent(context, SettingActivity::class.java))
		}
		JumpPreference(R.string.about, R.drawable.info, 1, 4, AboutActivity::class.java)
		JumpPreference(R.string.update, R.drawable.refresh, 2, 4, UpdateActivity::class.java)
		JumpPreference(R.string.help, R.drawable.help, 3, 4) {
			context.startActivity(Intent(context, BrowserActivity::class.java).setData("https://sysu-tang.github.io/sysuer-website/docs/user/introduction".toUri()))
		}
	}
}