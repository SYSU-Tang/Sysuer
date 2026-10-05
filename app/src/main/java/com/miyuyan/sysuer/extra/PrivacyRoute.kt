package com.miyuyan.sysuer.extra

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import com.miyuyan.preference.ItemPreference
import com.miyuyan.preference.PreferenceCategory
import com.miyuyan.preference.PreferenceScreen
import com.miyuyan.sysuer.R
import com.miyuyan.sysuer.nav.navigateBack
import com.miyuyan.sysuer.view.ActivityPager
import com.miyuyan.sysuer.view.MenuItem
import com.miyuyan.sysuer.view.StatePage

@Composable
fun PrivacyRoute(
	backStack: MutableList<NavKey>,
	sharedTransitionScope: SharedTransitionScope? = null,
	animatedVisibilityScope: AnimatedVisibilityScope? = null
) {
	val viewModel: PrivacyViewModel = viewModel()
	val netId by viewModel.netId.collectAsStateWithLifecycle()
	val password by viewModel.password.collectAsStateWithLifecycle()
	val personData by viewModel.personData.collectAsStateWithLifecycle()
	val uiState by viewModel.uiState.collectAsStateWithLifecycle()

	ActivityPager(
			title = stringResource(R.string.privacy),
			expandable = true,
			onNavigationClick = { backStack.navigateBack() },
			sharedTransitionScope = sharedTransitionScope,
			animatedVisibilityScope = animatedVisibilityScope,
			isNestedScrollEnabled = false,
			sharedKey = "Privacy",
			topBarMenus = {
				listOf(
						MenuItem(
								title = stringResource(R.string.edit),
								iconVector = Icons.Rounded.Edit,
								onClick = {
									viewModel.model.contextUtil.changeAccount(
											null, "sysu.edu.cn", null, null
									)
								})
				)
			},
			pageContent = {
				StatePage(state = uiState, onRetry = { viewModel.model.retryAll() }) {
					PreferenceScreen(modifier = Modifier.fillMaxSize()) {
						PreferenceCategory {
							item {
								ItemPreference(
										title = "NetID", summary = netId, icon = R.drawable.web
								)
							}
							item {
								ItemPreference(
										onClick = {
											viewModel.model.contextUtil.copy("password", password)
											viewModel.model.toast(R.string.copy_successfully)
										},
										title = stringResource(R.string.password),
										summary = stringResource(R.string.click_to_copy),
										icon = R.drawable.password
								)
							}
						}

						personData?.let { data ->
							PreferenceCategory {
								remember {
									listOf(
											Triple(R.string.name, "userName", R.drawable.name),
											Triple(R.string.student_id, "userCode", R.drawable.id),
											Triple(R.string.id_type, "idTypeStr", R.drawable.card),
											Triple(R.string.id_num, "idNum", R.drawable.account),
											Triple(R.string.phone, "tele", R.drawable.phone),
											Triple(R.string.email, "email", R.drawable.email)
									)
								}.forEach { (titleRes, apiKey, iconRes) ->
									val titleStr = stringResource(titleRes)
									val valueStr = data.getString(apiKey, "")
									item {
										ItemPreference(onClick = {
											viewModel.model.contextUtil.copy(titleStr, valueStr)
											viewModel.model.toast(R.string.copy_successfully)
										}, title = titleStr, summary = valueStr, icon = iconRes)
									}
								}
							}
						}
					}
				}
			})
}